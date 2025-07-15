package dev.merguru.utils.player;

import dev.merguru.utils.Utils;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils implements Utils {

    public static Block getBlockAtPos(BlockPos pos) {
        if (mc.theWorld == null || pos == null) {
            return Blocks.air;
        }
        IBlockState blockState = mc.theWorld.getBlockState(pos);
        if (blockState == null) {
            return Blocks.air;
        }
        return blockState.getBlock();
    }

    public static boolean isValidBlock(Block block, boolean placing) {
        if (block == null) {
            return false;
        }

        if (block instanceof BlockCarpet ||
                block instanceof BlockSnow ||
                block instanceof BlockContainer ||
                block instanceof BlockBasePressurePlate ||
                block.getMaterial().isLiquid()) {
            return false;
        }

        if (placing && (block instanceof BlockSlab ||
                block instanceof BlockStairs ||
                block instanceof BlockLadder ||
                block instanceof BlockStainedGlassPane ||
                block instanceof BlockWall ||
                block instanceof BlockWeb ||
                block instanceof BlockCactus ||
                block instanceof BlockFalling ||
                block == Blocks.glass_pane ||
                block == Blocks.iron_bars)) {
            return false;
        }
        return (block.getMaterial() != null && (block.getMaterial().isSolid() || !block.isTranslucent() || block.isFullBlock()));
    }

    public static boolean isValidBlock(BlockPos pos) {
        if (mc.theWorld == null || pos == null) {
            return false;
        }
        return isValidBlock(getBlockAtPos(pos), false);
    }

    public static boolean isFluid(Block block) {
        if (block == null) {
            return false;
        }
        return block.getMaterial() == Material.lava || block.getMaterial() == Material.water;
    }

    public static boolean replaceable(BlockPos blockPos) {
        if (mc.thePlayer == null || mc.theWorld == null || blockPos == null) {
            return false;
        }
        Block block = getBlockAtPos(blockPos);
        if (block == null) {
            return false;
        }
        return block.isReplaceable(mc.theWorld, blockPos);
    }

    public static List<BlockPos> getAllBlocksInAABB(BlockPos minPos, BlockPos maxPos) {
        List<BlockPos> blockPositions = new ArrayList<>();
        if (minPos == null || maxPos == null) {
            return blockPositions;
        }
        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int intY = minPos.getY(); intY <= maxPos.getY(); intY++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    blockPositions.add(new BlockPos(x, intY, z));
                }
            }
        }
        return blockPositions;
    }

    public static boolean isInLiquid() {
        if (mc.thePlayer == null) return false;
        if (mc.thePlayer.getEntityBoundingBox() == null) return false;

        for (int x = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minX); x < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxX) + 1; x++) {
            for (int z = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minZ); z < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxZ) + 1; z++) {
                BlockPos pos = new BlockPos(x, (int) mc.thePlayer.getEntityBoundingBox().minY, z);
                Block block = getBlockAtPos(pos);
                if (block != null && !(block instanceof BlockAir)) {
                    return block instanceof BlockLiquid;
                }
            }
        }
        return false;
    }

    public static boolean isOnLiquid() {
        if (mc.thePlayer == null) return false;
        AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox();
        if (boundingBox == null) {
            return false;
        }
        boundingBox = boundingBox.contract(0.01D, 0.0D, 0.01D).offset(0.0D, -0.01D, 0.0D);
        boolean onLiquid = false;
        int y = (int) boundingBox.minY;

        for (int x = MathHelper.floor_double(boundingBox.minX); x < MathHelper.floor_double(boundingBox.maxX + 1.0D); ++x) {
            for (int z = MathHelper.floor_double(boundingBox.minZ); z < MathHelper.floor_double(boundingBox.maxZ + 1.0D); ++z) {
                BlockPos pos = new BlockPos(x, y, z);
                Block block = getBlockAtPos(pos);
                if (block != null && block != Blocks.air) {
                    if (!(block instanceof BlockLiquid)) return false;
                    onLiquid = true;
                }
            }
        }
        return onLiquid;
    }
}