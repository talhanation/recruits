package com.talhanation.recruits.client.gui.diplomacy;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.client.gui.team.RecruitsTeamListScreen;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DiplomacyTeamEntry extends ListScreenEntryBase<DiplomacyTeamEntry> {
    protected static final int SKIN_SIZE = 24;
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);

    protected final Minecraft minecraft;
    protected final DiplomacyTeamListScreen screen;
    protected final @NotNull RecruitsTeam team;
    protected final List<Pair<BannerPattern, DyeColor>> resultBannerPatterns;
    protected final ModelPart flag;
    protected final ItemStack bannerItem;
    protected final RecruitsDiplomacyManager.DiplomacyStatus status;

    public DiplomacyTeamEntry(DiplomacyTeamListScreen screen, @NotNull RecruitsTeam team, RecruitsDiplomacyManager.DiplomacyStatus status) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.team = team;
        this.bannerItem = ItemStack.of(team.getBanner());
        this.resultBannerPatterns = BannerBlockEntity.createPatterns(((BannerItem) this.bannerItem.getItem()).getColor(), BannerBlockEntity.getItemPatterns(this.bannerItem));
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
        this.status = status;
    }

    @Override
    public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
        int skinX = left + PADDING;
        int skinY = top + (height - SKIN_SIZE) / 2;
        int textX = skinX + SKIN_SIZE + PADDING;
        int textY = top + (height - minecraft.font.lineHeight) / 2;

        GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL);

        renderElement(poseStack, index, top, left, width, height, mouseX, mouseY, hovered, delta, skinX, skinY, textX, textY);
    }

    public void renderElement(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta, int skinX, int skinY, int textX, int textY) {
        boolean selected = team.equals(screen.getSelected());
        if (selected) {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL);
        }

        if (bannerItem != null) {
            GuiComponent.blit(poseStack, left, top, left + width, top + height, 0, 0, 256, 256);
            Lighting.setupForFlatItems();

            MultiBufferSource.BufferSource multibuffersource$buffersource = this.minecraft.renderBuffers().bufferSource();
            poseStack.pushPose();
            poseStack.translate(left + 10, top + 20, 0.0D);
            poseStack.scale(15.0F, -15.0F, 1.0F);
            float f = 0.6666667F;
            poseStack.scale(f, -f, -f);
            this.flag.xRot = 0.0F;
            this.flag.y = -32.0F;
            BannerRenderer.renderPatterns(poseStack, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true, this.resultBannerPatterns);
            poseStack.popPose();
            multibuffersource$buffersource.endBatch();

        }
        /*
        Integer teamColor = ChatFormatting.getById(team.getTeamColor()).getColor();
        int unitColor = TeamCreationScreen.RecruitColorID.get(team.getUnitColor());

        GuiComponent.fill(poseStack, left + 10, top , left + 200,top + 10, 0xFFFF0000);

        GuiComponent.fill(poseStack, left + 10, top + 20, left + 200,top + 10, 0x8000FF00);
         */
        minecraft.font.draw(poseStack, team.getTeamName(), (float) textX + 45, (float) textY, PLAYER_NAME_COLOR);
    }

    @Nullable
    public RecruitsTeam getTeamInfo() {
        return team;
    }

    @Override
    public ListScreenListBase<DiplomacyTeamEntry> getList() {
        return null;//screen.teamList;
    }
}