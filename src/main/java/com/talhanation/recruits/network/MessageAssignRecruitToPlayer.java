package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.governor.GovernorManager;
import net.minecraft.network.chat.Component;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAssignRecruitToPlayer implements Message<MessageAssignRecruitToPlayer> {

    private UUID recruit;
    private UUID newOwner;
    public MessageAssignRecruitToPlayer() {
    }

    public MessageAssignRecruitToPlayer(UUID recruit, UUID newOwner) {
        this.recruit = recruit;
        this.newOwner = newOwner;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = context.getSender();
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(64.0D));
        ServerLevel serverLevel = (ServerLevel) serverPlayer.getCommandSenderWorld();

        for (AbstractRecruitEntity recruit : list) {
            if(recruit.getUUID().equals(this.recruit)){
                if(!canAssign(serverPlayer, recruit)) return;
                recruit.assignToPlayer(newOwner, null);
                FactionEvents.notifyPlayer(serverLevel, new RecruitsPlayerInfo(newOwner, ""), 0, serverPlayer.getName().getString());
                break;
            }
        }
    }

    /**
     * The owner may give his recruit to another player; a faction leader may take over the recruits of
     * his members (including garrison recruits). A Governor cannot be handed over, and garrison recruits
     * have to be released before their owner can give them away.
     */
    private boolean canAssign(ServerPlayer sender, AbstractRecruitEntity recruit) {
        if (recruit instanceof GovernorEntity) {
            sender.sendSystemMessage(Component.translatable("chat.recruits.governor.cannot_take_over", recruit.getName().getString()));
            return false;
        }
        boolean isOwner = sender.getUUID().equals(recruit.getOwnerUUID());
        if (isOwner) {
            if (recruit.isGarrison()) {
                sender.sendSystemMessage(Component.translatable("chat.recruits.governor.release_first", recruit.getName().getString()));
                return false;
            }
            return true;
        }

        String factionId = recruit.getTeam() != null ? recruit.getTeam().getName() : null;
        boolean isLeader = factionId != null && sender.getTeam() != null && factionId.equals(sender.getTeam().getName())
                && GovernorManager.isFactionLeader(factionId, sender.getUUID());
        if (!isLeader || !sender.getUUID().equals(this.newOwner)) return false;

        if (!RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(factionId, sender.getUUID())) {
            sender.sendSystemMessage(Component.translatable("chat.recruits.governor.release_limit", recruit.getName().getString()));
            return false;
        }
        return true;
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