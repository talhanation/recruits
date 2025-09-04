package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageUpdateClaim implements Message<MessageUpdateClaim> {

    private CompoundTag claimNBT;

    public MessageUpdateClaim(){

    }

    public MessageUpdateClaim(RecruitsClaim claim) {
        this.claimNBT = claim.toNBT();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        RecruitsClaim updatedClaim = RecruitsClaim.fromNBT(this.claimNBT);

        ClaimEvents.recruitsClaimManager.addOrUpdateClaim((ServerLevel) context.getSender().getCommandSenderWorld(), updatedClaim);
    }
    public MessageUpdateClaim fromBytes(FriendlyByteBuf buf) {
        this.claimNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(claimNBT);
    }
}
