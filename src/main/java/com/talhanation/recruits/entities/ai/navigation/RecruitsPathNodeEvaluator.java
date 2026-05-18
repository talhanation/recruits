package com.talhanation.recruits.entities.ai.navigation;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.pathfinder.*;
import org.jetbrains.annotations.NotNull;

public class RecruitsPathNodeEvaluator extends NodeEvaluator {

    private int x;
    private int y;
    private int z;
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        if(mob.isVehicle()){
            this.entityHeight = Mth.floor(mob.getBbHeight() + (float) getEntityHeight());
        }
        mob.setPathfindingMalus(PathType.WATER, 128.0F);
        mob.setPathfindingMalus(PathType.WATER_BORDER, 128.0F);
        mob.setPathfindingMalus(PathType.TRAPDOOR, -1.0F);
        mob.setPathfindingMalus(PathType.DAMAGE_FIRE, 32.0F);
        mob.setPathfindingMalus(PathType.DAMAGE_CAUTIOUS, 32.0F);
        mob.setPathfindingMalus(PathType.DANGER_POWDER_SNOW, -1.0F);
        mob.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, 0.0F);
        mob.setPathfindingMalus(PathType.FENCE, -1.0F);
        mob.setPathfindingMalus(PathType.LAVA, -1.0F);
        mob.setPathfindingMalus(PathType.LEAVES, -1.0F);

        mob.onPathfindingStart();
    }
    public RecruitsPathNodeEvaluator() {
        super();
    }

    private int getEntityHeight(){
        return mob.isVehicle() ? 2 : 1;
    }
    public void setTarget(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    //prefer blocks that have empty neighbors
    //prefer walk on dirt path
    //prefer walk on stairs when goal pos is higher or lower current position >2 blocks
    //prefer walk on ways with no leaves
    protected @NotNull Node getNode(int x, int y, int z) {
        Node node = super.getNode(x, y, z);
        PathType blockpathtypes = this.getCachedBlockType(this.mob, x, y, z);
        float f = this.mob.getPathfindingMalus(blockpathtypes);

        if (f >= 0.0F) {
            node.type = blockpathtypes;
            node.costMalus = Math.max(node.costMalus, f);

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            BlockPos aboveEntityHeightPos = pos.above(getEntityHeight());

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                mutablePos.set(pos).move(direction);
                BlockState belowStateNeighbors = this.currentContext.getBlockState(mutablePos.below());
                
                if (!belowStateNeighbors.is(Blocks.DIRT_PATH)) {
                    node.costMalus += 2.0F;
                    continue;
                }

                BlockState aboveLeavesCheck = this.currentContext.getBlockState(aboveEntityHeightPos.relative(direction, 2));
                if (aboveLeavesCheck.is(BlockTags.LEAVES)) {
                    node.costMalus = -1.0F;
                    break;
                }
            }
        }


        return node;
    }

    protected static PathType getBlockPathTypeRaw(BlockGetter p_77644_, BlockPos p_77645_) {
        BlockState blockstate = p_77644_.getBlockState(p_77645_);
        PathType type = blockstate.getBlockPathType(p_77644_, p_77645_, null);
        if (type != null) return type;
        Block block = blockstate.getBlock();
        if (blockstate.isAir()) {
            return PathType.OPEN;
        } else if (!blockstate.is(BlockTags.TRAPDOORS) && !blockstate.is(Blocks.LILY_PAD) && !blockstate.is(Blocks.BIG_DRIPLEAF)) {
            if (blockstate.is(Blocks.POWDER_SNOW)) {
                return PathType.POWDER_SNOW;
            } else if (!blockstate.is(Blocks.CACTUS) && !blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
                if (blockstate.is(Blocks.HONEY_BLOCK)) {
                    return PathType.STICKY_HONEY;
                } else if (blockstate.is(Blocks.COCOA)) {
                    return PathType.COCOA;
                } else if (!blockstate.is(Blocks.WITHER_ROSE) && !blockstate.is(Blocks.POINTED_DRIPSTONE)) {
                    FluidState fluidstate = p_77644_.getFluidState(p_77645_);
                    PathType nonLoggableFluidPathType = fluidstate.getBlockPathType(p_77644_, p_77645_, null, false);
                    if (nonLoggableFluidPathType != null) return nonLoggableFluidPathType;
                    if (fluidstate.is(FluidTags.LAVA)) {
                        return PathType.LAVA;
                    } else if (isBurningBlock(blockstate)) {
                        return PathType.DAMAGE_FIRE;
                    } else if (block instanceof DoorBlock) {
                        DoorBlock doorblock = (DoorBlock)block;
                        if (blockstate.getValue(DoorBlock.OPEN)) {
                            return PathType.DOOR_OPEN;
                        } else {
                            return doorblock.type().canOpenByHand() ? PathType.DOOR_WOOD_CLOSED : PathType.DOOR_IRON_CLOSED;
                        }
                    } else if (block instanceof FenceGateBlock && !blockstate.getValue(DoorBlock.OPEN)) {
                        return PathType.DOOR_WOOD_CLOSED;
                    } else if (block instanceof FenceGateBlock && blockstate.getValue(DoorBlock.OPEN)) {
                        return PathType.DOOR_OPEN;
                    }else if (block instanceof BaseRailBlock) {
                        return PathType.RAIL;
                    } else if (block instanceof LeavesBlock) {
                        return PathType.LEAVES;
                    } else if (!blockstate.is(BlockTags.FENCES) && !blockstate.is(BlockTags.WALLS) && (!(block instanceof FenceGateBlock) || blockstate.getValue(FenceGateBlock.OPEN))) {
                        if (!blockstate.isPathfindable(PathComputationType.LAND)) {
                            return PathType.BLOCKED;
                        } else {
                            PathType loggableFluidPathType = fluidstate.getBlockPathType(p_77644_, p_77645_, null, true);
                            if (loggableFluidPathType != null) return loggableFluidPathType;
                            return fluidstate.is(FluidTags.WATER) ? PathType.WATER : PathType.OPEN;
                        }
                    } else {
                        return PathType.FENCE;
                    }
                } else {
                    return PathType.DAMAGE_CAUTIOUS;
                }
            } else {
                return PathType.DAMAGE_OTHER;
            }
        } else {
            return PathType.TRAPDOOR;
        }
    }


    /**
     ** From here downwards it's just a copy of the WalkNodeEvaluator
     **/
    public static final double SPACE_BETWEEN_WALL_POSTS = 0.5D;
    private static final double DEFAULT_MOB_JUMP_HEIGHT = 1.125D;
    private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();
    private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();

        public void done() {
            this.mob.onPathfindingDone();
            this.pathTypesByPosCache.clear();
            this.collisionCache.clear();
            super.done();
        }

        public Node getStart() {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            int i = this.mob.getBlockY();
            BlockState blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), (double)i, this.mob.getZ()));
            if (!this.mob.canStandOnFluid(blockstate.getFluidState())) {
                if (this.canFloat() && this.mob.isInWater()) {
                    while(true) {
                        if (!blockstate.is(Blocks.WATER) && blockstate.getFluidState() != Fluids.WATER.getSource(false)) {
                            --i;
                            break;
                        }

                        ++i;
                        blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), (double)i, this.mob.getZ()));
                    }
                } else if (this.mob.onGround()) {
                    i = Mth.floor(this.mob.getY() + 0.5D);
                } else {
                    BlockPos blockpos;
                    for(blockpos = this.mob.blockPosition(); (this.currentContext.getBlockState(blockpos).isAir() || this.currentContext.getBlockState(blockpos).isPathfindable(PathComputationType.LAND)) && blockpos.getY() > this.mob.level().getMinBuildHeight(); blockpos = blockpos.below()) {
                    }

                    i = blockpos.above().getY();
                }
            } else {
                while(this.mob.canStandOnFluid(blockstate.getFluidState())) {
                    ++i;
                    blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), (double)i, this.mob.getZ()));
                }

                --i;
            }

            BlockPos blockpos1 = this.mob.blockPosition();
            if (!this.canStartAt(blockpos$mutableblockpos.set(blockpos1.getX(), i, blockpos1.getZ()))) {
                AABB aabb = this.mob.getBoundingBox();
                if (this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, (double)i, aabb.minZ)) || this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, (double)i, aabb.maxZ)) || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, (double)i, aabb.minZ)) || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, (double)i, aabb.maxZ))) {
                    return this.getStartNode(blockpos$mutableblockpos);
                }
            }

            return this.getStartNode(new BlockPos(blockpos1.getX(), i, blockpos1.getZ()));
        }

        protected Node getStartNode(BlockPos p_230632_) {
            Node node = this.getNode(p_230632_);
            node.type = this.getBlockPathType(this.mob, node.asBlockPos());
            node.costMalus = this.mob.getPathfindingMalus(node.type);
            return node;
        }

        protected boolean canStartAt(BlockPos p_262596_) {
            PathType blockpathtypes = this.getBlockPathType(this.mob, p_262596_);
            return blockpathtypes != PathType.OPEN && this.mob.getPathfindingMalus(blockpathtypes) >= 0.0F;
        }

        public Target getTarget(double p_77550_, double p_77551_, double p_77552_) {
            return this.getTargetNodeAt(p_77550_, p_77551_, p_77552_);
        }

        public int getNeighbors(Node[] p_77640_, Node p_77641_) {
            int i = 0;
            int j = 0;
            PathType blockpathtypes = this.getCachedBlockType(this.mob, p_77641_.x, p_77641_.y + 1, p_77641_.z);
            PathType blockpathtypes1 = this.getCachedBlockType(this.mob, p_77641_.x, p_77641_.y, p_77641_.z);
            if (this.mob.getPathfindingMalus(blockpathtypes) >= 0.0F && blockpathtypes1 != PathType.STICKY_HONEY) {
                j = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
            }

            double d0 = this.getFloorLevel(new BlockPos(p_77641_.x, p_77641_.y, p_77641_.z));
            Node node = this.findAcceptedNode(p_77641_.x, p_77641_.y, p_77641_.z + 1, j, d0, Direction.SOUTH, blockpathtypes1);
            if (this.isNeighborValid(node, p_77641_)) {
                p_77640_[i++] = node;
            }

            Node node1 = this.findAcceptedNode(p_77641_.x - 1, p_77641_.y, p_77641_.z, j, d0, Direction.WEST, blockpathtypes1);
            if (this.isNeighborValid(node1, p_77641_)) {
                p_77640_[i++] = node1;
            }

            Node node2 = this.findAcceptedNode(p_77641_.x + 1, p_77641_.y, p_77641_.z, j, d0, Direction.EAST, blockpathtypes1);
            if (this.isNeighborValid(node2, p_77641_)) {
                p_77640_[i++] = node2;
            }

            Node node3 = this.findAcceptedNode(p_77641_.x, p_77641_.y, p_77641_.z - 1, j, d0, Direction.NORTH, blockpathtypes1);
            if (this.isNeighborValid(node3, p_77641_)) {
                p_77640_[i++] = node3;
            }

            Node node4 = this.findAcceptedNode(p_77641_.x - 1, p_77641_.y, p_77641_.z - 1, j, d0, Direction.NORTH, blockpathtypes1);
            if (this.isDiagonalValid(p_77641_, node1, node3, node4)) {
                p_77640_[i++] = node4;
            }

            Node node5 = this.findAcceptedNode(p_77641_.x + 1, p_77641_.y, p_77641_.z - 1, j, d0, Direction.NORTH, blockpathtypes1);
            if (this.isDiagonalValid(p_77641_, node2, node3, node5)) {
                p_77640_[i++] = node5;
            }

            Node node6 = this.findAcceptedNode(p_77641_.x - 1, p_77641_.y, p_77641_.z + 1, j, d0, Direction.SOUTH, blockpathtypes1);
            if (this.isDiagonalValid(p_77641_, node1, node, node6)) {
                p_77640_[i++] = node6;
            }

            Node node7 = this.findAcceptedNode(p_77641_.x + 1, p_77641_.y, p_77641_.z + 1, j, d0, Direction.SOUTH, blockpathtypes1);
            if (this.isDiagonalValid(p_77641_, node2, node, node7)) {
                p_77640_[i++] = node7;
            }

            return i;
        }

        protected boolean isNeighborValid(@Nullable Node p_77627_, Node p_77628_) {
            return p_77627_ != null && !p_77627_.closed && (p_77627_.costMalus >= 0.0F || p_77628_.costMalus < 0.0F);
        }

        protected boolean isDiagonalValid(Node p_77630_, @Nullable Node p_77631_, @Nullable Node p_77632_, @Nullable Node p_77633_) {
            if (p_77633_ != null && p_77632_ != null && p_77631_ != null) {
                if (p_77633_.closed) {
                    return false;
                } else if (p_77632_.y <= p_77630_.y && p_77631_.y <= p_77630_.y) {
                    if (p_77631_.type != PathType.WALKABLE_DOOR && p_77632_.type != PathType.WALKABLE_DOOR && p_77633_.type != PathType.WALKABLE_DOOR) {
                        boolean flag = p_77632_.type == PathType.FENCE && p_77631_.type == PathType.FENCE && (double)this.mob.getBbWidth() < 0.5D;
                        return p_77633_.costMalus >= 0.0F && (p_77632_.y < p_77630_.y || p_77632_.costMalus >= 0.0F || flag) && (p_77631_.y < p_77630_.y || p_77631_.costMalus >= 0.0F || flag);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        private static boolean doesBlockHavePartialCollision(PathType p_230626_) {
            return p_230626_ == PathType.FENCE || p_230626_ == PathType.DOOR_WOOD_CLOSED || p_230626_ == PathType.DOOR_IRON_CLOSED;
        }

        private boolean canReachWithoutCollision(Node p_77625_) {
            AABB aabb = this.mob.getBoundingBox();
            Vec3 vec3 = new Vec3((double)p_77625_.x - this.mob.getX() + aabb.getXsize() / 2.0D, (double)p_77625_.y - this.mob.getY() + aabb.getYsize() / 2.0D, (double)p_77625_.z - this.mob.getZ() + aabb.getZsize() / 2.0D);
            int i = Mth.ceil(vec3.length() / aabb.getSize());
            vec3 = vec3.scale((double)(1.0F / (float)i));

            for(int j = 1; j <= i; ++j) {
                aabb = aabb.move(vec3);
                if (this.hasCollisions(aabb)) {
                    return false;
                }
            }

            return true;
        }

        protected double getFloorLevel(BlockPos p_164733_) {
            return (this.canFloat() || this.isAmphibious()) && this.currentContext.level().getFluidState(p_164733_).is(FluidTags.WATER) ? (double)p_164733_.getY() + 0.5D : getFloorLevel(this.currentContext.level(), p_164733_);
        }

        public static double getFloorLevel(BlockGetter p_77612_, BlockPos p_77613_) {
            BlockPos blockpos = p_77613_.below();
            VoxelShape voxelshape = p_77612_.getBlockState(blockpos).getCollisionShape(p_77612_, blockpos);
            return (double)blockpos.getY() + (voxelshape.isEmpty() ? 0.0D : voxelshape.max(Direction.Axis.Y));
        }

        protected boolean isAmphibious() {
            return false;
        }

        @Nullable
        protected Node findAcceptedNode(int p_164726_, int p_164727_, int p_164728_, int p_164729_, double p_164730_, Direction p_164731_, PathType p_164732_) {
            Node node = null;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            double d0 = this.getFloorLevel(blockpos$mutableblockpos.set(p_164726_, p_164727_, p_164728_));
            if (d0 - p_164730_ > this.getMobJumpHeight()) {
                return null;
            } else {
                PathType blockpathtypes = this.getCachedBlockType(this.mob, p_164726_, p_164727_, p_164728_);
                float f = this.mob.getPathfindingMalus(blockpathtypes);
                double d1 = (double)this.mob.getBbWidth() / 2.0D;
                if (f >= 0.0F) {
                    node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, blockpathtypes, f);
                }

                if (doesBlockHavePartialCollision(p_164732_) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                    node = null;
                }

                if (blockpathtypes != PathType.WALKABLE && (!this.isAmphibious() || blockpathtypes != PathType.WATER)) {
                    if ((node == null || node.costMalus < 0.0F) && p_164729_ > 0 && (blockpathtypes != PathType.FENCE || this.canWalkOverFences()) && blockpathtypes != PathType.UNPASSABLE_RAIL && blockpathtypes != PathType.TRAPDOOR && blockpathtypes != PathType.POWDER_SNOW) {
                        node = this.findAcceptedNode(p_164726_, p_164727_ + 1, p_164728_, p_164729_ - 1, p_164730_, p_164731_, p_164732_);
                        if (node != null && (node.type == PathType.OPEN || node.type == PathType.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
                            double d2 = (double)(p_164726_ - p_164731_.getStepX()) + 0.5D;
                            double d3 = (double)(p_164728_ - p_164731_.getStepZ()) + 0.5D;
                            AABB aabb = new AABB(d2 - d1, this.getFloorLevel(blockpos$mutableblockpos.set(d2, (double)(p_164727_ + 1), d3)) + 0.001D, d3 - d1, d2 + d1, (double)this.mob.getBbHeight() + this.getFloorLevel(blockpos$mutableblockpos.set((double)node.x, (double)node.y, (double)node.z)) - 0.002D, d3 + d1);
                            if (this.hasCollisions(aabb)) {
                                node = null;
                            }
                        }
                    }

                    if (!this.isAmphibious() && blockpathtypes == PathType.WATER && !this.canFloat()) {
                        if (this.getCachedBlockType(this.mob, p_164726_, p_164727_ - 1, p_164728_) != PathType.WATER) {
                            return node;
                        }

                        while(p_164727_ > this.mob.level().getMinBuildHeight()) {
                            --p_164727_;
                            blockpathtypes = this.getCachedBlockType(this.mob, p_164726_, p_164727_, p_164728_);
                            if (blockpathtypes != PathType.WATER) {
                                return node;
                            }

                            node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, blockpathtypes, this.mob.getPathfindingMalus(blockpathtypes));
                        }
                    }

                    if (blockpathtypes == PathType.OPEN) {
                        int j = 0;
                        int i = p_164727_;

                        while(blockpathtypes == PathType.OPEN) {
                            --p_164727_;
                            if (p_164727_ < this.mob.level().getMinBuildHeight()) {
                                return this.getBlockedNode(p_164726_, i, p_164728_);
                            }

                            if (j++ >= this.mob.getMaxFallDistance()) {
                                return this.getBlockedNode(p_164726_, p_164727_, p_164728_);
                            }

                            blockpathtypes = this.getCachedBlockType(this.mob, p_164726_, p_164727_, p_164728_);
                            f = this.mob.getPathfindingMalus(blockpathtypes);
                            if (blockpathtypes != PathType.OPEN && f >= 0.0F) {
                                node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, blockpathtypes, f);
                                break;
                            }

                            if (f < 0.0F) {
                                return this.getBlockedNode(p_164726_, p_164727_, p_164728_);
                            }
                        }
                    }

                    if (doesBlockHavePartialCollision(blockpathtypes) && node == null) {
                        node = this.getNode(p_164726_, p_164727_, p_164728_);
                        node.closed = true;
                        node.type = blockpathtypes;
                        node.costMalus = blockpathtypes.getMalus();
                    }

                    return node;
                } else {
                    return node;
                }
            }
        }

        private double getMobJumpHeight() {
            return Math.max(1.125D, (double)this.mob.maxUpStep());
        }

        private Node getNodeAndUpdateCostToMax(int p_230620_, int p_230621_, int p_230622_, PathType p_230623_, float p_230624_) {
            Node node = this.getNode(p_230620_, p_230621_, p_230622_);
            node.type = p_230623_;
            node.costMalus = Math.max(node.costMalus, p_230624_);
            return node;
        }

        private Node getBlockedNode(int p_230628_, int p_230629_, int p_230630_) {
            Node node = this.getNode(p_230628_, p_230629_, p_230630_);
            node.type = PathType.BLOCKED;
            node.costMalus = -1.0F;
            return node;
        }

        private boolean hasCollisions(AABB p_77635_) {
            return this.collisionCache.computeIfAbsent(p_77635_, (p_192973_) -> {
                return !this.currentContext.level().noCollision(this.mob, p_77635_);
            });
        }

        @Override
        public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
            return this.getBlockPathType(context.level(), x, y, z, mob);
        }

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            return this.getBlockPathType(context.level(), x, y, z);
        }

        public PathType getBlockPathType(BlockGetter p_265141_, int p_265661_, int p_265757_, int p_265716_, Mob p_265398_) {
            EnumSet<PathType> enumset = EnumSet.noneOf(PathType.class);
            PathType blockpathtypes = PathType.BLOCKED;
            blockpathtypes = this.getPathType(p_265141_, p_265661_, p_265757_, p_265716_, enumset, blockpathtypes, p_265398_.blockPosition());
            if (enumset.contains(PathType.FENCE)) {
                return PathType.FENCE;
            } else if (enumset.contains(PathType.UNPASSABLE_RAIL)) {
                return PathType.UNPASSABLE_RAIL;
            } else {
                PathType blockpathtypes1 = PathType.BLOCKED;

                for(PathType blockpathtypes2 : enumset) {
                    if (p_265398_.getPathfindingMalus(blockpathtypes2) < 0.0F) {
                        return blockpathtypes2;
                    }

                    if (p_265398_.getPathfindingMalus(blockpathtypes2) >= p_265398_.getPathfindingMalus(blockpathtypes1)) {
                        blockpathtypes1 = blockpathtypes2;
                    }
                }

                return blockpathtypes == PathType.OPEN && p_265398_.getPathfindingMalus(blockpathtypes1) == 0.0F && this.entityWidth <= 1 ? PathType.OPEN : blockpathtypes1;
            }
        }

        public PathType getPathType(BlockGetter p_265227_, int p_265066_, int p_265537_, int p_265771_, EnumSet<PathType> p_265263_, PathType p_265458_, BlockPos p_265515_) {
            for(int i = 0; i < this.entityWidth; ++i) {
                for(int j = 0; j < this.entityHeight; ++j) {
                    for(int k = 0; k < this.entityDepth; ++k) {
                        int l = i + p_265066_;
                        int i1 = j + p_265537_;
                        int j1 = k + p_265771_;
                        PathType blockpathtypes = this.getBlockPathType(p_265227_, l, i1, j1);
                        blockpathtypes = this.evaluateBlockPathType(p_265227_, p_265515_, blockpathtypes);
                        if (i == 0 && j == 0 && k == 0) {
                            p_265458_ = blockpathtypes;
                        }

                        p_265263_.add(blockpathtypes);
                    }
                }
            }

            return p_265458_;
        }

        protected PathType evaluateBlockPathType(BlockGetter p_265305_, BlockPos p_265350_, PathType p_265551_) {
            boolean flag = this.canPassDoors();
            if (p_265551_ == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && flag) {
                p_265551_ = PathType.WALKABLE_DOOR;
            }

            if (p_265551_ == PathType.DOOR_OPEN && !flag) {
                p_265551_ = PathType.BLOCKED;
            }

            if (p_265551_ == PathType.RAIL && !(p_265305_.getBlockState(p_265350_).getBlock() instanceof BaseRailBlock) && !(p_265305_.getBlockState(p_265350_.below()).getBlock() instanceof BaseRailBlock)) {
                p_265551_ = PathType.UNPASSABLE_RAIL;
            }

            return p_265551_;
        }

        protected PathType getBlockPathType(Mob p_77573_, BlockPos p_77574_) {
            return this.getCachedBlockType(p_77573_, p_77574_.getX(), p_77574_.getY(), p_77574_.getZ());
        }

        protected PathType getCachedBlockType(Mob p_77568_, int p_77569_, int p_77570_, int p_77571_) {
            return this.pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(p_77569_, p_77570_, p_77571_), (p_265015_) -> {
                return this.getPathTypeOfMob(this.currentContext, p_77569_, p_77570_, p_77571_, p_77568_);
            });
        }

        public PathType getBlockPathType(BlockGetter p_77576_, int p_77577_, int p_77578_, int p_77579_) {
            return getBlockPathTypeStatic(p_77576_, new BlockPos.MutableBlockPos(p_77577_, p_77578_, p_77579_));
        }

        public static PathType getBlockPathTypeStatic(BlockGetter p_77605_, BlockPos.MutableBlockPos p_77606_) {
            int i = p_77606_.getX();
            int j = p_77606_.getY();
            int k = p_77606_.getZ();
            PathType blockpathtypes = getBlockPathTypeRaw(p_77605_, p_77606_);
            if (blockpathtypes == PathType.OPEN && j >= p_77605_.getMinBuildHeight() + 1) {
                PathType blockpathtypes1 = getBlockPathTypeRaw(p_77605_, p_77606_.set(i, j - 1, k));
                blockpathtypes = blockpathtypes1 != PathType.WALKABLE && blockpathtypes1 != PathType.OPEN && blockpathtypes1 != PathType.WATER && blockpathtypes1 != PathType.LAVA ? PathType.WALKABLE : PathType.OPEN;
                if (blockpathtypes1 == PathType.DAMAGE_FIRE) {
                    blockpathtypes = PathType.DAMAGE_FIRE;
                }

                if (blockpathtypes1 == PathType.DAMAGE_OTHER) {
                    blockpathtypes = PathType.DAMAGE_OTHER;
                }

                if (blockpathtypes1 == PathType.STICKY_HONEY) {
                    blockpathtypes = PathType.STICKY_HONEY;
                }

                if (blockpathtypes1 == PathType.POWDER_SNOW) {
                    blockpathtypes = PathType.DANGER_POWDER_SNOW;
                }

                if (blockpathtypes1 == PathType.DAMAGE_CAUTIOUS) {
                    blockpathtypes = PathType.DAMAGE_CAUTIOUS;
                }
            }

            if (blockpathtypes == PathType.WALKABLE) {
                blockpathtypes = checkNeighbourBlocks(p_77605_, p_77606_.set(i, j, k), blockpathtypes);
            }

            return blockpathtypes;
        }

        public static PathType checkNeighbourBlocks(BlockGetter p_77608_, BlockPos.MutableBlockPos p_77609_, PathType p_77610_) {
            int i = p_77609_.getX();
            int j = p_77609_.getY();
            int k = p_77609_.getZ();

            for(int l = -1; l <= 1; ++l) {
                for(int i1 = -1; i1 <= 1; ++i1) {
                    for(int j1 = -1; j1 <= 1; ++j1) {
                        if (l != 0 || j1 != 0) {
                            p_77609_.set(i + l, j + i1, k + j1);
                            BlockState blockstate = p_77608_.getBlockState(p_77609_);
                            PathType blockPathType = blockstate.getAdjacentBlockPathType(p_77608_, p_77609_, null, p_77610_);
                            if (blockPathType != null) return blockPathType;
                            FluidState fluidState = blockstate.getFluidState();
                            PathType fluidPathType = fluidState.getAdjacentBlockPathType(p_77608_, p_77609_, null, p_77610_);
                            if (fluidPathType != null) return fluidPathType;
                            if (blockstate.is(Blocks.CACTUS) || blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
                                return PathType.DANGER_OTHER;
                            }

                            if (isBurningBlock(blockstate)) {
                                return PathType.DANGER_FIRE;
                            }

                            if (p_77608_.getFluidState(p_77609_).is(FluidTags.WATER)) {
                                return PathType.WATER_BORDER;
                            }

                            if (blockstate.is(Blocks.WITHER_ROSE) || blockstate.is(Blocks.POINTED_DRIPSTONE)) {
                                return PathType.DAMAGE_CAUTIOUS;
                            }
                        }
                    }
                }
            }

            return p_77610_;
        }
        public static boolean isBurningBlock(BlockState p_77623_) {
            return p_77623_.is(BlockTags.FIRE) || p_77623_.is(Blocks.LAVA) || p_77623_.is(Blocks.MAGMA_BLOCK) || CampfireBlock.isLitCampfire(p_77623_) || p_77623_.is(Blocks.LAVA_CAULDRON);
        }
    }