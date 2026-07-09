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

public class MessageAggro implements Message<MessageAggro> {

    private UUID player;
    private UUID recruit;
    private int state;
    private UUID group;
    private boolean fromGui;


    public MessageAggro() {
    }

    public MessageAggro(UUID player, int state, UUID group) {
        this.player = player;
        this.state = state;
        this.group = group;
        this.fromGui = false;
        this.recruit = null;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());

        double boundBoxInflateModifier = 16.0D;
        if(!fromGui) {
            boundBoxInflateModifier = 100.0D;
        }


        List<AbstractRecruitEntity> recruits = fromGui
                ? RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, boundBoxInflateModifier).map(List::of).orElse(List.of())
                : RecruitCommandTargetResolver.resolveGroupTargets(player, this.player, this.group, boundBoxInflateModifier);
        CommandIntent intent = new CommandIntent.Aggro(
                player.getCommandSenderWorld().getGameTime(),
                CommandIntentPriority.NORMAL,
                false,
                this.state,
                this.group,
                this.fromGui
        );
        CommandIntentDispatcher.dispatch(player, intent, recruits);
    }

    public MessageAggro fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readUUID();
        if (this.recruit != null) this.recruit = buf.readUUID();
        this.fromGui = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
        buf.writeUUID(this.group);
        buf.writeBoolean(this.fromGui);
        if (this.recruit != null) buf.writeUUID(this.recruit);
    }
}
