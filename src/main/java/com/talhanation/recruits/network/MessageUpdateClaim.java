package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.codec.ClaimNetworkCodec;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageUpdateClaim implements Message<MessageUpdateClaim> {

    private RecruitsClaim claim;

    public MessageUpdateClaim(){

    }

    public MessageUpdateClaim(RecruitsClaim claim) {
        this.claim = claim;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        RecruitsClaim updatedClaim = this.claim;
        if (updatedClaim == null || context.getSender() == null) return;
        if(!RecruitsServerConfig.AllowClaiming.get()) return;
        if(context.getSender().level().dimension() != Level.OVERWORLD) return;
        if (!updatedClaim.isRemoved
                && updatedClaim.getClaimedChunks().size() > RecruitsServerConfig.MaxClaimChunks.get()) return;

        ClaimEvents.recruitsClaimManager.addOrUpdateClaim((ServerLevel) context.getSender().getCommandSenderWorld(), updatedClaim);
    }
    public MessageUpdateClaim fromBytes(FriendlyByteBuf buf) {
        this.claim = ClaimNetworkCodec.readNullableClaim(buf);
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        ClaimNetworkCodec.writeNullableClaim(buf, this.claim);
    }
}
