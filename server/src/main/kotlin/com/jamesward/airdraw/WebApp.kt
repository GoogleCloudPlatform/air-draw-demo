/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jamesward.airdraw

import com.jamesward.airdraw.data.*
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings
import com.google.cloud.vision.v1.*
import com.google.cloud.vision.v1.Feature.Type
import com.google.protobuf.ByteString
import com.google.pubsub.v1.*
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.jackson.serialize.JacksonObjectSerializer
import io.micronaut.runtime.Micronaut
import io.micronaut.views.View
import io.reactivex.Single
import smile.interpolation.Interpolation
import smile.interpolation.KrigingInterpolation1D
import smile.plot.Headless
import smile.plot.LinePlot
import smile.plot.PlotCanvas
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.concurrent.ArrayBlockingQueue
import javax.annotation.PreDestroy
import javax.imageio.ImageIO
import javax.inject.Singleton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.math.abs


fun main() {
    Micronaut.build().packages("com.jamesward.airdraw").mainClass(WebApp::class.java).start()
}

fun List<EntityAnnotation>.toLabelAnnotation(): List<LabelAnnotation> {
    return this.map { entityAnnotation ->
        LabelAnnotation(entityAnnotation.description, entityAnnotation.score)
    }
}

@Controller
class WebApp(private val airDraw: AirDraw, private val bus: Bus) {

    @View("index")
    @Get("/")
    fun index(): HttpResponse<Unit> {
        return HttpResponse.ok()
    }

    @Post("/draw")
    fun draw(@Body readings: List<Orientation>): ImageResult? {
        return airDraw.run(readings)?.let { imageResult ->
            bus.put(imageResult)
            return imageResult
        }
    }

    @Post("/show")
    fun show(@Body imageResult: ImageResult): HttpResponse<Unit> {
        bus.put(imageResult)
        return HttpResponse.noContent()
    }

    @Get("/events")
    fun events(): HttpResponse<ImageResult> {
        val maybe = bus.take()
        return if (maybe != null)
            HttpResponse.ok(maybe)
        else
            HttpResponse.noContent()
    }

}

@Singleton
@Requires(beans = [MyImageAnnotatorClient::class])
class Vision(private val myImageAnnotatorClient: MyImageAnnotatorClient) {

    fun label(bytes: ByteArray): AnnotateImageResponse? {
        val imgBytes = ByteString.copyFrom(bytes)
        val img = Image.newBuilder().setContent(imgBytes).build()
        val feature = Feature.newBuilder().setType(Type.LABEL_DETECTION).build()
        val request = AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(img).build()

        val response = myImageAnnotatorClient.imageAnnotatorClient.batchAnnotateImages(arrayListOf(request))
        return response.responsesList.firstOrNull()
    }

}

// used to provide either a GCP or local ImageAnnotatorClient
interface MyImageAnnotatorClient {
    val imageAnnotatorClient: ImageAnnotatorClient
}

@Singleton
@Requires(property = "google.application.credentials")
class LocalImageAnnotatorClient: MyImageAnnotatorClient {
    override val imageAnnotatorClient = ImageAnnotatorClient.create()
}

@Singleton
@Requires(env = [Environment.GOOGLE_COMPUTE])
class GCPImageAnnotatorClient: MyImageAnnotatorClient {
    override val imageAnnotatorClient = ImageAnnotatorClient.create()
}

interface Bus {
    fun put(imageResult: ImageResult)
    fun take(): ImageResult?
}

interface CloudBusConfig {
    val projectId: String
        get() = ServiceOptions.getDefaultProjectId()

    val topic: String
        get() = "air-draw"

    val subsciption: String
        get() = "air-draw"
}

@Singleton
@Requires(property = "google.application.credentials")
class PropCloudBusConfig: CloudBusConfig

@Singleton
@Requires(env = [Environment.GOOGLE_COMPUTE])
class GcpCloudBusConfig: CloudBusConfig

@Singleton
@Requires(beans = [CloudBusConfig::class])
class CloudBus(cloudBusConfig: CloudBusConfig, private val objectSerializer: JacksonObjectSerializer): Bus, AutoCloseable {

    val topicName = ProjectTopicName.of(cloudBusConfig.projectId, cloudBusConfig.topic)
    val publisher = Publisher.newBuilder(topicName).build()

    val subscriptionName = ProjectSubscriptionName.format(cloudBusConfig.projectId, cloudBusConfig.subsciption)

    val subscriberStubSettings = SubscriberStubSettings.newBuilder()
            .setTransportChannelProvider(SubscriberStubSettings.defaultGrpcTransportProviderBuilder().build())
            .build()

    val subscriber = GrpcSubscriberStub.create(subscriberStubSettings)
    val pullRequest = PullRequest.newBuilder()
        .setMaxMessages(1)
        .setReturnImmediately(true)
        .setSubscription(subscriptionName)
        .build()

    override fun put(imageResult: ImageResult) {
        objectSerializer.serialize(imageResult).map { bytes ->
            val data: ByteString = ByteString.copyFrom(bytes)

            val pubsubMessage = PubsubMessage.newBuilder()
                    .setData(data)
                    .build()

            // block
            publisher.publish(pubsubMessage).get()
        }
    }

