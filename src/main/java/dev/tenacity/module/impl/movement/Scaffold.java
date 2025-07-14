package dev.tenacity.module.impl.movement;

import dev.tenacity.event.impl.game.TickEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.BlockPlaceableEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.SafeWalkEvent;
import dev.tenacity.event.impl.player.UpdateEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.misc.EnumFacingUtils;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.ui.notifications.NotificationManager;
import dev.tenacity.ui.notifications.NotificationType;
import dev.tenacity.utils.BlinkUtils;
import dev.tenacity.utils.addons.rise.MovementFix;
import dev.tenacity.utils.addons.rise.RayCastUtil;
import dev.tenacity.utils.addons.rise.RotationUtil;
import dev.tenacity.utils.addons.rise.component.RenderSlotComponent;
import dev.tenacity.utils.addons.rise.component.RotationComponent;
import dev.tenacity.utils.addons.vector.Rotation;
import dev.tenacity.utils.addons.vector.Vector2f;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.misc.SlotUtil;
import dev.tenacity.utils.player.*;
import dev.tenacity.utils.render.ColorUtil;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.render.RoundedUtil;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.IFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

import static dev.tenacity.utils.misc.MathUtils.getRandom;

public class Scaffold extends Module {

    private final ModeSetting countMode = new ModeSetting("Block Counter", "Tenacity", "None", "Tenacity", "Basic", "Polar", "Exhibition");
    private final ModeSetting animationMode = new ModeSetting("Animation Mode", "Normal", "None", "Normal");
    private final BooleanSetting rotations = new BooleanSetting("Rotations", true);
    private final ModeSetting rotationMode = new ModeSetting("Rotation Mode", "Watchdog", "Watchdog", "NCP", "Back", "45", "Enum", "Down", "0");
    private final ModeSetting placeType = new ModeSetting("Place Type", "Post", "Pre", "Post", "Legit", "Dynamic");
    public static ModeSetting keepYMode = new ModeSetting("Keep Y Mode", "Always", "Always", "Speed toggled");
    public static ModeSetting sprintMode = new ModeSetting("Sprint Mode", "Vanilla", "Vanilla", "Watchdog", "Cancel");
    public static ModeSetting towerMode = new ModeSetting("Tower Mode", "Watchdog", "Vanilla", "NCP", "Watchdog", "Verus", "Legit");
    public static ModeSetting swingMode = new ModeSetting("Swing Mode", "Client", "Client", "Silent");
    public static final ModeSetting swapMode = new ModeSetting("Swap Mode", "Normal", "Normal", "Spoof", "Lite Spoof");
    public static NumberSetting delay = new NumberSetting("Delay", 0, 2, 0, 0.05);
    private final NumberSetting timer = new NumberSetting("Timer", 1, 5, 0.1, 0.1);
    public static final BooleanSetting auto3rdPerson = new BooleanSetting("Auto 3rd Person", false);
    public static final BooleanSetting speedSlowdown = new BooleanSetting("Speed Slowdown", true);
    public static final NumberSetting speedSlowdownAmount = new NumberSetting("Slowdown Amount", 0.1, 0.2, 0.01, 0.01);
    public static final BooleanSetting downwards = new BooleanSetting("Downwards", false);
    public static final BooleanSetting safewalk = new BooleanSetting("Safewalk", false);
    public static final BooleanSetting sprint = new BooleanSetting("Sprint", false);
    private final BooleanSetting sneak = new BooleanSetting("Sneak", false);
    public static final BooleanSetting tower = new BooleanSetting("Tower", false);
    private final NumberSetting towerTimer = new NumberSetting("Tower Timer Boost", 1.2, 5, 0.1, 0.1);
    private final BooleanSetting swing = new BooleanSetting("Swing", true);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", false);
    private final BooleanSetting hideJump = new BooleanSetting("Hide Jump", false);
    private final BooleanSetting baseSpeed = new BooleanSetting("Base Speed", false);
    public static BooleanSetting keepY = new BooleanSetting("Keep Y", false);

