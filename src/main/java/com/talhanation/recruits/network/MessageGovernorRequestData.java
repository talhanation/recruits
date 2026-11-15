package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** The client asks for the Governor data of the claim it stands in (command screen tab). */
public class MessageGovernorRequestData implements Message<MessageGovernorRequestData> {

    private UUID claim;

    public MessageGovernorRequestData() {
    }

    public MessageGovernorRequestData(UUID claim) {
        this.claim = claim;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        // writing orders for a far Governor at a Messenger: the screen shows the draft
        if (GovernorPackets.sendDraft(player, claim)) return;
        RecruitsClaim recruitsClaim = GovernorManager.getManageableClaim(player, claim);
        if (recruitsClaim == null) return;
        // opening the screen: the modules plan right now (at most every 5 seconds), the snapshot shows it
        GovernorManager manager = GovernorManager.get();
        ClaimGovernorData data = manager == null ? null : manager.getData(recruitsClaim.getUUID());
        if (data != null && data.hasGovernor()) GovernorManager.updateModules(player.server.overworld(), recruitsClaim, data, 100L);
        GovernorManager.sendSnapshot(player, recruitsClaim.getUUID(), false);
    }

    @Override
    public MessageGovernorRequestData fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
    }
}
