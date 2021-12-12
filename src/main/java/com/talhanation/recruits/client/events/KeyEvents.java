package com.talhanation.recruits.client.events;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class KeyEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;


        if (Main.R_KEY.isDown()) {
            CommandEvents.openCommandScreen(clientPlayerEntity);
        }
    /*
        if(Main.C_KEY.isDown()){
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMove(clientPlayerEntity.getUUID(), 0));
            //clientPlayerEntity.sendMessage(new StringTextComponent("Everyone! Move!"), clientPlayerEntity.getUUID());
            //clientPlayerEntity.sendMessage(new StringTextComponent("Mount!"), clientPlayerEntity.getUUID());
        }


     */

         /*
        if (Main.X_KEY.isDown()) {

            X_state++;
            if (X_state > 3) X_state = 0;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(clientPlayerEntity.getUUID(), X_state, group));
            CommandEvents.sendAggroCommandInChat(X_state, clientPlayerEntity);
        }


        if(Main.V_KEY.isDown()){
            group++;
            if (group > 4) group = 0;
            //CommandEvents.sendGroupInChat(group, clientPlayerEntity);
        }
        */

    }
}
