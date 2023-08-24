package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageCreateTeam implements Message<MessageCreateTeam> {

    private String teamName;
    private String color;
    private ItemStack banner;

    public MessageCreateTeam(){
    }

    public MessageCreateTeam(String name, ItemStack banner, String color) {
        this.teamName = name;
        this.banner = banner;
        this.color = color;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        ServerLevel world = player.serverLevel();
        TeamEvents.createTeam(context.getSender(), world, this.teamName, player.getName().getString(), this.banner, this.color);
    }

    public MessageCreateTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.banner = buf.readItem();
        this.color = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeItemStack(this.banner, false);
        buf.writeUtf(this.color);
    }
}
