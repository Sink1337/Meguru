package dev.merguru.ui.clickguis.modern.components;

import dev.merguru.module.Category;
import dev.merguru.utils.animations.Animation;
import dev.merguru.utils.animations.Direction;
import dev.merguru.utils.animations.impl.DecelerateAnimation;
import dev.merguru.utils.animations.impl.EaseInOutQuad;
import dev.merguru.utils.font.FontUtil;
import dev.merguru.utils.misc.HoveringUtil;
import dev.merguru.utils.render.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;

public class CategoryButton extends Component {
    public final Category category;
    public Category currentCategory;
    private Animation hoverAnimation;
    private Animation enableAnimation;
    public Animation expandAnimation;

    public CategoryButton(Category category) {
        this.category = category;
    }

    @Override
    public void initGui() {
        hoverAnimation = new EaseInOutQuad(200, 1);
        enableAnimation = new DecelerateAnimation(250, 1);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {

        boolean hovering = HoveringUtil.isHovering(x, y - 3, 83 -
                (expandAnimation.getDirection().forwards() ? 62 : 0), 18, mouseX, mouseY);

        hoverAnimation.setDirection(hovering ? Direction.FORWARDS : Direction.BACKWARDS);
        hoverAnimation.setDuration(hovering ? 200 : 350);
        enableAnimation.setDirection(currentCategory == category ? Direction.FORWARDS : Direction.BACKWARDS);

        int color = ColorUtil.interpolateColor(new Color(68, 71, 78), new Color(115, 115, 115), (float) hoverAnimation.getOutput().floatValue());
        color = ColorUtil.interpolateColor(new Color(color), new Color(-1), (float) enableAnimation.getOutput().floatValue());


        float adjustment = 0;
        if (category == Category.COMBAT) adjustment = 2.5f;

        GlStateManager.color(1, 1, 1);
        FontUtil.iconFont35.drawCenteredString(category.icon, x + 10 + adjustment, y, color);
        GlStateManager.color(1, 1, 1);


        float xDiff = 10 * expandAnimation.getOutput().floatValue();
        tenacityFont24.drawString(category.name, x + 27 + xDiff, y, color);


    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovering = HoveringUtil.isHovering(x, y - 3, 83 -
                (expandAnimation.getDirection().forwards() ? 62 : 0), 18, mouseX, mouseY);
        this.hovering = hovering && button == 0;

    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {

    }
}
