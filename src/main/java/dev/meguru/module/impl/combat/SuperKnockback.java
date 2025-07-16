package dev.meguru.module.impl.combat;

import dev.meguru.event.impl.player.AttackEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.utils.server.PacketUtils;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public final class SuperKnockback extends Module {

    public SuperKnockback() {
        super("SuperKnockback", Category.COMBAT, "Makes the player your attacking take extra knockback");
    }

    @Override
    public void onAttackEvent(AttackEvent event) {
        if (event.getTargetEntity() != null) {
            if (mc.thePlayer.isSprinting())
                PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));

            PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
        }
    }
}
