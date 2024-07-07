package com.talhanation.recruits.inventory;

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

public class TeamCreationContainer extends ContainerBase {

    private final Container container;
    private final Player player;

    public TeamCreationContainer(int id, Inventory playerInventory) {
        this(id, new SimpleContainer(1), playerInventory);
    }

    public TeamCreationContainer(int id, Container container, Inventory playerInventory) {
        super(ModScreens.TEAM_CREATION_TYPE.get(), id, playerInventory, container);
        checkContainerSize(container, 1);
        this.container = container;
        this.player = playerInventory.player;
        addBannerSlot();
        addPlayerInventorySlots();
    }

    public void removed(Player p_39881_) {
        super.removed(p_39881_);
        player.getInventory().add(getBanner());
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public void addBannerSlot() {
        this.addSlot(new Slot(container, 0,80,28) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                Item item = itemStack.getItem();
                return  item instanceof BannerItem;
            }
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void set(@NotNull ItemStack stack){
                super.set(stack);
            }
        });
    }

    public ItemStack getBanner() {
        return container.getItem(0);
    }
}
