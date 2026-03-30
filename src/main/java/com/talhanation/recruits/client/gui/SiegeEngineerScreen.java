package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.entities.IHasTargetPriority;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.network.MessageSetTargetPrio;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SiegeEngineerScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.more_screen.title");
    private static final MutableComponent TEXT_PRIO_CLOSEST_ENEMY = Component.translatable("gui.recruits.inv.text.prioClosestEnemy");
    private static final MutableComponent TOOLTIP_PRIO_CLOSEST_ENEMY = Component.translatable("gui.recruits.inv.tooltip.prioClosestEnemy");
    private static final MutableComponent TEXT_PRIO_INFANTRY = Component.translatable("gui.recruits.inv.text.prioInfantry");
    private static final MutableComponent TOOLTIP_PRIO_INFANTRY = Component.translatable("gui.recruits.inv.tooltip.prioInfantry");
    private static final MutableComponent TEXT_PRIO_CAVALRY = Component.translatable("gui.recruits.inv.text.prioCavalry");
    private static final MutableComponent TOOLTIP_PRIO_CAVALRY = Component.translatable("gui.recruits.inv.tooltip.prioCavalry");
    private static final MutableComponent TEXT_PRIO_SIEGE_WEAPONS = Component.translatable("gui.recruits.inv.text.prioSiegeWeapons");
    private static final MutableComponent TOOLTIP_PRIO_SIEGE_WEAPONS = Component.translatable("gui.recruits.inv.tooltip.prioSiegeWeapons");
    private static final MutableComponent TEXT_PRIO_SHIPS = Component.translatable("gui.recruits.inv.text.prioShips");
    private static final MutableComponent TOOLTIP_PRIO_SHIPS = Component.translatable("gui.recruits.inv.tooltip.prioShips");
    private static final MutableComponent TEXT_SELECT_TARGET_PRIO = Component.translatable("gui.recruits.inv.text.selectTargetPrio");
    private final Player player;
    private final SiegeEngineerEntity siegeEngineer;
    private RecruitsCheckBox checkBoxClosestEnemy;
    private RecruitsCheckBox checkBoxInfantry;
    private RecruitsCheckBox checkBoxCavalry;
    private RecruitsCheckBox checkBoxSiegeWeapons;
    private RecruitsCheckBox checkBoxShips;
    private BlackShowingTextField titleTextField;
    public int prioIndex;

    public SiegeEngineerScreen(SiegeEngineerEntity siegeEngineer, Player player) {
        super(TITLE, 195,160);
        this.player = player;
        this.siegeEngineer = siegeEngineer;
    }

    @Override
    protected void init() {
        super.init();
        this.prioIndex = siegeEngineer.getTargetPriority();
        setButtons();
    }

    private void setButtons(){
        clearWidgets();
        int y = guiTop  - 27;
        int x = guiLeft + 32;

        titleTextField = new BlackShowingTextField(x, y, 130, 20, TEXT_SELECT_TARGET_PRIO);
        addRenderableWidget(titleTextField);

        y += 25;
        checkBoxClosestEnemy = new RecruitsCheckBox(x, y, 130, 20, TEXT_PRIO_CLOSEST_ENEMY, this.isSelected(IHasTargetPriority.TargetPriority.CLOSEST),
        (bool) -> {
                this.prioIndex = IHasTargetPriority.TargetPriority.CLOSEST.getIndex();
                sendUpdate();
            }
        );
        checkBoxClosestEnemy.setTooltip(Tooltip.create(TOOLTIP_PRIO_CLOSEST_ENEMY));
        addRenderableWidget(checkBoxClosestEnemy);


        y += 20;
        checkBoxInfantry = new RecruitsCheckBox(x, y, 130, 20, TEXT_PRIO_INFANTRY, this.isSelected(IHasTargetPriority.TargetPriority.INFANTRY),
                (bool) -> {
                    this.prioIndex = IHasTargetPriority.TargetPriority.INFANTRY.getIndex();
                    sendUpdate();
                }
        );
        checkBoxInfantry.setTooltip(Tooltip.create(TOOLTIP_PRIO_INFANTRY));
        addRenderableWidget(checkBoxInfantry);

        y += 20;
        checkBoxCavalry = new RecruitsCheckBox(x, y, 130, 20, TEXT_PRIO_CAVALRY, this.isSelected(IHasTargetPriority.TargetPriority.CAVALRY),
                (bool) -> {
                    this.prioIndex = IHasTargetPriority.TargetPriority.CAVALRY.getIndex();
                    sendUpdate();
                }
        );
        checkBoxCavalry.setTooltip(Tooltip.create(TEXT_PRIO_CAVALRY));
        addRenderableWidget(checkBoxCavalry);

        y += 20;
        checkBoxSiegeWeapons = new RecruitsCheckBox(x, y, 130, 20, TEXT_PRIO_SIEGE_WEAPONS, this.isSelected(IHasTargetPriority.TargetPriority.SIEGE_WEAPONS),
                (bool) -> {
                    this.prioIndex = IHasTargetPriority.TargetPriority.SIEGE_WEAPONS.getIndex();
                    sendUpdate();
                }
        );
        checkBoxSiegeWeapons.setTooltip(Tooltip.create(TOOLTIP_PRIO_SIEGE_WEAPONS));
        addRenderableWidget(checkBoxSiegeWeapons);

        y += 20;
        checkBoxShips = new RecruitsCheckBox(x, y, 130, 20, TEXT_PRIO_SHIPS, this.isSelected(IHasTargetPriority.TargetPriority.SHIPS),
                (bool) -> {
                    this.prioIndex = IHasTargetPriority.TargetPriority.SHIPS.getIndex();
                    sendUpdate();
                }
        );
        checkBoxShips.setTooltip(Tooltip.create(TOOLTIP_PRIO_SHIPS));
        addRenderableWidget(checkBoxShips);
    }

    private boolean isSelected(IHasTargetPriority.TargetPriority prio){
        return this.prioIndex == prio.getIndex();
    }

    private void sendUpdate() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageSetTargetPrio(siegeEngineer.getUUID(), this.prioIndex));
        setButtons();
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
    }
}
