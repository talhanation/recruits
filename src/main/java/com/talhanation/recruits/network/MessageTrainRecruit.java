package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.util.RecruitTraining;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Client → Server: train a recruit with the player's experience (More menu).
 * Cost, gain and max level are taken from the server config, never from the client.
 */
public class MessageTrainRecruit implements Message<MessageTrainRecruit> {

    /** Same reach as the recruit inventory menu (RecruitInventoryMenu#stillValid). */
    private static final double MAX_DISTANCE = 16.0D;

    private UUID recruit;

    public MessageTrainRecruit() {
    }

    public MessageTrainRecruit(UUID recruit) {
        this.recruit = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!(player.getCommandSenderWorld() instanceof ServerLevel level)) return;

        Entity entity = level.getEntity(this.recruit);
        if (!(entity instanceof AbstractRecruitEntity recruitEntity)) return;
        if (!NetworkSecurity.canControl(player, recruitEntity)) return;
        if (!NetworkSecurity.isNear(player, recruitEntity, MAX_DISTANCE)) return;

        RecruitTraining.train(player, recruitEntity);
    }

    public MessageTrainRecruit fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
    }
}
