package dev.merguru.ui.mainmenu;

import dev.merguru.ui.Screen;
import dev.merguru.utils.animations.Animation;
import dev.merguru.utils.animations.Direction;
import dev.merguru.utils.animations.impl.DecelerateAnimation;
import dev.merguru.utils.misc.HoveringUtil;
import dev.merguru.utils.render.RoundedUtil;
import dev.merguru.utils.render.blur.GaussianBlur;

import java.awt.*;

public class MenuButton implements Screen {

    public final String text;
    private Animation hoverAnimation;
    public float x, y, width, height;
    public Runnable clickAction;

    public MenuButton(String text) {
        this.text = text;
    }


    @Override
    public void initGui() {
        hoverAnimation = new DecelerateAnimation(200, 1);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {

        boolean hovered = HoveringUtil.isHovering(x, y, width, height, mouseX, mouseY);
        hoverAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);


        //RenderUtil.color(-1);
        //RenderUtil.drawImage(rs, x, y, width, height);
        GaussianBlur.startBlur();
        RoundedUtil.drawRound(x,y,width,height,4f,new Color(20,20,20,100));
        GaussianBlur.endBlur(40,2);
        RoundedUtil.drawRound(x,y,width,height,4f,new Color(255,255,255,(int)(80*hoverAnimation.getOutput())));

        tenacityFont22.drawCenteredString(text, x + width / 2f, y + tenacityFont22.getMiddleOfBox(height), -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovered = HoveringUtil.isHovering(x, y, width, height, mouseX, mouseY);
        if (hovered) {
            clickAction.run();
        }

    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {

    }
}
