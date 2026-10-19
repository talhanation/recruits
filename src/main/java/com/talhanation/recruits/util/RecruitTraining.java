package com.talhanation.recruits.util;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "Train" in the recruit's More menu: the owner pays experience points and the recruit gains XP.
 * Shared by the screen (button state) and the server packet, so both apply the same rules.
 * The server config is synced to clients, so the values are the same on both sides.
 */
public final class RecruitTraining {

    private RecruitTraining() {
    }

    /** Player experience points (not levels) per training. */
    public static int getPlayerXpCost() {
        return RecruitsServerConfig.RecruitTrainingPlayerXpCost.get();
    }

    /** Recruit XP per training. */
    public static int getRecruitXpGain() {
        return RecruitsServerConfig.RecruitTrainingRecruitXp.get();
    }

    public static boolean canBeTrained(AbstractRecruitEntity recruit) {
        return recruit != null
                && recruit.isAlive()
                && !(recruit instanceof VillagerNobleEntity)
                && recruit.getXpLevel() < RecruitsServerConfig.RecruitsMaxXpLevel.get();
    }

    /** Creative players train for free, like enchanting. */
    public static boolean canAfford(Player player) {
        return player.getAbilities().instabuild || getTotalXpPoints(player) >= getPlayerXpCost();
    }

    /**
     * Experience points the player currently has, computed from level + progress.
     * {@code Player#totalExperience} is not used: vanilla does not lower it when levels are spent (enchanting, anvil).
     */
    public static int getTotalXpPoints(Player player) {
        int level = player.experienceLevel;
        long base;
        if (level <= 16) base = (long) level * level + 6L * level;
        else if (level <= 31) base = Math.round(2.5D * level * level - 40.5D * level + 360D);
        else base = Math.round(4.5D * level * level - 162.5D * level + 2220D);

        long total = base + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, total));
    }

    /** Server side only. Permission and distance checks are done by the packet. */
    public static boolean train(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (!canBeTrained(recruit) || !canAfford(player)) return false;

        if (!player.getAbilities().instabuild) player.giveExperiencePoints(-getPlayerXpCost());

        recruit.addXp(getRecruitXpGain());
        recruit.checkLevel(); // plays the level-up sound and applies the buffs when the threshold is reached

        recruit.getCommandSenderWorld().playSound(null, recruit.getX(), recruit.getY(), recruit.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6F,
                0.9F + recruit.getRandom().nextFloat() * 0.3F);
        return true;
    }
}
