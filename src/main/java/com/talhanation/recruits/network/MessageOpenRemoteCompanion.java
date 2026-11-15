package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.governor.GovernorRemoteOrders;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.world.CompanionRecord;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Client → Server: the player wants to write orders for one of his far companions at his Messenger. The server
 * opens a draft for them ({@link CompanionOrders#beginDraft}) and answers with what the companion's screen needs
 * ({@link MessageToClientOpenRemoteCompanion}).
 */
public class MessageOpenRemoteCompanion implements Message<MessageOpenRemoteCompanion> {
    private UUID messenger;
    private UUID companion;

    public MessageOpenRemoteCompanion() {
    }

    public MessageOpenRemoteCompanion(UUID messenger, UUID companion) {
        this.messenger = messenger;
        this.companion = companion;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        CompanionRecord record = CompanionOrders.beginDraft(player, this.messenger, this.companion);
        if (record == null) return;

        // a Governor's screen shows his claim, not him: it gets a copy of the claim's data to work on
        CompoundTag governor = null;
        if (record.type.equals(ModEntityTypes.GOVERNOR.getId())) {
            GovernorRemoteOrders.Draft draft = CompanionOrders.beginGovernorDraft(player, this.companion);
            governor = draft == null ? null : GovernorRemoteOrders.snapshot(player, draft);
            if (governor == null) {
                player.sendSystemMessage(Component.translatable("chat.recruits.text.messenger_orders_no_office", record.name));
                CompanionOrders.endDraft(player, this.messenger, false);
                return;
            }
        }

        // in the world he is asked himself, otherwise the register knows how he looked when he was last seen
        byte[] synced = record.synced;
        if (player.serverLevel().getEntity(this.companion) instanceof AbstractRecruitEntity entity && entity.isAlive()) {
            synced = CompanionOrders.packSynced(entity);
        }
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MessageToClientOpenRemoteCompanion(record.uuid, record.type, synced, governor));
    }

    public MessageOpenRemoteCompanion fromBytes(FriendlyByteBuf buf) {
        this.messenger = buf.readUUID();
        this.companion = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.messenger);
        buf.writeUUID(this.companion);
    }
}
