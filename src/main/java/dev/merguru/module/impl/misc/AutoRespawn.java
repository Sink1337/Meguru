package dev.merguru.module.impl.misc;

import dev.merguru.event.impl.game.TickEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.utils.server.PacketUtils;
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
