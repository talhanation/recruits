package com.talhanation.recruits.inventory;

import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class RecruitInventoryMenu extends ContainerBase {
    private static final int PLAYER_SLOT_START = 0;
    private static final int PLAYER_SLOT_END = 36;
    private static final int OFFHAND_SLOT = 36;
    private static final int MAINHAND_SLOT = 37;
    private static final int ARMOR_SLOT_START = 38;
    private static final int ARMOR_SLOT_END = 42;
    private static final int RECRUIT_INVENTORY_SLOT_START = 42;

    private final Container recruitInventory;
    private final AbstractRecruitEntity recruit;
    private static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    };
    public static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.MAINHAND
    };

    public RecruitInventoryMenu(int id, AbstractRecruitEntity recruit, Inventory playerInventory) {
        super(ModScreens.RECRUIT_CONTAINER_TYPE.get(), id, playerInventory, recruit.getInventory());
        this.recruit = recruit;
        this.recruitInventory = recruit.getInventory();

        addPlayerInventorySlots();

        addRecruitHandSlots();
        addRecruitEquipmentSlots();
        addRecruitInventorySlots();
    }

    public AbstractRecruitEntity getRecruit() {
        return recruit;
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    //iv slots
    //0 = head
    //1 = chest
    //2 = legs
    //3 = boots
    //4 = offhand
    //5 = mainhand
    //6+ -> inventory

    public void addRecruitHandSlots() {
        this.addSlot(new Slot(recruit.inventory, 4,44,90) {
            @Override
            public boolean mayPlace(ItemStack stack){
                return !recruit.isUsingItem() && stack.getItem() instanceof ShieldItem;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !recruit.isUsingItem();
            }

            @Override
            public void set(ItemStack stack){
                super.set(stack);
                recruit.setItemSlot(EquipmentSlot.OFFHAND, stack);
                recruit.onItemStackAdded(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                recruit.onInventoryChanged();
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon () {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        this.addSlot(new Slot(recruit.inventory, 5,26,90) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return recruit.canHoldItem(itemStack);
            }

            @Override
            public void set(ItemStack stack){
                super.set(stack);
                recruit.setItemSlot(EquipmentSlot.MAINHAND, stack);
                recruit.onItemStackAdded(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                recruit.onInventoryChanged();
            }
        });
    }
    public void addRecruitEquipmentSlots() {
        for (int slotIndex = 0; slotIndex < 4; ++slotIndex) {
            final EquipmentSlot equipmentslottype = SLOT_IDS[slotIndex];
            this.addSlot(new Slot(recruit.inventory, slotIndex, 8, 18 + slotIndex * 18) {
                public int getMaxStackSize() {
                    return 1;
                }

                public boolean mayPlace(ItemStack itemStack) {
                    return itemStack.canEquip(equipmentslottype, recruit)
                            || (itemStack.getItem() instanceof BannerItem && equipmentslottype.equals(EquipmentSlot.HEAD));
                }

                @Override
                public void set(ItemStack stack){
                    super.set(stack);
                    recruit.setItemSlot(equipmentslottype, stack);
                    recruit.onItemStackAdded(stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    recruit.onInventoryChanged();
                }

                @OnlyIn(Dist.CLIENT)
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentslottype.getIndex()]);
                }
            });
        }
    }

    public void addRecruitInventorySlots() {
        for (int k = 0; k < 3; ++k) {
            for (int l = 0; l < 3; ++l) {
                this.addSlot(new Slot(recruitInventory, 6 + l + k * recruit.getInventoryColumns(), 2 * 18 + 82 + l * 18,  18 + k * 18){
                     @Override
                     public void set(ItemStack stack){
                         super.set(stack);
                         recruit.onItemStackAdded(stack);
                     }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        recruit.onInventoryChanged();
                    }
                }
                );
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.getSlot(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index >= PLAYER_SLOT_START && index < PLAYER_SLOT_END) {
                boolean moved = moveToFirstEmptySlot(stack, ARMOR_SLOT_START, ARMOR_SLOT_END)
                        || moveToEmptySlot(stack, OFFHAND_SLOT)
                        || (stack.getMaxStackSize() == 1 && moveToEmptySlot(stack, MAINHAND_SLOT));

                if (!moved && !this.moveItemStackTo(stack, RECRUIT_INVENTORY_SLOT_START, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return original;
    }

    private boolean moveToFirstEmptySlot(ItemStack stack, int start, int end) {
        for (int index = start; index < end; index++) {
            if (moveToEmptySlot(stack, index)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveToEmptySlot(ItemStack stack, int index) {
        Slot target = this.getSlot(index);
        return !target.hasItem()
                && target.mayPlace(stack)
                && this.moveItemStackTo(stack, index, index + 1, false);
    }
}
