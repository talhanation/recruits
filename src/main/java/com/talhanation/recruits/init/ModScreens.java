package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.*;
import com.talhanation.recruits.client.gui.team.*;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.entities.MessengerEntity;
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
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, Main.MOD_ID);

    public static void registerMenus() {
        registerMenu(RECRUIT_CONTAINER_TYPE.get(), RecruitInventoryScreen::new);
        registerMenu(DEBUG_CONTAINER_TYPE.get(), DebugInvScreen::new);
        registerMenu(COMMAND_CONTAINER_TYPE.get(), CommandScreen::new);
        registerMenu(ASSASSIN_CONTAINER_TYPE.get(), AssassinLeaderScreen::new);
        registerMenu(HIRE_CONTAINER_TYPE.get(), RecruitHireScreen::new);
        registerMenu(TEAM_EDIT_TYPE.get(), TeamEditScreen::new);
        registerMenu(PROMOTE.get(), PromoteScreen::new);
        registerMenu(MESSENGER.get(), MessengerScreen::new);
        registerMenu(MESSENGER_ANSWER.get(), MessengerAnswerScreen::new);
        registerMenu(PATROL_LEADER.get(), PatrolLeaderScreen::new);

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

    public static final RegistryObject<MenuType<TeamEditMenu>> TEAM_EDIT_TYPE =
            MENU_TYPES.register("team_edit", () -> IForgeMenuType.create((windowId, inv, data) -> {

                return new TeamEditMenu(windowId, inv);
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

    public static final RegistryObject<MenuType<PromoteContainer>> PROMOTE =
            MENU_TYPES.register("promote_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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

    public static final RegistryObject<MenuType<MessengerContainer>> MESSENGER =
            MENU_TYPES.register("messenger_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    return new MessengerContainer(windowId, playerEntity, (MessengerEntity) rec);

                } catch (Exception e) {
                    logger.error("Error in messenger_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final RegistryObject<MenuType<MessengerAnswerContainer>> MESSENGER_ANSWER =
            MENU_TYPES.register("messenger_answer_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
                try {
                    UUID workerId = data.readUUID();
                    Player playerEntity = inv.player;
                    AbstractRecruitEntity rec = getRecruitByUUID(playerEntity, workerId);

                    return new MessengerAnswerContainer(windowId, playerEntity, (MessengerEntity) rec);

                } catch (Exception e) {
                    logger.error("Error in messenger_answer_container: ");
                    logger.error(e.getMessage());
                    logger.error(e.getStackTrace().toString());
                    return null;
                }
            }));

    public static final RegistryObject<MenuType<PatrolLeaderContainer>> PATROL_LEADER =
            MENU_TYPES.register("patrol_leader_container", () -> IForgeMenuType.create((windowId, inv, data) -> {
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