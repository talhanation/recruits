package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.*;
import com.talhanation.recruits.client.gui.faction.*;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.inventory.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;


public class ModScreens {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, Main.MOD_ID);

    public static void registerMenus(RegisterMenuScreensEvent event) {
        registerMenu(event, RECRUIT_CONTAINER_TYPE.get(), RecruitInventoryScreen::new);
        registerMenu(event, DEBUG_CONTAINER_TYPE.get(), DebugInvScreen::new);
        registerMenu(event, COMMAND_CONTAINER_TYPE.get(), CommandScreen::new);
        registerMenu(event, ASSASSIN_CONTAINER_TYPE.get(), AssassinLeaderScreen::new);
        registerMenu(event, HIRE_CONTAINER_TYPE.get(), RecruitHireScreen::new);
        registerMenu(event, TEAM_EDIT_TYPE.get(), FactionEditScreen::new);
        registerMenu(event, PROMOTE.get(), PromoteScreen::new);

        logger.info("MenuScreens registered");
    }

    public static final DeferredHolder<MenuType<?>, MenuType<RecruitInventoryMenu>> RECRUIT_CONTAINER_TYPE =
        MENU_TYPES.register("recruit_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
            try {
                UUID workerId = data.readUUID();
                logger.info("{} is opening hire container for {}", inv.player.getDisplayName().getString(), workerId);

                AbstractRecruitEntity rec = getRecruitByUUID(inv.player, workerId);
                logger.info("Recruit is {}", rec);
                if (rec == null) {
                    return null;
                }
                return new RecruitInventoryMenu(windowId, rec, inv);

            } catch (Exception e) {
                logger.error("Error in recruit_container: ");
                logger.error(e.getMessage());
                logger.error(e.getStackTrace().toString());
                return null;
            }
    }));

    public static final DeferredHolder<MenuType<?>, MenuType<CommandMenu>> COMMAND_CONTAINER_TYPE =
        MENU_TYPES.register("command_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
            try {
                UUID player_uuid = inv.player.getUUID();
                logger.info("{} is opening command container for {}", inv.player.getDisplayName().getString(), player_uuid);

                Player playerEntity = inv.player;
                logger.info("Player is {}", playerEntity);
                if (playerEntity == null) {
                    return null;
                }
                return new CommandMenu(windowId, playerEntity);

            } catch (Exception e) {
                logger.error("Error in command_container: ");
                logger.error(e.getMessage());
                logger.error(e.getStackTrace().toString());
                return null;
            }
    }));
    public static final DeferredHolder<MenuType<?>, MenuType<AssassinLeaderMenu>> ASSASSIN_CONTAINER_TYPE =
        MENU_TYPES.register("assassin_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
            try {
                UUID workerId = data.readUUID();
                AssassinLeaderEntity rec = getAssassinByUUID(inv.player, workerId);
                logger.info("{} is opening assassin container for {}", inv.player.getDisplayName().getString(), workerId);
                if (rec == null) {
                    return null;
                }
                logger.info("Player is {}", rec);
                if (rec == null) {
                    return null;
                }
                return new AssassinLeaderMenu(windowId, rec, inv);

            } catch (Exception e) {
                logger.error("Error in assassin_container: ");
                logger.error(e.getMessage());
                logger.error(e.getStackTrace().toString());
                return null;
            }
    }));

    public static final DeferredHolder<MenuType<?>, MenuType<RecruitHireMenu>> HIRE_CONTAINER_TYPE =
            MENU_TYPES.register("hire_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    logger.info("{} is opening hire container for {}", playerEntity.getDisplayName().getString(), workerId);
                    if (rec == null) {
                        return null;
                    }
                    logger.info("Player is {}", playerEntity);
                    if (playerEntity == null) {
                        return null;
                    }

                    return new RecruitHireMenu(windowId, playerEntity, rec, inv);

                } catch (Exception e) {
                    logger.error("Error in hire_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<DebugInvMenu>> DEBUG_CONTAINER_TYPE =
            MENU_TYPES.register("debug_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    logger.info("{} is opening hire container for {}", playerEntity.getDisplayName().getString(), workerId);
                    if (rec == null) {
                        return null;
                    }
                    logger.info("Recruit is {}", rec);
                    if (playerEntity == null) {
                        return null;
                    }

                    return new DebugInvMenu(windowId, rec, inv);

                } catch (Exception e) {
                    logger.error("Error in debug_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<TeamEditMenu>> TEAM_EDIT_TYPE =
            MENU_TYPES.register("team_edit", () -> IMenuTypeExtension.create((windowId, inv, data) -> {

                return new TeamEditMenu(windowId, inv);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<DisbandContainer>> DISBAND =
            MENU_TYPES.register("disband_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    return new DisbandContainer(windowId, playerEntity, rec.getUUID());

                } catch (Exception e) {
                    logger.error("Error in disband_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<PromoteContainer>> PROMOTE =
            MENU_TYPES.register("promote_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    return new PromoteContainer(windowId, playerEntity, rec);

                } catch (Exception e) {
                    logger.error("Error in promote_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<PatrolLeaderContainer>> PATROL_LEADER =
            MENU_TYPES.register("patrol_leader_container", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    return new PatrolLeaderContainer(windowId, playerEntity, (AbstractLeaderEntity) rec);

                } catch (Exception e) {
                    logger.error("Error in disband_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    /**
     * Registers a menuType/container with a screen constructor.
     *
     * It has a try/catch block because the Forge screen constructor fails silently.
     */
    private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenu(RegisterMenuScreensEvent event, MenuType<? extends M> menuType, MenuScreens.ScreenConstructor<M, U> screenConstructor) {
        event.register(menuType, (MenuScreens.ScreenConstructor<M, U>) (menu, inventory, title) -> {
            try {
                return screenConstructor.create(menu, inventory, title);
            } catch (Exception e) {
                logger.error("Could not instantiate {}", screenConstructor.getClass().getSimpleName());
                logger.error(e.getMessage());
                logger.error(Arrays.toString(e.getStackTrace()));
                return null;
            }
        });
    }


    @Nullable
    public static AbstractRecruitEntity getRecruitByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    new AABB(
                            player.getX() - distance,
                            player.getY() - distance,
                            player.getZ() - distance,
                            player.getX() + distance,
                            player.getY() + distance,
                            player.getZ() + distance),
                    entity -> entity.getUUID().equals(uuid)
            ).stream().findAny().orElse(null);
    }

    @Nullable
    public static AssassinLeaderEntity getAssassinByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.getCommandSenderWorld().getEntitiesOfClass(
                    AssassinLeaderEntity.class,
                    new AABB(
                            player.getX() - distance,
                            player.getY() - distance,
                            player.getZ() - distance,
                            player.getX() + distance,
                            player.getY() + distance,
                            player.getZ() + distance),
                    entity -> entity.getUUID().equals(uuid)
            ).stream().findAny().orElse(null);
    }
}
