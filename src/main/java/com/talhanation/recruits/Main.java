package com.talhanation.recruits;

import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.init.*;
import com.talhanation.recruits.client.events.PlayerEvents;
import com.talhanation.recruits.client.gui.*;
import com.talhanation.recruits.client.gui.team.*;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.*;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "recruits";
    public static SimpleChannel SIMPLE_CHANNEL;
    public static VillagerProfession RECRUIT;
    public static VillagerProfession BOWMAN;
    public static VillagerProfession CROSSBOWMAN;
    public static VillagerProfession SHIELDMAN;
    public static VillagerProfession NOMAD;
    public static VillagerProfession HORSEMAN;
    public static PoiType POI_RECRUIT;
    public static PoiType POI_BOWMAN;
    public static PoiType POI_CROSSBOWMAN;
    public static PoiType POI_RECRUIT_SHIELDMAN;
    public static PoiType POI_NOMAD;
    public static PoiType POI_HORSEMAN;
    public static KeyMapping R_KEY;
    public static MenuType<RecruitInventoryMenu> RECRUIT_CONTAINER_TYPE;
    public static MenuType<DebugInvMenu> DEBUG_CONTAINER_TYPE;
    public static MenuType<CommandMenu> COMMAND_CONTAINER_TYPE;
    public static MenuType<RecruitHireMenu> HIRE_CONTAINER_TYPE;
    public static MenuType<AssassinLeaderMenu> ASSASSIN_CONTAINER_TYPE;
    public static KeyMapping U_KEY;
    public static MenuType<TeamCreationContainer> TEAM_CREATION_TYPE;
    public static MenuType<TeamMainContainer> TEAM_MAIN_TYPE;
    public static MenuType<TeamInspectionContainer> TEAM_INSPECTION_TYPE;
    public static MenuType<TeamListContainer> TEAM_LIST_TYPE;
    public static MenuType<DisbandContainer> DISBAND;
    public static MenuType<PromoteContainer> PROMOTE;
    public static MenuType<MessengerContainer> MESSENGER;
    public static MenuType<PatrolLeaderContainer> PATROL_LEADER;
    public static MenuType<TeamManagePlayerContainer> TEAM_ADD_PLAYER_TYPE;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean isMusketModLoaded;
    public static boolean isSmallShipsLoaded;
    public static boolean isSmallShipsCompatible;
    public static boolean isSiegeWeaponsLoaded;

    public Main() {
        CommonRegistry.registerConfig(ModConfig.Type.CLIENT, RecruitsClientConfig.class);
        CommonRegistry.registerConfig(ModConfig.Type.SERVER, RecruitsServerConfig.class);

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModScreens.MENU_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addCreativeTabs);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ModShortcuts::registerBindings);
            }
        );

        MinecraftForge.EVENT_BUS.register(this);

    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new RecruitEvents());
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
        MinecraftForge.EVENT_BUS.register(new PillagerEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
        MinecraftForge.EVENT_BUS.register(new DebugEvents());
        MinecraftForge.EVENT_BUS.register(new TeamEvents());
        MinecraftForge.EVENT_BUS.register(new DamageEvent());
        MinecraftForge.EVENT_BUS.register(new UpdateChecker());
        MinecraftForge.EVENT_BUS.register(this);

        SIMPLE_CHANNEL = CommonRegistry.registerChannel(Main.MOD_ID, "default");
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 0, MessageAggro.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 1, MessageAggroGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 2, MessageAssassinate.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 3, MessageAssassinCount.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 4, MessageAssassinGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 5, MessageMountEntity.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 6, MessageMountEntityGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 7, MessageClearTargetGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 8, MessageCommandScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 9, MessageDisband.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 10, MessageFollow.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 11, MessageFollowGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 12, MessageGroup.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 13, MessageListen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 14, MessageMove.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 15, MessageRecruitGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 16, MessageHireGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 17, MessageHire.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 18, MessageProtectEntity.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 19, MessageDismount.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 20, MessageDismountGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 21, MessageUpkeepPos.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 22, MessageStrategicFire.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 23, MessageShields.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 24, MessageDebugGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 25, MessageUpkeepEntity.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 26, MessageClearTarget.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 27, MessageCreateTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 28, MessageOpenTeamCreationScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 29, MessageLeaveTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 30, MessageTeamMainScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 31, MessageOpenTeamInspectionScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 32, MessageServerUpdateTeamInspectMenu.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 33, MessageToClientUpdateTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 34, MessageOpenTeamListScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 35, MessageAddPlayerToTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 36, MessageOpenTeamAddPlayerScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 37, MessageAddRecruitToTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 38, MessageSendJoinRequestTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 39, MessageRemoveFromTeam.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 40, MessageOpenDisbandScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 41, MessageAssignToTeamMate.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 42, MessageServerUpdateCommandScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 43, MessageToClientUpdateCommandScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 44, MessageWriteSpawnEgg.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 45, MessageBackToMountEntity.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 46, MessageDisbandGroup.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 47, MessageAssignGroupToTeamMate.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 48, MessagePromoteRecruit.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 49, MessageOpenPromoteScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 50, MessageOpenSpecialScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 51, MessageSendMessenger.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 52, MessagePatrolLeaderSetWaitTime.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 53, MessageToClientUpdateLeaderScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 54, MessagePatrolLeaderAddWayPoint.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 55, MessagePatrolLeaderRemoveWayPoint.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 56, MessagePatrolLeaderSetPatrolState.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 57, MessagePatrolLeaderSetCycle.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 58, MessagePatrolLeaderSetInfoMode.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 59, MessageAssignGroupToCompanion.class);
        isMusketModLoaded = ModList.get().isLoaded("musketmod");//MusketMod
        isSmallShipsLoaded = ModList.get().isLoaded("smallships");//small ships
        isSiegeWeaponsLoaded = ModList.get().isLoaded("siegeweapons");//siege weapons
        String smallshipsversion = ModList.get().getModFileById("smallships").versionString();//2.0.0-a2.3.1 above shall be supported e.g.: "2.0.0-b1.0"

        isSmallShipsCompatible = smallshipsversion.contains("2.0.0-b1");//TODO: Better Version check for compatible smallships version

        Main.LOGGER.info("smallships version: " + smallshipsversion);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModScreens::registerMenus);
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());

        R_KEY = ClientRegistry.registerKeyBinding("key.recruits.r_key", "category.recruits", GLFW.GLFW_KEY_R);
        U_KEY = ClientRegistry.registerKeyBinding("key.recruits.u_key", "category.recruits", GLFW.GLFW_KEY_U);

        ClientRegistry.registerScreen(Main.RECRUIT_CONTAINER_TYPE, RecruitInventoryScreen::new);
        ClientRegistry.registerScreen(Main.DEBUG_CONTAINER_TYPE, DebugInvScreen::new);
        ClientRegistry.registerScreen(Main.COMMAND_CONTAINER_TYPE, CommandScreen::new);
        ClientRegistry.registerScreen(Main.ASSASSIN_CONTAINER_TYPE, AssassinLeaderScreen::new);
        ClientRegistry.registerScreen(Main.HIRE_CONTAINER_TYPE, RecruitHireScreen::new);
        ClientRegistry.registerScreen(Main.TEAM_CREATION_TYPE, TeamCreationScreen::new);
        ClientRegistry.registerScreen(Main.TEAM_MAIN_TYPE, TeamMainScreen::new);
        ClientRegistry.registerScreen(Main.TEAM_INSPECTION_TYPE, TeamInspectionScreen::new);
        ClientRegistry.registerScreen(Main.TEAM_LIST_TYPE, TeamListScreen::new);
        ClientRegistry.registerScreen(Main.TEAM_ADD_PLAYER_TYPE, TeamManagePlayerScreen::new);
        ClientRegistry.registerScreen(Main.DISBAND, DisbandScreen::new);
        ClientRegistry.registerScreen(Main.PROMOTE, PromoteScreen::new);

        ClientRegistry.registerScreen(Main.MESSENGER, MessengerScreen::new);
        ClientRegistry.registerScreen(Main.PATROL_LEADER, PatrolLeaderScreen::new);
    }

    @SubscribeEvent
    public void registerPointsOfInterest(RegistryEvent.Register<PoiType> event) {
        POI_RECRUIT = new PoiType("poi_recruit", PoiType.getBlockStates(ModBlocks.RECRUIT_BLOCK.get()), 1, 1);
        POI_RECRUIT.setRegistryName(Main.MOD_ID, "poi_recruit");
        POI_BOWMAN = new PoiType("poi_bowman", PoiType.getBlockStates(ModBlocks.BOWMAN_BLOCK.get()), 1, 1);
        POI_BOWMAN.setRegistryName(Main.MOD_ID, "poi_bowman");
        POI_NOMAD = new PoiType("poi_nomad", PoiType.getBlockStates(ModBlocks.NOMAD_BLOCK.get()), 1, 1);
        POI_NOMAD.setRegistryName(Main.MOD_ID, "poi_nomad");
        POI_RECRUIT_SHIELDMAN = new PoiType("poi_recruit_shieldman", PoiType.getBlockStates(ModBlocks.RECRUIT_SHIELD_BLOCK.get()), 1, 1);
        POI_RECRUIT_SHIELDMAN.setRegistryName(Main.MOD_ID, "poi_recruit_shieldman");
        POI_HORSEMAN = new PoiType("poi_horseman", PoiType.getBlockStates(ModBlocks.HORSEMAN_BLOCK.get()), 1, 1);
        POI_HORSEMAN.setRegistryName(Main.MOD_ID, "poi_horseman");
        POI_CROSSBOWMAN = new PoiType("poi_crossbowman", PoiType.getBlockStates(ModBlocks.CROSSBOWMAN_BLOCK.get()), 1, 1);
        POI_CROSSBOWMAN.setRegistryName(Main.MOD_ID, "poi_crossbowman");

        event.getRegistry().register(POI_RECRUIT);
        event.getRegistry().register(POI_BOWMAN);
        event.getRegistry().register(POI_RECRUIT_SHIELDMAN);
        event.getRegistry().register(POI_NOMAD);
        event.getRegistry().register(POI_CROSSBOWMAN);
        event.getRegistry().register(POI_HORSEMAN);
    }

    @SubscribeEvent
    public void registerVillagerProfessions(RegistryEvent.Register<VillagerProfession> event) {
        RECRUIT = new VillagerProfession("recruit", POI_RECRUIT, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        RECRUIT.setRegistryName(Main.MOD_ID, "recruit");
        BOWMAN = new VillagerProfession("bowman", POI_BOWMAN, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        BOWMAN.setRegistryName(Main.MOD_ID, "bowman");
        NOMAD = new VillagerProfession("nomad", POI_NOMAD, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        NOMAD.setRegistryName(Main.MOD_ID, "nomad");
        SHIELDMAN = new VillagerProfession("recruit_shieldman", POI_RECRUIT_SHIELDMAN, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        SHIELDMAN.setRegistryName(Main.MOD_ID, "recruit_shieldman");
        CROSSBOWMAN = new VillagerProfession("crossbowman", POI_CROSSBOWMAN, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        CROSSBOWMAN.setRegistryName(Main.MOD_ID, "crossbowman");
        HORSEMAN = new VillagerProfession("horseman", POI_HORSEMAN, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        HORSEMAN.setRegistryName(Main.MOD_ID, "horseman");

        event.getRegistry().register(RECRUIT);
        event.getRegistry().register(SHIELDMAN);
        event.getRegistry().register(BOWMAN);
        event.getRegistry().register(NOMAD);
        event.getRegistry().register(CROSSBOWMAN);
        event.getRegistry().register(HORSEMAN);
    }


    @SubscribeEvent
    public void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        RECRUIT_CONTAINER_TYPE = new MenuType<>((IContainerFactory<RecruitInventoryMenu>) (windowId, inv, data) -> {
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new RecruitInventoryMenu(windowId, rec, inv);
        });
        RECRUIT_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "recruit_container"));
        event.getRegistry().register(RECRUIT_CONTAINER_TYPE);


        COMMAND_CONTAINER_TYPE = new MenuType<>((IContainerFactory<CommandMenu>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            if (playerEntity == null) {
                return null;
            }
            return new CommandMenu(windowId, playerEntity);
        });
        COMMAND_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "command_container"));
        event.getRegistry().register(COMMAND_CONTAINER_TYPE);


        ASSASSIN_CONTAINER_TYPE = new MenuType<>((IContainerFactory<AssassinLeaderMenu>) (windowId, inv, data) -> {
            AssassinLeaderEntity rec = getAssassinByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new AssassinLeaderMenu(windowId, rec, inv);
        });
        ASSASSIN_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "assassin_container"));
        event.getRegistry().register(ASSASSIN_CONTAINER_TYPE);

        HIRE_CONTAINER_TYPE = new MenuType<>((IContainerFactory<RecruitHireMenu>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (playerEntity == null) {
                return null;
            }
            return new RecruitHireMenu(windowId, playerEntity, rec, playerEntity.getInventory());
        });

        HIRE_CONTAINER_TYPE = new MenuType<>((IContainerFactory<RecruitHireMenu>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            return new RecruitHireMenu(windowId, playerEntity, rec, playerEntity.getInventory());
        });
        HIRE_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "hire_container"));
        event.getRegistry().register(HIRE_CONTAINER_TYPE);


        DEBUG_CONTAINER_TYPE = new MenuType<>((IContainerFactory<DebugInvMenu>) (windowId, inv, data) -> {
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new DebugInvMenu(windowId, rec, inv);
        });
        DEBUG_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "debug_container"));
        event.getRegistry().register(DEBUG_CONTAINER_TYPE);

        TEAM_CREATION_TYPE = new MenuType<>((IContainerFactory<TeamCreationContainer>) (windowId, inv, data) -> {
            return new TeamCreationContainer(windowId, inv);
        });
        TEAM_CREATION_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "team_creation"));
        event.getRegistry().register(TEAM_CREATION_TYPE);


        TEAM_MAIN_TYPE = new MenuType<>((IContainerFactory<TeamMainContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            return new TeamMainContainer(windowId, playerEntity);
        });
        TEAM_MAIN_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "team_main_container"));
        event.getRegistry().register(TEAM_MAIN_TYPE);


        TEAM_INSPECTION_TYPE = new MenuType<>((IContainerFactory<TeamInspectionContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            return new TeamInspectionContainer(windowId, playerEntity);
        });
        TEAM_INSPECTION_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "team_inspection_container"));
        event.getRegistry().register(TEAM_INSPECTION_TYPE);


        TEAM_LIST_TYPE = new MenuType<>((IContainerFactory<TeamListContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            return new TeamListContainer(windowId, playerEntity);
        });
        TEAM_LIST_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "team_list_container"));
        event.getRegistry().register(TEAM_LIST_TYPE);


        TEAM_ADD_PLAYER_TYPE = new MenuType<>((IContainerFactory<TeamManagePlayerContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            return new TeamManagePlayerContainer(windowId, playerEntity);
        });
        TEAM_ADD_PLAYER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "team_add_player_container"));
        event.getRegistry().register(TEAM_ADD_PLAYER_TYPE);

        DISBAND = new MenuType<>((IContainerFactory<DisbandContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new DisbandContainer(windowId, playerEntity, rec.getUUID());
        });
        DISBAND.setRegistryName(new ResourceLocation(Main.MOD_ID, "disband_container"));
        event.getRegistry().register(DISBAND);

        PROMOTE = new MenuType<>((IContainerFactory<PromoteContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new PromoteContainer(windowId, playerEntity, rec);
        });
        PROMOTE.setRegistryName(new ResourceLocation(Main.MOD_ID, "promote_container"));
        event.getRegistry().register(PROMOTE);

        MESSENGER = new MenuType<>((IContainerFactory<MessengerContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new MessengerContainer(windowId, playerEntity, (MessengerEntity) rec);
        });
        MESSENGER.setRegistryName(new ResourceLocation(Main.MOD_ID, "messenger_container"));
        event.getRegistry().register(MESSENGER);

        PATROL_LEADER = new MenuType<>((IContainerFactory<PatrolLeaderContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new PatrolLeaderContainer(windowId, playerEntity, (AbstractLeaderEntity) rec);
        });
        PATROL_LEADER.setRegistryName(new ResourceLocation(Main.MOD_ID, "patrol_leader_container"));
        event.getRegistry().register(PATROL_LEADER);
    }


    @Nullable
    public static AbstractRecruitEntity getRecruitByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AbstractRecruitEntity.class, new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }

    @Nullable
    public static AssassinLeaderEntity getAssassinByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AssassinLeaderEntity.class, new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(ModItems.BOWMAN_SPAWN_EGG.get());
            event.accept(ModItems.RECRUIT_SHIELD_SPAWN_EGG.get());
            event.accept(ModItems.RECRUIT_SPAWN_EGG.get());
            event.accept(ModItems.NOMAD_SPAWN_EGG.get());
            event.accept(ModItems.HORSEMAN_SPAWN_EGG.get());
            event.accept(ModItems.CROSSBOWMAN_SPAWN_EGG.get());
        }

        //if(event.getTab().)
    }
}
