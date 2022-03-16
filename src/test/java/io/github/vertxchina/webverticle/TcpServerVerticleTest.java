package io.github.vertxchina.webverticle;

import io.github.vertxchina.eventbus.Message;
import io.github.vertxchina.eventbus.TnbMessageCodec;
import io.github.vertxchina.persistverticle.MessageStoreVerticle;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.vertxchina.webverticle.TcpServerVerticle.DELIM;

/**
 * @author Leibniz on 2022/02/25 10:27 PM
 */
@ExtendWith(VertxExtension.class)
public class TcpServerVerticleTest {
  static Logger log = LoggerFactory.getLogger(TcpServerVerticleTest.class);
  
  @BeforeEach
  void init(Vertx vertx) {
    vertx.eventBus()
      .registerDefaultCodec(Message.class, new TnbMessageCodec());
  }

  @Test
  void singleClientSendMessageTest(Vertx vertx, VertxTestContext testCtx) throws Throwable {
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() Start");
    int port = 9527;
    JsonObject config = new JsonObject().put("TcpServer.port", port);
    vertx.deployVerticle(TcpServerVerticle.class, new DeploymentOptions().setConfig(config))
      .compose(did -> createClients(vertx, port, 1))
      .compose(cf -> sendMessages(vertx, cf, 1).get(0))
      .onSuccess(client -> {
        assert client.receivedMsgList.size() == 0; //登陆后响应消息不包含有message域，不统计在内，自身发出的消息不再发回来
        testCtx.completeNow();
      })
      .onFailure(testCtx::failNow);

    assert testCtx.awaitCompletion(10, TimeUnit.SECONDS);
    if (testCtx.failed()) {
      throw testCtx.causeOfFailure();
    }
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() End");
  }

  @Test
  void multiClientSendMessageMutuallyTest(Vertx vertx, VertxTestContext testCtx) throws Throwable {
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() Start");
    int port = 6666;
    int clientNum = 3;
    JsonObject config = new JsonObject().put("TcpServer.port", port);
    vertx.deployVerticle(TcpServerVerticle.class, new DeploymentOptions().setConfig(config))
      .compose(did -> createClients(vertx, port, clientNum))
      .compose(cf -> CompositeFuture.all(cast(sendMessages(vertx, cf, 1))))
      .onSuccess(closed -> {
        for (Object o : closed.result().list()) {
          TreeNewBeeClient client = (TreeNewBeeClient) o;
          log.info("client " + client.id + " send " + client.sendMsgList.size() + " message(s), received " + client.receivedMsgList.size() + " messages");
          assert client.receivedMsgList.size() == clientNum - 1; //N条其他Clients发出的消息，仅保留有消息（message字段）的消息，登陆后的响应和退出消息不保留，另外自身发送的消息不再返回
        }
        testCtx.completeNow();
      })
      .onFailure(testCtx::failNow);

    assert testCtx.awaitCompletion(10, TimeUnit.SECONDS);
    if (testCtx.failed()) {
      throw testCtx.causeOfFailure();
    }
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() End");
  }

  @Test
  void singleClientSendMessageErrorTest(Vertx vertx, VertxTestContext testCtx) throws Throwable {
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() Start");
    int port = 9527;
    JsonObject config = new JsonObject().put("TcpServer.port", port);
    vertx.deployVerticle(TcpServerVerticle.class, new DeploymentOptions().setConfig(config))
      .compose(did -> createClients(vertx, port, 1))
      .compose(ar -> sendErrorMessages(vertx, ar).get(0))
      .onSuccess(msgList -> {
        assert msgList.size() == 1; //发送错误消息应返回解析错误消息
        var stacktrace = msgList.get(0);
        log.info(stacktrace);
        testCtx.completeNow();
      })
      .onFailure(testCtx::failNow);

    assert testCtx.awaitCompletion(10, TimeUnit.SECONDS);
    if (testCtx.failed()) {
      throw testCtx.causeOfFailure();
    }
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() End");
  }

