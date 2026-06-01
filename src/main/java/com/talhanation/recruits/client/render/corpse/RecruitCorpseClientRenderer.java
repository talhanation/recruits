package com.talhanation.recruits.client.render.corpse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.talhanation.recruits.compat.corpse.RecruitCorpseAppearance;
import com.talhanation.recruits.compat.corpse.RecruitCorpseEntityTypeResolver;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RecruitCorpseClientRenderer {

    private static final String DUMMY_TEAM_NAME = "recruits_corpse_dummy_team";
    private static final Map<RecruitCorpseAppearance.Data, AbstractRecruitEntity> DUMMY_RECRUITS = new HashMap<>();

    @Nullable
    private static ClientLevel cachedLevel;

    private RecruitCorpseClientRenderer() {
    }

    public static boolean renderRecruitCorpse(CorpseEntity corpse, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (corpse.isSkeleton()) {
            return false;
        }

        Optional<java.util.UUID> corpseUUID = corpse.getCorpseUUID();
        RecruitCorpseAppearance.Data appearance = RecruitCorpseAppearance.decode(corpseUUID.orElse(null));
        if (appearance == null) {
            return false;
        }
        if (!(corpse.level() instanceof ClientLevel level)) {
            return false;
        }

        AbstractRecruitEntity recruit = getOrCreateDummyRecruit(level, appearance);
        if (recruit == null) {
            return false;
        }

        syncDummyRecruit(recruit, corpse, appearance);
        renderCorpsePose(corpse, recruit, appearance, partialTicks, poseStack, bufferSource, packedLight);
        return true;
    }

    @Nullable
    private static AbstractRecruitEntity getOrCreateDummyRecruit(ClientLevel level, RecruitCorpseAppearance.Data appearance) {
        if (cachedLevel != level) {
            cachedLevel = level;
            DUMMY_RECRUITS.clear();
        }

        AbstractRecruitEntity recruit = DUMMY_RECRUITS.get(appearance);
        if (recruit != null) {
            return recruit;
        }

        EntityType<? extends AbstractRecruitEntity> type = RecruitCorpseEntityTypeResolver.resolveEntityType(appearance.entityTypeHash(), appearance.companion(), level);
        if (type == null) {
            return null;
        }

        recruit = type.create(level);
        if (recruit == null) {
            return null;
        }

        recruit.setNoGravity(true);
        recruit.setSilent(true);
        recruit.setNoAi(true);
        recruit.setInvulnerable(true);
        DUMMY_RECRUITS.put(appearance, recruit);
        return recruit;
    }

    private static void syncDummyRecruit(AbstractRecruitEntity recruit, CorpseEntity corpse, RecruitCorpseAppearance.Data appearance) {
        recruit.setVariant(appearance.variant());
        recruit.setBiome(appearance.biome());
        recruit.setColor(appearance.color());
        recruit.setPos(corpse.getX(), corpse.getY(), corpse.getZ());
        recruit.setYRot(0F);
        recruit.yBodyRot = 0F;
        recruit.yBodyRotO = 0F;
        recruit.yHeadRot = 0F;
        recruit.yHeadRotO = 0F;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = ItemStack.EMPTY;
            if (corpse.getEquipment().size() > slot.ordinal()) {
                stack = corpse.getEquipment().get(slot.ordinal()).copy();
            }
            recruit.setItemSlot(slot, stack);
        }

        syncDummyTeam(recruit, appearance.hasTeam());
    }

    private static void syncDummyTeam(AbstractRecruitEntity recruit, boolean hasTeam) {
        Scoreboard scoreboard = recruit.level().getScoreboard();
        Team currentTeam = recruit.getTeam();

        if (currentTeam instanceof PlayerTeam playerTeam && DUMMY_TEAM_NAME.equals(playerTeam.getName()) && !hasTeam) {
            scoreboard.removePlayerFromTeam(recruit.getStringUUID(), playerTeam);
            return;
        }

        if (!hasTeam) {
            return;
        }

        PlayerTeam dummyTeam = scoreboard.getPlayerTeam(DUMMY_TEAM_NAME);
        if (dummyTeam == null) {
            dummyTeam = scoreboard.addPlayerTeam(DUMMY_TEAM_NAME);
        }

        if (currentTeam instanceof PlayerTeam playerTeam && !DUMMY_TEAM_NAME.equals(playerTeam.getName())) {
            scoreboard.removePlayerFromTeam(recruit.getStringUUID(), playerTeam);
        }

        scoreboard.addPlayerToTeam(recruit.getStringUUID(), dummyTeam);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderCorpsePose(CorpseEntity corpse, AbstractRecruitEntity recruit, RecruitCorpseAppearance.Data appearance, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(recruit);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-corpse.getYRot()));

        byte poseType = appearance.poseType();
        if (poseType == 1) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(0.0D, -1.05D, 0.14375D);
            poseStack.scale(1.06F, 1.0F, 1.06F);
        } else if (poseType == 2) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.0D, -1.05D, -0.14375D);
            poseStack.scale(1.06F, 1.0F, 1.06F);
        } else if (de.maxhenkel.corpse.Main.SERVER_CONFIG.spawnCorpseOnFace.get()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.0D, -1.0D, -0.125625D);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(0.0D, -1.0D, 0.125625D);
        }

        renderer.render(recruit, 0.0F, partialTicks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
