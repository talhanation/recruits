package com.talhanation.recruits.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class FormationUtils {

    public static BlockPos calculateBlockPosition(Vec3 targetPos, Vec3 linePos, int index, Level level) {
         Vec3 toTarget = linePos.vectorTo(targetPos).normalize();
        Vec3 rotation = toTarget.yRot(3.14F/2).normalize();
        Vec3 pos = linePos.lerp(linePos.add(rotation), index * 1.25);


        return level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));

    }
}
