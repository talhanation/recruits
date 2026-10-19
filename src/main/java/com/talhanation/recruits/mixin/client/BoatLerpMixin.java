package com.talhanation.recruits.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class BoatLerpMixin {
    @Shadow private double lerpX;
    @Shadow private double lerpY;
    @Shadow private double lerpZ;
    @Shadow private double lerpYRot;
    @Shadow private double lerpXRot;
    @Shadow private int lerpSteps;

    @Unique private boolean recruits$keepRotation;
    @Unique private boolean recruits$keepPosition;
    @Unique private double recruits$targetX;
    @Unique private double recruits$targetY;
    @Unique private double recruits$targetZ;
    @Unique private double recruits$targetYRot;
    @Unique private double recruits$targetXRot;

    @Inject(method = "lerpTo", at = @At("HEAD"))
    private void recruits$rememberTarget(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        boolean lerping = !teleport && this.lerpSteps > 0;
        this.recruits$keepRotation = lerping && yRot == self.getYRot() && xRot == self.getXRot();
        this.recruits$keepPosition = lerping && x == self.getX() && y == self.getY() && z == self.getZ();
        this.recruits$targetX = this.lerpX;
        this.recruits$targetY = this.lerpY;
        this.recruits$targetZ = this.lerpZ;
        this.recruits$targetYRot = this.lerpYRot;
        this.recruits$targetXRot = this.lerpXRot;
    }

    @Inject(method = "tickLerp", at = @At("TAIL"))
    private void recruits$keepYawContinuous(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        float jump = self.getYRot() - self.yRotO;
        if (jump > 180.0F || jump < -180.0F) self.yRotO += Math.round(jump / 360.0F) * 360.0F;
    }

    @Inject(method = "lerpTo", at = @At("TAIL"))
    private void recruits$keepTarget(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport, CallbackInfo ci) {
        if (this.recruits$keepRotation) {
            this.lerpYRot = this.recruits$targetYRot;
            this.lerpXRot = this.recruits$targetXRot;
        }
        if (this.recruits$keepPosition) {
            this.lerpX = this.recruits$targetX;
            this.lerpY = this.recruits$targetY;
            this.lerpZ = this.recruits$targetZ;
        }
    }
}
