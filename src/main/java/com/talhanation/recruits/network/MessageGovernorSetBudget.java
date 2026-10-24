package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPackets;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageGovernorSetBudget implements Message<MessageGovernorSetBudget> {

    private UUID claim;
    private int budget;

    public MessageGovernorSetBudget() {
    }

    public MessageGovernorSetBudget(UUID claim, int budget) {
        this.claim = claim;
        this.budget = budget;
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

        data.setBudgetPerDay(Math.max(0, Math.min(budget, 100000)));
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorSetBudget fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.budget = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeInt(budget);
    }
}
