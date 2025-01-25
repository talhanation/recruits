package com.talhanation.recruits.mixin;

import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The last word stays for RECRUITS AHAHAHAHAHAHAHA
// I made the priority so big because Canary also modifies this part of Minecraft code
@Mixin(value = ClassInstanceMultiMap.class, priority = Integer.MAX_VALUE)
public class ClassInstanceMultiMapMixin {
    @Mutable
    @Shadow
    @Final
    private Map<Class<?>, List<?>> byClass;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Class<?> elementType, CallbackInfo ci) {
        this.byClass = new ConcurrentHashMap<>(this.byClass);
    }
}
