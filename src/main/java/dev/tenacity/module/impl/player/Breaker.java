package dev.tenacity.module.impl.player;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.game.WorldEvent;
import dev.tenacity.event.impl.network.PacketEvent;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.ChatReceivedEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.TeleportEvent;
import dev.tenacity.event.impl.player.UpdateEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.movement.Scaffold;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.module.settings.impl.ColorSetting;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.ContinualAnimation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.font.FontUtil;
import dev.tenacity.utils.player.ChatUtil;
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
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;

import java.awt.*;

import static dev.tenacity.utils.server.PacketUtils.sendPacket;
import static sun.java2d.cmm.CMSManager.getModule;

public class Breaker extends Module {

    public final NumberSetting breakRange = new NumberSetting("Break Range", 4, 5, 1, 1);
    public final BooleanSetting breakSurroundings = new BooleanSetting("Break Top", true);
    public final BooleanSetting autoTool = new BooleanSetting("Auto Tool", true);
    public final BooleanSetting progressText = new BooleanSetting("Progress Text", true);
    public final BooleanSetting progressBar = new BooleanSetting("Progress Bar", true);
    public final BooleanSetting whitelistOwnBed = new BooleanSetting("Whitelist Own Bed", true);
    public final BooleanSetting swap = new BooleanSetting("Swap", false);
    public final BooleanSetting ignoreSlow = new BooleanSetting("Ignore Slow",false);
    public final BooleanSetting groundSpoof = new BooleanSetting("Hypixel Ground Spoof", false);
    public BlockPos bedPos;
    public boolean rotate = false;
    private float breakProgress;
    private int delayTicks;
    private Vec3 home;
    private boolean spoofed;
    public ContinualAnimation barAnim = new ContinualAnimation();
    float[] rot;

    public Breaker() {
        super("Breaker", Category.PLAYER, "Break Beds");
        addSettings(breakRange,breakSurroundings,swap,autoTool,whitelistOwnBed,progressBar,progressText,ignoreSlow,groundSpoof);
    }

    @Override
    public void onEnable() {
        rotate = false;
        bedPos = null;

        breakProgress = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset(true);
        super.onDisable();
    }

    @Override
    public void onTeleportEvent(TeleportEvent event){
        if(whitelistOwnBed.isEnabled()){
            final double distance = mc.thePlayer.getDistance(event.getPosX(), event.getPosY(), event.getPosZ());

            if (distance > 40) {
                home = new Vec3(event.getPosX(), event.getPosY(), event.getPosZ());
            }
        }
    }

    @Override
    public void onUpdateEvent(UpdateEvent event){

        setSuffix(swap.isEnabled() ? "Swap" : "Vanilla");

        if (Tenacity.INSTANCE.getModuleCollection().getModule(Scaffold.class).isEnabled() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            reset(true);
            return;
        }

        getBedPos();

        if (bedPos != null) {
            if (rotate) {
                rot = RotationUtils.getRotations(bedPos);
            }
            mine(bedPos);
        } else {
            reset(true);
        }
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (event.isPost())
            return;
        if (rotate && rot != null){
            event.setRotations(rot[0],rot[1]);
            RotationUtils.setVisualRotations(rot[0],rot[1]);
            rotate = false;
        }
        if (bedPos != null && groundSpoof.isEnabled() && !mc.thePlayer.onGround) {
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
        if (progressText.isEnabled() && bedPos != null) {
            RenderUtil.renderBlock(bedPos,HUDMod.color(1).getRGB(), true, true);

            if (breakProgress == 0.0f)
                return;

            final double n = bedPos.getX() + 0.5 - mc.getRenderManager().viewerPosX;
            final double n2 = bedPos.getY() + 0.5 - mc.getRenderManager().viewerPosY;
            final double n3 = bedPos.getZ() + 0.5 - mc.getRenderManager().viewerPosZ;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) n, (float) n2, (float) n3);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
            GlStateManager.scale(-0.02266667f, -0.02266667f, -0.02266667f);
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            String progressStr = (int) (100.0 * (this.breakProgress / 1.0)) + "%";
            mc.fontRendererObj.drawString(progressStr, (float) (-mc.fontRendererObj.getStringWidth(progressStr) / 2), -3.0f, -1, true);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        if (progressBar.isEnabled() && bedPos != null) {

            if (breakProgress == 0.0f)
                return;
            final ScaledResolution resolution = new ScaledResolution(mc);
            final int x = resolution.getScaledWidth() / 2;
            final int y = resolution.getScaledHeight() - 70;
            final float thickness = 6;

            final int width = resolution.getScaledWidth() / 4;
            final int half = width / 2;
            barAnim.animate(width * (breakProgress), 40);

            RoundedUtil.drawRound(x - half, y, width, thickness, thickness / 2, new Color(20,20,20,100));

            RoundedUtil.drawGradientHorizontal(x - half, y, barAnim.getOutput(), thickness, thickness / 2, HUDMod.color(1), HUDMod.color(3));

            String progressStr = (int) (100.0 * (this.breakProgress / 1.0)) + "%";

            FontUtil.idkFont12.drawCenteredStringWithShadow(progressStr, x, y + 1, -1);
        }
    }


