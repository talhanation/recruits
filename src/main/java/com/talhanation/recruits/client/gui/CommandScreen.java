package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.jetbrains.annotations.Nullable;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/command_gui.png");

    private static final MutableComponent TOOLTIP_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.tooltip.strategic_fire");
    private static final MutableComponent TOOLTIP_DISMOUNT = Component.translatable("gui.recruits.command.tooltip.dismount");
    private static final MutableComponent TOOLTIP_MOUNT = Component.translatable("gui.recruits.command.tooltip.mount");
    private static final MutableComponent TOOLTIP_SHIELDS = Component.translatable("gui.recruits.command.tooltip.shields");
    private static final MutableComponent TOOLTIP_PROTECT = Component.translatable("gui.recruits.command.tooltip.protect");
    private static final MutableComponent TOOLTIP_MOVE = Component.translatable("gui.recruits.command.tooltip.move");
    private static final MutableComponent TOOLTIP_FOLLOW = Component.translatable("gui.recruits.command.tooltip.follow");
    private static final MutableComponent TOOLTIP_WANDER = Component.translatable("gui.recruits.command.tooltip.wander");
    private static final MutableComponent TOOLTIP_HOLD_MY_POS = Component.translatable("gui.recruits.command.tooltip.holdMyPos");
    private static final MutableComponent TOOLTIP_HOLD_POS = Component.translatable("gui.recruits.command.tooltip.holdPos");
    private static final MutableComponent TOOLTIP_BACK_TO_POS = Component.translatable("gui.recruits.command.tooltip.backToPos");
	private static final MutableComponent TOOLTIP_BACK_TO_MOUNT = Component.translatable("gui.recruits.command.tooltip.backToMount");
    private static final MutableComponent TOOLTIP_PASSIVE = Component.translatable("gui.recruits.command.tooltip.passive");
    private static final MutableComponent TOOLTIP_NEUTRAL = Component.translatable("gui.recruits.command.tooltip.neutral");
    private static final MutableComponent TOOLTIP_AGGRESSIVE = Component.translatable("gui.recruits.command.tooltip.aggressive");
    private static final MutableComponent TOOLTIP_RAID = Component.translatable("gui.recruits.command.tooltip.raid");
    private static final MutableComponent TOOLTIP_UPKEEP = Component.translatable("gui.recruits.command.tooltip.upkeep");
    private static final MutableComponent TOOLTIP_TEAM = Component.translatable("gui.recruits.command.tooltip.team");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = Component.translatable("gui.recruits.command.tooltip.clearTargets");
    private static final MutableComponent TEXT_EVERYONE = Component.translatable("gui.recruits.command.text.everyone");
    private static final MutableComponent TEXT_PROTECT = Component.translatable("gui.recruits.command.text.protect");
    private static final MutableComponent TEXT_MOVE = Component.translatable("gui.recruits.command.text.move");
    private static final MutableComponent TEXT_SHIELDS = Component.translatable("gui.recruits.command.text.shields");
    private static final MutableComponent TEXT_DISMOUNT = Component.translatable("gui.recruits.command.text.dismount");
    private static final MutableComponent TEXT_MOUNT = Component.translatable("gui.recruits.command.text.mount");
    private static final MutableComponent TEXT_FOLLOW = Component.translatable("gui.recruits.command.text.follow");
    private static final MutableComponent TEXT_WANDER = Component.translatable("gui.recruits.command.text.wander");
    private static final MutableComponent TEXT_HOLD_MY_POS = Component.translatable("gui.recruits.command.text.holdMyPos");
    private static final MutableComponent TEXT_HOLD_POS = Component.translatable("gui.recruits.command.text.holdPos");
    private static final MutableComponent TEXT_BACK_TO_POS = Component.translatable("gui.recruits.command.text.backToPos");
	private static final MutableComponent TEXT_BACK_TO_MOUNT = Component.translatable("gui.recruits.command.text.backToMount");
		
    private static final MutableComponent TEXT_PASSIVE = Component.translatable("gui.recruits.command.text.passive");
    private static final MutableComponent TEXT_NEUTRAL = Component.translatable("gui.recruits.command.text.neutral");
    private static final MutableComponent TEXT_AGGRESSIVE = Component.translatable("gui.recruits.command.text.aggressive");
    private static final MutableComponent TEXT_RAID = Component.translatable("gui.recruits.command.text.raid");
    private static final MutableComponent TEXT_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.text.strategic_fire");
    private static final MutableComponent TEXT_CLEAR_TARGET = Component.translatable("gui.recruits.command.text.clearTargets");
    private static final MutableComponent TEXT_UPKEEP = Component.translatable("gui.recruits.command.text.upkeep");
    private static final MutableComponent TEXT_TEAM = Component.translatable("gui.recruits.command.text.team");
    private static final MutableComponent TEXT_FORMATION_NONE = Component.translatable("gui.recruits.command.text.formation_none");
    private static final MutableComponent TEXT_FORMATION_CUBE = Component.translatable("gui.recruits.command.text.formation_cube");
    private static final MutableComponent TEXT_FORMATION_LINE = Component.translatable("gui.recruits.command.text.formation_cube");
    private static final MutableComponent TOOLTIP_FORMATION = Component.translatable("gui.recruits.command.tooltip.formation");
    private static final int fontColor = 16250871;
    private final Player player;
    private int group;
    public static int recruitsInCommand;
    private boolean shields;
    private boolean strategicFire;
    private BlockPos rayBlockPos;
    private Entity rayEntity;
    private byte formation = 1;
    //private Button formationButton;
    public CommandScreen(CommandMenu commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }

    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x, y, z);
        if(!RecruitsClientConfig.CommandScreenToggle.get())this.onClose();
        return true;
    }
    /*
    Formation setting:
    0 -> none
    1 -> line
    2 -> cube
    */
    @Override
    protected void init() {
        super.init();

        this.rayBlockPos = getBlockPos();
        this.rayEntity = ClientEvent.getEntityByLooking();
        int zeroLeftPos = leftPos + 150;
        int zeroTopPos = topPos + 10;
        int topPosGab = 7;
        int mirror = 240 - 60;
        this.group = getSavedCurrentGroup(player);
        Main.SIMPLE_CHANNEL.sendToServer(new MessageServerUpdateCommandScreen(this.group));
        /*
        //TEAM SCREEN
        addRenderableWidget(new Button(zeroLeftPos - 150, zeroTopPos + (30 + topPosGab), 80, 20, TEXT_TEAM,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageTeamMainScreen(player));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_TEAM, i, i1);
                }
        ));
        */
        /*
        //Formation
        this.formationButton = new Button(zeroLeftPos + 150, zeroTopPos + (30 + topPosGab), 80, 20, TEXT_FORMATION(),
                button -> {


                    CommandEvents.sendFollowCommandInChat(98, player, group);
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_FORMATION, i, i1);
                }
        );
        */
        //addRenderableWidget(this.formationButton);
        //Dismount
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_DISMOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(98, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageDismount(player.getUUID(), group));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_DISMOUNT, i, i1);
                }
        ));

        //Back To Mount
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_BACK_TO_MOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(91, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageBackToMountEntity(player.getUUID(), group));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_BACK_TO_MOUNT, i, i1);
                }
        ));


        //SHIELDS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 5 + 35, 80, 20, TEXT_SHIELDS,
                button -> {
                    this.shields = !getSavedShieldBool(player);

                    if (shields)
                        CommandEvents.sendFollowCommandInChat(95, player, group);
                    else
                        CommandEvents.sendFollowCommandInChat(93, player, group);

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageShields(player.getUUID(), group, shields));

                    saveShieldBool(player);
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_SHIELDS, i, i1);
                }
        ));

        //PASSIVE
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_PASSIVE,
                button -> {
            CommandEvents.sendAggroCommandInChat(3, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 3, group));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_PASSIVE, i, i1);
                }
        ));

        //NEUTRAL
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_NEUTRAL,
                button -> {
                    CommandEvents.sendAggroCommandInChat(0, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 0, group));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_NEUTRAL, i, i1);
                }
        ));

        //AGGRESSIVE
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_AGGRESSIVE,
                button -> {
                    CommandEvents.sendAggroCommandInChat(1, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 1, group));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_AGGRESSIVE, i, i1);
                }
        ));

        //RAID
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_RAID,
                button -> {
                    CommandEvents.sendAggroCommandInChat(2, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 2, group));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_RAID, i, i1);
                }
        ));

        //CLEAR TARGET
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_CLEAR_TARGET,
                button -> {
            //Main.LOGGER.debug("client: clear target");
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(player.getUUID(), group));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_CLEAR_TARGET, i, i1);
                }
        ));


        //WANDER
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_WANDER,
                button -> {
            CommandEvents.sendFollowCommandInChat(0, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 0, group, formation));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_WANDER, i, i1);
                }
        ));


        //FOLLOW
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_FOLLOW,
                button -> {
            CommandEvents.sendFollowCommandInChat(1, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 1, group, formation));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_FOLLOW, i, i1);
                }
        ));


        //HOLD POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_HOLD_POS,
                button -> {
            CommandEvents.sendFollowCommandInChat(2, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 2, group, formation));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_HOLD_POS, i, i1);
                }
        ));


        //BACK TO POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_BACK_TO_POS,
                button -> {
            CommandEvents.sendFollowCommandInChat(3, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 3, group, formation));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_BACK_TO_POS, i, i1);
                }
        ));


        //HOLD MY POS
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_HOLD_MY_POS,
                button -> {
            CommandEvents.sendFollowCommandInChat(4, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 4, group, formation));

                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_HOLD_MY_POS, i, i1);
                }
        ));

        //GROUP
        addRenderableWidget(new ExtendedButton(leftPos - 5 + imageWidth / 2, topPos - 50 + imageHeight / 2, 12, 20, Component.literal("+"),
            button -> {
                this.group = getSavedCurrentGroup(player);
                if (this.group != 9) {
                    this.group++;

                    this.saveCurrentGroup(player);
                }

                Main.SIMPLE_CHANNEL.sendToServer(new MessageServerUpdateCommandScreen(this.group));
            }
        ));

        addRenderableWidget(new ExtendedButton(leftPos - 5 + imageWidth / 2, topPos + 10 + imageHeight / 2, 12, 20, Component.literal("-"),
            button -> {
                this.group = getSavedCurrentGroup(player);
                if (this.group != 0) {
                    this.group--;

                    this.saveCurrentGroup(player);
                }

                Main.SIMPLE_CHANNEL.sendToServer(new MessageServerUpdateCommandScreen(this.group));
            }
        ));

        /// switching Buttons

        //UPKEEP
        Button upkeepButton = addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 5 + 35, 80, 20, TEXT_UPKEEP,
                button -> {
                    CommandEvents.sendFollowCommandInChat(92, player, group);
                    //Entity entity = ClientEvent.getEntityByLooking();
                    //Main.LOGGER.debug("client: entity: " + entity);

                    if (rayEntity != null) {
                        //Main.LOGGER.debug("client: uuid: " + entity.getUUID());
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepEntity(player.getUUID(), rayEntity.getUUID(), group));
                    } else if(rayBlockPos != null)
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepPos(player.getUUID(), group, this.rayBlockPos));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_UPKEEP, i, i1);
                }
        ));
        if(rayEntity != null){
            upkeepButton.active = rayEntity instanceof Container || rayEntity instanceof AbstractChestedHorse horse && horse.hasChest() || rayEntity instanceof InventoryCarrier;
        }
        else if(rayBlockPos != null){
            upkeepButton.active = player.level.getBlockEntity(rayBlockPos) instanceof Container;
        }
        else
            upkeepButton.active = false;

        //PROTECT
        Button protectButton = addRenderableWidget(new Button(zeroLeftPos - mirror - 50, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_PROTECT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(5, player, group);
                    if (rayEntity != null) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageProtectEntity(player.getUUID(), rayEntity.getUUID(), group));
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 5, group, formation));
                    }
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_PROTECT, i, i1);
                }
        ));
        protectButton.active = rayEntity != null;

        //MOVE
        Button moveButton = addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos - (20 + topPosGab), 80, 20, TEXT_MOVE,
                button -> {
                    CommandEvents.sendFollowCommandInChat(97, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 6, group, formation));
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_MOVE, i, i1);
                }
        ));
        moveButton.active = rayBlockPos != null;

        //STRATEGIC FIRE
        Button strategicFireButton = addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos + (20 + topPosGab) * 5 + 35, 80, 20, TEXT_STRATEGIC_FIRE,
                button -> {
                    this.strategicFire = !getSavedStrategicFireBool(player);

                    if (strategicFire)
                        CommandEvents.sendFollowCommandInChat(96, player, group);
                    else
                        CommandEvents.sendFollowCommandInChat(94, player, group);

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageStrategicFire(player.getUUID(), group, strategicFire));

                    saveStrategicFireBool(player);
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_STRATEGIC_FIRE, i, i1);
                }
        ));
        strategicFireButton.active = rayBlockPos != null;

        //Mount
        Button mountButton = addRenderableWidget(new Button(zeroLeftPos + 40 + 10, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_MOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(99, player, group);
                    //Entity entity = ClientEvent.getEntityByLooking();
                    if (rayEntity != null) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageMountEntity(player.getUUID(), rayEntity.getUUID(), group));
                    }
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_MOUNT, i, i1);
                }
        ));
        mountButton.active = rayEntity != null;
    }

    private void updateFormationButton() {
        formation++;
        if(formation > 3) formation = 0;


        if(formation == 0){
            //formationButton.setMessage(TEXT_FORMATION_NONE);
        }
        else if(formation == 1){
            //formationButton.setMessage(TEXT_FORMATION_LINE);
        }
        else if(formation == 2){
            //formationButton.setMessage(TEXT_FORMATION_SQUARE);
        }
        else if(formation == 3){
            //formationButton.setMessage(TEXT_FORMATION_TRIANGLE);
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        //Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitsInCommand(player.getUUID()));

        //this.recCount = getSavedRecruitCount(player);
        this.group = getSavedCurrentGroup(player);
        //player.sendMessage(new StringTextComponent("SCREEN int: " + recCount), player.getUUID());

        int k = 78;//rechst links
        int l = 61;//höhe

        font.draw(matrixStack, "" + handleGroupText(this.group), k, l, fontColor);
        font.draw(matrixStack, "Recruits: " + recruitsInCommand, k , l + 10, fontColor);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }

    public static String handleGroupText(int group) {
        if (group == 0) {
            return TEXT_EVERYONE.getString();
        } else
            return TEXT_GROUP(String.valueOf(group)).getString();
    }

    public int getSavedCurrentGroup(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getInt("CommandingGroup");
    }

    public void saveCurrentGroup(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putInt("CommandingGroup", this.group);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public boolean getSavedShieldBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getBoolean("Shields");
    }

    public void saveShieldBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putBoolean("Shields", this.shields);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public boolean getSavedStrategicFireBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getBoolean("StrategicFire");
    }

    public void saveStrategicFireBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putBoolean("StrategicFire", this.strategicFire);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    private static MutableComponent TEXT_GROUP(String group) {
            return Component.translatable("gui.recruits.command.text.group", group);
    }
    @Nullable
    private BlockPos getBlockPos(){
        HitResult rayTraceResult = player.pick(100, 1F, true);
        if (rayTraceResult != null) {
            if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockraytraceresult = (BlockHitResult) rayTraceResult;

                return blockraytraceresult.getBlockPos();
            }
        }
        return null;
    }
}
