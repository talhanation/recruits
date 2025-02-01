package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageDisbandGroup implements Message<MessageDisbandGroup> {

    private UUID owner;
    private UUID recruit;
    private boolean keepTeam;

    public MessageDisbandGroup() {
    }

    public MessageDisbandGroup(UUID owner, UUID recruit, boolean keepTeam) {
        this.owner = owner;
        this.recruit = recruit;
        this.keepTeam = keepTeam;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> list = player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100D)
        );
        int group = -1;

        for (AbstractRecruitEntity recruit1 : list) {
            if (recruit1.getUUID().equals(recruit)) {
                group = recruit1.getGroup();
                break;
            }
        }

        if (group == -1) {
            return;
        }

        for (AbstractRecruitEntity recruit : list) {
            if (owner.equals(recruit.getOwnerUUID()) && recruit.getGroup() == group) {
                recruit.disband(context.getSender(), keepTeam, true);
            }
        }
    }

    public MessageDisbandGroup fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.recruit = buf.readUUID();
        this.keepTeam = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeUUID(recruit);
        buf.writeBoolean(keepTeam);
    }
}