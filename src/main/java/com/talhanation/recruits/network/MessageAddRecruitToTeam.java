package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;

public class MessageAddRecruitToTeam implements Message<MessageAddRecruitToTeam> {

    public static final CustomPacketPayload.Type<MessageAddRecruitToTeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageaddrecruittoteam"));
    private String teamName;
    private int x;

    public MessageAddRecruitToTeam(){
    }

    public MessageAddRecruitToTeam(String teamName, int x) {
        this.teamName = teamName;
        this.x = x;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerLevel level = Objects.requireNonNull(((ServerPlayer) context.player())).serverLevel();

        FactionEvents.addNPCToData(level, teamName, x);
    }

    public MessageAddRecruitToTeam fromBytes(RegistryFriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.x = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeInt(this.x);
    }

    @Override
    public CustomPacketPayload.Type<MessageAddRecruitToTeam> type() {
        return TYPE;
    }
}
