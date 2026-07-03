package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.events.RecruitsToastManager;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import static com.talhanation.recruits.client.events.RecruitsToastManager.*;


public class MessageToClientSetDiplomaticToast implements Message<MessageToClientSetDiplomaticToast> {

    public static final CustomPacketPayload.Type<MessageToClientSetDiplomaticToast> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientsetdiplomatictoast"));
    private int x;
    private String s;
    private CompoundTag nbt;

    public MessageToClientSetDiplomaticToast() {
    }
    public MessageToClientSetDiplomaticToast(int x, RecruitsFaction team) {
        this(x, team, team.getTeamDisplayName());
    }
    public MessageToClientSetDiplomaticToast(int x, RecruitsFaction team, String s) {
        this.x = x;
        this.s = s;
        this.nbt = team.toNBT();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        RecruitsFaction team = RecruitsFaction.fromNBT(nbt);
        switch (x){
            case 0 -> RecruitsToastManager.setTeamToastForPlayer(Images.NEUTRAL, TOAST_NEUTRAL_TITLE, TOAST_NEUTRAL_SET(s), team);
            case 1 -> RecruitsToastManager.setTeamToastForPlayer(Images.ALLY, TOAST_ALLY_TITLE, TOAST_ALLY_SET(s), team);
            case 2 -> RecruitsToastManager.setTeamToastForPlayer(Images.ENEMY, TOAST_ENEMY_TITLE, TOAST_ENEMY_SET(s), team);
            case 4 -> RecruitsToastManager.setTeamToastForPlayer(Images.NEUTRAL, TOAST_NEUTRAL_TITLE, TOAST_NEUTRAL_INFO(s), team);
            case 5 -> RecruitsToastManager.setTeamToastForPlayer(Images.ALLY, TOAST_ALLY_TITLE, TOAST_ALLY_INFO(s), team);
            case 6 -> RecruitsToastManager.setTeamToastForPlayer(Images.ENEMY, TOAST_ENEMY_TITLE, TOAST_ENEMY_INFO(s), team);

            case 7 -> RecruitsToastManager.setTeamToastForPlayer(Images.LETTER, TOAST_JOIN_REQUEST_TITLE, TOAST_WANTS_TO_JOIN(s), team);

            case 8 -> RecruitsToastManager.setTeamToastForPlayer(Images.TEAM_JOIN, TOAST_TEAM_JOINED_TITLE, TOAST_TEAM_JOINED(s), team);
            case 9 -> RecruitsToastManager.setTeamToastForPlayer(Images.TEAM_JOIN, TOAST_PLAYER_JOINED_TITLE, TOAST_PLAYER_JOINED_TEAM(s), team);

            case 10 -> RecruitsToastManager.setTeamToastForPlayer(Images.CROWN, TOAST_NEW_LEADER_TITLE, TOAST_NEW_LEADER(s), team);
            case 11 -> RecruitsToastManager.setTeamToastForPlayer(Images.CROWN, TOAST_NEW_FACTION_NAME_TITLE, TOAST_NEW_FACTION_NAME(s), team);
            case 12 -> RecruitsToastManager.setTeamToastForPlayer(Images.CROWN, TOAST_NEW_BANNER_TITLE, TOAST_NEW_BANNER(), team);

            case 20 -> RecruitsToastManager.setTeamToastForPlayer(Images.NEUTRAL, TOAST_TREATY_ESTABLISHED_TITLE, TOAST_TREATY_ESTABLISHED(s), team);
            case 21 -> RecruitsToastManager.setTeamToastForPlayer(Images.NEUTRAL, TOAST_TREATY_EXPIRED_TITLE, TOAST_TREATY_EXPIRED(s), team);

            case 30 -> RecruitsToastManager.setTeamToastForPlayer(Images.EMBARGO, TOAST_EMBARGO_DECLARED_TITLE, TOAST_EMBARGO_DECLARED(s), team);
            case 31 -> RecruitsToastManager.setTeamToastForPlayer(Images.EMBARGO, TOAST_EMBARGO_LIFTED_TITLE, TOAST_EMBARGO_LIFTED(s), team);
        }
    }

    @Override
    public MessageToClientSetDiplomaticToast fromBytes(RegistryFriendlyByteBuf buf) {
       this.x = buf.readInt();
       this.s = buf.readUtf();
       this.nbt = buf.readNbt();
       return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeUtf(s);
        buf.writeNbt(nbt);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientSetDiplomaticToast> type() {
        return TYPE;
    }
}
