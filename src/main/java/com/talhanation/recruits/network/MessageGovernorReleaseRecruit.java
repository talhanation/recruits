package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.governor.GovernorGarrison;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** "Aus Garnison lösen": the owner takes one of his garrison recruits back under his own command. */
public class MessageGovernorReleaseRecruit implements Message<MessageGovernorReleaseRecruit> {

    private UUID recruit;

    public MessageGovernorReleaseRecruit() {
    }

    public MessageGovernorReleaseRecruit(UUID recruit) {
        this.recruit = recruit;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        // anywhere in the loaded world: from the garrison tab the unit may stand far away in the claim
        if (!(player.serverLevel().getEntity(recruit) instanceof AbstractRecruitEntity entity) || !entity.isAlive()) return;
        if (!entity.isGarrison() || !player.getUUID().equals(entity.getOwnerUUID())) return;
        if (GovernorGarrison.releaseFromGarrison(player.serverLevel(), entity, player)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.released", entity.getName().getString()));
        }
    }

    @Override
    public MessageGovernorReleaseRecruit fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
    }
}
