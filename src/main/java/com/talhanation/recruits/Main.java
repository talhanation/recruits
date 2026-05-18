package com.talhanation.recruits;

import com.talhanation.recruits.client.gui.overlay.ClaimOverlayManager;
import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.client.events.ClientPlayerEvents;
import com.talhanation.recruits.client.gui.commandscreen.CombatCategory;
import com.talhanation.recruits.client.gui.commandscreen.MovementCategory;
import com.talhanation.recruits.client.gui.commandscreen.OtherCategory;
import com.talhanation.recruits.network.MessageSaveTeamSettings;
import com.talhanation.recruits.commands.PatrolSpawnCommand;
import com.talhanation.recruits.commands.RecruitsAdminCommands;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.init.ModScreens;
import com.talhanation.recruits.init.*;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.network.compat.RecruitsChannel;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "recruits";
    public static RecruitsChannel SIMPLE_CHANNEL = new RecruitsChannel();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static boolean isMusketModLoaded;
    public static boolean isSmallShipsLoaded;
    public static boolean isSmallShipsCompatible;
    public static boolean isSiegeWeaponsLoaded;
    public static boolean isSiegeWeaponsCompatible;
    public static boolean isEpicKnightsLoaded;
    public static boolean isCorpseLoaded;
    public static boolean isRPGZLoaded;

    public Main(IEventBus modEventBus, Dist dist, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.SERVER, RecruitsServerConfig.SERVER);
        modContainer.registerConfig(ModConfig.Type.CLIENT, RecruitsClientConfig.CLIENT);
        RecruitsClientConfig.loadConfig(RecruitsClientConfig.CLIENT, FMLPaths.CONFIGDIR.get().resolve("recruits-client.toml"));

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerPayloads);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModScreens.MENU_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::addCreativeTabs);

        if (dist == Dist.CLIENT) {
            modEventBus.addListener(Main.this::clientSetup);
            modEventBus.addListener(ModShortcuts::registerBindings);
        }

        //ModSounds.SOUNDS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PatrolSpawnCommand.register(event.getDispatcher());
        RecruitsAdminCommands.register(event.getDispatcher());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setup(final FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new RecruitEvents());
        NeoForge.EVENT_BUS.register(new VillagerEvents());
        NeoForge.EVENT_BUS.register(new PillagerEvents());
        NeoForge.EVENT_BUS.register(new CommandEvents());
        NeoForge.EVENT_BUS.register(new DebugEvents());
        NeoForge.EVENT_BUS.register(new FactionEvents());
        NeoForge.EVENT_BUS.register(new DamageEvent());
        NeoForge.EVENT_BUS.register(new UpdateChecker());
        NeoForge.EVENT_BUS.register(new ClaimEvents());
        NeoForge.EVENT_BUS.register(this);

        isMusketModLoaded = ModList.get().isLoaded("musketmod");//MusketMod
        isSmallShipsLoaded = ModList.get().isLoaded("smallships");//small ships
        isSiegeWeaponsLoaded = ModList.get().isLoaded("siegeweapons");//siege weapons
        isRPGZLoaded = ModList.get().isLoaded("rpgz");//rpgz mod
        isCorpseLoaded = ModList.get().isLoaded("corpse");//corpse mod
        isEpicKnightsLoaded = ModList.get().isLoaded("magistuarmory");//epic knights mod

        isSmallShipsCompatible = false;
        if(isSmallShipsLoaded){
            String smallshipsversion = ModList.get().getModFileById("smallships").versionString();
            isSmallShipsCompatible = isVersionAtLeast(smallshipsversion, "2.0.0-b1.4");
        }

        isSiegeWeaponsCompatible = false;
        if(isSiegeWeaponsLoaded){
            String siegeweaponsVersion = ModList.get().getModFileById("siegeweapons").versionString();
            isSiegeWeaponsCompatible = isVersionAtLeast(siegeweaponsVersion, "0.2.5");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Main.MOD_ID);
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
                MessageOpenTeamListScreen.class,
                MessageAddPlayerToTeam.class,
                MessageOpenTeamAddPlayerScreen.class,
                MessageAddRecruitToTeam.class,
                MessageSendJoinRequestTeam.class,
                MessageRemoveFromTeam.class,
                MessageOpenDisbandScreen.class,
                MessageAssignRecruitToPlayer.class,
                MessageWriteSpawnEgg.class,
                MessageBackToMountEntity.class,
                MessageDisbandGroup.class,
                MessageAssignGroupToPlayer.class,
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
                MessageToClientUpdateHireState.class,
                MessageRemoveAssignedGroupFromCompanion.class,
                MessageAnswerMessenger.class,
                MessageToClientOpenMessengerAnswerScreen.class,
                MessageClearUpkeepGui.class,
                MessageApplyNoGroup.class,
                MessageToClientUpdateGroups.class,
                MessagePatrolLeaderSetRoute.class,
                MessagePatrolLeaderSetEnemyAction.class,
                MessageSetLeaderGroup.class,
                MessageTransferRoute.class,
                MessageToClientReceiveRoute.class,
                MessageFormationFollowMovement.class,
                MessageRest.class,
                MessageRangedFire.class,
                MessageSaveFormationFollowMovement.class,
                MessageClearUpkeep.class,
                MessageToClientUpdateFactions.class,
                MessageToClientUpdateOnlinePlayers.class,
                MessageChangeDiplomacyStatus.class,
                MessageToClientSetToast.class,
                MessageToClientUpdateDiplomacyList.class,
                MessageSaveTeamSettings.class,
                MessageToClientSetDiplomaticToast.class,
                MessageScoutTask.class,
                MessageToClientOpenTakeOverScreen.class,
                MessageToClientUpdateClaims.class,
                MessageUpdateClaim.class,
                MessageDoPayment.class,
                MessageToClientUpdateClaim.class,
                MessageToClientUpdateOwnFaction.class,
                MessageDeleteClaim.class,
                MessageToClientOpenNobleTradeScreen.class,
                MessageHireFromNobleVillager.class,
                MessageAttack.class,
                MessageToClientUpdateUnitInfo.class,
                MessageUpdateGroup.class,
                MessageMergeGroup.class,
                MessageSplitGroup.class,
                MessageAssignNearbyRecruitsInGroup.class,
                MessageTeleportPlayer.class,
                MessageSendTreaty.class,
                MessageAnswerTreaty.class,
                MessageToClientOpenTreatyAnswerScreen.class,
                MessageToClientUpdateTreaties.class,
                MessageFaceCommand.class,
                MessageSetTargetPrio.class,
                MessageAddEmbargo.class,
                MessageRemoveEmbargo.class,
                MessageToClientUpdateEmbargoes.class,
                MessageAddEmbargoFaction.class
        };
        for (Class message : messages) {
            CommonRegistry.registerMessage(registrar, message);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModScreens::registerMenus);
        NeoForge.EVENT_BUS.register(new KeyEvents());
        NeoForge.EVENT_BUS.register(new ClientPlayerEvents());
        NeoForge.EVENT_BUS.register(new ClaimOverlayManager());

        CommandCategoryManager.register(new MovementCategory(), -2);
        CommandCategoryManager.register(new CombatCategory(), -3);
        CommandCategoryManager.register(new OtherCategory(), -1);
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(ModItems.BOWMAN_SPAWN_EGG.get());
            event.accept(ModItems.RECRUIT_SHIELD_SPAWN_EGG.get());
            event.accept(ModItems.RECRUIT_SPAWN_EGG.get());
            event.accept(ModItems.NOMAD_SPAWN_EGG.get());
            event.accept(ModItems.HORSEMAN_SPAWN_EGG.get());
            event.accept(ModItems.CROSSBOWMAN_SPAWN_EGG.get());
            event.accept(ModItems.VILLAGER_NOBLE_SPAWN_EGG.get());
        }

        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)){
            event.accept(ModBlocks.RECRUIT_BLOCK.get());
            event.accept(ModBlocks.BOWMAN_BLOCK.get());
            event.accept(ModBlocks.RECRUIT_SHIELD_BLOCK.get());
            event.accept(ModBlocks.CROSSBOWMAN_BLOCK.get());
            event.accept(ModBlocks.HORSEMAN_BLOCK.get());
            event.accept(ModBlocks.NOMAD_BLOCK.get());
        }
    }

    public static boolean isVersionAtLeast(String installedVersion, String minVersion) {
        String[] installed = installedVersion.split("[.\\-]", -1);
        String[] minimum  = minVersion.split("[.\\-]", -1);

        int len = Math.max(installed.length, minimum.length);
        for (int i = 0; i < len; i++) {
            String a = i < installed.length ? installed[i] : "0";
            String b = i < minimum.length  ? minimum[i]   : "0";

            int cmp;
            try {
                cmp = Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException e) {
                cmp = a.compareTo(b);
            }

            if (cmp != 0) return cmp > 0;
        }
        return true;
    }
}
