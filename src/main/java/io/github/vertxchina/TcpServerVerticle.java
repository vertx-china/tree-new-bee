package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TcpServerVerticle extends AbstractVerticle {
  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
  BiMap<String, NetSocket> idSocketBiMap = HashBiMap.create();
  Map<NetSocket, String> netSocketNicknameMap = new HashMap<>();

  @Override
  public void start() {
    Integer port = config().getInteger("TcpServerVerticle.port", 32167);
    vertx.createNetServer(new NetServerOptions().setTcpKeepAlive(true))
        .connectHandler(socket -> {
          var id = UUID.randomUUID().toString();
          var json = new JsonObject().put("id", id);
          idSocketBiMap.put(id, socket);
          netSocketNicknameMap.put(socket, id);//先用id做昵称，避免出现无昵称的情况，避免客户端发送无昵称消息
          
          final var recordParser = RecordParser.newDelimited("\r\n", h -> {
            try {
              var messageJson = new JsonObject(h);
              messageJson.put("id", id);
              messageJson.put("time", ZonedDateTime.now().format(dateFormatter));

              if (null != messageJson.getValue("nickname")
                  && !messageJson.getValue("nickname").toString().isBlank()) {
                var nickname = messageJson.getValue("nickname").toString().trim();
                netSocketNicknameMap.put(socket, nickname);
                updateUsersList();
              }

              if (netSocketNicknameMap.containsKey(socket)) {
                messageJson.put("nickname", netSocketNicknameMap.get(socket));
              }

              if (messageJson.containsKey("message")) {
                sendToOtherUsers(messageJson);
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }).maxRecordSize(1024 * 64);

          socket.handler(recordParser);

          socket.write(json.toString() + "\r\n");
          socket.closeHandler((e) -> {
            idSocketBiMap.inverse().remove(socket);
            netSocketNicknameMap.remove(socket);
            updateUsersList();
          });
        })
        .listen(port, res -> {
          if (res.succeeded()) {
            System.out.println("listen to port " + port);
          } else {
            System.out.println("netserver start failed");
          }
        });
  }

  private void updateUsersList(){
    var jsonArrays = new JsonArray();
    for (var nn : netSocketNicknameMap.values()) {
      jsonArrays.add(nn);
    }
    publishMessage(new JsonObject().put("nicknames", jsonArrays));
  }

  private void publishMessage(JsonObject jsonMsg){
    for (var receiverSocket : idSocketBiMap.values()) {
      receiverSocket.write(jsonMsg + "\r\n");
    }
  }

  private void sendToOtherUsers(JsonObject jsonMsg) {
    var id = jsonMsg.getValue("id").toString();
    for (var receiverSocket : idSocketBiMap.values()) {
      if(receiverSocket != idSocketBiMap.get(id))
        receiverSocket.write(jsonMsg + "\r\n");
    }
  }
}
