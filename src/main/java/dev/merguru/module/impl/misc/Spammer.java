package dev.merguru.module.impl.misc;

import dev.merguru.event.impl.player.MotionEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.BooleanSetting;
import dev.merguru.module.settings.impl.MultipleBoolSetting;
import dev.merguru.module.settings.impl.NumberSetting;
import dev.merguru.module.settings.impl.StringSetting;
import dev.merguru.utils.misc.MathUtils;
import dev.merguru.utils.time.TimerUtil;

public final class Spammer extends Module {

    private final StringSetting text = new StringSetting("Text");
    private final NumberSetting delay = new NumberSetting("Delay", 100, 1000, 100, 1);
    private final MultipleBoolSetting settings = new MultipleBoolSetting("Settings",
            new BooleanSetting("AntiSpam", false),
            new BooleanSetting("Bypass", false));
    private final TimerUtil delayTimer = new TimerUtil();

    @Override
    public void onMotionEvent(MotionEvent event) {
        String spammerText = text.getString();

        if (spammerText != null && delayTimer.hasTimeElapsed(settings.getSetting("Bypass").isEnabled() ? 2000 : delay.getValue().longValue())) {

            if (settings.getSetting("AntiSpam").isEnabled()) {
                spammerText += " " + MathUtils.getRandomInRange(10, 100000);
            }

            mc.thePlayer.sendChatMessage(spammerText);
            delayTimer.reset();
        }
    }

    public Spammer() {
        super("Spammer", Category.MISC, "Spams in chat");
        this.addSettings(text, delay, settings);
    }

}
