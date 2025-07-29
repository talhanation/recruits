package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.client.gui.team.TeamInspectionScreen;
import com.talhanation.recruits.client.gui.team.TeamMainScreen;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.client.gui.widgets.SelectedPlayerWidget;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class ClaimEditScreen extends RecruitsScreenBase {
    private static final Component TITLE = Component.translatable("gui.recruits.claim_edit.title");
    private static final Component BUTTON_SAVE = Component.translatable("gui.recruits.button.save");
    protected static final Component BUTTON_BACK = Component.translatable("gui.recruits.button.back");
    protected static final Component CHECKBOX_ALLOW_BREAKING = Component.translatable("gui.recruits.checkbox.allowBlockBreaking");
    protected static final Component CHECKBOX_ALLOW_PLACING = Component.translatable("gui.recruits.checkbox.allowBlockPlacing");
    protected static final Component CHECKBOX_ALLOW_INTERACTING = Component.translatable("gui.recruits.checkbox.allowBlockInteracting");
    private final RecruitsClaim claim;
    private final Player player;

    private EditBox editNameBox;
    private boolean allowBlockBreaking;
    private boolean allowBlockPlacing;
    private boolean allowBlockInteracting;
    private RecruitsCheckBox blockBreakingCheckBox;
    private RecruitsCheckBox blockPlacingCheckBox;
    private RecruitsCheckBox blockInteractionCheckBox;
    private SelectedPlayerWidget selectedPlayerWidget;
    private RecruitsPlayerInfo playerInfo;
    private Button saveButton;
    private Button backButton;
    private String claimName;
    private String savedName;
    private ClaimMapScreen parent;
    public ClaimEditScreen(ClaimMapScreen screen, RecruitsClaim claim, Player player) {
        super(TITLE, 1,1);
        this.parent = screen;
        this.claim = claim;
        this.player = player;
        this.playerInfo = claim.getPlayerInfo();
    }

    public int x;
    public int y;
    @Override
    protected void init() {
        clearWidgets();
        if(savedName == null) savedName = claim.getName();
        x = this.width / 2;
        y = this.height / 2;

        setWidgets();
    }

    private void setWidgets(){
        editNameBox = new EditBox(font, x - 70, y - 110, 140, 20, Component.literal(""));
        editNameBox.setTextColor(-1);
        editNameBox.setTextColorUneditable(-1);
        editNameBox.setBordered(true);
        editNameBox.setMaxLength(32);
        editNameBox.setValue(savedName);
        editNameBox.setResponder(this::onTextInput);
        this.addRenderableWidget(editNameBox);
        if(playerInfo != null){
            selectedPlayerWidget = new SelectedPlayerWidget(font, x - 70, y - 87, 140, 20, Component.literal("x"),
                    () -> {
                        playerInfo = null;
                        this.selectedPlayerWidget.setPlayer(null, null);

                        this.setWidgets();
                    }
            );
            selectedPlayerWidget.setPlayer(claim.playerInfo.getUUID(), claim.playerInfo.getName());
            this.addRenderableWidget(selectedPlayerWidget);
        }
        else{
            Button selectPlayerButton = addRenderableWidget(new ExtendedButton(x - 70, y - 87, 140, 20, SelectPlayerScreen.TITLE,
                button -> {
                    minecraft.setScreen(new SelectPlayerScreen(this, player, SelectPlayerScreen.TITLE, SelectPlayerScreen.BUTTON_SELECT, SelectPlayerScreen.BUTTON_SELECT_TOOLTIP, false, PlayersList.FilterType.NONE,
                        (playerInfo) -> {
                            this.playerInfo = playerInfo;
                            minecraft.setScreen(this);
                            setWidgets();
                        }
                    ));
                }
            ));
            this.addRenderableWidget(selectPlayerButton);
        }

        int checkBoxWidth = 140;
        int checkBoxHeight = 20;

        this.blockBreakingCheckBox = new RecruitsCheckBox(x - 70, y + 20, checkBoxWidth, checkBoxHeight, CHECKBOX_ALLOW_BREAKING,
                this.allowBlockBreaking,
                (bool) -> {
                    this.allowBlockBreaking = bool;
                }
        );
        this.addRenderableWidget(blockBreakingCheckBox);

        this.blockPlacingCheckBox = new RecruitsCheckBox(x - 70, y + 40, checkBoxWidth, checkBoxHeight, CHECKBOX_ALLOW_PLACING,
                this.allowBlockPlacing,
                (bool) -> {
                    this.allowBlockPlacing = bool;
                }
        );
        this.addRenderableWidget(blockPlacingCheckBox);

        this.blockInteractionCheckBox = new RecruitsCheckBox(x - 70, y + 60, checkBoxWidth, checkBoxHeight, CHECKBOX_ALLOW_INTERACTING,
                this.allowBlockInteracting,
                (bool) -> {
                    this.allowBlockInteracting = bool;
                }
        );
        addRenderableWidget(blockInteractionCheckBox);

        backButton = new ExtendedButton(x + 5, y + 90, 70, 20, BUTTON_BACK,
                button -> {
                    this.minecraft.setScreen(this.parent);
                });
        addRenderableWidget(backButton);

        saveButton = new ExtendedButton(x - 75, y + 90, 70, 20, BUTTON_SAVE,
                button -> {
                    this.claim.setName(editNameBox.getValue());
                    this.claim.setPlayer(playerInfo);
                    this.claim.setBlockInteractionAllowed(this.allowBlockInteracting);
                    this.claim.setBlockPlacementAllowed(this.allowBlockPlacing);
                    this.claim.setBlockBreakingAllowed(this.allowBlockBreaking);

                    //Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(selectedClaim));
                });
        addRenderableWidget(saveButton);
        this.checkSaveActive();
    }

    public void checkSaveActive(){
        this.saveButton.active = playerInfo != null;
    }

    private void onTextInput(String string) {
        this.savedName = string;
    }
    int panelWidth = 150;
    int panelHeight = 200;
    int panelX = -75;
    int panelY = -115;
    int claimMiniX = -70;
    int claimMiniY = -60;

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.fill(panelX - 1 + x, panelY - 1 + y, panelX + x  + panelWidth + 1, panelY + y + panelHeight + 1, 0xFF555555);
        guiGraphics.fill(panelX + x, panelY + y, panelX + x + panelWidth, panelY + y + panelHeight, 0xFF222222);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderForeground(guiGraphics, mouseX, mouseY, delta);

        renderClaimMiniMapAreaFramed(guiGraphics, claimMiniX + x, claimMiniY + y , 140, 70, this.claim);
    }

    private void renderClaimMiniMapAreaFramed(GuiGraphics guiGraphics, int x, int y, int width, int height, RecruitsClaim claim) {
        List<ChunkPos> chunks = claim.getClaimedChunks();
        if (chunks.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Chunk-Grenzen bestimmen
        int minX = chunks.stream().mapToInt(c -> c.x).min().orElse(0);
        int maxX = chunks.stream().mapToInt(c -> c.x).max().orElse(0);
        int minZ = chunks.stream().mapToInt(c -> c.z).min().orElse(0);
        int maxZ = chunks.stream().mapToInt(c -> c.z).max().orElse(0);

        int chunkWidth = maxX - minX + 1;
        int chunkHeight = maxZ - minZ + 1;

        // Zelle pro Chunk berechnen (Skalierung)
        float scaleX = (float) width / chunkWidth;
        float scaleZ = (float) height / chunkHeight;
        float cellSize = Math.min(scaleX, scaleZ);

        // Offset zur Zentrierung berechnen
        float usedWidth = chunkWidth * cellSize;
        float usedHeight = chunkHeight * cellSize;
        float offsetX = x + (width - usedWidth) / 2f;
        float offsetY = y + (height - usedHeight) / 2f;

        // Scissor aktivieren
        int scale = (int) mc.getWindow().getGuiScale();
        int screenHeight = mc.getWindow().getHeight();
        RenderSystem.enableScissor(x * scale, screenHeight - (y + height) * scale, width * scale, height * scale);

        // Farbwerte vorbereiten
        int alpha = 190;
        int rgb = TeamEditScreen.unitColors.get(claim.getOwnerFaction().getUnitColor()).getRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        // Zeichnen
        for (ChunkPos pos : chunks) {
            float px = offsetX + (pos.x - minX) * cellSize;
            float py = offsetY + (pos.z - minZ) * cellSize;

            guiGraphics.fill((int) px, (int) py, (int) (px + cellSize), (int) (py + cellSize), argb);

            // Ränder (Grenzen)
            ChunkPos[] dirs = {
                    new ChunkPos(pos.x, pos.z - 1),
                    new ChunkPos(pos.x, pos.z + 1),
                    new ChunkPos(pos.x - 1, pos.z),
                    new ChunkPos(pos.x + 1, pos.z)
            };
            boolean top = !claim.containsChunk(dirs[0]);
            boolean bottom = !claim.containsChunk(dirs[1]);
            boolean left = !claim.containsChunk(dirs[2]);
            boolean right = !claim.containsChunk(dirs[3]);

            int borderColor = 0xFFFFFFFF;
            if (top)    guiGraphics.fill((int) px, (int) py, (int) (px + cellSize), (int) py + 1, borderColor);
            if (bottom) guiGraphics.fill((int) px, (int) (py + cellSize - 1), (int) (px + cellSize), (int) (py + cellSize), borderColor);
            if (left)   guiGraphics.fill((int) px, (int) py, (int) px + 1, (int) (py + cellSize), borderColor);
            if (right)  guiGraphics.fill((int) (px + cellSize - 1), (int) py, (int) (px + cellSize), (int) (py + cellSize), borderColor);
        }

        // Claim-Name über Zentrum
        ChunkPos center = claim.getCenter();
        float cx = offsetX + (center.x - minX + 0.5f) * cellSize;
        float cz = offsetY + (center.z - minZ + 0.5f) * cellSize;

        //int textWidth = font.width(claim.getName());
        //guiGraphics.drawString(font, claim.getName(), (int) (cx - textWidth / 2f), (int) (cz - 6), 0xFFFFFF, false);

        RenderSystem.disableScissor();
    }
}

