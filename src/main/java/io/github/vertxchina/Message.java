package io.github.vertxchina;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;

/**
 * @author Leibniz on 2022/3/10 9:14 PM
 */
public class Message {
  public static final String NICKNAME_KEY = "nickname";
  public static final String CLIENT_ID_KEY = "id";
  public static final String MESSAGE_CONTENT_KEY = "message";

  private static final String MESSAGE_ID_KEY = "id";
  private static final String RECEIVE_TIME_KEY = "time";
  private static final String RECEIVE_TIMESTAMP_KEY = "timestamp";
  private static final String PROTOCOL_KEY = "protocol";

  private final JsonObject json;

  public Message(Buffer buffer) {
    this.json = new JsonObject(buffer);
  }

  public Message(JsonObject json) {
    this.json =json;
  }

  Message(String key, Object value) {
    this.json = new JsonObject().put(key, value);
  }

  Message initServerSide(String id, String time, String protocol) {
    json.put(MESSAGE_ID_KEY, id);
    json.put(RECEIVE_TIME_KEY, time);
    json.put(PROTOCOL_KEY, protocol);
    json.put(RECEIVE_TIMESTAMP_KEY, System.currentTimeMillis());
    return this;
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

  public Buffer toBuffer() {
    return json.toBuffer();
  }

  public String toString(){
    return json.toString();
  }

  public Message copy() {
    return new Message(this.json.copy());
  }

  public String protocol() {
    return this.json.getString(PROTOCOL_KEY);
  }
}
