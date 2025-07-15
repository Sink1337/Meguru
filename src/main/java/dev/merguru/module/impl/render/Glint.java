package dev.merguru.module.impl.render;

import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.ColorSetting;
import dev.merguru.module.settings.impl.ModeSetting;
import dev.merguru.utils.render.ColorUtil;
import dev.merguru.utils.tuples.Pair;

import java.awt.*;

public class Glint extends Module {

    public final ModeSetting colorMode = new ModeSetting("Color Mode", "Sync", "Sync", "Custom");
    public final ColorSetting color = new ColorSetting("Color", Color.PINK);

    public Glint() {
        super("Glint", Category.RENDER, "Colors the enchantment glint");
        color.addParent(colorMode, modeSetting -> modeSetting.is("Custom"));
        addSettings(colorMode, color);
    }

    public Color getColor() {
        Color customColor = Color.WHITE;
        switch (colorMode.getMode()) {
            case "Sync":
                Pair<Color, Color> colors = HUDMod.getClientColors();
                if (HUDMod.isRainbowTheme()) {
                    customColor = colors.getFirst();
                } else {
                    customColor = ColorUtil.interpolateColorsBackAndForth(20, 1, colors.getFirst(), colors.getSecond(), false);
                }
                break;
            case "Custom":
                customColor = color.getColor();
                break;
        }
        return customColor;
    }

}
