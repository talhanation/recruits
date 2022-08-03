package com.talhanation.recruits;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.client.events.PlayerEvents;
import com.talhanation.recruits.client.gui.AssassinLeaderScreen;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.RecruitHireScreen;
import com.talhanation.recruits.client.gui.RecruitInventoryScreen;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.AssassinLeaderContainer;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.inventory.RecruitHireContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.ClientRegistry;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
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

import javax.annotation.Nullable;
import java.util.UUID;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "recruits";
    public static SimpleChannel SIMPLE_CHANNEL;
    public static VillagerProfession RECRUIT;
    public static VillagerProfession BOWMAN;
    public static VillagerProfession RECRUIT_SHIELDMAN;
    public static VillagerProfession SCOUT;
    public static VillagerProfession NOMAD;
    public static PoiType POI_RECRUIT;
    public static PoiType POI_BOWMAN;
    public static PoiType POI_RECRUIT_SHIELDMAN;
    public static PoiType POI_SCOUT;
    public static PoiType POI_NOMAD;
    public static KeyMapping R_KEY;
    public static MenuType<RecruitInventoryContainer> RECRUIT_CONTAINER_TYPE;
    public static MenuType<CommandContainer> COMMAND_CONTAINER_TYPE;
    public static MenuType<RecruitHireContainer> HIRE_CONTAINER_TYPE;
    public static MenuType<AssassinLeaderContainer> ASSASSIN_CONTAINER_TYPE;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Main() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecruitsModConfig.CONFIG);
        RecruitsModConfig.loadConfig(RecruitsModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("recruits-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addGenericListener(PoiType.class, this::registerPointsOfInterest);
        modEventBus.addGenericListener(VillagerProfession.class, this::registerVillagerProfessions);
        modEventBus.addGenericListener(MenuType.class, this::registerContainers);
        ModBlocks.BLOCKS.register(modEventBus);
        //ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
    }

    @SuppressWarnings("deprecation")
    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new RecruitEvents());
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
        MinecraftForge.EVENT_BUS.register(new PillagerEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
        MinecraftForge.EVENT_BUS.register(new AssassinEvents());
        MinecraftForge.EVENT_BUS.register(this);
        SIMPLE_CHANNEL = CommonRegistry.registerChannel(Main.MOD_ID, "default");
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 0, MessageAggro.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 1, MessageAggroGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 2, MessageAssassinate.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 3, MessageAssassinCount.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 4, MessageAssassinGui.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 5, MessageMountEntity.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 6, MessageClearTarget.class);
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
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 18, MessageEscortEntity.class);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());

        R_KEY = ClientRegistry.registerKeyBinding("key.r_key", "category.recruits", 82);

        ClientRegistry.registerScreen(Main.RECRUIT_CONTAINER_TYPE, RecruitInventoryScreen::new);
        ClientRegistry.registerScreen(Main.COMMAND_CONTAINER_TYPE, CommandScreen::new);
        ClientRegistry.registerScreen(Main.ASSASSIN_CONTAINER_TYPE, AssassinLeaderScreen::new);
        ClientRegistry.registerScreen(Main.HIRE_CONTAINER_TYPE, RecruitHireScreen::new);
    }

    @SubscribeEvent
    public void registerPointsOfInterest(RegistryEvent.Register<PoiType> event) {
        POI_RECRUIT = new PoiType("poi_recruit", PoiType.getBlockStates(ModBlocks.RECRUIT_BLOCK.get()), 1, 1);
        POI_RECRUIT.setRegistryName(Main.MOD_ID, "poi_recruit");
        POI_BOWMAN = new PoiType("poi_bowman", PoiType.getBlockStates(ModBlocks.BOWMAN_BLOCK.get()), 1, 1);
        POI_BOWMAN.setRegistryName(Main.MOD_ID, "poi_bowman");
        //POI_NOMAD = new PointOfInterestType("poi_nomad", PointOfInterestType.getBlockStates(ModBlocks.NOMAD_BLOCK.get()), 1, 1);
        //POI_NOMAD.setRegistryName(Main.MOD_ID, "poi_nomad");
        POI_RECRUIT_SHIELDMAN = new PoiType("poi_recruit_shieldman", PoiType.getBlockStates(ModBlocks.RECRUIT_SHIELD_BLOCK.get()), 1, 1);
        POI_RECRUIT_SHIELDMAN.setRegistryName(Main.MOD_ID, "poi_recruit_shieldman");


        event.getRegistry().register(POI_RECRUIT);
        event.getRegistry().register(POI_BOWMAN);
        event.getRegistry().register(POI_RECRUIT_SHIELDMAN);
        //event.getRegistry().register(POI_NOMAD);
    }

    @SubscribeEvent
    public void registerVillagerProfessions(RegistryEvent.Register<VillagerProfession> event) {
        RECRUIT = new VillagerProfession("recruit", POI_RECRUIT, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        RECRUIT.setRegistryName(Main.MOD_ID, "recruit");
        BOWMAN = new VillagerProfession("bowman", POI_BOWMAN, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        BOWMAN.setRegistryName(Main.MOD_ID, "bowman");
        //NOMAD = new VillagerProfession("nomad", POI_NOMAD, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        //NOMAD.setRegistryName(Main.MOD_ID, "nomad");
        RECRUIT_SHIELDMAN = new VillagerProfession("recruit_shieldman", POI_RECRUIT_SHIELDMAN, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        RECRUIT_SHIELDMAN.setRegistryName(Main.MOD_ID, "recruit_shieldman");



        event.getRegistry().register(RECRUIT);
        event.getRegistry().register(RECRUIT_SHIELDMAN);
        event.getRegistry().register(BOWMAN);
        //event.getRegistry().register(NOMAD);
    }


    @SubscribeEvent
    public void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        RECRUIT_CONTAINER_TYPE = new MenuType<>((IContainerFactory<RecruitInventoryContainer>) (windowId, inv, data) -> {
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new RecruitInventoryContainer(windowId, rec, inv);
        });
        RECRUIT_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "recruit_container"));
        event.getRegistry().register(RECRUIT_CONTAINER_TYPE);


        COMMAND_CONTAINER_TYPE = new MenuType<>((IContainerFactory<CommandContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            if (playerEntity == null) {
                return null;
            }
            return new CommandContainer(windowId, playerEntity);
        });
        COMMAND_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "command_container"));
        event.getRegistry().register(COMMAND_CONTAINER_TYPE);


        ASSASSIN_CONTAINER_TYPE = new MenuType<>((IContainerFactory<AssassinLeaderContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AssassinLeaderEntity rec = getAssassinByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new AssassinLeaderContainer(windowId, rec, inv);
        });
        ASSASSIN_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "assassin_container"));
        event.getRegistry().register(ASSASSIN_CONTAINER_TYPE);

        HIRE_CONTAINER_TYPE = new MenuType<>((IContainerFactory<RecruitHireContainer>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (playerEntity == null) {
                return null;
            }
            return new RecruitHireContainer(windowId, playerEntity, rec, playerEntity.getInventory());
        });
        HIRE_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "hire_container"));
        event.getRegistry().register(HIRE_CONTAINER_TYPE);
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
}
