package dev.merguru.module.impl.player;

import dev.merguru.event.impl.player.SafeWalkEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;

public final class SafeWalk extends Module {
    @Override
    public void onSafeWalkEvent(SafeWalkEvent e) {
        if (mc.thePlayer == null) return;
        e.setSafe(true);
    }

    public SafeWalk() {
        super("SafeWalk", Category.PLAYER, "prevents walking off blocks");
    }

}
