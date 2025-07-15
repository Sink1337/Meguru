package dev.meguru.module.impl.player;

import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.utils.player.InventoryUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;

public class AutoTool extends Module {

    private final BooleanSetting autoSword = new BooleanSetting("Auto Sword", true);
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);
    private final BooleanSetting disableOnRightClick = new BooleanSetting("Disable On Right Click", true);

    private int lastSlot = -1;

    public AutoTool() {
        super("AutoTool", Category.PLAYER, "switches to the best tool");
        this.addSettings(autoSword, switchBack, disableOnRightClick);
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (e.isPre()) {
            if (disableOnRightClick.isEnabled() && mc.gameSettings.keyBindUseItem.isKeyDown()) {
                return;
            }

            if (mc.objectMouseOver != null && mc.gameSettings.keyBindAttack.isKeyDown()) {
                MovingObjectPosition objectMouseOver = mc.objectMouseOver;
                if (objectMouseOver.entityHit != null) {
                    if (lastSlot == -1) lastSlot = mc.thePlayer.inventory.currentItem;
                    switchSword();
                } else if (objectMouseOver.getBlockPos() != null) {
                    Block block = mc.theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock();
                    if (lastSlot == -1) lastSlot = mc.thePlayer.inventory.currentItem;
                    updateItem(block);
                }
            } else if (TargetManager.target != null) {
                if (lastSlot == -1) lastSlot = mc.thePlayer.inventory.currentItem;
                switchSword();
            } else if (switchBack.isEnabled() && lastSlot != -1) {
                mc.thePlayer.inventory.currentItem = lastSlot;
                lastSlot = -1;
            }
        }
    }

    private void updateItem(Block block) {
        float strength = 1.0F;
        int bestItem = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) {
                continue;
            }
            float strVsBlock = itemStack.getStrVsBlock(block);
            if (strVsBlock > strength) {
                strength = strVsBlock;
                bestItem = i;
            }
        }
        if (bestItem != -1 && mc.thePlayer.inventory.currentItem != bestItem) {
            mc.thePlayer.inventory.currentItem = bestItem;
        }
    }

    private void switchSword() {
        if (!autoSword.isEnabled()) return;
        float damage = 1;
        int bestItem = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack is = mc.thePlayer.inventory.mainInventory[i];
            if (is != null && is.getItem() instanceof ItemSword && InventoryUtils.getSwordStrength(is) > damage) {
                damage = InventoryUtils.getSwordStrength(is);
                bestItem = i;
            }
        }
        if (bestItem != -1 && mc.thePlayer.inventory.currentItem != bestItem) {
            mc.thePlayer.inventory.currentItem = bestItem;
        }
    }
}