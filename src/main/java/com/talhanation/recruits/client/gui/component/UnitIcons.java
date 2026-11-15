package com.talhanation.recruits.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.governor.GovernorUnitType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 21x21 icons of the unit types and companions, drawn in the style of the group icons
 * (textures/gui/image/unit). Used in the Governor garrison list, the promote screen and the inventory screen.
 * Addons add the icons of their units with {@link #register}.
 */
@OnlyIn(Dist.CLIENT)
public final class UnitIcons {

    public static final int SIZE = 21;

    public static final ResourceLocation RECRUIT = icon("recruit");
    public static final ResourceLocation SHIELDMAN = icon("shieldman");
    public static final ResourceLocation BOWMAN = icon("bowman");
    public static final ResourceLocation CROSSBOWMAN = icon("crossbowman");
    public static final ResourceLocation HORSEMAN = icon("horseman");
    public static final ResourceLocation NOMAD = icon("nomad");
    public static final ResourceLocation MESSENGER = icon("messenger");
    public static final ResourceLocation SCOUT = icon("scout");
    public static final ResourceLocation PATROL_LEADER = icon("patrol_leader");
    public static final ResourceLocation CAPTAIN = icon("captain");
    public static final ResourceLocation SIEGE_ENGINEER = icon("siege_engineer");
    public static final ResourceLocation GOVERNOR = icon("governor");
    public static final ResourceLocation VILLAGER_NOBLE = icon("villager_noble");
    public static final ResourceLocation ASSASSIN = icon("assassin");
    public static final ResourceLocation ASSASSIN_LEADER = icon("assassin_leader");

    /** Icons of addon units (the workers), checked before the Recruits types, in the order they were registered. */
    private static final List<Extra> EXTRA = new CopyOnWriteArrayList<>();

    private record Extra(Class<?> type, ResourceLocation entityId, ResourceLocation icon) {
    }

    private UnitIcons() {
    }

    /**
     * Registers the icon of an addon unit, e.g. in FMLClientSetupEvent. Register the most specific class first.
     * @param entityId entity type id, for lists that only know the id (the units of a Governor module)
     * @param icon 21x21 texture
     */
    public static void register(Class<? extends AbstractRecruitEntity> type, ResourceLocation entityId, ResourceLocation icon) {
        EXTRA.add(new Extra(type, entityId, icon));
    }

    /** Icon by entity type id: an addon icon, else the Recruits type, else the recruit icon. */
    public static ResourceLocation ofEntityId(@Nullable ResourceLocation entityId) {
        if (entityId != null) {
            for (Extra extra : EXTRA) if (extra.entityId().equals(entityId)) return extra.icon();
            if (entityId.getNamespace().equals(Main.MOD_ID)) {
                ResourceLocation known = switch (entityId.getPath()) {
                    case "recruit_shieldman" -> SHIELDMAN;
                    case "bowman" -> BOWMAN;
                    case "crossbowman" -> CROSSBOWMAN;
                    case "horseman" -> HORSEMAN;
                    case "nomad" -> NOMAD;
                    case "messenger" -> MESSENGER;
                    case "scout" -> SCOUT;
                    case "captain" -> CAPTAIN;
                    case "patrol_leader" -> PATROL_LEADER;
                    case "siege_engineer" -> SIEGE_ENGINEER;
                    case "governor" -> GOVERNOR;
                    case "villager_noble" -> VILLAGER_NOBLE;
                    case "assassin" -> ASSASSIN;
                    case "assassin_leader" -> ASSASSIN_LEADER;
                    default -> null;
                };
                if (known != null) return known;
            }
        }
        return RECRUIT;
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation(Main.MOD_ID, "textures/gui/image/unit/" + name + ".png");
    }

    /** Icon of the recruit's type; the most specific class first (companions extend the unit classes). */
    @Nullable
    public static ResourceLocation of(@Nullable AbstractRecruitEntity recruit) {
        if (recruit == null) return null;
        for (Extra extra : EXTRA) if (extra.type().isInstance(recruit)) return extra.icon();
        if (recruit instanceof GovernorEntity) return GOVERNOR;
        if (recruit instanceof VillagerNobleEntity) return VILLAGER_NOBLE;
        if (recruit instanceof MessengerEntity) return MESSENGER;
        if (recruit instanceof ScoutEntity) return SCOUT;
        if (recruit instanceof CaptainEntity) return CAPTAIN;
        if (recruit instanceof CommanderEntity) return PATROL_LEADER;
        if (recruit instanceof SiegeEngineerEntity) return SIEGE_ENGINEER;
        if (recruit instanceof NomadEntity) return NOMAD;
        if (recruit instanceof HorsemanEntity) return HORSEMAN;
        if (recruit instanceof CrossBowmanEntity) return CROSSBOWMAN;
        if (recruit instanceof BowmanEntity) return BOWMAN;
        if (recruit instanceof RecruitShieldmanEntity) return SHIELDMAN;
        return RECRUIT;
    }

    public static ResourceLocation of(GovernorUnitType type) {
        return switch (type) {
            case RECRUIT -> RECRUIT;
            case SHIELDMAN -> SHIELDMAN;
            case HORSEMAN -> HORSEMAN;
            case BOWMAN -> BOWMAN;
            case CROSSBOWMAN -> CROSSBOWMAN;
            case NOMAD -> NOMAD;
            case SIEGE_ENGINEER -> SIEGE_ENGINEER;
        };
    }

    /** Icon of a profession of the promote screen (same ids as MessagePromoteRecruit). */
    @Nullable
    public static ResourceLocation ofProfession(int profession) {
        return switch (profession) {
            case 0 -> MESSENGER;
            case 1 -> SCOUT;
            case 2 -> SIEGE_ENGINEER;
            case 3 -> CAPTAIN;
            case 4 -> PATROL_LEADER;
            case 5 -> ASSASSIN;
            case 6 -> GOVERNOR;
            case 7 -> SCOUT;
            case 8 -> ASSASSIN_LEADER;
            default -> null;
        };
    }

    public static void render(GuiGraphics guiGraphics, @Nullable ResourceLocation icon, int x, int y) {
        render(guiGraphics, icon, x, y, SIZE);
    }

    public static void render(GuiGraphics guiGraphics, @Nullable ResourceLocation icon, int x, int y, int size) {
        if (icon == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(icon, x, y, size, size, 0, 0, SIZE, SIZE, SIZE, SIZE);
        RenderSystem.disableBlend();
    }
}
