package io.github.vertxchina;

import io.vertx.core.AbstractVerticle;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Leibniz on 2022/3/3 8:18 AM
 */
public class MessageStoreVerticle extends AbstractVerticle {
    private final Deque<Message> fifo = new LinkedList<>();
    private int bufferSize;


    @Override
    public void start() {
        this.bufferSize = config().getInteger("MessageStore.chatLogSize", 30);
        fromPersisted();
    }


    public void add(Message msg) {
        fifo.addLast(msg);
        while (fifo.size() > bufferSize) {
            fifo.removeFirst();
        }
        if (ensurePersist()) {
            persist();
        }
    }

    public List<Message> storedMessages(){
        return new ArrayList<>(fifo);
    }

    private void fromPersisted() {
        //TODO
    }

    private void persist() {
        //TODO
    }

    private boolean ensurePersist() {
        //TODO
        return false;
    }
}
