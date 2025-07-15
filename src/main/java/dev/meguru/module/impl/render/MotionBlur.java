package dev.meguru.module.impl.render;

import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.impl.NumberSetting;

public class MotionBlur extends Module {
    public final NumberSetting blurAmount = new NumberSetting("Amount", 7.0, 10.0, 0.0, 0.1);
    public MotionBlur(){
        super("MotionBlur", Category.RENDER,"MotionBlur");
        addSettings(blurAmount);
    }
}