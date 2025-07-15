package dev.merguru.module.impl.render;

import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.NumberSetting;

public class MotionBlur extends Module {
    public final NumberSetting blurAmount = new NumberSetting("Amount", 7.0, 10.0, 0.0, 0.1);
    public MotionBlur(){
        super("MotionBlur", Category.RENDER,"MotionBlur");
        addSettings(blurAmount);
    }
}