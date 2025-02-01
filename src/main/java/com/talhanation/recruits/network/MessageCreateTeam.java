package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageCreateTeam implements Message<MessageCreateTeam> {

    private String teamName;
    private ChatFormatting color;
    private ItemStack banner;
    private int index;

    public MessageCreateTeam(){
    }

    public MessageCreateTeam(String name, ItemStack banner, ChatFormatting color, int index) {
        this.teamName = name;
        this.banner = banner;
        this.color = color;
        this.index = index;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        ServerLevel world = player.serverLevel();
        TeamEvents.createTeam(true, context.getSender(), world, this.teamName, player.getName().getString(), this.banner, this.color, (byte) index);
    }

    public MessageCreateTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.banner = buf.readItem();
        this.color = ChatFormatting.getById(buf.readInt());
        this.index = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeItemStack(this.banner, false);
        buf.writeInt(this.color.getId());
        buf.writeInt(this.index);
    }
}
