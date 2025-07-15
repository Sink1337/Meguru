package dev.merguru.module.impl.combat;

import dev.merguru.event.impl.player.MotionEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.NumberSetting;
import dev.merguru.utils.player.InventoryUtils;
import dev.merguru.utils.server.PacketUtils;
import dev.merguru.utils.time.TimerUtil;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.Potion;

public class AutoHead extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 750, 3000, 0, 50);
    private final NumberSetting healPercent = new NumberSetting("Health %", 50, 99, 1, 1);
    private final TimerUtil timer = new TimerUtil();

    public AutoHead() {
        super("AutoHead", Category.COMBAT, "auto consume heads");
        this.addSettings(delay, healPercent);
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (mc.thePlayer != null && mc.theWorld != null && e.isPre()
                && !(mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.isPotionActive(Potion.regeneration))
                && (mc.thePlayer.getHealth() / mc.thePlayer.getMaxHealth()) * 100 <= healPercent.getValue()
                && timer.hasTimeElapsed(delay.getValue().longValue())) {
            for (int i = 0; i < 45; i++) {
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (is != null && is.getItem() instanceof ItemSkull && is.getDisplayName().contains("Head")) {
                    int prevSlot = mc.thePlayer.inventory.currentItem;
                    if (i - 36 < 0) {
                        InventoryUtils.swap(i, 8);
                        PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(8));
                    } else {
                        PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(i - 36));
                    }

                    PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(is));
                    mc.thePlayer.inventory.currentItem = prevSlot;
                    PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(prevSlot));

                    timer.reset();
                }
            }
        }
    }

}
