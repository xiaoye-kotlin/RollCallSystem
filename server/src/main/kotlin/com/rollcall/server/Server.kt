package com.rollcall.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.host
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

private val onlineUsers = ConcurrentHashMap<String, Long>()

fun main() {
    val port = System.getenv("ROLLCALL_SERVER_PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, module = Application::rollCallServer).start(wait = true)
}

fun Application.rollCallServer() {
    routing {
        get("/") {
            call.respondText(
                """{"name":"RollCallSystem Ktor Server","mode":"embedded","files":${BuiltInServerData.fileNamesJson}}""",
                ContentType.Application.Json
            )
        }

        get("/index.php") {
            val remoteHost = call.request.host()
            val count = updateOnlineUsers(remoteHost)
            call.respondText(count.toString(), ContentType.Text.Plain)
        }

        get("/voice.php") {
            val text = call.request.queryParameters["text"]?.trim().orEmpty()
            if (text.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "missing text")
                return@get
            }
            val encoded = URLEncoder.encode(text, Charsets.UTF_8)
            call.respondRedirect("https://dict.youdao.com/dictvoice?audio=$encoded&type=2", permanent = false)
        }

        get("/{name...}") {
            val path = call.request.path().trimStart('/')
            val content = BuiltInServerData.fileMap[path]
            if (content == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondText(content, BuiltInServerData.contentTypeFor(path))
        }
    }
}

private fun updateOnlineUsers(remoteAddress: String): Int {
    val timeoutSeconds = 15L
    val now = System.currentTimeMillis() / 1000
    onlineUsers.entries.removeIf { (_, expireAt) -> expireAt <= now }
    onlineUsers[remoteAddress] = now + timeoutSeconds
    return onlineUsers.size
}
