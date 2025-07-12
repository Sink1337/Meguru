package dev.tenacity.module.impl.render;

import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.NumberSetting;

public class MotionBlur extends Module {
    public final NumberSetting blurAmount = new NumberSetting("Amount", 7.0, 10.0, 0.0, 0.1);
    public MotionBlur(){
        super("MotionBlur", Category.RENDER,"MotionBlur");
        addSettings(blurAmount);
    }
}