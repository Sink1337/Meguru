package dev.tenacity.module.impl.movement;

import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.MoveEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.ui.notifications.NotificationManager;
import dev.tenacity.ui.notifications.NotificationType;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBow;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

public final class LongJump extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Watchdog", "NCP", "AGC", "Bloxd");
    private final ModeSetting watchdogMode = new ModeSetting("Watchdog Mode", "Damage", "Damage", "Damageless");
    private final NumberSetting damageSpeed = new NumberSetting("Damage Speed", 1, 20, 1, 0.5);
    private final BooleanSetting spoofY = new BooleanSetting("Spoof Y", false);
    private int movementTicks = 0;
    private double speed;
    private float pitch;
    private int prevSlot, ticks = 0;
    private boolean damagedBow;
    private final TimerUtil jumpTimer = new TimerUtil();
    private boolean damaged;
    private double x;
    private double y;
    private double z;
    private final List<Packet> packets = new ArrayList<>();
    private int stage;

    private final TimerUtil flightTimer = new TimerUtil();
    private boolean bloxdFlying;

    private int bowModuleEnableTicks = 0;
    private int bowSlot = -1;
    private boolean hasTakenBowDamage = false;

    private final NumberSetting bloxdHorizontalSpeed = new NumberSetting("Bloxd Horizontal Speed", 0.5, 5, 0.1, 0.1);
    private final NumberSetting bloxdVerticalSpeed = new NumberSetting("Bloxd Vertical Speed", 0.2, 5, 0.1, 0.1);
    private final NumberSetting bowPullTicks = new NumberSetting("Bow Release Tick", 5, 20, 1, 1);
    private final NumberSetting bloxdFlyTime = new NumberSetting("Bloxd Fly Time(ms)", 1000, 5000, 500, 50);


    public static boolean isBloxdFlying = false;
    private boolean waitingForBowPullTick = false;

    @Override
    public void onMotionEvent(MotionEvent event) {
        setSuffix(mode.getMode());
        if (spoofY.isEnabled()) mc.thePlayer.posY = y;
        switch (mode.getMode()) {
            case "Vanilla":
                if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                    MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 2);
                    mc.thePlayer.jump();
                }
                break;
            case "Watchdog":
                if (event.isPre()) {
                    switch (watchdogMode.getMode()) {
                        case "Damage":
                            if (mc.thePlayer.onGround) {
                                stage++;
                                if (stage <= 3)
                                    mc.thePlayer.jump();
                                if (stage > 5 && damaged)
                                    toggle();
                            }
                            if (stage <= 3) {
                                event.setOnGround(false);
                                mc.thePlayer.posY = y;
                                mc.timer.timerSpeed = damageSpeed.getValue().floatValue();
                                speed = 1.2;
                            }
                            if (mc.thePlayer.hurtTime > 0) {
                                damaged = true;
                                ticks++;
                                if (ticks < 2)
                                    mc.thePlayer.motionY = 0.41999998688698;
                                MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * speed);
                                speed -= 0.01;
                                mc.timer.timerSpeed = 1;
                            }
                            if (damaged) {
                                mc.thePlayer.motionY += 0.0049;
                            }
                            break;
                        case "Damageless":
                            stage++;

                            if (stage == 1 && mc.thePlayer.onGround) {
                                mc.thePlayer.motionY = 0.42;
                                MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.2);
                                speed = 1.45f;
                            }

                            if (stage > 1) {
                                MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * speed);
                                speed -= 0.015;
                            }

                            if (mc.thePlayer.onGround && stage > 1)
                                toggle();
                            break;
                    }
                }
                break;
            case "NCP":
                if (MovementUtils.isMoving()) {
                    if (MovementUtils.isOnGround(0.00023)) {
                        mc.thePlayer.motionY = 0.41;
                    }

                    switch (movementTicks) {
                        case 1:
                            speed = MovementUtils.getBaseMoveSpeed();
                            break;
                        case 2:
                            speed = MovementUtils.getBaseMoveSpeed() + (0.132535 * Math.random());
                            break;
                        case 3:
                            speed = MovementUtils.getBaseMoveSpeed() / 2;
                            break;
                    }
                    MovementUtils.setSpeed(Math.max(speed, MovementUtils.getBaseMoveSpeed()));
                    movementTicks++;
                }
                break;
            case "AGC":
                if (event.isPre()) {
                    if (damagedBow) {
                        if (mc.thePlayer.onGround && jumpTimer.hasTimeElapsed(1000)) {
                            toggle();
                        }
                        if (mc.thePlayer.onGround && mc.thePlayer.motionY > 0.003) {
                            mc.thePlayer.motionY = 0.575f;
                        } else {
                            MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.8);
                        }
                    } else {
                        if (waitingForBowPullTick) {
                            ItemStack bowItemStack = mc.thePlayer.inventory.getStackInSlot(bowSlot);
                            if (bowItemStack != null && bowItemStack.getItem() instanceof ItemBow) {
                                PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(bowItemStack));
                            }
                            waitingForBowPullTick = false;
                        }

                        if (ticks == bowPullTicks.getValue().intValue()) {
                            event.setPitch(-89.93F);
                            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, new BlockPos(-1, -1, -1), EnumFacing.DOWN));
                            mc.thePlayer.stopUsingItem();
                        }
                        if (mc.thePlayer.hurtTime != 0) {
                            damagedBow = true;
                        }
                    }
                }
                break;
            case "Bloxd":
                if (event.isPre()) {
                    if (!bloxdFlying) {
                        event.setPitch(-90.0F);

                        if (waitingForBowPullTick) {
                            ItemStack bowItemStack = mc.thePlayer.inventory.getStackInSlot(bowSlot);
                            if (bowItemStack != null && bowItemStack.getItem() instanceof ItemBow) {
                                PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(bowItemStack));
                            }
                            waitingForBowPullTick = false;
                        }

                        if (mc.thePlayer.ticksExisted - bowModuleEnableTicks == bowPullTicks.getValue().intValue()) {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, -89.5f, true));
                            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                            mc.thePlayer.stopUsingItem();
                        }
                    } else {
                        if (mc.gameSettings.keyBindJump.isKeyDown()) {
                            mc.thePlayer.motionY = bloxdVerticalSpeed.getValue();
                        } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                            mc.thePlayer.motionY = -bloxdVerticalSpeed.getValue();
                        } else {
                            mc.thePlayer.motionY = 0;
                        }
                        if (flightTimer.hasTimeElapsed(bloxdFlyTime.getValue().longValue())) {
                            toggle();
                        }
                    }
                }
                break;
        }
        if (mode.is("AGC") || mode.is("Bloxd")) {
            ticks++;
        } else if (!mode.is("Watchdog")) {
            ticks++;
        }
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (mode.is("Bloxd") && !bloxdFlying) {
        }
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (mode.is("Bloxd")) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) packet;
                if (s12.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (!bloxdFlying) {
                        bloxdFlying = true;
                        hasTakenBowDamage = true;
                        flightTimer.reset();
                        mc.thePlayer.stopUsingItem();
                        isBloxdFlying = true;

                    }
                    event.cancel();
                }
            }
        }
    }

    @Override
    public void onMoveEvent(MoveEvent event) {
        if (!damagedBow && mode.is("AGC")) {
            event.setX(0);
            event.setZ(0);
        }
        if (!damaged && mode.is("Watchdog") && watchdogMode.is("Damage")) {
            event.setSpeed(0);
        }
        if (mode.is("Bloxd")) {
            if (!bloxdFlying && !hasTakenBowDamage) {
                event.setX(0);
                event.setZ(0);
                MovementUtils.setSpeed(0);
            } else {
                MovementUtils.setSpeed(MovementUtils.isMoving() ? bloxdHorizontalSpeed.getValue().floatValue() : 0);
            }
        }
    }

    public int getBowSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack is = mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && is.getItem() instanceof ItemBow) {
                return i;
            }
        }
        return -1;
    }

    public int getItemCount(Item item) {
        int count = 0;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == item) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            toggleSilent();
            return;
        }
        prevSlot = mc.thePlayer.inventory.currentItem;
        ticks = 0;
        damagedBow = false;
        damaged = false;
        jumpTimer.reset();
        x = mc.thePlayer.posX;
        y = mc.thePlayer.posY;
        z = mc.thePlayer.posZ;
        packets.clear();
        stage = 0;
        speed = 1.4f;
        mc.timer.timerSpeed = 1.0F;
        waitingForBowPullTick = false;

        if (mode.is("AGC")) {
            pitch = MathUtils.getRandomFloat(-89.2F, -89.99F);
            bowSlot = getBowSlot();
            if (bowSlot == -1) {
                this.toggleSilent();
                NotificationManager.post(NotificationType.DISABLE, "LongJump", "AGC: No bow found!");
                return;
            } else if (getItemCount(Items.arrow) == 0) {
                this.toggleSilent();
                NotificationManager.post(NotificationType.DISABLE, "LongJump", "AGC: No arrows found!");
                return;
            }
            waitingForBowPullTick = true;
        } else if (mode.is("Bloxd")) {
            bowSlot = getBowSlot();
            if (bowSlot == -1) {
                this.toggleSilent();
                NotificationManager.post(NotificationType.DISABLE, "LongJump", "Bloxd: No bow found!");
                return;
            } else if (getItemCount(Items.arrow) == 0) {
                this.toggleSilent();
                NotificationManager.post(NotificationType.DISABLE, "LongJump", "Bloxd: No arrows found!");
                return;
            }

            bloxdFlying = false;
            hasTakenBowDamage = false;
            bowModuleEnableTicks = mc.thePlayer.ticksExisted;
            isBloxdFlying = false;

            waitingForBowPullTick = true;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1;
        packets.forEach(PacketUtils::sendPacketNoEvent);
        packets.clear();
        if (mc.thePlayer != null) {
            mc.thePlayer.stopUsingItem();
        }

        if (mode.is("Bloxd")) {
            isBloxdFlying = false;
        }

        super.onDisable();
    }

    public LongJump() {
        super("LongJump", Category.MOVEMENT, "jump further");
        watchdogMode.addParent(mode, m -> m.is("Watchdog"));
        damageSpeed.addParent(mode, m -> m.is("Watchdog") && watchdogMode.is("Damage"));
        this.addSettings(mode, watchdogMode, damageSpeed, spoofY);

        bloxdHorizontalSpeed.addParent(mode, m -> m.is("Bloxd"));
        bloxdVerticalSpeed.addParent(mode, m -> m.is("Bloxd"));
        bowPullTicks.addParent(mode, m -> m.is("Bloxd"));
        bloxdFlyTime.addParent(mode, m -> m.is("Bloxd"));
        this.addSettings(bloxdHorizontalSpeed, bloxdVerticalSpeed, bowPullTicks, bloxdFlyTime);
    }
}