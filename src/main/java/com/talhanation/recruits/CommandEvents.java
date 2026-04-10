package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.ai.controller.SmallShipsController;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.SiegeWeaponCatapultController;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.SiegeWeaponBallistaController;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandEvents {

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
        onMovementCommand(player, recruits, movementState, formation, false, false);
    }

    public static void onMovementCommand(ServerPlayer player, List<AbstractRecruitEntity> recruits, int movementState, int formation, boolean tight, boolean holdFormation) {
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
                   Vec3 center = getSavedFormationCenter(player);
                   if(center == null) center = FormationUtils.getGeometricMedian(recruits, (ServerLevel) player.getCommandSenderWorld());
                   Vec3 forward = player.getForward();
                   Vec3 pos = center.add(forward.scale(getForwardScale(recruits)));
                   BlockPos blockPos = FormationUtils.getPositionOrSurface(
                           player.getCommandSenderWorld(),
                           new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                   );

                   targetPos = new Vec3(pos.x, blockPos.getY(), pos.z);
               }

               case 8 -> {//backward
                   Vec3 center = getSavedFormationCenter(player);
                   if(center == null) center = FormationUtils.getGeometricMedian(recruits, (ServerLevel) player.getCommandSenderWorld());
                   Vec3 forward = player.getForward();
                   Vec3 pos = center.add(forward.scale(-getForwardScale(recruits)));
                   BlockPos blockPos = FormationUtils.getPositionOrSurface(
                           player.getCommandSenderWorld(),
                           new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                   );

                   targetPos = new Vec3(pos.x, blockPos.getY(), pos.z);
               }
            }
            applyFormation(formation, recruits, player, targetPos, tight, holdFormation);
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
                recruit.holdFormation = false;
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
            if(recruit instanceof SiegeEngineerEntity) return getForwardScale(recruit);
        }
        return 10;
    }
    private static double getForwardScale(AbstractRecruitEntity recruit) {
        if(recruit instanceof CaptainEntity captain && captain.smallShipsController.ship != null && captain.smallShipsController.ship.isCaptainDriver()) return 25;
        if(recruit instanceof SiegeEngineerEntity siegeEngineer && siegeEngineer.siegeController.getSiegeEntity() != null) return 20;
        return 10;
    }
    public static void applyFormation(int formation, List<AbstractRecruitEntity> recruits, ServerPlayer player, Vec3 targetPos) {
        applyFormation(formation, recruits, player, targetPos, false, false);
    }

    public static void applyFormation(int formation, List<AbstractRecruitEntity> recruits, ServerPlayer player, Vec3 targetPos, boolean tight, boolean holdFormation) {
        saveFormationCenter(player, targetPos);
        double spacingMultiplier = tight ? 0.5 : 1.0;
        switch (formation){
            case 1 ->{//LINE UP
                FormationUtils.lineUpFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 2 ->{//SQUARE
                FormationUtils.squareFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 3 ->{//TRIANGLE
                FormationUtils.triangleFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 4 ->{//HOLLOW CIRCLE
                FormationUtils.hollowCircleFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 5 ->{//HOLLOW SQUARE
                FormationUtils.hollowSquareFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 6 ->{//V Formation
                FormationUtils.vFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 7 ->{//CIRCLE
                FormationUtils.circleFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
            case 8 ->{//MOVEMENT
                FormationUtils.movementFormation(player, recruits, targetPos, spacingMultiplier, holdFormation);
            }
        }
    }

    public static void onFaceCommand(ServerPlayer player, List<AbstractRecruitEntity> recruits, int formation, boolean tight, boolean hold) {
        if(recruits.isEmpty()) return;

        if(formation != 0) {
            Vec3 targetPos = getSavedFormationCenter(player);
            if(targetPos == null) targetPos = FormationUtils.getGeometricMedian(recruits, (ServerLevel) player.getCommandSenderWorld());
            applyFormation(formation, recruits, player, targetPos, tight, hold);
        } else {
            // Without formation: hold current position
            for(AbstractRecruitEntity recruit : recruits) {
                recruit.setHoldPos(recruit.position());
                recruit.setFollowState(3);
            }
        }

        for(AbstractRecruitEntity recruit : recruits) {
            recruit.ownerRot = player.getYRot();
            recruit.rotateTicks = 40;

            // Captain with ship: rotate the ship
            if(recruit instanceof CaptainEntity captain && captain.smallShipsController.ship != null && captain.smallShipsController.ship.isCaptainDriver()) {
                captain.smallShipsController.startFaceRotation(player.getYRot());
            }

            // SiegeEngineer with siege weapon: rotate the siege weapon
            if(recruit instanceof SiegeEngineerEntity siegeEngineer && siegeEngineer.siegeController.getSiegeEntity() != null) {
                siegeEngineer.siegeController.startFaceRotation(player.getYRot());
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

    public static void onAggroCommand(UUID player_uuid, AbstractRecruitEntity recruit, int x_state, UUID group, boolean fromGui) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            int state = recruit.getState();
            switch (x_state) {

                case 0:
                    if (state != 0)
                        recruit.setAggroState(0);
                    break;

                case 1:
                    if (state != 1)
                        recruit.setAggroState(1);
                    break;

                case 2:
                    if (state != 2)
                        recruit.setAggroState(2);
                    break;

                case 3:
                    if (state != 3)
                        recruit.setAggroState(3);
                    break;
            }
        }
    }
    public static void onAttackCommand(Player player, UUID player_uuid, List<AbstractRecruitEntity> list, UUID group) {
        HitResult hitResult = player.pick(100, 1F, false);
        BlockPos blockpos = null;
        AABB aabb = null;
        List<LivingEntity> targets = new ArrayList<>();
        if (hitResult.getType() == HitResult.Type.ENTITY){
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;

            blockpos = entityHitResult.getEntity().getOnPos();
            if(entityHitResult.getEntity() instanceof LivingEntity living) targets.add(living);
        }
        else if (hitResult.getType() == HitResult.Type.BLOCK){
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            blockpos = blockHitResult.getBlockPos();
        }
        else return;

        aabb = new AABB(blockpos).inflate(10);

        list.removeIf(recruit -> !recruit.isEffectedByCommand(player_uuid, group));

        List<LivingEntity> validTargets = player.getCommandSenderWorld()
                .getEntitiesOfClass(LivingEntity.class, aabb);

        if (list.isEmpty() || validTargets.isEmpty()) return;

        AbstractRecruitEntity firstRecruit = list.get(0);
        validTargets.removeIf(target -> !firstRecruit.canAttack(target));

        if (validTargets.isEmpty()) return;

        for (int i = 0; i < list.size(); i++) {
            AbstractRecruitEntity recruit = list.get(i);
            LivingEntity target = validTargets.get(i % validTargets.size());
            recruit.setTarget(target);
            recruit.setHoldPos(getEngageHoldPos(recruit, target));
            recruit.setFollowState(3);
        }
    }

    private static Vec3 getEngageHoldPos(AbstractRecruitEntity recruit, LivingEntity target) {
        double maxRange = -1;

        if (recruit instanceof SiegeEngineerEntity siegeEngineer) {
            if (siegeEngineer.siegeController == siegeEngineer.catapultController) {
                maxRange = Math.sqrt(SiegeWeaponCatapultController.MAX_ENGAGE_DISTANCE); // ~137
            } else if (siegeEngineer.siegeController == siegeEngineer.ballistaController) {
                maxRange = Math.sqrt(SiegeWeaponBallistaController.MAX_ENGAGE_DISTANCE); // ~89
            }
        }
        else if (recruit instanceof CaptainEntity captain) {
            if (captain.smallShipsController != null && captain.smallShipsController.ship != null) {
                maxRange = Math.sqrt(captain.smallShipsController.attackRange); // ~67
            }
        } else if (recruit instanceof BowmanEntity) {
            maxRange = 44.0;
        } else if (recruit instanceof CrossBowmanEntity) {
            maxRange = Math.sqrt(3500);
        }

        if (maxRange <= 0) return target.position();

        double holdRange = maxRange * 0.75;
        Vec3 toTarget = target.position().subtract(recruit.position());
        double dist = toTarget.horizontalDistance();

        if (dist <= holdRange) {
            return recruit.position();
        }

        Vec3 dir = toTarget.multiply(1 / dist, 0, 1 / dist);
        return recruit.position().add(dir.scale(holdRange));
    }

    public static void onStrategicFireCommand(Player player, UUID player_uuid, AbstractRecruitEntity recruit, UUID group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){

            if (recruit instanceof IStrategicFire bowman){
                HitResult hitResult = player.pick(200, 1F, true);
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

                    List<AbstractRecruitEntity> recruits = Objects.requireNonNull(serverPlayer).getCommandSenderWorld().getEntitiesOfClass(
                                    AbstractRecruitEntity.class,
                                    serverPlayer.getBoundingBox().inflate(200)
                            );

                    List<RecruitsGroup> groups = RecruitEvents.recruitsGroupsManager.getPlayerGroups(serverPlayer);
                    if(groups == null) return;

                    List<UUID> uuid = getSavedUUIDList(serverPlayer, "ActiveGroups");

                    recruits.removeIf(recruit -> !uuid.contains(recruit.getGroup()));
                    groups.removeIf(group -> !uuid.contains( group.getUUID()));

                    applyFormation(formation, recruits, serverPlayer, targetPosition);
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
            if (!data.contains("ActiveGroups")) data.put("ActiveGroups", new ListTag());
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


    public static void saveUUIDList(Player player, String key, Collection<UUID> uuids) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag persisted = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        ListTag list = new ListTag();
        for (UUID uuid : uuids) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("UUID", uuid);
            list.add(tag);
        }

        persisted.put(key, list);
        playerNBT.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static List<UUID> getSavedUUIDList(Player player, String key) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag persisted = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        List<UUID> result = new ArrayList<>();
        if (!persisted.contains(key, Tag.TAG_LIST)) return result;

        ListTag list = persisted.getList(key, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag tag = (CompoundTag) t;
            if (tag.hasUUID("UUID")) {
                result.add(tag.getUUID("UUID"));
            }
        }

        return result;
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

    public static void saveFormationCenter(Player player, Vec3 center) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putDouble("FormationCenterX", center.x);
        nbt.putDouble("FormationCenterY", center.y);
        nbt.putDouble("FormationCenterZ", center.z);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    @Nullable
    public static Vec3 getSavedFormationCenter(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        if(!nbt.contains("FormationCenterX")) return null;

        return new Vec3(
                nbt.getDouble("FormationCenterX"),
                nbt.getDouble("FormationCenterY"),
                nbt.getDouble("FormationCenterZ")
        );
    }

    public static boolean handleRecruiting(Player player, RecruitsGroup group, AbstractRecruitEntity recruit, boolean message){
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
            if(recruit.hire(player, group, message)) {
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
                        FactionEvents.addNPCToData(serverPlayer.serverLevel(), player.getTeam().getName(), 1);
                    }
                }

                return true;
            }
        }
        else
            player.sendSystemMessage(TEXT_HIRE_COSTS(name, sollPrice, currency));

        return false;
    }

    public static void onMountButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID mount_uuid, UUID group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            if(mount_uuid != null) recruit.shouldMount(true, mount_uuid);
            else if(recruit.getMountUUID() != null) recruit.shouldMount(true, recruit.getMountUUID());
            recruit.dismount = 0;
        }
    }

    public static void onDismountButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldMount(false, null);
            if(recruit.isPassenger()){
                recruit.stopRiding();
                recruit.dismount = 180;
            }
        }
    }

    public static void onProtectButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID protect_uuid, UUID group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.shouldProtect(true, protect_uuid);
        }
    }

    public static void onClearTargetButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            //Main.LOGGER.debug("event: clear");
            recruit.setTarget(null);
            recruit.setLastHurtByPlayer(null);
            recruit.setLastHurtMob(null);
            recruit.setLastHurtByMob(null);
        }
    }

    public static void onClearUpkeepButton(UUID player_uuid, AbstractRecruitEntity recruit, UUID group) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            //Main.LOGGER.debug("event: clear");
            recruit.clearUpkeepEntity();
            recruit.clearUpkeepPos();
        }
    }
    public static void onUpkeepCommand(UUID player_uuid, AbstractRecruitEntity recruit, UUID group, boolean isEntity, UUID entity_uuid, BlockPos blockPos) {
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

    public static void onShieldsCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, UUID group, boolean shields) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldBlock(shields);
        }
    }

    public static void onRangedFireCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, UUID group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            recruit.setShouldRanged(should);

            if(should){
                if(recruit instanceof CrossBowmanEntity) recruit.switchMainHandItem(itemStack -> itemStack.getItem() instanceof CrossbowItem);
                if(recruit instanceof BowmanEntity) recruit.switchMainHandItem(itemStack -> itemStack.getItem() instanceof BowItem);
            }
            else{
                recruit.switchMainHandItem(itemStack -> itemStack.getItem() instanceof SwordItem);
            }
        }
    }

    public static void onRestCommand(ServerPlayer serverPlayer, UUID player_uuid, AbstractRecruitEntity recruit, UUID group, boolean should) {
        if (recruit.isEffectedByCommand(player_uuid, group)){
            onClearTargetButton(player_uuid, recruit, group);
            recruit.setShouldRest(should);
        }
    }

    private static MutableComponent TEXT_HIRE_COSTS(String name, int sollPrice, Item item) {
        return Component.translatable("chat.recruits.text.hire_costs", name, String.valueOf(sollPrice), item.getDescription().getString());
    }
}
