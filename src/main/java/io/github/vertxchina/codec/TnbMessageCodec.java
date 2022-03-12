package io.github.vertxchina.codec;

import io.github.vertxchina.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * @author Leibniz on 2022/03/12 2:49 PM
 */
public class TnbMessageCodec implements MessageCodec<Message, Message> {
  @Override
  public void encodeToWire(Buffer buffer, Message message) {
    Buffer encoded = message.toBuffer();
    buffer.appendInt(encoded.length());
    buffer.appendBuffer(encoded);
  }

  @Override
  public Message decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    return new Message(buffer.slice(pos, pos + length));
  }

  @Override
  public Message transform(Message message) {
    return message.copy();
  }

  @Override
  public String name() {
    return "tnbmessage";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
