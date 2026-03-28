package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractLeaderEntity.EnemyAction;
import com.talhanation.recruits.entities.AbstractLeaderEntity.InfoMode;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.ArrayList;
import java.util.List;

public class PatrolLeaderScreen extends RecruitsScreenBase {
    private static final MutableComponent BTN_START    = Component.translatable("gui.recruits.inv.text.start");
    private static final MutableComponent BTN_STOP     = Component.translatable("gui.recruits.inv.text.stop");
    private static final MutableComponent BTN_PAUSE    = Component.translatable("gui.recruits.inv.text.pause");
    private static final MutableComponent BTN_RESUME   = Component.translatable("gui.recruits.inv.text.resume");
    private static final MutableComponent TT_START     = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_start");
    private static final MutableComponent TT_STOP      = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_stop");
    private static final MutableComponent TT_PAUSE     = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_pause");
    private static final MutableComponent TT_RESUME    = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_resume");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/blank_gui.png");
    private static final int TEXTURE_W = 195;
    private static final int TEXTURE_H = 160;
    private static final int FONT_COLOR = 4210752;
    private final Player player;
    private final AbstractLeaderEntity leaderEntity;
    private int leftPos;
    private int topPos;
    private AbstractLeaderEntity.State patrolState;
    private InfoMode infoMode;
    private EnemyAction enemyAction;
    private RecruitsRoute selectedRoute;
    private RecruitsGroup selectedGroup;
    private ScrollDropDownMenu<RecruitsRoute>  routeDropDown;
    private ScrollDropDownMenu<RecruitsGroup>  groupDropDown;

    public PatrolLeaderScreen(AbstractLeaderEntity leaderEntity, Player player) {
        super(Component.literal(""), 197,250);
        this.player  = player;
        this.leaderEntity = leaderEntity;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width  - TEXTURE_W) / 2;
        this.topPos  = (this.height - TEXTURE_H) / 2;

        this.patrolState = AbstractLeaderEntity.State.fromIndex(leaderEntity.getPatrollingState());
        this.infoMode    = InfoMode.fromIndex(leaderEntity.getInfoMode());
        this.enemyAction = EnemyAction.fromIndex(leaderEntity.getEnemyAction());

        // Restore route from leader
        if (leaderEntity.getRouteID() != null) {
            this.selectedRoute = ClientManager.routesMap.get(leaderEntity.getRouteID().toString());
        }

        // Restore group from leader
        if (leaderEntity.getGroup() != null) {
            this.selectedGroup = ClientManager.getGroup(leaderEntity.getGroup());
        }

        ClientManager.loadRoutes();
        buildWidgets();
    }

    // -------------------------------------------------------------------------

    private void buildWidgets() {
        clearWidgets();

        int x     = leftPos + 8;
        int y     = topPos  + 10;
        int btnH  = 18;
        int fullW = TEXTURE_W - 16;

        // --- Route dropdown ---
        List<RecruitsRoute> routeOptions = new ArrayList<>();
        routeOptions.add(null);
        routeOptions.addAll(ClientManager.getRoutesList());

        routeDropDown = new ScrollDropDownMenu<>(
                selectedRoute,
                x, y, fullW, btnH,
                routeOptions,
                r -> r == null ? "-- No Route --" : r.getName(),
                r -> {
                    selectedRoute = r;
                    sendRouteToServer(r);
                    buildWidgets(); // refresh button states after route selection
                }
        );
        addRenderableWidget(routeDropDown);
        y += btnH + 6;

        // --- Start / Stop row ---
        boolean isPatrolling = patrolState == AbstractLeaderEntity.State.PATROLLING;
        boolean isPaused     = patrolState == AbstractLeaderEntity.State.PAUSED;
        boolean canStart     = selectedRoute != null
                && (patrolState == AbstractLeaderEntity.State.STOPPED
                ||  patrolState == AbstractLeaderEntity.State.IDLE
                ||  isPaused);

        int btnW = (fullW - 4) / 2;

        Component startLabel   = isPaused  ? BTN_RESUME : BTN_START;
        Component startTooltip = isPaused  ? TT_RESUME  : TT_START;
        Component stopLabel    = isPatrolling ? BTN_PAUSE  : BTN_STOP;
        Component stopTooltip  = isPatrolling ? TT_PAUSE   : TT_STOP;

        Button startButton = addRenderableWidget(new ExtendedButton(x, y, btnW, btnH, startLabel, btn -> {
            patrolState = AbstractLeaderEntity.State.PATROLLING;
            sendRouteToServer(selectedRoute);
            Main.SIMPLE_CHANNEL.sendToServer(
                    new MessagePatrolLeaderSetPatrolState(leaderEntity.getUUID(), (byte) 1));
            buildWidgets();
        }));
        startButton.active = canStart;
        startButton.setTooltip(Tooltip.create(
                !canStart && selectedRoute == null ? Component.literal("No route selected") : startTooltip));

        Button stopButton = addRenderableWidget(new ExtendedButton(x + btnW + 4, y, btnW, btnH, stopLabel, btn -> {
            patrolState = isPatrolling
                    ? AbstractLeaderEntity.State.PAUSED
                    : AbstractLeaderEntity.State.STOPPED;
            Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetPatrolState(
                    leaderEntity.getUUID(), (byte) patrolState.getIndex()));
            buildWidgets();
        }));
        stopButton.active = patrolState != AbstractLeaderEntity.State.STOPPED
                && patrolState != AbstractLeaderEntity.State.IDLE;
        stopButton.setTooltip(Tooltip.create(stopTooltip));
        y += btnH + 8;

        // --- Report ---
        String infoLabel = "Report: " + switch (infoMode) {
            case ALL     -> "All";
            case HOSTILE -> "Hostiles";
            case ENEMY   -> "Enemies";
            case NONE    -> "None";
        };
        addRenderableWidget(new ExtendedButton(x, y, fullW, btnH,
                Component.literal(infoLabel), btn -> {
                    infoMode = infoMode.getNext();
                    Main.SIMPLE_CHANNEL.sendToServer(
                            new MessagePatrolLeaderSetInfoMode(leaderEntity.getUUID(), infoMode.getIndex()));
                    buildWidgets();
                }));
        y += btnH + 4;

        // --- On Enemy ---
        String actionLabel = "On Enemy: " + switch (enemyAction) {
            case CHARGE          -> "Charge";
            case HOLD            -> "Hold";
            case KEEP_PATROLLING -> "Keep Patrolling";
        };
        addRenderableWidget(new ExtendedButton(x, y, fullW, btnH,
                Component.literal(actionLabel), btn -> {
                    enemyAction = enemyAction.getNext();
                    Main.SIMPLE_CHANNEL.sendToServer(
                            new MessagePatrolLeaderSetEnemyAction(leaderEntity.getUUID(), enemyAction.getIndex()));
                    buildWidgets();
                }));
        y += btnH + 8;

        // --- Group dropdown ---
        List<RecruitsGroup> groupOptions = new ArrayList<>();
        groupOptions.add(null);
        groupOptions.addAll(ClientManager.groups);

        groupDropDown = new ScrollDropDownMenu<>(
                selectedGroup,
                x, y, fullW, btnH,
                groupOptions,
                g -> g == null ? "-- No Group --" : g.getName(),
                g -> {
                    RecruitsGroup previous = selectedGroup;
                    selectedGroup = g;

                    if (g != null) {
                        // Set group on leader then assign recruits automatically
                        Main.SIMPLE_CHANNEL.sendToServer(
                                new MessageSetLeaderGroup(leaderEntity.getUUID(), g.getUUID()));
                        Main.SIMPLE_CHANNEL.sendToServer(
                                new MessageAssignGroupToCompanion(player.getUUID(), leaderEntity.getUUID()));
                        g.leaderUUID = leaderEntity.getUUID();
                    } else if (previous != null) {
                        // Deselected — unassign recruits
                        Main.SIMPLE_CHANNEL.sendToServer(
                                new MessageRemoveAssignedGroupFromCompanion(player.getUUID(), leaderEntity.getUUID()));
                        previous.leaderUUID = null;
                    }

                    buildWidgets();
                }
        );
        addRenderableWidget(groupDropDown);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (routeDropDown != null) routeDropDown.onMouseClick(mouseX, mouseY);
        if (groupDropDown != null) groupDropDown.onMouseClick(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (routeDropDown != null) routeDropDown.onMouseMove(mouseX, mouseY);
        if (groupDropDown != null) groupDropDown.onMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    private void sendRouteToServer(RecruitsRoute route) {
        List<net.minecraft.core.BlockPos> positions = new ArrayList<>();
        List<Integer> waits = new ArrayList<>();
        if (route != null) {
            for (RecruitsRoute.Waypoint wp : route.getWaypoints()) {
                positions.add(wp.getPosition());
                int sec = 0;
                if (wp.getAction() != null
                        && wp.getAction().getType() == RecruitsRoute.WaypointAction.Type.WAIT) {
                    sec = wp.getAction().getWaitSeconds();
                }
                waits.add(sec);
            }
        }
        Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetRoute(
                leaderEntity.getUUID(),
                route != null ? route.getId() : null,
                positions,
                waits));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }
    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int fontColor = 4210752;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
