package com.talhanation.recruits;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.client.events.*;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.RecruitInventoryScreen;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModBlocks;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
    public static PointOfInterestType POI_RECRUIT;
    public static PointOfInterestType POI_BOWMAN;
    public static PointOfInterestType POI_RECRUIT_SHIELDMAN;
    public static PointOfInterestType POI_SCOUT;
    public static PointOfInterestType POI_NOMAD;
    public static KeyBinding R_KEY;
    public static KeyBinding X_KEY;
    public static KeyBinding C_KEY;
    public static KeyBinding Y_KEY;
    public static KeyBinding V_KEY;
    public static ContainerType<RecruitInventoryContainer> RECRUIT_CONTAINER_TYPE;
    public static ContainerType<CommandContainer> COMMAND_CONTAINER_TYPE;

    public Main() {
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecruitsModConfig.CONFIG);
        //RecruitsModConfig.loadConfig(RecruitsModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("recruits-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addGenericListener(PointOfInterestType.class, this::registerPointsOfInterest);
        modEventBus.addGenericListener(VillagerProfession.class, this::registerVillagerProfessions);
        modEventBus.addGenericListener(ContainerType.class, this::registerContainers);
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
        MinecraftForge.EVENT_BUS.register(this);
        SIMPLE_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "default"), () -> "1.0.0", s -> true, s -> true);


        SIMPLE_CHANNEL.registerMessage(0, MessageFollow.class, MessageFollow::toBytes,
                buf -> (new MessageFollow()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(1, MessageAttack.class, MessageAttack::toBytes,
                buf -> (new MessageAttack()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(2, MessageMove.class, MessageMove::toBytes,
                buf -> (new MessageMove()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(3, MessageClearTarget.class, MessageClearTarget::toBytes,
                buf -> (new MessageClearTarget()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(4, MessageListen.class, MessageListen::toBytes,
                buf -> (new MessageListen()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(5, MessageRecruitGui.class, MessageRecruitGui::toBytes,
                buf -> (new MessageRecruitGui()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(6, MessageCommandScreen.class, MessageCommandScreen::toBytes,
                buf -> (new MessageCommandScreen()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(7, MessageGroup.class, MessageGroup::toBytes,
                buf -> (new MessageGroup()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        DeferredWorkQueue.runLater(() -> {
            GlobalEntityTypeAttributes.put(ModEntityTypes.RECRUIT.get(), RecruitEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.RECRUIT_SHIELDMAN.get(), RecruitShieldmanEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.BOWMAN.get(), BowmanEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.CROSSBOWMAN.get(), BowmanEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.NOMAD.get(), NomadEntity.setAttributes().build());
            //GlobalEntityTypeAttributes.put(ModEntityTypes.SCOUT.get(), ScoutEntity.setAttributes().build());
        });
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.register(new KeyEvents());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());

        R_KEY = ClientRegistry.registerKeyBinding("key.r_key", "category.recruits", 82);
        X_KEY = ClientRegistry.registerKeyBinding("key.x_key", "category.recruits", 88);
        C_KEY = ClientRegistry.registerKeyBinding("key.c_key", "category.recruits", 67);
        Y_KEY = ClientRegistry.registerKeyBinding("key.y_key", "category.recruits", 90);
        V_KEY = ClientRegistry.registerKeyBinding("key.v_key", "category.recruits", 86);


        ClientRegistry.registerScreen(Main.RECRUIT_CONTAINER_TYPE, RecruitInventoryScreen::new);
        ClientRegistry.registerScreen(Main.COMMAND_CONTAINER_TYPE, CommandScreen::new);
    }

    @SubscribeEvent
    public void registerPointsOfInterest(RegistryEvent.Register<PointOfInterestType> event) {
        POI_RECRUIT = new PointOfInterestType("poi_recruit", PointOfInterestType.getBlockStates(ModBlocks.RECRUIT_BLOCK.get()), 1, 1);
        POI_RECRUIT.setRegistryName(Main.MOD_ID, "poi_recruit");
        POI_BOWMAN = new PointOfInterestType("poi_bowman", PointOfInterestType.getBlockStates(ModBlocks.BOWMAN_BLOCK.get()), 1, 1);
        POI_BOWMAN.setRegistryName(Main.MOD_ID, "poi_bowman");
        //POI_NOMAD = new PointOfInterestType("poi_nomad", PointOfInterestType.getBlockStates(ModBlocks.NOMAD_BLOCK.get()), 1, 1);
        //POI_NOMAD.setRegistryName(Main.MOD_ID, "poi_nomad");
        POI_RECRUIT_SHIELDMAN = new PointOfInterestType("poi_recruit_shieldman", PointOfInterestType.getBlockStates(ModBlocks.RECRUIT_SHIELD_BLOCK.get()), 1, 1);
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
    public void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
        RECRUIT_CONTAINER_TYPE = new ContainerType<>((IContainerFactory<RecruitInventoryContainer>) (windowId, inv, data) -> {
            AbstractRecruitEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new RecruitInventoryContainer(windowId, rec, inv);
        });
        RECRUIT_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "recruit_container"));
        event.getRegistry().register(RECRUIT_CONTAINER_TYPE);


        COMMAND_CONTAINER_TYPE = new ContainerType<>((IContainerFactory<CommandContainer>) (windowId, inv, data) -> {
            PlayerEntity playerEntity = inv.player;

            return new CommandContainer(windowId, playerEntity);
        });
        COMMAND_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "command_container"));
        event.getRegistry().register(COMMAND_CONTAINER_TYPE);
    }

    @Nullable
    public static AbstractRecruitEntity getRecruitByUUID(PlayerEntity player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AbstractRecruitEntity.class, new AxisAlignedBB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }
}