    private ScaffoldUtils.BlockCache blockCache, lastBlockCache;
    private float y;
    private float speed;
    private final MouseFilter pitchMouseFilter = new MouseFilter();
    private final TimerUtil delayTimer = new TimerUtil();
    private final TimerUtil timerUtil = new TimerUtil();
    public static double keepYCoord;
    private boolean shouldSendPacket;
    private boolean shouldTower;
    private boolean firstJump;
    private boolean pre;
    private int jumpTimer;
    private int blockPlacementSlot = -1;
    private int prevSlot;
    private float[] cachedRots = new float[2];
    private int offGroundTicks = 0;

    private final Animation anim = new DecelerateAnimation(250, 1);
    private boolean shouldrot;

    //hypixel bypasses
    public static Vec3 targetBlock;
    private int ticksOnAir;
    private EnumFacingUtils enumFacingABC;
    private BlockPos blockFace;
    private static float targetYaw;
    private float targetPitch;
    double startY;

    public Scaffold() {
        super("Scaffold", Category.MOVEMENT, "Automatically places blocks under you");
        this.addSettings(countMode, animationMode, rotations, rotationMode, placeType, keepYMode, sprintMode, towerMode, swingMode, swapMode, delay, timer,
                auto3rdPerson, speedSlowdown, speedSlowdownAmount, downwards, safewalk, sprint, sneak, tower, towerTimer,
                swing, autoJump, hideJump, baseSpeed, keepY);
        rotationMode.addParent(rotations, ParentAttribute.BOOLEAN_CONDITION);
        sprintMode.addParent(sprint, ParentAttribute.BOOLEAN_CONDITION);
        towerMode.addParent(tower, ParentAttribute.BOOLEAN_CONDITION);
        swingMode.addParent(swing, ParentAttribute.BOOLEAN_CONDITION);
        towerTimer.addParent(tower, ParentAttribute.BOOLEAN_CONDITION);
        keepYMode.addParent(keepY, ParentAttribute.BOOLEAN_CONDITION);
        hideJump.addParent(autoJump, ParentAttribute.BOOLEAN_CONDITION);
        speedSlowdownAmount.addParent(speedSlowdown, ParentAttribute.BOOLEAN_CONDITION);
        animationMode.addParent(countMode, modeSetting -> !modeSetting.is("None"));
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (!mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.timer.timerSpeed = timer.getValue().floatValue();
        } else {
            mc.timer.timerSpeed = tower.isEnabled() ? towerTimer.getValue().floatValue() : 1;
        }

        if (e.isPre()) {
            if (rotations.isEnabled() && rotationMode.is("Watchdog") && sprintMode.is("Watchdog")) {
                if (PlayerUtil.blockRelativeToPlayer(0, -1, 0) instanceof BlockAir) {
                    ticksOnAir++;
                } else {
                    ticksOnAir = 0;
                }
                // Gets block to place
                targetBlock = PlayerUtil.getPlacePossibility(0,0,0,(int) 5);;
                if (targetBlock == null) {
                    return;
                }
                //Gets EnumFacing
                enumFacingABC = PlayerUtil.getEnumFacing(targetBlock);
                if (enumFacingABC == null) {
                    return;
                }
                final BlockPos position = new BlockPos(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord);
                blockFace = position.add(enumFacingABC.getOffset().xCoord, enumFacingABC.getOffset().yCoord, enumFacingABC.getOffset().zCoord);
                if (blockFace == null || enumFacingABC == null) {
                    return;
                }
                calculateRotations();
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
                    mc.gameSettings.keyBindJump.pressed = ((mc.thePlayer.onGround && MoveUtil.isMoving()) || mc.gameSettings.keyBindJump.isPressed());
                } else {
                    mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump);
                }
                if (e.isOnGround()){
                    if (!BlinkUtils.instance.blinking){
                        BlinkUtils.startBlink();
                    }
                    mc.thePlayer.setSprinting(true);
                }else {
                    BlinkUtils.stopBlink();
                }
            } else {
                if (baseSpeed.isEnabled()) {
                    MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 0.7);
                }
                if (rotations.isEnabled()) {
                    float[] rotations = new float[]{0, 0};
                    switch (rotationMode.getMode()) {
                        case "NCP":
                            float prevYaw = cachedRots[0];
                            if ((blockCache = ScaffoldUtils.getBlockInfo()) == null) {
                                blockCache = lastBlockCache;
                            }
                            if (blockCache != null && (mc.thePlayer.ticksExisted % 3 == 0
                                    || mc.theWorld.getBlockState(new BlockPos(e.getX(), ScaffoldUtils.getYLevel(), e.getZ())).getBlock() == Blocks.air)) {
                                cachedRots = RotationUtils.getRotations(blockCache.getPosition(), blockCache.getFacing());
                            }
                            if ((mc.thePlayer.onGround || (MovementUtils.isMoving() && tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown())) && Math.abs(cachedRots[0] - prevYaw) >= 90) {
                                cachedRots[0] = MovementUtils.getMoveYaw(e.getYaw()) - 180;
                            }
                            rotations = cachedRots;
                            e.setRotations(rotations[0], rotations[1]);
                            break;
                        case "Back":
                            rotations = new float[]{MovementUtils.getMoveYaw(e.getYaw()) - 180, 77};
                            e.setRotations(rotations[0], rotations[1]);
                            break;
                        case "Down":
                            e.setPitch(90);
                            break;
                        case "45":
                            float val;
                            if (MovementUtils.isMoving()) {
                                float f = MovementUtils.getMoveYaw(e.getYaw()) - 180;
                                float[] numbers = new float[]{-135, -90, -45, 0, 45, 90, 135, 180};
                                float lastDiff = 999;
                                val = f;
                                for (float v : numbers) {
                                    float diff = Math.abs(v - f);
                                    if (diff < lastDiff) {
                                        lastDiff = diff;
                                        val = v;
                                    }
                                }
                            } else {
                                val = rotations[0];
                            }
                            rotations = new float[]{
                                    (val + MathHelper.wrapAngleTo180_float(mc.thePlayer.prevRotationYawHead)) / 2.0F,
                                    (77 + MathHelper.wrapAngleTo180_float(mc.thePlayer.prevRotationPitchHead)) / 2.0F};
                            e.setRotations(rotations[0], rotations[1]);
                            break;
                        case "Enum":
                            if (lastBlockCache != null) {
                                float yaw = RotationUtils.getEnumRotations(lastBlockCache.getFacing());
                                e.setRotations(yaw, 77);
                            } else {
                                e.setRotations(mc.thePlayer.rotationYaw + 180, 77);
                            }
                            break;
                        case "0":
                            e.setRotations(0, 0);
                            break;
                    }
                    RotationUtils.setVisualRotations(e);
                }

