package io.github.vertxchina.webverticle;

import io.github.vertxchina.eventbus.Message;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

import java.util.List;
import java.util.UUID;

import static io.github.vertxchina.eventbus.EventbusAddress.PUBLISH_MESSAGE;
import static io.github.vertxchina.eventbus.EventbusAddress.READ_STORED_MESSAGES;
import static io.github.vertxchina.eventbus.Message.CLIENT_ID_KEY;
import static io.github.vertxchina.eventbus.Message.MESSAGE_CONTENT_KEY;

public class TcpServerVerticle extends AbstractVerticle {
  Logger log = LoggerFactory.getLogger(TcpServerVerticle.class);
  private final String VERTICLE_ID = UUID.randomUUID().toString();
  public static final String DELIM = "\r\n";
  SocketWriteHolder<NetSocket> socketHolder = new SocketWriteHolder<>(this::writeSocket);

  @Override
  public void start(Promise<Void> startPromise) {
    Integer port = config().getInteger("TcpServer.port", 32167);

    vertx.createNetServer(new NetServerOptions().setTcpKeepAlive(true))
      .connectHandler(socket -> {
        var id = SocketWriteHolder.generateClientId();
        log.info(id + " Connected TcpServer!");
        writeSocket(socket, new Message(CLIENT_ID_KEY, id));//先将id发回
        socketHolder.addSocket(id, socket);

        //TODO 将来有了账户之后，改成登陆之后，再将历史记录发回
        vertx.eventBus().<List<Message>>request(READ_STORED_MESSAGES, null, ar -> {
          if (ar.succeeded()) {
            vertx.setTimer(3000, t -> {
              ar.result().body().forEach(msg -> writeSocket(socket, msg));
            });
          }
        });

        socket
          .exceptionHandler(t -> socket.close())
          .closeHandler(v -> socketHolder.removeSocket(socket))
          .handler(
            RecordParser.newDelimited(DELIM)
              .maxRecordSize(1024 * 64)
              .exceptionHandler(throwable -> writeSocket(socket, new Message(MESSAGE_CONTENT_KEY, throwable.getMessage())))
              .handler(buffer -> {
                log.debug("Message raw content: " + buffer);
                try {
                  var json = buffer.toJsonObject();
                  var message = new Message(json).initServerSide(id, VERTICLE_ID);
                  socketHolder.receiveMessage(socket, message);
                  if (message.hasMessage()) {
                    socketHolder.sendToOtherUsers(message);
                    vertx.eventBus().publish(PUBLISH_MESSAGE, message);
                  }
                }catch (Exception e){
                  writeSocket(socket, new Message(MESSAGE_CONTENT_KEY, e.getMessage()));
                }
              })
          );
      })
      .listen(port)
      .onSuccess(s -> {
        log.info("TcpServerVerticle deployed with verticle ID: " + VERTICLE_ID);
        log.info("TcpServer listen to port: " + port);
        startPromise.complete();
      })
      .onFailure(e -> {
        log.error("TcpServer start failed: " + e.getMessage(), e);
        startPromise.fail(e);
      });
    vertx.eventBus()
      .<Message>consumer(PUBLISH_MESSAGE)
      .handler(message -> {
        Message tnbMsg = message.body();
        if (!tnbMsg.generator().equals(VERTICLE_ID)) {
          socketHolder.publishMessage(tnbMsg);
        }
      });

    vertx.exceptionHandler(e -> {
      e.printStackTrace();
    });
  }

  private void writeSocket(NetSocket socket, Message message) {
    socket.write(message.toBuffer().appendString(DELIM));
  }
}
