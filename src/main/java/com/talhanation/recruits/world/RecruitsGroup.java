package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RecruitsGroup {

    public static List<ResourceLocation> IMAGES = new ArrayList<>(
        Arrays.asList(
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/sword.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/shield.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/bow.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/crossbow.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/arrow.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/horse.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/horse_arrow.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/house.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/tower.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/fort.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/tent.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/ship.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/ship2.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/catapult.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/axe.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/hoe.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/pickaxe.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/sword2.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/arrow2.png"),
            new ResourceLocation(Main.MOD_ID, "textures/gui/image/group/3arrow.png")
        )
    );
    private UUID uuid;
    private int size;
    private int count;
    private UUID playerUUID;
    private String playerName;
    private String name;
    private boolean disabled;
    public boolean removed;
    public DisbandContext disbandContext;
    private int image;

    public BlockPos upkeep;
    public int aggroState;
    public int followState;
    public UUID protectUUID;
    public UUID leaderUUID;
    public boolean allowRanged;
    public boolean allowRest;
    public int groupMorale;
    public int groupHealth;

    public RecruitsGroup(String name, RecruitsPlayerInfo playerInfo, int image){
        this(name, playerInfo.getUUID(), playerInfo.getName(), image);
    }

    public RecruitsGroup(String name, Player player, int image){
        this(name, player.getUUID(), player.getName().getString(), image);
    }
    public RecruitsGroup(String name, UUID playerUUID, String playerName, int image){
        this.name = name;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.uuid = UUID.randomUUID();
        this.image = image;
    }

    private RecruitsGroup(String name, UUID playerUUID, String playerName,  int size, boolean disabled, int image, DisbandContext disbandContext){
        this.name = name;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.size = size;
        this.disabled = disabled;
        this.image = image;
        this.disbandContext = disbandContext;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPlayer(RecruitsPlayerInfo playerInfo) {
        this.playerUUID = playerInfo.getUUID();
        this.playerName = playerInfo.getName();
    }

    public void setPlayer(Player player) {
        this.playerUUID = player.getUUID();
        this.playerName = player.getName().getString();
    }

    public void increaseSize() {
        this.size++;
    }

    public void decreaseSize(){
        this.size--;
        if(size < 0) size = 0;
    }
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }
    public int getImage() {
        return image;
    }
    public int getCount() {
        return count;
    }
    public void setImage(int x) {
        this.image = x;
    }
    public void setCount(int x) {
        this.count = x;
    }

    public void setDisabled(boolean x){
        this.disabled = x;
    }

    public boolean isDisabled(){
        return disabled;
    }

    public void setDisbandContext(DisbandContext disbandContext) {
        this.disbandContext = disbandContext;
    }

    public RecruitsGroup copy(){
        return new RecruitsGroup(this.name, this.playerUUID, this.playerName, this.size, this.disabled, this.image, this.disbandContext);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", this.uuid);
        tag.putString("name", this.name);
        tag.putUUID("playerUUID", this.playerUUID);
        tag.putString("playerName", this.playerName);
        tag.putInt("size", this.size);
        tag.putBoolean("disabled", this.disabled);
        tag.putBoolean("removed", this.removed);
        tag.putInt("image", this.image);
        if(leaderUUID != null) tag.putUUID("leaderUUID", this.leaderUUID);

        return tag;
    }

    public static RecruitsGroup fromNBT(CompoundTag tag) {
        if(tag == null || tag.isEmpty()) return null;

        UUID uuid = tag.getUUID("uuid");
        String name = tag.getString("name");
        String playerName = tag.getString("playerName");

        UUID playerUUID = tag.getUUID("playerUUID");
        
        int size = tag.getInt("size");
        int image = tag.getInt("image");

        boolean disabled = tag.getBoolean("disabled");
        boolean removed = tag.getBoolean("removed");
        DisbandContext disbandContext = DisbandContext.fromNBT(tag);


        RecruitsGroup group = new RecruitsGroup(name, playerUUID, playerName, size, disabled, image, disbandContext);
        group.uuid = uuid;
        group.removed = removed;

        if(tag.contains("leaderUUID")){
            group.leaderUUID = tag.getUUID("leaderUUID");
        }
        return group;
    }

    public static CompoundTag listToNbt(List<RecruitsGroup> groups) {
        CompoundTag compound = new CompoundTag();
        if (groups == null) return compound;

        ListTag list = new ListTag();
        for (RecruitsGroup t : groups) {
            list.add(t.toNBT());
        }
        compound.put("Groups", list);
        return compound;
    }

    public static List<RecruitsGroup> listFromNbt(CompoundTag compound) {
        List<RecruitsGroup> out = new ArrayList<>();
        if (compound == null || !compound.contains("Groups", Tag.TAG_LIST)) {
            return out;
        }

        ListTag list = compound.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            out.add(RecruitsGroup.fromNBT(entry));
        }
        return out;
    }

    public static class DisbandContext{

        public boolean disband;
        public boolean keepTeam;
        public boolean increaseCost;

        public DisbandContext(boolean disband, boolean keepTeam, boolean increaseCost){
            this.disband = disband;
            this.keepTeam = keepTeam;
            this.increaseCost = increaseCost;
        }


        public CompoundTag toNBT(){
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("disband", this.disband);
            tag.putBoolean("keepTeam", this.keepTeam);
            tag.putBoolean("increaseCost", this.increaseCost);

            return tag;
        }
        public static DisbandContext fromNBT(CompoundTag tag) {
            boolean disband = tag.getBoolean("disband");
            boolean keepTeam =  tag.getBoolean("keepTeam");;
            boolean increaseCost = tag.getBoolean("increaseCost");

            return new DisbandContext(disband, keepTeam, increaseCost);
        }

    }
}
