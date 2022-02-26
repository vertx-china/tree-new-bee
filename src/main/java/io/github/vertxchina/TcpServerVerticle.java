package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

import java.util.UUID;

public class TcpServerVerticle extends AbstractVerticle {
  BiMap<String, NetSocket> idSocketBiMap = HashBiMap.create();

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
              for (var receiverSocket : idSocketBiMap.values()) {
                receiverSocket.write(messageJson + "\r\n");
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }).maxRecordSize(1024 * 64);

          socket.handler(recordParser);

          socket.write(json.toString() + "\r\n");
          socket.closeHandler((e) -> idSocketBiMap.inverse().remove(socket));
        })
        .listen(port, res -> {
          if (res.succeeded()) {
            System.out.println("listen to port " + port);
          } else {
            System.out.println("netserver start failed");
          }
        });
  }
}
