package dev.meguru.module.impl.combat;

import dev.meguru.event.impl.game.WorldEvent;
import dev.meguru.event.impl.network.PacketReceiveEvent;
import dev.meguru.event.impl.network.PacketSendEvent;
import dev.meguru.event.impl.player.EventMoveInput;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.Setting;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.module.settings.impl.NumberSetting;
import dev.meguru.ui.notifications.NotificationManager;
import dev.meguru.ui.notifications.NotificationType;
import dev.meguru.utils.misc.MathUtils;
import dev.meguru.utils.player.MovementUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;

public class Velocity extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Matrix", "Tick", "Stack", "C0F Cancel","Hypixel");
    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0, 100, 0, 1);
    private final NumberSetting vertical = new NumberSetting("Vertical", 0, 100, 0, 1);
    private final NumberSetting chance = new NumberSetting("Chance", 100, 100, 0, 1);
    private final BooleanSetting onlyWhileMoving = new BooleanSetting("Only while moving", false);
    private final BooleanSetting staffCheck = new BooleanSetting("Staff check", false);

    public final BooleanSetting jumpValue = new BooleanSetting("Jump Rest", true);

    private long lastDamageTimestamp, lastAlertTimestamp;
    private boolean cancel;
    private int stack;

    //hyp
    private int counter = 0;

    public Velocity() {
        super("Velocity", Category.COMBAT, "Reduces your knockback");
        Setting.addParent(mode, m -> m.is("Packet"), horizontal, vertical, staffCheck);
        jumpValue.addParent(mode, m -> m.is("Hypixel"));
        this.addSettings(mode,jumpValue, horizontal, vertical, chance, onlyWhileMoving, staffCheck);
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (mode.is("C0F Cancel")) {
            if (event.getPacket() instanceof C0FPacketConfirmTransaction && mc.thePlayer.hurtTime > 0) {
                event.cancel();
            }
        }
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent e) {
        this.setSuffix(mode.getMode());
        if ((onlyWhileMoving.isEnabled() && !MovementUtils.isMoving()) || (chance.getValue() != 100 && MathUtils.getRandomInRange(0, 100) > chance.getValue()))
            return;
        Packet<?> packet = e.getPacket();
        switch (mode.getMode()) {
            case "Packet":
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) e.getPacket();
                    if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.getEntityId()) {
                        if (cancel(e)) return;
                        s12.motionX *= horizontal.getValue() / 100.0;
                        s12.motionZ *= horizontal.getValue() / 100.0;
                        s12.motionY *= vertical.getValue() / 100.0;
                    }
                } else if (packet instanceof S27PacketExplosion) {
                    if (cancel(e)) return;
                    S27PacketExplosion s27 = (S27PacketExplosion) e.getPacket();
                    s27.motionX *= horizontal.getValue() / 100.0;
                    s27.motionZ *= horizontal.getValue() / 100.0;
                    s27.motionY *= vertical.getValue() / 100.0;
                } else if (e.getPacket() instanceof S19PacketEntityStatus) {
                    S19PacketEntityStatus s19 = (S19PacketEntityStatus) e.getPacket();
                    if (mc.thePlayer != null && s19.getEntityId() == mc.thePlayer.getEntityId() && s19.getOpCode() == 2) {
                        lastDamageTimestamp = System.currentTimeMillis();
                    }
                }
                break;
            case "C0F Cancel":
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) e.getPacket();
                    if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.getEntityId()) {
                        e.cancel();
                    }
                }
                if (packet instanceof S27PacketExplosion) {
                    e.cancel();
                }
                break;
            case "Stack":
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) packet;
                    cancel = !cancel;
                    if (cancel) {
                        e.cancel();
                    }
                }
                if (packet instanceof S27PacketExplosion) {
                    e.cancel();
                }
                break;
            case "Matrix":
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) e.getPacket();
                    if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.getEntityId()) {
                        s12.motionX *= 5 / 100.0;
                        s12.motionZ *= 5 / 100.0;
                        s12.motionY *= 100 / 100.0;
                    }
                }
                break;
            case "Tick":
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) e.getPacket();
                    if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.getEntityId() && mc.thePlayer.ticksExisted % 3 == 0) {
                        s12.motionX *= 5 / 100.0;
                        s12.motionZ *= 5 / 100.0;
                        s12.motionY *= 100 / 100.0;
                    }
                }
                break;
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        stack = 0;
    }

    @Override
    public void onEventMoveInput(EventMoveInput event) {
        if (mode.is("Hypixel")) {
            if (this.jumpValue.isEnabled() && mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround && this.counter++ % 2 == 0) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    private boolean cancel(PacketReceiveEvent e) {
        if (staffCheck.isEnabled() && System.currentTimeMillis() - lastDamageTimestamp > 500) {
            if (System.currentTimeMillis() - lastAlertTimestamp > 250) {
                NotificationManager.post(NotificationType.WARNING, "Velocity", "Suspicious knockback detected!", 2);
                lastAlertTimestamp = System.currentTimeMillis();
            }
            return true;
        }
        if (horizontal.getValue() == 0 && vertical.getValue() == 0) {
            e.cancel();
            return true;
        }
        return false;
    }

}
