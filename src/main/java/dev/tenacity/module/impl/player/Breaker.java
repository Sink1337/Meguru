package dev.tenacity.module.impl.player;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.module.settings.impl.ColorSetting;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.player.PlayerUtils;
import dev.tenacity.utils.player.RotationUtils;
import dev.tenacity.utils.render.ColorUtil;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.render.RoundedUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.IFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition;

import java.awt.*;

public class Breaker extends Module {

    public final MultipleBoolSetting targetBlocks = new MultipleBoolSetting("Target Blocks",
            new BooleanSetting("Bed", true),
            new BooleanSetting("Lime Stained Glass", false));
    public final NumberSetting breakRange = new NumberSetting("Break Range", 4, 6, 1, 0.5);

    public final ModeSetting throughWalls = new ModeSetting("ThroughWalls", "None", "None", "Raycast", "Around");
    public final BooleanSetting surroundings = new BooleanSetting("Surroundings", true);

    public final BooleanSetting autoTool = new BooleanSetting("Auto Tool", true);
    public final BooleanSetting whitelistOwnBed = new BooleanSetting("Whitelist Own Bed", true);
    public final BooleanSetting swap = new BooleanSetting("Swap", false);
    public final BooleanSetting ignoreSlow = new BooleanSetting("Ignore Slow", false);
    public final BooleanSetting groundSpoof = new BooleanSetting("Hypixel Ground Spoof", false);
    public final BooleanSetting rotation = new BooleanSetting("Rotation", true);
    public final BooleanSetting renderTarget = new BooleanSetting("Render Target", true);
    public final BooleanSetting outline = new BooleanSetting("Outline", true);
    public final BooleanSetting filled = new BooleanSetting("Filled", false);
    public final ModeSetting colorMode = new ModeSetting("Color Mode", "Sync", "Sync", "Custom");
    public final ColorSetting targetColor = new ColorSetting("Target Color", new Color(0, 150, 255));
    public final NumberSetting filledAlpha = new NumberSetting("Filled Alpha", 100, 255, 0, 1);
    public final NumberSetting outlineAlpha = new NumberSetting("Outline Alpha", 255, 255, 0, 1);

    public final BooleanSetting progressText = new BooleanSetting("Progress Text", true);
    public final BooleanSetting progressBar = new BooleanSetting("Progress Bar", true);


    public BlockPos targetBlockPos;
    public BlockPos originalTargetBlock;
    public boolean rotate = false;
    private float breakProgress;
    private int delayTicks;
    private Vec3 home;
    private boolean spoofed;
    public Animation barAnim = new DecelerateAnimation(250, 1.0);


    public Breaker() {
        super("Breaker", Category.PLAYER, "Automatically breaks certain blocks");

        ignoreSlow.addParent(swap, ParentAttribute.BOOLEAN_CONDITION);
        groundSpoof.addParent(swap, ParentAttribute.BOOLEAN_CONDITION);

        filledAlpha.addParent(filled, BooleanSetting::isEnabled);
        outlineAlpha.addParent(outline, BooleanSetting::isEnabled);
        targetColor.addParent(colorMode, modeSetting -> modeSetting.is("Custom"));

        addSettings(targetBlocks, breakRange, throughWalls, surroundings, autoTool, whitelistOwnBed, swap, ignoreSlow, groundSpoof, rotation,
                renderTarget, outline, filled, colorMode, targetColor, filledAlpha, outlineAlpha,
                progressText, progressBar);
    }

