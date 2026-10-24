package com.talhanation.recruits.client.gui.governor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.network.MessageGovernorSetClaim;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

/**
 * List of the player's claims (same look as the player selection). Choosing a claim appoints the
 * Governor there; claims with another Governor in office cannot be chosen.
 */
@OnlyIn(Dist.CLIENT)
public class SelectClaimScreen extends ListScreenBase implements IClaimSelection {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/select_player.png");
    private static final Component TITLE = Component.translatable("gui.recruits.governor.claim_list.title");
    private static final Component BUTTON_APPOINT = Component.translatable("gui.recruits.governor.claim_list.appoint");
    private static final Component TOOLTIP_APPOINT = Component.translatable("gui.recruits.governor.claim_list.tooltip_appoint");
    private static final Component TOOLTIP_TAKEN = Component.translatable("gui.recruits.governor.claim_list.tooltip_taken");
    private static final Component BUTTON_BACK = Component.translatable("gui.recruits.button.back");
    private static final Component EMPTY = Component.translatable("gui.recruits.governor.claim_list.empty");
    private static final int HEADER_SIZE = 16;
    private static final int FOOTER_SIZE = 32;
    private static final int SEARCH_HEIGHT = 16;
    private static final int UNIT_SIZE = 18;
    private static final int CELL_HEIGHT = 36;

    private final GovernorScreen parent;
    private final Player player;
    private final UUID governorId;
    private GovernorSnapshot snapshot;

    private ClaimsList claimList;
    private EditBox searchBox;
    private String lastSearch;
    private int units;
    @Nullable
    private RecruitsClaim selected;
    private Button appointButton;

    public SelectClaimScreen(GovernorScreen parent, Player player, UUID governorId, GovernorSnapshot snapshot) {
        super(TITLE, 236, 0);
        this.parent = parent;
        this.player = player;
        this.governorId = governorId;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        super.init();

        int gapTop = (int) (this.height * 0.1);
        int gapBottom = (int) (this.height * 0.1);
        guiLeft = guiLeft + 2;
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT + SEARCH_HEIGHT + 4) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom - SEARCH_HEIGHT) / UNIT_SIZE);
        ySize = HEADER_SIZE + units * UNIT_SIZE + FOOTER_SIZE;

        if (claimList != null) {
            claimList.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            claimList = new ClaimsList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT, this, player);
        }

        String search = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, guiLeft + 8, guiTop + HEADER_SIZE, 220, SEARCH_HEIGHT, Component.literal(""));
        searchBox.setMaxLength(32);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(search);
        searchBox.setResponder(this::checkSearchStringUpdate);
        addWidget(searchBox);
        addWidget(claimList);
        setInitialFocus(searchBox);

        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;
        appointButton = new ExtendedButton(guiLeft + 7, buttonY, 100, 20, BUTTON_APPOINT, button -> {
            if (selected == null) return;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetClaim(governorId, selected.getUUID()));
            minecraft.setScreen(parent);
        });
        Button backButton = new ExtendedButton(guiLeft + 129, buttonY, 100, 20, BUTTON_BACK, button -> minecraft.setScreen(parent));
        updateAppointButton();
        addRenderableWidget(appointButton);
        addRenderableWidget(backButton);
    }

    /** New data from the server while the list is open. */
    public void update(GovernorSnapshot snapshot) {
        this.snapshot = snapshot;
        if (claimList != null) claimList.updateEntryList();
        updateAppointButton();
    }

    public GovernorScreen getParentScreen() {
        return parent;
    }

    private void updateAppointButton() {
        if (appointButton == null) return;
        ClaimStatus status = selected == null ? null : getStatus(selected);
        appointButton.active = status == ClaimStatus.FREE;
        appointButton.setTooltip(Tooltip.create(status == ClaimStatus.TAKEN ? TOOLTIP_TAKEN : TOOLTIP_APPOINT));
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) searchBox.tick();
    }

    private void checkSearchStringUpdate(String string) {
        if (!(string = string.toLowerCase(Locale.ROOT)).equals(lastSearch)) {
            claimList.setFilter(string);
            lastSearch = string;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (claimList != null) claimList.mouseClicked(x, y, button);
        boolean flag = super.mouseClicked(x, y, button);
        if (claimList != null && claimList.getFocused() != null) {
            selected = claimList.getFocused().getClaim();
            updateAppointButton();
        }
        return flag;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, HEADER_SIZE);
        for (int i = 0; i < units; i++) {
            guiGraphics.blit(TEXTURE, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * i, 0, HEADER_SIZE, xSize, UNIT_SIZE);
        }
        guiGraphics.blit(TEXTURE, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * units, 0, HEADER_SIZE + UNIT_SIZE, xSize, FOOTER_SIZE);
        guiGraphics.blit(TEXTURE, guiLeft + 10, guiTop + HEADER_SIZE + 6 - 2, xSize, 0, 12, 12);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, TITLE, width / 2 - font.width(TITLE) / 2, guiTop + 5, 4210752, false);
        if (!claimList.isEmpty()) {
            claimList.render(guiGraphics, mouseX, mouseY, delta);
        } else {
            guiGraphics.drawCenteredString(font, EMPTY, width / 2, guiTop + HEADER_SIZE + (units * UNIT_SIZE) / 2 - font.lineHeight / 2, -1);
        }
        searchBox.render(guiGraphics, mouseX, mouseY, delta);
    }

    // ------------------------------------------------------------------ IClaimSelection

    @Nullable
    @Override
    public RecruitsClaim getSelected() {
        return selected;
    }

    @Override
    public ListScreenListBase<RecruitsClaimEntry> getClaimList() {
        return claimList;
    }

    @Override
    public ClaimStatus getStatus(RecruitsClaim claim) {
        GovernorSnapshot.OwnClaim own = snapshot.ownClaims.get(claim.getUUID());
        if (own == null || own.governor() == null) return ClaimStatus.FREE;
        return governorId.equals(own.governor()) ? ClaimStatus.CURRENT : ClaimStatus.TAKEN;
    }

    @Override
    public String getGovernorName(RecruitsClaim claim) {
        GovernorSnapshot.OwnClaim own = snapshot.ownClaims.get(claim.getUUID());
        return own == null ? "" : own.governorName();
    }
}
