package com.talhanation.recruits.inventory;

import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeamEditMenu extends ContainerBase {

    private final Container container;
    private final Player player;
    @Nullable
    private TeamEditScreen screen;

    public TeamEditMenu(int id, Inventory playerInventory) {
        this(id, new SimpleContainer(1), playerInventory);
    }

    public TeamEditMenu(int id, Container container, Inventory playerInventory) {
        super(ModScreens.TEAM_EDIT_TYPE.get(), id, playerInventory, container);
        checkContainerSize(container, 1);
        this.container = container;
        this.player = playerInventory.player;
        addBannerSlot();
        addPlayerInventorySlots();
    }

    public void setScreen(TeamEditScreen screen) {
        this.screen = screen;
    }

    public void removed(Player p_39881_) {
        super.removed(p_39881_);
        player.getInventory().add(getBanner());
    }

    @Override
    public int getInvOffset() {
        return 76;
    }

    public int getInvXOffset() {
        return 23;
    }

    public void addBannerSlot() {
        this.addSlot(new Slot(container, 0,17,100) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                Item item = itemStack.getItem();
                return item instanceof BannerItem;
            }
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void set(@NotNull ItemStack stack){
                super.set(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if(screen != null) screen.onBannerPlaced();
            }
        });
    }

    public ItemStack getBanner() {
        return container.getItem(0);
    }

    @Override
    protected void addPlayerInventorySlots() {
        if (this.playerInventory != null) {
            int k;
            for (k = 0; k < 3; ++k) {
                for (int j = 0; j < 9; ++j) {
                    this.addSlot(new Slot(this.playerInventory, j + k * 9 + 9, 8 + j * 18 + this.getInvXOffset(), 84 + k * 18 + this.getInvOffset()) {
                        @Override
                        public void setChanged() {
                            super.setChanged();
                            if (screen != null) {
                                screen.onPlayerInventoryChanged();
                            }
                        }
                    });
                }
            }

            for (k = 0; k < 9; ++k) {
                this.addSlot(new Slot(this.playerInventory, k, 8 + k * 18 + this.getInvXOffset(), 142 + this.getInvOffset()) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if (screen != null) {
                            screen.onPlayerInventoryChanged();
                        }
                    }
                });
            }
        }
    }
}
