package io.github.vertxchina.webverticle;

import io.vertx.core.Handler;

public class CustomHandler {
  public static <T> Handler<T> handler(Handler<T> handler, Handler<Throwable> exceptionHandler){
    return t -> {
      try{
        handler.handle(t);
      }catch (Throwable e){
        exceptionHandler.handle(e);
      }
    };
  }
}
