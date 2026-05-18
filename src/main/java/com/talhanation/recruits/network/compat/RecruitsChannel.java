package com.talhanation.recruits.network.compat;

import de.maxhenkel.corelib.net.Message;

public final class RecruitsChannel {
    public void sendToServer(Message<?> message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
    }

    public void send(RecruitsPacketDistributor.Target target, Message<?> message) {
        target.send(message);
    }
}
