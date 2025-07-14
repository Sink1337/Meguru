package dev.tenacity.module.impl.render.targethud;

import dev.tenacity.utils.animations.ContinualAnimation;
import dev.tenacity.utils.render.ColorUtil;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.render.RoundedUtil;
import dev.tenacity.utils.render.StencilUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
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

    @Override
    public void render(float x, float y, float alpha, EntityLivingBase target) {

        setWidth(35 + interBold20.getStringWidth(target.getName()) + 15);
        setHeight(35f);

        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float space = (getWidth() - 45) / 100;

        animation.animate((100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 30);

        RoundedUtil.drawRound(x, y, getWidth(), getHeight(), 8, ColorUtil.applyOpacity(new Color(0, 0, 0, 80), alpha));

        RoundedUtil.drawRound(x + 38, y + 24f, (100 * space), 6f, 3f, ColorUtil.applyOpacity(Color.BLACK, alpha * (150f / 255f)));

        String text = String.format("%.1f", target.getHealth());

        RoundedUtil.drawRound(x + 38, y + 24f, (float) animation.getOutput(), 6f, 3f, ColorUtil.applyOpacity(colorWheel.getColor1(), alpha));

        GlStateManager.pushMatrix();
        int textColor = ColorUtil.applyOpacity(-1, alpha);

        float playerModelSize = 30;
        if (target instanceof AbstractClientPlayer) {
            StencilUtil.initStencilToWrite();
            RenderUtil.renderRoundedRect(x + 4f, y + 2.5f, playerModelSize, playerModelSize, 5, -1);
            StencilUtil.readStencilBuffer(1);
            RenderUtil.color(-1, alpha);
            renderPlayer2D(x + 4f, y + 2.5f, playerModelSize, playerModelSize,(AbstractClientPlayer) target);
            StencilUtil.uninitStencilBuffer();
            GlStateManager.disableBlend();
        } else {
            interBold20.drawString("?", (x + 15f), (y + 14f), textColor);
        }
        GlStateManager.popMatrix();

        interMedium14.drawSmoothString(text + " HP", x + 38, y + 17, textColor);
        interBold20.drawSmoothString(target.getName(), x + 38, y + 7, textColor);
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