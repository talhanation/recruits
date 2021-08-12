package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;

public class MessageHold implements Message<MessageHold> {

    private UUID recruit;

    public MessageHold(){
    }

    public MessageHold(UUID recruit) {
        this.recruit = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                        .inflate(20.0D), v -> v
                        .getUUID()
                        .equals(this.recruit))
                .stream()
                .filter(Entity::isAlive)
                .findAny();
                //.ifPresent(recruit -> KeyEvents.holdRecruit(recruit, player));

    }

    public MessageHold fromBytes(PacketBuffer buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.recruit);
    }

}