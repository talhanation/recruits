package com.talhanation.recruits;
import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.network.MessageSaveTeamSettings;
import com.talhanation.recruits.commands.PatrolSpawnCommand;
import com.talhanation.recruits.commands.RecruitsAdminCommands;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.ai.async.VisibilityGraphCache;
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
        MinecraftForge.EVENT_BUS.register(VisibilityGraphCache.class);
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
                MessageToClientUpdatePlayerCurrencyCount.class,
                MessageToClientOpenTakeOverScreen.class
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
        event.enqueueWork(ModScreens::registerMenus);
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
    }
}
