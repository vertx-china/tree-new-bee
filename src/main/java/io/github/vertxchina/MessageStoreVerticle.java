package io.github.vertxchina;

import io.github.vertxchina.codec.TnbMessageListCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static io.github.vertxchina.EventbusAddress.PUBLISH_MESSAGE;
import static io.github.vertxchina.EventbusAddress.READ_STORED_MESSAGES;

/**
 * @author Leibniz on 2022/3/3 8:18 AM
 */
public class MessageStoreVerticle extends AbstractVerticle {
    public static final String PROTOCOL = "CHAT_LOG";
    private final Deque<Message> fifo = new LinkedList<>();
    private int bufferSize;


    @Override
    public void start() {
        this.bufferSize = config().getInteger("MessageStore.chatLogSize", 30);
        fromPersisted();
        vertx.eventBus().<Message>consumer(PUBLISH_MESSAGE).handler(message -> add(message.body()));
        vertx.eventBus().registerCodec(new TnbMessageListCodec());
        DeliveryOptions deliveryOptions = new DeliveryOptions().setCodecName("tnblistmessage");
        vertx.eventBus().<Message>consumer(READ_STORED_MESSAGES)
          .handler(message -> message.reply(storedMessages(), deliveryOptions));
    }


    public void add(Message msg) {
        fifo.addLast(msg);
        while (fifo.size() > bufferSize) {
            fifo.removeFirst();
        }
        persist();
    }

    public List<Message> storedMessages(){
        return new ArrayList<>(fifo);
    }

    private void fromPersisted() {
        //TODO
    }

    private void persist() {
        if (ensurePersist()) {
            //TODO doPersist
        }
    }

    private boolean ensurePersist() {
        //TODO
        return false;
    }
}
