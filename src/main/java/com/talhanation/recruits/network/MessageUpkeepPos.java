package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageUpkeepPos implements Message<MessageUpkeepPos> {

    private UUID player;
    private UUID recruit;
    public MessageUpkeepPos(){
    }

    public MessageUpkeepPos(UUID player, UUID recruit) {
        this.player = player;
        this.recruit = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        List<AbstractRecruitEntity> list = player.level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractRecruitEntity recruits : list) {

            if (recruits.getUUID().equals(this.recruit))
                CommandEvents.setRecruitUpkeep(recruits, player.getOnPos());
        }

        Main.LOGGER.debug("new UpkeepPos: " + player.getOnPos());
    }
    public MessageUpkeepPos fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.recruit);
    }

}