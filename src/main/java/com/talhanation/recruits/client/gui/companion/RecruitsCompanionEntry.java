package com.talhanation.recruits.client.gui.companion;

import com.talhanation.recruits.client.gui.component.UnitIcons;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.CompanionRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/** One companion in the {@link RecruitsCompanionList}: unit icon, name with type, and where he is. */
@OnlyIn(Dist.CLIENT)
public class RecruitsCompanionEntry extends ListScreenEntryBase<RecruitsCompanionEntry> {
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int BG_ORDERS = FastColor.ARGB32.color(255, 60, 160, 60);
    protected static final int NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    protected static final int NAME_COLOR_DISABLED = FastColor.ARGB32.color(255, 140, 140, 140);
    protected static final int INFO_COLOR = FastColor.ARGB32.color(255, 180, 180, 180);

    protected final Minecraft minecraft;
    protected final ICompanionSelection screen;
    protected final @NotNull CompanionRecord companion;
    protected final ResourceLocation icon;
    protected final String title;
    protected final String info;
    /** Orders can be written for him; the others are only listed. */
    protected final boolean usable;

    public RecruitsCompanionEntry(ICompanionSelection screen, @NotNull CompanionRecord companion, String title, String info, boolean usable) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.companion = companion;
        this.icon = UnitIcons.ofEntityId(companion.type);
        this.title = title;
        this.info = info;
        this.usable = usable;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
        CompanionRecord selected = screen.getSelected();
        if (selected != null && selected.uuid.equals(companion.uuid)) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL);
        }
        // a green edge: the Messenger is holding orders for him
        if (companion.uuid.equals(screen.getOrdersCompanion())) {
            guiGraphics.fill(left, top, left + 2, top + height, BG_ORDERS);
        }

        int iconX = left + PADDING + 2;
        int iconY = top + (height - UnitIcons.SIZE) / 2;
        UnitIcons.render(guiGraphics, icon, iconX, iconY);

        int textX = iconX + UnitIcons.SIZE + PADDING + 2;
        int textWidth = left + width - textX - PADDING;
        int textY = top + (height - 2 * minecraft.font.lineHeight - 2) / 2;
        guiGraphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(title, textWidth), textX, textY,
                usable ? NAME_COLOR : NAME_COLOR_DISABLED, false);
        guiGraphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(info, textWidth), textX, textY + minecraft.font.lineHeight + 2,
                INFO_COLOR, false);
    }

    @NotNull
    public CompanionRecord getCompanion() {
        return companion;
    }

    public String getTitle() {
        return title;
    }

    public boolean isUsable() {
        return usable;
    }

    @Override
    public ListScreenListBase<RecruitsCompanionEntry> getList() {
        return screen.getCompanionList();
    }
}
