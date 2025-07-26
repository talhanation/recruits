package com.talhanation.recruits.client.gui.claim;

import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.widgets.ChunkMapWidget;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClaimMapScreen extends RecruitsScreenBase {

    private static final Component TITLE = Component.translatable("gui.recruits.claim_map.title");

    private final Screen parent;
    public static RecruitsTeam ownFaction;
    private final Player player;
    private ChunkMapWidget mapWidget;
    public ClaimMapScreen(Screen parent, Player player, RecruitsTeam ownFaction) {
        super(TITLE, 195,160);
        this.ownFaction = ownFaction;
        this.parent = parent;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        mapWidget = new ChunkMapWidget(guiLeft - 100, guiTop - ySize, 10);
        mapWidget.setWidth(300);
        mapWidget.setHeight(300);
        this.addRenderableWidget(mapWidget);
        mapWidget.updateCenter();
    }

    @Override
    public boolean mouseClicked(double p_94695_, double p_94696_, int p_94697_) {
        if(mapWidget != null) mapWidget.mouseClicked(p_94695_, p_94696_, p_94697_);
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
}
