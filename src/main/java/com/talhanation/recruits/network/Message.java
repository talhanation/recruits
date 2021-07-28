package com.talhanation.recruits.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

public interface Message<T extends Message> {
    Dist getExecutingSide();

    default void executeServerSide(NetworkEvent.Context context) {}

    default void executeClientSide(NetworkEvent.Context context) {}

    T fromBytes(PacketBuffer paramPacketBuffer);

    void toBytes(PacketBuffer paramPacketBuffer);
}
