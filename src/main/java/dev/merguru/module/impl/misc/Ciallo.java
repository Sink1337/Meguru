package dev.merguru.module.impl.misc;

import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.utils.misc.SoundUtils;
import net.minecraft.util.ResourceLocation;

public class Ciallo extends Module {

    private final ResourceLocation cialloSound = new ResourceLocation("merguru/sounds/Ciallo.wav");
    private final ResourceLocation boyangyangSound = new ResourceLocation("merguru/sounds/boyangyang.wav");

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