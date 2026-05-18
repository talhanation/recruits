package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

public class MessageCreateTeam implements RecruitsMessage<MessageCreateTeam> {

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

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.getTeam() != null || this.color == null || this.index < 0 || this.index > FactionNetworkAuthority.MAX_UNIT_COLOR_INDEX) {
            return;
        }
        ServerLevel world = player.serverLevel();
        FactionEvents.createTeam(true, player, world, this.teamName, this.displayName, player.getName().getString(), this.banner, this.color, (byte) index);
    }

    public MessageCreateTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.displayName = buf.readUtf();
        this.banner = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        this.color = ChatFormatting.getById(buf.readInt());
        this.index = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeUtf(this.displayName);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, this.banner == null ? ItemStack.EMPTY : this.banner);
        buf.writeInt(this.color.getId());
        buf.writeInt(this.index);
    }
}
