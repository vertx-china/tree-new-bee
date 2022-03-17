package io.github.vertxchina.webverticle

import io.github.vertxchina.eventbus.EventbusAddress
import io.github.vertxchina.eventbus.Message
import io.github.vertxchina.persistverticle.TelegraphImgVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch
import java.util.*

class WebSocketVerticle : CoroutineVerticle() {

  private val log = LoggerFactory.getLogger(this::class.java)
  private val verticleID = UUID.randomUUID().toString()
  private val socketHolder = SocketWriteHolder { socket: ServerWebSocket, message: Message ->
    socket.writeTextMessage(message.toString())
  }

  override suspend fun start() {
    try {
      val port: Int = config.getInteger("WebsocketServer.port", 32168)
      vertx.createHttpServer(HttpServerOptions().setMaxWebSocketFrameSize(1024 * 64)
        .setMaxWebSocketMessageSize(1024 * 1024 * 64)).webSocketHandler { webSocket: ServerWebSocket ->
        val id = SocketWriteHolder.generateClientId()
        log.info("$id Connected Websocket Server!")
        webSocket.writeTextMessage(Message(Message.CLIENT_ID_KEY, id).toString()) //先将id发回
        socketHolder.addSocket(id, webSocket)

        //todo 将来有了账户之后，改成登陆之后，再将历史记录发回
        vertx.eventBus().request<List<Message>>(EventbusAddress.READ_STORED_MESSAGES, null) { msgs ->
          if (msgs.succeeded()) {
            vertx.setTimer(3000) {
              msgs.result().body().forEach { webSocket.writeTextMessage(it.toString()) }
            }
          }
        }

        val stringBuilder = StringBuilder()
        webSocket.frameHandler {
          launch {
            if (it.isText) stringBuilder.clear()

            if (it.isText || it.isContinuation) stringBuilder.append(it.textData())

            if (it.isFinal && stringBuilder.isNotEmpty()) {//必需加上is not empty，因为有时候客户端会发空的final frame过来
              try {
                val json = JsonObject(stringBuilder.toString())

                if (it.isContinuation) processImagesInJson(json)

                log.debug("Message content: $json")
                val message = Message(json).initServerSide(id, verticleID)
                socketHolder.receiveMessage(webSocket, message)
                if (message.hasMessage()) {
                  socketHolder.sendToOtherUsers(message)
                  vertx.eventBus().publish(EventbusAddress.PUBLISH_MESSAGE, message)
                }
              } catch (e: Exception) {
                webSocket.writeTextMessage(Message(Message.MESSAGE_CONTENT_KEY, e.message).toString())
              }
              stringBuilder.clear()
            }
          }
        }.closeHandler { socketHolder.removeSocket(webSocket) }
      }.listen(port).await()

      log.info("WebSocketVerticle deployed with verticle ID: $verticleID")
      log.info("WebSocketVerticle listen to port: $port")

      vertx.eventBus().consumer<Message>(EventbusAddress.PUBLISH_MESSAGE).handler {
        val tnbMsg = it.body()
        if (tnbMsg.generator() != verticleID) {
          socketHolder.publishMessage(tnbMsg)
        }
      }
    } catch (error: Throwable) {
      log.error("WebsocketServer start failed: " + error.message, error)
    }
  }

  private suspend fun processImagesInJson(json: Any) {
    when (json) {
      is JsonObject -> {
        if (json.containsKey("message")) processImagesInJson(json.getValue("message"))
        if (json.containsKey("base64")) {
          val base64String = json.getString("base64", "")
          val url = vertx.eventBus().request<String>(TelegraphImgVerticle::class.java.name, base64String).await().body()
          json.remove("base64")
          json.put("url", url)
        }
      }
      is JsonArray -> {
        for (obj in json) processImagesInJson(obj)
      }
    }
  }

}