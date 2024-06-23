package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class MessageRemoveAssignedGroupFromCompanion implements Message<MessageRemoveAssignedGroupFromCompanion> {

    private UUID owner;
    private UUID companion;

    public MessageRemoveAssignedGroupFromCompanion(){
    }

    public MessageRemoveAssignedGroupFromCompanion(UUID owner, UUID companion) {
        this.owner = owner;
        this.companion = companion;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        List<AbstractLeaderEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractLeaderEntity.class, context.getSender().getBoundingBox().inflate(100D));
        for (AbstractLeaderEntity companionEntity : list){
            if(companionEntity.getUUID().equals(this.companion)){
                companionEntity.setRecruitsToHoldPos();
                companionEntity.setRecruitsToListen();

                companionEntity.RECRUITS_IN_COMMAND = new Stack<>();
                companionEntity.currentRecruitsInCommand = new ArrayList<>();
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(context::getSender), new MessageToClientUpdateLeaderScreen(companionEntity.WAYPOINTS, companionEntity.WAYPOINT_ITEMS, companionEntity.getRecruitsInCommand().size()));
                break;
            }
        }
    }
    public MessageRemoveAssignedGroupFromCompanion fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.companion = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.companion);
    }

}