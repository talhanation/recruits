package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageFollow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;
import java.util.UUID;

public class KeyEvents {

    public static int state_R;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.R_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(clientPlayerEntity.getUUID()));
            state_R++;
        }


        if (Main.X_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(clientPlayerEntity.getUUID()));
        }

    }

    public static void onRKeyPressed(UUID player, AbstractRecruitEntity recruit) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player)) {
            if(state_R == 0){
                recruit.setFollow(true);
            }
            else {
                recruit.setFollow(false);
                state_R = 0;
            }
        }
    }

    public static void onXKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player_uuid)) {
            int state = recruit.getState();
            LivingEntity player = recruit.getOwner();

            switch (state) {
                case 0:
                    recruit.setState(player, 1);

                    break;
                case 1:
                    recruit.setState(player, 2);
                    break;
                case 2:
                    recruit.setState(player, 0);
                    break;
            }
        }
    }

}
