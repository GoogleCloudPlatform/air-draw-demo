import kotlinx.coroutines.*
import kotlin.browser.*
import kotlinx.html.*
import kotlinx.html.dom.*
import kotlin.dom.clear
import kotlin.math.round
import com.jamesward.airdraw.data.ImageResult
import com.jamesward.airdraw.data.LabelAnnotation
import kotlinx.serialization.toUtf8Bytes

@ExperimentalStdlibApi
fun main() {
    GlobalScope.launch {
        poll()
    }
}

@ExperimentalStdlibApi
suspend fun poll() {
    val res = window.fetch("/events").await()
    when (res.status.toInt()) {
        200 -> {

            val json = res.json().await().asDynamic()

            // todo: deserializer
            val byteArray = (json.image as String).toUtf8Bytes()

            val labelAnnotations = if (json.labelAnnotations != null) {
                json.labelAnnotations.iterator().asSequence().map {
                    LabelAnnotation(it.description as String, it.score as Float)
                }.toList()
            }
            else {
                emptyList()
            }

            val imageResult = ImageResult(byteArray, labelAnnotations)

            val urlImage = "url('data:image/png;base64,${imageResult.image.decodeToString()}')"
            document.body?.style?.backgroundImage = urlImage

            document.body?.clear()

            val div = document.create.div()

            imageResult.labelAnnotations.forEach { labelAnnotation ->
                div.append {
                    p {
                        +"${labelAnnotation.description} = ${round(labelAnnotation.score * 100)}%"
                    }
                }
            }

            document.body?.append(div)
        }
    }

    window.setTimeout({ GlobalScope.launch { poll() } }, 1000)
}
