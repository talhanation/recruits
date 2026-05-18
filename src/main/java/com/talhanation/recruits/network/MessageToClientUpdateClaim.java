package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.api.ClientClaimEvent;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

public class MessageToClientUpdateClaim implements RecruitsMessage<MessageToClientUpdateClaim> {
    private CompoundTag claimNBT;

    public MessageToClientUpdateClaim() {
    }

    public MessageToClientUpdateClaim(RecruitsClaim claim) {
        this.claimNBT = claim.toNBT();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(RecruitsNetworkContext context) {
        this.updateOrAddClaimFromNBT(claimNBT);
    }
    @OnlyIn(Dist.CLIENT)
    public void updateOrAddClaimFromNBT(CompoundTag claimNBT) {
        RecruitsClaim newClaim = RecruitsClaim.fromNBT(claimNBT);

        for (int i = 0; i < ClientManager.recruitsClaims.size(); i++) {
            RecruitsClaim existing = ClientManager.recruitsClaims.get(i);
            if (existing.getUUID().equals(newClaim.getUUID())) {
                ClientManager.recruitsClaims.set(i, newClaim);

                boolean isCurrentClaim = ClientManager.currentClaim != null
                        && ClientManager.currentClaim.getUUID().equals(newClaim.getUUID());

                // Aktuellen Claim-Zeiger ebenfalls aktualisieren
                if (isCurrentClaim) {
                    ClientManager.currentClaim = newClaim;
                }

                ClientManager.updateActiveSiege(newClaim);

                NeoForge.EVENT_BUS.post(new ClientClaimEvent.DataUpdated(newClaim, isCurrentClaim));
                return;
            }
        }

        ClientManager.recruitsClaims.add(newClaim);
        ClientManager.updateActiveSiege(newClaim);
        NeoForge.EVENT_BUS.post(
                new ClientClaimEvent.DataUpdated(newClaim, false));
    }
    @Override
    public MessageToClientUpdateClaim fromBytes(FriendlyByteBuf buf) {
        this.claimNBT = buf.readNbt();

        return this;
    }
    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.claimNBT);
    }
}