    private void getBedPos() {
        if (home != null && mc.thePlayer.getDistanceSq(home.xCoord, home.yCoord, home.zCoord) < 35 * 35 && whitelistOwnBed.isEnabled()) {
            return;
        }
        bedPos = null;
        double range = breakRange.getValue();
        for (double x = mc.thePlayer.posX - range; x <= mc.thePlayer.posX + range; x++) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - range; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + range; y++) {
                for (double z = mc.thePlayer.posZ - range; z <= mc.thePlayer.posZ + range; z++) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(pos).getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                        if (breakSurroundings.isEnabled() && isBedCovered(pos)) {
                            bedPos = pos.add(0, 1, 0);
                        } else {
                            bedPos = pos;
                        }
                        break;
                    }
                }
            }
        }
    }

    private void mine(BlockPos blockPos) {
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        if (blockState.getBlock() instanceof BlockAir) {
            return;
        }

        if (breakProgress == 0) {
            rotate = true;
            if (autoTool.isEnabled() && !swap.isEnabled()) {
                doAutoTool(blockPos);
            }
            mc.thePlayer.swingItem();
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, bedPos, EnumFacing.UP));
        } else if (breakProgress >= 1) {
            rotate = true;
            if (autoTool.isEnabled() && swap.isEnabled()) {
                doAutoTool(blockPos);
            }
            mc.thePlayer.swingItem();
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, bedPos, EnumFacing.UP));

            reset(false);
            return;
        } else {
            if (!swap.isEnabled()) {
                rotate = true;
            }

            if (autoTool.isEnabled()) {
                if (!swap.isEnabled()) {
                    doAutoTool(blockPos);
                } else {
                    //mc.thePlayer.inventory.currentItem = 0;
                }
            }

            mc.thePlayer.swingItem();
        }

        if(swap.isEnabled()){
            breakProgress += (getBlockHardness(bedPos, PlayerUtils.findTool(bedPos) != -1 ? mc.thePlayer.inventory.getStackInSlot(PlayerUtils.findTool(bedPos)) : mc.thePlayer.getHeldItem(), ignoreSlow.isEnabled() , groundSpoof.isEnabled()));
        } else {
            breakProgress += mc.theWorld.getBlockState(bedPos).getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, bedPos);
        }

        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), bedPos, (int) (breakProgress * 10));
    }

    private void reset(boolean resetRotate) {
        if (bedPos != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), bedPos, -1);
            //test
            //sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,bedPos,EnumFacing.DOWN));
        }

        breakProgress = 0;
        delayTicks = 5;
        bedPos = null;
        rotate = !resetRotate;
    }
    private void doAutoTool(BlockPos pos) {
        if(PlayerUtils.findTool(pos) != -1) {
            mc.thePlayer.inventory.currentItem = PlayerUtils.findTool(pos);
        }
    }

    private boolean isBedCovered(BlockPos headBlockBedPos) {
        BlockPos headBlockBedPosOffSet1 = headBlockBedPos.add(1, 0, 0);
        BlockPos headBlockBedPosOffSet2 = headBlockBedPos.add(-1, 0, 0);
        BlockPos headBlockBedPosOffSet3 = headBlockBedPos.add(0, 0, 1);
        BlockPos headBlockBedPosOffSet4 = headBlockBedPos.add(0, 0, -1);

        if (!isBlockCovered(headBlockBedPos)) {
            return false;
        } else if (mc.theWorld.getBlockState(headBlockBedPosOffSet1).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(headBlockBedPosOffSet1).getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
            return isBlockCovered(headBlockBedPosOffSet1);
        } else if (mc.theWorld.getBlockState(headBlockBedPosOffSet2).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(headBlockBedPosOffSet2).getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
            return isBlockCovered(headBlockBedPosOffSet2);
        } else if (mc.theWorld.getBlockState(headBlockBedPosOffSet3).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(headBlockBedPosOffSet3).getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
            return isBlockCovered(headBlockBedPosOffSet3);
        } else if (mc.theWorld.getBlockState(headBlockBedPosOffSet4).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(headBlockBedPosOffSet4).getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
            return isBlockCovered(headBlockBedPosOffSet4);
        }

        return false;
    }

    private boolean isBlockCovered(BlockPos blockPos) {
        BlockPos[] directions = {
                blockPos.add(0, 1, 0), // Up
                blockPos.add(1, 0, 0), // East
                blockPos.add(-1, 0, 0), // West
                blockPos.add(0, 0, 1), // South
                blockPos.add(0, 0, -1) // North
        };

        for (BlockPos pos : directions) {
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block instanceof BlockAir || block.getMaterial() instanceof MaterialLiquid) {
                return false;
            }
        }

        return true;
    }

    public static float getBlockHardness(final BlockPos blockPos, final ItemStack itemStack, boolean ignoreSlow, boolean ignoreGround) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        final float getBlockHardness = block.getBlockHardness(mc.theWorld, null);
        if (getBlockHardness < 0.0f) {
            return 0.0f;
        }
        return (block.getMaterial().isToolNotRequired() || (itemStack != null && itemStack.canHarvestBlock(block))) ? (getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 30.0f) : (getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 100.0f);
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