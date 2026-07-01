package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.UUID;

public class MessageToClientWorldMapIdentity implements Message<MessageToClientWorldMapIdentity> {
    public static final CustomPacketPayload.Type<MessageToClientWorldMapIdentity> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientworldmapidentity"));
    private UUID worldId;

    public MessageToClientWorldMapIdentity() {
    }

    public MessageToClientWorldMapIdentity(UUID worldId) {
        this.worldId = worldId;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        if (worldId == null) return;

        WorldMapStorageId.setServerWorldId(worldId);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            WorldMapCacheManager.getInstance().initialize(mc.level);
        }
        ClientManager.loadRoutes();
        ClientManager.loadSpecialStates();
    }

    @Override
    public MessageToClientWorldMapIdentity fromBytes(RegistryFriendlyByteBuf buf) {
        this.worldId = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.worldId);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientWorldMapIdentity> type() {
        return TYPE;
    }
}