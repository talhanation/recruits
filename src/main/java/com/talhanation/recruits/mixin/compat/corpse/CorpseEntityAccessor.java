package com.talhanation.recruits.mixin.compat.corpse;

import de.maxhenkel.corpse.corelib.death.Death;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "de.maxhenkel.corpse.entities.CorpseEntity", remap = false)
public interface CorpseEntityAccessor {

    @Accessor(value = "death", remap = false)
    void recruits$setDeath(Death death);
}
