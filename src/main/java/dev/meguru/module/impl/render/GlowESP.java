package dev.meguru.module.impl.render;

import dev.meguru.Meguru;
import dev.meguru.event.impl.game.WorldEvent;
import dev.meguru.event.impl.render.Render2DEvent;
import dev.meguru.event.impl.render.RenderChestEvent;
import dev.meguru.event.impl.render.RenderModelEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.impl.player.ChestStealer;
import dev.meguru.module.settings.ParentAttribute;
import dev.meguru.module.settings.impl.*;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.misc.MathUtils;
import dev.meguru.utils.render.*;
import dev.meguru.utils.tuples.Pair;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUniform1;

public class GlowESP extends Module {

    private final BooleanSetting kawaseGlow = new BooleanSetting("Kawase Glow", false);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Sync", "Sync", "Random", "Custom");

    private final MultipleBoolSetting validEntities = new MultipleBoolSetting("Entities",
            new BooleanSetting("Players", true),
            new BooleanSetting("Self", false),
            new BooleanSetting("Bots", false),
            new BooleanSetting("Animals", true),
            new BooleanSetting("Mobs", true),
            new BooleanSetting("Chests", true));

    private final ColorSetting playerColor = new ColorSetting("Player Color", Meguru.INSTANCE.getClientColor());
    private final ColorSetting selfColor = new ColorSetting("Self Color", Color.CYAN);
    private final ColorSetting botColor = new ColorSetting("Bot Color", Color.MAGENTA);
    private final ColorSetting animalColor = new ColorSetting("Animal Color", Meguru.INSTANCE.getAlternateClientColor());
    private final ColorSetting mobColor = new ColorSetting("Mob Color", Color.RED);
    private final ColorSetting chestColor = new ColorSetting("Chest Color", Color.GREEN);
    private final ColorSetting openedChestColor = new ColorSetting("Opened Chest Color", Color.RED);
    private final ColorSetting hurtTimeColor = new ColorSetting("Hurt Time Color", Color.RED);
    private final NumberSetting radius = new NumberSetting("Radius", 4, 20, 2, 2);
    private final NumberSetting iterationsSetting = new NumberSetting("Iterations", 4, 10, 2, 1);
    private final NumberSetting offsetSetting = new NumberSetting("Offset", 4, 10, 2, 1);
    private final NumberSetting exposure = new NumberSetting("Exposure", 2.2, 3.5, .5, .1);
    private final BooleanSetting seperate = new BooleanSetting("Seperate Texture", false);

    public GlowESP() {
        super("GlowESP", Category.RENDER, "ESP that glows on players");
        playerColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Players").isEnabled());
        selfColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Self").isEnabled());
        botColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Bots").isEnabled());
        animalColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Animals").isEnabled());
        mobColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Mobs").isEnabled());
        chestColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Chests").isEnabled());
        openedChestColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom") && validEntities.getSetting("Chests").isEnabled());

        radius.addParent(kawaseGlow, ParentAttribute.BOOLEAN_CONDITION.negate());
        iterationsSetting.addParent(kawaseGlow, ParentAttribute.BOOLEAN_CONDITION);
        offsetSetting.addParent(kawaseGlow, ParentAttribute.BOOLEAN_CONDITION);
        addSettings(kawaseGlow, colorMode, validEntities, playerColor, selfColor, botColor, animalColor, mobColor, chestColor, openedChestColor, hurtTimeColor, iterationsSetting, offsetSetting, radius, exposure, seperate);
    }

    public static boolean renderNameTags = true;
    private final ShaderUtil chamsShader = new ShaderUtil("chams");
    private final ShaderUtil outlineShader = new ShaderUtil("meguru/shaders/outline.frag");
    private final ShaderUtil glowShader = new ShaderUtil("glow");
    private final ShaderUtil kawaseGlowShader = new ShaderUtil("kawaseDownBloom");
    private final ShaderUtil kawaseGlowShader2 = new ShaderUtil("kawaseUpGlow");

    public Framebuffer framebuffer;
    public Framebuffer outlineFrameBuffer;

    public Framebuffer glowFrameBuffer;
    public static boolean renderGlint = true;

    private final List<Entity> glowEntities = new ArrayList<>();
    private final List<TileEntity> glowTileEntities = new ArrayList<>();
    private final Map<Object, Color> entityColorMap = new HashMap<>();

