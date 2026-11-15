package com.talhanation.recruits.client.api.governor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** What a module tab sees: the claim, its data from the snapshot and the content area of the screen. */
@OnlyIn(Dist.CLIENT)
public class GovernorTabContext {

    private final Screen screen;
    private final UUID claimId;
    private final CompoundTag data;
    private final List<ChunkPos> claimChunks;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final Font font;
    private final Consumer<AbstractWidget> widgetAdder;

    public GovernorTabContext(Screen screen, UUID claimId, CompoundTag data, List<ChunkPos> claimChunks, int left, int top,
                              int width, int height, Font font, Consumer<AbstractWidget> widgetAdder) {
        this.screen = screen;
        this.claimId = claimId;
        this.data = data;
        this.claimChunks = claimChunks;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.font = font;
        this.widgetAdder = widgetAdder;
    }

    public Screen getScreen() {
        return screen;
    }

    public UUID getClaimId() {
        return claimId;
    }

    /** The module's data of the last snapshot, empty if it sent none. Do not change it. */
    public CompoundTag getData() {
        return data;
    }

    /** The chunks of the claim, e.g. for a {@code ClaimMapView}. Do not change the list. */
    public List<ChunkPos> getClaimChunks() {
        return claimChunks;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Font getFont() {
        return font;
    }

    /** Adds a widget (button, list) to the screen. Only during {@link IGovernorTab#init}. */
    public <T extends AbstractWidget> T addWidget(T widget) {
        widgetAdder.accept(widget);
        return widget;
    }
}
