package com.talhanation.recruits.client.api.governor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A tab of an addon module in the Governor screen. Its data is what the module wrote in
 * {@code IGovernorModule#writeSnapshot}. The screen is rebuilt after every snapshot, so {@link #init}
 * runs again with the new data.
 * Register it once on the client with {@link GovernorTabs#register}, e.g. in FMLClientSetupEvent.
 * More tabs of one module show the same data, each with its own {@link #tabId}.
 */
@OnlyIn(Dist.CLIENT)
public interface IGovernorTab {

    /** Id of the module whose snapshot data the tab shows. */
    ResourceLocation moduleId();

    /** Id of the tab itself. A module can have more than one tab, each with its own id. Default: the module id. */
    default ResourceLocation tabId() {
        return moduleId();
    }

    Component getTitle();

    /** Shown only if this returns true. Default: the module sent data for this claim. */
    default boolean isVisible(CompoundTag data) {
        return !data.isEmpty();
    }

    /** Adds the widgets of the tab through {@link GovernorTabContext#addWidget}. */
    default void init(GovernorTabContext ctx) {
    }

    /** Draws the content (text, items) in the content area of the context. */
    void render(GuiGraphics guiGraphics, GovernorTabContext ctx, int mouseX, int mouseY);

    default boolean mouseScrolled(GovernorTabContext ctx, double mouseX, double mouseY, double delta) {
        return false;
    }

    default boolean mouseClicked(GovernorTabContext ctx, double mouseX, double mouseY, int button) {
        return false;
    }

    /** The Governor screen closes: free what the tab holds (textures). */
    default void onClose() {
    }
}
