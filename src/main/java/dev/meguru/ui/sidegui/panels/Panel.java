package dev.meguru.ui.sidegui.panels;

import dev.meguru.module.impl.render.HUDMod;
import dev.meguru.ui.Screen;
import dev.meguru.utils.render.ColorUtil;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public abstract class Panel implements Screen {
    private float x, y, width, height, alpha;

    public Color getTextColor() {
        return ColorUtil.applyOpacity(Color.WHITE, alpha);
    }

    public Color getAccentColor() {
        return ColorUtil.applyOpacity(HUDMod.getClientColors().getFirst(), alpha);
    }

}
