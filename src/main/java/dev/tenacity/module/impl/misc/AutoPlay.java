package dev.tenacity.module.impl.misc;

import dev.tenacity.event.impl.player.ChatReceivedEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.module.settings.impl.StringSetting;
import dev.tenacity.ui.notifications.NotificationManager;
import dev.tenacity.ui.notifications.NotificationType;
import dev.tenacity.utils.misc.Multithreading;
import dev.tenacity.utils.misc.SoundUtils;
import dev.tenacity.utils.player.ChatUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import java.util.concurrent.TimeUnit;

public class AutoPlay extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Hypixel", "Hypixel", "Bloxd");
    private final BooleanSetting autoPlay = new BooleanSetting("AutoPlay", true);
    private final BooleanSetting autoGG = new BooleanSetting("AutoGG", true);
    private final BooleanSetting autoHubOnBan = new BooleanSetting("Auto /l on ban", false);
    private final NumberSetting autoPlayDelay = new NumberSetting("AutoPlay Delay", 2.5, 8, 1, 0.5);
    private final StringSetting autoGGMessage = new StringSetting("AutoGG Message", "gg");

    private static final ResourceLocation sound = new ResourceLocation("Tenacity/Sounds/0721.wav");

    public AutoPlay() {
        super("AutoPlay", Category.MISC, "Auto Queue Next Game");

        autoPlayDelay.addParent(autoPlay, ParentAttribute.BOOLEAN_CONDITION);

        autoGGMessage.addParent(autoGG, ParentAttribute.BOOLEAN_CONDITION);
        autoGG.addParent(mode, modeSetting -> modeSetting.is("Hypixel"));
        autoHubOnBan.addParent(mode, modeSetting -> modeSetting.is("Hypixel"));

        this.addSettings(mode, autoPlay, autoPlayDelay, autoGG, autoGGMessage, autoHubOnBan);
    }

    @Override
    public void onChatReceivedEvent(ChatReceivedEvent event) {
        this.setSuffix(mode.getMode());
        String message = event.message.getUnformattedText(), strippedMessage = StringUtils.stripControlCodes(message);

        if (mode.is("Hypixel")) {
            if (autoHubOnBan.isEnabled() && strippedMessage.equals("A player has been removed from your game.")) {
                ChatUtil.send("/lobby");
                NotificationManager.post(NotificationType.WARNING, "AutoPlay", "A player in your lobby got banned.");
            }

            String m = event.message.toString();
            if (m.contains("ClickEvent{action=RUN_COMMAND, value='/play ")) {
                if (autoGG.isEnabled() && !strippedMessage.startsWith("You died!")) {
                    ChatUtil.send("/ac " + autoGGMessage.getString());
                }
                if (autoPlay.isEnabled()) {
                    sendToGame(m.split("action=RUN_COMMAND, value='")[1].split("'}")[0]);
                }
            }
        }

        if (mode.is("Bloxd") && autoPlay.isEnabled()) {
            if (strippedMessage.contains("Click here to play again!")) {
                for (IChatComponent sibling : event.message.getSiblings()) {
                    ChatStyle chatStyle = sibling.getChatStyle();
                    if (chatStyle != null) {
                        ClickEvent clickEvent = chatStyle.getChatClickEvent();
                        if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                            String command = clickEvent.getValue();
                            if (command.startsWith("/play ")) {
                                sendToGame(command);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendToGame(String command) {
        float delay = autoPlayDelay.getValue().floatValue();
        NotificationManager.post(NotificationType.INFO, "AutoPlay",
                "Sending you to a new game" + (delay > 0 ? " in " + delay + "s" : "") + "!", delay);
            Multithreading.schedule(() -> ChatUtil.send(command), (long) delay, TimeUnit.SECONDS);
        if (HUDMod.specialsound.isEnabled()) {
            SoundUtils.playSound(sound, HUDMod.specicalsoundVolume.getValue().floatValue());
        }
    }
}