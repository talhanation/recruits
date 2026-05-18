package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.faction.TakeOverScreen;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.UUID;
public class MessageToClientOpenTakeOverScreen implements RecruitsMessage<MessageToClientOpenTakeOverScreen> {

    private UUID recruit;

    public MessageToClientOpenTakeOverScreen() {

    }

    public MessageToClientOpenTakeOverScreen(UUID recruit) {
        this.recruit = recruit;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(RecruitsNetworkContext context) {
        Player player = Minecraft.getInstance().player;
        player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.recruit))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(recruit -> Minecraft.getInstance().setScreen(new TakeOverScreen(recruit, player)));
    }

    @Override
    public MessageToClientOpenTakeOverScreen fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
    }
}