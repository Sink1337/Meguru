package dev.merguru.module.impl.render;

import dev.merguru.Merguru;
import dev.merguru.event.impl.render.Render3DEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.impl.player.ChestStealer;
import dev.merguru.module.settings.impl.BooleanSetting;
import dev.merguru.module.settings.impl.ColorSetting;
import dev.merguru.module.settings.impl.ModeSetting;
import dev.merguru.module.settings.impl.NumberSetting;
import dev.merguru.utils.render.ColorUtil;
import dev.merguru.utils.render.RenderUtil;
import dev.merguru.utils.tuples.Pair;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.BlockPos;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ChestESP extends Module {

    public final BooleanSetting outline = new BooleanSetting("Outline", false);
    public final BooleanSetting filled = new BooleanSetting("Filled", true);
    public final ModeSetting colorMode = new ModeSetting("Color Mode", "Sync", "Sync", "Random", "Custom");

    public final ColorSetting chestColor = new ColorSetting("Chest Color", Color.GREEN);
    public final ColorSetting openedChestColor = new ColorSetting("Opened Chest Color", Color.RED);

    public final NumberSetting filledAlpha = new NumberSetting("Filled Alpha", 255, 255, 0, 1);
    public final NumberSetting outlineAlpha = new NumberSetting("Outline Alpha", 255, 255, 0, 1);

    private final Map<Object, Color> tileEntityColorMap = new HashMap<>();

    public ChestESP() {
        super("ChestESP", Category.RENDER, "Highlights chests and ender chests");

        chestColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom"));
        openedChestColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom"));

        addSettings(outline, outlineAlpha, filled, filledAlpha, colorMode, chestColor, openedChestColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tileEntityColorMap.clear();
    }

    @Override
    public void onRender3DEvent(Render3DEvent e) {
        if (!Merguru.INSTANCE.isEnabled(ChestStealer.class)) {
            for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
                if (tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest) {
                    if (!tileEntity.isInvalid() && mc.theWorld.getBlockState(tileEntity.getPos()) != null) {
                        int renderColor = getRenderColor(tileEntity, filledAlpha.getValue().intValue());
                        int outlineRenderColor = getRenderColor(tileEntity, outlineAlpha.getValue().intValue());
                        RenderUtil.renderBlock(tileEntity.getPos(), renderColor, outlineRenderColor, outline.isEnabled(), filled.isEnabled());
                    }
                }
            }
            return;
        }

        for (BlockPos pos : ChestStealer.renderableChests) {
            TileEntity tileEntity = mc.theWorld.getTileEntity(pos);

            if (tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest) {
                if (!tileEntity.isInvalid() && mc.theWorld.getBlockState(pos) != null) {
                    int renderColor = getRenderColor(tileEntity, filledAlpha.getValue().intValue());
                    int outlineRenderColor = getRenderColor(tileEntity, outlineAlpha.getValue().intValue());
                    RenderUtil.renderBlock(tileEntity.getPos(), renderColor, outlineRenderColor, outline.isEnabled(), filled.isEnabled());
                }
            }
        }
    }

    private int getRenderColor(TileEntity tileEntity, int alphaValue) {
        Color finalColor = Color.WHITE;

        switch (colorMode.getMode()) {
            case "Custom":
                if (tileEntity instanceof TileEntityChest && ChestStealer.openedChests.contains(tileEntity.getPos())) {
                    finalColor = openedChestColor.getColor();
                } else {
                    finalColor = chestColor.getColor();
                }
                break;
            case "Sync":
                HUDMod hudMod = Merguru.INSTANCE.getModuleCollection().getModule(HUDMod.class);
                if (hudMod != null) {
                    Pair<Color, Color> clientColors = hudMod.getClientColors();
                    if (HUDMod.isRainbowTheme()) {
                        finalColor = clientColors.getFirst();
                    } else {
                        finalColor = ColorUtil.interpolateColorsBackAndForth(15, 0, clientColors.getFirst(), clientColors.getSecond(), false);
                    }
                } else {
                    finalColor = chestColor.getColor();
                }
                break;
            case "Random":
                if (tileEntityColorMap.containsKey(tileEntity)) {
                    finalColor = tileEntityColorMap.get(tileEntity);
                } else {
                    Color randomColor = ColorUtil.getRandomColor();
                    tileEntityColorMap.put(tileEntity, randomColor);
                    finalColor = randomColor;
                }
                break;
        }
        return new Color(finalColor.getRed(), finalColor.getGreen(), finalColor.getBlue(), alphaValue).getRGB();
    }
}