package dev.merguru.module.impl.player;

import dev.merguru.Merguru;
import dev.merguru.event.impl.game.WorldEvent;
import dev.merguru.event.impl.network.PacketReceiveEvent;
import dev.merguru.event.impl.player.MotionEvent;
import dev.merguru.event.impl.render.Render2DEvent;
import dev.merguru.event.impl.render.RenderChestEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.impl.combat.KillAura;
import dev.merguru.module.impl.render.HUDMod;
import dev.merguru.module.settings.ParentAttribute;
import dev.merguru.module.settings.impl.BooleanSetting;
import dev.merguru.module.settings.impl.NumberSetting;
import dev.merguru.utils.font.AbstractFontRenderer;
import dev.merguru.utils.font.FontUtil;
import dev.merguru.utils.player.RotationUtils;
import dev.merguru.utils.server.PacketUtils;
import dev.merguru.utils.time.TimerUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.player.inventory.ContainerLocalMenu;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.*;

public class ChestStealer extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 80, 300, 0, 10);
    private final BooleanSetting aura = new BooleanSetting("Aura", false);
    private final BooleanSetting noAuraWhenHoldingBlock = new BooleanSetting("Held Block Halt", true);
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
    private final BooleanSetting packet = new BooleanSetting("Packet", false);

    private final Set<Item> grabbedItems = new HashSet<>();
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil openTimer = new TimerUtil();
    private final TimerUtil auraTimer = new TimerUtil();
    public static boolean stealing;
    private InvManager invManager;
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
        this.addSettings(delay, aura, noAuraWhenHoldingBlock, throughWalls, wallDistance, swing, auraRange, auradelay, titleCheck, freeLook, reverse, silent, stealingIndicator, smart, rotation, packet);

        noAuraWhenHoldingBlock.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        auraRange.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        throughWalls.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        wallDistance.addParent(throughWalls, ParentAttribute.BOOLEAN_CONDITION);
        swing.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        auradelay.addParent(aura, ParentAttribute.BOOLEAN_CONDITION);
        stealingIndicator.addParent(silent, ParentAttribute.BOOLEAN_CONDITION);
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
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            return;
        }

        if (!e.isPre()) {
            return;
        }

        if (invManager == null) {
            invManager = Merguru.INSTANCE.getModuleCollection().getModule(InvManager.class);
        }

        if (killauraModule == null) {
            killauraModule = Merguru.INSTANCE.getModuleCollection().getModule(KillAura.class);
        }
        if (scaffoldModule == null) {
            scaffoldModule = Merguru.INSTANCE.getModuleCollection().getModule(Scaffold.class);
        }

        handleAuraStealing(e);
        handleContainerChestStealing();
    }

    private void handleAuraStealing(MotionEvent e) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }
        if (killauraModule != null && killauraModule.isEnabled() && KillAura.attacking) {
            return;
        }
        if (scaffoldModule != null && scaffoldModule.isEnabled()) {
            return;
        }

        if (noAuraWhenHoldingBlock.isEnabled() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            auraTimer.reset();
            return;
        }

        if (mc.thePlayer.openContainer instanceof ContainerChest || isOpeningChest) {
            auraTimer.reset();
            return;
        }

        if (aura.isEnabled() && auraTimer.hasTimeElapsed(auradelay.getValue().longValue(), true) &&
                getNearbyChestsInAuraRange().size() <= 10 &&
                !mc.thePlayer.isUsingItem()) {

            float radius = auraRange.getValue().floatValue();

            List<BlockPos> potentialChests = new ArrayList<>();
            for (float x = -radius; x <= radius; x += 0.5F) {
                for (float y = -2.0F; y <= 3.0F; y += 0.5F) {
                    for (float z = -radius; z <= radius; z += 0.5F) {
                        BlockPos pos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z);
                        if (mc.theWorld.getBlockState(pos) != null && mc.theWorld.getBlockState(pos).getBlock() == Blocks.chest && !openedChests.contains(pos)) {
                            potentialChests.add(pos);
                        }
                    }
                }
            }

            potentialChests.sort(Comparator.comparingDouble(pos -> mc.thePlayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));

            for (BlockPos pos : potentialChests) {
                final float[] rotations = RotationUtils.getFacingRotations2(pos.getX(), pos.getY() + 0.5, (int) (pos.getZ() + 0.5));
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

                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.getBlockPos() != null && mop.getBlockPos().equals(pos)) {
                        canInteract = true;
                    }
                }

                if (!canInteract && throughWalls.isEnabled() && distToChestSq <= wallDistSq) {
                    canInteract = true;
                }

                if (canInteract) {
                    isOpeningChest = true;
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
                        if (rotation.isEnabled() && freeLook.isEnabled() && !silent.isEnabled()) {
                            e.setRotations(originalYaw, originalPitch);
                            RotationUtils.setVisualRotations(originalYaw, originalPitch);
                        }
                        return;
                    } else {
                        isOpeningChest = false;
                        auraTimer.reset();
                    }
                }
            }
        }
    }


    private List<BlockPos> getNearbyChestsInAuraRange() {
        if (mc == null || mc.thePlayer == null) {
            return new ArrayList<>();
        }

        List<BlockPos> chestsInAuraRange = new ArrayList<>();
        double auraRadiusSq = auraRange.getValue().intValue() * auraRange.getValue().intValue();

        for (BlockPos pos : new ArrayList<>(renderableChests)) {
            if (pos == null || openedChests.contains(pos) || mc.thePlayer.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > auraRadiusSq) {
                renderableChests.remove(pos);
                continue;
            }
            chestsInAuraRange.add(pos);
        }
        return chestsInAuraRange;
    }


    private void handleContainerChestStealing() {
        if (mc == null || mc.thePlayer == null) {
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
        if (chestInv == null) {
            stealing = false;
            isClearState = false;
            grabbedItems.clear();
            looting = false;
            lastChest = null;
            canUndo = false;
            mc.thePlayer.closeScreen();
            return;
        }


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
            if (delay.getValue() != 0) {
                if (timer.hasTimeElapsed(delay.getValue().longValue(), true)) {
                    for (Integer slot : slotsToSteal) {
                        ItemStack is = chestInv.getStackInSlot(slot);
                        if (is != null) {
                            if (smart.isEnabled() && !(is.getItem() instanceof ItemBlock)) {
                                grabbedItems.add(is.getItem());
                            }

                            if (packet.isEnabled()) {
                                if (mc.thePlayer.inventory != null) {
                                    PacketUtils.sendPacketNoEvent(new C0EPacketClickWindow(chest.windowId, slot, 0, 1, is, chest.getNextTransactionID(mc.thePlayer.inventory)));
                                }
                            } else {
                                mc.playerController.windowClick(chest.windowId, slot, 0, 1, mc.thePlayer);
                            }

                            timer.reset();
                            return;
                        }
                    }
                    mc.thePlayer.closeScreen();
                }
            } else {
                for (Integer slot : slotsToSteal) {
                    ItemStack is = chestInv.getStackInSlot(slot);
                    if (is != null) {
                        if (smart.isEnabled() && !(is.getItem() instanceof ItemBlock)) {
                            grabbedItems.add(is.getItem());
                        }

                        if (packet.isEnabled()) {
                            if (mc.thePlayer.inventory != null) {
                                PacketUtils.sendPacketNoEvent(new C0EPacketClickWindow(chest.windowId, slot, 0, 1, is, chest.getNextTransactionID(mc.thePlayer.inventory)));
                            }
                        } else {
                            mc.playerController.windowClick(chest.windowId, slot, 0, 1, mc.thePlayer);
                        }
                    }
                }
                mc.thePlayer.closeScreen();
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
        if (stealingIndicator.isEnabled() && stealing && mc != null && mc.fontRendererObj != null) {
            ScaledResolution sr = new ScaledResolution(mc);
            AbstractFontRenderer fr;
            if (HUDMod.customFont.isEnabled()) {
                if (HUDMod.getCustomFontMode().is("Inter")) {
                    fr = FontUtil.idkFont18;
                } else {
                    fr = FontUtil.tenacityFont20;
                }
            } else {
                fr = mc.fontRendererObj;
            }

            String text = "§lStealing...";
            float x = sr.getScaledWidth() / 2.0F - fr.getStringWidth(text) / 2.0F;
            float y = sr.getScaledHeight() / 2.0F + 10;
            if (HUDMod.getClientColors() != null) {
                fr.drawStringWithShadow(text, x, y, HUDMod.getClientColors().getFirst().getRGB());
            } else {
                fr.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void onEnable() {
        resetModuleState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetModuleState();
        super.onDisable();
    }

    private boolean isInventoryFull() {
        if (mc == null || mc.thePlayer == null || mc.thePlayer.inventoryContainer == null) {
            return true;
        }
        for (int i = 9; i < 45; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i) == null || mc.thePlayer.inventoryContainer.getSlot(i).getStack() == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean canSteal() {
        if (mc == null || mc.currentScreen == null) {
            return false;
        }

        if (Merguru.INSTANCE.isEnabled(ChestStealer.class) && mc.currentScreen instanceof GuiChest) {
            if (mc.thePlayer == null || !(mc.thePlayer.openContainer instanceof ContainerChest)) {
                return false;
            }
            ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            IInventory chestInv = chest.getLowerChestInventory();
            if (chestInv == null) {
                return false;
            }
            return !titleCheck.isEnabled() || !(chestInv instanceof ContainerLocalMenu) || ((ContainerLocalMenu) chestInv).realChest;
        }
        return false;
    }

    private void resetModuleState() {
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

    @Override
    public void onWorldEvent(WorldEvent event) {
        if (event instanceof WorldEvent.Load) {
            resetModuleState();
        }
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat chatPacket = (S02PacketChat) event.getPacket();
            if (chatPacket.getChatComponent() != null) {
                String message = chatPacket.getChatComponent().getUnformattedText();

                if (message != null && message.contains("Starting game.")) {
                    resetModuleState();
                    openedChests.clear();
                    renderableChests.clear();
                }
            }
        }
    }
}