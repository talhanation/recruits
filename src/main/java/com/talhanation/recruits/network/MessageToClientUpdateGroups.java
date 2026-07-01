package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MessageToClientUpdateGroups implements Message<MessageToClientUpdateGroups> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateGroups> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdategroups"));
    private CompoundTag nbt;
    public MessageToClientUpdateGroups() {

    }

    public MessageToClientUpdateGroups(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(IPayloadContext context) {
        List<RecruitsGroup> updated = RecruitsGroup.listFromNbt(this.nbt);

        Map<UUID, RecruitsGroup> updatedMap = new HashMap<>();
        for (RecruitsGroup group : updated) {
            updatedMap.put(group.getUUID(), group);
        }

        ClientManager.groups.removeIf(
                clientGroup -> !updatedMap.containsKey(clientGroup.getUUID())
        );
        
        for (RecruitsGroup updatedGroup : updated) {

            RecruitsGroup existing = null;

            for (RecruitsGroup clientGroup : ClientManager.groups) {
                if (clientGroup.getUUID().equals(updatedGroup.getUUID())) {
                    existing = clientGroup;
                    break;
                }
            }

            if (existing == null) {
                ClientManager.groups.add(updatedGroup);
            } else {
                existing.leaderUUID = updatedGroup.leaderUUID;
                existing.members    = updatedGroup.members;
                existing.setSize(updatedGroup.getSize());
                existing.setName(updatedGroup.getName());
                existing.setCount(updatedGroup.getCount());
            }
        }
    }

    @Override
    public MessageToClientUpdateGroups fromBytes(RegistryFriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateGroups> type() {
        return TYPE;
    }
}