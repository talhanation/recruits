package com.talhanation.recruits.network;

import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageHold implements Message<MessageHold> {

    private UUID player;

    public MessageHold(){
    }

    public MessageHold(UUID player) {
        this.player = player;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        context.getSender().sendMessage(new StringTextComponent("MESSAGE_IN"), context.getSender().getUUID());
        List<AbstractRecruitEntity> list = context.getSender().level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(40.0D));
        for (AbstractRecruitEntity recruits : list) {
                KeyEvents.onRKeyPressed(this.player, recruits);
                context.getSender().sendMessage(new StringTextComponent("MESSAGE_DONE"), context.getSender().getUUID());


/*
            player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                            .inflate(20.0D), v -> v
                            .getUUID()
                            .equals(this.recruit))
                    .stream()
                    .filter(Entity::isAlive)
                    .findAny();
            //.ifPresent(recruit -> KeyEvents.holdRecruit(recruit, player));
*/
        }
    }
    public MessageHold fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
    }

}