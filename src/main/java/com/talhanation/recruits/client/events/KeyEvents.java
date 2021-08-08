package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageHold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ScoreTextComponent;
import net.minecraft.util.text.SelectorTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class KeyEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.ACTION_KEY.isDown()) {

            clientPlayerEntity.level.getEntitiesOfClass(AbstractRecruitEntity.class, clientPlayerEntity.getBoundingBox()
                            .inflate(20.0D), v -> v
                            .getUUID()
                            .equals(AbstractRecruitEntity.class))
                    .stream()
                    .filter(Entity::isAlive)
                    .findAny()
                    .ifPresent(recruit -> KeyEvents.holdRecruit(recruit, clientPlayerEntity));

        }
    }

    public static void holdRecruit(AbstractRecruitEntity recruit, PlayerEntity player) {
        if (recruit.isOwnedBy(player)) {
            recruit.onActionKeyPressed();
        }
    }
}
