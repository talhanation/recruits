package com.talhanation.recruits;

import com.talhanation.recruits.client.events.ClientRegistry;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.client.events.PlayerEvents;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
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
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod("recruits")
public class Main {
    public static final String MOD_ID = "recruits";
    public static SimpleChannel SIMPLE_CHANNEL;
    public static KeyBinding ACTION_KEY;


    public Main() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecruitsModConfig.CONFIG);
        RecruitsModConfig.loadConfig(RecruitsModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("recruits-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        //ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
    }

    @SuppressWarnings("deprecation")
    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        SIMPLE_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("smallships", "default"), () -> "1.0.0", s -> true, s -> true);

        /*
        SIMPLE_CHANNEL.registerMessage(0, MessagePaddleState.class, (msg, buf) -> msg.toBytes(buf),
                buf -> (new MessagePaddleState()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));
        */

        DeferredWorkQueue.runLater(() -> {
            GlobalEntityTypeAttributes.put(ModEntityTypes.RECRUIT.get(), RecruitEntity.setAttributes().build());
        });

    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
        ACTION_KEY = ClientRegistry.registerKeyBinding("key.action_key", "category.recruits", 82);
    }
}
