package dev.tenacity.module.impl.misc;

import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.utils.misc.SoundUtils;
import net.minecraft.util.ResourceLocation;

public class Ciallo extends Module {

    private final ResourceLocation cialloSound = new ResourceLocation("Tenacity/Sounds/Ciallo.wav");
    private final ResourceLocation boyangyangSound = new ResourceLocation("Tenacity/Sounds/boyangyang.wav");

    public Ciallo() {
        super("Ciallo", Category.MISC, "Plays sound on enable/disable");
    }

    @Override
    public void onEnable() {
        SoundUtils.playSound(cialloSound, 0.8f);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        SoundUtils.playSound(boyangyangSound, 0.8f);
        super.onDisable();
    }
}