package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageToClientWorldMapIdentity implements Message<MessageToClientWorldMapIdentity> {
    private UUID worldId;

    public MessageToClientWorldMapIdentity() {
    }

    public MessageToClientWorldMapIdentity(UUID worldId) {
        this.worldId = worldId;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
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
    public MessageToClientWorldMapIdentity fromBytes(FriendlyByteBuf buf) {
        this.worldId = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worldId);
    }
}