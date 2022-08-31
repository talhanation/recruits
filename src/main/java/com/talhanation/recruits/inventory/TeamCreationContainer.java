package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class TeamCreationContainer extends ContainerBase {

    private final Container container;

    public TeamCreationContainer(int id, Inventory playerInventory) {
        this(id, new SimpleContainer(1), playerInventory);
    }

    public TeamCreationContainer(int id, Container container, Inventory playerInventory) {
        super(Main.TEAM_CREATION_TYPE, id, container, playerInventory);
        checkContainerSize(container, 1);
        this.container = container;
        addBannerSlot();
        addPlayerInventorySlots();
    }

    public void addBannerSlot() {
        this.addSlot(new Slot(container, 0,26,90) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                return  itemStack.equals(Items.WHITE_BANNER.getDefaultInstance());
            }

            @Override
            public void set(@NotNull ItemStack stack){
                super.set(stack);
            }
        });
    }

    public Container getContainer() {
        return container;
    }

    public BannerItem getBanner() {
        return (BannerItem) container.getItem(0).getItem();
    }
}
