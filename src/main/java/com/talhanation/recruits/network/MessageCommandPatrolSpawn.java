package com.talhanation.recruits.network;

import com.talhanation.recruits.commands.PatrolSpawnCommand;
import com.talhanation.recruits.world.PillagerPatrolSpawn;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageCommandPatrolSpawn implements Message<MessageCommandPatrolSpawn> {
    private int type;

    public MessageCommandPatrolSpawn(){
    }

    public MessageCommandPatrolSpawn(int type) {
        this.type = type;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        BlockPos pos = context.getSender().getOnPos();
        switch (type){
            case 0 -> PillagerPatrolSpawn.spawnSmallPillagerPatrol(pos, pos, context.getSender().serverLevel());
            case 1 -> PillagerPatrolSpawn.spawnPillagerPatrol(pos, pos, context.getSender().serverLevel());
            case 2 -> PillagerPatrolSpawn.spawnMediumPillagerPatrol(pos, pos, context.getSender().serverLevel());
            case 3 -> PillagerPatrolSpawn.spawnLargePillagerPatrol(pos, pos, context.getSender().serverLevel());
            case 10 -> RecruitsPatrolSpawn.spawnCaravan(pos, context.getSender().serverLevel());
            case 11 -> RecruitsPatrolSpawn.spawnTinyPatrol(pos, context.getSender().serverLevel());
            case 12 -> RecruitsPatrolSpawn.spawnSmallPatrol(pos, context.getSender().serverLevel());
            case 13 -> RecruitsPatrolSpawn.spawnMediumPatrol(pos, context.getSender().serverLevel());
            case 14 -> RecruitsPatrolSpawn.spawnLargePatrol(pos, context.getSender().serverLevel());
            case 15 -> RecruitsPatrolSpawn.spawnHugePatrol(pos, context.getSender().serverLevel());
            default -> {
                return;
            }
        }

    }
    public MessageCommandPatrolSpawn fromBytes(FriendlyByteBuf buf) {
        this.type = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(type);
    }

}

