package com.talhanation.recruits.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {

    // Optional compat mixins must not be applied when their target mod is missing.
    private static final Map<String, String> OPTIONAL_TARGETS = Map.of();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String optionalTarget = OPTIONAL_TARGETS.get(mixinClassName);
        return optionalTarget == null || isClassPresent(optionalTarget);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String className) {
        String resourcePath = className.replace('.', '/') + ".class";
        return hasResource(Thread.currentThread().getContextClassLoader(), resourcePath)
                || hasResource(OptionalCompatMixinPlugin.class.getClassLoader(), resourcePath);
    }

    private static boolean hasResource(ClassLoader classLoader, String resourcePath) {
        return classLoader != null && classLoader.getResource(resourcePath) != null;
    }
}
