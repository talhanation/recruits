package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/command_gui.png");


    private static final TranslatableComponent TEXT_GROUP = new TranslatableComponent("gui.recruits.command.text.group");
    private static final TranslatableComponent TEXT_EVERYONE = new TranslatableComponent("gui.recruits.command.text.everyone");

    private static final TranslatableComponent TOOLTIP_DISMOUNT = new TranslatableComponent("gui.recruits.command.tooltip.dismount");
    private static final TranslatableComponent TOOLTIP_MOUNT = new TranslatableComponent("gui.recruits.command.tooltip.mount");
    private static final TranslatableComponent TOOLTIP_ESCORT = new TranslatableComponent("gui.recruits.command.tooltip.escort");
    private static final TranslatableComponent TOOLTIP_FOLLOW = new TranslatableComponent("gui.recruits.command.tooltip.follow");
    private static final TranslatableComponent TOOLTIP_WANDER = new TranslatableComponent("gui.recruits.command.tooltip.wander");
    private static final TranslatableComponent TOOLTIP_HOLD_MY_POS = new TranslatableComponent("gui.recruits.command.tooltip.holdMyPos");
    private static final TranslatableComponent TOOLTIP_HOLD_POS = new TranslatableComponent("gui.recruits.command.tooltip.holdPos");
    private static final TranslatableComponent TOOLTIP_BACK_TO_POS = new TranslatableComponent("gui.recruits.command.tooltip.backToPos");
    private static final TranslatableComponent TOOLTIP_PASSIVE = new TranslatableComponent("gui.recruits.command.tooltip.passive");
    private static final TranslatableComponent TOOLTIP_NEUTRAL = new TranslatableComponent("gui.recruits.command.tooltip.neutral");
    private static final TranslatableComponent TOOLTIP_AGGRESSIVE = new TranslatableComponent("gui.recruits.command.tooltip.aggressive");
    private static final TranslatableComponent TOOLTIP_RAID = new TranslatableComponent("gui.recruits.command.tooltip.raid");

    private static final TranslatableComponent TEXT_ESCORT = new TranslatableComponent("gui.recruits.command.text.escort");

    private static final TranslatableComponent TEXT_DISMOUNT = new TranslatableComponent("gui.recruits.command.text.dismount");
    private static final TranslatableComponent TEXT_MOUNT = new TranslatableComponent("gui.recruits.command.text.mount");
    private static final TranslatableComponent TEXT_FOLLOW = new TranslatableComponent("gui.recruits.command.text.follow");
    private static final TranslatableComponent TEXT_WANDER = new TranslatableComponent("gui.recruits.command.text.wander");
    private static final TranslatableComponent TEXT_HOLD_MY_POS = new TranslatableComponent("gui.recruits.command.text.holdMyPos");
    private static final TranslatableComponent TEXT_HOLD_POS = new TranslatableComponent("gui.recruits.command.text.holdPos");
    private static final TranslatableComponent TEXT_BACK_TO_POS = new TranslatableComponent("gui.recruits.command.text.backToPos");

    private static final TranslatableComponent TEXT_PASSIVE = new TranslatableComponent("gui.recruits.command.text.passive");
    private static final TranslatableComponent TEXT_NEUTRAL = new TranslatableComponent("gui.recruits.command.text.neutral");
    private static final TranslatableComponent TEXT_AGGRESSIVE = new TranslatableComponent("gui.recruits.command.text.aggressive");
    private static final TranslatableComponent TEXT_RAID = new TranslatableComponent("gui.recruits.command.text.raid");

    private static final TranslatableComponent TOOLTIP_CLEAR_TARGET = new TranslatableComponent("gui.recruits.command.tooltip.clearTargets");
    private static final TranslatableComponent TEXT_CLEAR_TARGET = new TranslatableComponent("gui.recruits.command.text.clearTargets");

    private static final int fontColor = 16250871;
    private Player player;
    private int group;
    private int recCount;

    public CommandScreen(CommandContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }

    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x,y,z);
        this.onClose();
        return true;
    }

    @Override
    protected void init() {
        super.init();

        int zeroLeftPos = leftPos + 150;
        int zeroTopPos = topPos + 10;
        int topPosGab = 7;
        int mirror = 240 - 60;

        //TEAM CREATION
        addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos + (20 + topPosGab) * 5 + 25, 80, 20, new TextComponent("TEAM"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamCreationScreen(player));
                }, (a, b, c, d) -> {
            //this.renderTooltip(b, TOOLTIP_DISMOUNT, c, d);
        }));

        //Dismount
        addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_DISMOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(98, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageDismount(player.getUUID(), group));
                }, (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_DISMOUNT, c, d);
        }));

        //Mount
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_MOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(99, player, group);
                    Entity entity = CommandEvents.getEntityByLooking();
                    if (entity != null){
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageMountEntity(player.getUUID(), entity.getUUID(), group));
                    }
                },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_MOUNT, c, d);
        }));

        //Escort
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_ESCORT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(5, player, group);
                    Entity entity = CommandEvents.getEntityByLooking();
                    if (entity != null){
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageEscortEntity(player.getUUID(), entity.getUUID(), group));
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 5, group));
                    }
                },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_ESCORT, c, d);
        }));

        //PASSIVE
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_PASSIVE, button -> {
            CommandEvents.sendAggroCommandInChat(3, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 3, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_PASSIVE, c, d);
        }));

        //NEUTRAL
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_NEUTRAL, button -> {
            CommandEvents.sendAggroCommandInChat(0, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 0, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_NEUTRAL, c, d);
        }));

        //AGGRESSIVE
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_AGGRESSIVE, button -> {
            CommandEvents.sendAggroCommandInChat(1, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 1, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_AGGRESSIVE, c, d);
        }));

        //RAID
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_RAID, button -> {
            CommandEvents.sendAggroCommandInChat(2, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 2, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_RAID, c, d);
        }));

        //CLEAR TARGET
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_CLEAR_TARGET, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(player.getUUID(), this.group));
        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_CLEAR_TARGET, c, d);
        }));



        //WANDER
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_WANDER, button -> {
            CommandEvents.sendFollowCommandInChat(0, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 0, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_WANDER, c, d);
        }));


        //FOLLOW
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_FOLLOW, button -> {
            CommandEvents.sendFollowCommandInChat(1, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 1, group));

        },  (a, b, c, d) -> {

            this.renderTooltip(b, TOOLTIP_FOLLOW, c, d);
        }));


        //HOLD POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_HOLD_POS, button -> {
            CommandEvents.sendFollowCommandInChat(2, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 2, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_HOLD_POS, c, d);
        }));


        //BACK TO POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_BACK_TO_POS, button -> {
            CommandEvents.sendFollowCommandInChat(3, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 3, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_BACK_TO_POS, c, d);
        }));


        //HOLD MY POS
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_HOLD_MY_POS, button -> {
            CommandEvents.sendFollowCommandInChat(4, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 4, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_HOLD_MY_POS, c, d);
        }));

        /*
        //WANDER
        addButton(new Button(leftPos - 70 - 40 + imageWidth / 2, topPos + 20 + 30, 80, 20, TEXT_WANDER, button -> {
            CommandEvents.sendFollowCommandInChat(0, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 0, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_WANDER, c, d);
        }));

        //FOLLOW
        addButton(new Button(leftPos - 40 - 50 + imageWidth / 2, topPos + 10, 80, 20, TEXT_FOLLOW, button -> {
            CommandEvents.sendFollowCommandInChat(1, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 1, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_FOLLOW, c, d);
        }));

        //TO PLAYER POS
        addButton(new Button(leftPos + 10 + imageWidth / 2, topPos + 10, 80, 20, TEXT_HOLD_MY_POS, button -> {
            CommandEvents.sendFollowCommandInChat(4, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 4, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_HOLD_MY_POS, c, d);
        }));

        //HOLD POS
        addButton(new Button(leftPos + 71 + imageWidth / 2, topPos + 20 + 30, 80, 20, TEXT_HOLD_POS, button -> {
            CommandEvents.sendFollowCommandInChat(2, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 2, group));

            },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_HOLD_POS, c, d);
        }));

        //Back to POS
        addButton(new Button(leftPos + 30 + imageWidth / 2, topPos + 20 + 30, 80, 20, TEXT_BACK_TO_POS, button -> {
            CommandEvents.sendFollowCommandInChat(3, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 3, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_BACK_TO_POS, c, d);
        }));

        //PASSIVE
        addButton(new Button(leftPos - 40 - 50 + imageWidth / 2, topPos + 120, 81, 20, TEXT_PASSIVE, button -> {
            CommandEvents.sendAggroCommandInChat(0, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 3, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_PASSIVE, c, d);
        }));

        //NEUTRAL
        addButton(new Button(leftPos - 40 - 50 + imageWidth / 2, topPos + 120, 81, 20, TEXT_NEUTRAL, button -> {
            CommandEvents.sendAggroCommandInChat(0, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 0, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_NEUTRAL, c, d);
        }));

        //AGGRESSIVE
        addButton(new Button(leftPos - 40 - 70 + imageWidth / 2, topPos + 20 + 30 + 30, 81, 20, TEXT_AGGRESSIVE, button -> {
            CommandEvents.sendAggroCommandInChat(1, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 1, group));

            },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_AGGRESSIVE, c, d);
        }));

        //RAID
        addButton(new Button(leftPos + 30 + imageWidth / 2, topPos + 20 + 30 + 30, 81, 20, TEXT_RAID, button -> {
            CommandEvents.sendAggroCommandInChat(2, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 2, group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_RAID, c, d);
        }));

        //CLEAR TARGET
        addButton(new Button(leftPos + 10 + imageWidth / 2, topPos + 120, 81, 20, TEXT_CLEAR_TARGET, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(player.getUUID(), group));

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_CLEAR_TARGET, c, d);
        }));

        /*
        //ATTACK TARGET
        addButton(new Button(leftPos + 30 + imageWidth / 2, topPos + 150, 81, 20, new StringTextComponent("Attack!"), button -> {
            this.onAttackButton();
        }));
        */

        //GROUP
        addRenderableWidget(new Button(leftPos - 4 + imageWidth / 2, topPos - 40 + imageHeight / 2, 11, 20, new TextComponent("+"), button -> {
            this.group = getSavedCurrentGroup(player);

            if (this.group != 9) {
                this.group ++;

                this.saveCurrentGroup(player);
            }
        }));

        addRenderableWidget(new Button(leftPos - 4 + imageWidth / 2, topPos + imageHeight / 2, 11, 20, new TextComponent("-"), button -> {
            this.group = getSavedCurrentGroup(player);

            if (this.group != 0) {
                this.group --;

                this.saveCurrentGroup(player);
            }
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        //Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitsInCommand(player.getUUID()));

        //this.recCount = getSavedRecruitCount(player);
        this.group = getSavedCurrentGroup(player);
        //player.sendMessage(new StringTextComponent("SCREEN int: " + recCount), player.getUUID());

        int k = 78;//rechst links
        int l = 71;//höhe

        font.draw(matrixStack, "" +  handleGroupText(this.group), k , l, fontColor);
        //font.draw(matrixStack, "" +  handleRecruitCountText(currentRecruits), k - 30 , 0, fontColor);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }

    public static String handleGroupText(int group){
        if (group == 0){
            return TEXT_EVERYONE.getString();
        }
        else
            return (TEXT_GROUP.getString()+ " " + group);
    }

    public static String handleRecruitCountText(int recCount){
        return ("Recruits in Command: " + recCount);
    }
    /*
    public void onAttackButton(){
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraft.player;

        RayTraceResult rayTraceResult = minecraft.hitResult;
        if (rayTraceResult != null) {
            if (rayTraceResult.getType() == RayTraceResult.Type.ENTITY) {
                EntityRayTraceResult entityRayTraceResult = (EntityRayTraceResult) rayTraceResult;
                if (entityRayTraceResult.getEntity() instanceof LivingEntity && clientPlayerEntity != null) {
                    LivingEntity living = (LivingEntity) entityRayTraceResult.getEntity();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAttackEntity(clientPlayerEntity.getUUID(), living.getUUID()));
                }
            }
        }
    }

     */


    public int getSavedCurrentGroup(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getInt("CommandingGroup");
    }

    public void saveCurrentGroup(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putInt( "CommandingGroup", this.group);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

}
