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
        Vec3 forward = player.getForward();
        Vec3 left = new Vec3(-forward.z, forward.y, forward.x);

        double spacing = 2.5;

        int numRecruits = recruits.size();
        int sideLength = (int) Math.ceil(Math.sqrt(numRecruits));

        List<FormationPosition> possiblePositions = new ArrayList<>();

        for (int i = 0; i < numRecruits; i++) {
            int row = i / sideLength;
            int col = i % sideLength;

            Vec3 basePos = targetPos.add(forward.scale(-3 * row));
            Vec3 offset = left.scale((col - sideLength / 2F) * spacing);

            Vec3 recruitPos = basePos.add(offset);
            possiblePositions.add(new FormationPosition(recruitPos, true));
        }

        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 pos = null;

            if (recruit.formationPos >= 0 && recruit.formationPos < possiblePositions.size() && possiblePositions.get(recruit.formationPos).isFree) {
                FormationPosition position = possiblePositions.get(recruit.formationPos);
                position.isFree = false;
                pos = position.position;
            } else {
                for (int i = 0; i < possiblePositions.size(); i++) {
                    FormationPosition position = possiblePositions.get(i);
                    if (position.isFree) {
                        pos = possiblePositions.get(i).position;
                        recruit.formationPos = i;
                        position.isFree = false;
                        break;
                    }
                }
            }

            if (pos != null) {
                BlockPos blockPos = player.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));

                recruit.setHoldPos(new Vec3(pos.x, blockPos.getY(), pos.z));
                recruit.ownerRot = player.getYRot();
                recruit.setFollowState(3);
            }
        }
    }


    public static void triangleFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {
        Vec3 forward = player.getForward();
        Vec3 left = new Vec3(-forward.z, forward.y, forward.x);

        double spacing = 2.5;
        int numRecruits = recruits.size();

        List<FormationPosition> possiblePositions = new ArrayList<>();

        int index = 0;
        int rowCount = 1;
        while (index < numRecruits) {
            for (int positionInRow = 0; positionInRow < rowCount && index < numRecruits; positionInRow++, index++) {
                Vec3 basePos = targetPos.add(forward.scale(-3 * (rowCount - 1)));
                Vec3 offset = left.scale((positionInRow - (rowCount - 1) / 2F) * spacing);

                Vec3 recruitPos = basePos.add(offset);
                possiblePositions.add(new FormationPosition(recruitPos, true));
            }
            rowCount++;
        }

        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 pos = null;

            if (recruit.formationPos >= 0 && recruit.formationPos < possiblePositions.size() && possiblePositions.get(recruit.formationPos).isFree) {
                FormationPosition position = possiblePositions.get(recruit.formationPos);
                position.isFree = false;
                pos = position.position;
            } else {
                for (int i = 0; i < possiblePositions.size(); i++) {
                    FormationPosition position = possiblePositions.get(i);
                    if (position.isFree) {
                        pos = possiblePositions.get(i).position;
                        recruit.formationPos = i;
                        position.isFree = false;
                        break;
                    }
                }
            }

            if (pos != null) {
                BlockPos blockPos = player.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));

                recruit.setHoldPos(new Vec3(pos.x, blockPos.getY(), pos.z));
                recruit.ownerRot = player.getYRot();
                recruit.setFollowState(3);
            }
        }
    }

    public static void circleFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {
        double spacing = 2.5; // Distance between recruits in the circle
        int numRecruits = recruits.size();

        double radius = spacing * numRecruits / (2 * Math.PI); // Calculate radius based on the number of recruits
        List<FormationPosition> possiblePositions = new ArrayList<>();

        for (int i = 0; i < numRecruits; i++) {
            double angle = (2 * Math.PI / numRecruits) * i; // Angle for each recruit

            // Calculate position for each recruit in the circle
            double x = targetPos.x + radius * Math.cos(angle);
            double z = targetPos.z + radius * Math.sin(angle);
            Vec3 recruitPos = new Vec3(x, targetPos.y, z);

            possiblePositions.add(new FormationPosition(recruitPos, true));
        }

        for (AbstractRecruitEntity recruit : recruits) {
            Vec3 pos = null;

            if (recruit.formationPos >= 0 && recruit.formationPos < possiblePositions.size() && possiblePositions.get(recruit.formationPos).isFree) {
                FormationPosition position = possiblePositions.get(recruit.formationPos);
                position.isFree = false;
                pos = position.position;
            } else {
                for (int i = 0; i < possiblePositions.size(); i++) {
                    FormationPosition position = possiblePositions.get(i);
                    if (position.isFree) {
                        pos = possiblePositions.get(i).position;
                        recruit.formationPos = i;
                        position.isFree = false;
                        break;
                    }
                }
            }

            if (pos != null) {
                BlockPos blockPos = player.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));

                recruit.setHoldPos(new Vec3(pos.x, blockPos.getY(), pos.z));
                recruit.ownerRot = player.getYRot();
                recruit.setFollowState(3);
            }
        }
    }


    public static void hollowSquareFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {
        int recruitsPerSide = Math.max(2, recruits.size() / 4); // Ensure at least 2 recruits per side
        double spacing = 2.5;

        int totalRecruitsNeeded = recruitsPerSide * 4;
        if (totalRecruitsNeeded > recruits.size()) {
            recruitsPerSide = recruits.size() / 4;
            totalRecruitsNeeded = recruitsPerSide * 4;
        }

        List<FormationPosition> possiblePositions = new ArrayList<>();

        for (int row = 0; row < 2; row++) { // Two rows per side
            double offset = (spacing * recruitsPerSide) / 2.0;
            for (int i = 0; i < recruitsPerSide; i++) {
                double positionOffset = i * spacing - offset;
                // Top side
                possiblePositions.add(new FormationPosition(targetPos.add(positionOffset, 0, -offset - row * spacing), true));
                // Bottom side
                possiblePositions.add(new FormationPosition(targetPos.add(positionOffset, 0, offset + row * spacing), true));
                // Left side
                possiblePositions.add(new FormationPosition(targetPos.add(-offset - row * spacing, 0, positionOffset), true));
                // Right side
                possiblePositions.add(new FormationPosition(targetPos.add(offset + row * spacing, 0, positionOffset), true));
            }
        }

        for (int i = 0; i < recruits.size() && i < possiblePositions.size(); i++) {
            AbstractRecruitEntity recruit = recruits.get(i);
            Vec3 pos = possiblePositions.get(i).position;

            BlockPos blockPos = player.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));
            recruit.setHoldPos(new Vec3(pos.x, blockPos.getY(), pos.z));
            recruit.ownerRot = player.getYRot();
            recruit.setFollowState(3);
        }
    }

    public static void vFormation(ServerPlayer player, List<AbstractRecruitEntity> recruits, Vec3 targetPos) {
        Vec3 forward = player.getForward();
        Vec3 left = new Vec3(-forward.z, forward.y, forward.x);

        double spacing = 2.5;
        int recruitsPerWing = recruits.size() / 2;

        List<FormationPosition> possiblePositions = new ArrayList<>();

        for (int i = 0; i < recruitsPerWing; i++) {
            double offset = i * spacing;


            Vec3 rightWingPos = targetPos.add(forward.scale(offset)).add(left.scale(offset));
            possiblePositions.add(new FormationPosition(rightWingPos, true));


            Vec3 leftWingPos = targetPos.add(forward.scale(offset)).subtract(left.scale(offset));
            possiblePositions.add(new FormationPosition(leftWingPos, true));
        }


        if (recruits.size() % 2 != 0) {
            possiblePositions.add(new FormationPosition(targetPos, true));
        }


        for (int i = 0; i < recruits.size() && i < possiblePositions.size(); i++) {
            AbstractRecruitEntity recruit = recruits.get(i);
            Vec3 pos = possiblePositions.get(i).position;

            BlockPos blockPos = player.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));
            recruit.setHoldPos(new Vec3(pos.x, blockPos.getY(), pos.z));
            recruit.ownerRot = player.getYRot();
            recruit.setFollowState(3);
        }
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
