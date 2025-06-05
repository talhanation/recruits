package com.talhanation.recruits;

import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.MessengerEntity;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecruitEvents {
    private static final Map<ServerLevel, RecruitsPatrolSpawn> RECRUIT_PATROL = new HashMap<>();
    private static final Map<ServerLevel, PillagerPatrolSpawn> PILLAGER_PATROL = new HashMap<>();
    public static RecruitsPlayerUnitManager recruitsPlayerUnitManager;
    public static MinecraftServer server;
    static HashMap<Integer, EntityType<? extends AbstractRecruitEntity>> entitiesByProfession = new HashMap<>() {
        {
            put(0, ModEntityTypes.MESSENGER.get());
            put(1, ModEntityTypes.SCOUT.get());
            put(2, ModEntityTypes.PATROL_LEADER.get());
            put(3, ModEntityTypes.CAPTAIN.get());
        }
    };

    public static void promoteRecruit(AbstractRecruitEntity recruit, int profession, String name, ServerPlayer player) {
        EntityType<? extends AbstractRecruitEntity> companionType = entitiesByProfession.get(profession);
        AbstractRecruitEntity abstractRecruit = companionType.create(recruit.getCommandSenderWorld());
        if (abstractRecruit instanceof ICompanion companion) {
            abstractRecruit.setCustomName(Component.literal(name));
            abstractRecruit.copyPosition(recruit);
            companion.applyRecruitValues(recruit);
            companion.setOwnerName(player.getName().getString());

            recruit.discard();
            abstractRecruit.getCommandSenderWorld().addFreshEntity(abstractRecruit);
        }
    }

    public static void openPromoteScreen(Player player, AbstractRecruitEntity recruit) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return recruit.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new PromoteContainer(i, playerEntity, recruit);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(recruit.getUUID());
            });
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
    public void onWorldSave(LevelEvent.Save event){
        recruitsPlayerUnitManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onTeleportEvent(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(event instanceof EntityTeleportEvent.EnderPearl) && !(event instanceof EntityTeleportEvent.ChorusFruit) && !(event instanceof EntityTeleportEvent.EnderEntity)) {
            double targetX = event.getTargetX();
            double targetY = event.getTargetY();
            double targetZ = event.getTargetZ();
            UUID player_uuid = player.getUUID();

            List<AbstractRecruitEntity> recruits = player.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    player.getBoundingBox()
                            .inflate(64, 32, 64),
                    recruit -> recruit.isAlive() && recruit.getFollowState() == 1 && recruit.getOwnerUUID().equals(player_uuid)
            );

            recruits.forEach(recruit -> recruit.teleportTo(targetX, targetY, targetZ));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.level instanceof ServerLevel serverWorld) {
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
    private static final Set<Projectile> canceledProjectiles = new HashSet<>();

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        HitResult rayTrace = event.getRayTraceResult();

        if (canceledProjectiles.contains(entity)) {
            return;
        }

        if (!(entity instanceof Projectile projectile)) {
            return;
        }
        Entity owner = projectile.getOwner();

        if(owner == null) return;

        if (rayTrace.getType() != HitResult.Type.ENTITY) {
            return;
        }

        Entity impactEntity = ((EntityHitResult) rayTrace).getEntity();
        String encode = impactEntity.getEncodeId();
        if (encode != null && encode.contains("corpse:corpse")) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            return;
        }

        if (!(impactEntity instanceof LivingEntity impactLiving)) {
            return;
        }

        if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {

            if (!canAttack((LivingEntity) owner, impactLiving)) {
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                canceledProjectiles.add(projectile);
                return;
            }
        }

        if (owner instanceof AbstractRecruitEntity recruit) {

            if (impactLiving instanceof Animal animal) {
                Entity passenger = animal.getFirstPassenger();
                if (passenger instanceof AbstractRecruitEntity passengerRecruit) {
                    if (!canAttack(recruit, passengerRecruit)) {

                        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                        return;
                    }
                } else if (passenger instanceof Player player) {
                    if (!canAttack(recruit, player)) {

                        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                        return;
                    }
                }
            }

            if (!canAttack(recruit, impactLiving)) {
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                return;
            } else {
                recruit.addXp(2);
                recruit.checkLevel();
            }
        }

        if (owner instanceof AbstractIllager illager && !RecruitsServerConfig.PillagerFriendlyFire.get()) {
            if (illager.isAlliedTo(impactLiving)) {
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                canceledProjectiles.add(projectile);
                return;
            }
        }

        if (owner instanceof Player player) {
            if (!canHarmTeam(player, impactLiving)) {
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onEntityLeaveWorld(EntityLeaveLevelEvent event) {
        if(event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();
        if (entity instanceof Projectile projectile) {
            canceledProjectiles.remove(projectile);
        }
    }

    @SubscribeEvent
    public void onPlayerInteractWithCaravan(PlayerInteractEvent.EntityInteract entityInteract) {
        if(entityInteract.getLevel().isClientSide()) return;

        Player player = entityInteract.getEntity();
        Entity interacting = entityInteract.getTarget();

        if (interacting instanceof AbstractChestedHorse chestedHorse) {
            CompoundTag nbt = chestedHorse.getPersistentData();
            if (!nbt.contains("Caravan") || !chestedHorse.hasChest()) {
                return;
            }

            player.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    player.getBoundingBox().inflate(64F),
                    (recruit) -> !recruit.isOwned() &&
                            (recruit.getName().getString().equals("Caravan Leader") ||
                                    recruit.getName().getString().equals("Caravan Guard"))
            ).forEach((recruit) -> recruit.setTarget(player));
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if(event.getEntity().getCommandSenderWorld().isClientSide()) return;

        if (Main.isMusketModLoaded) {
            Entity sourceEntity = event.getSource().getEntity();
            if (sourceEntity instanceof AbstractRecruitEntity owner && IWeapon.isMusketModWeapon(owner.getMainHandItem())) {
                Entity target = event.getEntity();
                if (target instanceof LivingEntity impactEntity) {

                    if (!canAttack(owner, impactEntity)) {
                        event.setCanceled(true);
                    } else {
                        owner.addXp(2);
                        owner.checkLevel();
                    }
                }
            }
        }

        Entity target = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity sourceEntity) {
            if (target.getTeam() == null) return;

            target.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    target.getBoundingBox().inflate(32D),
                    (recruit) -> recruit.getTarget() == null &&
                            recruit.getTeam() != null &&
                            recruit.getTeam().equals(target.getTeam())
            ).forEach((recruit) -> recruit.setTarget(sourceEntity));
        }
    }

    private static final double DAMAGE_THRESHOLD_PERCENTAGE = 0.75;

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if(event.getEntity().getCommandSenderWorld().isClientSide()) return;

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
        Team attackerTeam = attacker.getTeam();
        Team targetTeam = target.getTeam();

        if (attackerTeam == null || targetTeam == null) return;


        double newHealth = target.getHealth() - damage;
        double damageThreshold = target.getMaxHealth() * DAMAGE_THRESHOLD_PERCENTAGE;


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
    public void onHorseJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorse horse) {
            horse.goalSelector.addGoal(0, new HorseRiddenByRecruitGoal(horse));
        }
    }

    @SubscribeEvent
    public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if(event.getLevel().isClientSide()) return;
        if (RecruitsServerConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();
            if (blockBreaker == null) return;

            final boolean[] warn = {false};
            final String[] name = new String[1];
            blockBreaker.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    blockBreaker.getBoundingBox().inflate(32.0D)
            ).forEach((recruit) -> {
                if (canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 1) {
                    recruit.setTarget(blockBreaker);
                }

                if (!warn[0] && canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned()) {
                    warn[0] = true;
                    name[0] = recruit.getName().toString();
                }
            });

            if (warn[0]) {
                warnPlayer(blockBreaker, TEXT_BLOCK_WARN(name[0]));
            }
        }

        if (RecruitsServerConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();
            if (blockBreaker == null) return;

            final boolean[] warn = {false};
            final String[] name = new String[1];

            blockBreaker.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    blockBreaker.getBoundingBox().inflate(32.0D)
            ).forEach((recruit) -> {
                if (canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned()) {
                    recruit.setTarget(blockBreaker);
                }

                if (!warn[0] && canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned()) {
                    warn[0] = true;
                    name[0] = recruit.getName().toString();
                }
            });

            if (warn[0]) {
                warnPlayer(blockBreaker, TEXT_BLOCK_WARN(name[0]));
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if(event.getLevel().isClientSide()) return;
        if (RecruitsServerConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                final boolean[] warn = {false};
                final String[] name = new String[1];

                livingBlockPlacer.getCommandSenderWorld().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        livingBlockPlacer.getBoundingBox().inflate(32.0D),
                        (recruit) -> canDamageTargetBlockEvent(recruit, livingBlockPlacer)
                ).forEach((recruit) -> {
                    if (recruit.getState() == 1) {
                        recruit.setTarget(livingBlockPlacer);
                    }

                    if (blockPlacer instanceof Player && !warn[0] &&
                            recruit.getState() == 0 && recruit.isOwned()) {
                        warn[0] = true;
                        name[0] = recruit.getName().toString();
                    }
                });

                if (blockPlacer instanceof Player player && warn[0]) {
                    warnPlayer(player, TEXT_BLOCK_WARN(name[0]));
                }
            }
        }

        if (RecruitsServerConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            final boolean[] warn = {false};
            final String[] name = new String[1];

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                livingBlockPlacer.getCommandSenderWorld().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        livingBlockPlacer.getBoundingBox().inflate(32.0D),
                        (recruit) -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) &&
                                recruit.getState() == 0 && recruit.isOwned()
                ).forEach((recruit) -> {
                    recruit.setTarget(livingBlockPlacer);

                    if (blockPlacer instanceof Player && !warn[0]) {
                        warn[0] = true;
                        name[0] = recruit.getName().toString();
                    }
                });

                if (blockPlacer instanceof Player player && warn[0]) {
                    warnPlayer(player, TEXT_BLOCK_WARN(name[0]));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if(event.getLevel().isClientSide()) return;
        BlockPos pos = event.getHitVec().getBlockPos();
        Player player = event.getEntity();

        BlockState selectedBlock = player.getCommandSenderWorld().getBlockState(pos);
        BlockEntity blockEntity = player.getCommandSenderWorld().getBlockEntity(pos);

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
            if (RecruitsServerConfig.AggroRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = player.getCommandSenderWorld().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        player.getBoundingBox().inflate(32.0D)
                );
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 1) {
                        recruits.setTarget(player);
                    }
                }

                if (list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())) {
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }

            if (RecruitsServerConfig.NeutralRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(player);
                    }
                }

                if (list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())) {
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    public boolean canDamageTargetBlockEvent(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isOwned() && target instanceof AbstractRecruitEntity recruitEntityTarget) {
            if (recruit.getOwnerUUID().equals(recruitEntityTarget.getOwnerUUID())) {
                return false;
            }
            else if (recruit.getTeam() != null && recruitEntityTarget.getTeam() != null && recruit.getTeam().equals(recruitEntityTarget.getTeam())){
                return false;
            }
        } else if (recruit.isOwned() && target instanceof Player player) {
            if (recruit.getOwnerUUID().equals(player.getUUID())) {
                return false;
            }
        } else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getProtectUUID() != null && recruitEntityTarget.getProtectUUID() != null && recruit.getProtectUUID().equals(recruitEntityTarget.getProtectUUID())) {
            return false;
        }
        return RecruitEvents.canHarmTeamNoFriendlyFire(recruit, target);
    }

    public static boolean canAttack(LivingEntity attacker, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;

        if (target instanceof Player player) {
            return canAttackPlayer(attacker, player);
        } else if (target instanceof AbstractRecruitEntity targetRecruit) {
            return canAttackRecruit(attacker, targetRecruit);
        } else if (target instanceof Animal animal) {
            return canAttackAnimal(attacker, animal);
        } else {
            return canHarmTeam(attacker, target);
        }
    }

    public static boolean canAttackAnimal(LivingEntity attacker, Animal animal) {
        if (attacker instanceof AbstractRecruitEntity recruit ){
            if(recruit.getVehicle() != null && recruit.getVehicle().getUUID().equals(animal.getUUID())) return false;

            if (recruit.getProtectUUID() != null && recruit.getProtectUUID().equals(recruit.getProtectUUID())) return false;

            if(animal.isVehicle()){
                if(animal.getFirstPassenger() instanceof AbstractRecruitEntity targetRecruit) return canAttackRecruit(attacker, targetRecruit);
                if(animal.getFirstPassenger() instanceof Player playerTarget) return canAttackPlayer(attacker, playerTarget);
            }
        }
        return canHarmTeam(attacker, animal);
    }


    public static boolean canAttackPlayer(LivingEntity attacker, Player player) {
        if (attacker instanceof AbstractRecruitEntity recruit) {
            if(player.getUUID().equals(recruit.getOwnerUUID())
                    || player.getUUID().equals(recruit.getProtectUUID())
                    || player.isCreative()
                    || player.isSpectator()
            )
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

            if(targetRecruit instanceof MessengerEntity messenger && messenger.isAtMission()) return false;
        }

        return canHarmTeam(attacker, targetRecruit);
    }

    public static boolean isAlly(Team team1, Team team2) {
        if (team1 == null || team2 == null || TeamEvents.recruitsDiplomacyManager == null) {
            return false;
        }
        return TeamEvents.recruitsDiplomacyManager.getRelation(team1.getName(), team2.getName()) ==
                RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
    }

    public static boolean isEnemy(Team team1, Team team2) {
        if (team1 == null || team2 == null || TeamEvents.recruitsDiplomacyManager == null) {
            return false;
        }
        return TeamEvents.recruitsDiplomacyManager.getRelation(team1.getName(), team2.getName()) ==
                RecruitsDiplomacyManager.DiplomacyStatus.ENEMY;
    }

    public static boolean isNeutral(Team team1, Team team2) {
        if (team1 == null || team2 == null || TeamEvents.recruitsDiplomacyManager == null) {
            return true;
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

        if (target instanceof AbstractRecruitEntity recruit) {
            if (!recruit.getIsOwned() || server.overworld().isClientSide()) return;

            //Morale loss when recruits teammate die
            UUID owner = recruit.getOwnerUUID();
            recruit.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    recruit.getBoundingBox().inflate(64.0D),
                    (entity) -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(owner)
            ).forEach((entity) -> {
                float currentMoral = entity.getMorale();
                float newMorale = currentMoral - 0.2F;
                entity.setMoral(Math.max(newMorale, 0F));
            });
        }
    }

    private final List<AbstractArrow> trackedArrows = new ArrayList<>();
    private int tickCounter = 0;

    @SubscribeEvent
    public void onWorldTickArrowCleaner(TickEvent.LevelTickEvent event) {//for 1.18 and 1.19 use TickEvent.WorldTickEvent
        if (event.level.isClientSide()) return;
        if (!RecruitsServerConfig.AllowArrowCleaning.get()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (server == null) return;


        if (++tickCounter < 100) return; // Alle 5 Sekunden
        tickCounter = 0;


        List<AbstractArrow> arrows = event.level.getEntitiesOfClass(AbstractArrow.class, event.level.getWorldBorder().getCollisionShape().bounds());
        trackedArrows.addAll(arrows);


        Iterator<AbstractArrow> iterator = trackedArrows.iterator();
        while (iterator.hasNext()) {
            AbstractArrow arrow = iterator.next();
            if (arrow.pickup == AbstractArrow.Pickup.DISALLOWED && arrow.inGroundTime > 300) {
                arrow.discard();
                iterator.remove();
            }
        }
    }
    private void removeArrow(Entity entity){
        if(entity instanceof AbstractArrow arrow && arrow.pickup == AbstractArrow.Pickup.DISALLOWED && arrow.inGroundTime > 300){
            entity.discard();
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

    private void warnPlayer(Player player, Component component) {
        saveCurrentWarning(player, (byte) (getSavedWarning(player) + 1));

        if (getSavedWarning(player) >= 0) {
            player.sendSystemMessage(component);
            saveCurrentWarning(player, (byte) -10);
        }
    }

    public static MutableComponent TEXT_BLOCK_WARN(String name) {
        return Component.translatable("chat.recruits.text.block_placing_warn", name);
    }

    public static MutableComponent TEXT_INTERACT_WARN(String name) {
        return Component.translatable("chat.recruits.text.block_interact_warn", name);
    }
}
