package dev.tenacity.utils.player;

import dev.tenacity.Tenacity;
import dev.tenacity.module.impl.player.Scaffold;
import dev.tenacity.module.impl.movement.Speed;
import dev.tenacity.utils.Utils;
import dev.tenacity.utils.addons.vector.Rotation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class ScaffoldUtils implements Utils {

    @Getter
    public static class BlockCache {
        private BlockPos position;
        @Setter
        private EnumFacing facing;
        @Setter
        private Rotation rotation;
        @Setter
        private Vec3 hitVec;

        public BlockCache(BlockPos position, EnumFacing facing) {
            this.position = position;
            this.facing = facing;
        }

    }

    public static double getYLevel() {
        if (!Scaffold.keepY.isEnabled() || Scaffold.keepYMode.is("Speed toggled") && !Tenacity.INSTANCE.isEnabled(Speed.class)) {
            return mc.thePlayer.posY - 1.0;
        }
        return mc.thePlayer.posY - 1.0 >= Scaffold.keepYCoord && Math.max(mc.thePlayer.posY, Scaffold.keepYCoord)
                - Math.min(mc.thePlayer.posY, Scaffold.keepYCoord) <= 3.0 && !mc.gameSettings.keyBindJump.isKeyDown()
                ? Scaffold.keepYCoord
                : mc.thePlayer.posY - 1.0;
    }

    public static BlockCache getBlockInfo() {
        final BlockPos belowBlockPos = new BlockPos(mc.thePlayer.posX, getYLevel() - (Scaffold.isDownwards() ? 1 : 0), mc.thePlayer.posZ);
        if (mc.theWorld.getBlockState(belowBlockPos).getBlock() instanceof BlockAir) {
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 4; z++) {
                    for (int i = 1; i > -3; i -= 2) {
                        final BlockPos blockPos = belowBlockPos.add(x * i, 0, z * i);
                        if (mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockAir) {
                            for (EnumFacing direction : EnumFacing.values()) {
                                final BlockPos block = blockPos.offset(direction);
                                final Material material = mc.theWorld.getBlockState(block).getBlock().getMaterial();
                                if (material.isSolid() && !material.isLiquid()) {
                                    return new BlockCache(block, direction.getOpposite());
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                if (isBlockValid(itemBlock.getBlock())) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int getBlockCount() {
        int count = 0;
        if (mc == null || mc.thePlayer == null || mc.thePlayer.inventory == null || mc.thePlayer.inventory.mainInventory == null) {
            return 0;
        }

        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                if (itemBlock.getBlock() != null && isBlockValid(itemBlock.getBlock())) {
                    count += itemStack.stackSize;
                }
            }
        }
        return count;
    }

    private static boolean isBlockValid(final Block block) {
        return (block.isFullBlock() || block == Blocks.glass) &&
                block != Blocks.sand &&
                block != Blocks.gravel &&
                block != Blocks.dispenser &&
                block != Blocks.command_block &&
                block != Blocks.noteblock &&
                block != Blocks.furnace &&
                block != Blocks.crafting_table &&
                block != Blocks.tnt &&
                block != Blocks.dropper &&
                block != Blocks.beacon;
    }

    public static Vec3 getHypixelVec3(BlockCache data) {
        BlockPos pos = data.position;
        EnumFacing face = data.facing;
        double x = (double) pos.getX() + 0.5, y = (double) pos.getY() + 0.5, z = (double) pos.getZ() + 0.5;
        if (face != EnumFacing.UP && face != EnumFacing.DOWN) {
            y += 0.5;
        } else {
            x += 0.3;
            z += 0.3;
        }
        if (face == EnumFacing.WEST || face == EnumFacing.EAST) {
            z += 0.15;
        }
        if (face == EnumFacing.SOUTH || face == EnumFacing.NORTH) {
            x += 0.15;
        }
        return new Vec3(x, y, z);
    }

    public static BlockCache getBlockCache(boolean placeUp, int range) {

        BlockCache cache;
        List<Vec3> possibilities = new ArrayList<>();

        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    BlockPos blockPos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z);
                    Block block = mc.theWorld.getBlockState(blockPos).getBlock();

                    if (block.getDefaultState().getBlock().isReplaceable(mc.theWorld, blockPos)) {
                        possibilities.add(new Vec3(blockPos.getX(),blockPos.getY(),blockPos.getZ()));

                    }
                }
            }
        }

        if (possibilities.isEmpty()) {
            return null;
        }

        possibilities = possibilities.stream()
                .filter(vec3 -> mc.theWorld.getBlockState(new BlockPos(vec3.xCoord,vec3.yCoord,vec3.zCoord)).getBlock().isReplaceable(mc.theWorld, new BlockPos(vec3.xCoord,vec3.yCoord,vec3.zCoord)) && mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) <= range)
                .sorted(Comparator.comparingDouble(vec3 -> {
                    BlockPos blockPos = new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord);
                    Vec3 centerVec = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                    double d0 = mc.thePlayer.posX - centerVec.xCoord;
                    double d1 = mc.thePlayer.posY - centerVec.yCoord;
                    double d2 = mc.thePlayer.posZ - centerVec.zCoord;
                    return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
                }))
                .collect(Collectors.toList());

        //possibilities.sort();

        BlockCache closestCache = null;

        for (Vec3 possibility : possibilities) {
            BlockPos closestPos = new BlockPos(possibility.xCoord, possibility.yCoord, possibility.zCoord);
            cache = findValidBlockData(closestPos,true, true);

            if (cache != null) {
                if (closestCache == null || closestCache.getPosition().getY() + 1 > mc.thePlayer.posY) {
                    closestCache = cache;
                }

                if (!closestCache.equals(cache)) {
                    if (!cache.getPosition().equals(closestCache.getPosition())) {
                        return null;
                    }
                }

                if ((cache.getFacing() == EnumFacing.UP && !placeUp && !mc.thePlayer.onGround) || (closestCache.getPosition().getY() + 1 > mc.thePlayer.posY))
                    continue;

                Rotation floats = RotationUtils.rotationToFace(cache.getPosition(),cache.getFacing(),null);
                floats.setYaw(MathHelper.wrapAngleTo180_float(floats.getYaw()) + Math.round((mc.thePlayer.rotationYaw / 360)) * 360);
                MovingObjectPosition rayTraceResult = mc.thePlayer.rayTraceCustom(4.5, floats.getYaw(), floats.getPitch());
                if ((rayTraceResult.sideHit != cache.getFacing() || !rayTraceResult.getBlockPos().equals(cache.getPosition()))) {
                    continue;
                }

                cache.setRotation(floats);
                cache.setHitVec(rayTraceResult.hitVec);

                return cache;
            } /*else if (closestCache == null) {
                return null;
            }*/
        }

        return null;
    }




    public static BlockCache findValidBlockData(BlockPos searchPos, boolean excludedown, boolean sortMethod) {

        for (BlockPos targetPos : sortBlockPositionsWithMode(findAccessibleBlockPositions(searchPos), new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), sortMethod
        )) {
            for (EnumFacing EnumFacing : EnumFacing.values()) {
                if ((EnumFacing != EnumFacing.DOWN || !excludedown) && !isNotAir(targetPos) && isNotAir(targetPos.offset(EnumFacing, -1))) {
                    return new BlockCache(targetPos.offset(EnumFacing, -1), EnumFacing);
                }
            }
        }

        return null;

    }

    public static boolean isNotAir(BlockPos blockPos) {
        if (blockPos != null) {
            Block block = mc.theWorld.getBlockState(blockPos).getBlock();
            return (block.getMaterial().isSolid() || !block.getMaterial().isReplaceable()) && !(block instanceof BlockSnow);
        } else {
            return false;
        }
    }

    public static List<BlockPos> findAccessibleBlockPositions(BlockPos pos) {
        List<BlockPos> accessiblePositions = new ArrayList<>();
        double playerY = mc.thePlayer.posY;

        int range = Math.max((int) (mc.playerController.getBlockReachDistance() - 3.0F), 0);

        int baseX = pos.getX();
        int baseY = pos.getY();
        int baseZ = pos.getZ();

        for (int yOffset = -range; yOffset <= 0; yOffset++) {
            int currentY = baseY + yOffset;
            if (playerY >= currentY) {
                addValidXZPositions(accessiblePositions, baseX, currentY, baseZ, range);
            }
        }

        return accessiblePositions;
    }

    private static void addValidXZPositions(
            List<BlockPos> result,
            int baseX,
            int currentY,
            int baseZ,
            int range
    ) {
        for (int xOffset = -range; xOffset <= range; xOffset++) {
            for (int zOffset = -range; zOffset <= range; zOffset++) {
                BlockPos pos = new BlockPos(baseX + xOffset, currentY, baseZ + zOffset);
                result.add(pos);
            }
        }
    }

    public static List<BlockPos> sortBlockPositionsWithMode(List<BlockPos> posList, Vec3 playerPos, boolean sortMethod) {
        final double playerX = playerPos.getX();
        final double playerY = playerPos.getY();
        final double playerZ = playerPos.getZ();

        List<BlockPosSortData> sortDataList = new ArrayList<>(posList.size());

        for (BlockPos pos : posList) {
            final double centerX = pos.getX() + 0.5;
            final double centerY = pos.getY() + 0.5;
            final double centerZ = pos.getZ() + 0.5;

            if (sortMethod) {
                float dx = (float) (playerX - centerX);
                float dz = (float) (playerZ - centerZ);
                float horizontalSq = dx * dx + dz * dz;
                float verticalDiff = (float) (playerY - centerY);
                sortDataList.add(new BlockPosSortData(pos, horizontalSq, verticalDiff));
            } else {
                float dx = (float) (playerX - centerX);
                float dy = (float) (playerY - centerY);
                float dz = (float) (playerZ - centerZ);
                float distanceSq = dx * dx + dy * dy + dz * dz;
                sortDataList.add(new BlockPosSortData(pos, distanceSq, 0));
            }
        }

        sortDataList.sort((a, b) -> {
            int mainCompare = Float.compare(a.primaryMetric, b.primaryMetric);
            if (mainCompare != 0) return mainCompare;

            if (sortMethod) {
                return Float.compare(Math.abs(a.secondaryMetric), Math.abs(b.secondaryMetric));
            }
            return 0;
        });

        ListIterator<BlockPos> iter = posList.listIterator();
        for (BlockPosSortData data : sortDataList) {
            iter.next();
            iter.set(data.pos);
        }

        return posList;
    }

    private static class BlockPosSortData {
        final BlockPos pos;
        final float primaryMetric;
        final float secondaryMetric;

        BlockPosSortData(BlockPos pos, float primary, float secondary) {
            this.pos = pos;
            this.primaryMetric = primary;
            this.secondaryMetric = secondary;
        }
    }

    //Rvn Moment

    public static double getMovementAngle() {
        double angle = Math.toDegrees(Math.atan2(-mc.thePlayer.motionX, mc.thePlayer.motionZ));
        return (angle == 0.0D) ? 0.0D : angle;
    }

    public static float getMotionYaw() {
        return (float)Math.toDegrees(Math.atan2(mc.thePlayer.posZ - mc.thePlayer.prevPosZ, mc.thePlayer.posX - mc.thePlayer.prevPosX)) - 90.0F;
    }

    public static float hardcodedYaw() {
        float simpleYaw = 0.0F;
        boolean w = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean s = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean a = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        boolean d = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        boolean dupe = a & d; // Both A and D pressed

        if (w) { // Moving forward (W key)
            simpleYaw -= 180.0F;
            if (!dupe) {
                if (a) simpleYaw += 45.0F; // W + A = forward-left (45° adjustment)
                if (d) simpleYaw -= 45.0F; // W + D = forward-right (-45° adjustment)
            }
        } else if (!s) { // Not moving backward
            simpleYaw -= 180.0F;
            if (!dupe) {
                if (a) simpleYaw += 90.0F; // A only = strafe left (90° adjustment)
                if (d) simpleYaw -= 90.0F; // D only = strafe right (-90° adjustment)
            }
        } else if (!w && !dupe) { // Moving backward (S key) without A+D conflict
            if (a) simpleYaw -= 45.0F; // S + A = backward-left (-45° adjustment)
            if (d) simpleYaw += 45.0F; // S + D = backward-right (45° adjustment)
        }

        return simpleYaw;
    }

}
