package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
            clientPlayerEntity.sendMessage(new StringTextComponent("Recruits! Stop Following me!"), clientPlayerEntity.getUUID());
            List<AbstractRecruitEntity> list = clientPlayerEntity.level.getEntitiesOfClass(AbstractRecruitEntity.class, clientPlayerEntity.getBoundingBox().inflate(32.0D));
            for (AbstractRecruitEntity recruits : list) {
                if (!recruits.getStopFollow()) {
                    //onActionKeyPressed(clientPlayerEntity.getUUID());
                }
            }
        }
    }


}
