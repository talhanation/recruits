package com.talhanation.recruits.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageClearUpkeepGui implements Message<MessageClearUpkeepGui> {

    private UUID uuid;

    public MessageClearUpkeepGui(){
    }

    public MessageClearUpkeepGui(UUID uuid) {
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.uuid, 16.0D).ifPresent((recruit) -> {
            recruit.clearUpkeepPos();
            recruit.clearUpkeepEntity();
        });
    }

    public MessageClearUpkeepGui fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

}
