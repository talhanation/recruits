package com.talhanation.recruits;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.network.MessageSaveTeamSettings;
import com.talhanation.recruits.commands.PatrolSpawnCommand;
import com.talhanation.recruits.commands.RecruitsAdminCommands;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.init.*;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.ClientRegistry;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

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
    public static KeyMapping U_KEY;

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean isMusketModLoaded;
    public static boolean isSmallShipsLoaded;
    public static boolean isSmallShipsCompatible;
    public static boolean isSiegeWeaponsLoaded;
    public static boolean isEpicKnightsLoaded;
    public static boolean isCorpseLoaded;

    public Main() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RecruitsServerConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RecruitsClientConfig.CLIENT);
        RecruitsClientConfig.loadConfig(RecruitsClientConfig.CLIENT, FMLPaths.CONFIGDIR.get().resolve("recruits-client.toml"));

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));

        modEventBus.addGenericListener(PoiType.class, this::registerPointsOfInterest);
        modEventBus.addGenericListener(VillagerProfession.class, this::registerVillagerProfessions);
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModScreens.MENU_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PatrolSpawnCommand.register(event.getDispatcher());
        RecruitsAdminCommands.register(event.getDispatcher());
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

        Class[] messages = {
                MessageAggro.class,
                MessageAggroGui.class,
                MessageAssassinate.class,
                MessageAssassinCount.class,
                MessageAssassinGui.class,
                MessageMountEntity.class,
                MessageMountEntityGui.class,
                MessageClearTargetGui.class,
                MessageCommandScreen.class,
                MessageDisband.class,
                MessageMovement.class,
                MessageFollowGui.class,
                MessageGroup.class,
                MessageListen.class,
                MessageRecruitGui.class,
                MessageHireGui.class,
                MessageHire.class,
                MessageProtectEntity.class,
                MessageDismount.class,
                MessageDismountGui.class,
                MessageUpkeepPos.class,
                MessageStrategicFire.class,
                MessageShields.class,
                MessageDebugGui.class,
                MessageUpkeepEntity.class,
                MessageClearTarget.class,
                MessageCreateTeam.class,
                MessageOpenTeamEditScreen.class,
                MessageLeaveTeam.class,
                MessageTeamMainScreen.class,
                MessageOpenTeamInspectionScreen.class,
                MessageServerUpdateTeamInspectMenu.class,
                MessageOpenTeamListScreen.class,
                MessageAddPlayerToTeam.class,
                MessageOpenTeamAddPlayerScreen.class,
                MessageAddRecruitToTeam.class,
                MessageSendJoinRequestTeam.class,
                MessageRemoveFromTeam.class,
                MessageOpenDisbandScreen.class,
                MessageAssignToTeamMate.class,
                MessageToClientUpdateCommandScreen.class,
                MessageWriteSpawnEgg.class,
                MessageBackToMountEntity.class,
                MessageDisbandGroup.class,
                MessageAssignGroupToTeamMate.class,
                MessagePromoteRecruit.class,
                MessageOpenPromoteScreen.class,
                MessageOpenSpecialScreen.class,
                MessageSendMessenger.class,
                MessagePatrolLeaderSetWaitTime.class,
                MessageToClientUpdateLeaderScreen.class,
                MessagePatrolLeaderAddWayPoint.class,
                MessagePatrolLeaderRemoveWayPoint.class,
                MessagePatrolLeaderSetPatrolState.class,
                MessagePatrolLeaderSetCycle.class,
                MessagePatrolLeaderSetInfoMode.class,
                MessageAssignGroupToCompanion.class,
                MessagePatrolLeaderSetPatrollingSpeed.class,
                MessageToClientUpdateHireScreen.class,
                MessageToClientUpdateTeamEditScreen.class,
                MessageRemoveAssignedGroupFromCompanion.class,
                MessageToClientUpdateMessengerScreen.class,
                MessageAnswerMessenger.class,
                MessageToClientUpdateMessengerAnswerScreen.class,
                MessageOpenMessengerAnswerScreen.class,
                MessageClearUpkeepGui.class,
                MessageToServerRequestUpdateGroupList.class,
                MessageApplyNoGroup.class,
                MessageServerSavePlayerGroups.class,
                MessageToClientUpdateGroupList.class,
                MessageToClientUpdateRecruitInventoryScreen.class,
                MessageFormationFollowMovement.class,
                MessageRest.class,
                MessageRangedFire.class,
                MessageSaveFormationFollowMovement.class,
                MessageClearUpkeep.class,
                MessageToServerRequestUpdateTeamList.class,
                MessageToClientUpdateTeamList.class,
                MessageToServerRequestUpdatePlayerList.class,
                MessageToClientUpdatePlayerList.class,
                MessageDiplomacyChangeStatus.class,
                MessageToClientSetToast.class,
                MessageToClientUpdateDiplomacyList.class,
                MessageToServerRequestUpdateDiplomacyList.class,
                MessageToClientUpdateTeamInspection.class,
                MessageToServerRequestUpdateTeamInspaction.class,
                MessageSaveTeamSettings.class,
                MessageToClientSetDiplomaticToast.class,
                MessageScoutTask.class,
                MessageToServerRequestUpdatePlayerCurrencyCount.class,
                MessageToClientUpdatePlayerCurrencyCount.class
        };


        for (int i = 0; i < messages.length; i++){
            CommonRegistry.registerMessage(SIMPLE_CHANNEL, i, messages[i]);
        }


        isMusketModLoaded = ModList.get().isLoaded("musketmod");//MusketMod
        isSmallShipsLoaded = ModList.get().isLoaded("smallships");//small ships
        isSiegeWeaponsLoaded = ModList.get().isLoaded("siegeweapons");//siege weapons
        isCorpseLoaded = ModList.get().isLoaded("corpse");//corpse mod
        isEpicKnightsLoaded = ModList.get().isLoaded("magistuarmory");//epic knights mod

        isSmallShipsCompatible = false;
        if(isSmallShipsLoaded){
            String smallshipsversion = ModList.get().getModFileById("smallships").versionString();//2.0.0-a2.3.1 above shall be supported e.g.: "2.0.0-b1.1"
            isSmallShipsCompatible = smallshipsversion.contains("2.0.0-b1.3")||smallshipsversion.contains("2.0.0-b1.4");//TODO: Better Version check for compatible smallships versions
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
        event.enqueueWork(ModScreens::registerMenus);

        R_KEY = ClientRegistry.registerKeyBinding("key.recruits.r_key", "category.recruits", GLFW.GLFW_KEY_R);
        U_KEY = ClientRegistry.registerKeyBinding("key.recruits.u_key", "category.recruits", GLFW.GLFW_KEY_U);

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
}
