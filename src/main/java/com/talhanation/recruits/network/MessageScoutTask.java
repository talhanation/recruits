package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.ScoutEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageScoutTask implements Message<MessageScoutTask> {

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

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (this.state < ScoutEntity.State.IDLE.getIndex() || this.state > ScoutEntity.State.SEARCHING_LOST_RECRUITS.getIndex()) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16D, false)
                .filter(ScoutEntity.class::isInstance)
                .map(ScoutEntity.class::cast)
                .ifPresent((scout) -> scout.startTask(ScoutEntity.State.fromIndex(state)));
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
