package io.github.vertxchina

import io.github.vertxchina.eventbus.Message
import io.github.vertxchina.eventbus.TnbMessageCodec
import io.github.vertxchina.persistverticle.MessageStoreVerticle
import io.github.vertxchina.persistverticle.TelegraphImgVerticle
import io.github.vertxchina.webverticle.TcpServerVerticle
import io.github.vertxchina.webverticle.WebSocketVerticle
import io.github.vertxchina.webverticle.WebsocketServerVerticle
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {
  override suspend fun start() {
    vertx.eventBus().registerDefaultCodec(Message::class.java, TnbMessageCodec())
    vertx.deployVerticle(MessageStoreVerticle::class.java.name).await()
    vertx.deployVerticle(TelegraphImgVerticle::class.java.name).await()
    vertx.deployVerticle(TcpServerVerticle::class.java.name)
    vertx.deployVerticle(WebSocketVerticle::class.java.name)
  }
}