package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.GovernorManager;
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
        RecruitsClaim recruitsClaim = GovernorManager.getManageableClaim(player, claim);
        if (recruitsClaim == null) return;
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
