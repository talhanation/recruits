package com.talhanation.recruits.api.governor;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Registry of the Governor modules of addons. */
public final class GovernorModules {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<IGovernorModule> MODULES = new CopyOnWriteArrayList<>();

    private GovernorModules() {
    }

    /** Registers a module once, e.g. in FMLCommonSetupEvent. A second module with the same id is ignored. */
    public static void register(IGovernorModule module) {
        for (IGovernorModule known : MODULES) {
            if (known.id().equals(module.id())) {
                LOGGER.warn("Governor module {} is already registered", module.id());
                return;
            }
        }
        MODULES.add(module);
        LOGGER.info("Governor module registered: {}", module.id());
    }

    public static List<IGovernorModule> getAll() {
        return Collections.unmodifiableList(MODULES);
    }

    public static boolean isEmpty() {
        return MODULES.isEmpty();
    }

    public static boolean has(ResourceLocation id) {
        for (IGovernorModule module : MODULES) if (module.id().equals(id)) return true;
        return false;
    }

    /** Calls the hook on every module; a module that throws is logged and the others still run. */
    public static void forEach(String hook, Consumer<IGovernorModule> call) {
        for (IGovernorModule module : MODULES) {
            try {
                call.accept(module);
            } catch (Exception e) {
                LOGGER.error("Governor module {} failed in {}", module.id(), hook, e);
            }
        }
    }
}
