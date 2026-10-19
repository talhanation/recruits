package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
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
                if(!mayAssign(serverPlayer, recruit, newOwner)) break;
                recruit.assignToPlayer(newOwner, null);
                FactionEvents.notifyPlayer(serverLevel, new RecruitsPlayerInfo(newOwner, ""), 0, serverPlayer.getName().getString());
                break;
            }
        }
    }

    /**
     * Allowed:
     *  - the owner gives his recruit to another online player
     *  - the faction leader takes over a recruit of his faction (TakeOverScreen)
     *  - operators
     * Before: anyone within 64 blocks could assign any recruit to himself.
     */
    private static boolean mayAssign(ServerPlayer sender, AbstractRecruitEntity recruit, UUID newOwner) {
        if (newOwner == null || !recruit.isAlive()) return false;
        if (sender.getServer().getPlayerList().getPlayer(newOwner) == null) return false;
        if (NetworkSecurity.isAdmin(sender)) return true;
        if (NetworkSecurity.isOwner(sender, recruit)) return !newOwner.equals(sender.getUUID());

        boolean takeOver = newOwner.equals(sender.getUUID())
                && recruit.isOwned()
                && recruit.getTeam() != null
                && NetworkSecurity.isLeaderOf(sender, recruit.getTeam().getName());
        return takeOver;
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