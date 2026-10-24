package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPackets;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageGovernorSetTax implements Message<MessageGovernorSetTax> {

    private UUID claim;
    private int level;

    public MessageGovernorSetTax() {
    }

    public MessageGovernorSetTax(UUID claim, int level) {
        this.claim = claim;
        this.level = level;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ClaimGovernorData data = GovernorPackets.access(player, claim, true);
        if (data == null) return;

        data.setTaxLevel(level);
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorSetTax fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.level = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeInt(level);
    }
}
