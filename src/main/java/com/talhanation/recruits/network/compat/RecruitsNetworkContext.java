package com.talhanation.recruits.network.compat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

public final class RecruitsNetworkContext {
    private final IPayloadContext delegate;

    public RecruitsNetworkContext(IPayloadContext delegate) {
        this.delegate = delegate;
    }

    public ServerPlayer getSender() {
        return delegate.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    public CompletableFuture<Void> enqueueWork(Runnable runnable) {
        return delegate.enqueueWork(runnable);
    }
}
