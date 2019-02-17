/*
 * Copyright 2019 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.tenkiv.kuantify.networking.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.networking.*

private val logger = KotlinLogging.logger {}

fun Application.kuantifyHost() {
    KuantifyHost.apply { init() }
}

internal object KuantifyHost {

    @Volatile
    private var hostedDevice: LocalDevice? = null

    internal val isHosting = hostedDevice != null

    fun Application.init() {

        install(DefaultHeaders)

        install(WebSockets) {
            pingPeriod = 1.minutesSpan
        }

        install(Sessions) {
            cookie<ClientId>("CLIENT_ID")
        }

        intercept(ApplicationCallPipeline.Features) {
            if (call.sessions.get<ClientId>() == null) {
                call.sessions.set(ClientId(generateNonce()))
            }
        }

        routing {
            get(RC.INFO) {
                call.respondText(hostedDevice?.getInfo() ?: "null")
            }

            webSocket(RC.WEBSOCKET) {

                val clientID = call.sessions.get<ClientId>()

                if (clientID == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
                }

                ClientHandler.connectionOpened(clientID.id, this@webSocket)

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receiveMessage(clientID.id, frame.readText())
                            logger.trace {
                                "Received message - ${frame.readText()} - on local device ${hostedDevice?.uid}"
                            }
                        }
                    }
                } finally {
                    ClientHandler.connectionClosed(clientID.id, this@webSocket)
                    logger.trace { "Websocket connection closed for client ${clientID.id}" }
                }

            }
        }

    }

    internal fun startHosting(device: LocalDevice) {
        hostedDevice = device
    }

    internal suspend fun stopHosting() {
        ClientHandler.closeAllSessions()
        hostedDevice = null
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(clientId: String, message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)
        when (route.first()) {
            RC.MESSAGE_ERROR -> clientReportedError()
            else -> hostedDevice?.receiveNetworkMessage(route, message) ?: deviceNotHosted()
        }
    }

    private fun deviceNotHosted() {

    }

    private fun clientReportedError() {

    }

}

internal data class ClientId(val id: String)