package com.talhanation.recruits.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class FormationUtils {

    public static Vec3 calculateLineBlockPosition(Vec3 targetPos, Vec3 linePos, int size, int index, Level level) {

        Vec3 toTarget = linePos.vectorTo(targetPos).normalize();
        Vec3 rotation = toTarget.yRot(3.14F/2).normalize();
        Vec3 pos;
        if(index == 0 || size/index > size/2)
            pos = linePos.lerp(linePos.add(rotation), index * 1.50);
        else
            pos = linePos.lerp(linePos.add(rotation.reverse()), index * 1.50);

        BlockPos blockPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));

        return new Vec3(pos.x, blockPos.getY(), pos.z);
    }

    public static Vec3 getVectorRight(Vec3 pos, Vec3 dir){
        return pos.add(dir.yRot(3.14F / 2));
    }

    public static Vec3 getVectorLeft(Vec3 pos, Vec3 dir){
        return pos.add(dir.yRot(-3.14F / 2));
    }
}
