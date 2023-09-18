package com.talhanation.recruits.entities.ai.navigation;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class RecruitsHorseWalkNodeEvaluator extends WalkNodeEvaluator {
    //Horse side
    private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();

    public void done() {
        this.collisionCache.clear();
        super.done();
    }

    @Nullable
    protected Node findAcceptedNode(int p_164726_, int p_164727_, int p_164728_, int p_164729_, double p_164730_, Direction p_164731_, BlockPathTypes p_164732_) {
        Node node = null;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        double d0 = this.getFloorLevel(blockpos$mutableblockpos.set(p_164726_, p_164727_, p_164728_));
        if (d0 - p_164730_ > 1.125D) {
            return null;
        } else {
            BlockPathTypes blockpathtypes = this.getCachedBlockType(this.mob, p_164726_, p_164727_, p_164728_);
            float f = this.mob.getPathfindingMalus(blockpathtypes);
            double d1 = (double)this.mob.getBbWidth() / 2.0D;
            if (f >= 0.0F) {
                node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, blockpathtypes, f);
            }

            if (doesBlockHavePartialCollision(p_164732_) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }

            if (blockpathtypes != BlockPathTypes.WALKABLE && (!this.isAmphibious() || blockpathtypes != BlockPathTypes.WATER)) {
                if ((node == null || node.costMalus < 0.0F) && p_164729_ > 0 && blockpathtypes != BlockPathTypes.FENCE && blockpathtypes != BlockPathTypes.UNPASSABLE_RAIL && blockpathtypes != BlockPathTypes.TRAPDOOR && blockpathtypes != BlockPathTypes.POWDER_SNOW) {
                    node = this.findAcceptedNode(p_164726_, p_164727_ + 1, p_164728_, p_164729_ - 1, p_164730_, p_164731_, p_164732_);
                    if (node != null && (node.type == BlockPathTypes.OPEN || node.type == BlockPathTypes.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
                        double d2 = (double)(p_164726_ - p_164731_.getStepX()) + 0.5D;
                        double d3 = (double)(p_164728_ - p_164731_.getStepZ()) + 0.5D;

                        double mobHeight = this.mob.getBbHeight();
                        double passengerHeight = this.mob.getControllingPassenger() != null ? this.mob.getControllingPassenger().getBbHeight() : 0D;
                        double height = mobHeight + passengerHeight;

                        AABB aabb = new AABB(d2 - d1, getFloorLevel(this.level, blockpos$mutableblockpos.set(d2, (double)(p_164727_ + 1), d3)) + 0.001D, d3 - d1, d2 + d1, height + getFloorLevel(this.level, blockpos$mutableblockpos.set((double)node.x, (double)node.y, (double)node.z)) - 0.002D, d3 + d1);
                        if (this.hasCollisions(aabb)) {
                            node = null;
                        }
                    }
                }

                if (!this.isAmphibious() && blockpathtypes == BlockPathTypes.WATER && !this.canFloat()) {
                    if (this.getCachedBlockType(this.mob, p_164726_, p_164727_ - 1, p_164728_) != BlockPathTypes.WATER) {
                        return node;
                    }

                    while(p_164727_ > this.mob.level.getMinBuildHeight()) {
                        --p_164727_;
                        blockpathtypes = this.getCachedBlockType(this.mob, p_164726_, p_164727_, p_164728_);
                        if (blockpathtypes != BlockPathTypes.WATER) {
                            return node;
                        }

                        node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, blockpathtypes, this.mob.getPathfindingMalus(blockpathtypes));
                    }
                }

                if (blockpathtypes == BlockPathTypes.OPEN) {
                    int j = 0;
                    int i = p_164727_;

                    while(blockpathtypes == BlockPathTypes.OPEN) {
                        --p_164727_;
                        if (p_164727_ < this.mob.level.getMinBuildHeight()) {
                            return this.getBlockedNode(p_164726_, i, p_164728_);
                        }

                        if (j++ >= this.mob.getMaxFallDistance()) {
                            return this.getBlockedNode(p_164726_, p_164727_, p_164728_);
                        }

                        blockpathtypes = this.getCachedBlockType(this.mob, p_164726_, p_164727_, p_164728_);
                        f = this.mob.getPathfindingMalus(blockpathtypes);
                        if (blockpathtypes != BlockPathTypes.OPEN && f >= 0.0F) {
                            node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, blockpathtypes, f);
                            break;
                        }

                        if (f < 0.0F) {
                            return this.getBlockedNode(p_164726_, p_164727_, p_164728_);
                        }
                    }
                }

                if (doesBlockHavePartialCollision(blockpathtypes)) {
                    node = this.getNode(p_164726_, p_164727_, p_164728_);
                    if (node != null) {
                        node.closed = true;
                        node.type = blockpathtypes;
                        node.costMalus = blockpathtypes.getMalus();
                    }
                }

                return node;
            } else {
                return node;
            }
        }
    }

    public boolean hasCollisions(AABB p_77635_) {
        return this.collisionCache.computeIfAbsent(p_77635_, (p_192973_) -> {
            return !this.level.noCollision(this.mob, p_77635_);
        });
    }

    private static boolean doesBlockHavePartialCollision(BlockPathTypes p_230626_) {
        return p_230626_ == BlockPathTypes.FENCE || p_230626_ == BlockPathTypes.DOOR_WOOD_CLOSED || p_230626_ == BlockPathTypes.DOOR_IRON_CLOSED;
    }

    @Nullable
    private Node getBlockedNode(int p_230628_, int p_230629_, int p_230630_) {
        Node node = this.getNode(p_230628_, p_230629_, p_230630_);
        if (node != null) {
            node.type = BlockPathTypes.BLOCKED;
            node.costMalus = -1.0F;
        }

        return node;
    }

    @Nullable
    private Node getNodeAndUpdateCostToMax(int p_230620_, int p_230621_, int p_230622_, BlockPathTypes p_230623_, float p_230624_) {
        Node node = this.getNode(p_230620_, p_230621_, p_230622_);
        if (node != null) {
            node.type = p_230623_;
            node.costMalus = Math.max(node.costMalus, p_230624_);
        }

        return node;
    }

    public boolean canReachWithoutCollision(Node p_77625_) {
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
}
