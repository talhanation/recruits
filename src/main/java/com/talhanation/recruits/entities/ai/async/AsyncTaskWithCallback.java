package com.talhanation.recruits.entities.ai.async;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncTaskWithCallback<T> implements Runnable {
    private final Supplier<T> computation;
    private final Consumer<T> mainThreadCallback;
    private final ServerLevel serverLevel;
    public AsyncTaskWithCallback(Supplier<T> computation, Consumer<T> mainThreadCallback, ServerLevel serverLevel) {
        this.computation = computation;
        this.mainThreadCallback = mainThreadCallback;
        this.serverLevel = serverLevel;
    }

    @Override
    public void run() {
        T result = computation.get();

        this.serverLevel.getServer().execute( ()-> {
            mainThreadCallback.accept(result);
        });
    }
}
