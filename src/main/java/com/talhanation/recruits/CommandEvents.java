package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.network.MessageCommandScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class CommandEvents {

    private static int recruitsInCommand;
    public static int currentGroup;
    public static final TranslatableComponent TEXT_HIRE_COSTS = new TranslatableComponent("chat.recruits.text.hire_costs");
    public static final TranslatableComponent TEXT_EVERYONE = new TranslatableComponent("chat.recruits.text.everyone");
    public static final TranslatableComponent TEXT_GROUP = new TranslatableComponent("chat.recruits.text.group");

    public static final TranslatableComponent TEXT_PASSIVE = new TranslatableComponent("chat.recruits.command.passive");
    public static final TranslatableComponent TEXT_NEUTRAL = new TranslatableComponent("chat.recruits.command.neutral");
    public static final TranslatableComponent TEXT_AGGRESSIVE = new TranslatableComponent("chat.recruits.command.aggressive");
    public static final TranslatableComponent TEXT_RAID = new TranslatableComponent("chat.recruits.command.raid");

    public static final TranslatableComponent TEXT_FOLLOW = new TranslatableComponent("chat.recruits.command.follow");
    public static final TranslatableComponent TEXT_WANDER = new TranslatableComponent("chat.recruits.command.wander");
    public static final TranslatableComponent TEXT_HOLD_POS = new TranslatableComponent("chat.recruits.command.holdPos");
    public static final TranslatableComponent TEXT_HOLD_MY_POS = new TranslatableComponent("chat.recruits.command.holdMyPos");
    public static final TranslatableComponent TEXT_BACK_TO_POS = new TranslatableComponent("chat.recruits.command.backToPos");
    public static final TranslatableComponent TEXT_DISMOUNT = new TranslatableComponent("chat.recruits.command.dismount");
    public static final TranslatableComponent TEXT_MOUNT = new TranslatableComponent("chat.recruits.command.mount");
    public static final TranslatableComponent TEXT_ESCORT = new TranslatableComponent("chat.recruits.command.escort");


    public static void onRKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int r_state, int group, boolean fromGui) {
        if (recruit.isOwned() && (recruit.getListen() || fromGui) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
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

                case 5:
                    if (state != 5)
                        recruit.setFollowState(5);
                    break;
            }
        }
    }

    public static void onXKeyPressed(UUID player_uuid, AbstractRecruitEntity recruit, int x_state, int group, boolean fromGui) {
        if (recruit.isOwned() &&(recruit.getListen() || fromGui) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
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

                case 3:
                    if (state != 3)
                        recruit.setState(3);
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

    public static void openCommandScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return new TranslatableComponent("command_screen") {
                    };
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new CommandContainer(i, playerEntity);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(player.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageCommandScreen(player));
        }
    }


    public static void sendFollowCommandInChat(int state, LivingEntity owner, int group){
        String group_string = "";
        if (group == 0){
            group_string = TEXT_EVERYONE.getString() + ", ";
        }else
            group_string = TEXT_GROUP.getString() + " " + group + ", " ;

        switch (state) {
            case 0 -> owner.sendMessage(new TextComponent(group_string +  TEXT_WANDER.getString()), owner.getUUID());
            case 1 -> owner.sendMessage(new TextComponent(group_string +  TEXT_FOLLOW.getString()), owner.getUUID());
            case 2 -> owner.sendMessage(new TextComponent(group_string +  TEXT_HOLD_POS.getString()), owner.getUUID());
            case 3 -> owner.sendMessage(new TextComponent(group_string +  TEXT_BACK_TO_POS.getString()), owner.getUUID());
            case 4 -> owner.sendMessage(new TextComponent(group_string +  TEXT_HOLD_MY_POS.getString()), owner.getUUID());
            case 5 -> owner.sendMessage(new TextComponent(group_string +  TEXT_ESCORT.getString()), owner.getUUID());

            case 98 -> owner.sendMessage(new TextComponent(group_string +  TEXT_DISMOUNT.getString()), owner.getUUID());
            case 99 -> owner.sendMessage(new TextComponent(group_string +  TEXT_MOUNT.getString()), owner.getUUID());
        }
    }

    public static void sendAggroCommandInChat(int state, LivingEntity owner, int group){
        String group_string = "";
        if (group == 0){
            group_string = TEXT_EVERYONE.getString() + ", ";
        }else
            group_string = TEXT_GROUP.getString() + " " + group + ", " ;


        switch (state) {
            case 0 -> owner.sendMessage(new TextComponent(group_string + TEXT_NEUTRAL.getString()), owner.getUUID());
            case 1 -> owner.sendMessage(new TextComponent(group_string + TEXT_AGGRESSIVE.getString()), owner.getUUID());
            case 2 -> owner.sendMessage(new TextComponent(group_string + TEXT_RAID.getString()), owner.getUUID());
            case 3 -> owner.sendMessage(new TextComponent(group_string + TEXT_PASSIVE.getString()), owner.getUUID());
        }
    }

    public static void setRecruitsInCommand(AbstractRecruitEntity recruit, int count) {
        LivingEntity living = recruit.getOwner();
        Player player = (Player) living;
        if (player != null){

            CompoundTag playerNBT = player.getPersistentData();
            CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

            nbt.putInt( "RecruitsInCommand", count);
            player.sendMessage(new TextComponent("EVENT int: " + count), player.getUUID());

            playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
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
        CompoundTag playerData = event.getPlayer().getPersistentData();
        CompoundTag data = playerData.getCompound(Player.PERSISTED_NBT_TAG);
            if (!data.contains("MaxRecruits")) data.putInt("MaxRecruits", RecruitsModConfig.MaxRecruitsForPlayer.get());
            if (!data.contains("CommandingGroup")) data.putInt("CommandingGroup", 0);
            if (!data.contains("TotalRecruits")) data.putInt("TotalRecruits", 0);

            playerData.put(Player.PERSISTED_NBT_TAG, data);
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

    public static int getSavedRecruitCount(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);
        //player.sendMessage(new StringTextComponent("getSavedCount: " + nbt.getInt("TotalRecruits")), player.getUUID());
        return nbt.getInt("TotalRecruits");
    }

    public static void saveRecruitCount(Player player, int count) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);
        //player.sendMessage(new StringTextComponent("savedCount: " + count), player.getUUID());

        nbt.putInt( "TotalRecruits", count);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public static boolean playerCanRecruit(Player player) {
        return  (CommandEvents.getSavedRecruitCount(player) < RecruitsModConfig.MaxRecruitsForPlayer.get());
    }

    public static void handleRecruiting(Player player, AbstractRecruitEntity recruit){
        String name = recruit.getName().getString() + ": ";
        String hire_costs = TEXT_HIRE_COSTS.getString();
        int costs = recruit.recruitCosts();

        String recruit_info = String.format(hire_costs, costs);
        Inventory playerInv = player.getInventory();

        int playerEmeralds = 0;

        ItemStack emeraldItemStack = Items.EMERALD.getDefaultInstance();
        Item emerald = emeraldItemStack.getItem();//
        int sollPrice = recruit.recruitCosts();


        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald){
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        boolean playerCanPay = playerEmeralds >= sollPrice;

        if (playerCanPay){
            if(recruit.hire(player)) {

                //give player tradeGood
                //remove playerEmeralds ->add left
                //
                playerEmeralds = playerEmeralds - sollPrice;

                //merchantEmeralds = merchantEmeralds + sollPrice;

                //remove playerEmeralds
                for (int i = 0; i < playerInv.getContainerSize(); i++) {
                    ItemStack itemStackInSlot = playerInv.getItem(i);
                    Item itemInSlot = itemStackInSlot.getItem();
                    if (itemInSlot == emerald) {
                        playerInv.removeItemNoUpdate(i);
                    }
                }

                //add leftEmeralds to playerInventory
                ItemStack emeraldsLeft = emeraldItemStack.copy();
                emeraldsLeft.setCount(playerEmeralds);
                playerInv.add(emeraldsLeft);
            }
        }
        else
            player.sendMessage(new TextComponent(name + recruit_info), player.getUUID());
    }



    @Nullable
    public static Entity getEntityByLooking() {
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit instanceof EntityHitResult entityHitResult){
            Entity pointedEntity = entityHitResult.getEntity();
            return pointedEntity;
        }
        return null;
    }


    public static void onMountButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID mount_uuid, int group) {
        if (recruit.isOwned() && (recruit.getListen()) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            recruit.shouldMount(true, mount_uuid);
        }
    }

    public static void onDismountButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isOwned() && (recruit.getListen()) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            recruit.shouldMount(false, null);
            if(recruit.isPassenger()){
                recruit.stopRiding();
            }
        }
    }

    public static void onEscortButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID escort_uuid, int group) {
        if (recruit.isOwned() && (recruit.getListen()) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            recruit.shouldEscort(true, escort_uuid);
        }
    }

    public static void onStopButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isOwned() && (recruit.getListen()) && Objects.equals(recruit.getOwnerUUID(), player_uuid) && (recruit.getGroup() == group || group == 0)) {
            recruit.setTarget(null);
        }
    }
}
