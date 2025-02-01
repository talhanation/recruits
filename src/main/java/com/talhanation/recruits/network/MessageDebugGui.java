package com.talhanation.recruits.network;

import com.talhanation.recruits.DebugEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageDebugGui implements Message<MessageDebugGui> {

    private int id;
    private UUID uuid;
    private String name;

    public MessageDebugGui() {
    }

    public MessageDebugGui(int id, UUID uuid, String name) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                context.getSender().getBoundingBox().inflate(16.0D),
                (recruit) -> recruit.getUUID().equals(this.uuid)
        ).forEach((recruit) -> {
            DebugEvents.handleMessage(id, recruit, context.getSender());
            recruit.setCustomName(Component.literal(name));
        });
    }

    public MessageDebugGui fromBytes(FriendlyByteBuf buf) {
        this.id = buf.readInt();
        this.uuid = buf.readUUID();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUUID(uuid);
        buf.writeUtf(name);
    }
}
