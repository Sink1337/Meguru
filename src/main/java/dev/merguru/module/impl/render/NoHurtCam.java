package dev.merguru.module.impl.render;

import dev.merguru.event.impl.render.HurtCamEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;

public class NoHurtCam extends Module {

    public NoHurtCam() {
        super("NoHurtCam", Category.RENDER, "removes shaking after being hit");
    }

    @Override
    public void onHurtCamEvent(HurtCamEvent e) {
        e.cancel();
    }

}
