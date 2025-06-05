package com.talhanation.recruits.entities;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractChunkLoaderEntity extends AbstractRecruitEntity {
    private Optional<RecruitsChunk> loadedChunk = Optional.empty();

    public AbstractChunkLoaderEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        if(shouldLoadChunk()) updateChunkLoading();
    }

    public boolean shouldLoadChunk(){
        return true;
    }

    public void updateChunkLoading(){
        if (!this.getCommandSenderWorld().isClientSide() && RecruitsServerConfig.RecruitsChunkLoading.get()) {
            RecruitsChunk currentChunk = new RecruitsChunk(this.chunkPosition().x, this.chunkPosition().z);
            if (loadedChunk.isEmpty()) {
                this.setForceChunk(currentChunk, true);
                loadedChunk = Optional.of(currentChunk);

            } else if (!currentChunk.equals(loadedChunk.get())){

                Set<RecruitsChunk> toForce = getSetOfChunks(currentChunk);
                Set<RecruitsChunk> toUnForce = getSetOfChunks(loadedChunk.get());
                toUnForce.removeAll(toForce);

                //verbliebene chunks
                Set<RecruitsChunk> forced = getSetOfChunks(loadedChunk.get());
                toForce.removeAll(forced);


                toUnForce.forEach(chunk -> this.setForceChunk(chunk, false));
                toForce.forEach(chunk -> this.setForceChunk(chunk, true));
                loadedChunk = Optional.of(currentChunk);
            }
        }
    }

    ////////////////////////////////////DATA////////////////////////////////////

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if(loadedChunk.isPresent()) {
            nbt.putInt("chunkX", loadedChunk.get().x);
            nbt.putInt("chunkZ", loadedChunk.get().z);
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("chunkX")) {
            int x = nbt.getInt("chunkX");
            int z = nbt.getInt("chunkZ");
            loadedChunk = Optional.of(new RecruitsChunk(x, z));
        }
    }

    ////////////////////////////////////GET////////////////////////////////////

    private Set<RecruitsChunk> getSetOfChunks(RecruitsChunk chunk){
        Set<RecruitsChunk> set = new HashSet<>();

        for(int i = -1; i <= 1; i++){
            for (int k = -1; k <= 1; k++){
                set.add(new RecruitsChunk(chunk.x + i, chunk.z + k));
            }
        }
        return set;
    }

    ////////////////////////////////////SET////////////////////////////////////

    private void setForceChunk(RecruitsChunk chunk, boolean add) {
        ForgeChunkManager.forceChunk((ServerLevel) this.getCommandSenderWorld(), Main.MOD_ID, this, chunk.x, chunk.z, add, false);
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
        if(!this.getCommandSenderWorld().isClientSide) loadedChunk.ifPresent(chunk -> this.getSetOfChunks(chunk).forEach(chunk1 -> this.setForceChunk(chunk1, false)));
    }

    public static class RecruitsChunk {
        int x;
        int z;

        public RecruitsChunk(ChunkPos chunkPosition){
            this(chunkPosition.x, chunkPosition.z);
        }

        public RecruitsChunk(int x, int z){
            this.x = x;
            this.z = z;
        }

        public boolean isSame(RecruitsChunk otherChunk){
            if(otherChunk != null) return this.x == otherChunk.x && this.z == otherChunk.z;
            else return false;
        }

        public RecruitsChunk getNextChunk(Direction direction) {
            RecruitsChunk recruitsChunk = null;
            switch (direction){
                case NORTH -> recruitsChunk = new RecruitsChunk(this.x, this.z - 1);
                case EAST -> recruitsChunk = new RecruitsChunk(this.x + 1, this.z);
                case SOUTH -> recruitsChunk = new RecruitsChunk(this.x, this.z + 1);
                case WEST -> recruitsChunk = new RecruitsChunk(this.x - 1, this.z);

                default -> {}
            }

            return recruitsChunk;
        }

        public static List<RecruitsChunk> getSurroundingChunks(@NotNull AbstractChunkLoaderEntity.RecruitsChunk currentChunk){
            List<RecruitsChunk> list = new ArrayList<>();

            for(int i = -1; i <= 1; i++){
                for(int k = -1; k <= 1; k++){
                    RecruitsChunk newChunk = new RecruitsChunk(currentChunk.x + i, currentChunk.z + k);
                    list.add(newChunk);
                }
            }
            return list;
        }

        public static List<RecruitsChunk> getChunksToUnload(List<RecruitsChunk> currentChunks, List<RecruitsChunk> prevChunks) {
            List<RecruitsChunk> list = new ArrayList<>();

            for(RecruitsChunk prevChunk : prevChunks){
                for(RecruitsChunk currentChunk : currentChunks) {
                    if(!currentChunk.isSame(prevChunk)) list.add(prevChunk);
                }
            }
            return list;
        }
    }
}