package com.talhanation.recruits.client.gui.group;


import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class RecruitsGroupEntry extends ListScreenEntryBase<RecruitsGroupEntry> {
    protected static final int SKIN_SIZE = 24;
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 80, 80, 80);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);

    protected final Minecraft minecraft;
    protected final IGroupSelection screen;
    protected final @NotNull RecruitsGroup group;
    protected ResourceLocation image;
    public RecruitsGroupEntry(IGroupSelection screen, @NotNull RecruitsGroup group) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.group = group;
        this.image = RecruitsGroup.IMAGES.get(group.getImage());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
        int skinX = left + PADDING;
        int skinY = top + (height - SKIN_SIZE) / 2;
        int textX = skinX + SKIN_SIZE + PADDING;
        int textY = top + (height - minecraft.font.lineHeight) / 2;

        guiGraphics.fill(left, top, left + width, top + height, BG_FILL);

        renderElement(guiGraphics, index, top, left, width, height, mouseX, mouseY, hovered, delta, skinX, skinY, textX, textY);
    }

    public void renderElement(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta, int skinX, int skinY, int textX, int textY) {
        boolean selected = screen.getSelected() != null && group.getUUID().equals(screen.getSelected().getUUID());
        if (selected) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL);
        }

        if(this.image != null){
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.setShaderTexture(0, this.image);
            guiGraphics.blit(this.image,  left + 5,  top + 5, 0, 0, 21, 21, 21, 21);
        }

        guiGraphics.drawString(minecraft.font, group.getName(), (float) textX + 20, (float) textY,  PLAYER_NAME_COLOR, false);
        guiGraphics.drawString(minecraft.font, "[" + group.getCount() + "/" + group.getSize() + "]", (float) textX + 150, (float) textY,  PLAYER_NAME_COLOR, false);

    }
    public RecruitsGroup getGroup() {
        return group;
    }

    @Override
    public ListScreenListBase<RecruitsGroupEntry> getList() {
        return screen.getGroupList();
    }
}

