package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.ImageToast;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
@OnlyIn(Dist.CLIENT)
public class RecruitsToastManager {
    private static final ResourceLocation LETTER_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/letter.png");
    private static final ResourceLocation ALLY_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/ally.png");
    private static final ResourceLocation ENEMY_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/enemy.png");
    private static final ResourceLocation NEUTRAL_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/neutral.png");
    public static void setToastForPlayer(Images id, @Nullable Component title, @Nullable Component text){
        Toast toast = null;
        switch (id){
            case ALLY -> {
                toast = new ImageToast(ALLY_IMAGE, title, text);
            }
            case NEUTRAL -> {
                toast = new ImageToast(NEUTRAL_IMAGE, title, text);
            }
            case ENEMY -> {
                toast = new ImageToast(ENEMY_IMAGE, title, text);
            }
            default -> {
                toast = new ImageToast(LETTER_IMAGE, title, text);
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToasts().addToast(toast);
        //minecraft.getSoundManager().play(SimpleSoundInstance.forUI(, 1.0F));
    }
    @OnlyIn(Dist.CLIENT)
    public enum Images{
        LETTER,
        ALLY,
        NEUTRAL,
        ENEMY
    }

    public static Component TOAST_TO(String team){
        return new TranslatableComponent("gui.recruits.toast.to", team);
    }
    public static Component TOAST_FOR(String team){
        return new TranslatableComponent("gui.recruits.toast.for", team);
    }

    public static Component TOAST_FROM(String team){
        return new TranslatableComponent("gui.recruits.toast.from", team);
    }

    public static final Component TOAST_JOIN_REQUEST_TITLE = new TranslatableComponent("gui.recruits.toast.JoinRequestTitle");
    public static final Component TOAST_SENT_JOIN_REQUEST_TITLE = new TranslatableComponent("gui.recruits.toast.sendJoinRequestTitle");
    public static final Component TOAST_ENEMY_TITLE = new TranslatableComponent("gui.recruits.toast.enemyTitle").withStyle(ChatFormatting.RED);
    public static final Component TOAST_NEUTRAL_TITLE  = new TranslatableComponent("gui.recruits.toast.neutralTitle");
    public static final Component TOAST_ALLY_TITLE  = new TranslatableComponent("gui.recruits.toast.allyTitle").withStyle(ChatFormatting.GREEN);
    public static final Component TOAST_MESSENGER_ARRIVED_TITLE  = new TranslatableComponent("gui.recruits.toast.messengerArrivedTitle");
    public static final Component TOAST_RECRUIT_ASSIGNED_TITLE  = new TranslatableComponent("gui.recruits.toast.recruitAssignedTitle");



    public static Component TOAST_ENEMY_INFO(String s){
        return new TranslatableComponent("gui.recruits.toast.setYouAsEnemy", s).withStyle(ChatFormatting.RED);
    }
    public static Component TOAST_NEUTRAL_INFO(String s){
        return new TranslatableComponent("gui.recruits.toast.setYouAsNeutral", s);
    }
    public static Component TOAST_ALLY_INFO(String s) {
        return new TranslatableComponent("gui.recruits.toast.setYouAsAlly", s).withStyle(ChatFormatting.GREEN);
    }
    public static Component TOAST_ENEMY_SET(String s) {
        return new TranslatableComponent("gui.recruits.toast.setAsEnemy",s);
    }
    public static Component TOAST_NEUTRAL_SET(String s){
        return new TranslatableComponent("gui.recruits.toast.setAsNeutral",s);
    }
    public static Component TOAST_ALLY_SET(String s) {
        return new TranslatableComponent("gui.recruits.toast.setAsAlly", s);
    }

    public static Component TOAST_RECRUIT_ASSIGNED_INFO(String s) {
        return new TranslatableComponent("gui.recruits.toast.recruitAssigned", s);
    }

    public static Component TOAST_MESSENGER_ARRIVED_INFO(String s) {
        return new TranslatableComponent("gui.recruits.toast.messengerArrived", s);
    }
}
