package com.talhanation.recruits.client.gui.claim;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.diplomacy.DiplomacyEditScreen;
import com.talhanation.recruits.client.gui.widgets.ItemWithLabelWidget;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ClaimMapScreen extends RecruitsScreenBase {

    private static final Component TITLE = Component.translatable("gui.recruits.claim_map.title");
    private final Player player;
    private final Screen parent;

    public static RecruitsTeam ownFaction;
    private ChunkMapWidget mapWidget;
    private ItemWithLabelWidget currencyWidgetClaim;
    private ItemWithLabelWidget currencyWidgetChunk;
    private ItemStack currencyClaim;
    private ItemStack currencyChunk;
    public ClaimMapScreen(Screen parent, Player player, RecruitsTeam ownFaction) {
        super(TITLE, 195,160);
        this.ownFaction = ownFaction;
        this.parent = parent;
        this.player = player;
    }
    public int x;
    public int y;
    @Override
    protected void init() {
        clearWidgets();

        x = this.width / 2;
        y = this.height / 2;

        setButtons();
    }
    @Override
    public void tick() {
        super.tick();
        if (mapWidget != null) {
            mapWidget.tick();
        }
    }

    private void setButtons() {
        // ChunkMapWidget anpassen
        int cellSize = 16;
        int viewRadiusX = (width / cellSize) / 2;
        int viewRadiusY = (height / cellSize) / 2;
        int viewRadius = Math.max(viewRadiusX, viewRadiusY);

        mapWidget = new ChunkMapWidget(this, player, 0, 0, viewRadius, ownFaction);
        mapWidget.setWidth(width);
        mapWidget.setHeight(height);
        this.addRenderableWidget(mapWidget);

        currencyClaim = ClientManager.currencyItemStack.copy();
        currencyChunk = ClientManager.currencyItemStack.copy();
        currencyChunk.setCount(ClientManager.configValueChunkCost);
        currencyClaim.setCount(this.getClaimCost(ownFaction));

        currencyWidgetClaim = new ItemWithLabelWidget(0, 0, 100, 20, currencyClaim, Component.literal("Claim Area"), true, true);
        currencyWidgetChunk = new ItemWithLabelWidget(0, 20, 100, 20, currencyChunk, Component.literal("Claim Chunk"), true, true);

        this.addRenderableWidget(currencyWidgetClaim);
        this.addRenderableWidget(currencyWidgetChunk);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks); // Muss da sein!
    }

    @Override
    public boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {
        if(mapWidget != null) mapWidget.mouseClicked(p_94695_, p_94696_, p_94697_);
        currencyClaim.setCount(this.getClaimCost(ownFaction));
        return super.mouseClicked(p_94695_, p_94696_, p_94697_);
    }

    @Override
    public boolean mouseReleased(double p_94722_, double p_94723_, int p_94724_) {
        if(mapWidget != null) mapWidget.mouseReleased(p_94722_, p_94723_, p_94724_);
        return super.mouseReleased(p_94722_, p_94723_, p_94724_);
    }
    @Override
    public boolean mouseDragged(double p_94699_, double p_94700_, int p_94701_, double p_94702_, double p_94703_) {
        if(mapWidget != null) mapWidget.mouseDragged(p_94699_, p_94700_, p_94701_, p_94702_, p_94703_);
        return super.mouseDragged(p_94699_, p_94700_, p_94701_, p_94702_, p_94703_);
    }

    public void openDiplomacyOf(RecruitsTeam othersFaction) {
        boolean isLeader = player.getUUID().equals(ownFaction.getTeamLeaderUUID());
        minecraft.setScreen(new DiplomacyEditScreen(this, ownFaction, othersFaction, ClientManager.getRelation(ownFaction.getStringID(), ownFaction.getStringID()), ClientManager.getRelation(ownFaction.getStringID(), ownFaction.getStringID()), isLeader));

    }

    public void openClaimEditScreen(RecruitsClaim claim){
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateClaims());

        this.minecraft.setScreen(new ClaimEditScreen(this, claim, player));
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderForeground(guiGraphics, mouseX, mouseY, delta);

    }

    public int getClaimCost(RecruitsTeam ownerTeam) {
        if (!ClientManager.configValueCascadeClaimCost) {
            return ClientManager.configValueClaimCost;
        }

        int amount = 1;

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.getOwnerFaction().getStringID().equals(ownerTeam.getStringID())) {
                amount += 1;
            }
        }

        return amount * ClientManager.configValueClaimCost;
    }

    public boolean canPlayerClaim(int cost, Player player){
        if(!ownFaction.getTeamLeaderUUID().equals(player.getUUID())) return false;

        return cost <= player.getInventory().countItem(ClientManager.currencyItemStack.getItem());
    }
}
