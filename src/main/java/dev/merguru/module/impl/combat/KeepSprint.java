package dev.merguru.module.impl.combat;

import dev.merguru.event.impl.player.KeepSprintEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;

public final class KeepSprint extends Module {

    public KeepSprint() {
        super("KeepSprint", Category.COMBAT, "Stops sprint reset after hitting");
    }

    @Override
    public void onKeepSprintEvent(KeepSprintEvent event) {
        event.cancel();
    }

}
