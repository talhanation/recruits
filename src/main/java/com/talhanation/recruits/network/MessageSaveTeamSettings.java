package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
public class MessageSaveTeamSettings implements Message<MessageSaveTeamSettings> {
    public static final CustomPacketPayload.Type<MessageSaveTeamSettings> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesaveteamsettings"));
    private CompoundTag nbt;
    private String stringID;
    private int cost;

    public MessageSaveTeamSettings() {
    }

    public MessageSaveTeamSettings(RecruitsFaction team, int cost) {
        this.nbt = team.toNBT();
        this.stringID = team.getStringID();
        this.cost = cost;
    }

    @Override
    public PacketFlow getExecutingSide()  {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        RecruitsFaction editedTeam = RecruitsFaction.fromNBT(nbt);
        FactionEvents.modifyTeam(((ServerPlayer) context.player()).server.overworld(), stringID, editedTeam, ((ServerPlayer) context.player()), cost);
    }

    @Override
    public MessageSaveTeamSettings fromBytes(RegistryFriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.stringID = buf.readUtf();
        this.cost = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeUtf(this.stringID);
        buf.writeInt(this.cost);
    }

    @Override
    public CustomPacketPayload.Type<MessageSaveTeamSettings> type() {
        return TYPE;
    }
}