    public static Animation fadeIn;

    private static int currentIterations;

    private static final List<Framebuffer> framebufferList = new ArrayList<>();

    @Override
    public void onWorldEvent(WorldEvent event) {
        entityColorMap.clear();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        entityColorMap.clear();
        fadeIn = new DecelerateAnimation(250, 1);
    }

    public void createFrameBuffers() {
        framebuffer = RenderUtil.createFrameBuffer(framebuffer);
        outlineFrameBuffer = RenderUtil.createFrameBuffer(outlineFrameBuffer);
    }

    @Override
    public void onRenderChestEvent(RenderChestEvent e) {
        if (validEntities.getSetting("Chests").isEnabled() && framebuffer != null) {
            framebuffer.bindFramebuffer(false);
            chamsShader.init();
            chamsShader.setUniformi("textureIn", 0);
            Color color = getColor(e.getEntity());

            RenderUtil.resetColor();
            chamsShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1);
            e.drawChest();
            chamsShader.unload();

            this.mc.getFramebuffer().bindFramebuffer(false);
        }
    }

    @Override
    public void onRenderModelEvent(RenderModelEvent e) {
        if (e.isPost() && framebuffer != null) {
            if (!glowEntities.contains(e.getEntity())) return;

            framebuffer.bindFramebuffer(false);
            chamsShader.init();
            chamsShader.setUniformi("textureIn", 0);
            Color color = getColor(e.getEntity());

            chamsShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1);
            RenderUtil.resetColor();
            GlStateManager.enableCull();
            renderGlint = false;
            e.drawModel();

            e.drawLayers();
            renderGlint = true;
            GlStateManager.disableCull();

            chamsShader.unload();

            mc.getFramebuffer().bindFramebuffer(false);
        }
    }

    @Override
    public void onRender2DEvent(Render2DEvent e) {
        createFrameBuffers();
        collectEntities();

        ScaledResolution sr = new ScaledResolution(mc);
        if (framebuffer != null && outlineFrameBuffer != null &&
                (validEntities.getSetting("Chests").isEnabled() && !glowTileEntities.isEmpty() || !glowEntities.isEmpty())) {
            RenderUtil.setAlphaLimit(0);
            GLUtil.startBlend();

            outlineFrameBuffer.framebufferClear();
            outlineFrameBuffer.bindFramebuffer(false);
            outlineShader.init();
            setupOutlineUniforms(0, 1);
            RenderUtil.bindTexture(framebuffer.framebufferTexture);
            ShaderUtil.drawQuads();
            outlineShader.init();
            setupOutlineUniforms(1, 0);
            RenderUtil.bindTexture(framebuffer.framebufferTexture);
            ShaderUtil.drawQuads();
            outlineShader.unload();
            outlineFrameBuffer.unbindFramebuffer();

            if (kawaseGlow.isEnabled()) {
                int offset = offsetSetting.getValue().intValue();
                int iterations = iterationsSetting.getValue().intValue();

                if (framebufferList.isEmpty() || currentIterations != iterations || (framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight)) {
                    initFramebuffers(iterations);
                    currentIterations = iterations;
                }
                RenderUtil.setAlphaLimit(0);

                glBlendFunc(GL_ONE, GL_ONE);

                GL11.glClearColor(0, 0, 0, 0);
                renderFBO(framebufferList.get(1), outlineFrameBuffer.framebufferTexture, kawaseGlowShader, offset);

                for (int i = 1; i < iterations; i++) {
                    renderFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, kawaseGlowShader, offset);
                }

                for (int i = iterations; i > 1; i--) {
                    renderFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, kawaseGlowShader2, offset);
                }

                Framebuffer lastBuffer = framebufferList.get(0);
                lastBuffer.framebufferClear();
                lastBuffer.bindFramebuffer(false);
                kawaseGlowShader2.init();
                kawaseGlowShader2.setUniformf("offset", offset, offset);
                kawaseGlowShader2.setUniformi("inTexture", 0);
                kawaseGlowShader2.setUniformi("check", seperate.isEnabled() ? 1 : 0);
                kawaseGlowShader2.setUniformf("lastPass", 1);
                kawaseGlowShader2.setUniformf("exposure", (float) (exposure.getValue().floatValue() * fadeIn.getOutput().floatValue()));
                kawaseGlowShader2.setUniformi("textureToCheck", 16);
                kawaseGlowShader2.setUniformf("halfpixel", 1.0f / lastBuffer.framebufferWidth, 1.0f / lastBuffer.framebufferHeight);
                kawaseGlowShader2.setUniformf("iResolution", lastBuffer.framebufferWidth, lastBuffer.framebufferHeight);
                GL13.glActiveTexture(GL13.GL_TEXTURE16);
                RenderUtil.bindTexture(framebuffer.framebufferTexture);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                RenderUtil.bindTexture(framebufferList.get(1).framebufferTexture);

                ShaderUtil.drawQuads();
                kawaseGlowShader2.unload();

                GL11.glClearColor(0, 0, 0, 0);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                framebuffer.framebufferClear();
                RenderUtil.resetColor();
                mc.getFramebuffer().bindFramebuffer(true);
                RenderUtil.bindTexture(framebufferList.get(0).framebufferTexture);
            } else {
                if (!framebufferList.isEmpty()) {
                    for (Framebuffer fb : framebufferList) {
                        fb.deleteFramebuffer();
                    }
                    glowFrameBuffer = null;
                    framebufferList.clear();
                }

                glowFrameBuffer = RenderUtil.createFrameBuffer(glowFrameBuffer);

                GL11.glClearColor(0, 0, 0, 0);
                glowFrameBuffer.framebufferClear();
                glowFrameBuffer.bindFramebuffer(false);
                glowShader.init();
                setupGlowUniforms(1f, 0);
                RenderUtil.bindTexture(outlineFrameBuffer.framebufferTexture);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                ShaderUtil.drawQuads();
                glowShader.unload();

                mc.getFramebuffer().bindFramebuffer(false);

                GL11.glClearColor(0, 0, 0, 0);
                glowShader.init();
                setupGlowUniforms(0, 1f);
                if (seperate.isEnabled()) {
                    GL13.glActiveTexture(GL13.GL_TEXTURE16);
                    RenderUtil.bindTexture(framebuffer.framebufferTexture);
                }
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                RenderUtil.bindTexture(glowFrameBuffer.framebufferTexture);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                ShaderUtil.drawQuads();
                glowShader.unload();

                framebuffer.framebufferClear();
                mc.getFramebuffer().bindFramebuffer(false);
            }
            RenderUtil.setAlphaLimit(0);
            GlStateManager.bindTexture(0);
        }
    }

    private void initFramebuffers(int iterations) {
        for (Framebuffer framebuffer : framebufferList) {
            framebuffer.deleteFramebuffer();
        }
        framebufferList.clear();

        framebufferList.add(glowFrameBuffer = RenderUtil.createFrameBuffer(null));

        for (int i = 1; i <= iterations; i++) {
            Framebuffer currentBuffer = new Framebuffer((int) (mc.displayWidth / Math.pow(2, i)), (int) (mc.displayHeight / Math.pow(2, i)), true);
            currentBuffer.setFramebufferFilter(GL_LINEAR);

            GlStateManager.bindTexture(currentBuffer.framebufferTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT);
            GlStateManager.bindTexture(0);

            framebufferList.add(currentBuffer);
        }
    }

    private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtil shader, float offset) {
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        shader.init();
        RenderUtil.bindTexture(framebufferTexture);
        shader.setUniformf("offset", offset, offset);
        shader.setUniformi("inTexture", 0);
        shader.setUniformi("check", 0);
        shader.setUniformf("lastPass", 0);
        shader.setUniformf("halfpixel", 1.0f / framebuffer.framebufferWidth, 1.0f / framebuffer.framebufferHeight);
        shader.setUniformf("iResolution", framebuffer.framebufferWidth, framebuffer.framebufferHeight);

        ShaderUtil.drawQuads();
        shader.unload();
    }

    public void setupGlowUniforms(float dir1, float dir2) {
        glowShader.setUniformi("texture", 0);
        if (seperate.isEnabled()) {
            glowShader.setUniformi("textureToCheck", 16);
        }
        glowShader.setUniformf("radius", radius.getValue().floatValue());
        glowShader.setUniformf("texelSize", 1.0f / mc.displayWidth, 1.0f / mc.displayHeight);
        glowShader.setUniformf("direction", dir1, dir2);
        glowShader.setUniformf("exposure", (float) (exposure.getValue().floatValue() * fadeIn.getOutput().floatValue()));
        glowShader.setUniformi("avoidTexture", seperate.isEnabled() ? 1 : 0);

        final FloatBuffer buffer = BufferUtils.createFloatBuffer(256);
        for (int i = 1; i <= radius.getValue().floatValue(); i++) {
            buffer.put(MathUtils.calculateGaussianValue(i, radius.getValue().floatValue() / 2));
        }
        buffer.rewind();

        glUniform1(glowShader.getUniform("weights"), buffer);
    }

    public void setupOutlineUniforms(float dir1, float dir2) {
        outlineShader.setUniformi("textureIn", 0);
        float iterations = kawaseGlow.isEnabled() ? (iterationsSetting.getValue().floatValue() * 2f) : radius.getValue().floatValue() / 1.5f;
        outlineShader.setUniformf("radius", iterations);
        outlineShader.setUniformf("texelSize", 1.0f / mc.displayWidth, 1.0f / mc.displayHeight);
        outlineShader.setUniformf("direction", dir1, dir2);
    }

    private Color getColor(Object object) {
        Color color = Color.WHITE;
        switch (colorMode.getMode()) {
            case "Custom":
                if (object == mc.thePlayer) {
                    color = selfColor.getColor();
                } else if (object instanceof EntityPlayer) {
                    if (TargetManager.isBot((Entity) object)) {
                        color = botColor.getColor();
                    } else {
                        color = playerColor.getColor();
                    }
                } else if (object instanceof EntityMob) {
                    color = mobColor.getColor();
                } else if (object instanceof EntityAnimal) {
                    color = animalColor.getColor();
                } else if (object instanceof TileEntityChest) {
                    TileEntityChest chest = (TileEntityChest) object;
                    BlockPos chestPos = chest.getPos();
                    if (ChestStealer.openedChests.contains(chestPos)) {
                        color = openedChestColor.getColor();
                    } else {
                        color = chestColor.getColor();
                    }
                }
                break;
            case "Sync":
                Pair<Color, Color> colors = HUDMod.getClientColors();
                if (HUDMod.isRainbowTheme()) {
                    color = colors.getFirst();
                } else {
                    color = ColorUtil.interpolateColorsBackAndForth(15, 0, colors.getFirst(), colors.getSecond(), false);
                }
                break;
            case "Random":
                if (entityColorMap.containsKey(object)) {
                    color = entityColorMap.get(object);
                } else {
                    color = ColorUtil.getRandomColor();
                    entityColorMap.put(object, color);
                }
                break;
        }

        if (object instanceof EntityLivingBase) {
            EntityLivingBase entityLivingBase = (EntityLivingBase) object;
            if (entityLivingBase.hurtTime > 0) {
                color = ColorUtil.interpolateColorC(color, hurtTimeColor.getColor(), (float) Math.sin(entityLivingBase.hurtTime * (18 * Math.PI / 180)));
            }
        }

        return color;
    }

    public void collectEntities() {
        glowEntities.clear();
        glowTileEntities.clear();

        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity == null || entity.isDead || entity.isInvisible()) {
                continue;
            }
            if (!ESPUtil.isInView(entity)) {
                continue;
            }

            if (entity instanceof EntityPlayer) {
                if (entity == mc.thePlayer) {
                    if (validEntities.getSetting("Self").isEnabled() && mc.gameSettings.thirdPersonView != 0) {
                        glowEntities.add(entity);
                    }
                    continue;
                }

                if (TargetManager.isBot(entity)) {
                    if (validEntities.getSetting("Bots").isEnabled()) {
                        glowEntities.add(entity);
                    }
                    continue;
                }

                if (validEntities.getSetting("Players").isEnabled()) {
                    glowEntities.add(entity);
                }
                continue;
            }

            if (entity instanceof EntityAnimal && validEntities.getSetting("Animals").isEnabled()) {
                glowEntities.add(entity);
                continue;
            }

            if (entity instanceof EntityMob && validEntities.getSetting("Mobs").isEnabled()) {
                glowEntities.add(entity);
            }
        }
        if (validEntities.getSetting("Chests").isEnabled()) {
            for (BlockPos pos : ChestStealer.renderableChests) {
                TileEntity tileEntity = mc.theWorld.getTileEntity(pos);
                if (tileEntity instanceof TileEntityChest) {
                    glowTileEntities.add(tileEntity);
                }
            }
        }
    }
}