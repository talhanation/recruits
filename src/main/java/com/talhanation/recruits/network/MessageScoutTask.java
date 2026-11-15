package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.ScoutEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;

public class MessageScoutTask implements Message<MessageScoutTask>, CompanionOrder<ScoutEntity> {

    private UUID recruit;
    private int state;

    public MessageScoutTask() {
    }
    public MessageScoutTask(UUID recruit, int state) {
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
    public Class<ScoutEntity> getCompanionType() {
        return ScoutEntity.class;
    }

    @Override
    public boolean needsWorld() {
        return true;
    }

    @Override
    public void applyTo(ScoutEntity scout, @Nullable ServerPlayer sender) {
        scout.startTask(ScoutEntity.State.fromIndex(state));
    }
    public MessageScoutTask fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(state);
    }
}
