package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorGarrison;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.governor.GovernorPost;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;

public class MessageGovernorAddPost implements Message<MessageGovernorAddPost> {

    private static final int MAX_POSTS = 256;

    private UUID claim;
    private int type;
    private BlockPos pos;
    /** Upkeep entity with an inventory (a storage area), null for a block. */
    @Nullable
    private UUID entity;

    public MessageGovernorAddPost() {
    }

    public MessageGovernorAddPost(UUID claim, GovernorPost.Type type, BlockPos pos) {
        this(claim, type, pos, null);
    }

    public MessageGovernorAddPost(UUID claim, GovernorPost.Type type, BlockPos pos, @Nullable UUID entity) {
        this.claim = claim;
        this.type = type.getIndex();
        this.pos = pos;
        this.entity = entity;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ClaimGovernorData data = GovernorPackets.access(player, claim, true);
        if (data == null) return;
        RecruitsClaim recruitsClaim = GovernorManager.getClaim(claim);
        if (recruitsClaim == null) return;

        GovernorPost.Type postType = GovernorPost.Type.fromIndex(type);
        UUID upkeepEntity = null;
        if (entity != null) {
            // only Upkeep posts can be an entity, it must have an inventory
            Entity target = player.serverLevel().getEntity(entity);
            if (postType != GovernorPost.Type.UPKEEP || !(target instanceof Container) || !target.isAlive()) {
                player.sendSystemMessage(Component.translatable("chat.recruits.governor.post_no_container"));
                return;
            }
            upkeepEntity = entity;
            pos = target.blockPosition();
        }
        if (!GovernorManager.isInClaim(recruitsClaim, pos)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.post_outside"));
            return;
        }
        if (postType.isContainer() && upkeepEntity == null && !GovernorGarrison.isContainer(player.serverLevel(), pos)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.post_no_container"));
            return;
        }
        if (data.getPosts().size() >= MAX_POSTS) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.post_limit", MAX_POSTS));
            return;
        }
        for (GovernorPost existing : data.getPosts()) {
            if (existing.getType() != postType) continue;
            if (upkeepEntity != null ? upkeepEntity.equals(existing.getEntity()) : existing.getEntity() == null && existing.getPos().equals(pos)) return;
        }

        GovernorPost post = data.addPost(postType, pos, upkeepEntity);
        player.sendSystemMessage(Component.translatable("chat.recruits.governor.post_added", post.getLabel(), post.getCoordinateString()));
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorAddPost fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.type = buf.readInt();
        this.pos = buf.readBlockPos();
        this.entity = buf.readBoolean() ? buf.readUUID() : null;
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeInt(type);
        buf.writeBlockPos(pos);
        buf.writeBoolean(entity != null);
        if (entity != null) buf.writeUUID(entity);
    }
}
