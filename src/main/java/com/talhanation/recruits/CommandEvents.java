package com.talhanation.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.network.MessageCommandScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class CommandEvents {

    private static int recruitsInCommand;
    private static int currentGroup;

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

    public static void setRecruitsInCommand(int count) {
        recruitsInCommand = count;
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
}
