package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorOffice;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Claim widget of the Governor screen: appoint the Governor to one of the owner's claims,
 * or (claim = null, the "x") remove him from office.
 */
public class MessageGovernorSetClaim implements Message<MessageGovernorSetClaim> {

    private UUID governor;
    @Nullable
    private UUID claim;

    public MessageGovernorSetClaim() {
    }

    public MessageGovernorSetClaim(UUID governor, @Nullable UUID claim) {
        this.governor = governor;
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

        GovernorEntity entity = null;
        for (GovernorEntity candidate : player.serverLevel().getEntitiesOfClass(GovernorEntity.class,
                player.getBoundingBox().inflate(32D), g -> g.getUUID().equals(this.governor) && g.isAlive())) {
            entity = candidate;
        }
        if (entity == null || !player.getUUID().equals(entity.getOwnerUUID())) return;

        if (claim == null) {
            GovernorOffice.removeFromOffice(entity, player);
        } else {
            RecruitsClaim recruitsClaim = GovernorManager.getClaim(claim);
            if (recruitsClaim == null) {
                player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_no_claim"));
            } else {
                GovernorOffice.appoint(entity, recruitsClaim, player);
            }
        }
        GovernorManager.sendGovernorSnapshot(player, entity, false);
    }

    @Override
    public MessageGovernorSetClaim fromBytes(FriendlyByteBuf buf) {
        this.governor = buf.readUUID();
        this.claim = buf.readBoolean() ? buf.readUUID() : null;
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(governor);
        buf.writeBoolean(claim != null);
        if (claim != null) buf.writeUUID(claim);
    }
}
