package com.talhanation.recruits.world;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Formeln für den Noble-Ruf. Wird auf Server und Client gleich benutzt,
 * damit der Screen dieselben Preise und Sperren zeigt, die der Server prüft.
 */
public final class NobleReputation {

    public static final int MIN = -100;
    public static final int MAX = 100;

    public static final int HOSTILE_MAX = -50;    // ≤ −50
    public static final int RESPECTED_MIN = 25;   // 25 … 74
    public static final int HONORED_MIN = 75;     // ≥ 75

    public static final double MEMBER_PLAYER_WEIGHT = 0.75D;
    public static final double LEADER_PLAYER_WEIGHT = 0.50D;
    public static final double PRICE_FACTOR_PER_POINT = 0.003D;

    private NobleReputation() {
    }

    public enum Tier {
        HOSTILE("hostile", 0, ChatFormatting.DARK_RED),
        NEUTRAL("neutral", 1, ChatFormatting.GRAY),
        RESPECTED("respected", 2, ChatFormatting.GREEN),
        HONORED("honored", 3, ChatFormatting.GOLD);

        private final String key;
        private final int maxTradeLevel;
        private final ChatFormatting color;

        Tier(String key, int maxTradeLevel, ChatFormatting color) {
            this.key = key;
            this.maxTradeLevel = maxTradeLevel;
            this.color = color;
        }

        public int getMaxTradeLevel() {
            return maxTradeLevel;
        }

        public ChatFormatting getColor() {
            return color;
        }

        public Component getName() {
            return Component.translatable("gui.recruits.noble.tier." + key).withStyle(color);
        }
    }

    public static int clamp(int value) {
        return Mth.clamp(value, MIN, MAX);
    }

    public static Tier getTier(int reputation) {
        if (reputation <= HOSTILE_MAX) return Tier.HOSTILE;
        if (reputation < RESPECTED_MIN) return Tier.NEUTRAL;
        if (reputation < HONORED_MIN) return Tier.RESPECTED;
        return Tier.HONORED;
    }

    /**
     * Mitglied: 75 % Spieler + 25 % Fraktion. Leader: 50/50. Ohne Fraktion: nur Spieler.
     */
    public static int getEffective(int playerRep, int factionRep, boolean hasFaction, boolean isLeader) {
        if (!hasFaction) return clamp(playerRep);
        double w = isLeader ? LEADER_PLAYER_WEIGHT : MEMBER_PLAYER_WEIGHT;
        return clamp((int) Math.round(w * playerRep + (1D - w) * factionRep));
    }

    /**
     * Preis = round(Grundpreis × (1 − 0,003 × R)). −49 → ×1,15 · +100 → ×0,70.
     */
    public static int getPrice(int basePrice, int reputation) {
        if (basePrice <= 0) return 0;
        double factor = 1D - PRICE_FACTOR_PER_POINT * clamp(reputation);
        return Math.max(1, (int) Math.round(basePrice * factor));
    }

    /** Prozent-Abweichung vom Grundpreis, z. B. −8 für 8 % günstiger. */
    public static int getPricePercent(int reputation) {
        return (int) Math.round(-PRICE_FACTOR_PER_POINT * clamp(reputation) * 100D);
    }

    public static Tier getRequiredTier(int tradeLevel) {
        if (tradeLevel <= 1) return Tier.NEUTRAL;
        if (tradeLevel == 2) return Tier.RESPECTED;
        return Tier.HONORED;
    }

    public static boolean isTradeLevelUnlocked(int reputation, int tradeLevel) {
        return getTier(reputation).getMaxTradeLevel() >= Math.max(1, tradeLevel);
    }

    /** Ruf-Änderung für die Fraktion: Δ / 2 (ganzzahlig, wie im Konzept). */
    public static int getFactionDelta(int delta) {
        return delta / 2;
    }

    public static String format(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }
}
