package com.talhanation.recruits.client.gui.component;


import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractScrollWidget extends AbstractWidget implements Widget, GuiEventListener {
    private static final int BORDER_COLOR_FOCUSED = -1;
    private static final int BORDER_COLOR = -6250336;
    private static final int BACKGROUND_COLOR = -16777216;
    private static final int INNER_PADDING = 4;
    private double scrollAmount;
    private boolean scrolling;

    public AbstractScrollWidget(int p_240025_, int p_240026_, int p_240027_, int p_240028_, Component p_240029_) {
        super(p_240025_, p_240026_, p_240027_, p_240028_, p_240029_);
    }

    public boolean mouseClicked(double p_240170_, double p_240171_, int p_240172_) {
        if (!this.visible) {
            return false;
        } else {
            boolean flag = this.withinContentAreaPoint(p_240170_, p_240171_);
            boolean flag1 = this.scrollbarVisible() && p_240170_ >= (double)(this.x + this.width) && p_240170_ <= (double)(this.x + this.width + 8) && p_240171_ >= (double)this.y && p_240171_ < (double)(this.y + this.height);
            this.setFocused(flag || flag1);
            if (flag1 && p_240172_ == 0) {
                this.scrolling = true;
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean mouseReleased(double p_239063_, double p_239064_, int p_239065_) {
        if (p_239065_ == 0) {
            this.scrolling = false;
        }

        return super.mouseReleased(p_239063_, p_239064_, p_239065_);
    }

    public boolean mouseDragged(double p_239639_, double p_239640_, int p_239641_, double p_239642_, double p_239643_) {
        if (this.visible && this.isFocused() && this.scrolling) {
            if (p_239640_ < (double)this.y) {
                this.setScrollAmount(0.0D);
            } else if (p_239640_ > (double)(this.y + this.height)) {
                this.setScrollAmount((double)this.getMaxScrollAmount());
            } else {
                int i = this.getScrollBarHeight();
                double d0 = (double)Math.max(1, this.getMaxScrollAmount() / (this.height - i));
                this.setScrollAmount(this.scrollAmount + p_239643_ * d0);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean mouseScrolled(double p_239308_, double p_239309_, double p_239310_) {
        if (this.visible && this.isFocused()) {
            this.setScrollAmount(this.scrollAmount - p_239310_ * this.scrollRate());
            return true;
        } else {
            return false;
        }
    }

    public void renderButton(PoseStack p_239793_, int p_239794_, int p_239795_, float p_239796_) {
        if (this.visible) {
            this.renderBackground(p_239793_);
            enableScissor(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1);
            p_239793_.pushPose();
            p_239793_.translate(0.0D, -this.scrollAmount, 0.0D);
            this.renderContents(p_239793_, p_239794_, p_239795_, p_239796_);
            p_239793_.popPose();
            disableScissor();
            this.renderDecorations(p_239793_);
        }
    }

    private int getScrollBarHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    protected void renderDecorations(PoseStack p_239981_) {
        if (this.scrollbarVisible()) {
            this.renderScrollBar();
        }

    }

    protected int innerPadding() {
        return 4;
    }

    protected int totalInnerPadding() {
        return this.innerPadding() * 2;
    }

    protected double scrollAmount() {
        return this.scrollAmount;
    }

    protected void setScrollAmount(double p_240207_) {
        this.scrollAmount = Mth.clamp(p_240207_, 0.0D, (double)this.getMaxScrollAmount());
    }

    protected int getMaxScrollAmount() {
        return Math.max(0, this.getContentHeight() - (this.height - 4));
    }

    private int getContentHeight() {
        return this.getInnerHeight() + 4;
    }

    private void renderBackground(PoseStack p_240049_) {
        int i = this.isFocused() ? -1 : -6250336;
        fill(p_240049_, this.x, this.y, this.x + this.width, this.y + this.height, i);
        fill(p_240049_, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, -16777216);
    }

    private void renderScrollBar() {
        int i = this.getScrollBarHeight();
        int j = this.x + this.width;
        int k = this.x + this.width + 8;
        int l = Math.max(this.y, (int)this.scrollAmount * (this.height - i) / this.getMaxScrollAmount() + this.y);
        int i1 = l + i;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex((double)j, (double)i1, 0.0D).color(128, 128, 128, 255).endVertex();
        bufferbuilder.vertex((double)k, (double)i1, 0.0D).color(128, 128, 128, 255).endVertex();
        bufferbuilder.vertex((double)k, (double)l, 0.0D).color(128, 128, 128, 255).endVertex();
        bufferbuilder.vertex((double)j, (double)l, 0.0D).color(128, 128, 128, 255).endVertex();
        bufferbuilder.vertex((double)j, (double)(i1 - 1), 0.0D).color(192, 192, 192, 255).endVertex();
        bufferbuilder.vertex((double)(k - 1), (double)(i1 - 1), 0.0D).color(192, 192, 192, 255).endVertex();
        bufferbuilder.vertex((double)(k - 1), (double)l, 0.0D).color(192, 192, 192, 255).endVertex();
        bufferbuilder.vertex((double)j, (double)l, 0.0D).color(192, 192, 192, 255).endVertex();
        tesselator.end();
    }

    protected boolean withinContentAreaTopBottom(int p_239943_, int p_239944_) {
        return (double)p_239944_ - this.scrollAmount >= (double)this.y && (double)p_239943_ - this.scrollAmount <= (double)(this.y + this.height);
    }

    protected boolean withinContentAreaPoint(double p_239607_, double p_239608_) {
        return p_239607_ >= (double)this.x && p_239607_ < (double)(this.x + this.width) && p_239608_ >= (double)this.y && p_239608_ < (double)(this.y + this.height);
    }

    protected abstract int getInnerHeight();

    protected abstract boolean scrollbarVisible();

    protected abstract double scrollRate();

    protected abstract void renderContents(PoseStack p_239198_, int p_239199_, int p_239200_, float p_239201_);


    //FROM GuiComponent

    public void enableScissor(int p_239261_, int p_239262_, int p_239263_, int p_239264_) {
        Window window = Minecraft.getInstance().getWindow();
        int i = window.getHeight();
        double d0 = window.getGuiScale();
        double d1 = (double)p_239261_ * d0;
        double d2 = (double)i - (double)p_239264_ * d0;
        double d3 = (double)(p_239263_ - p_239261_) * d0;
        double d4 = (double)(p_239264_ - p_239262_) * d0;
        RenderSystem.enableScissor((int)d1, (int)d2, Math.max(0, (int)d3), Math.max(0, (int)d4));
    }

    public static void disableScissor() {
        RenderSystem.disableScissor();
    }
}