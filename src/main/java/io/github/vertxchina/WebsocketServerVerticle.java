package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        webSocket.write(new JsonObject().put("id", id).toBuffer().appendString("\r\n"));
        idSocketBiMap.put(id, webSocket);
        netSocketNicknameMap.put(webSocket, id);//先用id做昵称，避免出现无昵称的情况，避免客户端发送无昵称消息
        //todo 将来有了账户之后，改成登陆之后，再将历史记录发回
        /*var chatLogAtThisMoment = chatLog.storedMessages();
        vertx.setTimer(3000,t -> {
          chatLogAtThisMoment.forEach(m -> socket.write(m + "\r\n"));
        });*/
        webSocket.handler(RecordParser.newDelimited("\r\n", h -> {
          System.out.println(h.toString());
          try {
            var messageJson = new JsonObject(h);
            messageJson.put("id", id);
            messageJson.put("time", ZonedDateTime.now().format(dateFormatter));

            if (null != messageJson.getValue("nickname")
              && !messageJson.getValue("nickname").toString().isEmpty()) {
              var nickname = messageJson.getValue("nickname").toString().trim();
              netSocketNicknameMap.put(webSocket, nickname);
              updateUsersList();
            }

            if (netSocketNicknameMap.containsKey(webSocket)) {
              messageJson.put("nickname", netSocketNicknameMap.get(webSocket));
            }

            if (messageJson.containsKey("message")) {
              sendToOtherUsers(messageJson);
//              chatLog.add(messageJson);
            }
          } catch (Exception e) {
            webSocket.write(new JsonObject().put("message", e.getMessage()).toBuffer().appendString("\r\n"));
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
    for (var nn : netSocketNicknameMap.values()) {
      jsonArrays.add(nn);
    }
    publishMessage(new JsonObject().put("nicknames", jsonArrays));
  }

  private void publishMessage(JsonObject jsonMsg) {
    for (var receiverSocket : idSocketBiMap.values()) {
      receiverSocket.write(jsonMsg.toBuffer().appendString("\r\n"));
    }
  }

  private void sendToOtherUsers(JsonObject jsonMsg) {
    var id = jsonMsg.getValue("id").toString();
    for (var receiverSocket : idSocketBiMap.values()) {
      if (receiverSocket != idSocketBiMap.get(id))
        receiverSocket.write(jsonMsg.toBuffer().appendString("\r\n"));
    }
  }
}
