package dev.meguru.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public final class GLUtils {
    public static void startScissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        final int scaleFactor = sr.getScaleFactor();

        glPushMatrix();
        glEnable(GL11.GL_SCISSOR_TEST);
        glScissor(x * scaleFactor, Minecraft.getMinecraft().displayHeight - (y + height) * scaleFactor, width * scaleFactor, height * scaleFactor);
    }

    public static void stopScissor() {
        glDisable(GL11.GL_SCISSOR_TEST);
        glPopMatrix();
    }

    public static void scale(double x, double y, double z) {
        glScaled(x, y, z);
    }

    public static void pushMatrix() {
        glPushMatrix();
    }

    public static void popMatrix() {
        glPopMatrix();
    }

    public static void enable(int cap) {
        glEnable(cap);
    }

    public static void disable(int cap) {
        glDisable(cap);
    }

    public static void blendFunc(int sFactor,int dFactor) {
        glBlendFunc(sFactor,dFactor);
    }

    public static void translated(double x,double y,double z) {
        glTranslated(x,y,z);
    }

    public static void rotated(double angle,double x,double y,double z) {
        glRotated(angle,x,y,z);
    }

    public static void depthMask(boolean flag) {
        glDepthMask(flag);
    }

    public static void color(int r,int g,int b) {
        color(r,g,b,255);
    }

    public static void color(int r,int g,int b,int a) {
        GlStateManager.color(r / 255f,g / 255f,b / 255f,a / 255f);
    }

    public static void color(int hex) {
        GlStateManager.color(
                (hex >> 16 & 0xFF) / 255.0f,
                (hex >> 8 & 0xFF) / 255.0f,
                (hex & 0xFF) / 255.0f,
                (hex >> 24 & 0xFF) / 255.0f);
    }

    public static void resetColor() {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
