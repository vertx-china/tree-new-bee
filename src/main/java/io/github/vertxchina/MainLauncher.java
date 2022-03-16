package io.github.vertxchina;

import io.github.vertxchina.eventbus.Message;
import io.github.vertxchina.eventbus.TnbMessageCodec;
import io.github.vertxchina.persistverticle.MessageStoreVerticle;
import io.github.vertxchina.webverticle.TcpServerVerticle;
import io.github.vertxchina.webverticle.WebsocketServerVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class MainLauncher extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus()
      .registerDefaultCodec(Message.class, new TnbMessageCodec());
    vertx
      .deployVerticle(MessageStoreVerticle.class.getName())
      .onSuccess(id -> {
        vertx.deployVerticle(TcpServerVerticle.class.getName());
        vertx.deployVerticle(WebsocketServerVerticle.class.getName());
      });
  }

  //FOR 本地测试
  public static void main(String... args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(MainLauncher.class.getName());
  }
}
