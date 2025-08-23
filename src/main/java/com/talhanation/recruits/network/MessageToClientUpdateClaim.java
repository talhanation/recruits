package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class MessageToClientUpdateClaim implements Message<MessageToClientUpdateClaim> {
    private CompoundTag claimNBT;

    public MessageToClientUpdateClaim() {
    }

    public MessageToClientUpdateClaim(RecruitsClaim claim) {
        this.claimNBT = claim.toNBT();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        this.updateOrAddClaimFromNBT(claimNBT);
    }

    public void updateOrAddClaimFromNBT(CompoundTag claimNBT) {
        RecruitsClaim newClaim = RecruitsClaim.fromNBT(claimNBT);

        for (int i = 0; i < ClientManager.recruitsClaims.size(); i++) {
            RecruitsClaim existing = ClientManager.recruitsClaims.get(i);
            if (existing.getUUID().equals(newClaim.getUUID())) {

                ClientManager.recruitsClaims.set(i, newClaim);
                return;
            }
        }

        ClientManager.recruitsClaims.add(newClaim);
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
