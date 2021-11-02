package com.talhanation.recruits.mixin;

import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.SummonCommand;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Objects;

@Mixin({SummonCommand.class})
public class SummonCommandMixin {

    @Inject(at = @At(value = "RETURN"), method = "spawnEntity", locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void spawnEntityLog(CommandSource p_198737_0_, ResourceLocation p_198737_1_, Vector3d p_198737_2_, CompoundNBT p_198737_3_, boolean p_198737_4_, CallbackInfoReturnable<Integer> cir, BlockPos blockpos, CompoundNBT compoundnbt) {
        System.out.println(compoundnbt);
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("Passengers", Objects.requireNonNull(compoundnbt.get("Passengers")));
        System.out.println(nbt);
    }
}
