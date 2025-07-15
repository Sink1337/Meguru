package dev.merguru.module.impl.misc;

import dev.merguru.event.impl.network.PacketReceiveEvent;
import dev.merguru.event.impl.network.PacketSendEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.server.S3APacketTabComplete;

@SuppressWarnings("unused")
public final class AntiTabComplete extends Module {

    public AntiTabComplete() {
        super("AntiTabComplete", Category.MISC, "prevents you from tab completing");
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (event.getPacket() instanceof C14PacketTabComplete) {
            event.cancel();
        }
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S3APacketTabComplete) {
            event.cancel();
        }
    }

}
