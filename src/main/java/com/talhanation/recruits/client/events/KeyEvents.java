package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageClearTarget;
import com.talhanation.recruits.network.MessageFollow;
import com.talhanation.recruits.network.MessageMove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;
import java.util.UUID;


public class KeyEvents {


    public int R_state;
    public int X_state;
    public int group;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.X_KEY.isDown()) {

            X_state++;
            if (X_state > 3) X_state = 0;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(clientPlayerEntity.getUUID(), X_state, group));
            sendXCommandInChat(X_state, clientPlayerEntity);
        }

        if (Main.R_KEY.isDown()) {

            R_state++;
            if (R_state > 3) R_state = 0;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(clientPlayerEntity.getUUID(), R_state, group));
            sendRCommandInChat(R_state, clientPlayerEntity);
        }

        if(Main.C_KEY.isDown()){
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMove(clientPlayerEntity.getUUID(), group));
            //clientPlayerEntity.sendMessage(new StringTextComponent("Everyone! Move!"), clientPlayerEntity.getUUID());
            //clientPlayerEntity.sendMessage(new StringTextComponent("Mount!"), clientPlayerEntity.getUUID());
        }

        if(Main.Y_KEY.isDown()){
            clientPlayerEntity.sendMessage(new StringTextComponent("Stop!"), clientPlayerEntity.getUUID());
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(clientPlayerEntity.getUUID(), group));
        }

        if(Main.V_KEY.isDown()){
            group++;
            if (group > 4) group = 0;
            sendGroupInChat(group, clientPlayerEntity);
        }

    }

    public static void onRKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int r_state, int group) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            int state = recruit.getFollow();
            switch (r_state) {

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

    public static void onXKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int x_state, int group) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            int state = recruit.getState();
            switch (x_state) {

                case 0:
                    if (state != 0)
                        recruit.setState(0);
                    break;

                case 1:
                    if (state != 1)
                        recruit.setState(1);
                    break;

                case 2:
                    if (state != 2)
                        recruit.setState(2);
                    break;
            }
        }
    }

    public static void onCKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        /*
        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity owner = recruit.getOwner();
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player_uuid)) {
            int state = recruit.getFollow();

            if (state != 2){
                RayTraceResult rayTraceResult = minecraft.hitResult;
                if (rayTraceResult != null) {
                    if (rayTraceResult.getType() == RayTraceResult.Type.BLOCK) {
                        BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) rayTraceResult;
                        BlockPos blockpos = blockraytraceresult.getBlockPos();
                        recruit.setMovePos(blockpos);
                        recruit.setMove(true);
                    }
                    else if (rayTraceResult.getType() == RayTraceResult.Type.ENTITY){
                        Entity crosshairEntity = minecraft.crosshairPickEntity;
                        if (crosshairEntity != null){
                            recruit.setMount(crosshairEntity.getUUID());
                        }

                    }
                }

            }
        }
*/
    }

    public static void onYKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isTame() &&  Objects.equals(recruit.getOwnerUUID(), player_uuid) && recruit.getGroup() == group) {
            LivingEntity owner = recruit.getOwner();
            recruit.clearTarget();
        }
    }

    public void sendRCommandInChat(int state, LivingEntity owner){
        switch (state) {
            case 0:
                owner.sendMessage(new StringTextComponent("Release!"), owner.getUUID());
                break;

            case 1:
                owner.sendMessage(new StringTextComponent("Follow me!"), owner.getUUID());
                break;

            case 2:
                owner.sendMessage(new StringTextComponent("Hold your Position!"), owner.getUUID());
                break;
        }
    }

    public void sendXCommandInChat(int state, LivingEntity owner){
        switch (state) {
            case 0:
                owner.sendMessage(new StringTextComponent("Stay Neutral!"), owner.getUUID());
                break;

            case 1:
                owner.sendMessage(new StringTextComponent("Stay Aggressive!"), owner.getUUID());
                break;

            case 2:
                owner.sendMessage(new StringTextComponent("Raid!"), owner.getUUID());
                break;
        }
    }

    public void sendGroupInChat(int state, LivingEntity owner){
        switch (state) {
            case 0:
                owner.sendMessage(new StringTextComponent("Everyone!"), owner.getUUID());
                break;
            case 1:
                owner.sendMessage(new StringTextComponent("Group 1!"), owner.getUUID());
                break;
            case 2:
                owner.sendMessage(new StringTextComponent("Group 2!"), owner.getUUID());
                break;
            case 3:
                owner.sendMessage(new StringTextComponent("Group 3!"), owner.getUUID());
                break;

        }
    }

}
