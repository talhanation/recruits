package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.gui.faction.TakeOverScreen;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.UUID;
public class MessageToClientOpenTakeOverScreen implements Message<MessageToClientOpenTakeOverScreen> {

    public static final CustomPacketPayload.Type<MessageToClientOpenTakeOverScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientopentakeoverscreen"));
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
    public void executeClientSide(IPayloadContext context) {
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
    public MessageToClientOpenTakeOverScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientOpenTakeOverScreen> type() {
        return TYPE;
    }
}