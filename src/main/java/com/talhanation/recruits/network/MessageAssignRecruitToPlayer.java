package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageAssignRecruitToPlayer implements RecruitsMessage<MessageAssignRecruitToPlayer> {

    private UUID recruit;
    private UUID newOwner;
    public MessageAssignRecruitToPlayer() {
    }

    public MessageAssignRecruitToPlayer(UUID recruit, UUID newOwner) {
        this.recruit = recruit;
        this.newOwner = newOwner;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        ServerLevel serverLevel = (ServerLevel) serverPlayer.getCommandSenderWorld();

        serverPlayer.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                serverPlayer.getBoundingBox().inflate(64.0D),
                recruit -> recruit.getUUID().equals(this.recruit) && canAssign(serverPlayer, recruit)
        ).stream().findFirst()
                .ifPresent(recruit -> {
                    recruit.assignToPlayer(newOwner, null);
                    FactionEvents.notifyPlayer(serverLevel, new RecruitsPlayerInfo(newOwner, ""), 0, serverPlayer.getName().getString());
                });
    }

    private boolean canAssign(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (RecruitCommandAuthority.ownsRecruit(player, recruit)) {
            return true;
        }
        if (!player.getUUID().equals(this.newOwner) || !recruit.isOwned() || recruit.getTeam() == null) {
            return false;
        }
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(recruit.getTeam().getName());
        return faction != null && player.getUUID().equals(faction.getTeamLeaderUUID());
    }

    public MessageAssignRecruitToPlayer fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.newOwner = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeUUID(this.newOwner);
    }
}
