package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageUpkeepPos implements Message<MessageUpkeepPos> {

    private UUID player;
    private int group;
    private BlockPos pos;

    public MessageUpkeepPos(){
    }

    public MessageUpkeepPos(UUID player, int group, BlockPos pos) {
        this.player = player;
        this.group = group;
        this.pos = pos;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(100));
        for (AbstractRecruitEntity recruits : list) {
            CommandEvents.onUpkeepCommand(this.player, recruits, group, false, null, pos);
        }
    }
    public MessageUpkeepPos fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readInt();
        this.pos = buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.group);
        buf.writeBlockPos(this.pos);
    }

}