package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageFollowGui implements Message<MessageFollowGui> {

    private int state;
    private UUID uuid;

    public MessageFollowGui() {
    }

    public MessageFollowGui(int state, UUID uuid) {
        this.state = state;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedRecruit(serverPlayer, this.uuid, 16.0D)
                .ifPresent((recruit) -> CommandEvents.onMovementCommandGUI(recruit, state));
    }

    public MessageFollowGui fromBytes(FriendlyByteBuf buf) {
        this.state = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(state);
        buf.writeUUID(uuid);
    }
}
