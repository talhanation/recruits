package com.talhanation.recruits;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DamageEvent {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityHurt(LivingHurtEvent event) {
        if (!event.isCanceled()) {
            LivingEntity entity = event.getEntity();
            if (entity.getLevel().isClientSide()) {
                return;
            }
            DamageSource source = event.getSource();
            Entity directEntity = source.getDirectEntity();
            ResourceLocation resourceLocation = directEntity != null ? EntityType.getKey(directEntity.getType()) : null;

            /*
            if (NodamiConfig.CORE.excludePlayers && entity instanceof Player) {
                return;
            }

            if (NodamiConfig.CORE.excludeAllMobs && !(entity instanceof Player)) {
                return;
            }
            */

            ResourceLocation location = EntityType.getKey(entity.getType());
            /*
            if (location != null && NodamiConfig.EXCLUSIONS.dmgReceiveExcludedEntities.contains(location.toString())) {
                return;
            }



            if (NodamiConfig.EXCLUSIONS.damageSrcWhitelist.contains(source.getMsgId())) {
                return;
            }

            if (directEntity != null) {
                if (resourceLocation != null && NodamiConfig.EXCLUSIONS.attackExcludedEntities.contains(resourceLocation.toString())) {
                    return;
                }
            }
             */
            entity.invulnerableTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!event.isCanceled()) {
            Player player = event.getEntity();
            if (player.getLevel().isClientSide()) {
                return;
            }
            float str = player.getAttackStrengthScale(0);
            if (str <= 0.1) {
                event.setCanceled(true);
                return;
            }
            if (str <= 0.75) {
                Entity target = event.getTarget();
                if (target != null && target instanceof LivingEntity) {
                    ((LivingEntity)target).swinging = true;
                }
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!event.isCanceled()) {
            LivingEntity entity = event.getEntity();
            if (entity.swinging) {
                event.setCanceled(true);
                entity.swinging = false;
            }
        }

    }
}