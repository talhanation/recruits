package com.talhanation.recruits.compat.corpse;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

public final class RecruitCorpseEntityTypeResolver {

    private static final ResourceLocation DEFAULT_RECRUIT_ID = new ResourceLocation(Main.MOD_ID, "recruit");
    private static final ResourceLocation DEFAULT_SCOUT_ID = new ResourceLocation(Main.MOD_ID, "scout");
    private static final Map<Integer, EntityType<? extends AbstractRecruitEntity>> RESOLVED_TYPES = new ConcurrentHashMap<>();

    private RecruitCorpseEntityTypeResolver() {
    }

    public static int getEntityTypeHash(AbstractRecruitEntity recruit) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(recruit.getType());
        return id == null ? 0 : stableHash(id);
    }

    @Nullable
    public static EntityType<? extends AbstractRecruitEntity> resolveEntityType(int hash, boolean companionFallback, Level level) {
        if (hash != 0) {
            EntityType<? extends AbstractRecruitEntity> cached = RESOLVED_TYPES.get(hash);
            if (cached != null) {
                return cached;
            }

            for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
                ResourceLocation id = entry.getKey().location();
                if (!Main.MOD_ID.equals(id.getNamespace()) || stableHash(id) != hash) {
                    continue;
                }

                EntityType<?> rawType = entry.getValue();
                Entity probe = rawType.create(level);
                if (probe instanceof AbstractRecruitEntity) {
                    @SuppressWarnings("unchecked")
                    EntityType<? extends AbstractRecruitEntity> recruitType = (EntityType<? extends AbstractRecruitEntity>) rawType;
                    RESOLVED_TYPES.put(hash, recruitType);
                    return recruitType;
                }
            }
        }

        ResourceLocation fallbackId = companionFallback ? DEFAULT_SCOUT_ID : DEFAULT_RECRUIT_ID;
        EntityType<?> fallbackType = ForgeRegistries.ENTITY_TYPES.getValue(fallbackId);
        if (fallbackType == null) {
            return null;
        }

        Entity fallbackProbe = fallbackType.create(level);
        if (fallbackProbe instanceof AbstractRecruitEntity) {
            @SuppressWarnings("unchecked")
            EntityType<? extends AbstractRecruitEntity> recruitType = (EntityType<? extends AbstractRecruitEntity>) fallbackType;
            return recruitType;
        }
        return null;
    }

    private static int stableHash(ResourceLocation id) {
        CRC32 crc32 = new CRC32();
        crc32.update(id.toString().getBytes(StandardCharsets.UTF_8));
        return (int) crc32.getValue();
    }
}
