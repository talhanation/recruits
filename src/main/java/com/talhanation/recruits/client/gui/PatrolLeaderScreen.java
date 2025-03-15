package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import com.talhanation.recruits.inventory.PatrolLeaderContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PatrolLeaderScreen extends ScreenBase<PatrolLeaderContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/waypoint_list_gui.png");
    public static int recruitsSize;
    private EditBox textBoxX;
    private EditBox textBoxY;
    private EditBox textBoxZ;

    private final Player player;
    private final AbstractLeaderEntity recruit;
    private int page = 1;
    public static List<BlockPos> waypoints = new ArrayList<>();
    public static List<ItemStack> waypointItems = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private boolean cycle;
    private boolean fastPatrolling;
    private AbstractLeaderEntity.State state;
    private AbstractLeaderEntity.InfoMode infoMode;
    private static final MutableComponent TOOLTIP_START = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_start");
    private static final MutableComponent TOOLTIP_DISABLED_START = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_start_disabled");
    private static final MutableComponent TOOLTIP_STOP = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_stop");
    private static final MutableComponent TOOLTIP_PAUSE = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_pause");
    private static final MutableComponent TOOLTIP_RESUME = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_resume");

    private static final MutableComponent BUTTON_START = Component.translatable("gui.recruits.inv.text.start");
    private static final MutableComponent BUTTON_STOP = Component.translatable("gui.recruits.inv.text.stop");
    private static final MutableComponent BUTTON_PAUSE = Component.translatable("gui.recruits.inv.text.pause");
    private static final MutableComponent BUTTON_RESUME = Component.translatable("gui.recruits.inv.text.resume");


    private static final MutableComponent TOOLTIP_CYCLE = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_cycle");
    private static final MutableComponent TOOLTIP_LINE = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_line");
    private static final MutableComponent TOOLTIP_FAST_PATROLLING = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_fast");
    private static final MutableComponent BUTTON_CYCLE = Component.translatable("gui.recruits.inv.text.cycle");
    private static final MutableComponent BUTTON_LINE = Component.translatable("gui.recruits.inv.text.line");
    private static final MutableComponent BUTTON_FAST = Component.translatable("gui.recruits.inv.text.fast");
    private static final MutableComponent BUTTON_NORMAL = Component.translatable("gui.recruits.inv.text.normal");
    private static final MutableComponent INFO_MODE_ALL = Component.translatable("gui.recruits.inv.text.infomode.all");
    private static final MutableComponent INFO_MODE_HOSTILE = Component.translatable("gui.recruits.inv.text.infomode.hostiles");
    private static final MutableComponent INFO_MODE_ENEMY = Component.translatable("gui.recruits.inv.text.infomode.enemies");
    private static final MutableComponent INFO_MODE_NONE = Component.translatable("gui.recruits.inv.text.infomode.none");
    private static final MutableComponent TOOLTIP_ADD = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_add");
    private static final MutableComponent TOOLTIP_REMOVE = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_remove");
    private static final MutableComponent TOOLTIP_INFO_MODE = Component.translatable("gui.recruits.inv.tooltip.patrol_leader_info_mode");

    private static final MutableComponent BUTTON_ASSIGN_RECRUITS = Component.translatable("gui.recruits.inv.text.assign_recruits");
    private static final MutableComponent BUTTON_REMOVE_ASSIGNED_RECRUITS = Component.translatable("gui.recruits.inv.text.remove_assigned_recruits");
    private static final MutableComponent TOOLTIP_ASSIGN_RECRUITS = Component.translatable("gui.recruits.inv.tooltip.assign_recruits");
    private static final MutableComponent TOOLTIP_REMOVE_ASSIGNED_RECRUITS = Component.translatable("gui.recruits.inv.tooltip.assign_recruits");
    private static final MutableComponent TOOLTIP_CHEST = Component.translatable("gui.recruits.inv.tooltip.chest");
    private static final int fontColor = 4210752;
    private ForgeSlider waitSlider;
    private final int offset = 88;
    public PatrolLeaderScreen(PatrolLeaderContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.imageWidth = 384;
        this.imageHeight = 256;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = this.offset + (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.cycle = recruit.getCycle();
        this.fastPatrolling = recruit.getFastPatrolling();
        this.state = AbstractLeaderEntity.State.fromIndex(recruit.getPatrollingState());
        this.infoMode = AbstractLeaderEntity.InfoMode.fromIndex(recruit.getInfoMode());
        this.setButtons();
        this.setHoverAreas();
    }


    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, this.texture);
        guiGraphics.blit(this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,this.imageWidth, this.imageHeight);
        this.drawHoverAreas(guiGraphics, mouseX, mouseY);
    }

    private void setHoverAreas() {
        this.hoverAreas = new ArrayList<>();
        Supplier<List<FormattedCharSequence>> supplier = () ->  new ArrayList<>(List.of(FormattedCharSequence.forward(TOOLTIP_CHEST.getString(), Style.EMPTY)));

        HoverArea hoverAreaChest = new HoverArea(this.leftPos + 150, this.topPos + 57, 25, 25, supplier);
        this.hoverAreas.add(hoverAreaChest);
    }

    private void setButtons(){
        this.clearWidgets();

        this.setPageButtons();
        this.setWaypointButtons();
        this.setCoordinatesBoxes();

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
        if (state != AbstractLeaderEntity.State.PATROLLING) { // 1 = patrolling/started
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
        this.setNotificationButton(infoModeString);


        Component fastPatrollingString = fastPatrolling ? BUTTON_FAST : BUTTON_NORMAL;
        this.setFastPatrollingButton(fastPatrollingString);

        setInfoButton();
    }


    private void setNotificationButton(Component infoModeString) {
        Button infoButton = addRenderableWidget(new ExtendedButton(leftPos + 216, topPos + 62, 50, 20, infoModeString, button -> {
            this.infoMode = this.infoMode.getNext();
            Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetInfoMode(recruit.getUUID(), this.infoMode.getIndex()));
            this.setButtons();
        }
        ));
        infoButton.setTooltip(Tooltip.create(TOOLTIP_INFO_MODE));
    }

    private void setInfoButton() {
        Button infoButton = addRenderableWidget(new ExtendedButton(leftPos + 216, topPos + 11, 20, 20, Component.literal("i"), button -> {
            player.sendSystemMessage(Component.translatable("chat.recruits.info.patrol_leader_info"));
            this.onClose();
        }));
    }

    private void setAssignButton() {
        Button assignButton = addRenderableWidget(new ExtendedButton(leftPos + 216, topPos + 140, 107, 20, BUTTON_ASSIGN_RECRUITS, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignGroupToCompanion(player.getUUID(), this.recruit.getUUID()));
            }
        ));
        assignButton.setTooltip(Tooltip.create(TOOLTIP_ASSIGN_RECRUITS));

        Button removeButton = addRenderableWidget(new ExtendedButton(leftPos + 216, topPos + 165, 107, 20, BUTTON_REMOVE_ASSIGNED_RECRUITS, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageRemoveAssignedGroupFromCompanion(player.getUUID(), this.recruit.getUUID()));
        }
        ));

        removeButton.setTooltip(Tooltip.create(TOOLTIP_REMOVE));
    }

    private void setCoordinatesBoxes() {

        int posX = recruit.getOnPos().getX();
        int posY = recruit.getOnPos().getY();
        int posZ = recruit.getOnPos().getZ();

        textBoxX = new EditBox(font, leftPos + 16 + 15, topPos + 37, 43, 18, Component.literal(String.valueOf(posX)));
        textBoxX.setValue(String.valueOf(posX));
        textBoxX.setTextColor(-1);
        textBoxX.setTextColorUneditable(-1);
        textBoxX.setBordered(true);
        textBoxX.setMaxLength(13);

        textBoxY = new EditBox(font, leftPos + 16 + 75, topPos + 37, 43, 18, Component.literal(String.valueOf(posY)));
        textBoxY.setValue(String.valueOf(posY));
        textBoxY.setTextColor(-1);
        textBoxY.setTextColorUneditable(-1);
        textBoxY.setBordered(true);
        textBoxY.setMaxLength(13);

        textBoxZ = new EditBox(font, leftPos + 16 + 135, topPos + 37, 43, 18, Component.literal(String.valueOf(posZ)));
        textBoxZ.setValue(String.valueOf(posZ));
        textBoxZ.setTextColor(-1);
        textBoxZ.setTextColorUneditable(-1);
        textBoxZ.setBordered(true);
        textBoxZ.setMaxLength(13);

        addRenderableWidget(textBoxX);
        addRenderableWidget(textBoxY);
        addRenderableWidget(textBoxZ);
    }
    private void setWaitTimeSlider() {
        int minValue = 0;
        int maxValue = 30;
        int step = 0;
        Component prefix = Component.literal("");
        Component suffix = Component.literal(" min");

        this.waitSlider = new ForgeSlider(this.leftPos + 16,this.topPos + 36, 179, 20, prefix,  suffix,  minValue, maxValue, recruit.getWaitTimeInMin(),step, 0, true);
        addRenderableWidget(waitSlider);
    }

    public void setCycleButton(Component cycle, Component tooltip) {
        Button cycleButton = addRenderableWidget(new ExtendedButton(leftPos + 105, topPos + 11, 40, 20, cycle,
            button -> {
                this.cycle = !this.cycle;
                Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetCycle(this.recruit.getUUID(), this.cycle));
                this.setButtons();
            }
        ));

        cycleButton.setTooltip(Tooltip.create(tooltip));
        if(recruit instanceof CaptainEntity && !(recruit.getVehicle() instanceof Boat)){
            cycleButton.active =  false;
        }
        else
            cycleButton.active = (state == AbstractLeaderEntity.State.STOPPED || state == AbstractLeaderEntity.State.IDLE);
    }

    public void setFastPatrollingButton(Component cycle) {
        Button buttonFastPatrolling = addRenderableWidget(new ExtendedButton(leftPos + 216, topPos + 92, 50, 20, cycle,
                button -> {
                    this.fastPatrolling = !this.fastPatrolling;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetPatrollingSpeed(this.recruit.getUUID(), this.fastPatrolling));

                    this.setButtons();
                }

        ));
        buttonFastPatrolling.active = false;
        buttonFastPatrolling.setTooltip(Tooltip.create(TOOLTIP_FAST_PATROLLING));
    }
    public void setStartButtons(Component start, Component tooltip) {
        Button startButton = addRenderableWidget(new ExtendedButton(leftPos + 19, topPos + 11, 40, 20, start,
                button -> {
                    this.state = AbstractLeaderEntity.State.PATROLLING;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetPatrolState(this.recruit.getUUID(), (byte) 1));
                    this.setButtons();
            }
        ));
        if(recruit instanceof CaptainEntity && !(recruit.getVehicle() instanceof Boat)){
            startButton.active =  false;
        }
        else
            startButton.active = state != AbstractLeaderEntity.State.PATROLLING;

        startButton.setTooltip(Tooltip.create(recruit instanceof CaptainEntity && !(recruit.getVehicle() instanceof Boat) ? TOOLTIP_DISABLED_START : tooltip));
    }

    public void setStopButtons(AbstractLeaderEntity.State currentState, Component stop, Component tooltip) {
        Button startButton = addRenderableWidget(new ExtendedButton(leftPos + 62, topPos + 11, 40, 20, stop,
                button -> {
                    if (currentState != AbstractLeaderEntity.State.PATROLLING) {
                        this.state = AbstractLeaderEntity.State.STOPPED;
                    } else {
                        this.state = AbstractLeaderEntity.State.PAUSED;
                    }

                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetPatrolState(this.recruit.getUUID(), (byte) state.getIndex()));
                    this.setButtons();
                }
        ));
        startButton.setTooltip(Tooltip.create(tooltip));
        startButton.active = state != AbstractLeaderEntity.State.STOPPED && state != AbstractLeaderEntity.State.IDLE;
    }
    protected void containerTick() {
        super.containerTick();
        textBoxX.tick();
        textBoxY.tick();
        textBoxZ.tick();
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        if (this.textBoxX.isFocused()) {
            this.textBoxX.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        if (this.textBoxY.isFocused()) {
            this.textBoxY.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        if (this.textBoxZ.isFocused()) {
            this.textBoxZ.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }
    @Override
    public boolean mouseReleased(double p_97812_, double p_97813_, int p_97814_) {
        //if(this.recruit != null)
            //Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderSetWaitTime(this.recruit.getUUID(),  this.waitSlider.getValueInt()));

        return super.mouseReleased(p_97812_, p_97813_, p_97814_);
    }

    @Override
    public boolean mouseDragged(double p_97752_, double p_97753_, int p_97754_, double p_97755_, double p_97756_) {
        //if(waitSlider.isHoveredOrFocused() && waitSlider.mouseClicked(p_97752_, p_97753_, p_97754_))
        //    waitSlider.mouseDragged(p_97752_, p_97753_, p_97754_, p_97755_, p_97756_);
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

    private void renderItemAt(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if(itemStack != null) guiGraphics.renderFakeItem(itemStack, x, y);
    }

    public Button createPageBackButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 15, topPos + 230, 12, 12, Component.literal("<"),
                button -> {
                    if(this.page > 1) page--;
                    this.setButtons();
                }
        ));
    }

    public Button createPageForwardButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 184, topPos + 230, 12, 12, Component.literal(">"),
                button -> {
                    page++;
                    this.setButtons();
                }
        ));
    }

    private Button createAddWaypointButton(int x, int y){
        Button add = addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("+"),
                button -> {
                    int posX = Integer.parseInt(textBoxX.getValue());
                    int posY = Integer.parseInt(textBoxY.getValue());
                    int posZ = Integer.parseInt(textBoxZ.getValue());

                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderAddWayPoint(recruit.getUUID(), posX, posY, posZ));
                    this.setButtons();
                }
        ));
        add.setTooltip(Tooltip.create(TOOLTIP_ADD));
        return add;
    }

    private Button createRemoveWaypointButton(int x, int y){
        Button remove = addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("-"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePatrolLeaderRemoveWayPoint(recruit.getUUID()));
                    this.setButtons();
                }
        ));
        remove.setTooltip(Tooltip.create(TOOLTIP_REMOVE));
        return remove;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, "Recruit in Oder: " + recruitsSize, offset + 220, 122, fontColor, false);
        guiGraphics.drawString(font, "x: ",  offset + 20, 42, fontColor, false);
        guiGraphics.drawString(font, "y: ",  offset + 80, 42, fontColor, false);
        guiGraphics.drawString(font, "z: ",  offset + 140, 42, fontColor, false);

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

                if(!waypointItems.isEmpty() && waypointItems.get(i) != null) {
                    ItemStack chest = new ItemStack(Blocks.CHEST.asItem());
                    if(i == 0 && this.page == 1){
                        renderItemAt(guiGraphics, chest, offset + 15 + 160, 57);
                    }
                    renderItemAt(guiGraphics, waypointItems.get(i), offset + 15, 57 + ((i - startIndex) * 17));
                }
                else{
                    BlockPos pos1 =  waypoints.get(i);
                    ItemStack itemStack = recruit.getItemStackToRender(pos1);

                    renderItemAt(guiGraphics, itemStack, offset +15, 57 + ((i - startIndex) * 17));
                }
                guiGraphics.drawString(font, coordinates, offset +35, 60 + ((i - startIndex) * 17), fontColor, false);
            }

            if (waypoints.size() > waypointsPerPage)
                guiGraphics.drawString(font, "Page: " + page, offset + 90, 230, fontColor, false);
        }
    }
}
