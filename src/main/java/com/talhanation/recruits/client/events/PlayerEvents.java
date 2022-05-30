package com.talhanation.recruits.client.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityViewRenderEvent;
@OnlyIn(Dist.CLIENT)
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
