package com.talhanation.recruits;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.client.events.*;
import com.talhanation.recruits.client.gui.AssassinLeaderScreen;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.RecruitInventoryScreen;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.AssassinLeaderContainer;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import de.maxhenkel.corelib.ClientRegistry;

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
    public static MenuType<AssassinLeaderContainer> ASSASSIN_CONTAINER_TYPE;

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
        SIMPLE_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "default"), () -> "1.0.0", s -> true, s -> true);


        SIMPLE_CHANNEL.registerMessage(1, MessageFollow.class, MessageFollow::toBytes,
                buf -> (new MessageFollow()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(2, MessageAggro.class, MessageAggro::toBytes,
                buf -> (new MessageAggro()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(3, MessageMove.class, MessageMove::toBytes,
                buf -> (new MessageMove()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(4, MessageClearTarget.class, MessageClearTarget::toBytes,
                buf -> (new MessageClearTarget()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(5, MessageListen.class, MessageListen::toBytes,
                buf -> (new MessageListen()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(6, MessageRecruitGui.class, MessageRecruitGui::toBytes,
                buf -> (new MessageRecruitGui()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(7, MessageCommandScreen.class, MessageCommandScreen::toBytes,
                buf -> (new MessageCommandScreen()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(8, MessageGroup.class, MessageGroup::toBytes,
                buf -> (new MessageGroup()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(9, MessageFollowGui.class, MessageFollowGui::toBytes,
                buf -> (new MessageFollowGui()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(10, MessageAggroGui.class, MessageAggroGui::toBytes,
                buf -> (new MessageAggroGui()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(11, MessageAttackEntity.class, MessageAttackEntity::toBytes,
                buf -> (new MessageAttackEntity()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(12, MessageRecruitsInCommand.class, MessageRecruitsInCommand::toBytes,
                buf -> (new MessageRecruitsInCommand()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(13, MessageDisband.class, MessageDisband::toBytes,
                buf -> (new MessageDisband()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(14, MessageAssassinate.class, MessageAssassinate::toBytes,
                buf -> (new MessageAssassinate()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(15, MessageAssassinGui.class, MessageAssassinGui::toBytes,
                buf -> (new MessageAssassinGui()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(16, MessageAssassinCount.class, MessageAssassinCount::toBytes,
                buf -> (new MessageAssassinCount()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(17, MessageClearTargetGui.class, MessageClearTargetGui::toBytes,
                buf -> (new MessageClearTargetGui()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        DeferredWorkQueue.runLater(() -> {
            DefaultAttributes.put(ModEntityTypes.RECRUIT.get(), RecruitEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitShieldmanEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.BOWMAN.get(), BowmanEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.CROSSBOWMAN.get(), BowmanEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.NOMAD.get(), NomadEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.RECRUIT_HORSE.get(), RecruitHorseEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.ASSASSIN.get(), AssassinEntity.setAttributes().build());
            DefaultAttributes.put(ModEntityTypes.ASSASSIN_LEADER.get(), AssassinLeaderEntity.setAttributes().build());
            //GlobalEntityTypeAttributes.put(ModEntityTypes.SCOUT.get(), ScoutEntity.setAttributes().build());
        });
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
