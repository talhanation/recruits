package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessagePromoteRecruit implements Message<MessagePromoteRecruit> {

    private UUID recruit;
    private int profession;
    private String name;
    public MessagePromoteRecruit(){
    }

    public MessagePromoteRecruit(UUID recruit, int profession, String name) {
        this.recruit = recruit;
        this.profession = profession;
        this.name = name;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                context.getSender().getBoundingBox().inflate(16D),
                livingEntity -> livingEntity.getUUID().equals(this.recruit) && NetworkSecurity.canControl(context.getSender(), livingEntity)
                        && canPromote(livingEntity, this.profession)
        ).forEach((recruit) -> RecruitEvents.promoteRecruit(recruit, profession, NetworkSecurity.cleanText(name, 32), context.getSender()));

    }

    /** Server copy of the rules in PromoteScreen (before they were only checked on the client). */
    private static boolean canPromote(AbstractRecruitEntity recruit, int profession) {
        int level = recruit.getXpLevel();
        return switch (profession) {
            case 0, 1 -> level >= 2;                                                              // messenger, scout
            case 2 -> level >= 3 && Main.isSiegeWeaponsLoaded && Main.isSiegeWeaponsCompatible; // siege engineer
            case 3 -> level >= 5 && Main.isSmallShipsLoaded && Main.isSmallShipsCompatible;     // captain
            case 4 -> level >= 5;                                                                 // patrol leader
            default -> false;                                                                     // disabled / unknown
        };
    }

    public MessagePromoteRecruit fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.profession = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(profession);
        buf.writeUtf(name);
    }

}