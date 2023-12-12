package com.talhanation.recruits;

import java.util.Optional;

public class AssassinEvents {
    /*
    public static void createAssassin(String playerName, int count, Level world) {
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        Player target = list.getPlayerByName(playerName);
        BlockPos blockPos;

        if (target != null) {
            blockPos = calculateSpawnPos(target);

            while (!hasEnoughSpace(world, blockPos)){
                blockPos = calculateSpawnPos(target);
            }
            /*
            if (hasEnoughSpace(world, blockPos)){
                world.setBlock(blockPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                for (int i = 0; i < count; i++) {
                    AssassinEntity assassin = ModEntityTypes.ASSASSIN.get().create(target.level);
                    assassin.setPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
                    assassin.setEquipment();
                    //assassin.setIsOwned(false);
                    assassin.setIsInOrder(true);
                    assassin.setDropEquipment();
                    assassin.setPersistenceRequired();
                    assassin.setCanPickUpLoot(true);
                    assassin.setTarget(target);
                    target.getCommandSenderWorld().addFreshEntity(assassin);
                }
            }
        }

    }

    private static BlockPos calculateSpawnPos(Player target){
        BlockPos blockPos = null;


        for(int i = 0; i < 10; ++i) {
            int d0 = (int) (target.getX() + (target.getCommandSenderWorld().random.nextInt(16) + 32));
            int d2 = (int) (target.getZ() + (target.getCommandSenderWorld().random.nextInt(16) + 32));
            int d1 = target.getCommandSenderWorld().getHeight(Heightmap.Types.WORLD_SURFACE, d0, d2);


            BlockPos blockpos1 = new BlockPos(d0, d1, d2);

            if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, target.level, blockpos1, ModEntityTypes.ASSASSIN.get())) {
                blockPos = blockpos1;
                break;
            }
        }
        return blockPos;
    }

    private static boolean hasEnoughSpace(BlockGetter reader, BlockPos pos) {
        for(BlockPos blockpos : BlockPos.betweenClosed(pos, pos.offset(1, 2, 1))) {
            if (!reader.getBlockState(blockpos).getCollisionShape(reader, blockpos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public static void doPayment(Player player, int costs){
        Inventory playerInv = player.getInventory();
        int playerEmeralds = 0;
        String str = RecruitsModConfig.RecruitCurrency.get();
        ItemStack currencyItemStack;
        Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));

        if (holder.isPresent()){
            currencyItemStack = holder.get().value().getDefaultInstance();
        }
        else
            currencyItemStack = Items.EMERALD.getDefaultInstance();

        //checkPlayerMoney
        playerEmeralds = playerGetEmeraldsInInventory(player);
        //player.sendMessage(new TextComponent("PlayerEmeralds: " + playerEmeralds), player.getUUID());
        //player.sendMessage(new TextComponent("Costs: " + costs), player.getUUID());
        playerEmeralds = playerEmeralds - costs;

        //remove Player Emeralds
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot.equals(currencyItemStack)){
                playerInv.removeItemNoUpdate(i);
            }
        }

        //add Player Emeralds what is left
        ItemStack emeraldsLeft = Items.EMERALD.getDefaultInstance();

        emeraldsLeft.setCount(playerEmeralds);
        playerInv.add(emeraldsLeft);
    }


    public static int playerGetEmeraldsInInventory(Player player) {
        int emeralds = 0;
        Inventory playerInv = player.getInventory();
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == Items.EMERALD){
                emeralds = emeralds + itemStackInSlot.getCount();
            }
        }
        return emeralds;
    }

    public static boolean playerHasEnoughEmeralds(Player player, int price) {
        int emeraldCount = AssassinEvents.playerGetEmeraldsInInventory(player);
        if (emeraldCount >= price){
            return true;
        }
        return player.isCreative();
    }
     */
}
