package dev.meguru.module.impl.player;

import dev.meguru.event.impl.player.SafeWalkEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;

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
