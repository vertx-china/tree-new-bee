package io.github.vertxchina;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MainLauncher extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    vertx.deployVerticle(TcpServerVerticle.class.getName());
    vertx.deployVerticle(WebsocketServerVerticle.class.getName());
  }
}
