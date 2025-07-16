package dev.meguru.module.impl.render;

import dev.meguru.event.impl.render.HurtCamEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;

public class NoHurtCam extends Module {

    public NoHurtCam() {
        super("NoHurtCam", Category.RENDER, "removes shaking after being hit");
    }

    @Override
    public void onHurtCamEvent(HurtCamEvent e) {
        e.cancel();
    }

}
