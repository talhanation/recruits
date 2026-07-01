package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.gui.NobleTradeScreen;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.UUID;

public class MessageToClientOpenNobleTradeScreen implements Message<MessageToClientOpenNobleTradeScreen> {

    public static final CustomPacketPayload.Type<MessageToClientOpenNobleTradeScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientopennobletradescreen"));
    private UUID recruit_uuid;

    public MessageToClientOpenNobleTradeScreen() {

    }

    public MessageToClientOpenNobleTradeScreen(UUID recruit_uuid) {
        this.recruit_uuid = recruit_uuid;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        Player player = Minecraft.getInstance().player;
        player.getCommandSenderWorld().getEntitiesOfClass(VillagerNobleEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.recruit_uuid))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(nobleVillager -> Minecraft.getInstance().setScreen(new NobleTradeScreen(nobleVillager, player)));
    }

    @Override
    public MessageToClientOpenNobleTradeScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit_uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit_uuid);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientOpenNobleTradeScreen> type() {
        return TYPE;
    }
}