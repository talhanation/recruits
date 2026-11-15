package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.companion.ICompanionSelection;
import com.talhanation.recruits.client.gui.companion.RecruitsCompanionEntry;
import com.talhanation.recruits.client.gui.companion.RecruitsCompanionList;
import com.talhanation.recruits.client.gui.governor.GovernorScreen;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.entities.ScoutEntity;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.network.MessageOpenRemoteCompanion;
import com.talhanation.recruits.network.MessageRequestCompanionList;
import com.talhanation.recruits.network.MessageSendRemoteOrders;
import com.talhanation.recruits.world.CompanionRecord;
import io.netty.buffer.Unpooled;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The companions of the player and where they are, opened at his Messenger. From here he writes orders for a far
 * companion - in the companion's normal screen - and then sends the Messenger off with them.
 * <p>
 * Built like the faction and group lists ({@link ListScreenBase} with a searchable list).
 * <p>
 * The far companion is not known to the client. Its screen gets a copy built from the entity type and the synced
 * data the server sent ({@link #openOrders}); the copy is in no world. The screen sends its normal packets, and
 * the server writes them down for the Messenger instead of carrying them out ({@code CompanionOrders}). The
 * Governor screen works on a snapshot of his claim instead, which the server keeps as a draft.
 */
@OnlyIn(Dist.CLIENT)
public class CompanionListScreen extends ListScreenBase implements ICompanionSelection {
    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/select_player.png");
    protected static final Component TITLE = Component.translatable("gui.recruits.messenger.companions.title");
    protected static final Component BUTTON_ORDERS = Component.translatable("gui.recruits.messenger.companions.orders");
    protected static final Component BUTTON_SEND = Component.translatable("gui.recruits.messenger.companions.send");
    protected static final Component BUTTON_BACK = Component.translatable("gui.recruits.button.back");
    protected static final Component TOOLTIP_ORDERS = Component.translatable("gui.recruits.messenger.companions.tooltip.orders");
    protected static final Component TOOLTIP_SEND = Component.translatable("gui.recruits.messenger.companions.tooltip.send");
    protected static final Component TEXT_LOADING = Component.translatable("gui.recruits.messenger.companions.loading");
    protected static final Component TEXT_EMPTY = Component.translatable("gui.recruits.messenger.companions.empty");
    protected static final int HEADER_SIZE = 16;
    protected static final int FOOTER_SIZE = 32;
    protected static final int SEARCH_HEIGHT = 16;
    protected static final int UNIT_SIZE = 18;
    protected static final int CELL_HEIGHT = 36;

    protected final MessengerEntity messenger;
    protected final Player player;
    protected RecruitsCompanionList companionList;
    protected EditBox searchBox;
    protected String lastSearch;
    protected int units;
    /** Null until the server answered. */
    @Nullable
    private List<CompanionRecord> companions;
    @Nullable
    private CompanionRecord selected;
    /** The companion the server holds a draft of orders for; the Messenger takes it along when he is sent. */
    @Nullable
    private UUID ordersCompanion;
    private Button ordersButton;
    private Button sendButton;

    public CompanionListScreen(MessengerEntity messenger, Player player) {
        super(TITLE, 236, 0);
        this.messenger = messenger;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        // also runs when the player comes back from a companion's screen: the list is only asked for once
        if (this.companions == null) Main.SIMPLE_CHANNEL.sendToServer(new MessageRequestCompanionList(this.messenger.getUUID()));

        int gapTop = (int) (this.height * 0.1);
        int gapBottom = (int) (this.height * 0.1);
        guiLeft = guiLeft + 2;
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT + SEARCH_HEIGHT + 4) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom - SEARCH_HEIGHT) / UNIT_SIZE);

        if (companionList != null) {
            companionList.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            companionList = new RecruitsCompanionList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT);
        }

        String string = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, guiLeft + 8, guiTop + HEADER_SIZE, 220, SEARCH_HEIGHT, Component.literal(""));
        searchBox.setMaxLength(16);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(string);
        searchBox.setResponder(this::checkSearchStringUpdate);
        addWidget(searchBox);
        addWidget(companionList);

        this.setInitialFocus(searchBox);

        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;
        ordersButton = new ExtendedButton(guiLeft + 7, buttonY, 72, 20, BUTTON_ORDERS,
                button -> {
                    if (selected != null) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenRemoteCompanion(messenger.getUUID(), selected.uuid));
                    }
                });
        ordersButton.setTooltip(Tooltip.create(TOOLTIP_ORDERS));
        addRenderableWidget(ordersButton);

        sendButton = new ExtendedButton(guiLeft + 82, buttonY, 72, 20, BUTTON_SEND,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSendRemoteOrders(messenger.getUUID(), true));
                    ordersCompanion = null;
                    minecraft.setScreen(null);
                });
        sendButton.setTooltip(Tooltip.create(TOOLTIP_SEND));
        addRenderableWidget(sendButton);

        addRenderableWidget(new ExtendedButton(guiLeft + 157, buttonY, 72, 20, BUTTON_BACK, button -> this.onClose()));

        this.updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) {
            searchBox.tick();
        }
    }

    /** Leaving without sending the Messenger: the orders written so far are thrown away. */
    @Override
    public void onClose() {
        if (this.ordersCompanion != null) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageSendRemoteOrders(this.messenger.getUUID(), false));
            this.ordersCompanion = null;
        }
        Minecraft.getInstance().setScreen(new MessengerMainScreen(this.messenger, this.player));
    }

    // ---- From the server ----

    /** The answer to {@link MessageRequestCompanionList}. Companions the Messenger can reach first, the nearest on top. */
    public void setCompanions(List<CompanionRecord> list) {
        List<CompanionRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing((CompanionRecord record) -> !this.sameDimension(record))
                .thenComparingDouble(record -> record.pos.distSqr(this.player.blockPosition())));
        this.companions = sorted;

        List<RecruitsCompanionEntry> entries = new ArrayList<>();
        for (CompanionRecord record : sorted) {
            entries.add(new RecruitsCompanionEntry(this, record, this.title(record), this.info(record), this.canOrder(record)));
        }
        if (this.companionList != null) this.companionList.setEntries(entries);
        this.updateButtons();
    }

    /**
     * The server opened a draft for this companion: his normal screen is shown. Closing that screen leads back
     * here, where the Messenger is sent off.
     * @param synced   the companion's synced entity data, for a copy of him the screen can read
     * @param governor the snapshot of his claim if he is a Governor - his screen shows the claim, not him
     */
    public void openOrders(UUID companion, ResourceLocation type, byte[] synced, @Nullable CompoundTag governor) {
        Screen screen;
        if (governor != null) {
            GovernorSnapshot snapshot = GovernorSnapshot.fromNBT(governor);
            screen = snapshot.self == null ? null : new GovernorScreen(snapshot) {
                @Override
                public void onClose() {
                    Minecraft.getInstance().setScreen(CompanionListScreen.this);
                }
            };
        } else {
            Entity copy = copyOf(companion, type, synced);
            screen = copy == null ? null : this.ordersScreen(copy);
        }
        if (screen == null) return;
        this.ordersCompanion = companion;
        Minecraft.getInstance().setScreen(screen);
    }

    /** An entity of this type with the synced data of the far companion. It is in no world and never ticks. */
    @Nullable
    private static Entity copyOf(UUID uuid, ResourceLocation type, byte[] synced) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(type);
        if (minecraft.level == null || entityType == null) return null;
        Entity entity = entityType.create(minecraft.level);
        if (entity == null) return null;

        entity.setUUID(uuid);
        if (synced.length > 0) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(synced));
            try {
                entity.getEntityData().assignValues(new ClientboundSetEntityDataPacket(buf).packedItems());
            } catch (RuntimeException e) {
                // data of another version: the screen then starts from the defaults, the orders still count
                Main.LOGGER.warn("Recruits: could not read the synced data of companion {}", uuid, e);
            } finally {
                buf.release();
            }
        }
        return entity;
    }

    /** The same screens the "special" button of the inventory opens, but closing them leads back to this list. */
    @Nullable
    private Screen ordersScreen(Entity companion) {
        if (companion instanceof AbstractLeaderEntity leader) {
            return new PatrolLeaderScreen(leader, this.player) {
                @Override
                public void onClose() {
                    Minecraft.getInstance().setScreen(CompanionListScreen.this);
                }
            };
        }
        if (companion instanceof SiegeEngineerEntity siegeEngineer) {
            return new SiegeEngineerScreen(siegeEngineer, this.player) {
                @Override
                public void onClose() {
                    Minecraft.getInstance().setScreen(CompanionListScreen.this);
                }
            };
        }
        if (companion instanceof ScoutEntity scout) {
            return new ScoutScreen(scout, this.player) {
                @Override
                public void onClose() {
                    Minecraft.getInstance().setScreen(CompanionListScreen.this);
                }
            };
        }
        return null;
    }

    // ---- List ----

    private boolean sameDimension(CompanionRecord record) {
        return record.dimension.equals(this.player.level().dimension().location());
    }

    /** Companions with a screen of orders, in the dimension of the player - a Messenger does not change dimensions. */
    private boolean canOrder(CompanionRecord record) {
        if (!this.sameDimension(record)) return false;
        return record.type.equals(ModEntityTypes.PATROL_LEADER.getId())
                || record.type.equals(ModEntityTypes.CAPTAIN.getId())
                || record.type.equals(ModEntityTypes.SIEGE_ENGINEER.getId())
                || record.type.equals(ModEntityTypes.SCOUT.getId())
                || record.type.equals(ModEntityTypes.GOVERNOR.getId());
    }

    private String title(CompanionRecord record) {
        String type = I18n.get(Util.makeDescriptionId("entity", record.type));
        return record.name.isEmpty() || record.name.equals(type) ? type : record.name + " (" + type + ")";
    }

    private String info(CompanionRecord record) {
        String where = this.sameDimension(record)
                ? I18n.get("gui.recruits.messenger.companions.distance", (int) Math.sqrt(record.pos.distSqr(this.player.blockPosition())))
                : record.dimension.getPath();
        String key = "gui.recruits.messenger.companions.status." + record.status;
        return !record.status.isEmpty() && I18n.exists(key) ? where + ", " + I18n.get(key) : where;
    }

    private void updateButtons() {
        if (this.ordersButton == null || this.sendButton == null) return;
        this.ordersButton.active = this.selected != null && this.canOrder(this.selected);
        this.sendButton.active = this.ordersCompanion != null;
    }

    private void checkSearchStringUpdate(String string) {
        if (!(string = string.toLowerCase(Locale.ROOT)).equals(lastSearch)) {
            companionList.setFilter(string);
            lastSearch = string;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int z) {
        if (companionList != null) companionList.mouseClicked(x, y, z);
        boolean flag = super.mouseClicked(x, y, z);
        if (this.companionList != null && this.companionList.getFocused() != null) {
            this.selected = this.companionList.getFocused().getCompanion();
            this.updateButtons();
        }
        return flag;
    }

    // ---- Render ----

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
        guiGraphics.drawString(font, this.getTitle(), width / 2 - font.width(TITLE) / 2, guiTop + 5, 4210752, false);

        if (!companionList.isEmpty()) {
            companionList.render(guiGraphics, mouseX, mouseY, delta);
        } else {
            Component text = this.companions == null ? TEXT_LOADING : TEXT_EMPTY;
            guiGraphics.drawCenteredString(font, text, width / 2, guiTop + HEADER_SIZE + (units * UNIT_SIZE) / 2 - font.lineHeight / 2, -1);
        }
        searchBox.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    // ---- ICompanionSelection ----

    @Override
    @Nullable
    public CompanionRecord getSelected() {
        return selected;
    }

    @Override
    public ListScreenListBase<RecruitsCompanionEntry> getCompanionList() {
        return companionList;
    }

    @Override
    @Nullable
    public UUID getOrdersCompanion() {
        return ordersCompanion;
    }
}