                if (speedSlowdown.isEnabled() && mc.thePlayer.isPotionActive(Potion.moveSpeed) && !mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.onGround) {
                    MovementUtils.setSpeed(speedSlowdownAmount.getValue());
                }

                if (sneak.isEnabled()) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);

                if (mc.thePlayer.onGround) {
                    keepYCoord = Math.floor(mc.thePlayer.posY - 1.0);
                }

                if (tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown()) {
                    double centerX = Math.floor(e.getX()) + 0.5, centerZ = Math.floor(e.getZ()) + 0.5;
                    switch (towerMode.getMode()) {
                        case "Vanilla":
                            mc.thePlayer.motionY = 0.42f;
                            break;
                        case "Verus":
                            if (mc.thePlayer.ticksExisted % 2 == 0)
                                mc.thePlayer.motionY = 0.42f;
                            break;
                        case "Watchdog":
                            if (!MovementUtils.isMoving() && mc.theWorld.getBlockState(new BlockPos(mc.thePlayer).down()).getBlock() != Blocks.air && lastBlockCache != null) {
                                if (mc.thePlayer.ticksExisted % 6 == 0) {
                                    e.setX(centerX + 0.1);
                                    e.setZ(centerZ + 0.1);
                                } else {
                                    e.setX(centerX - 0.1);
                                    e.setZ(centerZ - 0.1);
                                }
                                MovementUtils.setSpeed(0);
                            }

                            mc.thePlayer.motionY = 0.3;
                            e.setOnGround(true);
                            break;
                        case "NCP":
                            if (!MovementUtils.isMoving() || MovementUtils.getSpeed() < 0.16) {
                                if (mc.thePlayer.onGround) {
                                    mc.thePlayer.motionY = 0.42;
                                } else if (mc.thePlayer.motionY < 0.23) {
                                    mc.thePlayer.setPosition(mc.thePlayer.posX, (int) mc.thePlayer.posY, mc.thePlayer.posZ);
                                    mc.thePlayer.motionY = 0.42;
                                }
                            }
                            break;
                        case "Legit":
                            if (mc.thePlayer.onGround) {
                                mc.thePlayer.jump();
                            }
                            break;
                    }
                }

                blockCache = ScaffoldUtils.getBlockInfo();
                if (blockCache != null) {
                    lastBlockCache = ScaffoldUtils.getBlockInfo();
                } else {
                    return;
                }

                if (mc.thePlayer.ticksExisted % 4 == 0) {
                    pre = true;
                }

                if (placeType.is("Pre") || (placeType.is("Dynamic") && pre)) {
                    if (place()) {
                        pre = false;
                    }
                } else {
                    if (placeType.is("Post") || (placeType.is("Dynamic") && !pre)) {
                        place();
                    }

                    pre = false;
                }
            }
        }
        if (e.isPost()) {
            if (rotations.isEnabled() && rotationMode.is("Watchdog") && sprintMode.is("Watchdog")) {
                int slot = SlotUtil.findBlock();
                if (slot == -1) {
                    NotificationManager.post(NotificationType.WARNING, "Scaffold", "No blocks in hotbar!");
                    toggle();
                    return;
                }
                mc.thePlayer.inventory.currentItem = slot;
            }
        }
    }

    @Override
    public void onBlockPlaceable(BlockPlaceableEvent event) {
        if (rotations.isEnabled() && rotationMode.is("Watchdog") && sprintMode.is("Watchdog")) {
            if (mc.thePlayer == null) {
                return;
            }
            // Same Y
            if (keepY.isEnabled()) {
                final boolean sameY = !GameSettings.isKeyDown(mc.gameSettings.keyBindJump) && MoveUtil.isMoving();
                if (targetBlock!=null) {
                    if (startY - 1 != Math.floor(targetBlock.yCoord) && !mc.thePlayer.onGround && sameY) {
                        return;
                    }
                }
            }
            if (ticksOnAir > getRandom(0, 0) && RayCastUtil.overBlock(RotationComponent.lastServerRotations, enumFacingABC.getEnumFacing(), blockFace, false)) {
//
                Vec3 hitVec = this.getHitVec();
//
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), blockFace, enumFacingABC.getEnumFacing(), hitVec)) {
                    mc.thePlayer.swingItem();
                }
                mc.rightClickDelayTimer = 0;
            }
            //For Same Y
            if (mc.thePlayer != null && keepY.isEnabled()) {
                if (mc.thePlayer.onGround || (mc.gameSettings.keyBindJump.isKeyDown() && !MoveUtil.isMoving())) {
                    startY = Math.floor(mc.thePlayer.posY);
                }
                if (mc.thePlayer.posY < startY) {
                    startY = mc.thePlayer.posY;
                }
            }
        }else {
            if (placeType.is("Legit")){
                place();
            }
        }
    }

    public Vec3 getHitVec() {
        /* Correct HitVec */
        Vec3 hitVec = new Vec3(blockFace.getX() + Math.random(), blockFace.getY() + Math.random(), blockFace.getZ() + Math.random());
        final MovingObjectPosition movingObjectPosition = RayCastUtil.rayCast(RotationComponent.rotations, mc.playerController.getBlockReachDistance());
        switch (enumFacingABC.getEnumFacing()) {
            case DOWN:
                hitVec.yCoord = blockFace.getY();
                break;
            case UP:
                hitVec.yCoord = blockFace.getY() + 1;
                break;
            case NORTH:
                hitVec.zCoord = blockFace.getZ();
                break;
            case EAST:
                hitVec.xCoord = blockFace.getX() + 1;
                break;
            case SOUTH:
                hitVec.zCoord = blockFace.getZ() + 1;
                break;
            case WEST:
                hitVec.xCoord = blockFace.getX();
                break;
        }
        if (movingObjectPosition != null && movingObjectPosition.getBlockPos().equals(blockFace) &&
                movingObjectPosition.sideHit == enumFacingABC.getEnumFacing()) {
            hitVec = movingObjectPosition.hitVec;
        }
        return hitVec;
    }

    private boolean place() {
        int blockSlot = ScaffoldUtils.getBlockSlot();
        if (blockCache == null || lastBlockCache == null || blockSlot == -1) return false;

        if (swapMode.is("Spoof")) {
            if (this.blockPlacementSlot != blockSlot) {
                this.blockPlacementSlot = blockSlot;
                PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(this.blockPlacementSlot));
            }
        } else if (swapMode.is("Normal")) {
            mc.thePlayer.inventory.currentItem = blockSlot;
            this.blockPlacementSlot = blockSlot;
        } else if (swapMode.is("Lite Spoof")) {
            int currentClientSlot = mc.thePlayer.inventory.currentItem;

            if (currentClientSlot != blockSlot) {
                PacketUtils.sendPacket(new C09PacketHeldItemChange(blockSlot));
                mc.thePlayer.inventory.currentItem = blockSlot;
            }
            this.blockPlacementSlot = blockSlot;
        }


        boolean placed = false;
        if (delayTimer.hasTimeElapsed(delay.getValue() * 1000)) {
            firstJump = false;
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                    mc.thePlayer.inventory.getStackInSlot(this.blockPlacementSlot),
                    lastBlockCache.getPosition(), lastBlockCache.getFacing(),
                    ScaffoldUtils.getHypixelVec3(lastBlockCache))) {
                placed = true;
                y = MathUtils.getRandomInRange(79.5f, 83.5f);
                if (swing.isEnabled()) {
                    if (swingMode.is("Client")) {
                        mc.thePlayer.swingItem();
                    } else {
                        PacketUtils.sendPacket(new C0APacketAnimation());
                    }
                }
            }
            delayTimer.reset();
            blockCache = null;

            if (placed && swapMode.is("Lite Spoof")) {
                if (mc.thePlayer.inventory.currentItem != prevSlot) {
                    PacketUtils.sendPacket(new C09PacketHeldItemChange(prevSlot));
                    mc.thePlayer.inventory.currentItem = prevSlot;
                }
            }
        }

        return placed;
    }

    @Override
    public void onTickEvent(TickEvent event) {
        if (mc.thePlayer == null) return;
        if (hideJump.isEnabled() && !mc.gameSettings.keyBindJump.isKeyDown() && MovementUtils.isMoving() && !mc.thePlayer.onGround && autoJump.isEnabled()) {
            mc.thePlayer.posY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY;
            mc.thePlayer.lastTickPosY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY;
            mc.thePlayer.cameraYaw = mc.thePlayer.cameraPitch = 0.1F;
        }
        if (downwards.isEnabled()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            mc.thePlayer.movementInput.sneak = false;
        }
        if (mc.thePlayer.isOnGround()) {
            offGroundTicks++;
        }else {
            offGroundTicks = 0;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            if (swapMode.is("Spoof")) {
                if (blockPlacementSlot != prevSlot) {
                    PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(prevSlot));
                }
            } else if (swapMode.is("Normal")) {
                mc.thePlayer.inventory.currentItem = prevSlot;
            } else if (swapMode.is("Lite Spoof")) {
                if (mc.thePlayer.inventory.currentItem != prevSlot) {
                    PacketUtils.sendPacket(new C09PacketHeldItemChange(prevSlot));
                    mc.thePlayer.inventory.currentItem = prevSlot;
                }
            }

            if (auto3rdPerson.isEnabled()) {
                mc.gameSettings.thirdPersonView = 0;
            }
            if (mc.thePlayer.isSneaking() && sneak.isEnabled())
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindSneak));
        }
        mc.timer.timerSpeed = 1;
        BlinkUtils.stopBlink();
        super.onDisable();
    }

    @Override
    public void onUpdateEvent(UpdateEvent event) {
        if (autoJump.isEnabled() && !sprintMode.is("Watchdog") && GameSettings.isKeyDown(mc.gameSettings.keyBindForward)) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.gameSettings.keyBindJump.pressed = false;
            }
        }
    }

    @Override
    public void onEnable() {
        lastBlockCache = null;
        if (mc.thePlayer != null) {
            prevSlot = mc.thePlayer.inventory.currentItem;
            blockPlacementSlot = -1;

            if (mc.thePlayer.isSprinting() && sprint.isEnabled() && sprintMode.is("Cancel")) {
                PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }
            if (auto3rdPerson.isEnabled()) {
                mc.gameSettings.thirdPersonView = 1;
            }
        }
        firstJump = true;
        speed = 1.1f;
        timerUtil.reset();
        jumpTimer = 0;
        y = 80;
        if (animationMode.is("None")) {
            anim.setDuration(1);
            anim.setDirection(Direction.FORWARDS);
        } else {
            anim.setDuration(250);
        }
        super.onEnable();
    }

    public void renderCounterBlur() {
        if (!enabled && anim.isDone()) return;

        int slotToDisplay;
        if (swapMode.is("Lite Spoof") || swapMode.is("Spoof")) {
            slotToDisplay = ScaffoldUtils.getBlockSlot();
        } else {
            slotToDisplay = mc.thePlayer.inventory.currentItem;
        }

        ItemStack heldItem = slotToDisplay == -1 ? null : mc.thePlayer.inventory.mainInventory[slotToDisplay];
        int count = ScaffoldUtils.getBlockCount();
        String countStr = String.valueOf(count);
        IFontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        float x, y;
        String str = countStr + " block" + (count != 1 ? "s" : "");

        float animationOutput = animationMode.is("None") ? 1.0f : anim.getOutput().floatValue();
        float alpha = 1.0f;
        float scaleFactor = animationMode.is("Normal") ? animationOutput : 1.0f;


        switch (countMode.getMode()) {
            case "Tenacity":
                float blockWH = heldItem != null ? 15 : -2;
                int spacing = 3;
                String text = "§l" + countStr + "§r block" + (count != 1 ? "s" : "");
                float textWidth = tenacityFont18.getStringWidth(text);

                float totalWidth = ((textWidth + blockWH + spacing) + 6) * scaleFactor;
                x = sr.getScaledWidth() / 2f - (totalWidth / 2f);
                y = sr.getScaledHeight() - (sr.getScaledHeight() / 2f - 20);
                float height = 20;
                RenderUtil.scissorStart(x - 1.5, y - 1.5, totalWidth + 3, height + 3);

                RoundedUtil.drawRound(x, y, totalWidth, height, 5, new Color(ColorUtil.applyOpacity(Color.BLACK.getRGB(), alpha)));
                RenderUtil.scissorEnd();
                break;
            case "Basic":
                x = sr.getScaledWidth() / 2F - fr.getStringWidth(str) / 2F + 1;
                y = sr.getScaledHeight() / 2F + 10;
                if (animationMode.is("Normal")) {
                    RenderUtil.scaleStart(sr.getScaledWidth() / 2.0F, y + fr.FONT_HEIGHT / 2.0F, scaleFactor);
                }
                fr.drawStringWithShadow(str, x, y, ColorUtil.applyOpacity(0x000000, alpha));
                if (animationMode.is("Normal")) {
                    RenderUtil.scaleEnd();
                }
                break;
            case "Polar":
                x = sr.getScaledWidth() / 2F - fr.getStringWidth(countStr) / 2F + (heldItem != null ? 6 : 1);
                y = sr.getScaledHeight() / 2F + 10;

                GlStateManager.pushMatrix();
                RenderUtil.fixBlendIssues();
                GL11.glTranslatef(x + (heldItem == null ? 1 : 0), y, 1);
                if (animationMode.is("Normal")) {
                    GL11.glScaled(scaleFactor, scaleFactor, 1);
                }
                GL11.glTranslatef(-x - (heldItem == null ? 1 : 0), -y, 1);

                fr.drawOutlinedString(countStr, x, y, ColorUtil.applyOpacity(0x000000, alpha), true);

                if (heldItem != null) {
                    double itemScale = 0.7;
                    GlStateManager.scale(itemScale, itemScale, itemScale);
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(
                            heldItem,
                            (int) ((sr.getScaledWidth() / 2F - fr.getStringWidth(countStr) / 2F - 7) / itemScale),
                            (int) ((sr.getScaledHeight() / 2F + 8.5F) / itemScale)
                    );
                    RenderHelper.disableStandardItemLighting();
                }
                GlStateManager.popMatrix();
                break;
        }
    }

    public void renderCounter() {
        anim.setDirection(enabled ? Direction.FORWARDS : Direction.BACKWARDS);
        if (animationMode.is("None")) {
            anim.setDuration(1);
            anim.setDirection(enabled ? Direction.FORWARDS : Direction.BACKWARDS);
        } else {
            anim.setDuration(250);
        }

        if (!enabled && anim.isDone()) return;

        int slotToDisplay;
        if (swapMode.is("Lite Spoof") || swapMode.is("Spoof")) {
            slotToDisplay = ScaffoldUtils.getBlockSlot();
        } else {
            slotToDisplay = mc.thePlayer.inventory.currentItem;
        }

        ItemStack heldItem = slotToDisplay == -1 ? null : mc.thePlayer.inventory.mainInventory[slotToDisplay];
        int count = ScaffoldUtils.getBlockCount();
        String countStr = String.valueOf(count);
        IFontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        int color;
        float x, y;
        String str = countStr + " block" + (count != 1 ? "s" : "");

        float animationOutput = animationMode.is("None") ? 1.0f : anim.getOutput().floatValue();
        float alpha = 1.0f;
        float scaleFactor = animationMode.is("Normal") ? animationOutput : 1.0f;


        switch (countMode.getMode()) {
            case "Tenacity":
                float blockWH = heldItem != null ? 15 : -2;
                int spacing = 3;
                String text = "§l" + countStr + "§r block" + (count != 1 ? "s" : "");
                float textWidth = tenacityFont18.getStringWidth(text);

                float totalWidth = ((textWidth + blockWH + spacing) + 6) * scaleFactor;
                x = sr.getScaledWidth() / 2f - (totalWidth / 2f);
                y = sr.getScaledHeight() - (sr.getScaledHeight() / 2f - 20);
                float height = 20;
                RenderUtil.scissorStart(x - 1.5, y - 1.5, totalWidth + 3, height + 3);

                RoundedUtil.drawRound(x, y, totalWidth, height, 5, ColorUtil.tripleColor(20, .45f * alpha));


                tenacityFont18.drawString(text, x + 3 + blockWH + spacing, y + tenacityFont18.getMiddleOfBox(height) + .5f, ColorUtil.applyOpacity(-1, alpha));

                if (heldItem != null) {
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, (int) x + 3, (int) (y + 10 - (blockWH / 2)));
                    RenderHelper.disableStandardItemLighting();
                }
                RenderUtil.scissorEnd();
                break;
            case "Basic":
                x = sr.getScaledWidth() / 2F - fr.getStringWidth(str) / 2F + 1;
                y = sr.getScaledHeight() / 2F + 10;
                if (animationMode.is("Normal")) {
                    RenderUtil.scaleStart(sr.getScaledWidth() / 2.0F, y + fr.FONT_HEIGHT / 2.0F, scaleFactor);
                }
                fr.drawStringWithShadow(str, x, y, ColorUtil.applyOpacity(-1, alpha));
                if (animationMode.is("Normal")) {
                    RenderUtil.scaleEnd();
                }
                break;
            case "Polar":
                color = count < 24 ? 0xFFFF5555 : count < 128 ? 0xFFFFFF55 : 0xFF55FF55;
                x = sr.getScaledWidth() / 2F - fr.getStringWidth(countStr) / 2F + (heldItem != null ? 6 : 1);
                y = sr.getScaledHeight() / 2F + 10;

                GlStateManager.pushMatrix();
                RenderUtil.fixBlendIssues();
                GL11.glTranslatef(x + (heldItem == null ? 1 : 0), y, 1);
                if (animationMode.is("Normal")) {
                    GL11.glScaled(scaleFactor, scaleFactor, 1);
                }
                GL11.glTranslatef(-x - (heldItem == null ? 1 : 0), -y, 1);

                fr.drawOutlinedString(countStr, x, y, ColorUtil.applyOpacity(color, alpha), true);

                if (heldItem != null) {
                    double itemScale = 0.7;
                    GlStateManager.scale(itemScale, itemScale, itemScale);
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(
                            heldItem,
                            (int) ((sr.getScaledWidth() / 2F - fr.getStringWidth(countStr) / 2F - 7) / itemScale),
                            (int) ((sr.getScaledHeight() / 2F + 8.5F) / itemScale)
                    );
                    RenderHelper.disableStandardItemLighting();
                }
                GlStateManager.popMatrix();
                break;
            case "Exhibition":
                if (!enabled) return;

                int c = ColorUtil.getColor(255, 0, 0, 150);
                if (count >= 64 && 128 > count) {
                    c = ColorUtil.getColor(255, 255, 0, 150);
                } else if (count >= 128) {
                    c = ColorUtil.getColor(0, 255, 0, 150);
                }

                fr.drawString(String.valueOf(count), sr.getScaledWidth() / 2f - (fr.getStringWidth(String.valueOf(count)) / 2f) - 1, sr.getScaledHeight() / 2f - 36, 0xff000000, false);
                fr.drawString(String.valueOf(count), sr.getScaledWidth() / 2f - (fr.getStringWidth(String.valueOf(count)) / 2f) + 1, sr.getScaledHeight() / 2f - 36, 0xff000000, false);
                fr.drawString(String.valueOf(count), sr.getScaledWidth() / 2f - (fr.getStringWidth(String.valueOf(count)) / 2f), sr.getScaledHeight() / 2f - 35, 0xff000000, false);
                fr.drawString(String.valueOf(count), sr.getScaledWidth() / 2f - (fr.getStringWidth(String.valueOf(count)) / 2f), sr.getScaledHeight() / 2f - 37, 0xff000000, false);
                fr.drawString(String.valueOf(count), sr.getScaledWidth() / 2f - (fr.getStringWidth(String.valueOf(count)) / 2f), sr.getScaledHeight() / 2f - 36, c, false);
                break;
        }
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent e) {
        if (e.getPacket() instanceof C0BPacketEntityAction
                && ((C0BPacketEntityAction) e.getPacket()).getAction() == C0BPacketEntityAction.Action.START_SPRINTING
                && sprint.isEnabled() && sprintMode.is("Cancel")) {
            e.cancel();
        }
        if (e.getPacket() instanceof C09PacketHeldItemChange && swapMode.is("Spoof")) {
            e.cancel();
        }
    }

    @Override
    public void onSafeWalkEvent(SafeWalkEvent event) {
        if ((safewalk.isEnabled() && !isDownwards()) || ScaffoldUtils.getBlockCount() == 0) {
            event.setSafe(true);
        }
    }

    public static boolean isDownwards() {
        return downwards.isEnabled() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak);
    }

    public static Vec3i translate(BlockPos blockPos, EnumFacing enumFacing) {
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();
        double r1 = ThreadLocalRandom.current().nextDouble(0.3, 0.5);
        double r2 = ThreadLocalRandom.current().nextDouble(0.9, 1.0);
        if (enumFacing.equals(EnumFacing.UP)) {
            x += r1;
            z += r1;
            y += 1.0;
        } else if (enumFacing.equals(EnumFacing.DOWN)) {
            x += r1;
            z += r1;
        } else if (enumFacing.equals(EnumFacing.WEST)) {
            y += r2;
            z += r1;
        } else if (enumFacing.equals(EnumFacing.EAST)) {
            y += r2;
            z += r1;
            x += 1.0;
        } else if (enumFacing.equals(EnumFacing.SOUTH)) {
            y += r2;
            x += r1;
            z += 1.0;
        } else if (enumFacing.equals(EnumFacing.NORTH)) {
            y += r2;
            x += r1;
        }
        return new Vec3i(x, y, z);
    }

    public void calculateRotations() {
        final Vector2f rotations = RotationUtil.calculate(new dev.tenacity.utils.addons.vector.Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), enumFacingABC.getEnumFacing());
        if (mc.thePlayer.offGroundTicks > 0) {
            if (!RayCastUtil.overBlock(RotationComponent.rotations, enumFacingABC.getEnumFacing(), blockFace, false)) {
                getRotationsA();
                targetYaw = rotations.x;
            }
        } else {
            getRotationsA();
            targetYaw = getYaw();
        }
        RotationComponent.setRotations(new Vector2f(targetYaw, targetPitch), 10f, MovementFix.NORMAL);
    }

    public void getRotationsA() {
        final Vector2f rotations = RotationUtil.calculate(new dev.tenacity.utils.addons.vector.Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), enumFacingABC.getEnumFacing());
        targetYaw = rotations.x;
        if (mc.gameSettings.keyBindJump.isKeyDown() && !MoveUtil.isMoving()) {
            targetYaw = rotations.x;
        }
        targetPitch = rotations.y;
    }

    private float getYaw() {
        final Vector2f rotations = RotationUtil.calculate(new dev.tenacity.utils.addons.vector.Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), enumFacingABC.getEnumFacing());
        if (mc.thePlayer.hurtTime > 0 || mc.gameSettings.keyBindBack.isKeyDown()) {
            return mc.thePlayer.rotationYaw - rotations.x;
        }
        return mc.thePlayer.rotationYaw;
    }
}