package dev.meguru.module.impl.movement;

import dev.meguru.Meguru;
import dev.meguru.event.impl.game.TickEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.impl.BooleanSetting;

public class Sprint extends Module {

    private final BooleanSetting omniSprint = new BooleanSetting("Omni Sprint", false);

    public Sprint() {
        super("Sprint", Category.MOVEMENT, "Sprints automatically");
        this.addSettings(omniSprint);
    }


    @Override
    public void onTickEvent(TickEvent event) {
        if (mc.thePlayer == null) return;
        if (omniSprint.isEnabled()) {
            mc.thePlayer.setSprinting(true);
        } else {
            if (mc.thePlayer.isUsingItem()) {
                if (mc.thePlayer.moveForward > 0 && (Meguru.INSTANCE.isEnabled(NoSlow.class) || !mc.thePlayer.isUsingItem()) && !mc.thePlayer.isSneaking() && !mc.thePlayer.isCollidedHorizontally && mc.thePlayer.getFoodStats().getFoodLevel() > 6) {
                    mc.thePlayer.setSprinting(true);
                }
            } else {
                mc.gameSettings.keyBindSprint.pressed = true;
            }
        }
    }

    @Override
    public void onDisable() {
        mc.thePlayer.setSprinting(false);
        mc.gameSettings.keyBindSprint.pressed = false;
        super.onDisable();
    }


}
