package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.DebugEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageDebugScreen implements Message<MessageDebugScreen> {
    private UUID uuid;
    private UUID recruit;


    public MessageDebugScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageDebugScreen(Player player, UUID recruit) {
        this.uuid = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        if (!context.getSender().getUUID().equals(uuid)) {
            return;
        }

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.recruit))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(recruit -> recruit.openDebugScreen(player));
    }

    @Override
    public MessageDebugScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(recruit);
    }
}