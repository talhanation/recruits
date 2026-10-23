package com.talhanation.recruits.network;

import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsVillageManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * Aufträge: annehmen, abgeben, abbrechen. Danach bekommt der Client die aktuelle Info zurück.
 */
public class MessageNobleQuestAction implements Message<MessageNobleQuestAction> {

    public enum Action {
        ACCEPT,
        TURN_IN,
        ABANDON
    }

    private UUID nobleUUID;
    private Action action;
    private UUID offerId;

    public MessageNobleQuestAction() {
    }

    public MessageNobleQuestAction(UUID nobleUUID, Action action, UUID offerId) {
        this.nobleUUID = nobleUUID;
        this.action = action;
        this.offerId = offerId;
    }

    public static MessageNobleQuestAction accept(UUID nobleUUID, UUID offerId) {
        return new MessageNobleQuestAction(nobleUUID, Action.ACCEPT, offerId);
    }

    public static MessageNobleQuestAction turnIn(UUID nobleUUID) {
        return new MessageNobleQuestAction(nobleUUID, Action.TURN_IN, new UUID(0L, 0L));
    }

    public static MessageNobleQuestAction abandon(UUID nobleUUID) {
        return new MessageNobleQuestAction(nobleUUID, Action.ABANDON, new UUID(0L, 0L));
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

        switch (action) {
            case ACCEPT -> VillageEvents.villageManager.acceptQuest(player, noble, offerId);
            case TURN_IN -> VillageEvents.villageManager.turnInQuest(player, noble);
            case ABANDON -> VillageEvents.villageManager.abandonQuest(player);
        }

        VillageEvents.villageManager.sendInfo(player, noble, false);
    }

    @Override
    public MessageNobleQuestAction fromBytes(FriendlyByteBuf buf) {
        this.nobleUUID = buf.readUUID();
        this.action = buf.readEnum(Action.class);
        this.offerId = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(nobleUUID);
        buf.writeEnum(action);
        buf.writeUUID(offerId);
    }
}
