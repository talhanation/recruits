package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.ImageToast;
import com.talhanation.recruits.client.gui.component.RecruitsTeamImageToast;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
@OnlyIn(Dist.CLIENT)
public class RecruitsToastManager {
    private static final ResourceLocation LETTER_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/letter.png");
    private static final ResourceLocation ALLY_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/ally.png");
    private static final ResourceLocation ENEMY_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/enemy.png");
    private static final ResourceLocation NEUTRAL_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/neutral.png");
    private static final ResourceLocation CROWN_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/image/leader_crown.png");
    public static Images savedTeamForPlayer;//dirty fix calling twice bug
    public static Images savedForPlayer;//dirty fix for calling twice bug
    public static void setTeamToastForPlayer(Images id, @Nullable Component title, @Nullable Component text, RecruitsTeam team){
        if(savedTeamForPlayer == id){
            savedTeamForPlayer = null;
            return;
        }
        else{
            savedTeamForPlayer = id;
        }

        Toast toast;
        switch (id) {
            case ALLY -> {
                toast = new RecruitsTeamImageToast(ALLY_IMAGE, title, text, team);
            }
            case NEUTRAL -> {
                toast = new RecruitsTeamImageToast(NEUTRAL_IMAGE, title, text, team);
            }
            case ENEMY -> {
                toast = new RecruitsTeamImageToast(ENEMY_IMAGE, title, text, team);
            }
            case TEAM_JOIN -> {
                toast = new RecruitsTeamImageToast(null, title, text, team);
            }
            case CROWN -> {
                toast = new RecruitsTeamImageToast(CROWN_IMAGE, title, text, team);
            }
            default -> {
                toast = new RecruitsTeamImageToast(LETTER_IMAGE, title, text, team);
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToasts().addToast(toast);
    }
    public static void setToastForPlayer(Images id, @Nullable Component title, @Nullable Component text){
        if(savedForPlayer == id){
            savedForPlayer = null;
            return;
        }
        else{
            savedForPlayer = id;
        }

        Toast toast;
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
            case CROWN -> {
                toast = new ImageToast(CROWN_IMAGE, title, text);
            }
            default -> {
                toast = new ImageToast(LETTER_IMAGE, title, text);
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToasts().addToast(toast);
    }

    @OnlyIn(Dist.CLIENT)
    public enum Images{
        NONE,
        LETTER,
        ALLY,
        NEUTRAL,
        ENEMY,
        CROWN,
        TEAM_JOIN
    }

    public static Component TOAST_TO(String team){
        return Component.translatable("gui.recruits.toast.to", team);
    }
    public static Component TOAST_FOR(String team){
        return Component.translatable("gui.recruits.toast.for", team);
    }

    public static Component TOAST_FROM(String team){
        return Component.translatable("gui.recruits.toast.from", team);
    }
    public static final Component TOAST_PLAYER_JOINED_TITLE = Component.translatable("gui.recruits.toast.playerJoinedTeamTitle");
    public static final Component TOAST_TEAM_JOINED_TITLE = Component.translatable("gui.recruits.toast.teamJoinedTitle");
    public static final Component TOAST_JOIN_REQUEST_TITLE = Component.translatable("gui.recruits.toast.JoinRequestTitle");
    public static final Component TOAST_SENT_JOIN_REQUEST_TITLE = Component.translatable("gui.recruits.toast.sendJoinRequestTitle");
    public static final Component TOAST_ENEMY_TITLE = Component.translatable("gui.recruits.toast.enemyTitle").withStyle(ChatFormatting.RED);
    public static final Component TOAST_NEUTRAL_TITLE  = Component.translatable("gui.recruits.toast.neutralTitle");
    public static final Component TOAST_ALLY_TITLE  = Component.translatable("gui.recruits.toast.allyTitle").withStyle(ChatFormatting.GREEN);
    public static final Component TOAST_MESSENGER_ARRIVED_TITLE  = Component.translatable("gui.recruits.toast.messengerArrivedTitle");
    public static final Component TOAST_RECRUIT_ASSIGNED_TITLE  = Component.translatable("gui.recruits.toast.recruitAssignedTitle");

    public static final Component TOAST_NEW_LEADER_TITLE = Component.translatable("gui.recruits.toast.newTeamLeaderTitle");
    public static final Component TOAST_NEW_FACTION_NAME_TITLE = Component.translatable("gui.recruits.toast.newFactionNameTitle");
    public static final Component TOAST_NEW_BANNER_TITLE = Component.translatable("gui.recruits.toast.newFactionBannerTitle");

    public static Component TOAST_PLAYER_JOINED_TEAM(String s){
        return Component.translatable("gui.recruits.toast.playerJoinedTeam", s);
    }
    public static Component TOAST_NEW_LEADER(String s){
        return Component.translatable("gui.recruits.toast.newLeader", s);
    }
    public static Component TOAST_WANTS_TO_JOIN(String s){
        return Component.translatable("gui.recruits.toast.wantsToJoin", s);
    }
    public static Component TOAST_ENEMY_INFO(String s){
        return Component.translatable("gui.recruits.toast.setYouAsEnemy", s).withStyle(ChatFormatting.RED);
    }
    public static Component TOAST_NEUTRAL_INFO(String s){
        return Component.translatable("gui.recruits.toast.setYouAsNeutral", s);
    }
    public static Component TOAST_ALLY_INFO(String s) {
        return Component.translatable("gui.recruits.toast.setYouAsAlly", s).withStyle(ChatFormatting.GREEN);
    }
    public static Component TOAST_ENEMY_SET(String s) {
        return Component.translatable("gui.recruits.toast.setAsEnemy",s);
    }
    public static Component TOAST_NEUTRAL_SET(String s){
        return Component.translatable("gui.recruits.toast.setAsNeutral",s);
    }
    public static Component TOAST_ALLY_SET(String s) {
        return Component.translatable("gui.recruits.toast.setAsAlly", s);
    }

    public static Component TOAST_RECRUIT_ASSIGNED_INFO(String s) {
        return Component.translatable("gui.recruits.toast.recruitAssigned", s);
    }

    public static Component TOAST_MESSENGER_ARRIVED_INFO(String s) {
        return Component.translatable("gui.recruits.toast.messengerArrived", s);
    }
    public static Component TOAST_TEAM_JOINED(String s){
        return Component.translatable("gui.recruits.toast.teamJoined", s);
    }

    public static Component TOAST_NEW_FACTION_NAME(String s){
        return Component.translatable("gui.recruits.toast.newName", s);
    }

    public static Component TOAST_NEW_BANNER(){
        return Component.translatable("gui.recruits.toast.newBanner");
    }
}
