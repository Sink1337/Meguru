package dev.meguru.module.impl.misc;

import dev.meguru.event.impl.network.PacketReceiveEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.utils.player.ChatUtil;
import net.minecraft.network.play.server.S29PacketSoundEffect;

public final class LightningTracker extends Module {

    public LightningTracker() {
        super("LightningTracker", Category.MISC, "detects lightning");
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect soundPacket = ((S29PacketSoundEffect) event.getPacket());
            if (soundPacket.getSoundName().equals("ambient.weather.thunder")) {
                ChatUtil.print(String.format("Lightning detected at (%s, %s, %s)", (int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ()));
            }
        }
    }

}
