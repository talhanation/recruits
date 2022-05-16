package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageCommandScreen implements Message<MessageCommandScreen> {

    private UUID uuid;

    public MessageCommandScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageCommandScreen(Player player) {
        this.uuid = player.getUUID();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (!player.getUUID().equals(uuid)) {
            return;
        }
        CommandEvents.openCommandScreen(player);
    }

    @Override
    public MessageCommandScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

}