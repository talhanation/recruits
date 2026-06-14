package com.talhanation.recruits.network.codec;

import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ClaimPayloadCodec {
    private static final int FLAG_ALLOW_INTERACTION = 1;
    private static final int FLAG_ALLOW_PLACEMENT = 1 << 1;
    private static final int FLAG_ALLOW_BREAKING = 1 << 2;
    private static final int FLAG_ADMIN = 1 << 3;
    private static final int FLAG_UNDER_SIEGE = 1 << 4;
    private static final int FLAG_REMOVED = 1 << 5;
    private static final int MAX_CLAIM_PARTIES = 1024;
    private static final int MAX_CHUNKS_PER_CLAIM = 4096;

    private ClaimPayloadCodec() {}

    static void write(FriendlyByteBuf buf, RecruitsClaim claim) {
        ClaimCenter center = ClaimCenter.of(claim);

        writeHeader(buf, claim);
        NetworkBufferHelper.writeOptionalNbt(
                buf,
                claim.getOwnerFaction() == null ? null : claim.getOwnerFaction().toNBT());
        NetworkBufferHelper.writeOptionalNbt(
                buf,
                claim.getPlayerInfo() == null ? null : claim.getPlayerInfo().toNBT());
        writeState(buf, claim, center);
        writeFactionList(buf, claim.defendingParties);
        writeFactionList(buf, claim.attackingParties);
    }

    static RecruitsClaim read(FriendlyByteBuf buf) {
        ClaimHeader header = readHeader(buf);
        RecruitsFaction ownerFaction =
                RecruitsFaction.fromNBT(NetworkBufferHelper.readOptionalNbt(buf));
        RecruitsPlayerInfo playerInfo =
                RecruitsPlayerInfo.getFromNBT(NetworkBufferHelper.readOptionalNbt(buf));
        RecruitsClaim claim = readState(buf, header, ownerFaction, playerInfo);
        claim.defendingParties.addAll(readFactionList(buf));
        claim.attackingParties.addAll(readFactionList(buf));
        return claim;
    }

    static void writeWithFactionTable(
            FriendlyByteBuf buf, RecruitsClaim claim, ClaimFactionTable factionTable) {
        ClaimCenter center = ClaimCenter.of(claim);

        writeHeader(buf, claim);
        writeFactionReference(buf, factionTable, claim.getOwnerFaction());
        writePlayerInfo(buf, claim.getPlayerInfo(), factionTable);
        writeState(buf, claim, center);
        writeFactionReferences(buf, claim.defendingParties, factionTable);
        writeFactionReferences(buf, claim.attackingParties, factionTable);
    }

    static RecruitsClaim readWithFactionTable(
            FriendlyByteBuf buf, ClaimFactionTable factionTable) {
        ClaimHeader header = readHeader(buf);
        RecruitsFaction ownerFaction = readFactionReference(buf, factionTable);
        RecruitsPlayerInfo playerInfo = readPlayerInfo(buf, factionTable);
        RecruitsClaim claim = readState(buf, header, ownerFaction, playerInfo);
        claim.defendingParties.addAll(readFactionReferences(buf, factionTable));
        claim.attackingParties.addAll(readFactionReferences(buf, factionTable));
        return claim;
    }

    private static void writeHeader(FriendlyByteBuf buf, RecruitsClaim claim) {
        buf.writeUUID(claim.getUUID());
        buf.writeUtf(claim.getName() == null ? "" : claim.getName());
    }

    private static ClaimHeader readHeader(FriendlyByteBuf buf) {
        return new ClaimHeader(
                buf.readUUID(),
                buf.readUtf());
    }

    private static void writeState(FriendlyByteBuf buf, RecruitsClaim claim, ClaimCenter center) {
        buf.writeByte(packFlags(claim));
        buf.writeInt(center.x());
        buf.writeInt(center.z());
        buf.writeVarInt(claim.getHealth());
        buf.writeFloat(claim.getSiegeSpeedPercent());
        writeChunks(buf, claim.getClaimedChunks(), center.x(), center.z());
    }

    private static RecruitsClaim readState(
            FriendlyByteBuf buf,
            ClaimHeader header,
            @Nullable RecruitsFaction ownerFaction,
            @Nullable RecruitsPlayerInfo playerInfo) {
        int flags = buf.readUnsignedByte();
        int centerX = buf.readInt();
        int centerZ = buf.readInt();
        int health = buf.readVarInt();
        float siegeSpeedPercent = buf.readFloat();

        RecruitsClaim claim = RecruitsClaim.fromNetwork(header.uuid(), header.name(), ownerFaction);
        claim.setPlayer(playerInfo);
        claim.setBlockInteractionAllowed((flags & FLAG_ALLOW_INTERACTION) != 0);
        claim.setBlockPlacementAllowed((flags & FLAG_ALLOW_PLACEMENT) != 0);
        claim.setBlockBreakingAllowed((flags & FLAG_ALLOW_BREAKING) != 0);
        claim.setAdminClaim((flags & FLAG_ADMIN) != 0);
        claim.isUnderSiege = (flags & FLAG_UNDER_SIEGE) != 0;
        claim.isRemoved = (flags & FLAG_REMOVED) != 0;
        claim.setCenter(new ChunkPos(centerX, centerZ));
        claim.setHealth(health);
        claim.setSiegeSpeedPercent(siegeSpeedPercent);

        readChunks(buf, claim, centerX, centerZ);
        return claim;
    }

    private static int packFlags(RecruitsClaim claim) {
        int flags = 0;
        if (claim.isBlockInteractionAllowed()) flags |= FLAG_ALLOW_INTERACTION;
        if (claim.isBlockPlacementAllowed()) flags |= FLAG_ALLOW_PLACEMENT;
        if (claim.isBlockBreakingAllowed()) flags |= FLAG_ALLOW_BREAKING;
        if (claim.isAdmin) flags |= FLAG_ADMIN;
        if (claim.isUnderSiege) flags |= FLAG_UNDER_SIEGE;
        if (claim.isRemoved) flags |= FLAG_REMOVED;
        return flags;
    }

    private static void writeChunks(FriendlyByteBuf buf, List<ChunkPos> chunks, int centerX, int centerZ) {
        int count = 0;
        if (chunks != null) {
            for (ChunkPos chunk : chunks) {
                if (chunk != null) count++;
            }
        }

        buf.writeVarInt(count);
        if (chunks == null) return;

        for (ChunkPos chunk : chunks) {
            if (chunk == null) continue;
            buf.writeVarInt(NetworkBufferHelper.zigZagEncode(chunk.x - centerX));
            buf.writeVarInt(NetworkBufferHelper.zigZagEncode(chunk.z - centerZ));
        }
    }

    private static void readChunks(FriendlyByteBuf buf, RecruitsClaim claim, int centerX, int centerZ) {
        int count =
                NetworkBufferHelper.readBoundedVarInt(buf, MAX_CHUNKS_PER_CLAIM, "claim chunks");
        for (int i = 0; i < count; i++) {
            int x = centerX + NetworkBufferHelper.zigZagDecode(buf.readVarInt());
            int z = centerZ + NetworkBufferHelper.zigZagDecode(buf.readVarInt());
            claim.addChunk(new ChunkPos(x, z));
        }
    }

    private static void writeFactionList(FriendlyByteBuf buf, List<RecruitsFaction> factions) {
        int count = 0;
        if (factions != null) {
            for (RecruitsFaction faction : factions) {
                if (faction != null) count++;
            }
        }

        buf.writeVarInt(count);
        if (factions == null) return;

        for (RecruitsFaction faction : factions) {
            if (faction != null) {
                NetworkBufferHelper.writeOptionalNbt(buf, faction.toNBT());
            }
        }
    }

    private static List<RecruitsFaction> readFactionList(FriendlyByteBuf buf) {
        int count = NetworkBufferHelper.readBoundedVarInt(buf, MAX_CLAIM_PARTIES, "claim parties");
        List<RecruitsFaction> factions = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            RecruitsFaction faction =
                    RecruitsFaction.fromNBT(NetworkBufferHelper.readOptionalNbt(buf));
            if (faction != null) {
                factions.add(faction);
            }
        }

        return factions;
    }

    private static void writePlayerInfo(
            FriendlyByteBuf buf,
            @Nullable RecruitsPlayerInfo playerInfo,
            ClaimFactionTable factionTable) {
        buf.writeBoolean(playerInfo != null);
        if (playerInfo == null) return;

        buf.writeUUID(playerInfo.getUUID());
        buf.writeUtf(playerInfo.getName() == null ? "" : playerInfo.getName());
        buf.writeBoolean(playerInfo.isOnline());
        writeFactionReference(buf, factionTable, playerInfo.getFaction());
    }

    @Nullable
    private static RecruitsPlayerInfo readPlayerInfo(
            FriendlyByteBuf buf, ClaimFactionTable factionTable) {
        if (!buf.readBoolean()) return null;

        UUID uuid = buf.readUUID();
        String name = buf.readUtf();
        boolean online = buf.readBoolean();
        RecruitsFaction faction = readFactionReference(buf, factionTable);
        RecruitsPlayerInfo playerInfo = new RecruitsPlayerInfo(uuid, name, faction);
        playerInfo.setOnline(online);
        return playerInfo;
    }

    private static void writeFactionReferences(
            FriendlyByteBuf buf,
            List<RecruitsFaction> factions,
            ClaimFactionTable factionTable) {
        int count = 0;
        if (factions != null) {
            for (RecruitsFaction faction : factions) {
                if (faction != null) count++;
            }
        }

        buf.writeVarInt(count);
        if (factions == null) return;

        for (RecruitsFaction faction : factions) {
            if (faction != null) {
                writeFactionReference(buf, factionTable, faction);
            }
        }
    }

    private static List<RecruitsFaction> readFactionReferences(
            FriendlyByteBuf buf, ClaimFactionTable factionTable) {
        int count = NetworkBufferHelper.readBoundedVarInt(buf, MAX_CLAIM_PARTIES, "claim parties");
        List<RecruitsFaction> factions = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            RecruitsFaction faction = readFactionReference(buf, factionTable);
            if (faction != null) {
                factions.add(faction);
            }
        }

        return factions;
    }

    private static void writeFactionReference(
            FriendlyByteBuf buf,
            ClaimFactionTable factionTable,
            @Nullable RecruitsFaction faction) {
        int index = factionTable.indexOf(faction);
        buf.writeVarInt(index < 0 ? 0 : index + 1);
    }

    @Nullable
    private static RecruitsFaction readFactionReference(
            FriendlyByteBuf buf, ClaimFactionTable factionTable) {
        int encodedIndex =
                NetworkBufferHelper.readBoundedVarInt(
                        buf, factionTable.size(), "faction reference");
        if (encodedIndex == 0) return null;
        return factionTable.faction(encodedIndex - 1);
    }

    private record ClaimHeader(UUID uuid, String name) {}

    private record ClaimCenter(int x, int z) {
        static ClaimCenter of(RecruitsClaim claim) {
            ChunkPos center = claim.getCenter();
            return center == null ? new ClaimCenter(0, 0) : new ClaimCenter(center.x, center.z);
        }
    }
}
