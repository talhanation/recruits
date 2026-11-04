package com.talhanation.recruits.api.governor;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * A noble of a village in the claim offers an entity type.
 *
 * @param price the price for the Governor's owner (reputation), -1 if the owner may not buy it
 * @param uses  how many the noble still offers today
 */
public record HireOffer(UUID noble, String nobleName, ResourceLocation entityId, int price, int uses) {

    public boolean isLocked() {
        return price < 0;
    }
}
