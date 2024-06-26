package com.talhanation.recruits.network;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public class MessageMountEntityGui implements Message<MessageMountEntityGui> {
    private UUID recruit;
    private boolean back;
    public MessageMountEntityGui() {
    }

    public MessageMountEntityGui(UUID recruit, boolean back) {
        this.recruit = recruit;
        this.back = back;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @SuppressWarnings({"all"})
    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();

        player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.recruit))
                .stream()
                .filter(AbstractRecruitEntity::isAlive)
                .findAny()
                .ifPresent(this::mount);
    }

    @SuppressWarnings({"all"})
    private void mount(AbstractRecruitEntity recruit) {
        if(this.back && recruit.getMountUUID() != null){
            recruit.shouldMount(true, recruit.getMountUUID());
        }
        else if (recruit.getVehicle() == null){
            ArrayList<Entity> list = (ArrayList<Entity>) recruit.level.getEntitiesOfClass(Entity.class, recruit.getBoundingBox().inflate(8));

            list.removeIf(mount -> !RecruitsServerConfig.MountWhiteList.get().contains(mount.getEncodeId()));
            list.removeIf(mount -> mount instanceof AbstractHorse horse && horse.hasControllingPassenger());
            list.sort(Comparator.comparing(horseInList -> horseInList.distanceTo(recruit)));

            if(!list.isEmpty()){
                recruit.shouldMount(true, list.get(0).getUUID());
            }
            else//owner cant be null
                recruit.getOwner().sendSystemMessage(TEXT_NO_MOUNT(recruit.getName().getString()));

        }
    }

    public MessageMountEntityGui fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.back = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.back);
    }

    private static MutableComponent TEXT_NO_MOUNT(String name){
        return Component.translatable("chat.recruits.text.noMount", name);
    }
}
