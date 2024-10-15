package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdatePlayerList implements Message<MessageToClientUpdatePlayerList> {
    public List<RecruitsPlayerInfo> playerInfoList;

    public MessageToClientUpdatePlayerList() {
    }

    public MessageToClientUpdatePlayerList(RecruitsPlayerInfo playerInfo) {
        //this.playerInfo = playerInfo;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        PlayersList.onlinePlayers = this.playerInfoList;
    }

    @Override
    public MessageToClientUpdatePlayerList fromBytes(FriendlyByteBuf buf) {
        //this.playerInfo = RecruitsPlayerInfo.fromBytes(buf);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        //this.playerInfo.toBytes(buf);
    }

}