package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPackets;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageGovernorRemovePost implements Message<MessageGovernorRemovePost> {

    private UUID claim;
    private UUID post;

    public MessageGovernorRemovePost() {
    }

    public MessageGovernorRemovePost(UUID claim, UUID post) {
        this.claim = claim;
        this.post = post;
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

        // the recruit of a removed guard post is free again and walks to the nearest reserve
        data.removePost(post);
        // patrols skip the removed point; a patrol without points left returns to the reserve
        data.patrols.forEach(patrol -> patrol.removeMissingPoints(data));
        data.patrols.removeIf(patrol -> patrol.getCurrentPoint() == null);
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorRemovePost fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.post = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeUUID(post);
    }
}
