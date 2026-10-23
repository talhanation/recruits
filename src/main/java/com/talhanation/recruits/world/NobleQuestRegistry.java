package com.talhanation.recruits.world;

import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Feste Auftragsliste, wie die Hire-Trades in {@link RecruitsHireTradesRegistry}.
 */
public class NobleQuestRegistry {

    public static final int OFFERS_PER_DAY = 3;

    public static final int REWARD_REPUTATION = 5;
    public static final int BOUNTY_REWARD_REPUTATION = 10;
    public static final int REWARD_CURRENCY = 1;

    /** Ab diesem Ruf (Spieler oder Fraktion) kann ein Kopfgeld ausgesetzt werden. */
    public static final int BOUNTY_THRESHOLD = -30;
    /** „Schuld beglichen“: Ruf-Gewinn für das Ziel nach erfülltem Kopfgeld. */
    public static final int BOUNTY_ATONEMENT = 15;

    public record Entry(String target, int min, int max) {
        int roll(RandomSource random) {
            return min + random.nextInt(max - min + 1);
        }
    }

    /** Lieferung: Brot, Weizen, Eisen, Pfeile, Holz. */
    public static final List<Entry> DELIVERIES = List.of(
            new Entry("minecraft:bread", 8, 16),
            new Entry("minecraft:wheat", 16, 32),
            new Entry("minecraft:iron_ingot", 4, 8),
            new Entry("minecraft:arrow", 16, 32),
            new Entry("#minecraft:logs", 16, 32)
    );

    /** Monsterjagd: Zombies, Skelette, Spinnen. */
    public static final List<Entry> MONSTER_HUNTS = List.of(
            new Entry("minecraft:zombie", 4, 8),
            new Entry("minecraft:skeleton", 4, 8),
            new Entry("minecraft:spider", 3, 6)
    );

    public static NobleQuest createDelivery(RandomSource random, @Nullable String excludeTarget) {
        List<Entry> pool = new ArrayList<>(DELIVERIES);
        if (excludeTarget != null && pool.size() > 1) pool.removeIf(e -> e.target().equals(excludeTarget));
        Entry entry = pool.get(random.nextInt(pool.size()));
        return new NobleQuest(NobleQuest.Type.DELIVERY, entry.target(), entry.roll(random), REWARD_REPUTATION);
    }

    public static NobleQuest createMonsterHunt(RandomSource random) {
        Entry entry = MONSTER_HUNTS.get(random.nextInt(MONSTER_HUNTS.size()));
        return new NobleQuest(NobleQuest.Type.MONSTER_HUNT, entry.target(), entry.roll(random), REWARD_REPUTATION);
    }

    public static NobleQuest createBounty(UUID target, String targetName, @Nullable String targetFaction) {
        NobleQuest quest = new NobleQuest(NobleQuest.Type.BOUNTY, target.toString(), 1, BOUNTY_REWARD_REPUTATION);
        quest.targetName = targetName == null ? "" : targetName;
        quest.targetFaction = targetFaction == null ? "" : targetFaction;
        return quest;
    }

    public record BountyTarget(UUID player, String name, @Nullable String faction) {
    }

    /**
     * Drei Angebote: Lieferung, Monsterjagd und Kopfgeld. Gibt es kein Kopfgeld-Ziel,
     * wird statt Kopfgeld eine zweite Lieferung gezogen.
     */
    public static List<NobleQuest> drawOffers(RandomSource random, List<BountyTarget> bountyTargets) {
        List<NobleQuest> offers = new ArrayList<>();
        NobleQuest delivery = createDelivery(random, null);
        offers.add(delivery);
        offers.add(createMonsterHunt(random));

        if (!bountyTargets.isEmpty()) {
            BountyTarget target = bountyTargets.get(random.nextInt(bountyTargets.size()));
            offers.add(createBounty(target.player(), target.name(), target.faction()));
        } else {
            offers.add(createDelivery(random, delivery.target));
        }
        return offers;
    }
}
