package dev.tenacity.module.impl.player;

import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.player.InventoryUtils;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class AutoArmor extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 150, 300, 0, 10);
    private final BooleanSetting onlyWhileNotMoving = new BooleanSetting("Stop when moving", false);
    private final BooleanSetting invOnly = new BooleanSetting("Inventory only", false);
    private final TimerUtil timer = new TimerUtil();

    public AutoArmor() {
        super("AutoArmor", Category.PLAYER, "Automatically equips armor");
        this.addSettings(delay, onlyWhileNotMoving, invOnly);
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (e.isPost()) return;

        if ((invOnly.isEnabled() && !(mc.currentScreen instanceof GuiInventory)) || (onlyWhileNotMoving.isEnabled() && MovementUtils.isMoving())) {
            return;
        }

        if (mc.thePlayer.openContainer instanceof ContainerChest) {
            timer.reset();
            return;
        }

        if (delay.getValue().longValue() == 0 || timer.hasTimeElapsed(delay.getValue().longValue())) {
            boolean didEquipActionThisTick = false;

            for (int armorSlot = 5; armorSlot < 9; armorSlot++) {
                if (equipBest(armorSlot)) {
                    didEquipActionThisTick = true;
                    if (delay.getValue().longValue() != 0) {
                        timer.reset();
                        return;
                    }
                }
            }

            if (didEquipActionThisTick && delay.getValue().longValue() == 0) {
                timer.reset();
            }
        }
    }

    private boolean equipBest(int armorSlot) {
        int equipSlot = -1;
        int currProt = -1;
        ItemArmor currItem = null;
        ItemStack slotStack = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();

        if (slotStack != null && slotStack.getItem() instanceof ItemArmor) {
            currItem = (ItemArmor) slotStack.getItem();
            currProt = currItem.damageReduceAmount
                    + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, slotStack);
        }

        for (int i = 9; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() instanceof ItemArmor) {
                int prot = ((ItemArmor) is.getItem()).damageReduceAmount + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, is);

                if (isValidPiece(armorSlot, (ItemArmor) is.getItem()) && (currItem == null || prot > currProt)) {
                    currItem = (ItemArmor) is.getItem();
                    equipSlot = i;
                    currProt = prot;
                }
            }
        }

        if (equipSlot != -1) {
            if (slotStack != null) {
                InventoryUtils.drop(armorSlot);
            }
            InventoryUtils.click(equipSlot, 0, true);
            return true;
        }
        return false;
    }

    private boolean isValidPiece(int armorSlot, ItemArmor item) {
        String unlocalizedName = item.getUnlocalizedName();
        return (armorSlot == 5 && unlocalizedName.startsWith("item.helmet"))
                || (armorSlot == 6 && unlocalizedName.startsWith("item.chestplate"))
                || (armorSlot == 7 && unlocalizedName.startsWith("item.leggings"))
                || (armorSlot == 8 && unlocalizedName.startsWith("item.boots"));
    }
}