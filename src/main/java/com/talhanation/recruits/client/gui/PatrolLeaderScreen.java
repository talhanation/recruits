package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.inventory.PatrolLeaderContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.util.ArrayList;
import java.util.List;

public class PatrolLeaderScreen extends ScreenBase<PatrolLeaderContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/waypoint_list_gui.png");
    private final Player player;
    private final AbstractLeaderEntity recruit;
    private int page = 1;
    public static List<BlockPos> waypoints = new ArrayList<>();
    public static List<ItemStack> waypointItems = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private boolean cycle;
    private AbstractLeaderEntity.State state;
    private AbstractLeaderEntity.InfoMode infoMode;

    private static final MutableComponent TOOLTIP_START = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_start");
    private static final MutableComponent TOOLTIP_STOP = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_stop");
    private static final MutableComponent TOOLTIP_PAUSE = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_pause");
    private static final MutableComponent TOOLTIP_RESUME = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_resume");

    private static final MutableComponent BUTTON_START = new TranslatableComponent("gui.recruits.inv.text.start");
    private static final MutableComponent BUTTON_STOP = new TranslatableComponent("gui.recruits.inv.text.stop");
    private static final MutableComponent BUTTON_PAUSE = new TranslatableComponent("gui.recruits.inv.text.pause");
    private static final MutableComponent BUTTON_RESUME = new TranslatableComponent("gui.recruits.inv.text.resume");


    private static final MutableComponent TOOLTIP_CYCLE = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_cycle");
    private static final MutableComponent TOOLTIP_LINE = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_line");

    private static final MutableComponent BUTTON_CYCLE = new TranslatableComponent("gui.recruits.inv.text.cycle");
    private static final MutableComponent BUTTON_LINE = new TranslatableComponent("gui.recruits.inv.text.line");

    private static final MutableComponent INFO_MODE_ALL = new TranslatableComponent("gui.recruits.inv.text.infomode.all");
    private static final MutableComponent INFO_MODE_HOSTILE = new TranslatableComponent("gui.recruits.inv.text.infomode.hostiles");
    private static final MutableComponent INFO_MODE_ENEMY = new TranslatableComponent("gui.recruits.inv.text.infomode.enemies");
    private static final MutableComponent INFO_MODE_NONE = new TranslatableComponent("gui.recruits.inv.text.infomode.none");
    private static final MutableComponent TOOLTIP_ADD = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_add");
    private static final MutableComponent TOOLTIP_REMOVE = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_remove");
    private static final MutableComponent TOOLTIP_INFO_MODE = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader_info_mode");

    private static final MutableComponent BUTTON_ASSIGN_RECRUITS = new TranslatableComponent("gui.recruits.inv.text.assign_recruits");
    private static final MutableComponent TOOLTIP_ASSIGN_RECRUITS = new TranslatableComponent("gui.recruits.inv.tooltip.assign_recruits");

    private static final int fontColor = 4210752;
    private ForgeSlider waitSlider;

    public PatrolLeaderScreen(PatrolLeaderContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        this.imageWidth = 211;
        this.imageHeight = 250;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.cycle = recruit.getCycle();
        this.state = AbstractLeaderEntity.State.fromIndex(recruit.getPatrollingState());
        this.infoMode = AbstractLeaderEntity.InfoMode.fromIndex(recruit.getInfoMode());
        this.setButtons();
    }

    private void setButtons(){
        this.clearWidgets();

        this.setPageButtons();
        this.setWaypointButtons();
        this.setWaitTimeSlider();

        this.setAssignButton();

        Component startString;
        Component startToolTip;
        if (state != AbstractLeaderEntity.State.PAUSED) {// 2 = paused
            startString = BUTTON_START;
            startToolTip = TOOLTIP_START;
        } else {
            startString = BUTTON_RESUME;
            startToolTip = TOOLTIP_RESUME;
        }


        Component stopString;
        Component stopToolTip;
        if (state != AbstractLeaderEntity.State.STARTED) { // 1 = patrolling/started
            stopString = BUTTON_STOP;
            stopToolTip = TOOLTIP_STOP;
        } else {
            stopString = BUTTON_PAUSE;
            stopToolTip = TOOLTIP_PAUSE;
        }

        Component cycleString = cycle ? BUTTON_CYCLE : BUTTON_LINE;
        Component cycleToolTip = cycle ? TOOLTIP_CYCLE : TOOLTIP_LINE;
        this.setCycleButton(cycleString, cycleToolTip);

        this.setStartButtons(startString, startToolTip);
        this.setStopButtons(state, stopString, stopToolTip);


        Component infoModeString = null;

        switch (infoMode){
            case ALL -> infoModeString = INFO_MODE_ALL;
            case HOSTILE -> infoModeString = INFO_MODE_HOSTILE;
            case ENEMY -> infoModeString = INFO_MODE_ENEMY;
            case NONE -> infoModeString = INFO_MODE_NONE;
        }
        this.setInfoButton(infoModeString);

    }

    private void setInfoButton(Component infoModeString) {
        Button infoButton = addRenderableWidget(new Button(leftPos + 230, topPos + 62, 50, 20, infoModeString, button -> {
            this.infoMode = this.infoMode.getNext();
            Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetInfoMode(recruit.getUUID(), this.infoMode.getIndex()));
            this.setButtons();
        },
        (button1, poseStack, i, i1) -> {
            this.renderTooltip(poseStack, TOOLTIP_INFO_MODE, i, i1);
        }));
    }

    private void setAssignButton() {
        Button assignButton = addRenderableWidget(new Button(leftPos + 230, topPos + 32, 50, 20, BUTTON_ASSIGN_RECRUITS, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignGroupToCompanion(player.getUUID(), this.recruit.getUUID()));
            onClose();
        },
            (button1, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_ASSIGN_RECRUITS, i, i1);
            }));
    }

    private void setWaitTimeSlider() {
        int minValue = 0;
        int maxValue = 30;
        int step = 0;
        Component prefix = new TextComponent("");
        Component suffix = new TextComponent(" min");

        this.waitSlider = new ForgeSlider(this.leftPos + 16,this.topPos + 36, 179, 20, prefix,  suffix,  minValue, maxValue, recruit.getWaitTimeInMin(),step, 0, true);
        addRenderableWidget(waitSlider);
    }

    public void setCycleButton(Component cycle, Component tooltip) {
        Button startButton = addRenderableWidget(new Button(leftPos + 105, topPos + 11, 40, 20, cycle,
                button -> {
                    this.cycle = !this.cycle;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetCycle(this.recruit.getUUID(), this.cycle));

                    this.setButtons();
                },
                (button, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, tooltip, i, i1);
                }
        ));
        startButton.active = state == AbstractLeaderEntity.State.STOPPED || state == AbstractLeaderEntity.State.IDLE;
    }

    public void setStartButtons(Component start, Component tooltip) {
        Button startButton = addRenderableWidget(new Button(leftPos + 19, topPos + 11, 40, 20, start,
                button -> {
                    this.state = AbstractLeaderEntity.State.STARTED;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetPatrolState(this.recruit.getUUID(), (byte) 1));

                    this.setButtons();
                },
                (button, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, tooltip, i, i1);
                }
        ));
        startButton.active = state != AbstractLeaderEntity.State.STARTED;

    }

    public void setStopButtons(AbstractLeaderEntity.State currentState, Component stop, Component tooltip) {
        Button startButton = addRenderableWidget(new Button(leftPos + 62, topPos + 11, 40, 20, stop,
                button -> {
                    if (currentState != AbstractLeaderEntity.State.STARTED) {
                        this.state = AbstractLeaderEntity.State.STOPPED;
                    } else {
                        this.state = AbstractLeaderEntity.State.PAUSED;
                    }

                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetPatrolState(this.recruit.getUUID(), (byte) state.getIndex()));
                    this.setButtons();
                },
                (button, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, tooltip, i, i1);
                }
        ));
        startButton.active = state != AbstractLeaderEntity.State.STOPPED && state != AbstractLeaderEntity.State.IDLE;
    }

    @Override
    public boolean mouseReleased(double p_97812_, double p_97813_, int p_97814_) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetWaitTime(this.recruit.getUUID(),  this.waitSlider.getValueInt()));

        return super.mouseReleased(p_97812_, p_97813_, p_97814_);
    }

    @Override
    public boolean mouseDragged(double p_97752_, double p_97753_, int p_97754_, double p_97755_, double p_97756_) {
        if(waitSlider.isHoveredOrFocused() && waitSlider.mouseClicked(p_97752_, p_97753_, p_97754_))
            waitSlider.mouseDragged(p_97752_, p_97753_, p_97754_, p_97755_, p_97756_);
        return super.mouseDragged(p_97752_, p_97753_, p_97754_, p_97755_, p_97756_);
    }

    public void setPageButtons() {
        Button pageForwardButton = createPageForwardButton();
        pageForwardButton.active = waypoints.size() > 9;

        Button pageBackButton = createPageBackButton();
        pageBackButton.active = page != 1;
    }

    public void setWaypointButtons() {
        Button addButton = createAddWaypointButton(this.leftPos + 171, this.topPos + 11);
        Button removeButton = createRemoveWaypointButton(this.leftPos + 148, this.topPos + 11);
        addButton.active = state == AbstractLeaderEntity.State.STOPPED || state == AbstractLeaderEntity.State.IDLE || state == AbstractLeaderEntity.State.PAUSED;
        removeButton.active = state == AbstractLeaderEntity.State.STOPPED || state == AbstractLeaderEntity.State.IDLE;
    }

    private void renderItemAt(ItemStack itemStack, int x, int y) {
        if(itemStack != null) itemRenderer.renderAndDecorateItem(itemStack, x, y);
    }

    public Button createPageBackButton() {
        return addRenderableWidget(new Button(leftPos + 15, topPos + 230, 12, 12, new TextComponent("<"),
                button -> {
                    if(this.page > 1) page--;
                    this.setButtons();
                }
        ));
    }

    public Button createPageForwardButton() {
        return addRenderableWidget(new Button(leftPos + 184, topPos + 230, 12, 12, new TextComponent(">"),
                button -> {
                    page++;
                    this.setButtons();
                }
        ));
    }

    private Button createAddWaypointButton(int x, int y){
        return addRenderableWidget(new Button(x, y, 20, 20, new TextComponent("+"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderAddWayPoint(recruit.getUUID()));
                    this.setButtons();
                },
                (button, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_ADD, i, i1);
                }
        ));
    }

    private Button createRemoveWaypointButton(int x, int y){
        return addRenderableWidget(new Button(x, y, 20, 20, new TextComponent("-"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderRemoveWayPoint(recruit.getUUID()));
                    this.setButtons();
                },
                (button, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_REMOVE, i, i1);
                }
        ));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        // Info
        int fontColor = 4210752;
        int waypointsPerPage = 10;

        int startIndex = (page - 1) * waypointsPerPage;
        int endIndex = Math.min(startIndex + waypointsPerPage, waypoints.size());

        if (!waypoints.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                BlockPos pos = waypoints.get(i);

                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();

                String coordinates = String.format("%d:  (%d,  %d,  %d)", i + 1, x, y, z);

                if(!waypointItems.isEmpty() && waypointItems.get(i) != null) renderItemAt(waypointItems.get(i), 15, 58 + ((i - startIndex) * 17)); // Adjust the Y position here
                else{
                    BlockPos pos1 =  waypoints.get(i);
                    ItemStack itemStack = recruit.getItemStackToRender(pos1);

                    renderItemAt(itemStack, 15, 58 + ((i - startIndex) * 17));
                }
                font.draw(matrixStack, coordinates, 35, 60 + ((i - startIndex) * 17), fontColor);
            }

            if (waypoints.size() > waypointsPerPage)
                font.draw(matrixStack, "Page: " + page, 90, 230, fontColor);
        }

    }


}
