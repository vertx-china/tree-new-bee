package io.github.vertxchina;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class FSMessageBuffer extends MessageBuffer {
    public static final String TEMP_STORE_PATH = System.getProperty("java.io.tmpdir") + "tnb-msg.json";

    public FSMessageBuffer(Vertx vertx, int bufferSize) {
        super(vertx, bufferSize);
    }

    @Override
    protected void fromPersisted() {
        var fs = vertx.fileSystem();
        fs.exists(TEMP_STORE_PATH)
                .compose(it -> it ? Future.succeededFuture() : fs.createFile(TEMP_STORE_PATH))
                .compose(it -> fs.readFile(TEMP_STORE_PATH))
                .map(Buffer::toJsonArray)
                .map(msgList -> msgList.stream().map(JsonObject::mapFrom).collect(Collectors.toList()))
                .onSuccess(it -> {
                    if (it != null && !it.isEmpty()) it.forEach(this::add);
                })
                .onFailure(it -> System.out.println(it));
    }

    @Override
    protected void persist() {
        List<JsonObject> msgList = storedMessages();
        if (msgList.isEmpty()) return;
        vertx.fileSystem().writeFile(TEMP_STORE_PATH, Json.encodeToBuffer(msgList));
    }

    @Override
    protected boolean ensurePersist() {
        return true;
    }
}
