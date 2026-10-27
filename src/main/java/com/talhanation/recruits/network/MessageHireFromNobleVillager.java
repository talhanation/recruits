package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsVillageManager;
import net.minecraft.network.chat.Component;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsHireTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageHireFromNobleVillager implements Message<MessageHireFromNobleVillager> {
    private UUID nobleUUID;
    private UUID villagerUUID;
    private int cost;
    private boolean needsVillager;
    private boolean closing;
    private ResourceLocation resource;
    private UUID groupUUID;
    public MessageHireFromNobleVillager() {
    }

    public MessageHireFromNobleVillager(UUID nobleUUID, UUID villagerUUID, RecruitsHireTrade trade, RecruitsGroup group, boolean needsVillager, boolean closing) {
        this.nobleUUID = nobleUUID;
        this.villagerUUID = villagerUUID;
        this.groupUUID = group.getUUID();
        if(trade != null){
            this.cost = trade.cost;
            this.resource = trade.resourceLocation;
        }
        else{
            this.cost = 0;
            this.resource = new ResourceLocation("","");
        }

        this.needsVillager = needsVillager;
        this.closing = closing;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null) return;
        ServerLevel serverLevel = player.serverLevel();

        VillagerNobleEntity villagerNoble = RecruitsVillageManager.findNoble(player, this.nobleUUID, 32.0D);
        if(villagerNoble == null) return;

        if(closing){
            villagerNoble.isTrading(false);
            return;
        }

        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        if(group == null) return;

        // Alles auf dem Server prüfen: Trade vorhanden, Nutzungen übrig, Ruf-Stufe, Preis.
        RecruitsHireTrade trade = villagerNoble.findTrade(resource);
        if(trade == null || trade.uses <= 0) return;

        int price = VillageEvents.villageManager != null ? VillageEvents.villageManager.getHirePrice(player, villagerNoble, trade) : trade.cost;
        if(price < 0){
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.trade_locked"));
            return;
        }

        Optional<EntityType<?>> optionalType = EntityType.byString(trade.resourceLocation.toString());
        if(optionalType.isEmpty()) return;
        @SuppressWarnings("unchecked")
        EntityType<? extends AbstractRecruitEntity> recruitType = (EntityType<? extends AbstractRecruitEntity>) optionalType.get();

        boolean hired;
        // Die Config entscheidet, nicht das Flag vom Client.
        if(RecruitsServerConfig.NobleVillagerNeedsVillagers.get()){
            // the villager comes from the noble's village (jobless residents), not from around the player;
            // the server picks him, the UUID from the client is not used any more
            Villager villager = VillageEvents.villageManager != null ? VillageEvents.villageManager.pickVillagerForHire(serverLevel, villagerNoble) : null;
            if(villager == null){
                player.sendSystemMessage(Component.translatable("chat.recruits.noble.no_villagers", RecruitsVillageManager.MIN_JOBLESS_FOR_HIRE));
                return;
            }

            hired = VillagerEvents.createHiredRecruitFromVillager(serverLevel, villager, recruitType, player, group, price);
        }
        else{
            hired = VillagerEvents.spawnHiredRecruit(serverLevel, recruitType, player, group, price);
        }

        if(hired) villagerNoble.doTrade(trade.resourceLocation);
        // new count of jobless villagers etc. for the open screen
        if(VillageEvents.villageManager != null) VillageEvents.villageManager.sendInfo(player, villagerNoble, false);

        String stringID = player.getTeam() != null ? player.getTeam().getName() : "";
        boolean canHire = RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(stringID, player.getUUID());
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateHireState(canHire));
    }

    public MessageHireFromNobleVillager fromBytes(FriendlyByteBuf buf) {
        this.nobleUUID = buf.readUUID();
        this.villagerUUID = buf.readUUID();
        this.cost = buf.readInt();
        this.resource = buf.readResourceLocation();
        this.needsVillager = buf.readBoolean();
        this.closing = buf.readBoolean();
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.nobleUUID);
        buf.writeUUID(this.villagerUUID);
        buf.writeInt(this.cost);
        buf.writeResourceLocation(resource);
        buf.writeBoolean(needsVillager);
        buf.writeBoolean(closing);
        buf.writeUUID(this.groupUUID);
    }
}