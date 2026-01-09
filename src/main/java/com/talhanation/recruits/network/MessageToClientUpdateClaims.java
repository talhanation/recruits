package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;

public class MessageToClientUpdateClaims implements Message<MessageToClientUpdateClaims> {
    private CompoundTag claimsListNBT;
    private int claimCost;
    private int chunkCost;
    private boolean cascadeOfCost;
    private boolean allowClaiming;
    private ItemStack currencyItemStack;
    public MessageToClientUpdateClaims() {
    }

    public MessageToClientUpdateClaims(List<RecruitsClaim> list, int claimCost, int chunkCost, boolean cascadeOfCost, boolean allowClaiming, ItemStack currencyItemStack) {
        this.claimsListNBT = RecruitsClaim.toNBT(list);
        this.claimCost = claimCost;
        this.chunkCost = chunkCost;
        this.cascadeOfCost = cascadeOfCost;
        this.currencyItemStack = currencyItemStack;
        this.allowClaiming = allowClaiming;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.recruitsClaims = RecruitsClaim.getListFromNBT(claimsListNBT);
        ClientManager.configValueClaimCost = this.claimCost;
        ClientManager.configValueChunkCost = this.chunkCost;
        ClientManager.configValueCascadeClaimCost = this.cascadeOfCost;
        ClientManager.currencyItemStack = this.currencyItemStack;
        ClientManager.configValueIsClaimingAllowed = this.allowClaiming;
    }

    @Override
    public MessageToClientUpdateClaims fromBytes(FriendlyByteBuf buf) {
        this.claimsListNBT = buf.readNbt();
        this.claimCost = buf.readInt();
        this.chunkCost = buf.readInt();
        this.cascadeOfCost = buf.readBoolean();
        this.currencyItemStack = buf.readItem();
        this.allowClaiming = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.claimsListNBT);
        buf.writeInt(this.claimCost);
        buf.writeInt(this.chunkCost);
        buf.writeBoolean(this.cascadeOfCost);
        buf.writeItemStack(this.currencyItemStack, false);
        buf.writeBoolean(this.allowClaiming);
    }

}
