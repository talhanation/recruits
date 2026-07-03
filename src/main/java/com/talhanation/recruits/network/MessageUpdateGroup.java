package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsGroupsManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
public class MessageUpdateGroup implements Message<MessageUpdateGroup> {

    public static final CustomPacketPayload.Type<MessageUpdateGroup> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageupdategroup"));
    private CompoundTag groupNBT;

    public MessageUpdateGroup(){

    }

    public MessageUpdateGroup(RecruitsGroup group) {
        this.groupNBT = group.toNBT();
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        RecruitsGroup updatedGroup = RecruitsGroup.fromNBT(this.groupNBT);
        ServerPlayer serverPLayer = ((ServerPlayer) context.player());

        RecruitEvents.recruitsGroupsManager.addOrUpdateGroup((ServerLevel) serverPLayer.getCommandSenderWorld(), serverPLayer, updatedGroup);
    }
    public MessageUpdateGroup fromBytes(RegistryFriendlyByteBuf buf) {
        this.groupNBT = buf.readNbt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(groupNBT);
    }

    @Override
    public CustomPacketPayload.Type<MessageUpdateGroup> type() {
        return TYPE;
    }
}
