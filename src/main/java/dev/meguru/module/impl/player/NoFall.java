package dev.meguru.module.impl.player;

import dev.meguru.Meguru;
import dev.meguru.event.impl.player.BoundingBoxEvent;
import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.impl.combat.KillAura;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.utils.player.PlayerUtils;
import dev.meguru.utils.server.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;

@SuppressWarnings("unused")
public final class NoFall extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet", "Verus", "BlocksMC");
    private double dist;
    private boolean doNofall;
    private double lastFallDistance;
    private boolean c04;
    public boolean spoof;

    public NoFall() {
        super("NoFall", Category.PLAYER, "prevents fall damage");
        this.addSettings(mode);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (event.isPre()) {
            this.setSuffix(mode.getMode());

            if (mode.is("BlocksMC")) {
                if (mc.thePlayer.fallDistance > 3.5 && !PlayerUtils.overVoid() && Meguru.INSTANCE.getModuleCollection().getModule(KillAura.class).isEnabled()) {
                    mc.thePlayer.fallDistance = 0;
                    mc.timer.timerSpeed = 0.5F;
                    spoof = true;
                } else if (spoof) {
                    mc.timer.timerSpeed = 1.0F;
                    spoof = false;
                }
            }


            if (mc.thePlayer.fallDistance > 3.0 && isBlockUnder()) {
                switch (mode.getMode()) {
                    case "Vanilla":
                        event.setOnGround(true);
                        break;
                    case "Packet":
                        PacketUtils.sendPacket(new C03PacketPlayer(true));
                        break;
                }
                if (!mode.is("BlocksMC")) {
                    mc.thePlayer.fallDistance = 0;
                }
            }
        }
    }

    @Override
    public void onBoundingBoxEvent(BoundingBoxEvent event) {
        if (mode.is("Verus") && mc.thePlayer.fallDistance > 2) {
            final AxisAlignedBB axisAlignedBB = AxisAlignedBB.fromBounds(-5, -1, -5, 5, 1, 5).offset(event.getBlockPos().getX(), event.getBlockPos().getY(), event.getBlockPos().getZ());
            event.setBoundingBox(axisAlignedBB);
        }
    }

    private boolean isBlockUnder() {
        if (mc.thePlayer.posY < 0) return false;
        for (int offset = 0; offset < (int) mc.thePlayer.posY + 2; offset += 2) {
            AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, -offset, 0);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (spoof) {
            mc.timer.timerSpeed = 1.0F;
            spoof = false;
        }
        super.onDisable();
    }
}