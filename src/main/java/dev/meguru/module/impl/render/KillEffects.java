package dev.meguru.module.impl.render;

import dev.meguru.event.impl.player.UpdateEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.utils.animations.ContinualAnimation;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;

public class KillEffects extends Module {

    private final ModeSetting killEffectValue = new ModeSetting("KillEffect", "Squid", "LightningBolt",
            "Flame",
            "Smoke",
            "Water",
            "Love",
            "Blood",
            "Squid",
            "Off");
    private final ModeSetting killSoundValue = new ModeSetting("KillSound", "Squid", "Squid",
            "Off");
    private final ContinualAnimation anim = new ContinualAnimation();
    private EntityLivingBase target;
    private EntitySquid squid;
    private double percent = 0.0;

    public KillEffects() {
        super("KillEffects", Category.RENDER, "Plays animation on killing another player");
        addSettings(killEffectValue,killSoundValue);
    }

    public static void playSound(String st, float volume) {
        new Thread(() -> {
            try {
                AudioInputStream as = AudioSystem.getAudioInputStream(new BufferedInputStream(mc.getResourceManager().getResource(new ResourceLocation("meguru/sounds/" + st)).getInputStream()));
                Clip clip = AudioSystem.getClip();
                clip.open(as);
                clip.start();
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volume);
                clip.start();
            } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public static float nextFloat(float startInclusive, float endInclusive) {
        if (startInclusive == endInclusive || endInclusive - startInclusive <= 0.0f) {
            return startInclusive;
        }
        return (float) ((double) startInclusive + (double) (endInclusive - startInclusive) * Math.random());
    }

    public double easeInOutCirc(double x) {
        return x < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * x + 2.0, 2.0)) + 1.0) / 2.0;
    }



    @Override
    public void onUpdateEvent(UpdateEvent event) {
        if (TargetManager.target != null) {
            target = TargetManager.target;
        }
        if (killEffectValue.getMode().equals("Squid") && squid != null) {
            if (mc.theWorld.loadedEntityList.contains(squid)) {
                if (percent < 1.0) {
                    percent += Math.random() * 0.048;
                }
                if (percent >= 1.0) {
                    percent = 0.0;
                    for (int i = 0; i <= 8; ++i) {
                        mc.effectRenderer.emitParticleAtEntity(squid, EnumParticleTypes.FLAME);
                    }
                    mc.theWorld.removeEntity(squid);
                    squid = null;
                    return;
                }
            } else {
                percent = 0.0;
            }
            double easeInOutCirc = easeInOutCirc(1.0 - percent);
            anim.animate((float) easeInOutCirc, 500);
            squid.setPositionAndUpdate(squid.posX, squid.posY + (double) anim.getOutput() * 0.9, squid.posZ);
        }
        if (squid != null && killEffectValue.getMode().equals("Squid")) {
            squid.squidPitch = 0.0f;
            squid.prevSquidPitch = 0.0f;
            squid.squidYaw = 0.0f;
            squid.squidRotation = 90.0f;
        }
        if (target != null && !mc.theWorld.loadedEntityList.contains(this.target)) {
            if (killSoundValue.getMode().equals("Squid")) {
                playSound("kill.wav", 0.6f);
            }
            if (killEffectValue.getMode().equals("LightningBolt")) {
                EntityLightningBolt entityLightningBolt = new EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ);
                mc.theWorld.addEntityToWorld((int) (-Math.random() * 100000.0), entityLightningBolt);
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "ambient.weather.thunder", 1.0f, 1.0f, false);
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.explode", 1.0f, 1.0f, false);
                for (int i = 0; i <= 8; ++i) {
                    mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.FLAME);
                }
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "item.fireCharge.use", 1.0f, 1.0f, false);
            }
            if (killEffectValue.getMode().equals("Squid")) {
                squid = new EntitySquid(mc.theWorld);
                mc.theWorld.addEntityToWorld(-8, squid);
                squid.setPosition(target.posX, target.posY, target.posZ);
            }
            target = null;
        }
        if (target != null && !target.isDead) {
            switch (killEffectValue.getMode()) {
                case "Flame": {
                    mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.FLAME);
                    target = null;
                    break;
                }
                case "Smoke": {
                    mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.SMOKE_LARGE);
                    target = null;
                    break;
                }
                case "Water": {
                    mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.WATER_DROP);
                    target = null;
                    break;
                }
                case "Love": {
                    mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.HEART);
                    mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.WATER_DROP);
                    target = null;
                    break;
                }
                case "Blood": {
                    for (int i = 0; i < 10; ++i) {
                        mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.BLOCK_CRACK.getParticleID(), target.posX, target.posY + (double) (target.height / 2.0f), target.posZ, target.motionX + (double) nextFloat(-0.5f, 0.5f), target.motionY + (double) nextFloat(-0.5f, 0.5f), target.motionZ + (double) nextFloat(-0.5f, 0.5f), Block.getStateId(Blocks.redstone_block.getDefaultState()));
                    }
                    target = null;
                }
            }
        }
    }

}
