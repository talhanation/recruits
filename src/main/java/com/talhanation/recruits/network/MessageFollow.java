package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageFollow implements Message<MessageFollow> {

    private UUID player;
    private UUID recruit;
    private int state;
    private int group;
    private boolean fromGui;

    public MessageFollow(){
    }

    public MessageFollow(UUID player, int state, int group) {
        this.player = player;
        this.state  = state;
        this.group  = group;
        this.fromGui = false;
        this.recruit = null;
    }

    public MessageFollow(UUID player, UUID recruit, int state, boolean fromGui) {
        this.player = player;
        this.recruit = recruit;
        this.state  = state;
        this.fromGui = fromGui;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayerEntity player = context.getSender();

        if (!fromGui) {
            List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(40.0D)));
            for (AbstractRecruitEntity recruits : list) {
                CommandEvents.onRKeyPressed(this.player, recruits, this.state, this.group, false);
            }
        }
        else{
            List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(16.0D)));
            for (AbstractRecruitEntity recruits : list) {
                if (recruits.getUUID().equals(this.recruit)){
                    CommandEvents.onRKeyPressed(this.player, recruits, this.state,  0, true);
                }
            }

            /*
            player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                    .inflate(16.0D), v -> v
                    .getUUID()
                    .equals(this.recruit))
                    .stream()
                    .filter(AbstractRecruitEntity::isAlive)
                    .findAny()
                    .ifPresent(recruit -> CommandEvents.onRKeyPressed(this.player, recruit, this.state, 0, true));

            */
        }

    }
    public MessageFollow fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readInt();
        this.fromGui = buf.readBoolean();
        if (this.recruit != null) this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.group);
        buf.writeInt(this.state);
        buf.writeBoolean(this.fromGui);
        if (this.recruit != null) buf.writeUUID(this.recruit);
    }

}