package io.github.vertxchina.eventbus;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
  private static final String GENERATOR_KEY = "generatorVerticleID";

  public static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

  private final JsonObject json;

  public Message(Buffer buffer) {
    this.json = new JsonObject(buffer);
  }

  public Message(JsonObject json) {
    this.json =json;
  }

  public Message(String key, Object value) {
    this.json = new JsonObject().put(key, value);
  }

  public Message initServerSide(String id, String generatorVerticle) {
    json.put(MESSAGE_ID_KEY, id);
    json.put(RECEIVE_TIME_KEY, ZonedDateTime.now().format(dateFormatter));
    json.put(GENERATOR_KEY, generatorVerticle);
    json.put(RECEIVE_TIMESTAMP_KEY, System.currentTimeMillis());
    return this;
  }

  public boolean hasNickName() {
    return null != json.getValue(NICKNAME_KEY)
      && !json.getValue(NICKNAME_KEY).toString().isEmpty();
  }

  public String nickName() {
    return json.getValue(NICKNAME_KEY).toString().trim();
  }

  public void setNickName(String nickname) {
    json.put(NICKNAME_KEY, nickname);
  }

  public boolean hasMessage(){
    return json.containsKey(MESSAGE_CONTENT_KEY);
  }

  public String messageId(){
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

  public String generator() {
    return this.json.getString(GENERATOR_KEY);
  }
}
