package com.talhanation.recruits;

import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.init.*;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
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

    public Main() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecruitsModConfig.CONFIG);
        RecruitsModConfig.loadConfig(RecruitsModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("recruits-common.toml"));

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
        isMusketModLoaded = ModList.get().isLoaded("musketmod");//MusketMod

    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModScreens::registerMenus);
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
    }

    private void addCreativeTabs(CreativeModeTabEvent.BuildContents event) {
        if (event.getTab() == CreativeModeTabs.SPAWN_EGGS) {
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
