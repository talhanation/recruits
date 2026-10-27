package com.talhanation.recruits.network;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorGarrison;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.governor.GovernorUnitType;
import com.talhanation.recruits.world.RecruitsClaim;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * "Save" of the unit edit screen: wanted amount and the equipment template (main hand, off hand, armor)
 * of one unit type in one go. An empty item id means "keep what he has".
 */
public class MessageGovernorSetUnit implements Message<MessageGovernorSetUnit> {

    private UUID claim;
    private int type;
    private int amount;
    private String[] items = new String[ClaimGovernorData.LOADOUT_SLOTS];

    public MessageGovernorSetUnit() {
    }

    public MessageGovernorSetUnit(UUID claim, GovernorUnitType type, int amount, String[] items) {
        this.claim = claim;
        this.type = type.getIndex();
        this.amount = amount;
        for (int i = 0; i < ClaimGovernorData.LOADOUT_SLOTS; i++) {
            this.items[i] = i < items.length && items[i] != null ? items[i] : "";
        }
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
        RecruitsClaim recruitsClaim = GovernorManager.getClaim(claim);
        if (data == null || recruitsClaim == null) return;

        GovernorUnitType unitType = GovernorUnitType.fromIndex(type);
        int others = data.getTotalQuota() - data.getQuota(unitType);
        int max = Math.max(0, GovernorManager.getGarrisonCap(recruitsClaim) - others);
        data.setQuota(unitType, Math.max(0, Math.min(amount, max)));

        for (int i = 0; i < ClaimGovernorData.LOADOUT_SLOTS; i++) {
            String item = items[i];
            if (!item.isEmpty() && GovernorGarrison.getItem(item) == null) continue;
            data.setLoadout(unitType, i, item);
        }
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorSetUnit fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.type = buf.readVarInt();
        this.amount = buf.readVarInt();
        for (int i = 0; i < ClaimGovernorData.LOADOUT_SLOTS; i++) {
            this.items[i] = buf.readUtf(256);
        }
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeVarInt(type);
        buf.writeVarInt(amount);
        for (int i = 0; i < ClaimGovernorData.LOADOUT_SLOTS; i++) {
            buf.writeUtf(items[i] == null ? "" : items[i], 256);
        }
    }
}
