package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageFollowGui implements Message<MessageFollowGui> {

    private int state;
    private UUID uuid;

    public MessageFollowGui() {
    }

    public MessageFollowGui(int state, UUID uuid) {
        this.state = state;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));

        for (AbstractRecruitEntity recruit : list) {
            if (recruit.getUUID().equals(this.uuid) && recruit.isEffectedByCommand(context.getSender().getUUID(), 0)){
                CommandEvents.onMovementCommandGUI(recruit, this.state);
            }
        }
    }

    public MessageFollowGui fromBytes(FriendlyByteBuf buf) {
        this.state = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(state);
        buf.writeUUID(uuid);
    }
}
