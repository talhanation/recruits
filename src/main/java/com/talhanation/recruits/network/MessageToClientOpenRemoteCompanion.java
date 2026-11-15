package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.CompanionListScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Server → Client: the draft for this companion is open. Carries his entity type and synced data, so the client
 * can show his normal screen although he is far away ({@link CompanionListScreen#openOrders}). For a Governor
 * it carries the snapshot of his claim instead, as his screen shows the claim.
 */
public class MessageToClientOpenRemoteCompanion implements Message<MessageToClientOpenRemoteCompanion> {
    private static final int MAX_SYNCED_BYTES = 32767;
    private UUID companion;
    private ResourceLocation type;
    private byte[] synced;
    @Nullable
    private CompoundTag governor;

    public MessageToClientOpenRemoteCompanion() {
    }

    public MessageToClientOpenRemoteCompanion(UUID companion, ResourceLocation type, byte[] synced, @Nullable CompoundTag governor) {
        this.companion = companion;
        this.type = type;
        this.synced = synced.length > MAX_SYNCED_BYTES ? new byte[0] : synced;
        this.governor = governor;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        if (Minecraft.getInstance().screen instanceof CompanionListScreen screen) screen.openOrders(this.companion, this.type, this.synced, this.governor);
    }

    @Override
    public MessageToClientOpenRemoteCompanion fromBytes(FriendlyByteBuf buf) {
        this.companion = buf.readUUID();
        this.type = buf.readResourceLocation();
        this.synced = buf.readByteArray(MAX_SYNCED_BYTES);
        this.governor = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.companion);
        buf.writeResourceLocation(this.type);
        buf.writeByteArray(this.synced);
        buf.writeNbt(this.governor);
    }
}
