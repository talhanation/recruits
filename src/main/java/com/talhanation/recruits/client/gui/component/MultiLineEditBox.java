package com.talhanation.recruits.client.gui.component;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiLineEditBox extends AbstractScrollWidget {
    private static final int CURSOR_INSERT_WIDTH = 1;
    private static final int CURSOR_INSERT_COLOR = -3092272;
    private static final String CURSOR_APPEND_CHARACTER = "_";
    private static final int TEXT_COLOR = -2039584;
    private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
    private final Font font;
    private final Component placeholder;
    private final MultilineTextField textField;
    private int frame;

    public MultiLineEditBox(Font p_239008_, int p_239009_, int p_239010_, int p_239011_, int p_239012_, Component p_239013_, Component p_239014_) {
        super(p_239009_, p_239010_, p_239011_, p_239012_, p_239014_);
        this.font = p_239008_;
        this.placeholder = p_239013_;
        this.textField = new MultilineTextField(p_239008_, p_239011_ - this.totalInnerPadding());
        this.textField.setCursorListener(this::scrollToCursor);
    }

    public void setCharacterLimit(int p_239314_) {
        this.textField.setCharacterLimit(p_239314_);
    }

    public void setValueListener(Consumer<String> p_239274_) {
        this.textField.setValueListener(p_239274_);
    }

    public void setValue(String p_240160_) {
        this.textField.setValue(p_240160_);
    }

    public String getValue() {
        return this.textField.value();
    }

    public void tick() {
        ++this.frame;
    }

    public void updateNarration(NarrationElementOutput p_240122_) {
        p_240122_.add(NarratedElementType.TITLE, new TranslatableComponent("narration.edit_box", this.getValue()));
    }

    public boolean mouseClicked(double p_239101_, double p_239102_, int p_239103_) {
        if (super.mouseClicked(p_239101_, p_239102_, p_239103_)) {
            return true;
        } else if (this.withinContentAreaPoint(p_239101_, p_239102_) && p_239103_ == 0) {
            this.textField.setSelecting(Screen.hasShiftDown());
            this.seekCursorScreen(p_239101_, p_239102_);
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseDragged(double p_238978_, double p_238979_, int p_238980_, double p_238981_, double p_238982_) {
        if (super.mouseDragged(p_238978_, p_238979_, p_238980_, p_238981_, p_238982_)) {
            return true;
        } else if (this.withinContentAreaPoint(p_238978_, p_238979_) && p_238980_ == 0) {
            this.textField.setSelecting(true);
            this.seekCursorScreen(p_238978_, p_238979_);
            this.textField.setSelecting(Screen.hasShiftDown());
            return true;
        } else {
            return false;
        }
    }

    public boolean keyPressed(int p_239433_, int p_239434_, int p_239435_) {
        return this.textField.keyPressed(p_239433_);
    }

    public boolean charTyped(char p_239387_, int p_239388_) {
        if (this.visible && this.isFocused() && SharedConstants.isAllowedChatCharacter(p_239387_)) {
            this.textField.insertText(Character.toString(p_239387_));
            return true;
        } else {
            return false;
        }
    }

    protected void renderContents(PoseStack p_239001_, int p_239002_, int p_239003_, float p_239004_) {
        String s = this.textField.value();
        if (s.isEmpty() && !this.isFocused()) {
            this.font.drawWordWrap(this.placeholder, this.x + this.innerPadding(), this.y + this.innerPadding(), this.width - this.totalInnerPadding(), -857677600);
        } else {
            int i = this.textField.cursor();
            boolean flag = this.isFocused() && this.frame / 6 % 2 == 0;
            boolean flag1 = i < s.length();
            int j = 0;
            int k = 0;
            int l = this.y + this.innerPadding();

            for (MultilineTextField.StringView multilinetextfield$stringview : this.textField.iterateLines()) {
                boolean flag2 = this.withinContentAreaTopBottom(l, l + 9);
                if (flag && flag1 && i >= multilinetextfield$stringview.beginIndex() && i <= multilinetextfield$stringview.endIndex()) {
                    if (flag2) {
                        j = this.font.drawShadow(p_239001_, s.substring(multilinetextfield$stringview.beginIndex(), i), (float) (this.x + this.innerPadding()), (float) l, -2039584) - 1;
                        GuiComponent.fill(p_239001_, j, l - 1, j + 1, l + 1 + 9, -3092272);
                        this.font.drawShadow(p_239001_, s.substring(i, multilinetextfield$stringview.endIndex()), (float) j, (float) l, -2039584);
                    }
                } else {
                    if (flag2) {
                        j = this.font.drawShadow(p_239001_, s.substring(multilinetextfield$stringview.beginIndex(), multilinetextfield$stringview.endIndex()), (float) (this.x + this.innerPadding()), (float) l, -2039584) - 1;
                    }

                    k = l;
                }

                l += 9;
            }

            if (flag && !flag1 && this.withinContentAreaTopBottom(k, k + 9)) {
                this.font.drawShadow(p_239001_, "_", (float) j, (float) k, -3092272);
            }

            if (this.textField.hasSelection()) {
                MultilineTextField.StringView multilinetextfield$stringview2 = this.textField.getSelected();
                int k1 = this.x + this.innerPadding();
                l = this.y + this.innerPadding();

                for (MultilineTextField.StringView multilinetextfield$stringview1 : this.textField.iterateLines()) {
                    if (multilinetextfield$stringview2.beginIndex() > multilinetextfield$stringview1.endIndex()) {
                        l += 9;
                    } else {
                        if (multilinetextfield$stringview1.beginIndex() > multilinetextfield$stringview2.endIndex()) {
                            break;
                        }

                        if (this.withinContentAreaTopBottom(l, l + 9)) {
                            int i1 = this.font.width(s.substring(multilinetextfield$stringview1.beginIndex(), Math.max(multilinetextfield$stringview2.beginIndex(), multilinetextfield$stringview1.beginIndex())));
                            int j1;
                            if (multilinetextfield$stringview2.endIndex() > multilinetextfield$stringview1.endIndex()) {
                                j1 = this.width - this.innerPadding();
                            } else {
                                j1 = this.font.width(s.substring(multilinetextfield$stringview1.beginIndex(), multilinetextfield$stringview2.endIndex()));
                            }

                            this.renderHighlight(p_239001_, k1 + i1, l, k1 + j1, l + 9);
                        }

                        l += 9;
                    }
                }
            }

        }
    }

    protected void renderDecorations(PoseStack p_239517_) {
        super.renderDecorations(p_239517_);
        if (this.textField.hasCharacterLimit()) {
            int i = this.textField.characterLimit();
            Component component = new TranslatableComponent("gui.multiLineEditBox.character_limit", this.textField.value().length(), i);
            drawString(p_239517_, this.font, component, this.x + this.width - this.font.width(component), this.y + this.height + 4, 10526880);
        }

    }

    public int getInnerHeight() {
        return 9 * this.textField.getLineCount();
    }

    protected boolean scrollbarVisible() {
        return (double) this.textField.getLineCount() > this.getDisplayableLineCount();
    }

    protected double scrollRate() {
        return 9.0D / 2.0D;
    }

    private void renderHighlight(PoseStack p_239487_, int p_239488_, int p_239489_, int p_239490_, int p_239491_) {
        Matrix4f matrix4f = p_239487_.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, 1.0F, 1.0F);
        RenderSystem.disableTexture();
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.vertex(matrix4f, (float) p_239488_, (float) p_239491_, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f, (float) p_239490_, (float) p_239491_, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f, (float) p_239490_, (float) p_239489_, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f, (float) p_239488_, (float) p_239489_, 0.0F).endVertex();
        tesselator.end();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableColorLogicOp();
        RenderSystem.enableTexture();
    }

    private void scrollToCursor() {
        double d0 = this.scrollAmount();
        MultilineTextField.StringView multilinetextfield$stringview = this.textField.getLineView((int) (d0 / 9.0D));
        if (this.textField.cursor() <= multilinetextfield$stringview.beginIndex()) {
            d0 = (double) (this.textField.getLineAtCursor() * 9);
        } else {
            MultilineTextField.StringView multilinetextfield$stringview1 = this.textField.getLineView((int) ((d0 + (double) this.height) / 9.0D) - 1);
            if (this.textField.cursor() > multilinetextfield$stringview1.endIndex()) {
                d0 = (double) (this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding());
            }
        }

        this.setScrollAmount(d0);
    }

    private double getDisplayableLineCount() {
        return (double) (this.height - this.totalInnerPadding()) / 9.0D;
    }

    private void seekCursorScreen(double p_239276_, double p_239277_) {
        double d0 = p_239276_ - (double) this.x - (double) this.innerPadding();
        double d1 = p_239277_ - (double) this.y - (double) this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(d0, d1);
    }
}