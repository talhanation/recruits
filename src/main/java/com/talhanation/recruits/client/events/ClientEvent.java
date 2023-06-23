package com.talhanation.recruits.client.events;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.models.RecruitVillagerModel;
import com.talhanation.recruits.client.render.human.*;
import com.talhanation.recruits.client.render.RecruitHorseRenderer;
import com.talhanation.recruits.client.render.layer.RecruitArmorLayer;
import com.talhanation.recruits.client.render.villager.*;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.network.MessageWriteSpawnEgg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEvent {

    public static ModelLayerLocation RECRUIT = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit"), "recruit");
    public static ModelLayerLocation RECRUIT_OUTER_ARMOR = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit_outer_layer"), "recruit_outer_layer");
    public static ModelLayerLocation RECRUIT_INNER_ARMOR = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit_inner_layer"), "recruit_inner_layer");

    @SubscribeEvent
    public static void clientSetup(EntityRenderersEvent.RegisterRenderers event){
        if(RecruitsModConfig.RecruitsLookLikeVillagers.get()){
            EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.BOWMAN.get(), BowmanVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.NOMAD.get(), NomadVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.HORSEMAN.get(), HorsemanVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), CrossbowmanVillagerRenderer::new );
            EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), ShieldmanVillagerRenderer::new );
        }
        else{
            EntityRenderers.register(ModEntityTypes.RECRUIT.get(), RecruitHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.BOWMAN.get(), BowmanHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.NOMAD.get(), NomadHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.HORSEMAN.get(), HorsemanHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.CROSSBOWMAN.get(), CrossbowmanHumanRenderer::new );
            EntityRenderers.register(ModEntityTypes.RECRUIT_SHIELDMAN.get(), ShieldmanHumanRenderer::new );
        }

        EntityRenderers.register(ModEntityTypes.RECRUIT_HORSE.get(), RecruitHorseRenderer::new );
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClientEvent.RECRUIT, RecruitVillagerModel::createLayerDefinition);
        event.registerLayerDefinition(ClientEvent.RECRUIT_OUTER_ARMOR, RecruitArmorLayer::createOuterArmorLayer);
        event.registerLayerDefinition(ClientEvent.RECRUIT_INNER_ARMOR, RecruitArmorLayer::createInnerArmorLayer);

    }

    @SubscribeEvent
    public void onPlayerPick(InputEvent.InteractionKeyMappingTriggered event){
        if(event.isPickBlock()){
            Entity target = getEntityByLooking();
            if(target instanceof AbstractRecruitEntity recruitEntity){
                Main.SIMPLE_CHANNEL.sendToServer(new MessageWriteSpawnEgg(recruitEntity.getUUID()));
                event.setCanceled(true);
            }
        }
    }

    @Nullable
    public static Entity getEntityByLooking() {
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit instanceof EntityHitResult entityHitResult){
            return entityHitResult.getEntity();
        }
        return null;
    }
}