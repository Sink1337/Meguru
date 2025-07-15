package dev.meguru.module.impl.combat;

import dev.meguru.Meguru;
import dev.meguru.event.impl.network.PacketSendEvent;
import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.event.impl.player.MoveEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.impl.movement.Flight;
import dev.meguru.module.impl.movement.Step;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.module.settings.impl.NumberSetting;
import dev.meguru.utils.player.BloxdPhysicsUtils;
import dev.meguru.utils.server.PacketUtils;
import dev.meguru.utils.time.TimerUtil;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C18PacketSpectate;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@SuppressWarnings("unused")
public final class Criticals extends Module {

    private boolean stage;
    private double offset;
    private int groundTicks;
    private final ModeSetting mode = new ModeSetting("Mode", "Watchdog", "Watchdog", "Packet", "Dev", "Verus", "Bloxd");
    private final ModeSetting watchdogMode = new ModeSetting("Watchdog Mode", "Packet", "Packet", "Edit");
    private final ModeSetting bloxdMode = new ModeSetting("Bloxd Mode", "LowHop", "LowHop", "Packet");
    private final NumberSetting delay = new NumberSetting("Delay", 1, 20, 0, 1);
    private final TimerUtil timer = new TimerUtil();
    private final BloxdPhysicsUtils.NoaPhysics bloxdPhysics = new BloxdPhysicsUtils.NoaPhysics();

    public Criticals() {
        super("Criticals", Category.COMBAT, "Crit attacks");
        delay.addParent(mode, m -> !(m.is("Verus") || (m.is("Watchdog") && watchdogMode.is("Edit"))));
        watchdogMode.addParent(mode, m -> m.is("Watchdog"));
        bloxdMode.addParent(mode, m -> m.is("Bloxd"));
        this.addSettings(mode, watchdogMode, bloxdMode, delay);
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent e) {
    }

    @Override
    public void onMoveEvent(MoveEvent e) {
        if (mode.is("Bloxd")) {
            if (KillAura.attacking && mc.thePlayer.onGround && !Step.isStepping) {
                if (TargetManager.target != null && TargetManager.target.hurtTime >= delay.getValue().intValue()) {
                    switch (bloxdMode.getMode()) {
                        case "LowHop":
                            bloxdPhysics.reset();
                            bloxdPhysics.getImpulseVector().add(0, 8, 0);
                            bloxdPhysics.getMotionForTick();
                            e.setY(bloxdPhysics.getVelocityVector().getY() / 30.0);
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        this.setSuffix(mode.getMode());
        switch (mode.getMode()) {
            case "Watchdog":
                if (watchdogMode.is("Packet")) {
                    if (KillAura.attacking && e.isOnGround() && !Step.isStepping) {
                        if (TargetManager.target != null && TargetManager.target.hurtTime >= delay.getValue().intValue()) {
                            for (double offset : new double[]{0.06f, 0.01f}) {
                                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset + (Math.random() * 0.001), mc.thePlayer.posZ, false));
                            }
                        }
                    }
                }
                if (e.isPre() && watchdogMode.is("Edit") && !Meguru.INSTANCE.isEnabled(Flight.class) && !Step.isStepping && KillAura.attacking) {
                    if (e.isOnGround()) {
                        groundTicks++;
                        if (groundTicks > 2) {
                            stage = !stage;
                            e.setY(e.getY() + (stage ? 0.015 : 0.01) - Math.random() * 0.0001);
                            e.setOnGround(false);
                        }
                    } else {
                        groundTicks = 0;
                    }
                }
                break;
            case "Packet":
                if (mc.objectMouseOver.entityHit != null && mc.thePlayer.onGround) {
                    if (mc.objectMouseOver.entityHit.hurtResistantTime > delay.getValue().intValue()) {
                        for (double offset : new double[]{0.006253453, 0.002253453, 0.001253453}) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false));
                        }
                    }
                }
                break;
            case "Dev":
                if (mc.objectMouseOver.entityHit != null && mc.thePlayer.onGround) {
                    if (mc.objectMouseOver.entityHit.hurtResistantTime > delay.getValue().intValue()) {
                        for (double offset : new double[]{0.06253453, 0.02253453, 0.001253453, 0.0001135346}) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false));
                            PacketUtils.sendPacketNoEvent(new C18PacketSpectate(UUID.randomUUID()));
                        }
                    }
                }
                break;
            case "Verus":
                if (KillAura.attacking && TargetManager.target != null && e.isOnGround()) {
                    switch (TargetManager.target.hurtResistantTime) {
                        case 17:
                        case 19:
                            e.setOnGround(false);
                            e.setY(e.getY() + ThreadLocalRandom.current().nextDouble(0.001, 0.0011));
                            break;
                        case 18:
                        case 20:
                            e.setOnGround(false);
                            e.setY(e.getY() + 0.03 + ThreadLocalRandom.current().nextDouble(0.001, 0.0011));
                            break;
                    }
                }
                break;
            case "Bloxd":
                if (KillAura.attacking && mc.thePlayer.onGround && !Step.isStepping) {
                    if (TargetManager.target != null && TargetManager.target.hurtTime >= delay.getValue().intValue()) {
                        if (bloxdMode.is("Packet")) {
                            for (double offset : new double[]{0.000005}) {
                                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false));
                            }
                        }
                    }
                }
                break;
        }
    }
}