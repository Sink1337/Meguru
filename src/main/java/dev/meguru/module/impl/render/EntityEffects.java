package dev.meguru.module.impl.render;

import dev.meguru.Meguru;
import dev.meguru.event.impl.render.Render2DEvent;
import dev.meguru.event.impl.render.Render3DEvent;
import dev.meguru.event.impl.render.RenderModelEvent;
import dev.meguru.event.impl.render.ShaderEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.settings.ParentAttribute;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.module.settings.impl.MultipleBoolSetting;
import dev.meguru.ui.notifications.NotificationManager;
import dev.meguru.ui.notifications.NotificationType;
import dev.meguru.utils.render.ESPUtil;
import dev.meguru.utils.render.GLUtil;
import dev.meguru.utils.render.RenderUtil;
import dev.meguru.utils.render.ShaderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EntityEffects extends Module {

    private final MultipleBoolSetting validEntities = new MultipleBoolSetting("Valid Entities",
            new BooleanSetting("Players", true),
            new BooleanSetting("Self", false),
            new BooleanSetting("Bots", false),
            new BooleanSetting("Animals", true),
            new BooleanSetting("Mobs", true));

    private final BooleanSetting blur = new BooleanSetting("Blur", true);
    private final BooleanSetting bloom = new BooleanSetting("Bloom", true);
    private final BooleanSetting blackBloom = new BooleanSetting("Black Bloom", true);

    private Framebuffer entityFramebuffer = new Framebuffer(1, 1, false);

    public EntityEffects() {
        super("Entity Effects", Category.RENDER, "Very unnecessary blur of entities");
        blackBloom.addParent(bloom, ParentAttribute.BOOLEAN_CONDITION);
        addSettings(validEntities, blur, bloom, blackBloom);
    }

    @Override
    public void onEnable() {
        if (Meguru.INSTANCE.isEnabled(PostProcessing.class)) {
            super.onEnable();
        } else {
            NotificationManager.post(NotificationType.WARNING, "Error", "Post Processing is not enabled");
            toggleSilent();
        }
    }

    private final List<Entity> entities = new ArrayList<>();

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        entities.clear();
        for (final Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRender(entity) && ESPUtil.isInView(entity)) {
                entities.add(entity);
            }
        }
    }

    @Override
    public void onRenderModelEvent(RenderModelEvent event) {
        if (event.isPost() && entities.contains(event.getEntity())) {
            entityFramebuffer.bindFramebuffer(false);
            RenderUtil.resetColor();
            GlStateManager.enableCull();
            GlowESP.renderGlint = false;
            event.drawModel();

            event.drawLayers();
            GlowESP.renderGlint = true;
            GlStateManager.disableCull();

            mc.getFramebuffer().bindFramebuffer(false);
        }
    }

    @Override
    public void onShaderEvent(ShaderEvent e) {
        if (e.isBloom() ? bloom.isEnabled() : blur.isEnabled()) {
            RenderUtil.setAlphaLimit(0);
            RenderUtil.resetColor();
            GLUtil.startBlend();

            if (e.isBloom() && blackBloom.isEnabled()) {
                RenderUtil.color(Color.BLACK.getRGB());
            }

            RenderUtil.bindTexture(entityFramebuffer.framebufferTexture);
            ShaderUtil.drawQuads();
        }
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        entityFramebuffer = RenderUtil.createFrameBuffer(entityFramebuffer);
        entityFramebuffer.framebufferClear();
        mc.getFramebuffer().bindFramebuffer(true);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == null || entity.isDead || entity.isInvisible()) {
            return false;
        }

        if (entity == mc.thePlayer) {
            return validEntities.isEnabled("Self") && mc.gameSettings.thirdPersonView != 0;
        }

        if (TargetManager.isBot(entity)) {
            return validEntities.isEnabled("Bots");
        }

        if (validEntities.isEnabled("Players") && entity instanceof EntityPlayer) {
            return true;
        }

        if (validEntities.isEnabled("Animals") && entity instanceof EntityAnimal) {
            return true;
        }

        return validEntities.isEnabled("Mobs") && entity instanceof EntityMob;
    }
}