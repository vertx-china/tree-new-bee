package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  public void start() throws Exception {
      Integer port = config().getInteger("TcpServerVerticle.port", 32167);
      vertx.createNetServer()
        .connectHandler(socket -> {
          var id = UUID.randomUUID().toString();
          var json = new JsonObject().put("id", id);
          idSocketBiMap.put(id, socket);

          final var recordParser = RecordParser.newDelimited("\r\n", h -> {
            try {
              var messageJson = new JsonObject(h);
              messageJson.put("id", id);
              messageJson.put("time", ZonedDateTime.now().format(dateFormatter));

              if(null != messageJson.getValue("nickname")
                  && !messageJson.getValue("nickname").toString().isBlank()){
                var nickname = messageJson.getValue("nickname").toString().trim();
                netSocketNicknameMap.put(socket, nickname);
                var jsonArrays = new JsonArray();
                for(var nn:netSocketNicknameMap.values()){
                  jsonArrays.add(nn);
                }
                publishSpeak(new JsonObject().put("nicknames",jsonArrays));
              }

              if(netSocketNicknameMap.containsKey(socket)){
                messageJson.put("nickname",netSocketNicknameMap.get(socket));
              }

              if(messageJson.containsKey("message")){
                publishSpeak(messageJson);
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

  private void publishSpeak(JsonObject jsonMsg){
    for (var receiverSocket : idSocketBiMap.values()) {
      receiverSocket.write(jsonMsg + "\r\n");
    }
  }
}
