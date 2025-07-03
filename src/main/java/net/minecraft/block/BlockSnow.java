package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockSnow extends Block {
    public static final PropertyInteger LAYERS = PropertyInteger.create("layers", 1, 8);

    protected BlockSnow() {
        super(Material.snow);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LAYERS, Integer.valueOf(1)));
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setBlockBoundsForItemRender();
    }

    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);

        if (state.getBlock() != this) {
            return false;
        }

        return ((Integer) state.getValue(LAYERS)).intValue() < 5;
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        if (state.getBlock() != this) {
            return super.getCollisionBoundingBox(worldIn, pos, state);
        }

        int i = ((Integer) state.getValue(LAYERS)).intValue() - 1;
        float f = 0.125F;
        return new AxisAlignedBB((double) pos.getX() + this.minX, (double) pos.getY() + this.minY, (double) pos.getZ() + this.minZ, (double) pos.getX() + this.maxX, (double) ((float) pos.getY() + (float) i * f), (double) pos.getZ() + this.maxZ);
    }

    /**
     * Used to determine ambient occlusion and culling when rebuilding chunks for render
     */
    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    /**
     * Sets the block's bounds for rendering it as an item
     */
    public void setBlockBoundsForItemRender() {
        this.getBoundsForLayers(0);
    }

    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if (iblockstate.getBlock() == this) {
            this.getBoundsForLayers(((Integer) iblockstate.getValue(LAYERS)).intValue());
        } else {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
        }
    }

    protected void getBoundsForLayers(int p_150154_1_) {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, (float) p_150154_1_ / 8.0F, 1.0F);
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos.down());
        Block block = iblockstate.getBlock();
        boolean isSnowBlockBelowAndFull = block == this && iblockstate.getProperties().containsKey(LAYERS) && ((Integer) iblockstate.getValue(LAYERS)).intValue() >= 7;

        return block != Blocks.ice && block != Blocks.packed_ice ?
                (block.getMaterial() == Material.leaves ? true : (isSnowBlockBelowAndFull ? true : block.isOpaqueCube() && block.blockMaterial.blocksMovement())) :
                false;
    }

    /**
     * Called when a neighboring block changes.
     */
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        this.checkAndDropBlock(worldIn, pos, state);
    }

    private boolean checkAndDropBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!this.canPlaceBlockAt(worldIn, pos)) {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
            return false;
        } else {
            return true;
        }
    }

    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
        if (state.getProperties().containsKey(LAYERS)) {
            spawnAsEntity(worldIn, pos, new ItemStack(Items.snowball, ((Integer) state.getValue(LAYERS)).intValue() + 1, 0));
        } else {
            spawnAsEntity(worldIn, pos, new ItemStack(Items.snowball, 1, 0));
        }
        worldIn.setBlockToAir(pos);
        player.triggerAchievement(StatList.mineBlockStatArray[Block.getIdFromBlock(this)]);
    }

    /**
     * Get the Item that this Block should drop when harvested.
     */
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.snowball;
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random random) {
        return 0;
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.getLightFor(EnumSkyBlock.BLOCK, pos) > 11) {
            if (worldIn.getBlockState(pos).getBlock() == this && worldIn.getBlockState(pos).getProperties().containsKey(LAYERS)) {
                this.dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
            } else {
                worldIn.setBlockToAir(pos);
                return;
            }
            worldIn.setBlockToAir(pos);
        }
    }

    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockState stateAtPos = worldIn.getBlockState(pos);
        if (stateAtPos.getBlock() == this) {
            return side == EnumFacing.UP ? true : super.shouldSideBeRendered(worldIn, pos, side);
        }
        return super.shouldSideBeRendered(worldIn, pos, side);
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(LAYERS, Integer.valueOf((meta & 7) + 1));
    }

    /**
     * Whether this Block can be replaced directly by other blocks (true for e.g. tall grass)
     */
    public boolean isReplaceable(World worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() == this && state.getProperties().containsKey(LAYERS)) {
            return ((Integer) state.getValue(LAYERS)).intValue() == 1;
        }
        return false;
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state) {
        if (state.getProperties().containsKey(LAYERS)) {
            return ((Integer) state.getValue(LAYERS)).intValue() - 1;
        }
        return 0;
    }

    protected BlockState createBlockState() {
        return new BlockState(this, new IProperty[]{LAYERS});
    }
}