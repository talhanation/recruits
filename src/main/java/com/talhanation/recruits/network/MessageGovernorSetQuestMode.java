package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPackets;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** Village tab: below which reputation the Governor takes village quests for his owner (0 = never). */
public class MessageGovernorSetQuestMode implements Message<MessageGovernorSetQuestMode> {

    private UUID claim;
    private int mode;

    public MessageGovernorSetQuestMode() {
    }

    public MessageGovernorSetQuestMode(UUID claim, int mode) {
        this.claim = claim;
        this.mode = mode;
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

        data.setQuestMode(mode);
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorSetQuestMode fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.mode = buf.readVarInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeVarInt(mode);
    }
}
