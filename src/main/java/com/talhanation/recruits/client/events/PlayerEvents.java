package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerEvents {

    public void RenderRecruitNameTag(EntityViewRenderEvent event){

    }

    /*
    @SubscribeEvent
    public void onInteractwithPassenger(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractRecruitEntity) ) {
            return;
        }

        Entity recruit = event.getTarget();
        PlayerEntity player = event.getPlayer();

        if (!player.isShiftKeyDown()) {
            return;
        }

        Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitGui(player, recruit.getUUID()));
        event.setCancellationResult(ActionResultType.SUCCESS);
        event.setCanceled(true);

    }
    */



}
