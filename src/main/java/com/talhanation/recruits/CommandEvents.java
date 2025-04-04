package com.talhanation.recruits;

import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.util.FormationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandEvents {
    public static final MutableComponent TEXT_EVERYONE = Component.translatable("chat.recruits.text.everyone");
    public static final MutableComponent TEXT_GROUP = Component.translatable("chat.recruits.text.group");

    //0 = wander
    //1 = follow
    //2 = hold your position
    //3 = back to position
    //4 = hold my position
    //5 = Protect
    //6 = move
    //7 = forward
    //8 = backward
    public static void onMovementCommand(ServerPlayer player, List<AbstractRecruitEntity> recruits, int movementState, int formation) {
        if(formation != 0 && (movementState == 2|| movementState == 4 || movementState == 6 || movementState == 7 || movementState == 8)) {
            Vec3 targetPos = null;

            switch (movementState){
               case 2 -> {//hold your position
                   targetPos = FormationUtils.getGeometricMedian(recruits, (ServerLevel) player.getCommandSenderWorld());
               }

               case 4 -> {//hold my position
                    targetPos = player.position();
               }

               case 6 -> {//move
                   HitResult hitResult = player.pick(200, 1F, true);
                   targetPos = hitResult.getLocation();
               }

               case 7 -> {//forward
                   Vec3 center = FormationUtils.getGeometricMedian(recruits, (ServerLevel) player.getCommandSenderWorld());
                   Vec3 forward = player.getForward();
                   Vec3 pos = center.add(forward.scale(getForwardScale(recruits)));
                   BlockPos blockPos = FormationUtils.getPositionOrSurface(
                           player.getCommandSenderWorld(),
                           new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                   );

                   targetPos = new Vec3(pos.x, blockPos.getY(), pos.z);
               }

               case 8 -> {//backward
                   Vec3 center = FormationUtils.getGeometricMedian(recruits, (ServerLevel) player.getCommandSenderWorld());
                   Vec3 forward = player.getForward();
                   Vec3 pos = center.add(forward.scale(-getForwardScale(recruits)));
                   BlockPos blockPos = FormationUtils.getPositionOrSurface(
                           player.getCommandSenderWorld(),
                           new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                   );

                   targetPos = new Vec3(pos.x, blockPos.getY(), pos.z);
               }
            }
            applyFormation(formation, recruits, player, targetPos);
        }
        else{
            for(AbstractRecruitEntity recruit : recruits){
                int state = recruit.getFollowState();

                switch (movementState) {
                    case 0 -> {
                        if (state != 0)
                            recruit.setFollowState(0);
                    }
                    case 1 -> {
                        if (state != 1)
                            recruit.setFollowState(1);
                    }
                    case 2 -> {
                        if (state != 2)
                            recruit.setFollowState(2);
                    }
                    case 3 -> {
                        if (state != 3)
                            recruit.setFollowState(3);
                    }
                    //
                    case 4 -> {
                        if (state != 4)
                            recruit.setFollowState(4);
                    }
                    //PROTECT
                    case 5 -> {
                        if (state != 5)
                            recruit.setFollowState(5);
                    }
                    //MOVE
                    case 6 ->{
                        HitResult hitResult = player.pick(100, 1F, true);
                        if (hitResult.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                            BlockPos blockpos = blockHitResult.getBlockPos();

                            recruit.setMovePos(blockpos);// needs to be above setFollowState

                            recruit.setFollowState(0);// needs to be above setShouldMovePos

                            recruit.setShouldMovePos(true);
                        }
                    }
                    //FORWARD
                    case 7 ->{
                        Vec3 forward = player.getForward();
                        Vec3 pos = recruit.position().add(forward.scale(getForwardScale(recruit)));
                        BlockPos blockPos = FormationUtils.getPositionOrSurface(
                                player.getCommandSenderWorld(),
                                new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                        );

                        Vec3 targetPos = new Vec3(pos.x, blockPos.getY(), pos.z);

                        recruit.setHoldPos(targetPos);
                        recruit.ownerRot = player.getYRot();
                        recruit.setFollowState(3);
                    }
                    //BACKWARD
                    case 8 ->{
                        Vec3 forward = player.getForward();
                        Vec3 pos = recruit.position().add(forward.scale(-getForwardScale(recruit)));
                        BlockPos blockPos = FormationUtils.getPositionOrSurface(
                                player.getCommandSenderWorld(),
                                new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                        );

                        Vec3 targetPos = new Vec3(pos.x, blockPos.getY(), pos.z);

                        recruit.setHoldPos(targetPos);
                        recruit.ownerRot = player.getYRot();
                        recruit.setFollowState(3);
                    }
                }
                recruit.isInFormation = false;
            }

        }
         for(AbstractRecruitEntity recruit : recruits) {
             recruit.setUpkeepTimer(recruit.getUpkeepCooldown());
             if (recruit.getShouldMount()) recruit.setShouldMount(false);

             checkPatrolLeaderState(recruit);
             recruit.forcedUpkeep = false;
         }
    }

    private static double getForwardScale(List<AbstractRecruitEntity> recruits) {
        for (AbstractRecruitEntity recruit : recruits){
            if(recruit instanceof CaptainEntity) return getForwardScale(recruit);
        }
        return 10;
    }
    private static double getForwardScale(AbstractRecruitEntity recruit) {
        return (recruit instanceof CaptainEntity captain && captain.smallShipsController.ship != null && captain.smallShipsController.ship.isCaptainDriver()) ? 25 : 10;
    }
    public static void applyFormation(int formation, List<AbstractRecruitEntity> recruits, ServerPlayer player, Vec3 targetPos) {
        switch (formation){
            case 1 ->{//LINE UP
                FormationUtils.lineUpFormation(player, recruits, targetPos);
            }
            case 2 ->{//SQUARE
                FormationUtils.squareFormation(player, recruits, targetPos);
            }
            case 3 ->{//TRIANGLE
                FormationUtils.triangleFormation(player, recruits, targetPos);
            }
            case 4 ->{//HOLLOW CIRCLE
                FormationUtils.hollowCircleFormation(player, recruits, targetPos);
            }
            case 5 ->{//HOLLOW SQUARE
                FormationUtils.hollowSquareFormation(player, recruits, targetPos);
            }
            case 6 ->{//V Formation
                FormationUtils.vFormation(player, recruits, targetPos);
            }
            case 7 ->{//CIRCLE
                FormationUtils.circleFormation(player, recruits, targetPos);
            }
            case 8 ->{//MOVEMENT
                FormationUtils.movementFormation(player, recruits, targetPos);
            }
        }
    }

    public static void onMovementCommandGUI(AbstractRecruitEntity recruit, int movementState) {
        int state = recruit.getFollowState();

        switch (movementState) {
            case 0 -> {
                if (state != 0)
                    recruit.setFollowState(0);
            }
            case 1 -> {
                if (state != 1)
                    recruit.setFollowState(1);
            }
            case 2 -> {
                if (state != 2)
                    recruit.setFollowState(2);
            }
            case 3 -> {
                if (state != 3)
                    recruit.setFollowState(3);
            }
            case 4 -> {
                if (state != 4)
                    recruit.setFollowState(4);
            }
            case 5 -> {
                if (state != 5)
                    recruit.setFollowState(5);
            }
        }

        recruit.setUpkeepTimer(recruit.getUpkeepCooldown());
        if (recruit.getShouldMount()) recruit.setShouldMount(false);
        //if (recruit instanceof CaptainEntity captain) captain.attackController. = false;
        checkPatrolLeaderState(recruit);
        recruit.forcedUpkeep = false;
    }

    public static void checkPatrolLeaderState(AbstractRecruitEntity recruit) {
        if(recruit instanceof AbstractLeaderEntity leader) {
            AbstractLeaderEntity.State patrolState = AbstractLeaderEntity.State.fromIndex(leader.getPatrollingState());
            if(patrolState == AbstractLeaderEntity.State.PATROLLING || patrolState == AbstractLeaderEntity.State.WAITING) {
                leader.setPatrolState(AbstractLeaderEntity.State.PAUSED);
            }
            else if(patrolState == AbstractLeaderEntity.State.RETREATING || patrolState == AbstractLeaderEntity.State.UPKEEP){
                leader.resetPatrolling();
                leader.setPatrolState(AbstractLeaderEntity.State.IDLE);
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

    public static void openCommandScreen(Player player) {
        if (player instanceof ServerPlayer) {
            updateCommandScreen((ServerPlayer)player);
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {

                @Override
                public @NotNull Component getDisplayName() {
                    return Component.literal("command_screen");
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
    @SubscribeEvent
    public void onServerPlayerTick(TickEvent.PlayerTickEvent event){
        if(event.player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 20 == 0){
            int formation = getSavedFormation(serverPlayer);

            if(formation > 0){
                int[] savedPos = getSavedFormationPos(serverPlayer);
                if(savedPos.length == 0) {
                    savedPos = new int[]{(int) serverPlayer.getX(), (int) serverPlayer.getZ()};
                    saveFormationPos(serverPlayer, savedPos);
                }
                int savedX = savedPos[0];
                int savedZ = savedPos[1];
                Vec3 oldPos = new Vec3(savedX, serverPlayer.getY(), savedZ);
                Vec3 targetPosition = serverPlayer.position();

                if(targetPosition.distanceToSqr(oldPos) > 50){

                    List<AbstractRecruitEntity> list = Objects.requireNonNull(serverPlayer).getCommandSenderWorld().getEntitiesOfClass(
                                    AbstractRecruitEntity.class,
                                    serverPlayer.getBoundingBox().inflate(200)
                            );
                    int[] array = getActiveGroups(serverPlayer);

                    list.removeIf(recruit -> Arrays.stream(array).noneMatch(x -> recruit.isEffectedByCommand(serverPlayer.getUUID(), x)));

                    applyFormation(formation, list, serverPlayer, targetPosition);
                    int[] position = new int[]{(int) targetPosition.x, (int) targetPosition.z};
                    saveFormationPos(serverPlayer, position);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        CompoundTag playerData = event.getEntity().getPersistentData();
        CompoundTag data = playerData.getCompound(Player.PERSISTED_NBT_TAG);
            if (!data.contains("MaxRecruits")) data.putInt("MaxRecruits", RecruitsServerConfig.MaxRecruitsForPlayer.get());
            if (!data.contains("CommandingGroup")) data.putInt("CommandingGroup", 0);
            if (!data.contains("TotalRecruits")) data.putInt("TotalRecruits", 0);
            if (!data.contains("ActiveGroups")) data.putIntArray("ActiveGroups", new int[0]);
            if (!data.contains("Formation")) data.putInt("Formation", 0);
            if (!data.contains("FormationPos")) data.putIntArray("FormationPos", new int[]{(int) event.getEntity().getX(), (int) event.getEntity().getZ()});

        playerData.put(Player.PERSISTED_NBT_TAG, data);
    }

    public static int getSavedFormation(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getInt("Formation");
    }

    public static void saveFormation(Player player, int formation) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putInt( "Formation", formation);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }
    public static int[] getSavedFormationPos(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getIntArray("FormationPos");
    }

    public static void saveFormationPos(Player player, int[] pos) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putIntArray( "FormationPos", pos);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }
    public static int[] getActiveGroups(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getIntArray("ActiveGroups");
    }

    public static void saveActiveGroups(Player player, int[] count) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putIntArray( "ActiveGroups", count);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
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

        if (playerCanPay || player.isCreative()){
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
                        TeamEvents.addNPCToData(serverPlayer.serverLevel(), player.getTeam().getName(), 1);
                    }
                }
            }
        }
        else
            player.sendSystemMessage(TEXT_HIRE_COSTS(name, sollPrice, currency));
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

    public static void onClearUpkeepButton(UUID player_uuid, AbstractRecruitEntity recruit, int group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            //Main.LOGGER.debug("event: clear");
            recruit.clearUpkeepEntity();
            recruit.clearUpkeepPos();
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
            recruit.forcedUpkeep = true;
            recruit.setUpkeepTimer(0);
            onClearTargetButton(player_uuid, recruit, group);
        }
    }

    public static void onShieldsCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean shields) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldBlock(shields);
        }
    }

    public static void onRangedFireCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldRanged(should);
        }
    }

    public static void onRestCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, int group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldRest(should);
        }
    }

    private static MutableComponent TEXT_HIRE_COSTS(String name, int sollPrice, Item item) {
        return Component.translatable("chat.recruits.text.hire_costs", name, String.valueOf(sollPrice), item.getDescription().getString());
    }

    private static final List<RecruitsGroup> GROUP_DEFAULT_SETTING = new ArrayList<>(
            Arrays.asList(
                    new RecruitsGroup(0, "No Group", false),
                    new RecruitsGroup(1, "Infantry", false),
                    new RecruitsGroup(2, "Ranged", false),
                    new RecruitsGroup(3, "Cavalry", false)
            )
    );
    public static void updateCommandScreen(ServerPlayer player) {
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateCommandScreen(getCompoundTagFromRecruitsGroupList(getAvailableGroups(player))));
    }

    public static void updateRecruitInventoryScreen(ServerPlayer player) {
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateRecruitInventoryScreen(getCompoundTagFromRecruitsGroupList(loadPlayersGroupsFromNBT(player))));
    }

    public static List<RecruitsGroup> getAvailableGroups(ServerPlayer player) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(120)));
        list.removeIf(recruit -> !recruit.isEffectedByCommand(player.getUUID(), 0));

        List<RecruitsGroup> allGroups = loadPlayersGroupsFromNBT(player);

        Map<Integer, Integer> groupCounts = new HashMap<>();

        for (AbstractRecruitEntity recruit : list) {
            int groupId = recruit.getGroup();
            groupCounts.put(groupId, groupCounts.getOrDefault(groupId, 0) + 1);
        }

        // Liste der verfügbaren Gruppen erstellen und die Anzahl der Rekruten sowie den disabled-Status aktualisieren
        List<RecruitsGroup> availableGroups = new ArrayList<>();
        for (RecruitsGroup group : allGroups) {
            if (groupCounts.containsKey(group.getId())) {
                group.setCount(groupCounts.get(group.getId()));
                availableGroups.add(group);
            }
        }

        return availableGroups;
    }

    public static List<RecruitsGroup> loadPlayersGroupsFromNBT(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        List<RecruitsGroup> groups = getRecruitsGroupListFormNBT(nbt);

        if(groups.isEmpty())
            groups = GROUP_DEFAULT_SETTING;

        return groups;
    }

    public static void savePlayersGroupsToNBT(ServerPlayer player, List<RecruitsGroup> groups, boolean update) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        if(update)
            updateCompoundTag(groups, nbt, player);
        else{
            overrideCompoundTag(groups, nbt, player);
        }


        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public static List<RecruitsGroup> getRecruitsGroupListFormNBT(CompoundTag nbt){
        List<RecruitsGroup> groups = new ArrayList<>();

        if(nbt.contains("recruits-groups")){
            ListTag groupList = nbt.getList("recruits-groups", 10);
            for (int i = 0; i < groupList.size(); ++i) {
                CompoundTag compoundnbt = groupList.getCompound(i);
                int id = compoundnbt.getInt("id");
                int count = compoundnbt.getInt("count");
                String name = compoundnbt.getString("name");
                boolean disabled = compoundnbt.getBoolean("disabled");

                RecruitsGroup recruitsGroup = new RecruitsGroup(id, name, disabled);
                recruitsGroup.setCount(count);

                groups.add(recruitsGroup);
            }
        }
        return groups;
    }

    public static CompoundTag updateCompoundTag(List<RecruitsGroup> groups, CompoundTag nbt, ServerPlayer player) {
        List<RecruitsGroup> currentList = loadPlayersGroupsFromNBT(player);

        Map<Integer, RecruitsGroup> groupMap = new HashMap<>();
        for (RecruitsGroup group : currentList) {
            groupMap.put(group.getId(), group);
        }

        for (RecruitsGroup group : groups) {
            if (group != null) {
                groupMap.put(group.getId(), group);
            }
        }

        ListTag groupList = new ListTag();
        for (RecruitsGroup group : groupMap.values()) {
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putInt("id", group.getId());
            compoundnbt.putInt("count", group.getCount());
            compoundnbt.putString("name", group.getName());
            compoundnbt.putBoolean("disabled", group.isDisabled());

            groupList.add(compoundnbt);
        }
        nbt.put("recruits-groups", groupList);

        return nbt;
    }

    public static CompoundTag overrideCompoundTag(List<RecruitsGroup> groups, CompoundTag nbt, ServerPlayer player) {
        ListTag groupList = new ListTag();
        for (RecruitsGroup group : groups) {
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putInt("id", group.getId());
            compoundnbt.putInt("count", group.getCount());
            compoundnbt.putString("name", group.getName());
            compoundnbt.putBoolean("disabled", group.isDisabled());

            groupList.add(compoundnbt);
        }

        nbt.put("recruits-groups", groupList);

        return nbt;
    }

    public static CompoundTag getCompoundTagFromRecruitsGroupList(List<RecruitsGroup> groups){
        CompoundTag nbt = new CompoundTag();
        ListTag groupList = new ListTag();
        for (RecruitsGroup group : groups) {
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putInt("id", group.getId());
            compoundnbt.putInt("count", group.getCount());
            compoundnbt.putString("name", group.getName());
            compoundnbt.putBoolean("disabled", group.isDisabled());

            groupList.add(compoundnbt);
        }
        nbt.put("recruits-groups", groupList);

        return nbt;
    }
}
