package com.talhanation.recruits.network;

import com.talhanation.recruits.command.CommandIntent;
import com.talhanation.recruits.command.CommandIntentDispatcher;
import com.talhanation.recruits.command.CommandIntentPriority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageStrategicFire implements Message<MessageStrategicFire> {

    private UUID player;
    private UUID group;
    private boolean should;

    public MessageStrategicFire() {
    }

    public MessageStrategicFire(UUID player, UUID group, boolean should) {
        this.player = player;
        this.group = group;
        this.should = should;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> recruits = RecruitCommandTargetResolver.resolveGroupTargets(serverPlayer, this.player, this.group, 200D);
        CommandIntent intent = new CommandIntent.StrategicFire(
                serverPlayer.getCommandSenderWorld().getGameTime(),
                CommandIntentPriority.NORMAL,
                false,
                this.group,
                this.should
        );
        CommandIntentDispatcher.dispatch(serverPlayer, intent, recruits);
    }

    public MessageStrategicFire fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readUUID();
        this.should = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.group);
        buf.writeBoolean(this.should);
    }
}
