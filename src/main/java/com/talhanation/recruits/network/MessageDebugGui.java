package com.talhanation.recruits.network;

import com.talhanation.recruits.DebugEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
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
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractRecruitEntity recruit : list) {

            if (recruit.getUUID().equals(this.uuid)){
                DebugEvents.handleMessage(id, recruit);
                recruit.setCustomName(new TextComponent(name));
            }
        }

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
