package dev.tenacity.module.impl.player;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.game.WorldEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.event.impl.render.RenderChestEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.font.AbstractFontRenderer;
import dev.tenacity.utils.player.RotationUtils;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import dev.tenacity.module.impl.movement.Scaffold;
import dev.tenacity.module.impl.combat.KillAura;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.player.inventory.ContainerLocalMenu;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

public class ChestStealer extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 80, 300, 0, 10);
    private final BooleanSetting aura = new BooleanSetting("Aura", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final NumberSetting wallDistance = new NumberSetting("Through Block Distance", 3, 6, 1, 0.1);
    private final BooleanSetting swing = new BooleanSetting("Swing", false);
    private final NumberSetting auraRange = new NumberSetting("Aura Range", 3, 6, 1, 0.1);
    private final NumberSetting auradelay = new NumberSetting("Aura Delay (ms)", 500, 1000, 50, 50);
    public static final BooleanSetting stealingIndicator = new BooleanSetting("Stealing Indicator", false);
    public static final BooleanSetting freeLook = new BooleanSetting("Free Look", true);
    private final BooleanSetting reverse = new BooleanSetting("Reverse", false);
    public static final BooleanSetting silent = new BooleanSetting("Silent", false);
    public static final BooleanSetting titleCheck = new BooleanSetting("Validate Chest Type", true);
    private final BooleanSetting smart = new BooleanSetting("Smart", false);
    private final BooleanSetting rotation = new BooleanSetting("Rotation", true);

    private final Set<Item> grabbedItems = new HashSet<>();
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil openTimer = new TimerUtil();
    private final TimerUtil auraTimer = new TimerUtil();
    public static boolean stealing;
    private InvManager invManager;
    private AbstractFontRenderer tenacityFont20;
    private boolean isClearState;

    public static final Set<BlockPos> openedChests = new HashSet<>();
    public static final Set<BlockPos> renderableChests = new HashSet<>();

    private boolean looting;
    private BlockPos lastChest;
    private boolean canUndo;
    private boolean isOpeningChest = false;

    private KillAura killauraModule;
    private Scaffold scaffoldModule;

    public ChestStealer() {
        super("ChestStealer", Category.PLAYER, "auto loot chests");
        auraRange.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        throughWalls.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        wallDistance.addParent(throughWalls, ParentAttribute.BOOLEAN_CONDITION);
        swing.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        auradelay.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        stealingIndicator.addParent(silent, ParentAttribute.BOOLEAN_CONDITION);
        this.addSettings(delay, aura, throughWalls, wallDistance, swing, auraRange, auradelay, titleCheck, freeLook, reverse, silent, stealingIndicator, smart, rotation);
    }

    @Override
    public void onRenderChestEvent(RenderChestEvent e) {
        if (e.getEntity() instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) e.getEntity();
            BlockPos chestPos = chest.getPos();
            if (this.isEnabled()) {
                renderableChests.add(chestPos);
            }
        }
    }


    @Override
    public void onMotionEvent(MotionEvent e) {
        if (!e.isPre()) {
            return;
        }

        setSuffix(smart.isEnabled() ? "Smart" : null);

        if (invManager == null) {
            invManager = Tenacity.INSTANCE.getModuleCollection().getModule(InvManager.class);
        }

        if (killauraModule == null) {
            killauraModule = Tenacity.INSTANCE.getModuleCollection().getModule(KillAura.class);
        }
        if (scaffoldModule == null) {
            scaffoldModule = Tenacity.INSTANCE.getModuleCollection().getModule(Scaffold.class);
        }

        handleAuraStealing(e);
        handleContainerChestStealing();
    }

    private void handleAuraStealing(MotionEvent e) {
        if (mc.currentScreen != null) {
            return;
        }
        if (killauraModule != null && killauraModule.isEnabled() && KillAura.attacking) {
            return;
        }
        if (scaffoldModule != null && scaffoldModule.isEnabled()) {
            return;
        }
        if (mc.thePlayer.openContainer instanceof ContainerChest) {
            return;
        }

        if (auraTimer.hasTimeElapsed(auradelay.getValue().longValue(), true) && aura.isEnabled() &&
                mc.thePlayer.motionY < 0.42 && mc.thePlayer.motionY > -0.42 &&
                getNearbyChestsInAuraRange().size() <= 10 &&
                !mc.thePlayer.isUsingItem() && !isOpeningChest) {

            float radius = auraRange.getValue().floatValue();

            List<BlockPos> potentialChests = new ArrayList<>();
            for (float x = -radius; x <= radius; x += 0.5F) {
                for (float y = -2.0F; y <= 3.0F; y += 0.5F) {
                    for (float z = -radius; z <= radius; z += 0.5F) {
                        BlockPos pos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z);
                        if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.chest && !openedChests.contains(pos)) {
                            potentialChests.add(pos);
                        }
                    }
                }
            }

            potentialChests.sort(Comparator.comparingDouble(pos -> mc.thePlayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));

            for (BlockPos pos : potentialChests) {
                final float[] rotations = RotationUtils.getFacingRotations2(pos.getX(), pos.getY(), pos.getZ());
                float originalYaw = e.getYaw();
                float originalPitch = e.getPitch();

                if (rotation.isEnabled()) {
                    e.setRotations(rotations[0], rotations[1]);
                    if (!freeLook.isEnabled() || silent.isEnabled()) {
                        RotationUtils.setVisualRotations(rotations[0], rotations[1]);
                    }
                }

                Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                boolean canInteract = false;
                double distToChestSq = mc.thePlayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                double reachSq = mc.playerController.getBlockReachDistance() * mc.playerController.getBlockReachDistance();
                double wallDistSq = throughWalls.isEnabled() ? wallDistance.getValue().doubleValue() * wallDistance.getValue().doubleValue() : -1.0;


                if (distToChestSq < reachSq) {
                    Vec3 playerEyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
                    MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(playerEyePos, hitVec, false, true, false);

                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.getBlockPos().equals(pos)) {
                        canInteract = true;
                    }
                }

                if (!canInteract && throughWalls.isEnabled() && distToChestSq <= wallDistSq) {
                    canInteract = true;
                }

                if (canInteract) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), pos, EnumFacing.UP, hitVec)) {
                        if (swing.isEnabled()) {
                            mc.thePlayer.swingItem();
                        } else {
                            PacketUtils.sendPacketNoEvent(new C0APacketAnimation());
                        }
                        openedChests.add(pos);
                        looting = true;
                        lastChest = pos;
                        canUndo = true;
                        auraTimer.reset();
                        isOpeningChest = true;
                        if (rotation.isEnabled() && freeLook.isEnabled() && !silent.isEnabled()) {
                            e.setRotations(originalYaw, originalPitch);
                            RotationUtils.setVisualRotations(originalYaw, originalPitch);
                        }
                        return;
                    }
                }
            }
        }
    }


    private List<BlockPos> getNearbyChestsInAuraRange() {
        List<BlockPos> chestsInAuraRange = new ArrayList<>();
        double auraRadiusSq = auraRange.getValue().intValue() * auraRange.getValue().intValue();

        for (BlockPos pos : new ArrayList<>(renderableChests)) {
            if (openedChests.contains(pos) || mc.thePlayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > auraRadiusSq) {
                renderableChests.remove(pos);
                continue;
            }
            chestsInAuraRange.add(pos);
        }
        return chestsInAuraRange;
    }


    private void handleContainerChestStealing() {
        if (!(mc.thePlayer.openContainer instanceof ContainerChest)) {
            if (isClearState) {
                grabbedItems.clear();
                isClearState = false;
                stealing = false;
                looting = false;
                lastChest = null;
                canUndo = false;
            }
            if (isOpeningChest) {
                isOpeningChest = false;
            }
            return;
        }

        if (isOpeningChest) {
            isOpeningChest = false;
        }

        ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
        IInventory chestInv = chest.getLowerChestInventory();

        if (titleCheck.isEnabled()) {
            if (chestInv instanceof ContainerLocalMenu && !((ContainerLocalMenu) chestInv).realChest) {
                stealing = false;
                isClearState = false;
                grabbedItems.clear();
                looting = false;
                lastChest = null;
                canUndo = false;
                mc.thePlayer.closeScreen();
                return;
            }
        }

        isClearState = true;

        List<Integer> slotsToSteal = new ArrayList<>();
        for (int i = 0; i < chestInv.getSizeInventory(); i++) {
            ItemStack is = chestInv.getStackInSlot(i);
            if (is != null) {
                boolean shouldGrab = true;
                if (smart.isEnabled()) {
                    if (invManager != null && invManager.isBadItem(is, -1, true)) {
                        shouldGrab = false;
                    }
                    if (!(is.getItem() instanceof ItemBlock) && grabbedItems.contains(is.getItem())) {
                        shouldGrab = false;
                    }
                }
                if (shouldGrab) {
                    slotsToSteal.add(i);
                }
            }
        }

        if (reverse.isEnabled()) {
            Collections.reverse(slotsToSteal);
        }

        stealing = !slotsToSteal.isEmpty() && !isInventoryFull();

        if (stealing) {
            if (delay.getValue() == 0 || timer.hasTimeElapsed(delay.getValue().longValue(), true)) {
                for (Integer slot : slotsToSteal) {
                    ItemStack is = chestInv.getStackInSlot(slot);
                    if (is != null) {
                        if (smart.isEnabled() && !(is.getItem() instanceof ItemBlock)) {
                            grabbedItems.add(is.getItem());
                        }
                        mc.playerController.windowClick(chest.windowId, slot, 0, 1, mc.thePlayer);
                        timer.reset();
                        return;
                    }
                }
            }
        } else {
            grabbedItems.clear();
            isClearState = false;
            looting = false;
            lastChest = null;
            canUndo = false;
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        if (stealingIndicator.isEnabled() && stealing) {
            ScaledResolution sr = new ScaledResolution(mc);
            AbstractFontRenderer fr = HUDMod.customFont.isEnabled() ? (tenacityFont20 != null ? tenacityFont20 : mc.fontRendererObj) : mc.fontRendererObj;
            String text = "§lStealing...";
            float x = sr.getScaledWidth() / 2.0F - fr.getStringWidth(text) / 2.0F;
            float y = sr.getScaledHeight() / 2.0F + 10;
            fr.drawStringWithShadow(text, x, y, HUDMod.getClientColors().getFirst().getRGB());
        }
    }

    @Override
    public void onEnable() {
        grabbedItems.clear();
        isClearState = false;
        stealing = false;
        looting = false;
        lastChest = null;
        canUndo = false;
        openTimer.reset();
        timer.reset();
        auraTimer.reset();
        isOpeningChest = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        openedChests.clear();
        renderableChests.clear();
        grabbedItems.clear();
        isClearState = false;
        stealing = false;
        looting = false;
        lastChest = null;
        canUndo = false;
        isOpeningChest = false;
        super.onDisable();
    }

    private boolean isInventoryFull() {
        for (int i = 9; i < 45; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean canSteal() {
        if (Tenacity.INSTANCE.isEnabled(ChestStealer.class) && mc.currentScreen instanceof GuiChest) {
            ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            IInventory chestInv = chest.getLowerChestInventory();
            return !titleCheck.isEnabled() || !(chestInv instanceof ContainerLocalMenu) || ((ContainerLocalMenu) chestInv).realChest;
        }
        return false;
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        if (event instanceof WorldEvent.Load) {
            openedChests.clear();
            renderableChests.clear();
            grabbedItems.clear();
            isClearState = false;
            stealing = false;
            looting = false;
            lastChest = null;
            canUndo = false;
            openTimer.reset();
            timer.reset();
            auraTimer.reset();
            isOpeningChest = false;
        }
    }
}