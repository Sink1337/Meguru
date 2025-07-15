package dev.meguru.module.impl.render;

import dev.meguru.event.impl.player.UpdateEvent;
import dev.meguru.event.impl.render.Render3DEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.utils.render.RenderUtil;
import dev.meguru.utils.time.TimerUtil;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Trails extends Module {
    private final BooleanSetting timeoutProperty = new BooleanSetting("Timeout", true);
    private final List<Trail> trails = new ArrayList<Trail>();
    public Trails() {
        super("Trails", Category.RENDER, "shows where you've walked");
        addSettings(timeoutProperty);
    }


    @Override
    public void onUpdateEvent(UpdateEvent event) {
        this.clearTrails();
        this.updateTrails();
    }


    @Override
    public void onRender3DEvent(Render3DEvent event) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2884);
        GL11.glFrontFace(2304);
        Trails.mc.entityRenderer.setupCameraTransform(event.getTicks(), 0);
        GL11.glBegin(7);
        int i = 0;
        double x = -RenderManager.viewerPosX;
        double y = -RenderManager.viewerPosY;
        double z = -RenderManager.viewerPosZ;
        for (Trail trail : this.trails) {
            if (!trail.visible) {
                if (trail.opacity > 0.0f) {
                    Trail trail2 = trail;
                    trail2.opacity = trail2.opacity - 1.0f;
                }
                trail.timer.reset();
            }
            if (i > 1) {
                if (Trails.mc.gameSettings.thirdPersonView == 0 && i >= this.trails.size() - 4) continue;
                Trail previousTrail = this.trails.get(i - 1);
                double diffX = trail.x - previousTrail.x;
                double diffY = trail.maxY - trail.minY - (previousTrail.maxY - previousTrail.minY);
                double diffZ = trail.z - previousTrail.z;
                double dist = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
                RenderUtil.color(new Color(Trails.rainbow(4.0f, 0.4f, 1.0f, i)), (int)trail.opacity);
                GL11.glVertex3d(x + trail.x, y + trail.maxY, z + trail.z);
                RenderUtil.color(new Color(Trails.rainbow(4.0f, 0.4f, 1.0f, i)), (int)((double)trail.opacity - dist / 50.0));
                GL11.glVertex3d(x + previousTrail.x, y + previousTrail.maxY, z + previousTrail.z);
                RenderUtil.color(new Color(Trails.rainbow(4.0f, 0.4f, 1.0f, i)), (int)((double)trail.opacity - dist / 50.0));
                GL11.glVertex3d(x + previousTrail.x, y + previousTrail.minY, z + previousTrail.z);
                RenderUtil.color(new Color(Trails.rainbow(4.0f, 0.4f, 1.0f, i)), (int)trail.opacity);
                GL11.glVertex3d(x + trail.x, y + trail.minY, z + trail.z);
            }
            ++i;
        }
        GL11.glEnd();
        GL11.glEnable(2848);
        GL11.glBegin(3);
        for (Trail trail : this.trails) {
            if (Trails.mc.gameSettings.thirdPersonView == 0 && i >= this.trails.size() - 4) continue;
            RenderUtil.color(new Color(Trails.rainbow(4.0f, 0.4f, 1.0f, i)), (int)trail.opacity);
            GL11.glVertex3d(x + trail.x, y + trail.maxY, z + trail.z);
        }
        GL11.glEnd();
        GL11.glBegin(3);
        for (Trail trail : this.trails) {
            if (Trails.mc.gameSettings.thirdPersonView == 0 && i >= this.trails.size() - 4) continue;
            RenderUtil.color(new Color(Trails.rainbow(4.0f, 0.4f, 1.0f, i)), (int)trail.opacity);
            GL11.glVertex3d(x + trail.x, y + trail.minY, z + trail.z);
        }
        GL11.glEnd();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(2848);
        GL11.glFrontFace(2305);
        GL11.glEnable(2884);
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
    }

    public static int rainbow(float seconds, float saturation, float brightness, long index) {
        float hue = (float)((System.currentTimeMillis() + index) % (long)((int)(seconds * 1000.0f))) / (seconds * 1000.0f);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.trails.clear();
    }

    public double getDistanceToTrail(Trail trail) {
        double xDiff = Math.abs(trail.x - Trails.mc.thePlayer.posX);
        double zDiff = Math.abs(trail.z - Trails.mc.thePlayer.posZ);
        return MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
    }

    public void clearTrails() {
        this.trails.removeIf(breadcrumb -> !breadcrumb.visible && breadcrumb.opacity == 0.0f || this.getDistanceToTrail(breadcrumb) > 30.0);
    }

    public void updateTrails() {
        for (Trail trail : this.trails) {
            if (System.currentTimeMillis() - trail.time <= 150L || !this.timeoutProperty.isEnabled()) continue;
            trail.visible = false;
        }
        if (Trails.mc.thePlayer.motionX != 0.0 || Trails.mc.thePlayer.motionY != 0.0 || Trails.mc.thePlayer.motionZ != 0.0) {
            this.trails.add(new Trail(Trails.mc.thePlayer.posX, Trails.mc.thePlayer.getEntityBoundingBox().minY, Trails.mc.thePlayer.getEntityBoundingBox().maxY, Trails.mc.thePlayer.posZ));
        }
    }

    private static class Trail {
        private final TimerUtil timer;
        private final double x;
        private final double minY;
        private final double maxY;
        private final double z;
        private boolean visible;
        private float opacity;
        private final long time;

        public Trail(double x, double minY, double maxY, double z) {
            this.x = x;
            this.minY = minY;
            this.maxY = maxY;
            this.z = z;
            this.opacity = 100.0f;
            this.visible = true;
            this.timer = new TimerUtil();
            this.time = System.currentTimeMillis();
        }
    }
}
