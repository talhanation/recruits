package com.talhanation.recruits.network;

import com.talhanation.recruits.client.events.RecruitsToastManager;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import static com.talhanation.recruits.client.events.RecruitsToastManager.*;


public class MessageToClientSetDiplomaticToast implements Message<MessageToClientSetDiplomaticToast> {

    private int x;
    private String s;
    private CompoundTag nbt;

    public MessageToClientSetDiplomaticToast() {
    }
    public MessageToClientSetDiplomaticToast(int x, RecruitsTeam team) {
        this(x, team, team.getTeamDisplayName());
    }
    public MessageToClientSetDiplomaticToast(int x, RecruitsTeam team, String s) {
        this.x = x;
        this.s = s;
        this.nbt = team.toNBT();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        RecruitsTeam team = RecruitsTeam.fromNBT(nbt);
        switch (x){
            case 0 -> RecruitsToastManager.setTeamToastForPlayer(Images.NEUTRAL, TOAST_NEUTRAL_TITLE, TOAST_NEUTRAL_SET(s), team);//
            case 1 -> RecruitsToastManager.setTeamToastForPlayer(Images.ALLY, TOAST_ALLY_TITLE, TOAST_ALLY_SET(s), team);//
            case 2 -> RecruitsToastManager.setTeamToastForPlayer(Images.ENEMY, TOAST_ENEMY_TITLE,TOAST_ENEMY_SET(s), team);//
            case 4 -> RecruitsToastManager.setTeamToastForPlayer(Images.NEUTRAL, TOAST_NEUTRAL_TITLE, TOAST_NEUTRAL_INFO(s), team);//
            case 5 -> RecruitsToastManager.setTeamToastForPlayer(Images.ALLY, TOAST_ALLY_TITLE, TOAST_ALLY_INFO(s), team);//
            case 6 -> RecruitsToastManager.setTeamToastForPlayer(Images.ENEMY, TOAST_ENEMY_TITLE, TOAST_ENEMY_INFO(s), team);//

            case 7 -> RecruitsToastManager.setTeamToastForPlayer(Images.LETTER, TOAST_JOIN_REQUEST_TITLE, TOAST_WANTS_TO_JOIN(s), team);//

            case 8 -> RecruitsToastManager.setTeamToastForPlayer(Images.TEAM_JOIN, TOAST_TEAM_JOINED_TITLE, TOAST_TEAM_JOINED(s), team);//
            case 9 -> RecruitsToastManager.setTeamToastForPlayer(Images.TEAM_JOIN, TOAST_PLAYER_JOINED_TITLE, TOAST_PLAYER_JOINED_TEAM(s), team);//

            case 10 -> RecruitsToastManager.setTeamToastForPlayer(Images.CROWN, TOAST_NEW_LEADER_TITLE, TOAST_NEW_LEADER(s), team);
            case 11 -> RecruitsToastManager.setTeamToastForPlayer(Images.CROWN, TOAST_NEW_FACTION_NAME_TITLE, TOAST_NEW_FACTION_NAME(s), team);
            case 12 -> RecruitsToastManager.setTeamToastForPlayer(Images.CROWN, TOAST_NEW_BANNER_TITLE, TOAST_NEW_BANNER(), team);
        }
    }

    @Override
    public MessageToClientSetDiplomaticToast fromBytes(FriendlyByteBuf buf) {
       this.x = buf.readInt();
       this.s = buf.readUtf();
       this.nbt = buf.readNbt();
       return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeUtf(s);
        buf.writeNbt(nbt);
    }
}