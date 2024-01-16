package com.talhanation.recruits.inventory;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModScreens;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MessengerContainer extends ContainerBase {

    private final Player playerEntity;
    private final MessengerEntity recruit;

    public MessengerContainer(int id, Player playerEntity, MessengerEntity messenger) {
        super(ModScreens.MESSENGER.get(), id, playerEntity.getInventory(), messenger.getDeliverSlot());
        this.playerEntity = playerEntity;
        this.recruit = messenger;

        this.addPlayerInventorySlots();

        this.addSlot(new Slot(recruit.getDeliverSlot(), 0, 88, 81) {
            @Override
            public void set(ItemStack stack){
                super.set(stack);
                recruit.getDeliverSlot().setItem(0, stack);
            }
        });
    }
    @Override
    protected void addPlayerInventorySlots() {
        if (this.playerInventory != null) {
            int k;
            for(k = 0; k < 3; ++k) {
                for(int j = 0; j < 9; ++j) {
                    this.addSlot(new Slot(this.playerInventory, j + k * 9 + 9 , 8 + 8 + j * 18, 84 + k * 18 + this.getInvOffset()));
                }
            }

            for(k = 0; k < 9; ++k) {
                this.addSlot(new Slot(this.playerInventory, k, 8 + 8 + k * 18, 142 + this.getInvOffset()));
            }
        }

    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public MessengerEntity getRecruit() {
        return recruit;
    }
}
