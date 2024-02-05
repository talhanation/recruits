package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.MessageAddRecruitToTeam;
import com.talhanation.recruits.network.MessageCommandScreen;
import com.talhanation.recruits.network.MessageToClientUpdateCommandScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandEvents {
    public static final MutableComponent TEXT_EVERYONE = new TranslatableComponent("chat.recruits.text.everyone");
    public static final MutableComponent TEXT_GROUP = new TranslatableComponent("chat.recruits.text.group");

    public static void onFollowCommand(UUID player_uuid, AbstractRecruitEntity recruit, int r_state, int group, boolean fromGui) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            int state = recruit.getFollowState();

            recruit.setUpkeepTimer(recruit.getUpkeepCooldown());
            if(recruit.getShouldMount()) recruit.setShouldMount(false);
            if(recruit instanceof CaptainEntity captain) captain.shipAttacking = false;
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

    public static void onStrategicFireCommand(Player player, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){

            if (recruit instanceof IStrategicFire bowman){
                HitResult hitResult = player.pick(100, 1F, false);
                bowman.setShouldStrategicFire(should);
                if (hitResult != null) {
                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                        BlockPos blockpos = blockHitResult.getBlockPos();
                        bowman.setStrategicFirePos(blockpos);
                    }
                }
            }
        }
    }

    public static void onMoveCommand(Player player, UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){

            HitResult hitResult = player.pick(100, 1F, true);
            if(recruit instanceof CaptainEntity captain) captain.shipAttacking = false;
            if(recruit.getShouldMount()) recruit.setShouldMount(false);

            if (hitResult != null) {
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                    BlockPos blockpos = blockHitResult.getBlockPos();
                    recruit.setFollowState(0);// needs to be above setShouldMovePos


                    recruit.setMovePos(blockpos);
                    recruit.setFollowState(0);// needs to be above setShouldMovePos
                    recruit.setShouldMovePos(true);

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
                    return new TextComponent("command_screen");
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new CommandMenu(i, playerEntity);
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
            case 0 -> owner.sendMessage(TEXT_WANDER(group_string), owner.getUUID());
            case 1 -> owner.sendMessage(TEXT_FOLLOW(group_string), owner.getUUID());
            case 2 -> owner.sendMessage(TEXT_HOLD_POS(group_string), owner.getUUID());
            case 3 -> owner.sendMessage(TEXT_BACK_TO_POS(group_string), owner.getUUID());
            case 4 -> owner.sendMessage(TEXT_HOLD_MY_POS(group_string), owner.getUUID());
            case 5 -> owner.sendMessage(TEXT_PROTECT(group_string), owner.getUUID());

            case 91 -> owner.sendMessage(TEXT_BACK_TO_MOUNT(group_string), owner.getUUID());
            case 92 -> owner.sendMessage(TEXT_UPKEEP(group_string), owner.getUUID());
            case 93 -> owner.sendMessage(TEXT_SHIELDS_OFF(group_string), owner.getUUID());
            case 94 -> owner.sendMessage(TEXT_STRATEGIC_FIRE_OFF(group_string), owner.getUUID());
            case 95 -> owner.sendMessage(TEXT_SHIELDS(group_string), owner.getUUID());
            case 96 -> owner.sendMessage(TEXT_STRATEGIC_FIRE(group_string), owner.getUUID());
            case 97 -> owner.sendMessage(TEXT_MOVE(group_string), owner.getUUID());
            case 98 -> owner.sendMessage(TEXT_DISMOUNT(group_string), owner.getUUID());
            case 99 -> owner.sendMessage(TEXT_MOUNT(group_string), owner.getUUID());
        }
    }

    private static MutableComponent TEXT_WANDER(String group_string) {
        return new TranslatableComponent("chat.recruits.command.wander", group_string);
    }

    private static MutableComponent TEXT_FOLLOW(String group_string) {
        return new TranslatableComponent("chat.recruits.command.follow", group_string);
    }

    private static MutableComponent TEXT_HOLD_POS(String group_string) {
        return new TranslatableComponent("chat.recruits.command.holdPos", group_string);
    }

    private static MutableComponent TEXT_BACK_TO_POS(String group_string) {
        return new TranslatableComponent("chat.recruits.command.backToPos", group_string);
    }

    private static MutableComponent TEXT_BACK_TO_MOUNT(String group_string) {
        return new TranslatableComponent("chat.recruits.command.backToMount", group_string);
    }


    private static MutableComponent TEXT_HOLD_MY_POS(String group_string) {
        return new TranslatableComponent("chat.recruits.command.holdMyPos", group_string);
    }

    private static MutableComponent TEXT_PROTECT(String group_string) {
        return new TranslatableComponent("chat.recruits.command.protect", group_string);
    }

    private static MutableComponent TEXT_UPKEEP(String group_string) {
        return new TranslatableComponent("chat.recruits.command.upkeep", group_string);
    }

    private static MutableComponent TEXT_SHIELDS_OFF(String group_string) {
        return new TranslatableComponent("chat.recruits.command.shields_off", group_string);
    }

    private static MutableComponent TEXT_STRATEGIC_FIRE_OFF(String group_string) {
        return new TranslatableComponent("chat.recruits.command.strategic_fire_off", group_string);
    }

    private static MutableComponent TEXT_SHIELDS(String group_string) {
        return new TranslatableComponent("chat.recruits.command.shields", group_string);
    }

    private static MutableComponent TEXT_STRATEGIC_FIRE(String group_string) {
        return new TranslatableComponent("chat.recruits.command.strategic_fire", group_string);
    }

    private static MutableComponent TEXT_MOVE(String group_string) {
        return new TranslatableComponent("chat.recruits.command.move", group_string);
    }

    private static MutableComponent TEXT_DISMOUNT(String group_string) {
        return new TranslatableComponent("chat.recruits.command.dismount", group_string);
    }

    private static MutableComponent TEXT_MOUNT(String group_string) {
        return new TranslatableComponent("chat.recruits.command.mount", group_string);
    }

    public static void sendAggroCommandInChat(int state, LivingEntity owner, int group){
        String group_string = "";
        if (group == 0){
            group_string = TEXT_EVERYONE.getString() + ", ";
        }else
            group_string = TEXT_GROUP.getString() + " " + group + ", " ;


        switch (state) {
            case 0 -> owner.sendMessage(TEXT_NEUTRAL(group_string), owner.getUUID());
            case 1 -> owner.sendMessage(TEXT_AGGRESSIVE(group_string), owner.getUUID());
            case 2 -> owner.sendMessage(TEXT_RAID(group_string), owner.getUUID());
            case 3 -> owner.sendMessage(TEXT_PASSIVE(group_string), owner.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        CompoundTag playerData = event.getEntity().getPersistentData();
        CompoundTag data = playerData.getCompound(Player.PERSISTED_NBT_TAG);
            if (!data.contains("MaxRecruits")) data.putInt("MaxRecruits", RecruitsServerConfig.MaxRecruitsForPlayer.get());
            if (!data.contains("CommandingGroup")) data.putInt("CommandingGroup", 0);
            if (!data.contains("TotalRecruits")) data.putInt("TotalRecruits", 0);

            playerData.put(Player.PERSISTED_NBT_TAG, data);
    }

    public static int getSavedRecruitCount(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);
        //player.sendSystemMessage(new StringTextComponent("getSavedCount: " + nbt.getInt("TotalRecruits")), player.getUUID());
        return nbt.getInt("TotalRecruits");
    }

    public static void saveRecruitCount(Player player, int count) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);
        //player.sendSystemMessage(new StringTextComponent("savedCount: " + count), player.getUUID());

        nbt.putInt( "TotalRecruits", count);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public static boolean playerCanRecruit(Player player) {
        return  (CommandEvents.getSavedRecruitCount(player) < RecruitsServerConfig.MaxRecruitsForPlayer.get());
    }

    public static void handleRecruiting(Player player, AbstractRecruitEntity recruit){
        String name = recruit.getName().getString() + ": ";
        int sollPrice = recruit.getCost();
        Inventory playerInv = player.getInventory();
        int playerEmeralds = 0;

        String str = RecruitsServerConfig.RecruitCurrency.get();
        Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));

        ItemStack currencyItemStack = holder.map(itemHolder -> itemHolder.value().getDefaultInstance()).orElseGet(Items.EMERALD::getDefaultInstance);

        Item currency = currencyItemStack.getItem();//

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


                if(player.getTeam() != null){
                    if(player.getCommandSenderWorld().isClientSide){
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageAddRecruitToTeam(player.getTeam().getName(), 1));
                    }
                    else {
                        ServerPlayer serverPlayer = (ServerPlayer) player;
                        TeamEvents.addNPCToData(serverPlayer.getLevel(), player.getTeam().getName(), 1);
                    }
                }
            }
        }
        else
            player.sendMessage(TEXT_HIRE_COSTS(name, sollPrice, currency), player.getUUID());
    }

    public static void onMountButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID mount_uuid, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            if(mount_uuid != null) recruit.shouldMount(true, mount_uuid);
            else if(recruit.getMountUUID() != null) recruit.shouldMount(true, recruit.getMountUUID());
            recruit.dismount = 0;
        }
    }

    public static void onDismountButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldMount(false, null);
            if(recruit.isPassenger()){
                recruit.stopRiding();
                recruit.dismount = 180;
            }
        }
    }

    public static void onProtectButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID protect_uuid, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldProtect(true, protect_uuid);
        }
    }

    public static void onClearTargetButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            //Main.LOGGER.debug("event: clear");
            recruit.setTarget(null);
            recruit.setLastHurtByPlayer(null);
            recruit.setLastHurtMob(null);
            recruit.setLastHurtByMob(null);
        }
    }

    public static void onUpkeepCommand(UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean isEntity, UUID entity_uuid, BlockPos blockPos) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            if (isEntity) {
                //Main.LOGGER.debug("server: entity_uuid: " + entity_uuid);
                recruit.setUpkeepUUID(Optional.of(entity_uuid));
                recruit.clearUpkeepPos();
            }
            else {
                recruit.setUpkeepPos(blockPos);
                recruit.clearUpkeepEntity();
            }

        }
        recruit.setUpkeepTimer(0);
        onClearTargetButton(player_uuid, recruit, group);

    }

    public static void onShieldsCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean shields) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldBlock(shields);
        }
    }

    private static Component TEXT_PASSIVE(String group_string) {
        return new TranslatableComponent("chat.recruits.command.passive", group_string);
    }

    private static MutableComponent TEXT_RAID(String group_string) {
        return new TranslatableComponent("chat.recruits.command.raid", group_string);
    }

    private static MutableComponent TEXT_AGGRESSIVE(String group_string) {
        return new TranslatableComponent("chat.recruits.command.aggressive", group_string);
    }

    private static MutableComponent TEXT_NEUTRAL(String group_string) {
        return new TranslatableComponent("chat.recruits.command.neutral", group_string);
    }

    private static MutableComponent TEXT_HIRE_COSTS(String name, int sollPrice, Item item) {
        return new TranslatableComponent("chat.recruits.text.hire_costs", name, String.valueOf(sollPrice), item.getDescription().getString());
    }


    public static void updateCommandScreen(ServerPlayer player, int group) {

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateCommandScreen(getRecruitsInCommand(player, group)));
    }

    public static int getRecruitsInCommand(ServerPlayer player, int group){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(100)));
        List<AbstractRecruitEntity> loyals = new ArrayList<>();

        for (AbstractRecruitEntity recruit : list){
            if(recruit.isEffectedByCommand(player.getUUID(), group)){
                loyals.add(recruit);
            }
        }

        return loyals.size();
    }
}