    @Override
    public void onEnable() {
        rotate = false;
        targetBlockPos = null;
        originalTargetBlock = null;
        breakProgress = 0;
        home = null;
        barAnim.setDirection(Direction.FORWARDS);
        barAnim.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset(true);
        super.onDisable();
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        originalTargetBlock = findActualTargetBlock();

        targetBlockPos = determineBreakTarget(originalTargetBlock);


        if (targetBlockPos != null) {
            if (rotation.isEnabled()) {
                if (rotate) {
                    float[] rot = RotationUtils.getRotations(targetBlockPos.getX() + 0.5, targetBlockPos.getY() + 0.5, targetBlockPos.getZ() + 0.5);

                    if (event.isPre()) {
                        event.setYaw(rot[0]);
                        event.setPitch(rot[1]);
                    }
                    rotate = false;
                }
            }
            mine(targetBlockPos);
        } else {
            reset(true);
        }

        if (event.isPost())
            return;

        if (targetBlockPos != null && groundSpoof.isEnabled() && !mc.thePlayer.onGround) {
            if (mc.thePlayer.ticksExisted % 2 == 0) {
                mc.timer.timerSpeed = 0.5f;
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                spoofed = true;
            } else {
                mc.timer.timerSpeed = 1f;
                spoofed = false;
            }
        } else if (spoofed) {
            mc.timer.timerSpeed = 1f;
            spoofed = false;
        }
    }

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        if (originalTargetBlock == null) return;

