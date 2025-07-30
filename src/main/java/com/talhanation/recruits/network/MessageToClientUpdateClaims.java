package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;

public class MessageToClientUpdateClaims implements Message<MessageToClientUpdateClaims> {
    private CompoundTag claimsListNBT;
    public MessageToClientUpdateClaims() {
    }

    public MessageToClientUpdateClaims(List<RecruitsClaim> list) {
        this.claimsListNBT = RecruitsClaim.toNBT(list);
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.recruitsClaims = RecruitsClaim.getListFromNBT(claimsListNBT);
    }

    @Override
    public MessageToClientUpdateClaims fromBytes(FriendlyByteBuf buf) {
        this.claimsListNBT = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.claimsListNBT);
    }

}
