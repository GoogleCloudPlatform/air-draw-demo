import kotlinx.coroutines.*
import kotlin.browser.*
import kotlinx.html.*
import kotlinx.html.dom.*
import kotlin.dom.clear
import kotlin.math.round


fun main() {
    GlobalScope.launch {
        poll()
    }
}

suspend fun poll() {
    val res = window.fetch("/events").await()
    when (res.status.toInt()) {
        200 -> {
            val json = res.json().await().asDynamic();
            val urlImage = "url('data:image/png;base64,${json.image}')"
            document.body?.style?.backgroundImage = urlImage

            if (json.labelAnnotations != null) {
                document.body?.clear()

                val div = document.create.div()

                json.labelAnnotations.iterator().forEach { labelAnnotation ->
                    div.append {
                        p {
                            +"${labelAnnotation.description} = ${round((labelAnnotation.score as Double) * 100)}%"
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
