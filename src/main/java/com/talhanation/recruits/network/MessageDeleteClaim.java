package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;


public class MessageDeleteClaim implements Message<MessageDeleteClaim> {

    public static final CustomPacketPayload.Type<MessageDeleteClaim> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagedeleteclaim"));
    private UUID claimId;

    public MessageDeleteClaim(){

    }

    public MessageDeleteClaim(RecruitsClaim claim) {
        this.claimId = claim == null ? null : claim.getUUID();
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        if (this.claimId == null || ((ServerPlayer) context.player()) == null) return;
        if (((ServerPlayer) context.player()).level().dimension() != Level.OVERWORLD) return;

        ClaimEvents.recruitsClaimManager.removeClaim(
                (ServerLevel) ((ServerPlayer) context.player()).getCommandSenderWorld(),
                this.claimId);
    }
    public MessageDeleteClaim fromBytes(RegistryFriendlyByteBuf buf) {
        this.claimId = buf.readBoolean() ? buf.readUUID() : null;
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.claimId != null);
        if (this.claimId != null) {
            buf.writeUUID(this.claimId);
        }
    }

    @Override
    public CustomPacketPayload.Type<MessageDeleteClaim> type() {
        return TYPE;
    }
}
