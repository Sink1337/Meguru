package dev.meguru.module.impl.movement;

import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.impl.ModeSetting;

public final class Spider extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Verus");

    public Spider() {
        super("Spider", Category.MOVEMENT, "Climbs you up walls like a spider");
        addSettings(mode);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        setSuffix(mode.getMode());
        if (mc.thePlayer.isCollidedHorizontally) {
            if (!mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically) return;
            switch (mode.getMode()) {
                case "Vanilla":
                    mc.thePlayer.jump();
                    break;
                case "Verus":
                    if (mc.thePlayer.ticksExisted % 3 == 0)
                        mc.thePlayer.motionY = 0.42f;
                    break;
            }
        }
    }
}
