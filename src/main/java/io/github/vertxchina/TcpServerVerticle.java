package io.github.vertxchina;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class TcpServerVerticle extends AbstractVerticle {
  BiMap<String, NetSocket> idSocketBiMap = HashBiMap.create();
  @Override
  public void start() throws Exception {
    super.start();

    var netserver = vertx.createNetServer()
        .connectHandler(socket ->{

        })
        .listen(32167, res ->{
          if(res.succeeded()){
            System.out.println("listen to port 32167");
          }else{
            System.out.println("netserver start failed");
          }
        });
  }

  @Override
  public void stop() throws Exception {
    super.stop();
  }
}
