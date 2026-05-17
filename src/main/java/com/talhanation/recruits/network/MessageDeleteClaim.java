package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageDeleteClaim implements Message<MessageDeleteClaim> {

    private CompoundTag claimNBT;

    public MessageDeleteClaim(){

    }

    public MessageDeleteClaim(RecruitsClaim claim) {
        this.claimNBT = claim.toNBT();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if (player == null || this.claimNBT == null || this.claimNBT.isEmpty()) return;
        RecruitsClaim requestedClaim = ClaimNetworkAuthority.readClaim(this.claimNBT);
        if (requestedClaim == null) return;
        RecruitsClaim claim = ClaimNetworkAuthority.claimByUuid(requestedClaim.getUUID());
        if (claim == null) return;
        if (!ClaimNetworkAuthority.isCreativeAdmin(player)) return;

        ClaimEvents.recruitsClaimManager.removeClaim(claim);
        ClaimEvents.recruitsClaimManager.broadcastClaimsToAll((ServerLevel) player.getCommandSenderWorld());
    }

    public MessageDeleteClaim fromBytes(FriendlyByteBuf buf) {
        this.claimNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(claimNBT);
    }
}
