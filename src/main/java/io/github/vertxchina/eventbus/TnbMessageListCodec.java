package io.github.vertxchina.eventbus;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leibniz on 2022/03/12 2:49 PM
 */
public class TnbMessageListCodec implements MessageCodec<List<Message>, List<Message>> {
  @Override
  public void encodeToWire(Buffer buffer, List<Message> messages) {
    buffer.appendInt(messages.size());
    for (Message message: messages) {
      Buffer encoded = message.toBuffer();
      buffer.appendInt(encoded.length());
      buffer.appendBuffer(encoded);
    }
  }

  @Override
  public List<Message> decodeFromWire(int pos, Buffer buffer) {
    int size = buffer.getInt(pos);
    pos += 4;
    List<Message> result = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      int length = buffer.getInt(pos);
      pos += 4;
      result.add(new Message(buffer.slice(pos, pos + length)));
      pos += length;
    }
    return result;
  }

  @Override
  public List<Message> transform(List<Message> message) {
    return new ArrayList<>(message);
  }

  @Override
  public String name() {
    return "tnblistmessage";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
