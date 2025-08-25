import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    val faceRecognizer = FaceRecognizer("haarcascade_frontalface_default.xml")

    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ContentNegotiation) {
            gson()
        }

        routing {
            // Serve static files from resources/static/
            staticResources("/", "static") {
                default("index.html")
            }

            post("/enroll") {
                val multipart = call.receiveMultipart()
                var name: String? = null
                var imageBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is io.ktor.http.content.PartData.FormItem -> {
                            if (part.name == "name") {
                                name = part.value.trim()
                            }
                        }

                        is io.ktor.http.content.PartData.FileItem -> {
                            if (part.name == "file") {
                                imageBytes = part.streamProvider().readBytes()
                            }
                        }

                        else -> {}
                    }
                    part.dispose()
                }

                if (name.isNullOrEmpty() || imageBytes == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing name or image"))
                    return@post
                }

                val success = faceRecognizer.enroll(imageBytes!!, name!!)
                if (!success) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No face detected"))
                } else {
                    call.respond(mapOf("name" to name))
                }
            }

            post("/recognize") {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    if (part is io.ktor.http.content.PartData.FileItem && part.name == "file") {
                        imageBytes = part.streamProvider().readBytes()
                    }
                    part.dispose()
                }

                if (imageBytes == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing image file"))
                    return@post
                }

                val result = faceRecognizer.recognize(imageBytes!!)
                call.respond(result)
            }
        }
    }.start(wait = true)
}
