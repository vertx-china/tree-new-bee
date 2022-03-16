package io.github.vertxchina.webverticle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.vertxchina.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.streams.WriteStream;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.vertxchina.Message.NICKNAME_KEY;

/**
 * @author Leibniz on 2022/03/10 1:52 PM
 */
public class SocketWriteHolder<S extends WriteStream<Buffer>> {
  BiMap<String, S> idSocketBiMap = HashBiMap.create();
  Map<S, String> netSocketNicknameMap = new HashMap<>();
  SocketWriter<S> socketWriter;

  public SocketWriteHolder(SocketWriter<S> socketWriter) {
    this.socketWriter = socketWriter;
  }

  static String generateClientId(){
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  void addSocket(String id, S socket) {
    idSocketBiMap.put(id, socket);
    netSocketNicknameMap.put(socket, id);//先用id做昵称，避免出现无昵称的情况，避免客户端发送无昵称消息
  }

  void removeSocket(S socket) {
    idSocketBiMap.inverse().remove(socket);
    netSocketNicknameMap.remove(socket);
    updateUsersList();
  }

  void receiveMessage(S socket, Message message) {
    if (message.hasNickName()) {
      if(!netSocketNicknameMap.containsKey(socket) ||
        !netSocketNicknameMap.get(socket).equals(message.nickName())){//只有昵称与存储昵称不同时候，才需要更新
        netSocketNicknameMap.put(socket, message.nickName());
        updateUsersList();
      }
    }

    if (netSocketNicknameMap.containsKey(socket)) {//添加昵称
      message.setNickName(netSocketNicknameMap.get(socket));
    }
  }

  void updateUsersList() {
    var jsonArrays = new JsonArray();
    netSocketNicknameMap.values().forEach(jsonArrays::add);
    publishMessage(new Message(NICKNAME_KEY+"s", jsonArrays));//复数，nickname表示当前用户昵称，复数表示用户列表，后续可能去掉这个功能，因为支持的客户端太少
  }

  void publishMessage(Message msg) {
    idSocketBiMap.values().forEach(socket -> {
      socketWriter.write(socket,msg);
    });
  }

  void sendToOtherUsers(Message msg) {
    var id = msg.messageId();
    for (var receiverSocket : idSocketBiMap.values()) {
      if(receiverSocket != idSocketBiMap.get(id))
        socketWriter.write(receiverSocket, msg);
    }
  }
}
