package dev.tenacity.utils.player;

import dev.tenacity.module.impl.misc.EnumFacingUtils;
import dev.tenacity.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class PlayerUtils implements Utils {

    public static Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public static int findTool(final BlockPos blockPos) {
        float bestSpeed = 1;
        int bestSlot = -1;

        final IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack != null) {

                final float speed = itemStack.getStrVsBlock(blockState.getBlock());

                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    public static boolean overVoid() {
        double playerPosY = mc.thePlayer.posY;
        for (int y = (int) playerPosY; y >= 0; y--) {
            BlockPos currentPos = new BlockPos(mc.thePlayer.posX, y, mc.thePlayer.posZ);
            Block blockAtPos = mc.theWorld.getBlockState(currentPos).getBlock();
            if (!(blockAtPos instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public static EnumFacingUtils getEnumFacing(final Vec3 position) {
        for (int x2 = -1; x2 <= 1; x2 += 2) {
            if (!(PlayerUtils.block(position.xCoord + x2, position.yCoord, position.zCoord) instanceof BlockAir)) {
                if (x2 > 0) {
                    return new EnumFacingUtils(EnumFacing.WEST, new Vec3(x2, 0, 0));
                } else {
                    return new EnumFacingUtils(EnumFacing.EAST, new Vec3(x2, 0, 0));
                }
            }
        }

        for (int y2 = -1; y2 <= 1; y2 += 2) {
            if (!(PlayerUtils.block(position.xCoord, position.yCoord + y2, position.zCoord) instanceof BlockAir)) {
                if (y2 < 0) {
                    return new EnumFacingUtils(EnumFacing.UP, new Vec3(0, y2, 0));
                }
            }
        }

        for (int z2 = -1; z2 <= 1; z2 += 2) {
            if (!(PlayerUtils.block(position.xCoord, position.yCoord, position.zCoord + z2) instanceof BlockAir)) {
                if (z2 < 0) {
                    return new EnumFacingUtils(EnumFacing.SOUTH, new Vec3(0, 0, z2));
                } else {
                    return new EnumFacingUtils(EnumFacing.NORTH, new Vec3(0, 0, z2));
                }
            }
        }

        return null;
    }

    public static Block block(final double x, final double y, final double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

}
