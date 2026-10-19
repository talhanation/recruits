package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.CaptainEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Home harbour orders from the captain screen: set it where his ship lies now,
 * or sail back to it. The player is the sender, not a uuid from the packet.
 */
public class MessageCaptainHarbour implements Message<MessageCaptainHarbour> {

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
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(CaptainEntity.class, player.getBoundingBox().inflate(100.0D),
                captain -> captain.getUUID().equals(this.captain) && NetworkSecurity.canControl(player, captain)
        ).forEach(captain -> {
            if (this.returnHome) CommandEvents.onReturnHarbourCommand(player, captain);
            else CommandEvents.onHarbourCommand(player, captain);
        });
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
