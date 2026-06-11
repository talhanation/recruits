package com.talhanation.recruits.network.codec;

import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ClaimNetworkCodec {
    private static final int MAX_CLAIMS_PER_PACKET = 4096;

    private ClaimNetworkCodec() {}

    public static void writeClaimList(FriendlyByteBuf buf, List<RecruitsClaim> claims) {
        List<RecruitsClaim> filteredClaims = copyNonNullClaims(claims);
        ClaimFactionTable factionTable = ClaimFactionTable.collect(filteredClaims);

        factionTable.write(buf);
        buf.writeVarInt(filteredClaims.size());

        for (RecruitsClaim claim : filteredClaims) {
            ClaimPayloadCodec.writeWithFactionTable(buf, claim, factionTable);
        }
    }

    public static List<RecruitsClaim> readClaimList(FriendlyByteBuf buf) {
        ClaimFactionTable factionTable = ClaimFactionTable.read(buf);
        int count = NetworkBufferHelper.readBoundedVarInt(buf, MAX_CLAIMS_PER_PACKET, "claims");
        List<RecruitsClaim> claims = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            claims.add(ClaimPayloadCodec.readWithFactionTable(buf, factionTable));
        }

        return claims;
    }

    public static void writeNullableClaim(FriendlyByteBuf buf, @Nullable RecruitsClaim claim) {
        buf.writeBoolean(claim != null);
        if (claim != null) {
            ClaimPayloadCodec.write(buf, claim);
        }
    }

    @Nullable
    public static RecruitsClaim readNullableClaim(FriendlyByteBuf buf) {
        return buf.readBoolean() ? ClaimPayloadCodec.read(buf) : null;
    }

    private static List<RecruitsClaim> copyNonNullClaims(List<RecruitsClaim> claims) {
        List<RecruitsClaim> filteredClaims = new ArrayList<>();
        if (claims == null) return filteredClaims;

        for (RecruitsClaim claim : claims) {
            if (claim != null) {
                filteredClaims.add(claim);
            }
        }

        return filteredClaims;
    }
}
