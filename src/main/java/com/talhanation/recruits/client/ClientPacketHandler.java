package com.talhanation.recruits.client;

import com.talhanation.recruits.client.gui.MessengerAnswerScreen;
import com.talhanation.recruits.client.gui.MessengerTreatyAnswerScreen;
import com.talhanation.recruits.client.gui.NobleTradeScreen;
import com.talhanation.recruits.client.gui.faction.TakeOverScreen;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Client only part of clientbound packets.
 * <p>
 * NeoForge payload registration instantiates every message on both sides, so a message class must not
 * contain code that makes the bytecode verifier load client classes (e.g. LocalPlayer as Player) -
 * that crashes dedicated servers. The message classes only call these static methods.
 */
public class ClientPacketHandler {

    public static void openMessengerAnswerScreen(UUID messengerUUID, String message, CompoundTag playerInfo) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCommandSenderWorld().getEntitiesOfClass(MessengerEntity.class, player.getBoundingBox().inflate(16.0D), v -> v.getUUID().equals(messengerUUID))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(messenger -> Minecraft.getInstance().setScreen(new MessengerAnswerScreen(messenger, player, message, RecruitsPlayerInfo.getFromNBT(playerInfo))));
    }

    public static void openTreatyAnswerScreen(UUID messengerUUID, int durationHours, CompoundTag playerInfo) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCommandSenderWorld().getEntitiesOfClass(MessengerEntity.class, player.getBoundingBox().inflate(16.0D), v -> v.getUUID().equals(messengerUUID))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(messenger -> Minecraft.getInstance().setScreen(new MessengerTreatyAnswerScreen(messenger, player, durationHours, RecruitsPlayerInfo.getFromNBT(playerInfo))));
    }

    public static void openNobleTradeScreen(UUID nobleUUID) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCommandSenderWorld().getEntitiesOfClass(VillagerNobleEntity.class, player.getBoundingBox().inflate(16.0D), v -> v.getUUID().equals(nobleUUID))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(nobleVillager -> Minecraft.getInstance().setScreen(new NobleTradeScreen(nobleVillager, player)));
    }

    public static void openTakeOverScreen(UUID recruitUUID) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(16.0D), v -> v.getUUID().equals(recruitUUID))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(recruit -> Minecraft.getInstance().setScreen(new TakeOverScreen(recruit, player)));
    }

    public static void setWorldMapIdentity(UUID worldId) {
        WorldMapStorageId.setServerWorldId(worldId);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            WorldMapCacheManager.getInstance().initialize(mc.level);
        }
        ClientManager.loadRoutes();
        ClientManager.loadSpecialStates();
    }
}
