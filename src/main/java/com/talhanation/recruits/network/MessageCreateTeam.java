package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
public class MessageCreateTeam implements Message<MessageCreateTeam> {

    public static final CustomPacketPayload.Type<MessageCreateTeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagecreateteam"));
    private String teamName;
    private String displayName;
    private ChatFormatting color;
    private ItemStack banner;
    private int index;

    public MessageCreateTeam(){
    }

    public MessageCreateTeam(String name, String displayName, ItemStack banner, ChatFormatting color, int index) {
        this.teamName = name;
        this.displayName = displayName;
        this.banner = banner;
        this.color = color;
        this.index = index;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        ServerLevel world = player.serverLevel();
        FactionEvents.createTeam(true, ((ServerPlayer) context.player()), world, this.teamName, this.displayName, player.getName().getString(), this.banner, this.color, (byte) index);
    }

    public MessageCreateTeam fromBytes(RegistryFriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.displayName = buf.readUtf();
        this.banner = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.color = ChatFormatting.getById(buf.readInt());
        this.index = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeUtf(this.displayName);
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.banner);
        buf.writeInt(this.color.getId());
        buf.writeInt(this.index);
    }

    @Override
    public CustomPacketPayload.Type<MessageCreateTeam> type() {
        return TYPE;
    }
}
