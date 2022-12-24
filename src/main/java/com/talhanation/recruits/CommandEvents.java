package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.network.MessageCommandScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class CommandEvents {
    public static final TranslatableComponent TEXT_HIRE_COSTS_1 = new TranslatableComponent("chat.recruits.text.hire_costs_1");
    public static final TranslatableComponent TEXT_HIRE_COSTS_2 = new TranslatableComponent("chat.recruits.text.hire_costs_2");
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
    public static final TranslatableComponent TEXT_MOVE = new TranslatableComponent("chat.recruits.command.move");
    public static final TranslatableComponent TEXT_HAILOFARROWS = new TranslatableComponent("chat.recruits.command.arrows");
    public static final TranslatableComponent TEXT_SHIELDS = new TranslatableComponent("chat.recruits.command.shields");
    public static final TranslatableComponent TEXT_SHIELDS_OFF = new TranslatableComponent("chat.recruits.command.shields_off");
    public static final TranslatableComponent TEXT_HAILOFARROWS_OFF = new TranslatableComponent("chat.recruits.command.arrows_off");
    public static final TranslatableComponent TEXT_MOUNT = new TranslatableComponent("chat.recruits.command.mount");
    public static final TranslatableComponent TEXT_ESCORT = new TranslatableComponent("chat.recruits.command.escort");
    public static final TranslatableComponent TEXT_UPKEEP = new TranslatableComponent("chat.recruits.command.upkeep");

    public static void onFollowCommand(UUID player_uuid, AbstractRecruitEntity recruit, int r_state, int group, boolean fromGui) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
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

    public static void onAggroCommand(UUID player_uuid, AbstractRecruitEntity recruit, int x_state, int group, boolean fromGui) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
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

    public static void onArrowsCommand(Player player, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){

            if (recruit instanceof BowmanEntity bowman){
                HitResult hitResult = player.pick(100, 1F, false);
                bowman.setShouldArrow(should);
                if (hitResult != null) {
                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                        BlockPos blockpos = blockHitResult.getBlockPos();
                        bowman.setArrowPos(blockpos);
                    }
                }
            }
        }
    }

    public static void onMoveCommand(Player player, UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){

            HitResult hitResult = player.pick(100, 1F, false);

            if (hitResult != null) {
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                    BlockPos blockpos = blockHitResult.getBlockPos();
                    recruit.setMovePos(blockpos);
                    recruit.setShouldMovePos(true);
                    recruit.setFollowState(0);
                }
                //mount maybe
                /*
                else if (hitResult.getType() == HitResult.Type.ENTITY){
                    Entity crosshairEntity = minecraft.crosshairPickEntity;
                    if (crosshairEntity != null){
                        recruit.setMount(crosshairEntity.getUUID());
                    }
                }
                */
            }
        }
    }

    public static void openCommandScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {

                @Override
                public @NotNull Component getDisplayName() {
                    return new TranslatableComponent("command_screen") {
                    };
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
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

            case 92 -> owner.sendMessage(new TextComponent(group_string +  TEXT_UPKEEP.getString()), owner.getUUID());
            case 93 -> owner.sendMessage(new TextComponent(group_string +  TEXT_SHIELDS_OFF.getString()), owner.getUUID());
            case 94 -> owner.sendMessage(new TextComponent(group_string +  TEXT_HAILOFARROWS_OFF.getString()), owner.getUUID());
            case 95 -> owner.sendMessage(new TextComponent(group_string +  TEXT_SHIELDS.getString()), owner.getUUID());
            case 96 -> owner.sendMessage(new TextComponent(group_string +  TEXT_HAILOFARROWS.getString()), owner.getUUID());
            case 97 -> owner.sendMessage(new TextComponent(group_string +  TEXT_MOVE.getString()), owner.getUUID());
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
        Player living = recruit.getOwner();
        if (living != null){

            CompoundTag playerNBT = living.getPersistentData();
            CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

            nbt.putInt( "RecruitsInCommand", count);
            living.sendMessage(new TextComponent("EVENT int: " + count), living.getUUID());

            playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
        }
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
        String hire_costs_1 = TEXT_HIRE_COSTS_1.getString();
        String hire_costs_2 = TEXT_HIRE_COSTS_2.getString();

        int sollPrice = recruit.getCost();
        Inventory playerInv = player.getInventory();

        int playerEmeralds = 0;

        String str = RecruitsModConfig.RecruitCurrency.get();
        //Main.LOGGER.debug("str: " + str);
        ItemStack currencyItemStack;
        Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));
        //Main.LOGGER.debug("holder: " + holder);

        if (holder.isPresent()){
            currencyItemStack = holder.get().value().getDefaultInstance();
            //Main.LOGGER.debug("currencyItemStack: " + currencyItemStack);
        }
        else
            currencyItemStack = Items.EMERALD.getDefaultInstance();


        Item currency = currencyItemStack.getItem();//
        //Main.LOGGER.debug("currency: " + currency);

        String recruit_info_1 = String.format(hire_costs_1 + " " + sollPrice + " ");
        String recruit_info_2 = String.format(currency.getDescription().getString() + " " + hire_costs_2);
        //Main.LOGGER.debug("currency.getDescription().getString(): " + currency.getDescription().getString());




        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot.equals(currency)){
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
                    if (itemInSlot.equals(currency)) {
                        playerInv.removeItemNoUpdate(i);
                    }
                }

                //add leftEmeralds to playerInventory
                ItemStack emeraldsLeft = currencyItemStack.copy();
                emeraldsLeft.setCount(playerEmeralds);
                playerInv.add(emeraldsLeft);
            }
        }
        else
            player.sendMessage(new TextComponent(name + recruit_info_1 + recruit_info_2), player.getUUID());
    }


    public static void onMountButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID mount_uuid, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldMount(true, mount_uuid);
        }
    }

    public static void onDismountButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldMount(false, null);
            if(recruit.isPassenger()){
                recruit.stopRiding();
            }
        }
    }

    public static void onEscortButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID escort_uuid, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldEscort(true, escort_uuid);
        }
    }

    public static void onStopButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setTarget(null);
        }
    }

    public static void onUpkeepCommand(Player player, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean isEntity, UUID entity_uuid) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            if (isEntity) {
                Main.LOGGER.debug("server: entity_uuid: " + entity_uuid);
                recruit.setUpkeepUUID(Optional.of(entity_uuid));
                recruit.setUpkeepPos(BlockPos.ZERO);
            }
            else {
                HitResult hitResult = player.pick(100, 1F, false);
                if (hitResult != null) {
                    if (hitResult.getType().equals(HitResult.Type.BLOCK)) {
                        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                        BlockPos blockpos = blockHitResult.getBlockPos();
                        recruit.setUpkeepPos(blockpos);
                        recruit.setUpkeepUUID(Optional.empty());
                    }
                }
            }
        }
    }

    public static void onShieldsCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean shields) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldBlock(shields);
        }
    }
}
