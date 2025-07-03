package dev.tenacity.module.impl.movement;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.MoveEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.combat.TargetStrafe;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
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
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import dev.tenacity.ui.notifications.NotificationManager;
import dev.tenacity.ui.notifications.NotificationType;

import java.util.ArrayList;
import java.util.List;

public final class LongJump extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Watchdog", "NCP", "AGC", "Bloxd");
    private final ModeSetting watchdogMode = new ModeSetting("Watchdog Mode", "Damage", "Damage", "Damageless");
    private final NumberSetting damageSpeed = new NumberSetting("Damage Speed", 1, 20, 1, 0.5);
    private final BooleanSetting spoofY = new BooleanSetting("Spoof Y", false);

    private final ModeSetting bloxdSubMode = new ModeSetting("Bloxd Mode", "Bow", "Bow", "FireBall");

    private final NumberSetting bloxdHorizontalSpeed = new NumberSetting("Bloxd Horizontal Speed", 0.3, 5, 0, 0.1);
    private final NumberSetting bloxdVerticalSpeed = new NumberSetting("Bloxd Vertical Speed", 0, 2, -1, 0.05);

    private final NumberSetting bowReleaseTime = new NumberSetting("Bow Release Time", 3, 20, 1, 1);
    private final NumberSetting explosionDetectRadius = new NumberSetting("Explosion Detect Radius", 3, 10, 1, 0.5);

    private final BooleanSetting stopMovement = new BooleanSetting("Stop Movement", true);


    public static boolean isBloxdFlying = false;

    private int movementTicks = 0;
    private double speed;
    private float pitch;
    private int prevSlot;
    private int ticks;

    private boolean damagedItem;
    private final TimerUtil jumpTimer = new TimerUtil();
    private final TimerUtil flightTimer = new TimerUtil();

    private boolean damaged;
    private final List<Packet> packets = new ArrayList<>();
    private int stage;

    private int currentItemSlot = -1;
    private int originalSlotId;

    public LongJump() {
        super("LongJump", Category.MOVEMENT, "jump further");
        watchdogMode.addParent(mode, m -> m.is("Watchdog"));
        damageSpeed.addParent(mode, m -> m.is("Watchdog") && watchdogMode.is("Damage"));
        spoofY.addParent(mode, m -> m.is("Watchdog") && watchdogMode.is("Damage"));

        bloxdSubMode.addParent(mode, m -> m.is("Bloxd"));

        bloxdHorizontalSpeed.addParent(mode, m -> m.is("Bloxd"));
        bloxdVerticalSpeed.addParent(mode, m -> m.is("Bloxd"));

        bowReleaseTime.addParent(mode, m -> m.is("AGC") || (m.is("Bloxd") && bloxdSubMode.is("Bow")));

        explosionDetectRadius.addParent(mode, m -> m.is("Bloxd") && bloxdSubMode.is("FireBall"));

        stopMovement.addParent(mode, m -> m.is("Bloxd"));

        this.addSettings(mode, watchdogMode, damageSpeed, spoofY, bloxdSubMode, bloxdHorizontalSpeed, bloxdVerticalSpeed, bowReleaseTime, explosionDetectRadius, stopMovement);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        damagedItem = false;
        damaged = false;
        jumpTimer.reset();
        flightTimer.reset();
        packets.clear();
        stage = 0;
        speed = 1.4f;

        isBloxdFlying = false;
        originalSlotId = mc.thePlayer.inventory.currentItem;


        switch (mode.getMode()) {
            case "AGC":
                prevSlot = mc.thePlayer.inventory.currentItem;
                pitch = MathUtils.getRandomFloat(-89.99F, -89.2F);
                if (getBowSlot() == -1) {
                    NotificationManager.post(NotificationType.WARNING, "LongJump", "No bow found!", 2);
                    this.toggleSilent();
                    return;
                } else if (getItemCount(Items.arrow) == 0) {
                    NotificationManager.post(NotificationType.WARNING, "LongJump", "No arrows found!", 2);
                    this.toggleSilent();
                    return;
                }
                break;
            case "Bloxd":
                Item requiredItem = bloxdSubMode.is("Bow") ? Items.bow : Items.fire_charge;
                String itemName = bloxdSubMode.is("Bow") ? "Bow" : "Fire Charge";
                String noItemMessage = "Bloxd " + itemName + ": No " + itemName.toLowerCase() + " found" + (bloxdSubMode.is("FireBall") ? " in hotbar!" : "!");

                if (bloxdSubMode.is("Bow") && !mc.thePlayer.inventory.hasItem(Items.arrow)) {
                    NotificationManager.post(NotificationType.WARNING, "LongJump", "Bloxd Bow: No arrows found!", 2);
                    this.toggleSilent();
                    return;
                }

                currentItemSlot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
                    if (itemStack != null && (itemStack.getItem() == requiredItem || (bloxdSubMode.is("Bow") && itemStack.getItem() instanceof ItemBow))) {
                        currentItemSlot = i;
                        break;
                    }
                }

                if (currentItemSlot == -1) {
                    NotificationManager.post(NotificationType.WARNING, "LongJump", noItemMessage, 2);
                    this.toggleSilent();
                    return;
                } else {
                    if (currentItemSlot != originalSlotId) {
                        PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(currentItemSlot));
                    }
                    PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(currentItemSlot)));
                    ticks = mc.thePlayer.ticksExisted;
                }
                break;
        }
        super.onEnable();
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        setSuffix(mode.getMode() + (mode.is("Bloxd") ? " " + bloxdSubMode.getMode() : ""));
        if (mode.is("Watchdog") && watchdogMode.is("Damage") && spoofY.isEnabled()) mc.thePlayer.posY = mc.thePlayer.lastTickPosY;
        TargetStrafe targetStrafeModule = Tenacity.INSTANCE.getModuleCollection().getModule(TargetStrafe.class);
        boolean isTargetStrafeActive = (targetStrafeModule != null && targetStrafeModule.active);

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
                int bowSlot = getBowSlot();

                if (damagedItem) {
                    isBloxdFlying = false;
                    if (mc.thePlayer.onGround && jumpTimer.hasTimeElapsed(1000)) {
                        toggle();
                    } else {
                        if (flightTimer.hasTimeElapsed(3000)) {
                            toggle();
                            return;
                        }
                    }
                    if (mc.thePlayer.onGround && mc.thePlayer.motionY > 0.003) {
                        mc.thePlayer.motionY = 0.575f;
                    } else {
                        MovementUtils.setSpeed(MovementUtils.getBaseMoveSpeed() * 1.8);
                    }
                } else {
                    isBloxdFlying = false;
                    if (event.isPre()) {
                        if (prevSlot != bowSlot) {
                            PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(bowSlot));
                        }
                        PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(bowSlot)));
                        if (mc.thePlayer.ticksExisted - ticks == bowReleaseTime.getValue()) {
                            event.setPitch(-89.5F);
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, -89.5f, true));
                            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, new BlockPos(-1, -1, -1), EnumFacing.DOWN));
                            if (prevSlot != bowSlot) {
                                PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(prevSlot));
                            }
                        }
                        if (mc.thePlayer.hurtTime != 0) {
                            damagedItem = true;
                            flightTimer.reset();
                        }
                    }
                }
                break;
            case "Bloxd":
                isBloxdFlying = true;
                if (!damagedItem) {
                    if (event.isPre()) {
                        float targetPitch = bloxdSubMode.is("Bow") ? -89.5F : 89.5F;
                        event.setPitch(targetPitch);
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, targetPitch, true));

                        if (bloxdSubMode.is("Bow") && mc.thePlayer.ticksExisted - ticks == bowReleaseTime.getValue()) {
                            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, new BlockPos(0, 0, 0), EnumFacing.DOWN));
                        }
                        if (mc.thePlayer.hurtTime > 0) {
                            damagedItem = true;
                            flightTimer.reset();
                            NotificationManager.post(NotificationType.INFO, "LongJump", "Bloxd " + bloxdSubMode.getMode() + ": Damaged! Flying!", 1);
                        }
                    }
                } else {
                    if (flightTimer.hasTimeElapsed(1000)) {
                        toggle();
                        return;
                    }

                    event.setOnGround(false);
                    mc.thePlayer.fallDistance = 0;

                    mc.thePlayer.motionY = isTargetStrafeActive ? 0 : mc.gameSettings.keyBindJump.isKeyDown() ? bloxdVerticalSpeed.getValue() : mc.gameSettings.keyBindSneak.isKeyDown() ? -bloxdVerticalSpeed.getValue() : 0;

                    if (currentItemSlot != -1 && currentItemSlot != originalSlotId) {
                        PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(originalSlotId));
                        currentItemSlot = originalSlotId;
                    }
                }
                break;
        }
        if (!mode.is("Bloxd") && !mode.is("Watchdog")) {
            ticks++;
        }
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (mode.is("Bloxd") && stopMovement.isEnabled() && !damagedItem && event.getPacket() instanceof C03PacketPlayer) {
            event.cancel();
        }
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (mode.is("Bloxd") && bloxdSubMode.is("FireBall") && event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packetExplosion = (S27PacketExplosion) event.getPacket();
            Vec3 explosionPos = new Vec3(packetExplosion.getX(), packetExplosion.getY(), packetExplosion.getZ());
            Vec3 playerPos = mc.thePlayer.getPositionVector();

            double distance = playerPos.distanceTo(explosionPos);

            if (distance <= explosionDetectRadius.getValue() && !damagedItem) {
                damagedItem = true;
                flightTimer.reset();
                NotificationManager.post(NotificationType.INFO, "LongJump", "Bloxd FireBall: Explosion detected! Flying!", 1);
            }
        }
    }

    @Override
    public void onMoveEvent(MoveEvent event) {
        if (mode.is("Bloxd")) {
            if (!damagedItem) {
                if (stopMovement.isEnabled()) {
                    event.setX(0);
                    event.setZ(0);
                }
            } else {
                event.setSpeed(MovementUtils.isMoving() ? bloxdHorizontalSpeed.getValue().floatValue() : 0);
            }
        } else if (mode.is("AGC") && !damagedItem) {
            event.setX(0);
            event.setZ(0);
        } else if (mode.is("Watchdog") && watchdogMode.is("Damage") && !damaged) {
            event.setSpeed(0);
        }
    }

    public int getBowSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack is = mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && is.getItem() == Items.bow) {
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
    public void onDisable() {
        mc.timer.timerSpeed = 1;
        packets.forEach(PacketUtils::sendPacketNoEvent);
        packets.clear();
        isBloxdFlying = false;
        if (mode.is("Bloxd") && currentItemSlot != -1 && originalSlotId != currentItemSlot) {
            PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(originalSlotId));
        }
        super.onDisable();
    }
}