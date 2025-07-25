package dev.meguru.ui.sidegui.utils;

import dev.meguru.Meguru;
import dev.meguru.ui.Screen;
import dev.meguru.ui.sidegui.SideGUI;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.font.FontUtil;
import dev.meguru.utils.misc.HoveringUtil;
import dev.meguru.utils.render.ColorUtil;
import dev.meguru.utils.render.RenderUtil;
import dev.meguru.utils.render.RoundedUtil;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public class ToggleButton implements Screen {

    @Getter
    @Setter
    private float x, y, alpha;
    private boolean enabled;
    private final String name;
    private boolean bypass;
    private final float WH = 10;

    private final Animation toggleAnimation = new DecelerateAnimation(250, 1);

    public ToggleButton(String name, boolean initialState) {
        this.name = name;
        this.enabled = initialState;
    }

    public ToggleButton(String name) {
        this(name, false);
    }


    @Override
    public void initGui() {

    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        int textColor = ColorUtil.applyOpacity(-1, alpha);
        tenacityFont16.drawString(name, x - (tenacityFont16.getStringWidth(name) + 5), y + tenacityFont16.getMiddleOfBox(WH), textColor);

        toggleAnimation.setDirection(enabled ? Direction.FORWARDS : Direction.BACKWARDS);

        float toggleAnim = toggleAnimation.getOutput().floatValue();
        Color roundColor = ColorUtil.interpolateColorC(ColorUtil.tripleColor(64), Meguru.INSTANCE.getSideGui().getGreenEnabledColor(), toggleAnim);
        RoundedUtil.drawRound(x, y, WH, WH, WH / 2f - .25f, roundColor);

        if (enabled || !toggleAnimation.isDone()) {
            RenderUtil.scaleStart(x + getWH() / 2f, y + getWH() / 2f, toggleAnim);
            iconFont16.drawString(FontUtil.CHECKMARK, x + 1, y + 3.5f, ColorUtil.applyOpacity(textColor, toggleAnim));
            RenderUtil.scaleEnd();
        }

    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (bypass && HoveringUtil.isHovering(x, y, WH, WH, mouseX, mouseY)) {
                enabled = !enabled;
            } else if (SideGUI.isHovering(x, y, WH, WH, mouseX, mouseY)) {
                enabled = !enabled;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {

    }

    public float getActualX() {
        return x - ((tenacityFont16.getStringWidth(name) + 5));
    }

}