import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson()
        }
        install(CallLogging)

        routing {
            staticResources("/", "static") {
                default("index.html") // place index.html and app.js in `resources/static/`
            }
            post("/recognize") {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        imageBytes = part.streamProvider().readBytes()
                    }
                    part.dispose()
                }

                val name = FaceRecognizer.processImage(imageBytes!!)
                call.respond(HttpStatusCode.OK, mapOf("name" to name))
            }

            post("/enroll") {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null
                var name: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "name") {
                                name = part.value
                            }
                        }
                        is PartData.FileItem -> {
                            imageBytes = part.streamProvider().readBytes()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                FaceRecognizer.enroll(imageBytes!!, name!!)
                call.respond(HttpStatusCode.OK, mapOf("status" to "enrolled", "name" to name))
            }
        }
    }.start(wait = true)
}
