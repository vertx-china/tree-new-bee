package io.github.vertxchina;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static io.github.vertxchina.Message.CLIENT_ID_KEY;
import static io.github.vertxchina.Message.MESSAGE_CONTENT_KEY;

public class TcpServerVerticle extends AbstractVerticle {
  Logger log = LoggerFactory.getLogger(TcpServerVerticle.class);
  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
  SocketWriteHolder<NetSocket> socketHolder = new SocketWriteHolder<>();

  @Override
  public void start() {
    Integer port = config().getInteger("TcpServer.port", 32167);

    vertx.createNetServer(new NetServerOptions().setTcpKeepAlive(true))
      .connectHandler(socket -> {
        var id = SocketWriteHolder.generateClientId();
        new Message(CLIENT_ID_KEY, id).writeTo(socket); //先将id发回
        socketHolder.addSocket(id, socket);
        //todo 将来有了账户之后，改成登陆之后，再将历史记录发回
        vertx.setTimer(3000, t -> {
//          chatLog.storedMessages().forEach(m -> socket.write(m.toBuffer()));
        });
        socket.handler(RecordParser.newDelimited(Message.DELIM, h -> {
          System.out.println(h.toString());
          try {
            var message = new Message(h);
            message.initServerSide(id, ZonedDateTime.now().format(dateFormatter));
            socketHolder.receiveMessage(socket, message);

            if (message.hasMessage()) {
              socketHolder.sendToOtherUsers(message);
//              chatLog.add(message);
            }
          } catch (Exception e) {
            new Message(MESSAGE_CONTENT_KEY, e.getMessage()).writeTo(socket);
          }
        }).maxRecordSize(1024 * 64));

        socket.closeHandler((e) -> socketHolder.removeSocket(socket));
      })
      .listen(port)
      .onSuccess(s -> System.out.println("listen to port " + port))
      .onFailure(e -> {
        System.out.println("TcpServer start failed");
        e.printStackTrace();
      });
  }
}
