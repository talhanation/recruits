package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.claim.WorldMapClaimIndex;
import com.talhanation.recruits.network.codec.ClaimNetworkCodec;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageToClientUpdateClaims implements Message<MessageToClientUpdateClaims> {
    private List<RecruitsClaim> claims = Collections.emptyList();
    private int claimCost;
    private int chunkCost;
    private int maxClaimChunks;
    private boolean cascadeOfCost;
    private boolean allowClaiming;
    private boolean fogOfWarEnabled;
    private ItemStack currencyItemStack;
    private boolean resetClaims = true;
    private boolean syncComplete = true;

    public MessageToClientUpdateClaims() {
    }

    public MessageToClientUpdateClaims(List<RecruitsClaim> list, int claimCost, int chunkCost, int maxClaimChunks, boolean cascadeOfCost, boolean allowClaiming, boolean fogOfWarEnabled, ItemStack currencyItemStack) {
        this(list, claimCost, chunkCost, maxClaimChunks, cascadeOfCost, allowClaiming, fogOfWarEnabled, currencyItemStack, true, true);
    }

    public MessageToClientUpdateClaims(
            List<RecruitsClaim> list,
            int claimCost,
            int chunkCost,
            int maxClaimChunks,
            boolean cascadeOfCost,
            boolean allowClaiming,
            boolean fogOfWarEnabled,
            ItemStack currencyItemStack,
            boolean resetClaims,
            boolean syncComplete) {
        this.claims = list == null ? Collections.emptyList() : new ArrayList<>(list);
        this.claimCost = claimCost;
        this.chunkCost = chunkCost;
        this.maxClaimChunks = maxClaimChunks;
        this.cascadeOfCost = cascadeOfCost;
        this.currencyItemStack = currencyItemStack;
        this.allowClaiming = allowClaiming;
        this.fogOfWarEnabled = fogOfWarEnabled;
        this.resetClaims = resetClaims;
        this.syncComplete = syncComplete;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        if (resetClaims) {
            ClientManager.recruitsClaims = new ArrayList<>(this.claims);
            ClientManager.activeSiegeClaims.clear();
        } else {
            ClientManager.recruitsClaims.addAll(this.claims);
        }
        WorldMapClaimIndex.invalidate();
        ClientManager.configValueClaimCost = this.claimCost;
        ClientManager.configValueChunkCost = this.chunkCost;
        ClientManager.configValueMaxClaimChunks = this.maxClaimChunks;
        ClientManager.configValueCascadeClaimCost = this.cascadeOfCost;
        ClientManager.currencyItemStack = this.currencyItemStack;
        ClientManager.configValueIsClaimingAllowed = this.allowClaiming;
        ClientManager.configFogOfWarEnabled = this.fogOfWarEnabled;

        if (syncComplete) {
            ClientManager.rebuildActiveSieges();
        }
    }

    @Override
    public MessageToClientUpdateClaims fromBytes(FriendlyByteBuf buf) {
        this.claims = ClaimNetworkCodec.readClaimList(buf);
        this.claimCost = buf.readInt();
        this.chunkCost = buf.readInt();
        this.maxClaimChunks = buf.readInt();
        this.cascadeOfCost = buf.readBoolean();
        this.currencyItemStack = buf.readItem();
        this.allowClaiming = buf.readBoolean();
        this.fogOfWarEnabled = buf.readBoolean();
        this.resetClaims = buf.readBoolean();
        this.syncComplete = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        ClaimNetworkCodec.writeClaimList(buf, this.claims);
        buf.writeInt(this.claimCost);
        buf.writeInt(this.chunkCost);
        buf.writeInt(this.maxClaimChunks);
        buf.writeBoolean(this.cascadeOfCost);
        buf.writeItemStack(this.currencyItemStack, false);
        buf.writeBoolean(this.allowClaiming);
        buf.writeBoolean(this.fogOfWarEnabled);
        buf.writeBoolean(this.resetClaims);
        buf.writeBoolean(this.syncComplete);
    }

}
