package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.governor.GovernorUnitType;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageGovernorSetQuota implements Message<MessageGovernorSetQuota> {

    private UUID claim;
    private int type;
    private int amount;

    public MessageGovernorSetQuota() {
    }

    public MessageGovernorSetQuota(UUID claim, GovernorUnitType type, int amount) {
        this.claim = claim;
        this.type = type.getIndex();
        this.amount = amount;
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
        RecruitsClaim recruitsClaim = GovernorManager.getClaim(claim);
        if (data == null || recruitsClaim == null) return;

        GovernorUnitType unitType = GovernorUnitType.fromIndex(type);
        int others = data.getTotalQuota() - data.getQuota(unitType);
        int max = Math.max(0, GovernorManager.getGarrisonCap(recruitsClaim) - others);
        data.setQuota(unitType, Math.max(0, Math.min(amount, max)));
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorSetQuota fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.type = buf.readInt();
        this.amount = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeInt(type);
        buf.writeInt(amount);
    }
}
