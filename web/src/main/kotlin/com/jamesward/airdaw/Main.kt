import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.dom.*
import kotlinx.dom.clear
import kotlin.math.round
import com.jamesward.airdraw.data.ImageResult


fun main() {
    GlobalScope.launch {
        poll()
    }
}

suspend fun poll() {
    val res = window.fetch("/events").await()
    when (res.status.toInt()) {
        200 -> {
            val imageResult = res.json().await().unsafeCast<ImageResult>()

            val urlImage = "url('data:image/png;base64,${imageResult.image}')"
            document.body?.style?.backgroundImage = urlImage

            if (imageResult.labelAnnotations.isNotEmpty()) {
                document.body?.clear()

                val div = document.create.div()

                imageResult.labelAnnotations.forEach { labelAnnotation ->
                    div.append {
                        p {
                            +"${labelAnnotation.description} = ${round((labelAnnotation.score.toDouble()) * 100)}%"
                        }
                    }
                }

                document.body?.append(div)
            }
            else {
                document.body?.clear()
            }
        }
    }

    window.setTimeout({ GlobalScope.launch { poll() } }, 1000)
}
