package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.async.EntityCache;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenPromoteScreen implements Message<MessageOpenPromoteScreen> {

    private UUID player;
    private UUID recruit;

    public MessageOpenPromoteScreen() {
        this.player = new UUID(0, 0);
    }

    public MessageOpenPromoteScreen(Player player, UUID recruit) {
        this.player = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (!player.getUUID().equals(this.player)) {
            return;
        }
        EntityCache.withLevel(player.getLevel()).getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(16.0D),
                v -> v.getUUID().equals(this.recruit)
        ).stream().filter(Entity::isAlive).findAny().ifPresent(recruit ->  RecruitEvents.openPromoteScreen(player, recruit));
    }

    @Override
    public MessageOpenPromoteScreen fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.recruit= buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(player);
        buf.writeUUID(recruit);
    }

}