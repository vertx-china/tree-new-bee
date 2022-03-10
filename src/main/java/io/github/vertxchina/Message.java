package io.github.vertxchina;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;

/**
 * @author Leibniz on 2022/3/10 9:14 PM
 */
public class Message {
  public static final String DELIM = "\r\n";
  public static final String NICKNAME_KEY = "nickname";
  public static final String CLIENT_ID_KEY = "id";
  private static final String MESSAGE_ID_KEY = "id";
  private static final String RECEIVE_TIME_KEY = "time";
  public static final String MESSAGE_CONTENT_KEY = "message";

  private final JsonObject json;

  Message(Buffer buffer) {
    this.json = new JsonObject(buffer);
  }

  Message(JsonObject json) {
    this.json =json;
  }

  Message(String key, Object value) {
    this.json = new JsonObject().put(key, value);
  }

  void initServerSide(String id, String time) {
    json.put(MESSAGE_ID_KEY, id);
    json.put(RECEIVE_TIME_KEY, time);
  }

  boolean hasNickName() {
    return null != json.getValue(NICKNAME_KEY)
      && !json.getValue(NICKNAME_KEY).toString().isEmpty();
  }

  String nickName() {
    return json.getValue(NICKNAME_KEY).toString().trim();
  }

  void setNickName(String nickname) {
    json.put(NICKNAME_KEY, nickname);
  }

  boolean hasMessage(){
    return json.containsKey(MESSAGE_CONTENT_KEY);
  }

  String messageId(){
    return json.getString(MESSAGE_ID_KEY);
  }

  Future<Void> writeTo(WriteStream<Buffer> writeStream) {
    return writeStream.write(json.toBuffer().appendString(DELIM));
  }

  public Buffer toBuffer() {
    return json.toBuffer().appendString(DELIM);
  }
}
