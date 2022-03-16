package io.github.vertxchina.webverticle;

import io.github.vertxchina.eventbus.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

@FunctionalInterface
public interface SocketWriter<S extends WriteStream<Buffer>> {
  void write(S socket, Message message);
}
