package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;


public class MessageDeleteClaim implements Message<MessageDeleteClaim> {

    private UUID claimId;

    public MessageDeleteClaim(){

    }

    public MessageDeleteClaim(RecruitsClaim claim) {
        this.claimId = claim == null ? null : claim.getUUID();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        if (this.claimId == null || context.getSender() == null) return;
        if (context.getSender().level().dimension() != Level.OVERWORLD) return;

        ClaimEvents.recruitsClaimManager.removeClaim(
                (ServerLevel) context.getSender().getCommandSenderWorld(),
                this.claimId);
    }
    public MessageDeleteClaim fromBytes(FriendlyByteBuf buf) {
        this.claimId = buf.readBoolean() ? buf.readUUID() : null;
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.claimId != null);
        if (this.claimId != null) {
            buf.writeUUID(this.claimId);
        }
    }
}
