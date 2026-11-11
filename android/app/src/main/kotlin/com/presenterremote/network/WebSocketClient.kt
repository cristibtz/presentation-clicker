package com.presenterremote.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, AUTH_FAILED }

data class PresentationState(
    val slide: Int = 0,
    val total: Int = 0,
    val showRunning: Boolean = false,
    val blank: Boolean = false,
    val notes: String = ""
)

class WebSocketClient(
    private val onStateUpdate: (PresentationState) -> Unit,
    private val onConnectionStatus: (ConnectionStatus) -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var ip: String = ""
    private var port: Int = 8765
    private var pin: String = ""
    private var shouldReconnect = false
    private var reconnectDelay = 1000L

    fun connect(ip: String, port: Int, pin: String) {
        this.ip = ip
        this.port = port
        this.pin = pin
        shouldReconnect = true
        reconnectDelay = 1000L
        doConnect()
    }

    private fun doConnect() {
        onConnectionStatus(ConnectionStatus.CONNECTING)
        val request = Request.Builder()
            .url("ws://$ip:$port")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val auth = JsonObject().apply {
                    addProperty("type", "auth")
                    addProperty("pin", pin)
                }
                webSocket.send(gson.toJson(auth))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onConnectionStatus(ConnectionStatus.DISCONNECTED)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onConnectionStatus(ConnectionStatus.DISCONNECTED)
                onError(t.message ?: "Connection failed")
                scheduleReconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            when (json.get("type")?.asString) {
                "auth" -> {
                    val ok = json.get("ok")?.asBoolean ?: false
                    if (ok) {
                        reconnectDelay = 1000L
                        onConnectionStatus(ConnectionStatus.CONNECTED)
                    } else {
                        shouldReconnect = false
                        onConnectionStatus(ConnectionStatus.AUTH_FAILED)
                        webSocket?.close(1000, "Auth failed")
                    }
                }
                "state" -> {
                    val state = PresentationState(
                        slide = json.get("slide")?.asInt ?: 0,
                        total = json.get("total")?.asInt ?: 0,
                        showRunning = json.get("showRunning")?.asBoolean ?: false,
                        blank = json.get("blank")?.asBoolean ?: false,
                        notes = json.get("notes")?.asString ?: ""
                    )
                    onStateUpdate(state)
                }
                "error" -> {
                    onError(json.get("message")?.asString ?: "Unknown error")
                }
            }
        } catch (_: Exception) {}
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        Thread {
            try {
                Thread.sleep(reconnectDelay)
                reconnectDelay = (reconnectDelay * 2).coerceAtMost(30000L)
                if (shouldReconnect) doConnect()
            } catch (_: InterruptedException) {}
        }.start()
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        onConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    fun sendCommand(action: String) {
        val msg = JsonObject().apply {
            addProperty("type", "cmd")
            addProperty("action", action)
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun sendMove(dx: Float, dy: Float) {
        val msg = JsonObject().apply {
            addProperty("type", "move")
            addProperty("dx", dx.toInt())
            addProperty("dy", dy.toInt())
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun sendClick(button: String = "left") {
        val msg = JsonObject().apply {
            addProperty("type", "click")
            addProperty("button", button)
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun sendLaser(state: String) {
        val msg = JsonObject().apply {
            addProperty("type", "laser")
            addProperty("state", state)
        }
        webSocket?.send(gson.toJson(msg))
    }
}
