package com.talhanation.recruits;
import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.commands.PatrolSpawnCommand;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.init.ModScreens;
import com.talhanation.recruits.network.MessageServerSavePlayerGroups;
import com.talhanation.recruits.init.*;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "recruits";
    public static SimpleChannel SIMPLE_CHANNEL;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean isMusketModLoaded;
    public static boolean isSmallShipsLoaded;
    public static boolean isSmallShipsCompatible;
    public static boolean isSiegeWeaponsLoaded;
    public static boolean isEpicKnightsLoaded;
    public static boolean isCorpseLoaded;

    public Main() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RecruitsServerConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RecruitsClientConfig.CLIENT);
        RecruitsClientConfig.loadConfig(RecruitsClientConfig.CLIENT, FMLPaths.CONFIGDIR.get().resolve("recruits-client.toml"));

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup);
                    FMLJavaModLoadingContext.get().getModEventBus().addListener(ModShortcuts::registerBindings);
                }
        );

        modEventBus.addListener(this::setup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModScreens.MENU_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PatrolSpawnCommand.register(event.getDispatcher());
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
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 10, MessageMovement.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 11, MessageFollowGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 12, MessageGroup.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 13, MessageListen.class);
        //CommonRegistry.registerMessage(SIMPLE_CHANNEL, 14, MessageMove.class);
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
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 60, MessagePatrolLeaderSetPatrollingSpeed.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 61, MessageToClientUpdateHireScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 62, MessageToClientUpdateTeamCreationScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 63, MessageRemoveAssignedGroupFromCompanion.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 64, MessageToClientUpdateMessengerScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 65, MessageAnswerMessenger.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 66, MessageToClientUpdateMessengerAnswerScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 67, MessageOpenMessengerAnswerScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 68, MessageClearUpkeepGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 69, MessageOpenGroupManageScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 70, MessageApplyNoGroup.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 71, MessageServerSavePlayerGroups.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 72, MessageToClientUpdateGroupManageScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 73, MessageToClientUpdateRecruitInventoryScreen.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 74, MessageFormationFollowMovement.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 75, MessageRest.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 76, MessageRangedFire.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 77,  MessageCommandPatrolSpawn.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 78,  MessageSaveFormationFollowMovement.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 79,  MessageClearUpkeep.class);
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
        event.enqueueWork(ModScreens::registerMenus);
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
    }
}
