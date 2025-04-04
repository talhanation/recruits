package com.talhanation.recruits.util;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class WaterObstacleScanner {
    private final Level world;
    private final Entity ship;
    private double scanDistance = 4.0; // Scan-Entfernung in Blöcken
    private final int scanResolution = 10; // Anzahl der Punkte pro Richtung (je mehr, desto genauer)

    public WaterObstacleScanner(Level world, Entity ship) {
        this.world = world;
        this.ship = ship;
    }

    public enum Direction {
        FORWARD, LEFT, RIGHT, NONE
    }

    public int getObstaclesLeft() {
        return scanLeftSide(ship.getForward().normalize());
    }

    public int getObstaclesRight() {
        return scanRightSide(ship.getForward().normalize());
    }


    public int getObstaclesFront(int range) {
        scanDistance = range;
        return scanForward(ship.getForward().normalize());
    }


    private int scanForward(Vec3 forwardVector) {
        int obstacleCount = 0;

        if (hasObstacle(forwardVector, ship, scanDistance)) {
            obstacleCount++;
        }

        return obstacleCount;
    }

    private int scanRightSide(Vec3 forwardVector) {
        int obstacleCount = 0;
        forwardVector = rotateVector(forwardVector, 30);
        for (double angleOffset = 0; angleOffset <= 45; angleOffset += 15) {
            Vec3 scanDir = rotateVector(forwardVector, angleOffset);
            if (hasObstacle(scanDir, ship, scanDistance)) {
                obstacleCount++;
            }
        }

        return obstacleCount;
    }

    private int scanLeftSide(Vec3 forwardVector) {
        int obstacleCount = 0;
        forwardVector = rotateVector(forwardVector, -30);
        for (double angleOffset = 0; angleOffset >= -45; angleOffset -= 15) {
            Vec3 scanDir = rotateVector(forwardVector, angleOffset);
            if (hasObstacle(scanDir, ship, scanDistance)) {
                obstacleCount++;
            }
        }

        return obstacleCount;
    }


    public static boolean hasObstacle(Vec3 scanDirection, Entity ship, double scanDistance) {
        Vec3 shipPos = ship.position();
        Vec3 targetPos = shipPos.add(scanDirection.scale(scanDistance));

        BlockHitResult hitResult = ship.level().clip(new ClipContext(shipPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ship));

       if(hitResult.getType() == HitResult.Type.BLOCK){
           //world.setBlock(new BlockPos(hitResult.getBlockPos().getX(), (int) (hitResult.getBlockPos().getY() + 4), hitResult.getBlockPos().getZ()), Blocks.ICE.defaultBlockState(), 3);

           return true;
       }
        return false;
    }

    private Vec3 rotateVector(Vec3 forward, double angleOffset) {
        double angleRad = Math.toRadians(angleOffset);

        double rotatedX = forward.x * Math.cos(angleRad) - forward.z * Math.sin(angleRad);
        double rotatedZ = forward.x * Math.sin(angleRad) + forward.z * Math.cos(angleRad);

        return new Vec3(rotatedX, forward.y, rotatedZ);
    }
    private static final Set<Block> ALLOWED_WATER_BLOCKS = ImmutableSet.of(
            Blocks.WATER, Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS);

    public static boolean isWaterBlockPos(Level world, BlockPos pos){
        BlockState state = world.getBlockState(pos);

        return ALLOWED_WATER_BLOCKS.contains(state.getBlock());
    }
    public boolean isAirBlockPos(BlockPos pos){
        BlockState state = this.world.getBlockState(pos);

        return state.isAir();
    }
}

