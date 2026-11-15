package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.CaptainEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Home harbour orders from the captain screen: set it where his ship lies now,
 * or sail back to it. The player is the sender, not a uuid from the packet.
 */
public class MessageCaptainHarbour implements Message<MessageCaptainHarbour>, CompanionOrder<CaptainEntity> {

    private UUID captain;
    private boolean returnHome;

    public MessageCaptainHarbour() {
    }

    public MessageCaptainHarbour(UUID captain, boolean returnHome) {
        this.captain = captain;
        this.returnHome = returnHome;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        CompanionOrders.handle(context.getSender(), this);
    }

    @Override
    public UUID getCompanion() {
        return this.captain;
    }

    @Override
    public Class<CaptainEntity> getCompanionType() {
        return CaptainEntity.class;
    }

    @Override
    public boolean needsWorld() {
        return true;
    }

    @Override
    public void applyTo(CaptainEntity captain, @Nullable ServerPlayer sender) {
        ServerPlayer player = CompanionOrders.playerFor(captain, sender);
        if (this.returnHome) CommandEvents.onReturnHarbourCommand(player, captain);
        else CommandEvents.onHarbourCommand(player, captain);
    }

    public MessageCaptainHarbour fromBytes(FriendlyByteBuf buf) {
        this.captain = buf.readUUID();
        this.returnHome = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.captain);
        buf.writeBoolean(this.returnHome);
    }
}
