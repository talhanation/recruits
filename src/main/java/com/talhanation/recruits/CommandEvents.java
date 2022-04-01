package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.network.MessageCommandScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class CommandEvents {

    private static int recruitsInCommand;
    public static int currentGroup;

    public static void onRKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int r_state, int group, boolean fromGui) {
        if (recruit.isTame() && (recruit.getListen() || fromGui) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            int state = recruit.getFollowState();
            switch (r_state) {

                case 0:
                    if (state != 0)
                        recruit.setFollowState(0);
                    break;

                case 1:
                    if (state != 1)
                        recruit.setFollowState(1);
                    break;

                case 2:
                    if (state != 2)
                        recruit.setFollowState(2);
                    break;

                case 3:
                    if (state != 3)
                        recruit.setFollowState(3);
                    break;

                case 4:
                    if (state != 4)
                        recruit.setFollowState(4);
                    break;
            }
        }
    }

    public static void onXKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int x_state, int group, boolean fromGui) {
        if (recruit.isTame() &&(recruit.getListen() || fromGui) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
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

    public static void openCommandScreen(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {

                @Override
                public ITextComponent getDisplayName() {
                    return new StringTextComponent("command_screen") {
                    };
                }

                @Nullable
                @Override
                public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                    return new CommandContainer(i, playerEntity);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(player.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandScreen(player));
        }
    }


    public static void sendFollowCommandInChat(int state, LivingEntity owner){
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

            case 3:
                owner.sendMessage(new StringTextComponent("Back to your Position!"), owner.getUUID());
                break;

            case 4:
                owner.sendMessage(new StringTextComponent("To my Position!"), owner.getUUID());
                break;
        }
    }

    public static void sendAggroCommandInChat(int state, LivingEntity owner){
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

    public static void setRecruitsInCommand(AbstractRecruitEntity recruit, int count) {
        LivingEntity living = recruit.getOwner();
        PlayerEntity player = (PlayerEntity) living;
        if (player != null){

            CompoundNBT playerNBT = player.getPersistentData();
            CompoundNBT nbt = playerNBT.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

            nbt.putInt( "RecruitsInCommand", count);
            player.sendMessage(new StringTextComponent("EVENT int: " + count), player.getUUID());

            playerNBT.put(PlayerEntity.PERSISTED_NBT_TAG, nbt);
        }
    }

    public static int getRecruitsInCommand() {
        return recruitsInCommand;
    }

    public static void setCurrentGroup(int group) {
        currentGroup = group;
    }

    public static int getCurrentGroup() {
        return currentGroup;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        CompoundNBT playerData = event.getPlayer().getPersistentData();
        CompoundNBT data = playerData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
            if (!data.contains("MaxRecruits")) data.putInt("MaxRecruits", RecruitsModConfig.MaxRecruitsForPlayer.get());
            if (!data.contains("CommandingGroup")) data.putInt("CommandingGroup", 0);
            if (!data.contains("TotalRecruits")) data.putInt("TotalRecruits", 0);

            playerData.put(PlayerEntity.PERSISTED_NBT_TAG, data);
    }
    /*
    @SubscribeEvent
    public void onPlayerLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;

            CompoundNBT playerData = player.getPersistentData();
            CompoundNBT data = playerData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);

            if (player.isCrouching())
                player.sendMessage(new StringTextComponent("NBT: " + data.getInt("RecruitsInCommand")), player.getUUID());
        }
    }
     */

    public static int getSavedRecruitCount(PlayerEntity player) {
        CompoundNBT playerNBT = player.getPersistentData();
        CompoundNBT nbt = playerNBT.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        //player.sendMessage(new StringTextComponent("getSavedCount: " + nbt.getInt("TotalRecruits")), player.getUUID());
        return nbt.getInt("TotalRecruits");
    }

    public static void saveRecruitCount(PlayerEntity player, int count) {
        CompoundNBT playerNBT = player.getPersistentData();
        CompoundNBT nbt = playerNBT.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        //player.sendMessage(new StringTextComponent("savedCount: " + count), player.getUUID());

        nbt.putInt( "TotalRecruits", count);
        playerNBT.put(PlayerEntity.PERSISTED_NBT_TAG, nbt);
    }
}
