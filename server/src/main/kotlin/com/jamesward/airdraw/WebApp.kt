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

import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.Micronaut
import io.micronaut.views.View
import smile.interpolation.KrigingInterpolation1D
import smile.plot.Headless
import smile.plot.LinePlot
import smile.plot.PlotCanvas
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import java.nio.file.Files
import javax.inject.Singleton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import com.google.cloud.vision.v1.Feature.Type
import io.micronaut.context.annotation.Requirements
import io.micronaut.http.MediaType
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.http.server.types.files.SystemFile
import io.micronaut.http.sse.Event
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.reactivestreams.Publisher
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.stream.Stream


fun main() {
    Micronaut.build().packages("com.jamesward.airdraw").mainClass(WebApp::class.java).start()
}

data class Reading(val azimuth: Float, val pitch: Float, val timestamp: Long)

data class LabelAnnotation(val description: String, val score: Float)

fun List<EntityAnnotation>.toLabelAnnotation(): List<LabelAnnotation> {
    return this.map { entityAnnotation ->
        LabelAnnotation(entityAnnotation.description, entityAnnotation.score)
    }
}

@Controller
class WebApp(private val airDraw: AirDraw) {

    val queue = ArrayBlockingQueue<Pair<String, List<LabelAnnotation>>>(256)

    @View("index")
    @Get("/")
    fun index(): Single<HttpResponse<String>> {
        return Single.just(HttpResponse.ok(""))
    }

    @Post("/draw")
    fun draw(@Body readingsSingle: Single<List<Reading>>): Single<HttpResponse<String>> {
        return readingsSingle.map { readings ->
            airDraw.run(readings)?.let { (file, annotateImageResponse) ->
                queue.add(Pair(file.path, annotateImageResponse.labelAnnotationsList.toLabelAnnotation()))
            }

            HttpResponse.ok("")
        }
    }

    // TODO: This is insecure
    @Get("/img{?path}")
    fun img(path: String?): StreamedFile? {
        return path?.let {
            StreamedFile(File(it).inputStream(), MediaType.IMAGE_PNG_TYPE)
        }
    }

    @Get("/events")
    fun events(): Maybe<Pair<String, List<LabelAnnotation>>> {
        val maybe = queue.firstOrNull()
        return if (maybe != null) {
            queue.remove(maybe)
            Maybe.just(maybe)
        } else {
            Maybe.empty()
        }
    }

}

@Singleton
@Requires(beans = [MyImageAnnotatorClient::class])
class Vision(private val myImageAnnotatorClient: MyImageAnnotatorClient) {

    fun label(f: File): AnnotateImageResponse? {
        val data = Files.readAllBytes(f.toPath())
        val imgBytes = ByteString.copyFrom(data)
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

interface AirDraw {
    fun run(readings: List<Reading>): Pair<File, AnnotateImageResponse>?
}

@Singleton
@Requires(beans = [Vision::class])
class CloudAirDraw(private val vision: Vision): AirDraw {
    override fun run(readings: List<Reading>): Pair<File, AnnotateImageResponse>? {
        val f = Files.createTempFile("airdraw", ".png").toFile()

        val canvas = AirDrawSmileViewer.draw(readings)

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

        canvas.save(f)

        return vision.label(f)?.let { annotateImageResponse ->
            Pair(f, annotateImageResponse)
        }
    }
}

@Singleton
@Requires(missingBeans = [Vision::class])
class LocalAirDraw: AirDraw {
    override fun run(readings: List<Reading>): Pair<File, AnnotateImageResponse>? {
        val canvas = AirDrawSmileViewer.draw(readings)
        AirDrawSmileViewer.show(canvas)
        return null
    }
}

object AirDrawSmileViewer {
    fun show(jPanel: JPanel) {
        val frame = JFrame()
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.contentPane.add(JPanel(GridLayout(4, 4)))
        frame.size = Dimension(1000, 1000)
        frame.isVisible = true
        frame.add(jPanel)
    }

    fun draw(readings: List<Reading>): PlotCanvas {
        val t = readings.map { it.timestamp.toDouble() }.toDoubleArray()
        val x = readings.map { it.azimuth.toDouble() }.toDoubleArray()
        val y = readings.map { it.pitch.toDouble() * -1 }.toDoubleArray()

        val xl = KrigingInterpolation1D(t, x)
        val yl = KrigingInterpolation1D(t, y)

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

        val width = Math.abs(minX) + Math.abs(maxX)
        val xBounds = if (width < defaultXWidth) {
            val more = (defaultXWidth - width) / 2
            doubleArrayOf(minX - more, maxX + more)
        } else {
            doubleArrayOf(minX, maxX)
        }

        val linePlot = LinePlot(xy).setStroke(BasicStroke(10F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND))
        val canvas = PlotCanvas(doubleArrayOf(xBounds[0], yBounds[0]), doubleArrayOf(xBounds[1], yBounds[1]))
        canvas.add(linePlot)

        return canvas
    }
}
