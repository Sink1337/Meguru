package dev.merguru.module.impl.misc;

import dev.merguru.event.impl.network.PacketReceiveEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.BooleanSetting;
import dev.merguru.module.settings.impl.ModeSetting;
import dev.merguru.utils.server.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public final class NoRotate extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Cancel");
    private final BooleanSetting fakeUpdate = new BooleanSetting("Fake Update", false);

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent e) {
        if (mc.thePlayer == null) return;
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) e.getPacket();
            switch (mode.getMode()) {
                case "Normal":
                    packet.setYaw(mc.thePlayer.rotationYaw);
                    packet.setPitch(mc.thePlayer.rotationPitch);
                    break;
                case "Cancel":
                    e.cancel();
                    break;
            }

            if (fakeUpdate.isEnabled()) {
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(
                        mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                        packet.getYaw(), packet.getPitch(), mc.thePlayer.onGround));
            }
        }
    }

    public NoRotate() {
        super("NoRotate", Category.MISC, "Prevents servers from rotating you");
        this.addSettings(fakeUpdate);
    }

}
