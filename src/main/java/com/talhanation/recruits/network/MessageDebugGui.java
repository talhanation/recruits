package com.talhanation.recruits.network;

import com.talhanation.recruits.DebugEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageDebugGui implements Message<MessageDebugGui> {

    private int id;
    private UUID uuid;

    public MessageDebugGui() {
    }

    public MessageDebugGui(int id, UUID uuid) {
        this.id = id;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractRecruitEntity recruits : list) {

            if (recruits.getUUID().equals(this.uuid))
                DebugEvents.handleMessage(id, recruits);
        }

    }

    public MessageDebugGui fromBytes(FriendlyByteBuf buf) {
        this.id = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUUID(uuid);
    }
}
