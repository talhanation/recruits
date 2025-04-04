package com.talhanation.recruits.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class Kalkuel {

    public static double horizontalAngleBetweenVectors(Vec3 vector1, Vec3 vector2) {
        double dotProduct = vector1.x * vector2.x + vector1.z * vector2.z;
        double magnitude1 = Math.sqrt(vector1.x * vector1.x + vector1.z * vector1.z);
        double magnitude2 = Math.sqrt(vector2.x * vector2.x + vector2.z * vector2.z);

        double cosTheta = dotProduct / (magnitude1 * magnitude2);

        return Math.toDegrees(Math.acos(cosTheta));
    }

    /**
     * Adds from the provided number, but does not cross the set point
     *
     * @param current the current number
     * @param positiveChange the amount to add
     * @param setPoint the amount to not cross
     * @return the resulting number
     */
    public static float addToSetPoint(float current, float positiveChange, float setPoint) {
        if (current < setPoint) {
            current = current + positiveChange;
        }
        return current;
    }


    public static double calculateMotionX(float speed, float rotationYaw) {
        return Mth.sin(-rotationYaw * 0.017453292F) * speed;
    }

    public static double calculateMotionZ(float speed, float rotationYaw) {
        return Mth.cos(rotationYaw * 0.017453292F) * speed;
    }

    /**
     * Subtracts from the provided number, but does not cross zero
     *
     * @param num the number
     * @param sub the amount to subtract
     * @return the resulting number
     */
    static float subtractToZero(float num, float sub) {
        float erg;
        if (num < 0F) {
            erg = num + sub;
            if (erg > 0F) {
                erg = 0F;
            }
        }
        else {
            erg = num - sub;
            if (erg < 0F) {
                erg = 0F;
            }
        }
        return erg;
    }


    public static class SailPointCalculator {

        private static final Random random = new Random();

        public static Vec3 getRandomSailPoint(Level world, Vec3 origin, Vec3 direction, double radius) {
            for (int i = 0; i < 10; i++) {
                Vec3 normDir = direction.normalize();
                double baseAngle = Math.atan2(normDir.z, normDir.x);
                double angleOffset = (random.nextDouble() - 0.5) * (Math.PI / 2);
                double newAngle = baseAngle + angleOffset;
                double distance = random.nextDouble() * radius;

                double offsetX = Math.cos(newAngle) * distance;
                double offsetZ = Math.sin(newAngle) * distance;
                Vec3 candidate = origin.add(offsetX, 0, offsetZ);

                if (hasWaterConnection(world, origin, candidate)) {
                    return candidate;
                }
            }
            return origin;
        }

        public static Vec3 getRandomSailPoint(Level world, Vec3 origin, double radius) {
            for (int i = 0; i < 10; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;
                Vec3 candidate = origin.add(offsetX, 0, offsetZ);

                if (hasWaterConnection(world, origin, candidate)) {
                    return candidate;
                }
            }
            return origin;
        }

        private static boolean hasWaterConnection(Level world, Vec3 origin, Vec3 target) {
            int steps = 20;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = origin.x + t * (target.x - origin.x);
                double y = origin.y + t * (target.y - origin.y);
                double z = origin.z + t * (target.z - origin.z);
                BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                if (!world.getBlockState(pos).getBlock().equals(Blocks.WATER)) {
                    return false;
                }
            }
            return true;
        }
    }
}
