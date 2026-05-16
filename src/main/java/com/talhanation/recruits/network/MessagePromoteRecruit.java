package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.compat.workers.IVillagerWorker;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

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
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!RecruitEvents.entitiesByProfession.containsKey(this.profession)) {
            return;
        }

        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16D, false)
                .filter((recruit) -> canPromoteTo(recruit, this.profession))
                .ifPresent((recruit) -> RecruitEvents.promoteRecruit(recruit, profession, name, player));
    }

    static boolean canPromote(AbstractRecruitEntity recruit) {
        return recruit.getXpLevel() >= 3 || recruit instanceof IVillagerWorker;
    }

    static boolean canPromoteTo(AbstractRecruitEntity recruit, int profession) {
        boolean worker = recruit instanceof IVillagerWorker;
        return switch (profession) {
            case 0, 1 -> worker || recruit.getXpLevel() >= 2;
            case 2 -> (worker || recruit.getXpLevel() >= 3) && Main.isSiegeWeaponsLoaded && Main.isSiegeWeaponsCompatible;
            case 3 -> (worker || recruit.getXpLevel() >= 5) && Main.isSmallShipsLoaded && Main.isSmallShipsCompatible;
            case 4 -> worker || recruit.getXpLevel() >= 5;
            default -> false;
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
