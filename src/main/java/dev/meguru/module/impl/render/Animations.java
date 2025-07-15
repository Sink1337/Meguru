package dev.meguru.module.impl.render;

import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.module.settings.impl.NumberSetting;

public final class Animations extends Module {

    public static final ModeSetting mode = new ModeSetting("Mode", "Swank",
            "Swank", "Swing", "Swang", "Swong", "Swaing", "Punch", "Virtue", "Push", "Stella", "Styles", "Slide", "Interia", "Ethereal", "1.7", "Sigma", "Spinning", "Exhibition", "Old Exhibition", "Smooth", "Moon", "Leaked", "Astolfo", "Small");
    public static final NumberSetting slowdown = new NumberSetting("Slow Down", 0, 15, -5, 1);
    public static final BooleanSetting oldDamage = new BooleanSetting("Old Damage", false);
    public static final BooleanSetting smallSwing = new BooleanSetting("Small Swing", false);
    public static final NumberSetting x = new NumberSetting("X", 0, 50, -50, 1);
    public static final NumberSetting y = new NumberSetting("Y", 0, 50, -50, 1);
    public static final NumberSetting z = new NumberSetting("Z", 0, 50, -50, 1);
    public static final NumberSetting size = new NumberSetting("Size", 0, 50, -50, 1);

    public static final NumberSetting blockingX = new NumberSetting("Blocking X", 0, 50, -50, 1);
    public static final NumberSetting blockingY = new NumberSetting("Blocking Y", 0, 50, -50, 1);
    public static final NumberSetting blockingZ = new NumberSetting("Blocking Z", 0, 50, -50, 1);
    public static final NumberSetting blockingSize = new NumberSetting("Blocking Size", 0, 50, -50, 1);


    public Animations() {
        super("Animations", Category.RENDER, "changes animations");
        this.addSettings(x, y, z, size, blockingX, blockingY, blockingZ, blockingSize, smallSwing, mode, slowdown, oldDamage);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        this.setSuffix(mode.getMode());
    }

}