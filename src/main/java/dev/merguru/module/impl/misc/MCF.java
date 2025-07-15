package dev.merguru.module.impl.misc;

import dev.merguru.commands.impl.FriendCommand;
import dev.merguru.event.impl.game.TickEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.ui.notifications.NotificationManager;
import dev.merguru.ui.notifications.NotificationType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StringUtils;

public class MCF extends Module {

    private boolean wasDown;

    public MCF() {
        super("MCF", Category.MISC, "middle click friends");
    }

    @Override
    public void onTickEvent(TickEvent event) {
        if (mc.inGameHasFocus) {
            boolean down = mc.gameSettings.keyBindPickBlock.isKeyDown();
            if (down && !wasDown) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) mc.objectMouseOver.entityHit;
                    String name = StringUtils.stripControlCodes(player.getName());
                    if (FriendCommand.isFriend(name)) {
                        FriendCommand.friends.removeIf(f -> f.equalsIgnoreCase(name));
                        NotificationManager.post(NotificationType.SUCCESS, "Friend Manager", "You are no longer friends with " + name + "!", 2);
                    } else {
                        FriendCommand.friends.add(name);
                        NotificationManager.post(NotificationType.SUCCESS, "Friend Manager", "You are now friends with " + name + "!", 2);
                    }
                    FriendCommand.save();
                    wasDown = true;
                }
            } else if (!down) {
                wasDown = false;
            }
        }
    }

}
