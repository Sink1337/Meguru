package dev.meguru.module.impl.misc;

import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.utils.misc.SoundUtils;
import net.minecraft.util.ResourceLocation;

public class Ciallo extends Module {

    private final ResourceLocation cialloSound = new ResourceLocation("meguru/sounds/Ciallo.wav");
    private final ResourceLocation boyangyangSound = new ResourceLocation("meguru/sounds/boyangyang.wav");

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