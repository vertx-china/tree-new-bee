package io.github.vertxchina

import io.github.vertxchina.eventbus.Message
import io.github.vertxchina.eventbus.TnbMessageCodec
import io.github.vertxchina.persistverticle.MessageStoreVerticle
import io.github.vertxchina.persistverticle.TelegraphImgVerticle
import io.github.vertxchina.webverticle.TcpServerVerticle
import io.github.vertxchina.webverticle.WebSocketVerticle
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {
  var log = LoggerFactory.getLogger(MainVerticle::class.java)
  override suspend fun start() {
    vertx.exceptionHandler { log.info(it.message) }
    vertx.eventBus().registerDefaultCodec(Message::class.java, TnbMessageCodec())
    vertx.deployVerticle(MessageStoreVerticle::class.java.name).await()
    vertx.deployVerticle(TelegraphImgVerticle::class.java.name).await()
    vertx.deployVerticle(TcpServerVerticle::class.java.name)
    vertx.deployVerticle(WebSocketVerticle::class.java.name)
  }
}