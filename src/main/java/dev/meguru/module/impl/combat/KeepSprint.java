package dev.meguru.module.impl.combat;

import dev.meguru.event.impl.player.KeepSprintEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;

public final class KeepSprint extends Module {

    public KeepSprint() {
        super("KeepSprint", Category.COMBAT, "Stops sprint reset after hitting");
    }

    @Override
    public void onKeepSprintEvent(KeepSprintEvent event) {
        event.cancel();
    }

}
