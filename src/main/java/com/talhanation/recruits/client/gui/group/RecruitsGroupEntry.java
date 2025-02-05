package com.talhanation.recruits.client.gui.group;


import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class RecruitsGroupEntry extends ListScreenEntryBase<RecruitsGroupEntry> {
    protected static final int SKIN_SIZE = 24;
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);

    protected final Minecraft minecraft;
    protected final RecruitsGroupListScreen screen;
    protected final @NotNull RecruitsGroup group;

    public RecruitsGroupEntry(RecruitsGroupListScreen screen, @NotNull RecruitsGroup group) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.group = group;
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
        boolean selected = group.equals(screen.getSelected());
        if (selected) {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL);
        }

        minecraft.font.draw(poseStack, group.getName(), (float) textX + 15, (float) textY,  PLAYER_NAME_COLOR);

    }
    public RecruitsGroup getGroup() {
        return group;
    }

    @Override
    public ListScreenListBase<RecruitsGroupEntry> getList() {
        return screen.groupList;
    }
}

