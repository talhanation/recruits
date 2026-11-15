package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IHasTargetPriority;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;

public class MessageSetTargetPrio implements Message<MessageSetTargetPrio>, CompanionOrder<AbstractRecruitEntity> {

    private UUID recruit;
    private int state;

    public MessageSetTargetPrio() {
    }
    public MessageSetTargetPrio(UUID recruit, int state) {
        this.recruit = recruit;
        this.state = state;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        CompanionOrders.handle(context.getSender(), this);
    }

    @Override
    public UUID getCompanion() {
        return this.recruit;
    }

    @Override
    public Class<AbstractRecruitEntity> getCompanionType() {
        return AbstractRecruitEntity.class;
    }

    @Override
    public void applyTo(AbstractRecruitEntity companion, @Nullable ServerPlayer sender) {
        if (companion instanceof IHasTargetPriority specialRecruit) {
            specialRecruit.setTargetPriority(IHasTargetPriority.TargetPriority.fromIndex(state));
        }
    }
    public MessageSetTargetPrio fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(state);
    }
}
