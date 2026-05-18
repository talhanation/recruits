package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;

public class MessageToClientUpdateClaims implements RecruitsMessage<MessageToClientUpdateClaims> {
    private CompoundTag claimsListNBT;
    private int claimCost;
    private int chunkCost;
    private boolean cascadeOfCost;
    private boolean allowClaiming;
    private boolean fogOfWarEnabled;
    private ItemStack currencyItemStack;
    public MessageToClientUpdateClaims() {
    }

    public MessageToClientUpdateClaims(List<RecruitsClaim> list, int claimCost, int chunkCost, boolean cascadeOfCost, boolean allowClaiming, boolean fogOfWarEnabled, ItemStack currencyItemStack) {
        this.claimsListNBT = RecruitsClaim.toNBT(list);
        this.claimCost = claimCost;
        this.chunkCost = chunkCost;
        this.cascadeOfCost = cascadeOfCost;
        this.currencyItemStack = currencyItemStack;
        this.allowClaiming = allowClaiming;
        this.fogOfWarEnabled = fogOfWarEnabled;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.recruitsClaims = RecruitsClaim.getListFromNBT(claimsListNBT);
        ClientManager.configValueClaimCost = this.claimCost;
        ClientManager.configValueChunkCost = this.chunkCost;
        ClientManager.configValueCascadeClaimCost = this.cascadeOfCost;
        ClientManager.currencyItemStack = this.currencyItemStack;
        ClientManager.configValueIsClaimingAllowed = this.allowClaiming;
        ClientManager.configFogOfWarEnabled = this.fogOfWarEnabled;

        ClientManager.rebuildActiveSieges();
    }

    @Override
    public MessageToClientUpdateClaims fromBytes(FriendlyByteBuf buf) {
        this.claimsListNBT = buf.readNbt();
        this.claimCost = buf.readInt();
        this.chunkCost = buf.readInt();
        this.cascadeOfCost = buf.readBoolean();
        this.currencyItemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        this.allowClaiming = buf.readBoolean();
        this.fogOfWarEnabled = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.claimsListNBT);
        buf.writeInt(this.claimCost);
        buf.writeInt(this.chunkCost);
        buf.writeBoolean(this.cascadeOfCost);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, this.currencyItemStack == null ? ItemStack.EMPTY : this.currencyItemStack);
        buf.writeBoolean(this.allowClaiming);
        buf.writeBoolean(this.fogOfWarEnabled);
    }

}
