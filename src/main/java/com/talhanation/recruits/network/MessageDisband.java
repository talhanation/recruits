package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageDisband implements Message<MessageDisband> {

    private UUID recruit;
    private boolean keepTeam;

    public MessageDisband() {
    }

    public MessageDisband(UUID recruit, boolean keepTeam) {
        this.recruit = recruit;
        this.keepTeam = keepTeam;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(16D),
                (recruit) -> recruit.getUUID().equals(this.recruit)
        ).forEach((recruit) -> recruit.disband(context.getSender(), keepTeam, true));
    }

    public MessageDisband fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.keepTeam = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(keepTeam);
    }
}