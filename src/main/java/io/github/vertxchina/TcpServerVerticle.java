package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.vertxchina.Message.*;

public class TcpServerVerticle extends AbstractVerticle {
  Logger log = LoggerFactory.getLogger(TcpServerVerticle.class);
  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
  BiMap<String, NetSocket> idSocketBiMap = HashBiMap.create();
  Map<NetSocket, String> netSocketNicknameMap = new HashMap<>();

  @Override
  public void start() {
    Integer port = config().getInteger("TcpServer.port", 32167);
    vertx.createNetServer(new NetServerOptions().setTcpKeepAlive(true))
      .connectHandler(socket -> {
        var id = UUID.randomUUID().toString().replaceAll("-", "");
        new Message(CLIENT_ID_KEY, id).writeTo(socket); //先将id发回
        idSocketBiMap.put(id, socket);
        netSocketNicknameMap.put(socket, id);//先用id做昵称，避免出现无昵称的情况，避免客户端发送无昵称消息
        //todo 将来有了账户之后，改成登陆之后，再将历史记录发回
        vertx.setTimer(3000, t -> {
//          chatLog.storedMessages().forEach(m -> socket.write(m.toBuffer()));
        });
        socket.handler(RecordParser.newDelimited(Message.DELIM, h -> {
          System.out.println(h.toString());
          try {
            var message = new Message(h);
            message.initServerSide(id, ZonedDateTime.now().format(dateFormatter));

            if (message.hasNickName()) {
              netSocketNicknameMap.put(socket, message.nickName());
              updateUsersList();
            }

            if (netSocketNicknameMap.containsKey(socket)) {
              message.setNickName(netSocketNicknameMap.get(socket));
            }

            if (message.hasMessage()) {
              sendToOtherUsers(message);
//              chatLog.add(message);
            }
          } catch (Exception e) {
            new Message(MESSAGE_CONTENT_KEY, e.getMessage()).writeTo(socket);
          }
        }).maxRecordSize(1024 * 64));

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
