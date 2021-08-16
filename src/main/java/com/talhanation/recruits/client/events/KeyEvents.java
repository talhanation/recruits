package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageHold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class KeyEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.R_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHold(clientPlayerEntity.getUUID()));
            clientPlayerEntity.sendMessage(new StringTextComponent("KEY_R"), clientPlayerEntity.getUUID());
            /*clientPlayerEntity.sendMessage(new StringTextComponent("Recruits! Stop Following me!"), clientPlayerEntity.getUUID());
            List<AbstractRecruitEntity> list = clientPlayerEntity.level.getEntitiesOfClass(AbstractRecruitEntity.class, clientPlayerEntity.getBoundingBox().inflate(32.0D));
            for (AbstractRecruitEntity recruits : list) {
                if (recruits.getFollow()) {
                   recruits.onRKeyPressed(clientPlayerEntity.getUUID());
                }
            }

             */


        }

        if (Main.X_KEY.isDown()) {
            clientPlayerEntity.sendMessage(new StringTextComponent("Recruits! Stop Following me!"), clientPlayerEntity.getUUID());
            List<AbstractRecruitEntity> list = clientPlayerEntity.level.getEntitiesOfClass(AbstractRecruitEntity.class, clientPlayerEntity.getBoundingBox().inflate(32.0D));
            for (AbstractRecruitEntity recruits : list) {
                recruits.kill();

            }
        }

    }

    public static void onRKeyPressed(UUID player, AbstractRecruitEntity recruit) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player)) {
            /*
            switch (state) {
                case 1:
            }*/

            recruit.setFollow(true);
            recruit.getOwner().sendMessage(new StringTextComponent("Recruits! Stop Following me!"), recruit.getOwnerUUID());
        }
    }



}
