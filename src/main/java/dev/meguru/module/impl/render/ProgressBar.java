package dev.meguru.module.impl.render;

import dev.meguru.Meguru;
import dev.meguru.event.impl.render.Render2DEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.impl.exploit.Disabler;
import dev.meguru.module.impl.movement.LongJump;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.objects.Dragging;
import dev.meguru.utils.render.RenderUtil;

import java.awt.*;

public class ProgressBar extends Module {

    private final BooleanSetting animationSetting = new BooleanSetting("Animation", true);
    private final BooleanSetting showLongJumpBow = new BooleanSetting("LongJump Bow", true);
    private final BooleanSetting showLongJumpDamage = new BooleanSetting("LongJump Damage", true);
    private final BooleanSetting showDisablerDamageBoost = new BooleanSetting("Disabler Damage Boost", true);
    private final BooleanSetting showDisablerDamageFlight = new BooleanSetting("Disabler Damage Flight", true);

    private final Dragging progressBarPos;
    private float currentRenderProgress = 0.0f;
    private final Animation progressBarOpenAnimation = new DecelerateAnimation(250, 1);

    private LongJump longJumpModule;
    private Disabler disablerModule;

    public ProgressBar() {
        super("ProgressBar", Category.RENDER, "Displays progress bars for movement module states.");
        this.progressBarPos = Meguru.INSTANCE.createDrag(this, "progressBar", 450, 300);
        this.addSettings(animationSetting, showLongJumpBow, showLongJumpDamage, showDisablerDamageBoost, showDisablerDamageFlight);
    }

    @Override
    public void onEnable() {
        longJumpModule = Meguru.INSTANCE.getModuleCollection().getModule(LongJump.class);
        disablerModule = Meguru.INSTANCE.getModuleCollection().getModule(Disabler.class);

        if (longJumpModule == null && disablerModule == null) {
            this.toggleSilent();
            return;
        }

        progressBarOpenAnimation.setDirection(Direction.FORWARDS);
        progressBarOpenAnimation.reset();
        currentRenderProgress = 0.0f;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (animationSetting.isEnabled()) {
            progressBarOpenAnimation.setDirection(Direction.BACKWARDS);
        } else {
            currentRenderProgress = 0.0f;
            progressBarOpenAnimation.setDirection(Direction.BACKWARDS);
            progressBarOpenAnimation.timerUtil.lastMS = System.currentTimeMillis() - progressBarOpenAnimation.getDuration();
        }
        super.onDisable();
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        if (mc == null || mc.thePlayer == null || mc.timer == null) {
            return;
        }

        float targetProgress = 0.0f;
        boolean shouldRender = false;

        if (showLongJumpBow.isEnabled() && longJumpModule != null && longJumpModule.isEnabled() &&
                (longJumpModule.mode != null && (longJumpModule.mode.is("AGC") || (longJumpModule.mode.is("Bloxd") && longJumpModule.bloxdSubMode != null && longJumpModule.bloxdSubMode.is("Bow")))) &&
                !longJumpModule.damagedItem && longJumpModule.bowReleaseTime != null && longJumpModule.ticks != 0) {
            float totalDuration = longJumpModule.bowReleaseTime.getValue().floatValue();
            float elapsedTicks = mc.thePlayer.ticksExisted - longJumpModule.ticks;
            if (elapsedTicks >= 0 && elapsedTicks <= totalDuration) {
                targetProgress = elapsedTicks / totalDuration;
                shouldRender = true;
            }
        }

        if (!shouldRender && showLongJumpDamage.isEnabled() && longJumpModule != null && longJumpModule.isEnabled() &&
                longJumpModule.damagedItem && longJumpModule.mode != null && longJumpModule.mode.is("Bloxd")) {

            float totalDuration = longJumpModule.damageTime.getValue().floatValue();
            float elapsedTime = (float) longJumpModule.damageFlightTimer.getTime();

            if (totalDuration > 0 && elapsedTime >= 0 && elapsedTime <= totalDuration) {
                targetProgress = elapsedTime / totalDuration;
                shouldRender = true;
            }
        }

        if (!shouldRender && showDisablerDamageBoost.isEnabled() && disablerModule != null && disablerModule.isEnabled() &&
                disablerModule.disablers.getSetting("Bloxd").isEnabled() &&
                disablerModule.bloxdDamageBoost.isEnabled() &&
                disablerModule.damageBoostStartTime != 0L && disablerModule.bloxdDamageTime != null) {
            long totalDuration = disablerModule.bloxdDamageTime.getValue().longValue();
            long elapsedTime = System.currentTimeMillis() - disablerModule.damageBoostStartTime;
            if (elapsedTime >= 0 && elapsedTime <= totalDuration) {
                targetProgress = (float) elapsedTime / totalDuration;
                shouldRender = true;
            }
        }

        if (!shouldRender && showDisablerDamageFlight.isEnabled() && disablerModule != null && disablerModule.isEnabled() &&
                disablerModule.disablers.getSetting("Bloxd").isEnabled() &&
                disablerModule.bloxdDamageFlight.isEnabled() &&
                disablerModule.damageFlightStartTime != 0L && disablerModule.bloxdDamageTime != null) {
            long totalDuration = disablerModule.bloxdDamageTime.getValue().longValue();
            long elapsedTime = System.currentTimeMillis() - disablerModule.damageFlightStartTime;
            if (elapsedTime >= 0 && elapsedTime <= totalDuration) {
                targetProgress = (float) elapsedTime / totalDuration;
                shouldRender = true;
            }
        }

        updateAnimationState(shouldRender);
        renderProgressBar(shouldRender, targetProgress);
    }

