package com.talhanation.recruits;

import com.talhanation.recruits.entities.AssassinEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.spawner.WorldEntitySpawner;

public class AssassinEvents {

    public static void createAssassin(String playerName, int count, World world) {
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        PlayerEntity target = list.getPlayerByName(playerName);
        BlockPos blockPos;

        if (target != null) {
            blockPos = calculateSpawnPos(target);

            while (!hasEnoughSpace(world, blockPos)){
                blockPos = calculateSpawnPos(target);
            }

            if (hasEnoughSpace(world, blockPos)){
                world.setBlock(blockPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                for (int i = 0; i < count; i++) {
                    AssassinEntity assassin = ModEntityTypes.ASSASSIN.get().create(target.level);
                    assassin.setPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
                    assassin.setEquipment();
                    assassin.setTame(false);
                    assassin.setIsInOrder(true);
                    assassin.setDropEquipment();
                    assassin.setPersistenceRequired();
                    assassin.setCanPickUpLoot(true);
                    assassin.setTarget(target);
                    target.level.addFreshEntity(assassin);
                }
            }
        }
    }

    private static BlockPos calculateSpawnPos(PlayerEntity target){
        BlockPos blockPos = null;


        for(int i = 0; i < 10; ++i) {
            int d0 = (int) (target.getX() + (target.level.random.nextInt(16) + 32));
            int d2 = (int) (target.getZ() + (target.level.random.nextInt(16) + 32));
            int d1 = target.level.getHeight(Heightmap.Type.WORLD_SURFACE, d0, d2);

            System.out.println("DEBUG: x = " + d0);
            System.out.println("DEBUG: y = " + d1);
            System.out.println("DEBUG: z = " + d2);
            BlockPos blockpos1 = new BlockPos(d0, d1, d2);

            if (WorldEntitySpawner.isSpawnPositionOk(EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, target.level, blockpos1, ModEntityTypes.ASSASSIN.get())) {
                blockPos = blockpos1;
                break;
            }
        }
        return blockPos;
    }

    private static boolean hasEnoughSpace(IBlockReader reader, BlockPos pos) {
        for(BlockPos blockpos : BlockPos.betweenClosed(pos, pos.offset(1, 2, 1))) {
            if (!reader.getBlockState(blockpos).getCollisionShape(reader, blockpos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public static void doPayment(PlayerEntity player, int costs){
        PlayerInventory playerInv = player.inventory;
        int playerEmeralds = 0;

        //checkPlayerMoney
        playerEmeralds = playerGetEmeraldsInInventory(player);
        player.sendMessage(new StringTextComponent("PlayerEmeralds: " + playerEmeralds), player.getUUID());
        player.sendMessage(new StringTextComponent("Costs: " + costs), player.getUUID());
        playerEmeralds = playerEmeralds - costs;

        //remove Player Emeralds
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == Items.EMERALD){
                playerInv.removeItemNoUpdate(i);
            }
        }

        //add Player Emeralds what is left
        ItemStack emeraldsLeft = Items.EMERALD.getDefaultInstance();

        emeraldsLeft.setCount(playerEmeralds);
        playerInv.add(emeraldsLeft);
    }


    public static int playerGetEmeraldsInInventory(PlayerEntity player) {
        int emeralds = 0;
        PlayerInventory playerInv = player.inventory;
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == Items.EMERALD){
                emeralds = emeralds + itemStackInSlot.getCount();
            }
        }
        return emeralds;
    }
}
