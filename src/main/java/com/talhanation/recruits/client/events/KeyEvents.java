package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageFollow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;
import java.util.UUID;

public class KeyEvents {

    public static int X_state;
    public static int R_state;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.R_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(clientPlayerEntity.getUUID()));
            R_state++;
            if (R_state > 3) R_state = 0;
            sendRCommandInChat(R_state, clientPlayerEntity);

        }


        if (Main.X_KEY.isDown()) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(clientPlayerEntity.getUUID()));
            X_state++;
            if (X_state > 3) X_state = 0;
            sendXCommandInChat(X_state, clientPlayerEntity);
        }

    }

    public static void onRKeyPressed(UUID player, AbstractRecruitEntity recruit) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player)) {
            int state = recruit.getFollow();
            switch (R_state) {

                case 0:
                    if (state != 0)
                        recruit.setFollow(0);
                    break;

                case 1:
                    if (state != 1)
                        recruit.setFollow(1);
                    break;

                case 2:
                    if (state != 2)
                        recruit.setFollow(2);
                    break;


            }
        }
    }

    public static void onXKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player_uuid)) {
            int state = recruit.getState();
            LivingEntity owner = recruit.getOwner();

            switch (X_state) {
                case 0:
                    if (state != 0)
                        recruit.setState(owner, 0);
                    break;

                case 1:
                    if (state != 1)
                        recruit.setState(owner, 1);
                    break;

                case 2:
                    if (state != 2)
                        recruit.setState(owner, 2);
                    break;
            }
        }
    }

    public void sendRCommandInChat(int state, LivingEntity owner){
        switch (state) {
            case 0:
                owner.sendMessage(new StringTextComponent("Everyone! Release!"), owner.getUUID());
                break;

            case 1:
                owner.sendMessage(new StringTextComponent("Everyone! Follow me!"), owner.getUUID());
                break;

            case 2:
                owner.sendMessage(new StringTextComponent("Everyone! Hold Position!"), owner.getUUID());
                break;


        }
    }

    public void sendXCommandInChat(int state, LivingEntity owner){
        switch (state) {
            case 0:
                owner.sendMessage(new StringTextComponent("Everyone! Stay Neutral!"), owner.getUUID());
                break;

            case 1:
                owner.sendMessage(new StringTextComponent("Everyone! Stay Aggressive!"), owner.getUUID());
                break;

            case 2:
                owner.sendMessage(new StringTextComponent("Everyone! Raid!"), owner.getUUID());
                break;
        }
    }

}
