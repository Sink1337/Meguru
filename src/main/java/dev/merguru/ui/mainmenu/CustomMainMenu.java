package dev.merguru.ui.mainmenu;

import dev.merguru.Merguru;
import dev.merguru.intent.cloud.Cloud;
import dev.merguru.ui.Screen;
import dev.merguru.ui.altmanager.panels.LoginPanel;
import dev.merguru.ui.mainmenu.particles.ParticleEngine;
import dev.merguru.utils.animations.Animation;
import dev.merguru.utils.animations.Direction;
import dev.merguru.utils.animations.impl.DecelerateAnimation;
import dev.merguru.utils.misc.DiscordRPC;
import dev.merguru.utils.misc.HoveringUtil;
import dev.merguru.utils.misc.IOUtils;
import dev.merguru.utils.misc.NetworkingUtils;
import dev.merguru.utils.render.RenderUtil;
import dev.merguru.utils.render.StencilUtil;
import lombok.Getter;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomMainMenu extends GuiScreen {
    private ParticleEngine particleEngine;

    public static boolean animatedOpen = false;

    private final List<MenuButton> buttons = new ArrayList() {{
        add(new MenuButton("Singleplayer"));
        add(new MenuButton("Multiplayer"));
        add(new MenuButton("Alt Manager"));
        add(new MenuButton("Settings"));
        add(new MenuButton("Exit"));
    }};

    private final List<TextButton> textButtons = new ArrayList() {{
        add(new TextButton("QQ"));
        add(new TextButton("Discord"));
    }};

    private final ResourceLocation backgroundResource = new ResourceLocation("merguru/mainMenu/funny.png");
    private final ResourceLocation blurredRect = new ResourceLocation("merguru/mainMenu/rect-test.png");
    private final ResourceLocation hoverCircle = new ResourceLocation("merguru/mainMenu/hover-circle.png");

    private static boolean firstInit = false;

    @Override
    public void initGui() {
        if (!firstInit) {
            NetworkingUtils.bypassSSL();
            if (Util.getOSType() == Util.EnumOS.WINDOWS) {
                Merguru.INSTANCE.setDiscordRPC(new DiscordRPC());
            }
            firstInit = true;
        }

        if (particleEngine == null) particleEngine = new ParticleEngine();
        if (mc.gameSettings.guiScale != 2) {
            Merguru.prevGuiScale = mc.gameSettings.guiScale;
            Merguru.updateGuiScale = true;
            mc.gameSettings.guiScale = 2;
            mc.resize(mc.displayWidth - 1, mc.displayHeight);
            mc.resize(mc.displayWidth + 1, mc.displayHeight);
        }
        buttons.forEach(MenuButton::initGui);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        width = sr.getScaledWidth();
        height = sr.getScaledHeight();

        RenderUtil.resetColor();

        Merguru.INSTANCE.videoRenderer.render(width, height);

//        float rectWidth = 277;
//        float rectHeight = 275.5f;

//        GaussianBlur.startBlur();
//        RoundedUtil.drawRound(width / 2f - rectWidth / 2f, height / 2f - rectHeight / 2f,
//                rectWidth, rectHeight, 10, Color.WHITE);
//        GaussianBlur.endBlur(40, 2);


//        float outlineImgWidth = 688 / 2f;
//        float outlineImgHeight = 681 / 2f;
//        GLUtil.startBlend();
//        RenderUtil.color(-1);
//        RenderUtil.drawImage(blurredRect, width / 2f - outlineImgWidth / 2f, height / 2f - outlineImgHeight / 2f,
//                outlineImgWidth, outlineImgHeight);
//

        if (animatedOpen) {
            //    tenacityFont80.drawCenteredString("Meguru", width / 2f, height / 2f - 110, Color.WHITE.getRGB());
            //    tenacityFont32.drawString(Tenacity.VERSION, width / 2f + tenacityFont80.getStringWidth("Meguru") / 2f - (tenacityFont32.getStringWidth(Tenacity.VERSION) / 2f), height / 2f - 113, Color.WHITE.getRGB());
        }
        Color textcolor =new Color(255,255,255,255);
        tenacityBoldFont80.drawCenteredString("Meguru", width / 2f, height / 2f - 110, textcolor);
        tenacityFont32.drawString(Merguru.VERSION, width / 2f + tenacityBoldFont80.getStringWidth("Meguru") / 2f - (tenacityFont32.getStringWidth(Merguru.VERSION) / 2f), height / 2f - 113, textcolor);
        if (Merguru.is0721){
            tenacityFont18.drawCenteredString("Edit by 0d00", width / 2f, height / 2f - 68 + 5, textcolor);
        }else {
            tenacityFont18.drawCenteredString("Edit by Sink", width / 2f, height / 2f - 68 + 5, textcolor);
        }

        GL11.glEnable(GL11.GL_BLEND);


        StencilUtil.initStencilToWrite();

        RenderUtil.setAlphaLimit(13);

        RenderUtil.setAlphaLimit(0);
        StencilUtil.readStencilBuffer(1);

        StencilUtil.uninitStencilBuffer();


        float buttonWidth = 140;
        float buttonHeight = 25;

        int count = 0;
        for (MenuButton button : buttons) {
            button.x = width / 2f - buttonWidth / 2f;
            button.y = ((height / 2f - buttonHeight / 2f) - 25) + count;
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.clickAction = () -> {
                switch (button.text) {
                    case "Singleplayer":
                        mc.displayGuiScreen(new GuiSelectWorld(this));
                        break;
                    case "Multiplayer":
                        mc.displayGuiScreen(new GuiMultiplayer(this));
                        break;
                    case "Alt Manager":
                        mc.displayGuiScreen(Merguru.INSTANCE.getAltManager());
                        break;
                    case "Settings":
                        mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                        break;
                    case "Exit":
                        mc.shutdown();
                        break;
                }
            };
            button.drawScreen(mouseX, mouseY);
            count += buttonHeight + 7;
        }


        float buttonCount = 0;
        float buttonsWidth = (float) textButtons.stream().mapToDouble(TextButton::getWidth).sum();
        int buttonsSize = textButtons.size();
        buttonsWidth += tenacityFont16.getStringWidth(" | ") * (buttonsSize - 1);

        int buttonIncrement = 0;
        for (TextButton button : textButtons) {
            button.x = width / 2f - buttonsWidth / 2f + buttonCount;
            button.y = (height / 2f) + 130;
            switch (button.text) {
                case "QQ":
                    button.clickAction = () -> {
                        IOUtils.openLink("https://qm.qq.com/q/i5ZMkG5S1O");
                    };
                    break;
                case "Discord":
                    button.clickAction = () -> {
                        IOUtils.openLink("https://discord.com/channels/1389913736168345753/1389917096280068189/1389917987888169031");
                    };
                    break;
            }

            button.addToEnd = (buttonIncrement != (buttonsSize - 1));

            button.drawScreen(mouseX, mouseY);


            buttonCount += button.getWidth() + tenacityFont14.getStringWidth(" | ");
            buttonIncrement++;
        }


    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        LoginPanel.cracked = Cloud.getApiKey() == null;
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        textButtons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void onGuiClosed() {
        if (Merguru.updateGuiScale) {
            mc.gameSettings.guiScale = Merguru.prevGuiScale;
            Merguru.updateGuiScale = false;
        }
    }

    private static class TextButton implements Screen {
        public float x, y;
        @Getter
        private final float width, height;
        public Runnable clickAction;
        private final String text;

        private final Animation hoverAnimation = new DecelerateAnimation(150, 1);

        public boolean addToEnd;

        public TextButton(String text) {
            this.text = text;
            width = tenacityFont16.getStringWidth(text);
            height = tenacityFont16.getHeight();
        }

        @Override
        public void initGui() {

        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {

        }

        @Override
        public void drawScreen(int mouseX, int mouseY) {
            boolean hovered = HoveringUtil.isHovering(x, y, width, height, mouseX, mouseY);
            hoverAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
            tenacityFont16.drawString(text, x, y - (height / 2f * hoverAnimation.getOutput().floatValue()), Color.WHITE.getRGB());
            if (addToEnd) {
                tenacityFont16.drawString(" | ", x + width, y, Color.WHITE.getRGB());
            }
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int button) {
            boolean hovered = HoveringUtil.isHovering(x, y, width, height, mouseX, mouseY);
            if (hovered && button == 0) {
                clickAction.run();
            }
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY, int state) {

        }
    }

}
