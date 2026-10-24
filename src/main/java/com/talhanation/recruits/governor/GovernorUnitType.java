package com.talhanation.recruits.governor;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;

/**
 * Unit types a Governor can hold in his garrison.
 * MELEE types may stand on Melee posts, the others on Ranged posts.
 */
public enum GovernorUnitType {
    RECRUIT(0, "recruit", true),
    SHIELDMAN(1, "recruit_shieldman", true),
    HORSEMAN(2, "horseman", true),
    BOWMAN(3, "bowman", false),
    CROSSBOWMAN(4, "crossbowman", false),
    NOMAD(5, "nomad", false),
    SIEGE_ENGINEER(6, "siege_engineer", false);

    public static final int COUNT = values().length;

    private final int index;
    private final String key;
    private final boolean melee;

    GovernorUnitType(int index, String key, boolean melee) {
        this.index = index;
        this.key = key;
        this.melee = melee;
    }

    public int getIndex() {
        return index;
    }

    public boolean isMelee() {
        return melee;
    }

    public boolean fitsPost(GovernorPost.Type postType) {
        return switch (postType) {
            case MELEE -> melee;
            case RANGED -> !melee;
            default -> false;
        };
    }

    public EntityType<? extends AbstractRecruitEntity> getEntityType() {
        return switch (this) {
            case RECRUIT -> ModEntityTypes.RECRUIT.get();
            case SHIELDMAN -> ModEntityTypes.RECRUIT_SHIELDMAN.get();
            case HORSEMAN -> ModEntityTypes.HORSEMAN.get();
            case BOWMAN -> ModEntityTypes.BOWMAN.get();
            case CROSSBOWMAN -> ModEntityTypes.CROSSBOWMAN.get();
            case NOMAD -> ModEntityTypes.NOMAD.get();
            case SIEGE_ENGINEER -> ModEntityTypes.SIEGE_ENGINEER.get();
        };
    }

    public ResourceLocation getEntityId() {
        return EntityType.getKey(getEntityType());
    }

    public Component getDisplayName() {
        return Component.translatable("entity.recruits." + key);
    }

    @Nullable
    public static GovernorUnitType of(AbstractRecruitEntity recruit) {
        if (recruit == null) return null;
        EntityType<?> type = recruit.getType();
        for (GovernorUnitType unitType : values()) {
            if (unitType.getEntityType() == type) return unitType;
        }
        return null;
    }

    @Nullable
    public static GovernorUnitType ofEntityId(ResourceLocation entityId) {
        if (entityId == null) return null;
        for (GovernorUnitType unitType : values()) {
            if (unitType.getEntityId().equals(entityId)) return unitType;
        }
        return null;
    }

    public static GovernorUnitType fromIndex(int index) {
        for (GovernorUnitType unitType : values()) {
            if (unitType.index == index) return unitType;
        }
        return RECRUIT;
    }
}
