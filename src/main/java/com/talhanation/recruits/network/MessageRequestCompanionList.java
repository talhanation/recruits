package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.CompanionRecord;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client → Server: the player opened the companion list at his Messenger and asks which companions he has and
 * where they are. Answered with {@link MessageToClientCompanionList}.
 */
public class MessageRequestCompanionList implements Message<MessageRequestCompanionList> {
    private UUID messenger;

    public MessageRequestCompanionList() {
    }

    public MessageRequestCompanionList(UUID messenger) {
        this.messenger = messenger;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || RecruitEvents.recruitsCompanionManager == null) return;
        if (CompanionOrders.idleMessenger(player, this.messenger) == null) return;

        // only his own companions, and not the Messenger he is talking to
        List<CompanionRecord> list = new ArrayList<>();
        for (CompanionRecord record : RecruitEvents.recruitsCompanionManager.getCompanions(player.getUUID())) {
            if (!record.uuid.equals(this.messenger)) list.add(record);
        }
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MessageToClientCompanionList(list));
    }

    public MessageRequestCompanionList fromBytes(FriendlyByteBuf buf) {
        this.messenger = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.messenger);
    }
}
