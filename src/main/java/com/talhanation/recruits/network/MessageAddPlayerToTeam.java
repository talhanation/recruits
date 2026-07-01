package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;

public class MessageAddPlayerToTeam implements Message<MessageAddPlayerToTeam> {

    public static final CustomPacketPayload.Type<MessageAddPlayerToTeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageaddplayertoteam"));
    private String teamName;
    private String namePlayerToAdd;

    public MessageAddPlayerToTeam(){
    }

    public MessageAddPlayerToTeam(String teamName, String namePlayerToAdd) {
        this.teamName = teamName;
        this.namePlayerToAdd = namePlayerToAdd;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        ServerLevel world = player.serverLevel();

        FactionEvents.addPlayerToTeam(player, world, this.teamName, this.namePlayerToAdd);
    }

    public MessageAddPlayerToTeam fromBytes(RegistryFriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.namePlayerToAdd = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeUtf(this.namePlayerToAdd);
    }

    @Override
    public CustomPacketPayload.Type<MessageAddPlayerToTeam> type() {
        return TYPE;
    }
}
