package dev.merguru.module.impl.render;

import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.BooleanSetting;
import dev.merguru.module.settings.impl.ModeSetting;
import dev.merguru.module.settings.impl.NumberSetting;

public class ScoreboardMod extends Module {

    public static final NumberSetting yOffset = new NumberSetting("Y Offset", 0, 250, 1, 5);
    public static final ModeSetting fontMode = new ModeSetting("Font Mode", "Minecraft", "Tenacity", "Inter", "Minecraft");
    public static final BooleanSetting bold = new BooleanSetting("Bold", false);
    public static final NumberSetting fontScale = new NumberSetting("Font Size", 18, 24, 16, 1);
    public static final BooleanSetting textShadow = new BooleanSetting("Text Shadow", true);
    public static final BooleanSetting redNumbers = new BooleanSetting("Red Numbers", false);

    public ScoreboardMod() {
        super("Scoreboard", Category.RENDER, "Scoreboard preferences");
        bold.addParent(fontMode, modeSetting -> !modeSetting.is("Minecraft"));
        fontScale.addParent(fontMode, modeSetting -> !modeSetting.is("Minecraft"));
        this.addSettings(yOffset, fontMode, bold, fontScale, textShadow, redNumbers);
        this.setToggled(true);
    }

}