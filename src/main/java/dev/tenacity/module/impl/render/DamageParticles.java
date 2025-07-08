package dev.tenacity.module.impl.render;

import dev.tenacity.event.impl.player.UpdateEvent;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.api.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class DamageParticles extends Module{
    private final ArrayList<hit> hits = new ArrayList();
    private float lastHealth;
    private EntityLivingBase lastTarget = null;
    public static float particleHue = 0.8f;
    public static boolean rainbowParticles = false;

    public DamageParticles() {
        super("DamageParticles", Category.RENDER, "Damage Particles");
    }

    @Override
    public void onUpdateEvent(UpdateEvent event) {
        if (TargetManager.target == null) {
            this.lastHealth = 20.0f;
            this.lastTarget = null;
            return;
        }
        if (this.lastTarget == null || TargetManager.target != this.lastTarget) {
            this.lastTarget = TargetManager.target;
            this.lastHealth = TargetManager.target.getHealth();
            return;
        }
        if (TargetManager.target.getHealth() != this.lastHealth) {
            if (TargetManager.target.getHealth() < this.lastHealth) {
                this.hits.add(new hit(TargetManager.target.getPosition().add(ThreadLocalRandom.current().nextDouble(-0.5, 0.5), ThreadLocalRandom.current().nextDouble(1.0, 1.5), ThreadLocalRandom.current().nextDouble(-0.5, 0.5)), this.lastHealth - TargetManager.target.getHealth()));
            }
            this.lastHealth = TargetManager.target.getHealth();
        }
    }

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        try {
            for (hit h : this.hits) {
                if (h.isFinished()) {
                    this.hits.remove(h);
                    continue;
                }
                h.onRender();
            }
        }
        catch (Exception exception) {
            //防止拉屎就不print了
            //exception.printStackTrace();
        }
    }

    static class hit {
        protected Minecraft mc = Minecraft.getMinecraft();
        private final long startTime = System.currentTimeMillis();
        private final BlockPos pos;
        private final double healthVal;
        private final long maxTime = 1000L;

        public hit(BlockPos pos, double healthVal) {
            this.pos = pos;
            this.healthVal = healthVal;
        }

        public void onRender() {
            //注意看，眼前这个男人叫小帅，它现在需要使用GL渲染器了

            //首先，小帅先保存了一下GL设置
            GlStateManager.pushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

            //接着，小帅以迅雷不及掩耳之势之直接调整了GL设置
            GL11.glEnable(GL11.GL_BLEND);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            //小帅使用他的最强大脑对位置进行了精确的计算
            double x = (double)((float)this.pos.getX() + 0.0f * this.mc.timer.renderPartialTicks) - RenderManager.viewerPosX;
            double y = (double)((float)this.pos.getY() + 0.0f * this.mc.timer.renderPartialTicks) - RenderManager.viewerPosY;
            double z = (double)((float)this.pos.getZ() + 0.0f * this.mc.timer.renderPartialTicks) - RenderManager.viewerPosZ;
            float var10001 = this.mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f;

            //小帅发现摄像头位置不对，没法拍到他的d1ck，可能出片效果比较差，于是便调整了一下摄像机
            Minecraft.getMinecraft().entityRenderer.setupCameraTransform(this.mc.timer.renderPartialTicks, 0);
            GlStateManager.translate(x, y, z);
            GL11.glNormal3f(0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(-this.mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(this.mc.getRenderManager().playerViewX, var10001, 0.0f, 0.0f);
            GlStateManager.scale(-0.025000001303851604, -0.025000001303851604, 0.025000001303851604);

            //接着小帅计算了一下时间
            float sizePercentage = 1.0f;
            long timeLeft = this.startTime + this.maxTime - System.currentTimeMillis();
            float yPercentage = 0.0f;

            if (timeLeft < 75L) {
                sizePercentage = Math.min((float)timeLeft / 75.0f, 1.0f);
                yPercentage = Math.min((float)timeLeft / 75.0f, 1.0f);
            } else {
                sizePercentage = Math.min((float)(System.currentTimeMillis() - this.startTime) / 300.0f, 1.0f);
                yPercentage = Math.min((float)(System.currentTimeMillis() - this.startTime) / 600.0f, 1.0f);
            }

            GlStateManager.scale(2.0f * sizePercentage, 2.0f * sizePercentage, 2.0f * sizePercentage);

            //小帅很快的画出了一个矩形背景
            Gui.drawRect(-100, -100, 100, 100, new Color(255, 0, 0, 0).getRGB());

            //小帅决定从中选取渲染伤害字符的颜色
            Color render = new Color(85, 194, 85);
            if (this.healthVal < 3.0 && this.healthVal > 1.0) {
                render = new Color(203, 203, 65);
            } else if (this.healthVal <= 1.0) {
                render = new Color(203, 57, 57);
            }

            //小帅用他那从mc.fontRendererObj里面学到的书法进行了一个书法展示
            this.mc.fontRendererObj.drawStringWithShadow("-" + new DecimalFormat("#.#").format(this.healthVal),
                    0.0f, -(yPercentage * 4.0f), render.getRGB());

            //好了视频也接近尾声了，小帅还原了GL状态
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);

            GL11.glPopAttrib();
            GlStateManager.popMatrix();

            //小帅最后重置了颜色，你们觉得小帅怎么样呢？
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public boolean isFinished() {
            return System.currentTimeMillis() - this.startTime >= 1000L;
        }
    }
}