        if (renderTarget.isEnabled()) {
            Color baseColor = getColor(targetColor.getColor());
            int fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), filledAlpha.getValue().intValue()).getRGB();
            int outlineColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), outlineAlpha.getValue().intValue()).getRGB();

            RenderUtil.renderBlock(originalTargetBlock, fillColor, outlineColor, outline.isEnabled(), filled.isEnabled());
        }

        if (progressText.isEnabled()) {
            BlockPos currentBreakingPos = targetBlockPos;
            if (currentBreakingPos == null || breakProgress == 0.0f) return;

            final double n = currentBreakingPos.getX() + 0.5 - mc.getRenderManager().viewerPosX;
            final double n2 = currentBreakingPos.getY() + 0.5 - mc.getRenderManager().viewerPosY;
            final double n3 = currentBreakingPos.getZ() + 0.5 - mc.getRenderManager().viewerPosZ;

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) n, (float) n2, (float) n3);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
            GlStateManager.scale(-0.02266667f, -0.02266667f, -0.02266667f);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            String progressStr = (int) (breakProgress * 100.0) + "%";
            IFontRenderer fontRenderer = mc.fontRendererObj;
            float textWidth = fontRenderer.getStringWidth(progressStr);
            fontRenderer.drawStringWithShadow(progressStr, -textWidth / 2, -3.0f, -1);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        if (progressBar.isEnabled() && targetBlockPos != null) {

            if (breakProgress == 0.0f)
                return;

            final ScaledResolution resolution = new ScaledResolution(mc);
            final int x = resolution.getScaledWidth() / 2;
            final int y = resolution.getScaledHeight() - 70;
            final float thickness = 6;

            final int width = resolution.getScaledWidth() / 4;
            final int half = width / 2;

            barAnim.setEndPoint(width);
            barAnim.setDirection(Direction.FORWARDS);

            RoundedUtil.drawRound(x - half, y, width, thickness, thickness / 2, new Color(0, 0, 0, 150));

            Color barColor = getColor(targetColor.getColor());
            RoundedUtil.drawRound(x - half, y, barAnim.getOutput().floatValue() * breakProgress, thickness, thickness / 2, barColor);

            String progressStr = (int) (breakProgress * 100.0) + "%";
            float textWidth = interFont12.getStringWidth(progressStr);
            interFont12.drawStringWithShadow(progressStr, x - textWidth / 2, y + 1, -1);
        }
    }

    private Color getColor(Color customColor) {
        switch (colorMode.getMode()) {
            case "Sync":
                HUDMod hudMod = Tenacity.INSTANCE.getModuleCollection().getModule(HUDMod.class);
                if (hudMod != null) {
                    if (HUDMod.isRainbowTheme()) {
                        return hudMod.getClientColors().getFirst();
                    } else {
                        return ColorUtil.interpolateColorsBackAndForth(15, 0, hudMod.getClientColors().getFirst(), hudMod.getClientColors().getSecond(), false);
                    }
                } else {
                    return customColor;
                }
            case "Custom":
            default:
                return customColor;
        }
    }

    private BlockPos findActualTargetBlock() {
        if (home != null && mc.thePlayer.getDistanceSq(home.xCoord, home.yCoord, home.zCoord) < 35 * 35 && whitelistOwnBed.isEnabled()) {
            return null;
        }

        BlockPos foundTarget = null;
        double range = breakRange.getValue();
        double minDistanceSq = Double.MAX_VALUE;

        for (int x = (int) (mc.thePlayer.posX - range); x <= (int) (mc.thePlayer.posX + range); x++) {
            for (int y = (int) (mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - range); y <= (int) (mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + range); y++) {
                for (int z = (int) (mc.thePlayer.posZ - range); z <= (int) (mc.thePlayer.posZ + range); z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    IBlockState blockState = mc.theWorld.getBlockState(currentPos);
                    Block block = blockState.getBlock();

                    boolean isTargetBlock = false;

                    if (targetBlocks.getSetting("Bed").isEnabled() && block instanceof BlockBed && blockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                        isTargetBlock = true;
                    }
                    if (targetBlocks.getSetting("Lime Stained Glass").isEnabled() && block instanceof BlockStainedGlass && blockState.getValue(BlockStainedGlass.COLOR) == EnumDyeColor.LIME) {
                        isTargetBlock = true;
                    }

                    if (isTargetBlock) {
                        boolean canHitThisBlock = false;
                        if (surroundings.isEnabled()) {
                            canHitThisBlock = true;
                        } else {
                            canHitThisBlock = isHitable(currentPos, throughWalls.getMode());
                        }

                        if (canHitThisBlock) {
                            double distanceSq = mc.thePlayer.getDistanceSq(currentPos);
                            if (distanceSq < minDistanceSq) {
                                minDistanceSq = distanceSq;
                                foundTarget = currentPos;
                            }
                        }
                    }
                }
            }
        }
        return foundTarget;
    }

    private BlockPos determineBreakTarget(BlockPos actualTarget) {
        if (actualTarget == null) {
            return null;
        }

        if (surroundings.isEnabled()) {
            Vec3 eyesPos = mc.thePlayer.getPositionEyes(1F);
            float[] rotations = RotationUtils.getRotations(actualTarget.getX() + 0.5, actualTarget.getY() + 0.5, actualTarget.getZ() + 0.5);
            Vec3 lookVec = RotationUtils.getVectorForRotation(rotations[1], rotations[0]);
            Vec3 vec3 = eyesPos.addVector(lookVec.xCoord * breakRange.getValue(), lookVec.yCoord * breakRange.getValue(), lookVec.zCoord * breakRange.getValue());

            MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, vec3, false, false, true);

            if (movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos rayHitPos = movingObjectPosition.getBlockPos();
                if (!rayHitPos.equals(actualTarget) && mc.theWorld.getBlockState(rayHitPos).getBlock() != Blocks.air) {
                    return rayHitPos;
                }
            }
            return actualTarget;
        }

        if (isHitable(actualTarget, throughWalls.getMode())) {
            return actualTarget;
        }

        return null;
    }


    private void mine(BlockPos blockPos) {
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        if (blockState.getBlock() instanceof BlockAir) {
            reset(true);
            return;
        }

        if (breakProgress == 0) {
            rotate = true; // 标记需要转头
            if (autoTool.isEnabled() && !swap.isEnabled()) {
                doAutoTool(blockPos);
            }
            mc.thePlayer.swingItem();
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, EnumFacing.UP));
        } else if (breakProgress >= 1) {
            rotate = true; // 标记需要转头
            if (autoTool.isEnabled() && swap.isEnabled()) {
                doAutoTool(blockPos);
            }
            mc.thePlayer.swingItem();
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.UP));

            reset(false);
            return;
        } else {
            if (!swap.isEnabled()) {
                rotate = true; // 标记需要转头
            }

            if (autoTool.isEnabled()) {
                if (!swap.isEnabled()) {
                    doAutoTool(blockPos);
                } else {
                }
            }

            mc.thePlayer.swingItem();
        }

        if(swap.isEnabled()){
            breakProgress += (getBlockHardness(blockPos, PlayerUtils.findTool(blockPos) != -1 ? mc.thePlayer.inventory.getStackInSlot(PlayerUtils.findTool(blockPos)) : mc.thePlayer.getHeldItem(), ignoreSlow.isEnabled() , groundSpoof.isEnabled()));
        } else {
            breakProgress += mc.theWorld.getBlockState(blockPos).getBlock().getPlayerRelativeBlockHardness(mc.thePlayer);
        }

        barAnim.reset();
        barAnim.setEndPoint(1.0);
        barAnim.setDirection(Direction.FORWARDS);

        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int) (breakProgress * 10));
    }

    private void reset(boolean resetRotate) {
        if (targetBlockPos != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), targetBlockPos, -1);
        }

        breakProgress = 0;
        delayTicks = 5;
        targetBlockPos = null;
        originalTargetBlock = null;
        rotate = !resetRotate;
        barAnim.setDirection(Direction.BACKWARDS);
        barAnim.reset();
    }
    private void doAutoTool(BlockPos pos) {
        if(PlayerUtils.findTool(pos) != -1) {
            mc.thePlayer.inventory.currentItem = PlayerUtils.findTool(pos);
        }
    }

    private boolean isHitable(BlockPos blockPos, String mode) {
        switch (mode.toLowerCase()) {
            case "raycast":
                Vec3 eyesPos = mc.thePlayer.getPositionEyes(1F);
                Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, blockCenter, false, true, false);

                return movingObjectPosition != null && movingObjectPosition.getBlockPos().equals(blockPos);
            case "around":
                return isHitableAround(blockPos);
            case "none":
            default:
                return true;
        }
    }

    private boolean isHitableAround(BlockPos blockPos) {
        BlockPos[] directions = {
                blockPos.up(),
                blockPos.down(),
                blockPos.north(),
                blockPos.south(),
                blockPos.east(),
                blockPos.west()
        };

        for (BlockPos pos : directions) {
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            Material material = block.getMaterial();
            if (block instanceof BlockAir || material instanceof MaterialLiquid) {
                return true;
            }
        }
        return false;
    }


    public static float getBlockHardness(final BlockPos blockPos, final ItemStack itemStack, boolean ignoreSlow, boolean ignoreGround) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        final float blockHardnessValue = block.getBlockHardness();
        if (blockHardnessValue < 0.0f) {
            return 0.0f;
        }
        return (block.getMaterial().isToolNotRequired() || (itemStack != null && itemStack.canHarvestBlock(block))) ? (getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / blockHardnessValue / 30.0f) : (getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / blockHardnessValue / 100.0f);
    }

    public static float getToolDigEfficiency(ItemStack itemStack, Block block, boolean ignoreSlow, boolean ignoreGround) {
        float n = (itemStack == null) ? 1.0f : itemStack.getItem().getStrVsBlock(itemStack, block);
        if (n > 1.0f) {
            final int getEnchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            if (getEnchantmentLevel > 0 && itemStack != null) {
                n += getEnchantmentLevel * getEnchantmentLevel + 1;
            }
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            n *= 1.0f + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2f;
        }
        if (!ignoreSlow) {
            if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
                float n2;
                switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                    case 0: {
                        n2 = 0.3f;
                        break;
                    }
                    case 1: {
                        n2 = 0.09f;
                        break;
                    }
                    case 2: {
                        n2 = 0.0027f;
                        break;
                    }
                    default: {
                        n2 = 8.1E-4f;
                        break;
                    }
                }
                n *= n2;
            }
            if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
                n /= 5.0f;
            }
            if (!mc.thePlayer.onGround && !ignoreGround) {
                n /= 5.0f;
            }
        }
        return n;
    }
}