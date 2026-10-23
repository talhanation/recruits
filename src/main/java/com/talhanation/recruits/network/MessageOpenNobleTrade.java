package com.talhanation.recruits.network;

import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsVillageManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * Hauptscreen → „Anheuern“. Der Server prüft den Ruf und öffnet dann den bestehenden Trade-Screen.
 */
public class MessageOpenNobleTrade implements Message<MessageOpenNobleTrade> {

    private UUID nobleUUID;

    public MessageOpenNobleTrade() {
    }

    public MessageOpenNobleTrade(UUID nobleUUID) {
        this.nobleUUID = nobleUUID;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || VillageEvents.villageManager == null) return;

        VillagerNobleEntity noble = RecruitsVillageManager.findNoble(player, nobleUUID, 16.0D);
        if (noble == null) return;

        if (RecruitsVillageManager.hasEmbargo(player, noble)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.text.embargoBlocked", noble.getName().getString()));
            return;
        }

        if (VillageEvents.villageManager.isHostile(player, noble)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.hostile"));
            return;
        }

        // Aktuellen Ruf mitschicken, damit Preise und Sperren im Trade-Screen stimmen.
        VillageEvents.villageManager.sendInfo(player, noble, false);
        noble.openTradeGUI(player);
    }

    @Override
    public MessageOpenNobleTrade fromBytes(FriendlyByteBuf buf) {
        this.nobleUUID = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(nobleUUID);
    }
}
