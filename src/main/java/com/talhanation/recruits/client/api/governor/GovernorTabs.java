package com.talhanation.recruits.client.api.governor;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry of the Governor screen tabs of addon modules (client side). */
@OnlyIn(Dist.CLIENT)
public final class GovernorTabs {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<IGovernorTab> TABS = new CopyOnWriteArrayList<>();

    private GovernorTabs() {
    }

    /** Registers a tab once. A second tab with the same tab id is ignored. */
    public static void register(IGovernorTab tab) {
        for (IGovernorTab known : TABS) {
            if (known.tabId().equals(tab.tabId())) {
                LOGGER.warn("Governor tab {} is already registered", tab.tabId());
                return;
            }
        }
        TABS.add(tab);
    }

    public static List<IGovernorTab> getAll() {
        return Collections.unmodifiableList(TABS);
    }

    /** The tab with this tab id, null if none. */
    public static IGovernorTab get(ResourceLocation tabId) {
        for (IGovernorTab tab : TABS) if (tab.tabId().equals(tabId)) return tab;
        return null;
    }
}
