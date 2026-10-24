package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorGarrison;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.governor.GovernorUnitType;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageGovernorSetLoadout implements Message<MessageGovernorSetLoadout> {

    private UUID claim;
    private int type;
    private int slot;
    private String item;

    public MessageGovernorSetLoadout() {
    }

    /** item = registry id of the template item, "" clears the slot ("egal"). */
    public MessageGovernorSetLoadout(UUID claim, GovernorUnitType type, int slot, String item) {
        this.claim = claim;
        this.type = type.getIndex();
        this.slot = slot;
        this.item = item;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ClaimGovernorData data = GovernorPackets.access(player, claim, true);
        if (data == null) return;
        if (slot < 0 || slot >= ClaimGovernorData.LOADOUT_SLOTS) return;
        if (!item.isEmpty() && GovernorGarrison.getItem(item) == null) return;

        data.setLoadout(GovernorUnitType.fromIndex(type), slot, item);
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorSetLoadout fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.type = buf.readInt();
        this.slot = buf.readInt();
        this.item = buf.readUtf(256);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeInt(type);
        buf.writeInt(slot);
        buf.writeUtf(item, 256);
    }
}
