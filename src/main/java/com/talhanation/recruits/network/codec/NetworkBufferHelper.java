package com.talhanation.recruits.network.codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

final class NetworkBufferHelper {
    private NetworkBufferHelper() {}

    static void writeOptionalNbt(FriendlyByteBuf buf, @Nullable CompoundTag tag) {
        boolean present = tag != null && !tag.isEmpty();
        buf.writeBoolean(present);
        if (present) {
            buf.writeNbt(tag);
        }
    }

    static CompoundTag readOptionalNbt(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return new CompoundTag();

        CompoundTag tag = buf.readNbt();
        return tag == null ? new CompoundTag() : tag;
    }

    static int readBoundedVarInt(FriendlyByteBuf buf, int max, String label) {
        int value = buf.readVarInt();
        if (value < 0 || value > max) {
            throw new IllegalArgumentException("Invalid " + label + " count: " + value);
        }
        return value;
    }

    static int zigZagEncode(int value) {
        return (value << 1) ^ (value >> 31);
    }

    static int zigZagDecode(int value) {
        return (value >>> 1) ^ -(value & 1);
    }
}
