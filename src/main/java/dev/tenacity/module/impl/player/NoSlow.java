package dev.tenacity.module.impl.player;

import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.PreInputEvent;
import dev.tenacity.event.impl.player.SlowDownEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.server.PacketUtils;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class NoSlow extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Watchdog", "Vanilla", "NCP", "Watchdog", "BlocksMC");
    private int tick;

    public NoSlow() {
        super("NoSlow", Category.PLAYER, "prevent item slowdown");
        this.addSettings(mode);
    }

    @Override
    public void onSlowDownEvent(SlowDownEvent event) {
        if (mode.is("Hypixel")) {
            if (mode.is("BlocksMC") && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                event.cancel();
            }
        } else {
            event.cancel();
        }

        if (mode.getMode().equalsIgnoreCase("BlocksMC") && mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            event.cancel();
        }
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        this.setSuffix(mode.getMode());

        if (!mc.thePlayer.isUsingItem()) {
            tick = 0;
        }

        switch (mode.getMode()) {
            case "NCP":
                if (MovementUtils.isMoving() && mc.thePlayer.isUsingItem()) {
                    if (e.isPre()) {
                        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    } else {
                        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getCurrentEquippedItem()));
                    }

                }
                break;
            case "BlocksMC":
                if (mc.thePlayer.isUsingItem()) {
                    if (mc.thePlayer.getHeldItem() != null && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                        if (e.isPre()) {
                            if (tick == 0) {
                                PacketUtils.sendPacket(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9));
                                PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 0, mc.thePlayer.getHeldItem(), 0, 0, 0));
                            }
                        }
                    }
                    tick++;
                }
                break;
        }
    }

    @Override
    public void onPreInput(PreInputEvent e) {
        if (mc.thePlayer.isUsingItem() && mode.getMode().equalsIgnoreCase("BlocksMC")) {
            if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                mc.thePlayer.movementInput.moveStrafe *= 0.2f;
                mc.thePlayer.movementInput.moveForward *= 0.2f;
            }
        }
    }
}