package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
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
    private int steps;

    public MessageFollow(){
    }

    public MessageFollow(UUID player, int state, int group, boolean fromGui) {
        this.player = player;
        this.state  = state;
        this.group  = group;
        this.fromGui = fromGui;
    }

    public MessageFollow(UUID player, UUID recruit, int state, int steps, boolean fromGui) {
        this.player = player;
        this.recruit = recruit;
        this.state  = state;
        this.fromGui = fromGui;
        this.steps = steps;
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
            player.sendMessage(new StringTextComponent("MESSAGE! STATE: " + this.state), player.getUUID());

            List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(8.0D)));
            for (AbstractRecruitEntity recruits : list) {
                CommandEvents.onRKeyPressed(this.player, recruits, this.state,  0, true);
                    player.sendMessage(new StringTextComponent("MESSAGE 2222222! STATE: " + this.state), player.getUUID());
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
        this.steps = buf.readInt();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
        buf.writeInt(this.group);
        buf.writeBoolean(this.fromGui);
        buf.writeInt(this.steps);
    }

}