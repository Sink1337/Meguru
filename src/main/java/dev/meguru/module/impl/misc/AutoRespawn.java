package dev.meguru.module.impl.misc;

import dev.meguru.event.impl.game.TickEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.utils.server.PacketUtils;
import net.minecraft.network.play.client.C16PacketClientStatus;

public final class AutoRespawn extends Module {

    @Override
    public void onTickEvent(TickEvent event) {
        if (mc.thePlayer != null && mc.thePlayer.isDead) {
            PacketUtils.sendPacketNoEvent(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
        }
    }

    public AutoRespawn() {
        super("AutoRespawn", Category.MISC, "automatically respawn");
    }

}
