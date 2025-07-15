package dev.merguru.utils.player;


import dev.merguru.module.impl.misc.EnumFacingUtils;
import dev.merguru.utils.Utils;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class PlayerUtil implements Utils {
    public static void debug(Object message) {
        String text = "§f[§3Maple§f] " + message;
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(text));
    }

    public static void log(Object message) {
        String text = String.valueOf(message);
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(text));
    }

    public static Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ) {
        if (mc.thePlayer.motionX + mc.thePlayer.motionZ + mc.thePlayer.motionY == -0.0784000015258789) return null;
        final List<Vec3> possibilities = new ArrayList<>();
        final int range = (int) (5 + (Math.abs(offsetX) + Math.abs(offsetZ)));

        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = PlayerUtil.blockRelativeToPlayer(x, y, z);

                    if (!(block instanceof BlockAir)) {
                        for (int x2 = -1; x2 <= 1; x2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));

                        for (int y2 = -1; y2 <= 1; y2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));

                        for (int z2 = -1; z2 <= 1; z2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                    }
                }
            }
        }

        possibilities.removeIf(vec3 -> mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) > 5 || !(PlayerUtil.block(vec3.xCoord, vec3.yCoord, vec3.zCoord) instanceof BlockAir));

        if (possibilities.isEmpty()) return null;

        possibilities.sort(Comparator.comparingDouble(vec3 -> {

            final double d0 = (mc.thePlayer.posX + offsetX) - vec3.xCoord;
            final double d1 = (mc.thePlayer.posY - 1 + offsetY) - vec3.yCoord;
            final double d2 = (mc.thePlayer.posZ + offsetZ) - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);

        }));

        return possibilities.get(0);
    }

    public static Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ, int blockRange) {
        final List<Vec3> possibilities = new ArrayList<>();
        final int range = (int) (blockRange + (Math.abs(offsetX) + Math.abs(offsetZ)));

        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = PlayerUtil.blockRelativeToPlayer(x, y, z);

                    if (!(block instanceof BlockAir)) {
                        for (int x2 = -1; x2 <= 1; x2 += 1)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));

                        for (int y2 = -1; y2 <= 1; y2 += 1)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));

                        for (int z2 = -1; z2 <= 1; z2 += 1)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                    }
                }
            }
        }

        possibilities.removeIf(vec3 -> mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) > blockRange || !(PlayerUtil.block(vec3.xCoord, vec3.yCoord, vec3.zCoord) instanceof BlockAir));

        if (possibilities.isEmpty()) return null;

        possibilities.sort(Comparator.comparingDouble(vec3 -> {

            final double d0 = (mc.thePlayer.posX + offsetX) - vec3.xCoord;
            final double d1 = (mc.thePlayer.posY - 1 + offsetY) - vec3.yCoord;
            final double d2 = (mc.thePlayer.posZ + offsetZ) - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);

        }));

        return possibilities.get(0);
    }

    public static boolean isBlockBlacklisted(final Item item) {
        return item instanceof ItemAnvilBlock || item.getUnlocalizedName().contains("sand") || item.getUnlocalizedName().contains("gravel") || item.getUnlocalizedName().contains("ladder") || item.getUnlocalizedName().contains("tnt") || item.getUnlocalizedName().contains("chest") || item.getUnlocalizedName().contains("web");
    }

    public static EnumFacingUtils getEnumFacing(final Vec3 position) {
        for (int x2 = -1; x2 <= 1; x2 += 2) {
            if (!(PlayerUtil.block(position.xCoord + x2, position.yCoord, position.zCoord) instanceof BlockAir)) {
                if (x2 > 0) {
                    return new EnumFacingUtils(EnumFacing.WEST, new Vec3(x2, 0, 0));
                } else {
                    return new EnumFacingUtils(EnumFacing.EAST, new Vec3(x2, 0, 0));
                }
            }
        }

        for (int y2 = -1; y2 <= 1; y2 += 2) {
            if (!(PlayerUtil.block(position.xCoord, position.yCoord + y2, position.zCoord) instanceof BlockAir)) {
                if (y2 < 0) {
                    return new EnumFacingUtils(EnumFacing.UP, new Vec3(0, y2, 0));
                }
            }
        }

        for (int z2 = -1; z2 <= 1; z2 += 2) {
            if (!(PlayerUtil.block(position.xCoord, position.yCoord, position.zCoord + z2) instanceof BlockAir)) {
                if (z2 < 0) {
                    return new EnumFacingUtils(EnumFacing.SOUTH, new Vec3(0, 0, z2));
                } else {
                    return new EnumFacingUtils(EnumFacing.NORTH, new Vec3(0, 0, z2));
                }
            }
        }

        return null;
    }

    public static BlockPos getBlockCorner(final BlockPos start, final BlockPos end) {
        for (int x = 0; x <= 1; ++x) {
            for (int y = 0; y <= 1; ++y) {
                for (int z = 0; z <= 1; ++z) {
                    final BlockPos pos = new BlockPos(end.getX() + x, end.getY() + y, end.getZ() + z);
                    if (!isBlockBetween(start, pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isBlockBetween(final BlockPos start, final BlockPos end) {
        final int startX = start.getX();
        final int startY = start.getY();
        final int startZ = start.getZ();
        final int endX = end.getX();
        final int endY = end.getY();
        final int endZ = end.getZ();
        final double diffX = endX - startX;
        final double diffY = endY - startY;
        final double diffZ = endZ - startZ;
        double x = startX;
        double y = startY;
        double z = startZ;
        final double STEP = 0.1;
        for (int STEPS = (int) Math.max(Math.abs(diffX), Math.max(Math.abs(diffY), Math.abs(diffZ))) * 4, i = 0; i < STEPS - 1; ++i) {
            x += diffX / STEPS;
            y += diffY / STEPS;
            z += diffZ / STEPS;
            if (x != endX || y != endY || z != endZ) {
                final BlockPos pos = new BlockPos(x, y, z);
                final Block block = mc.theWorld.getBlockState(pos).getBlock();
                if (block.getMaterial() != Material.air && block.getMaterial() != Material.water && !(block instanceof BlockVine) && !(block instanceof BlockLadder)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isBlockUnder(final double height) {
        return isBlockUnder(height, true);
    }

    public static boolean isBlockUnder(final double height, final boolean boundingBox) {
        if (boundingBox) {
            for (int offset = 0; offset < height; offset += 2) {
                final AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0.0, -offset, 0.0);
                if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                    return true;
                }
            }
        } else {
            for (int offset = 0; offset < height; ++offset) {
                if (blockRelativeToPlayer(0.0, -offset, 0.0).isFullBlock()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return mc.theWorld.getBlockState(new BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).getBlock();
    }


    public static Block block(final double x, final double y, final double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }


    public static int findSoup() {
        for (int i = 36; i < 45; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (itemStack != null && itemStack.getItem().equals(Items.mushroom_stew) && itemStack.stackSize > 0 && itemStack.getItem() instanceof ItemFood) {
                return i;
            }
        }
        return -1;
    }

    public static int findItem(final int startSlot, final int endSlot, final Item item) {
        for (int i = startSlot; i < endSlot; ++i) {
            final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasSpaceHotbar() {
        for (int i = 36; i < 45; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (itemStack == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOnGround(final double height) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, -height, 0.0)).isEmpty();
    }

    public static Block getBlock(final BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock();
    }

    public static Block getBlock(final double d, final double d2, final double d3) {
        return mc.theWorld.getBlockState(new BlockPos(d, d2, d3)).getBlock();
    }

    public static boolean isOnGround(final Entity entity, final double height) {
        return !mc.theWorld.getCollidingBoundingBoxes(entity, entity.getEntityBoundingBox().offset(0.0, -height, 0.0)).isEmpty();
    }

    public static boolean colorTeam(final EntityPlayer sb) {
        String targetName = sb.getDisplayName().getFormattedText().replace("§r", "");
        String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        return targetName.startsWith("§" + clientName.charAt(1));
    }

    public static boolean armorTeam(final EntityPlayer entityPlayer) {
        if (mc.thePlayer.inventory.armorInventory[3] != null && entityPlayer.inventory.armorInventory[3] != null) {
            final ItemStack myHead = mc.thePlayer.inventory.armorInventory[3];
            final ItemArmor myItemArmor = (ItemArmor) myHead.getItem();
            final ItemStack entityHead = entityPlayer.inventory.armorInventory[3];
            final ItemArmor entityItemArmor = (ItemArmor) entityHead.getItem();
            return String.valueOf(entityItemArmor.getColor(entityHead)).equals("10511680") || myItemArmor.getColor(myHead) == entityItemArmor.getColor(entityHead);
        }
        return false;
    }

    public static boolean scoreTeam(final EntityPlayer entityPlayer) {
        return mc.thePlayer.isOnSameTeam(entityPlayer);
    }

    public static boolean MovementInput() {
        return mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown();
    }

    public static boolean isInLiquid() {
        if (mc.thePlayer.isInWater()) {
            return true;
        }
        boolean inLiquid = false;
        final int y = (int) mc.thePlayer.getEntityBoundingBox().minY;
        for (int x = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minX); x < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxX) + 1; ++x) {
            for (int z = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minZ); z < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxZ) + 1; ++z) {
                final Block block = mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block != null && block.getMaterial() != Material.air) {
                    if (!(block instanceof BlockLiquid)) {
                        return false;
                    }
                    inLiquid = true;
                }
            }
        }
        return inLiquid;
    }

    public static boolean isOnLiquid() {
        AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox();
        if (boundingBox == null) {
            return false;
        }
        boundingBox = boundingBox.contract(0.01, 0.0, 0.01).offset(0.0, -0.01, 0.0);
        boolean onLiquid = false;
        final int y = (int) boundingBox.minY;
        for (int x = MathHelper.floor_double(boundingBox.minX); x < MathHelper.floor_double(boundingBox.maxX + 1.0); ++x) {
            for (int z = MathHelper.floor_double(boundingBox.minZ); z < MathHelper.floor_double(boundingBox.maxZ + 1.0); ++z) {
                final Block block = mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block != Blocks.air) {
                    if (!(block instanceof BlockLiquid)) {
                        return false;
                    }
                    onLiquid = true;
                }
            }
        }
        return onLiquid;
    }

    public Vec3 getPlacePossibility() {
        return getPlacePossibility(0, 0, 0);
    }
}
