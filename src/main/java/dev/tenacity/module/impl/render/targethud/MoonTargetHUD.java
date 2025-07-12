package dev.tenacity.module.impl.render.targethud;

import dev.tenacity.utils.animations.ContinualAnimation;
import dev.tenacity.utils.font.CustomFont;
import dev.tenacity.utils.render.ColorUtil;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.render.RoundedUtil;
import dev.tenacity.utils.render.StencilUtil;
import dev.tenacity.utils.render.GLUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import java.awt.*;
import java.text.DecimalFormat;

public class MoonTargetHUD extends TargetHUD {

    private final ContinualAnimation animation = new ContinualAnimation();
    private final DecimalFormat DF_1 = new DecimalFormat("0.0");

    public MoonTargetHUD() {
        super("Moon");
    }

    private static void renderPlayer2D(EntityLivingBase abstractClientPlayer, final float x, final float y, final float size, float radius, int color) {
        if (abstractClientPlayer instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer) abstractClientPlayer;

            StencilUtil.initStencilToWrite();
            RenderUtil.drawRoundedRect(x, y, size, size, radius, -1);
            StencilUtil.readStencilBuffer(1);
            RenderUtil.color(color);
            GLUtil.startBlend();
            mc.getTextureManager().bindTexture(player.getLocationSkin());
            Gui.drawScaledCustomSizeModalRect(x, y, (float) 8.0, (float) 8.0, 8, 8, size, size, 64.0F, 64.0F);
            GLUtil.endBlend();
            StencilUtil.uninitStencilBuffer();
        }
    }

    @Override
    public void render(float x, float y, float alpha, EntityLivingBase target) {

        setWidth(35 + intersemiBoldFont18.getStringWidth(target.getName()) + 33);
        setHeight(40.5f);

        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float space = (getWidth() - 48) / 100;

        animation.animate((100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 30);

        RoundedUtil.drawRound(x, y, getWidth(), getHeight(), 8, ColorUtil.applyOpacity(new Color(0, 0, 0, 100), alpha));

        RoundedUtil.drawRound(x + 42, y + 26.5f, (100 * space), 8, 4, ColorUtil.applyOpacity(Color.BLACK, alpha * (150f / 255f)));

        String text = String.format("%.1f", target.getHealth());

        RoundedUtil.drawRound(x + 42, y + 26.5f, (float) animation.getOutput(), 8.5f, 4, ColorUtil.applyOpacity(colorWheel.getColor1(), alpha));

        GlStateManager.pushMatrix();
        int textColor = ColorUtil.applyOpacity(-1, alpha);

        float playerModelSize = 35;
        if (target instanceof AbstractClientPlayer) {
            renderPlayer2D((AbstractClientPlayer) target, x + 2.5f, y + 2.5f, playerModelSize, 4, textColor);
        } else {
            Gui.drawRect(x + 2.5, y + 2.5, x + 2.5 + playerModelSize, y + 2.5 + playerModelSize, ColorUtil.applyOpacity(Color.DARK_GRAY, alpha).getRGB());
            GlStateManager.scale(2, 2, 2);
            intersemiBoldFont18.drawStringWithShadow("?", (x + 2.5f + playerModelSize / 2f) / 2.0F, (y + 2.5f + playerModelSize / 2f) / 2.0F, textColor);
        }
        GlStateManager.popMatrix();

        intersemiBoldFont13.drawStringWithShadow(text + "HP", x + 40, y + 17, textColor);
        intersemiBoldFont18.drawStringWithShadow(target.getName(), x + 40, y + 6, textColor);
    }

    @Override
    public void renderEffects(float x, float y, float alpha, boolean glow) {
        if (glow) {
            RoundedUtil.drawRound(x, y, getWidth(), getHeight(), 8, ColorUtil.applyOpacity(colorWheel.getColor1(), alpha));
        } else {
            RoundedUtil.drawRound(x, y, getWidth(), getHeight(), 8, ColorUtil.applyOpacity(Color.BLACK, alpha));
        }
    }
}