    override fun take(): ImageResult? {
        val pullResponse = subscriber.pullCallable().call(pullRequest)

        return pullResponse.receivedMessagesList.firstOrNull()?.let { receivedMessage ->
            val acknowledgeRequest = AcknowledgeRequest.newBuilder()
                    .setSubscription(subscriptionName)
                    .addAckIds(receivedMessage.ackId)
                    .build()

            subscriber.acknowledgeCallable().call(acknowledgeRequest)

            objectSerializer.deserialize<ImageResult>(receivedMessage.message.data.toByteArray(), ImageResult::class.java).get()
        }
    }

    @PreDestroy
    override fun close() {
        publisher.shutdown()
        subscriber.shutdown()
    }

}

@Singleton
@Requires(missingBeans = [CloudBus::class])
class LocalBus: Bus {

    private val queue = ArrayBlockingQueue<ImageResult>(256)

    override fun put(imageResult: ImageResult) {
        queue.add(imageResult)
    }

    override fun take(): ImageResult? {
        val maybe = queue.firstOrNull()
        if (maybe != null)
            queue.remove(maybe)

        return maybe
    }
}

interface AirDraw {
    fun run(readings: List<Orientation>): ImageResult?
}

@Singleton
@Requires(beans = [Vision::class])
class CloudAirDraw(private val vision: Vision): AirDraw {
    override fun run(readings: List<Orientation>): ImageResult? {
        val canvas = AirDrawSmileViewer.draw(readings)
        val bytes = Drawer.draw(canvas)

        return vision.label(bytes)?.let { annotateImageResponse ->
            ImageResult(bytes, annotateImageResponse.labelAnnotationsList.toLabelAnnotation())
        }
    }
}

object Drawer {
    fun draw(canvas: PlotCanvas): ByteArray {
        canvas.getAxis(0).isGridVisible = false
        canvas.getAxis(0).isFrameVisible = false
        canvas.getAxis(0).isLabelVisible = false
        canvas.getAxis(1).isGridVisible = false
        canvas.getAxis(1).isFrameVisible = false
        canvas.getAxis(1).isLabelVisible = false
        canvas.margin = 0.0

        val headless = Headless(canvas)
        headless.pack()
        headless.isVisible = true
        headless.setSize(1024, 1024)

        val bi = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
        val g2d = bi.createGraphics()
        canvas.print(g2d)

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bi, "png", outputStream)

        return outputStream.toByteArray()
    }
}

@Singleton
@Requires(missingBeans = [Vision::class])
class LocalAirDraw: AirDraw {

    // displays the drawing in a local window and draws it to a bitmap
    override fun run(readings: List<Orientation>): ImageResult? {
        val canvas = AirDrawSmileViewer.draw(readings)
        // todo: move this to a debug mode that doesn't return the ImageResult or something
        //       because the canvas can't be reused for both the jpanel and the bitmap
        //       and having two different canvasi also causes problems where one doesn't get rendered
        //       and it is nice to have the interactive plotcanvas for debugging ranges
        //AirDrawSmileViewer.show(canvas)
        return ImageResult(Drawer.draw(canvas), emptyList())
    }
}

object AirDrawSmileViewer {

    fun show(jPanel: JPanel) {
        val frame = JFrame()
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.contentPane.add(jPanel)
        frame.size = Dimension(1024, 1024)
        frame.isVisible = true
    }

    fun draw(readings: List<Orientation>): PlotCanvas {
        val t = readings.map { it.timestamp.toDouble() }.toDoubleArray()
        val x = readings.map { it.azimuth.toDouble() }.toDoubleArray()
        val y = readings.map { it.pitch.toDouble() * -1 }.toDoubleArray()

        val xl = KrigingInterpolation1D(t, x)

        // the pitch (y) may be the same across all readings which means the values can't be interpolated
        val yl = try {
            KrigingInterpolation1D(t, y)
        } catch (e: Exception) {
            Interpolation { it }
        }

        val minTimestamp = readings.minBy { it.timestamp }!!.timestamp
        val maxTimestamp = readings.maxBy { it.timestamp }!!.timestamp
        val time = maxTimestamp - minTimestamp

        val xy: Array<DoubleArray> = (minTimestamp..maxTimestamp step(time / 100)).map { timestamp ->
            val ix = xl.interpolate(timestamp.toDouble())
            val iy = yl.interpolate(timestamp.toDouble())
            doubleArrayOf(ix, iy)
        }.toTypedArray()

        val yBounds = doubleArrayOf(-0.5, 1.5)
        val defaultXWidth = 2

        val minX = xy.minBy { it[0] }!![0]
        val maxX = xy.maxBy { it[0] }!![0]

        val width = abs(minX) + abs(maxX)
        val xBounds = if (width < defaultXWidth) {
            val more = (defaultXWidth - width) / 2
            doubleArrayOf(minX - more, maxX + more)
        } else {
            doubleArrayOf(minX, maxX)
        }

        val linePlot = LinePlot(xy).setStroke(BasicStroke(20F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND))
        val canvas = PlotCanvas(doubleArrayOf(xBounds[0], yBounds[0]), doubleArrayOf(xBounds[1], yBounds[1]))
        canvas.add(linePlot)

        return canvas
    }
}
