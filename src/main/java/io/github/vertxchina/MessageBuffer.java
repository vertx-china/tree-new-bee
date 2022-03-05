package io.github.vertxchina;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Leibniz on 2022/3/3 8:18 AM
 */
public class MessageBuffer {
    private final Deque<JsonObject> fifo = new LinkedList<>();
    protected final Vertx vertx;
    protected final int bufferSize;

    public MessageBuffer(Vertx vertx, int bufferSize) {
        this.vertx = vertx;
        this.bufferSize = bufferSize;
        fromPersisted();
    }

    public void add(JsonObject msg) {
        fifo.addLast(msg);
        while (fifo.size() > bufferSize) {
            fifo.removeFirst();
        }
        if (ensurePersist()) {
            persist();
        }
    }

    public List<JsonObject> storedMessages(){
        return new ArrayList<>(fifo);
    }

    protected void fromPersisted() {
        //TODO
    }

    protected void persist() {
        //TODO
    }

    protected boolean ensurePersist() {
        //TODO
        return false;
    }
}
