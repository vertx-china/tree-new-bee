package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.parsetools.RecordParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.vertxchina.Message.*;

/**
 * @author Leibniz on 2022/03/10 1:05 PM
 */
public class WebsocketServerVerticle extends AbstractVerticle {
  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
  BiMap<String, ServerWebSocket> idSocketBiMap = HashBiMap.create();
  Map<ServerWebSocket, String> netSocketNicknameMap = new HashMap<>();

  @Override
  public void start() {
    Integer port = config().getInteger("WebsocketServer.port", 32168);
    vertx.createHttpServer()
      .webSocketHandler(webSocket -> {
        var id = UUID.randomUUID().toString().replaceAll("-", "");
        System.out.println(id + "Connected!");
        new Message(CLIENT_ID_KEY, id).writeTo(webSocket); //先将id发回
        idSocketBiMap.put(id, webSocket);
        netSocketNicknameMap.put(webSocket, id);//先用id做昵称，避免出现无昵称的情况，避免客户端发送无昵称消息
        //todo 将来有了账户之后，改成登陆之后，再将历史记录发回
        vertx.setTimer(3000,t -> {
//          chatLog.storedMessages().forEach(m -> webSocket.write(m.toBuffer()));
        });
        webSocket.handler(RecordParser.newDelimited(Message.DELIM, h -> {
          System.out.println(h.toString());
          try {
            var message = new Message(h);
            message.initServerSide(id, ZonedDateTime.now().format(dateFormatter));

            if (message.hasNickName()) {
              netSocketNicknameMap.put(webSocket, message.nickName());
              updateUsersList();
            }

            if (netSocketNicknameMap.containsKey(webSocket)) {
              message.setNickName(netSocketNicknameMap.get(webSocket));
            }

            if (message.hasMessage()) {
              sendToOtherUsers(message);
//              chatLog.add(message);
            }
          } catch (Exception e) {
            new Message(MESSAGE_CONTENT_KEY, e.getMessage()).writeTo(webSocket);
          }
        }).maxRecordSize(1024 * 64));

        webSocket.closeHandler(v -> {
          idSocketBiMap.inverse().remove(webSocket);
          netSocketNicknameMap.remove(webSocket);
          updateUsersList();
        });
      })
      .listen(port);
  }

  private void updateUsersList() {
    var jsonArrays = new JsonArray();
    netSocketNicknameMap.values().forEach(jsonArrays::add);
    publishMessage(new Message(NICKNAME_KEY, jsonArrays));
  }

  private void publishMessage(Message msg) {
    idSocketBiMap.values().forEach(msg::writeTo);
  }

  private void sendToOtherUsers(Message msg) {
    var id = msg.messageId();
    for (var receiverSocket : idSocketBiMap.values()) {
      if (receiverSocket != idSocketBiMap.get(id))
        msg.writeTo(receiverSocket);
    }
  }
}
