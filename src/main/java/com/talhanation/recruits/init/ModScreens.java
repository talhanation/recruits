package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.*;
import com.talhanation.recruits.client.gui.team.*;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.inventory.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;


public class ModScreens {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Main.MOD_ID);

    public static void registerMenus() {
        registerMenu(RECRUIT_CONTAINER_TYPE.get(), RecruitInventoryScreen::new);
        registerMenu(DEBUG_CONTAINER_TYPE.get(), DebugInvScreen::new);
        registerMenu(COMMAND_CONTAINER_TYPE.get(), CommandScreen::new);
        registerMenu(ASSASSIN_CONTAINER_TYPE.get(), AssassinLeaderScreen::new);
        registerMenu(HIRE_CONTAINER_TYPE.get(), RecruitHireScreen::new);
        registerMenu(TEAM_CREATION_TYPE.get(), TeamCreationScreen::new);
        registerMenu(TEAM_MAIN_TYPE.get(), TeamMainScreen::new);
        registerMenu(TEAM_INSPECTION_TYPE.get(), TeamInspectionScreen::new);
        registerMenu(TEAM_LIST_TYPE.get(), TeamListScreen::new);
        registerMenu(TEAM_ADD_PLAYER_TYPE.get(), TeamManagePlayerScreen::new);
        registerMenu(DISBAND.get(), DisbandScreen::new);

        logger.info("MenuScreens registered");
    }


    public static final RegistryObject<MenuType<RecruitInventoryMenu>> RECRUIT_CONTAINER_TYPE =
        MENU_TYPES.register("recruit_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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

    public static final RegistryObject<MenuType<CommandMenu>> COMMAND_CONTAINER_TYPE =
        MENU_TYPES.register("command_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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
    public static final RegistryObject<MenuType<AssassinLeaderMenu>> ASSASSIN_CONTAINER_TYPE =
        MENU_TYPES.register("assassin_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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

    public static final RegistryObject<MenuType<RecruitHireMenu>> HIRE_CONTAINER_TYPE =
            MENU_TYPES.register("hire_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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

    public static final RegistryObject<MenuType<DebugInvMenu>> DEBUG_CONTAINER_TYPE =
            MENU_TYPES.register("debug_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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

    public static final RegistryObject<MenuType<TeamCreationContainer>> TEAM_CREATION_TYPE =
            MENU_TYPES.register("team_creation", () -> IForgeMenuType.create((windowId, inv, data) -> {

                return new TeamCreationContainer(windowId, inv);
            }));


    public static final RegistryObject<MenuType<TeamMainContainer>> TEAM_MAIN_TYPE =
            MENU_TYPES.register("team_main_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                     Player playerEntity = inv.player;
                    if (playerEntity == null) {
                        return null;
                    }
                    return new TeamMainContainer(windowId, playerEntity);

                } catch (Exception e) {
                    logger.error("Error in team_main_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));


    public static final RegistryObject<MenuType<TeamInspectionContainer>> TEAM_INSPECTION_TYPE =
            MENU_TYPES.register("team_inspection_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    Player playerEntity = inv.player;
                    if (playerEntity == null) {
                        return null;
                    }
                    return new TeamInspectionContainer(windowId, playerEntity);

                } catch (Exception e) {
                    logger.error("Error in team_inspection_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));


    public static final RegistryObject<MenuType<TeamListContainer>> TEAM_LIST_TYPE =
            MENU_TYPES.register("team_list_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    Player playerEntity = inv.player;
                    if (playerEntity == null) {
                        return null;
                    }
                    return new TeamListContainer(windowId, playerEntity);

                } catch (Exception e) {
                    logger.error("Error in team_list_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final RegistryObject<MenuType<TeamManagePlayerContainer>> TEAM_ADD_PLAYER_TYPE =
            MENU_TYPES.register("team_add_player_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    Player playerEntity = inv.player;
                    if (playerEntity == null) {
                        return null;
                    }
                    return new TeamManagePlayerContainer(windowId, playerEntity);

                } catch (Exception e) {
                    logger.error("Error in team_add_player_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final RegistryObject<MenuType<DisbandContainer>> DISBAND =
            MENU_TYPES.register("disband_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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

    /**
     * Registers a menuType/container with a screen constructor.
     *
     * It has a try/catch block because the Forge screen constructor fails silently.
     */
    private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenu(MenuType<? extends M> menuType, MenuScreens.ScreenConstructor<M, U> screenConstructor) {
        MenuScreens.register(menuType, (MenuScreens.ScreenConstructor<M, U>) (menu, inventory, title) -> {
            try {
                return screenConstructor.create(menu, inventory, title);
            } catch (Exception e) {
                logger.error("Could not instantiate {}", screenConstructor.getClass().getSimpleName());
                logger.error(e.getMessage());
                logger.error(e.getStackTrace().toString());
                return null;
            }
        });
    }


    @Nullable
    public static AbstractRecruitEntity getRecruitByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AbstractRecruitEntity.class, new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }

    @Nullable
    public static AssassinLeaderEntity getAssassinByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AssassinLeaderEntity.class, new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }
}
