package com.talhanation.recruits.util;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FormationUtils {

    public static Vec3 calculateLineBlockPosition(Vec3 targetPos, Vec3 linePos, int size, int index, Level level) {
        Vec3 toTarget = linePos.vectorTo(targetPos).normalize();
        Vec3 rotation = toTarget.yRot(3.14F/2).normalize();
        Vec3 pos;
        if(index == 0 || size/index > size/2)
            pos = linePos.lerp(linePos.add(rotation), index * 1.50);
        else
            pos = linePos.lerp(linePos.add(rotation.reverse()), index * 1.50);

        BlockPos blockPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));

        return new Vec3(pos.x, blockPos.getY(), pos.z);
    }

    public static void lineUpFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {
        Vec3 forward = player.getForward();
        Vec3 left = new Vec3(-forward.z, forward.y, forward.x);

        int maxInRow = 20;
        double spacing = 2.5;

        List<FormationPosition> possiblePositions = new ArrayList<>();

        for (int i = 0; i < recruits.size(); i++) {
            int row = i / maxInRow;
            int positionInRow = i % maxInRow;

            Vec3 basePos = targetPos.add(forward.scale(-3 * row));
            Vec3 offset = left.scale((positionInRow - maxInRow / 2F) * spacing);

            Vec3 recruitPos = basePos.add(offset);
            possiblePositions.add(new FormationPosition(recruitPos, true));
        }

        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 pos = null;

            if (recruit.formationPos >= 0 && recruit.formationPos < possiblePositions.size() && possiblePositions.get(recruit.formationPos).isFree) {
                FormationPosition position = possiblePositions.get(recruit.formationPos);
                position.isFree = false;
                pos = position.position;
            }
            else {
                for(int i = 0; i < possiblePositions.size(); i++){
                    FormationPosition position = possiblePositions.get(i);
                    if(position.isFree){
                        pos = possiblePositions.get(i).position;
                        recruit.formationPos = i;
                        position.isFree = false;
                        break;
                    }
                }
            }

            if(pos != null){
                BlockPos blockPos = player.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));

                recruit.setHoldPos(new Vec3(pos.x, blockPos.getY(), pos.z));
                recruit.ownerRot = player.getYRot();
                recruit.setFollowState(3);
            }
        }
    }

    public static void squareFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {

    }

    public static void triangleFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {

    }

    public static class FormationPosition{
        public Vec3 position;
        public boolean isFree;

        FormationPosition(Vec3 position, boolean isFree){
            this.position = position;
            this.isFree = isFree;
        }
    }

    public static Vec3 getCenterOfPositions(List<AbstractRecruitEntity> recruits, ServerLevel level) {
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;

        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 pos = recruit.position();
            sumX += pos.x;
            sumY += pos.y;
            sumZ += pos.z;
        }

        double centerX = sumX / recruits.size();
        double centerY = sumY / recruits.size();
        double centerZ = sumZ / recruits.size();

        BlockPos blockPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(centerX, centerY, centerZ));

        return new Vec3(centerX, blockPos.getY(), centerZ);
    }
}
