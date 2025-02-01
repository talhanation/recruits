package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class MessageToServerRequestUpdatePlayerCurrencyCount implements Message<MessageToServerRequestUpdatePlayerCurrencyCount> {

    public MessageToServerRequestUpdatePlayerCurrencyCount() {
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        int count = player.getInventory().countItem(TeamEvents.getCurrency().getItem());

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdatePlayerCurrencyCount(count));
    }

    @Override
    public MessageToServerRequestUpdatePlayerCurrencyCount fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {

    }

}