  @Test
  void chatLogSendTest(Vertx vertx, VertxTestContext testCtx) throws Throwable {
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() Start");
    int port = 9527;
    int prevClientSendMsgNum = 10;
    int chatLogSize = 5;
    JsonObject config = new JsonObject().put("TcpServer.port", port).put("MessageStore.chatLogSize", chatLogSize);
    DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config);
    vertx
      .deployVerticle(MessageStoreVerticle.class, deploymentOptions)
      .compose(did -> vertx.deployVerticle(TcpServerVerticle.class, deploymentOptions))
      .compose(did -> createClients(vertx, port, 1))
      .compose(cf -> sendMessages(vertx, cf, prevClientSendMsgNum).get(0))
      .compose(client -> {
        log.info("First client send " + client.sendMsgList.size() + " message(s), received " + client.receivedMsgList.size() + " messages");
        assert client.sendMsgList.size() == prevClientSendMsgNum;
        assert client.receivedMsgList.size() <= chatLogSize;
        return createClients(vertx, port, 1);
      })
      .compose(cf -> sendMessages(vertx, cf, 1).get(0))
      .onSuccess(client -> {
        log.info("Second client send " + client.sendMsgList.size() + " message(s), received " + client.receivedMsgList.size() + " messages");
        assert client.receivedMsgList.size() == Math.min(prevClientSendMsgNum, chatLogSize); //服务器只保留最近chatLogSize条记录
        testCtx.completeNow();
      })
      .onFailure(testCtx::failNow);

    assert testCtx.awaitCompletion(10, TimeUnit.SECONDS);
    if (testCtx.failed()) {
      throw testCtx.causeOfFailure();
    }
    log.info("====> " + Thread.currentThread().getStackTrace()[1].getMethodName() + "() End");
  }

  private List<Future<List<String>>> sendErrorMessages(Vertx vertx, CompositeFuture ar) {
    List<Future<List<String>>> closeFutures = new ArrayList<>();
    for (Object o : ar.list()) {
      if (o instanceof TreeNewBeeClient client) {
        var socket = client.socket;
        socket.write("fjdslkjlfa\r\n");
        Promise<List<String>> promise = Promise.promise();
        closeFutures.add(promise.future());
        socket.closeHandler(v -> {
          log.info("Client " + socket + " closed");
          promise.complete(client.receivedMsgList);
        });
        vertx.setTimer(1000L, tid -> socket.close());
      }
    }
    return closeFutures;
  }

  private List<Future<TreeNewBeeClient>> sendMessages(Vertx vertx, CompositeFuture ar, int msgNum) {
    List<Future<TreeNewBeeClient>> closeFutures = new ArrayList<>();
    for (Object o : ar.list()) {
      if (o instanceof TreeNewBeeClient client) {
        var clientId = client.id;
        var socket = client.socket;
        for (int i = 0; i < msgNum; i++) {
          client.sendMsg("Hello gays! I'm client " + clientId);
        }
        Promise<TreeNewBeeClient> promise = Promise.promise();
        closeFutures.add(promise.future());
        socket.closeHandler(v -> {
          log.info("Client " + socket + " closed");
          promise.complete(client);
        });
        vertx.setTimer(4000L, tid -> socket.close());
      }
    }
    return closeFutures;
  }

  private final AtomicInteger clientIdCnt = new AtomicInteger(0);

  @SuppressWarnings("rawtypes")
  private CompositeFuture createClients(Vertx vertx, int port, int num) {
    List<Future> createClientFutures = new ArrayList<>();
    for (int i = 0; i < num; i++) {
      var clientId = clientIdCnt.addAndGet(1);
      createClientFutures.add(
        vertx.createNetClient()
          .connect(port, "localhost")
          .map(s -> new TreeNewBeeClient(s, clientId))
          .onFailure(e -> log.info("Failed to connect: " + e.getMessage()))
      );
    }
    return CompositeFuture.all(createClientFutures);
  }

  static class TreeNewBeeClient {
    List<String> receivedMsgList = new ArrayList<>();
    List<String> sendMsgList = new ArrayList<>();
    final NetSocket socket;
    final int id;

    public TreeNewBeeClient(NetSocket socket, int id) {
      this.socket = socket;
      this.id = id;
      this.socket.handler(RecordParser.newDelimited(DELIM, h -> {
          try {
            JsonObject msg = new JsonObject(h);
            log.info("Client " + id + " Received message: " + msg);
            if (msg.containsKey("message")) {
              receivedMsgList.add(msg.getString("message"));
            }
          } catch (Exception e) {
            log.error("Client " + id + " parse message err: " + e.getMessage() + "original message:" + h.toString());
          }
        })
        .maxRecordSize(1024 * 64));
      log.info("Client " + id + " Connected!");
    }


    void sendMsg(String msg) {
      socket.write(new JsonObject()
        .put("time", System.currentTimeMillis())
        .put("message", msg)
        .put("fromClientId", id).toString() + "\r\n");
      sendMsgList.add(msg);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T cast(Object obj) {
    return (T) obj;
  }
}