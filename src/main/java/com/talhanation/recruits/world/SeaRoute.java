package com.talhanation.recruits.world;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * The way over water along a patrol route, found once by the {@link SeaRoutePlanner} when the route is given to a
 * captain. One leg per stretch from a way point to the next one; a route sailed round in a circle has one more,
 * from the last way point back to the first.
 * <p>
 * A ship out of the world ({@link SimulatedTravel}) sails along these legs instead of the straight line.
 */
public final class SeaRoute {
    private static final int[] NONE = new int[0];

    /** What the way was found for: the way points, the kind of route and the ship. Another value: find it again. */
    public final long signature;
    /** Per leg the corners of the way: x, z, x, z, ... from way point i towards the next one. */
    private final int[][] legs;
    /** Per leg: the way ends short of the next way point - no water leads there. */
    private final boolean[] partial;

    public SeaRoute(long signature, int[][] legs, boolean[] partial) {
        this.signature = signature;
        this.legs = legs;
        this.partial = partial;
    }

    public int legs() {
        return legs.length;
    }

    /** @return the corners of this leg as x, z pairs - empty for a leg that does not exist */
    public int[] leg(int leg) {
        return leg >= 0 && leg < legs.length ? legs[leg] : NONE;
    }

    /** @return true if no water leads all the way along this leg */
    public boolean isPartial(int leg) {
        return leg >= 0 && leg < partial.length && partial[leg];
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Signature", signature);
        ListTag list = new ListTag();
        byte[] flags = new byte[legs.length];
        for (int i = 0; i < legs.length; i++) {
            list.add(new IntArrayTag(legs[i]));
            flags[i] = (byte) (partial[i] ? 1 : 0);
        }
        tag.put("Legs", list);
        tag.put("Partial", new ByteArrayTag(flags));
        return tag;
    }

    public static SeaRoute load(CompoundTag tag) {
        ListTag list = tag.getList("Legs", Tag.TAG_INT_ARRAY);
        byte[] flags = tag.getByteArray("Partial");
        int[][] legs = new int[list.size()][];
        boolean[] partial = new boolean[list.size()];
        for (int i = 0; i < legs.length; i++) {
            int[] way = list.getIntArray(i);
            legs[i] = way.length % 2 == 0 ? way : NONE;
            partial[i] = i < flags.length && flags[i] != 0;
        }
        return new SeaRoute(tag.getLong("Signature"), legs, partial);
    }
}
