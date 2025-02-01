package com.talhanation.recruits;

import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.ai.horse.HorseRiddenByRecruitGoal;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.inventory.PromoteContainer;
import com.talhanation.recruits.network.MessageOpenPromoteScreen;
import com.talhanation.recruits.world.PillagerPatrolSpawn;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsPlayerUnitManager;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class RecruitEvents {
    private static final Map<ServerLevel, RecruitsPatrolSpawn> RECRUIT_PATROL = new HashMap<>();
    private static final Map<ServerLevel, PillagerPatrolSpawn> PILLAGER_PATROL = new HashMap<>();
    public static RecruitsPlayerUnitManager recruitsPlayerUnitManager;
    public static MinecraftServer server;
    static HashMap<Integer, EntityType<? extends  AbstractRecruitEntity>> entitiesByProfession = new HashMap<>(){{
            put(0, ModEntityTypes.MESSENGER.get());
            put(1, ModEntityTypes.SCOUT.get());
            put(2, ModEntityTypes.PATROL_LEADER.get());
            put(3, ModEntityTypes.CAPTAIN.get());
        }
    };
    public static void promoteRecruit(AbstractRecruitEntity recruit, int profession, String name, ServerPlayer player) {
        EntityType<? extends AbstractRecruitEntity> companionType = entitiesByProfession.get(profession);
        AbstractRecruitEntity abstractRecruit = companionType.create(recruit.getCommandSenderWorld());
        if(abstractRecruit != null && abstractRecruit instanceof ICompanion companion){
            abstractRecruit.setCustomName(new TextComponent(name));
            abstractRecruit.copyPosition(recruit);
            companion.applyRecruitValues(recruit);
            companion.setOwnerName(player.getName().getString());

            recruit.discard();
            abstractRecruit.getCommandSenderWorld().addFreshEntity(abstractRecruit);
        }
    }

    public static void openPromoteScreen(Player player, AbstractRecruitEntity recruit) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return recruit.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new PromoteContainer(i, playerEntity,  recruit);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(recruit.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenPromoteScreen(player, recruit.getUUID()));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();

        recruitsPlayerUnitManager = new RecruitsPlayerUnitManager();
        recruitsPlayerUnitManager.load(server.overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recruitsPlayerUnitManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event){
        recruitsPlayerUnitManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onTeleportEvent(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(event instanceof EntityTeleportEvent.EnderPearl) && !(event instanceof EntityTeleportEvent.ChorusFruit) && !(event instanceof EntityTeleportEvent.EnderEntity)){

            UUID player_uuid = player.getUUID();
            double targetX = event.getTargetX();
            double targetY = event.getTargetY();
            double targetZ = event.getTargetZ();

            List <AbstractRecruitEntity> recruits = player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                    .inflate(64, 32, 64), AbstractRecruitEntity::isAlive)
                    .stream()
                    .filter(recruit -> recruit.getFollowState() == 1)
                    .filter(recruit -> recruit.getOwnerUUID().equals(player_uuid))
                    .toList();

            recruits.forEach(recruit -> recruit.teleportTo(targetX, targetY, targetZ));

            //wip
        }

    }
    @SubscribeEvent
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isClientSide && event.world instanceof ServerLevel serverWorld) {
            if (RecruitsServerConfig.ShouldRecruitPatrolsSpawn.get()) {
                RECRUIT_PATROL.computeIfAbsent(serverWorld,
                    serverLevel -> new RecruitsPatrolSpawn(serverWorld));
                    RecruitsPatrolSpawn spawner = RECRUIT_PATROL.get(serverWorld);
                    spawner.tick();
            }

            if (RecruitsServerConfig.ShouldPillagerPatrolsSpawn.get()) {
                PILLAGER_PATROL.computeIfAbsent(serverWorld,
                    serverLevel -> new PillagerPatrolSpawn(serverWorld));
                    PillagerPatrolSpawn pillagerSpawner = PILLAGER_PATROL.get(serverWorld);
                    pillagerSpawner.tick();
            }
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        HitResult rayTrace = event.getRayTraceResult();
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();

            if (rayTrace.getType() == HitResult.Type.ENTITY) {
                Entity impactEntity = ((EntityHitResult) rayTrace).getEntity();
                String encode = impactEntity.getEncodeId();
                if (impactEntity instanceof LivingEntity impactLiving) {

                    if (owner instanceof AbstractRecruitEntity recruit) {

                        if(impactLiving instanceof Animal animal){
                            if(animal.getFirstPassenger() instanceof AbstractRecruitEntity passenger){
                                if (!canAttack(recruit, passenger)) {
                                    event.setCanceled(true);
                                }
                            }
                            else if(animal.getFirstPassenger() instanceof Player player){
                                if (!canAttack(recruit, player)) {
                                    event.setCanceled(true);
                                }
                            }
                        }

                        if (!canAttack(recruit, impactLiving)) {
                            event.setCanceled(true);
                        }
                        else {
                            recruit.addXp(2);
                            recruit.checkLevel();
                        }
                    }

                    if (owner instanceof AbstractIllager illager && !RecruitsServerConfig.PillagerFriendlyFire.get()) {

                        if (illager.isAlliedTo(impactLiving)) {
                            event.setCanceled(true);
                        }
                    }

                    if (owner instanceof Player player) {
                        if (!canHarmTeam(player, impactLiving)) {
                            event.setCanceled(true);
                        }
                    }

                }
                else if (encode != null && encode.contains("corpse")){
                    event.setCanceled(true);
                }
            }
        }
    }
    @SubscribeEvent
    public void onPlayerInteractWithCaravan(PlayerInteractEvent.EntityInteract entityInteract){
        Player player = entityInteract.getPlayer();
        Entity interacting = entityInteract.getTarget();

        if(interacting instanceof AbstractChestedHorse chestedHorse){
            CompoundTag nbt = chestedHorse.getPersistentData();
            if(nbt.contains("Caravan") && chestedHorse.hasChest()){
                List<AbstractRecruitEntity> recruits = player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(64F));
                for(AbstractRecruitEntity recruit : recruits){
                    if(!recruit.isOwned() && (recruit.getName().getString().equals("Caravan Leader") || recruit.getName().getString().equals("Caravan Guard"))){
                        recruit.setTarget(player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if(Main.isMusketModLoaded) {
            Entity sourceEntity = event.getSource().getEntity();
            if(sourceEntity instanceof AbstractRecruitEntity owner && IWeapon.isMusketModWeapon(owner.getMainHandItem())){
                Entity target = event.getEntity();
                if (target instanceof LivingEntity impactEntity) {

                    if (!canAttack(owner, impactEntity)) {
                        event.setCanceled(true);
                    }
                    else {
                        owner.addXp(2);
                        owner.checkLevel();
                    }

                }
            }
        }

        Entity target = event.getEntity();
        Entity source = event.getSource().getEntity();
        if(source instanceof LivingEntity sourceEntity){
            if(target.getTeam() != null){
                List<AbstractRecruitEntity> list = Objects.requireNonNull(target.level.getEntitiesOfClass(AbstractRecruitEntity.class, target.getBoundingBox().inflate(32D)));

                for (AbstractRecruitEntity recruit : list){
                    if(recruit.getTarget() == null && recruit.getTeam() != null && recruit.getTeam().equals(target.getTeam()))
                        recruit.setTarget(sourceEntity);
                }
            }
        }
    }
    private static final double DAMAGE_THRESHOLD_PERCENTAGE = 0.75;

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        Entity target = event.getEntity();
        Entity source = event.getSource().getEntity();


        if (!target.getCommandSenderWorld().isClientSide() && target instanceof LivingEntity livingTarget && source instanceof LivingEntity livingSource) {
            if (!canAttack(livingSource, livingTarget)){
                event.setCanceled(true);
            }
            else{
                handleSignificantDamage(livingSource, livingTarget, event.getAmount(), (ServerLevel) livingTarget.getCommandSenderWorld());
            }
        }
    }

    private void handleSignificantDamage(LivingEntity attacker, LivingEntity target, double damage, ServerLevel level) {
        // Check teams
        Team attackerTeam = attacker.getTeam();
        Team targetTeam = target.getTeam();

        if (attackerTeam == null || targetTeam == null) return;

        // Calculate threshold for significant damage
        double newHealth = target.getHealth() - damage;
        double damageThreshold = target.getMaxHealth() * DAMAGE_THRESHOLD_PERCENTAGE;

        // If damage is significant, update diplomacy
        if (newHealth < damageThreshold) {
            setTeamsAsEnemies(attackerTeam, targetTeam, level);
        }
    }

    private void setTeamsAsEnemies(Team attackerTeam, Team targetTeam, ServerLevel level) {
        String attackerTeamName = attackerTeam.getName();
        String targetTeamName = targetTeam.getName();

        if (TeamEvents.recruitsDiplomacyManager != null) {
            TeamEvents.recruitsDiplomacyManager.setRelation(attackerTeamName, targetTeamName,
                    RecruitsDiplomacyManager.DiplomacyStatus.ENEMY, level);
            TeamEvents.recruitsDiplomacyManager.setRelation(targetTeamName, attackerTeamName,
                    RecruitsDiplomacyManager.DiplomacyStatus.ENEMY, level);
        }
    }

    @SubscribeEvent
    public void onHorseJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorse horse) {
            horse.goalSelector.addGoal(0, new HorseRiddenByRecruitGoal(horse));
        }
    }

    @SubscribeEvent
    public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if(RecruitsServerConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();

            if (blockBreaker != null){
                List<AbstractRecruitEntity> list = blockBreaker.level.getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 1) {
                        recruits.setTarget(blockBreaker);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(blockBreaker, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }

        if(RecruitsServerConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();

            if (blockBreaker != null){
                List<AbstractRecruitEntity> list = blockBreaker.level.getEntitiesOfClass(AbstractRecruitEntity.class, blockBreaker.getBoundingBox().inflate(32.0D));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, blockBreaker) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(blockBreaker);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(blockBreaker, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if(RecruitsServerConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(livingBlockPlacer.level.getEntitiesOfClass(AbstractRecruitEntity.class, livingBlockPlacer.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, livingBlockPlacer) && recruits.getState() == 1) {
                        recruits.setTarget(livingBlockPlacer);
                    }
                }

                if(blockPlacer instanceof Player player && list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }

        if(RecruitsServerConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(livingBlockPlacer.level.getEntitiesOfClass(AbstractRecruitEntity.class, livingBlockPlacer.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, livingBlockPlacer) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(livingBlockPlacer);
                    }
                }

                if(blockPlacer instanceof Player player && list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_BLOCK_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getHitVec().getBlockPos();
        Player player = event.getPlayer();
        BlockState selectedBlock = player.level.getBlockState(pos);
        BlockEntity blockEntity = player.level.getBlockEntity(pos);

        if (selectedBlock.is(BlockTags.BUTTONS) ||
            selectedBlock.is(BlockTags.DOORS) ||
            selectedBlock.is(BlockTags.WOODEN_TRAPDOORS) ||
            selectedBlock.is(BlockTags.WOODEN_BUTTONS) ||
            selectedBlock.is(BlockTags.WOODEN_DOORS) ||
            selectedBlock.is(BlockTags.SHULKER_BOXES) ||
            selectedBlock.is(BlockTags.FENCE_GATES) ||
            selectedBlock.is(BlockTags.ANVIL) ||
            (blockEntity instanceof Container)
        ) {


            if(RecruitsServerConfig.AggroRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 1) {
                        recruits.setTarget(player);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }

            if(RecruitsServerConfig.NeutralRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(player);
                    }
                }

                if(list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())){
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    public boolean canDamageTargetBlockEvent(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isOwned() && target instanceof AbstractRecruitEntity recruitEntityTarget) {
            if (recruit.getOwnerUUID().equals(recruitEntityTarget.getOwnerUUID())){
                return false;
            }
            //extra for safety
            else if (recruit.getTeam() != null && recruitEntityTarget.getTeam() != null && recruit.getTeam().equals(recruitEntityTarget.getTeam())){
                return false;
            }
        }
        else if (recruit.isOwned() && target instanceof Player player) {
            if (recruit.getOwnerUUID().equals(player.getUUID())){
                return false;
            }
        }
        else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getProtectUUID() != null && recruitEntityTarget.getProtectUUID() != null && recruit.getProtectUUID().equals(recruitEntityTarget.getProtectUUID())){
            return false;
        }
        return RecruitEvents.canHarmTeamNoFriendlyFire(recruit, target);
    }

    public static boolean canAttack(LivingEntity attacker, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;

        // Default logic for non-recruit attackers
        if (target instanceof Player player) {
            return canAttackPlayer(attacker, player);
        } else if (target instanceof AbstractRecruitEntity targetRecruit) {
            return canAttackRecruit(attacker, targetRecruit);
        } else {
            return canHarmTeam(attacker, target);
        }
    }


    public static boolean canAttackPlayer(LivingEntity attacker, Player player) {
        if (attacker instanceof AbstractRecruitEntity recruit && player.getUUID().equals(recruit.getOwnerUUID())) {
            return false;
        }
        return canHarmTeam(attacker, player);
    }

    public static boolean canAttackRecruit(LivingEntity attacker, AbstractRecruitEntity targetRecruit) {
        if (attacker.equals(targetRecruit)) return false;

        if (attacker instanceof AbstractRecruitEntity attackerRecruit) {
            if (attackerRecruit.isOwned() && targetRecruit.isOwned() &&
                    attackerRecruit.getOwnerUUID().equals(targetRecruit.getOwnerUUID())) {
                return false;
            }

            if (attackerRecruit.getTeam() != null && targetRecruit.getTeam() != null &&
                    attackerRecruit.getTeam().equals(targetRecruit.getTeam()) &&
                    !attackerRecruit.getTeam().isAllowFriendlyFire()) {
                return false;
            }

            if (attackerRecruit.getProtectUUID() != null &&
                    attackerRecruit.getProtectUUID().equals(targetRecruit.getProtectUUID())) {
                return false;
            }
        }

        return canHarmTeam(attacker, targetRecruit);
    }

    public static boolean isAlly(Team team1, Team team2) {
        if (team1 == null || team2 == null || TeamEvents.recruitsDiplomacyManager == null) {
            return false; // No team or diplomacy manager, cannot be allies
        }
        return TeamEvents.recruitsDiplomacyManager.getRelation(team1.getName(), team2.getName()) ==
                RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
    }

    public static boolean isEnemy(Team team1, Team team2) {
        if (team1 == null || team2 == null || TeamEvents.recruitsDiplomacyManager == null) {
            return false; // No team or diplomacy manager, cannot be enemies
        }
        return TeamEvents.recruitsDiplomacyManager.getRelation(team1.getName(), team2.getName()) ==
                RecruitsDiplomacyManager.DiplomacyStatus.ENEMY;
    }

    public static boolean isNeutral(Team team1, Team team2) {
        if (team1 == null || team2 == null || TeamEvents.recruitsDiplomacyManager == null) {
            return true; // No team or diplomacy manager, assume neutral
        }
        return TeamEvents.recruitsDiplomacyManager.getRelation(team1.getName(), team2.getName()) ==
                RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL;
    }

    public static boolean canHarmTeam(LivingEntity attacker, LivingEntity target) {
        Team attackerTeam = attacker.getTeam();
        Team targetTeam = target.getTeam();

        if (attackerTeam == null || targetTeam == null) return true;


        if (attackerTeam.equals(targetTeam) && !attackerTeam.isAllowFriendlyFire()) return false;


        return !isAlly(attackerTeam, targetTeam);
    }

    public static boolean canHarmTeamNoFriendlyFire(LivingEntity attacker, LivingEntity target) {
        Team team = attacker.getTeam();
        Team team1 = target.getTeam();

        if (team == null) {
            return true;

        } else if (team1 == null) {
            return true;
        }
        else if(team == team1){
            return false;
        }
        else {
            RecruitsDiplomacyManager.DiplomacyStatus relation = TeamEvents.recruitsDiplomacyManager.getRelation(team.getName(), team1.getName());

            return relation != RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
        }
    }

    @SubscribeEvent
    public void onRecruitDeath(LivingDeathEvent event) {
        Entity target = event.getEntity();

        if(target instanceof AbstractRecruitEntity recruit){

            //Morale loss when recruits friend die
            if(recruit.getIsOwned() && !server.overworld().isClientSide()){
                UUID owner = recruit.getOwnerUUID();
                List<AbstractRecruitEntity> recruits = recruit.level.getEntitiesOfClass(AbstractRecruitEntity.class, recruit.getBoundingBox().inflate(64.0D));
    
                for (AbstractRecruitEntity recruit2 : recruits) {
                    if(recruit2.getOwnerUUID() != null && recruit2.getOwnerUUID().equals(owner)){
                        float currentMoral = recruit2.getMoral();
                        float newMorale = currentMoral - 0.2F;
                        if(newMorale > 0) recruit2.setMoral(newMorale);
                        else recruit2.setMoral(0F);

                        //add to target list
                    }
                } 
            }
        }
    }

    public byte getSavedWarning(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getByte("RecruitWarnings");
    }

    public void saveCurrentWarning(Player player, byte x) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putByte("RecruitWarnings", x);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    private void warnPlayer(Player player, Component component){
        saveCurrentWarning(player, (byte) (getSavedWarning(player) + 1));

        if(getSavedWarning(player) >= 0){
            player.sendMessage(component, player.getUUID());
            saveCurrentWarning(player, (byte) -10);
        }
    }

    public static MutableComponent TEXT_BLOCK_WARN(String name) {
        return new TranslatableComponent("chat.recruits.text.block_placing_warn", name);
    }

    public static MutableComponent TEXT_INTERACT_WARN(String name) {
        return new TranslatableComponent("chat.recruits.text.block_interact_warn", name);
    }



}
