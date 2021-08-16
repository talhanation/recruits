package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageFollow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class KeyEvents {

    public boolean wasRPressed;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.R_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(clientPlayerEntity.getUUID()));
        }

        if (Main.X_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(clientPlayerEntity.getUUID()));
        }

    }

    public static void onRKeyPressed(UUID player, AbstractRecruitEntity recruit) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player)) {
                recruit.setFollow(true);

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