    private void updateAnimationState(boolean shouldRender) {
        if (animationSetting.isEnabled()) {
            if (shouldRender) {
                progressBarOpenAnimation.setDirection(Direction.FORWARDS);
            } else {
                progressBarOpenAnimation.setDirection(Direction.BACKWARDS);
            }
        } else {
            if (!shouldRender) {
                currentRenderProgress = 0.0f;
            }
            progressBarOpenAnimation.setDirection(shouldRender ? Direction.FORWARDS : Direction.BACKWARDS);
        }
    }

    private void renderProgressBar(boolean shouldRender, float targetProgress) {
        float barWidth = 80;
        float barHeight = 5;

        float x = progressBarPos.getX();
        float y = progressBarPos.getY();

        progressBarPos.setWidth(barWidth);
        progressBarPos.setHeight(barHeight);

        if (shouldRender || (animationSetting.isEnabled() && !progressBarOpenAnimation.isDone())) {
            float animationProgress = animationSetting.isEnabled() ? progressBarOpenAnimation.getOutput().floatValue() : 1.0f;

            if (shouldRender) {
                currentRenderProgress = lerp(currentRenderProgress, targetProgress, 0.1f * mc.timer.timerSpeed);
            } else {
                currentRenderProgress = lerp(currentRenderProgress, 0.0f, 0.1f * mc.timer.timerSpeed);
                if (currentRenderProgress < 0.01f) {
                    currentRenderProgress = 0.0f;
                }
            }

            if (animationProgress > 0) {
                float animatedHeight = barHeight * animationProgress;
                float animatedY = y + (barHeight - animatedHeight) / 2f;

                RenderUtil.drawGradientRect(x, animatedY, x + barWidth, animatedY + animatedHeight,
                        new Color(0, 0, 0, (int)(150 * animationProgress)).getRGB(),
                        new Color(0, 0, 0, (int)(150 * animationProgress)).getRGB());

                Color startColor = Color.WHITE;
                Color endColor = Color.LIGHT_GRAY;
                if (HUDMod.getClientColors() != null) {
                     startColor = HUDMod.getClientColors().getFirst();
                     endColor = HUDMod.getClientColors().getSecond();
                }

                float filledWidth = barWidth * Math.max(0.0f, Math.min(1.0f, currentRenderProgress));

                RenderUtil.drawGradientRect(x, animatedY, x + filledWidth, animatedY + animatedHeight,
                        new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), (int)(startColor.getAlpha() * animationProgress)).getRGB(),
                        new Color(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), (int)(endColor.getAlpha() * animationProgress)).getRGB());
            }
        } else {
            currentRenderProgress = 0.0f;
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}