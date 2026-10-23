package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Der aktive Auftrag eines Spielers. Höchstens einer pro Spieler.
 */
public class NobleQuestProgress {

    /** Gilt einen Ingame-Tag ab Annahme. */
    public static final long DURATION_TICKS = 24000L;

    public final NobleQuest quest;
    public final UUID villageId;
    public int progress;
    public boolean completed;
    public long expiresAt;

    public NobleQuestProgress(NobleQuest quest, UUID villageId, long expiresAt) {
        this.quest = quest;
        this.villageId = villageId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(long gameTime) {
        return gameTime >= expiresAt;
    }

    public void addProgress(int amount) {
        if (completed) return;
        this.progress = Math.min(quest.count, progress + amount);
        if (progress >= quest.count) completed = true;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("quest", quest.toNBT());
        tag.putUUID("village", villageId);
        tag.putInt("progress", progress);
        tag.putBoolean("completed", completed);
        tag.putLong("expiresAt", expiresAt);
        return tag;
    }

    public static NobleQuestProgress fromNBT(CompoundTag tag) {
        if (!tag.hasUUID("village")) return null;
        NobleQuestProgress progress = new NobleQuestProgress(
                NobleQuest.fromNBT(tag.getCompound("quest")),
                tag.getUUID("village"),
                tag.getLong("expiresAt"));
        progress.progress = tag.getInt("progress");
        progress.completed = tag.getBoolean("completed");
        return progress;
    }
}
