package dev.meguru.module;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.meguru.Meguru;
import dev.meguru.config.ConfigSetting;
import dev.meguru.event.ListenerAdapter;
import dev.meguru.module.impl.render.GlowESP;
import dev.meguru.module.impl.render.HUDMod;
import dev.meguru.module.impl.render.NotificationsMod;
import dev.meguru.module.settings.Setting;
import dev.meguru.module.settings.impl.*;
import dev.meguru.ui.notifications.NotificationManager;
import dev.meguru.ui.notifications.NotificationType;
import dev.meguru.utils.Utils;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.misc.Multithreading;
import dev.meguru.utils.misc.SoundUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import store.intent.intentguard.annotation.Exclude;
import store.intent.intentguard.annotation.Strategy;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class Module extends ListenerAdapter implements Utils {
    @Expose
    @SerializedName("name")
    private final String name;
    private final String description;
    private final Category category;
    private final CopyOnWriteArrayList<Setting> settingsList = new CopyOnWriteArrayList<>();
    private String suffix;
    private String author = "";

    @Expose
    @SerializedName("toggled")
    protected boolean enabled;
    @Expose
    @SerializedName("settings")
    public ConfigSetting[] cfgSettings;


    private boolean expanded;
    private final Animation animation = new DecelerateAnimation(250, 1).setDirection(Direction.BACKWARDS);

    public static int categoryCount;
    public static float allowedClickGuiHeight = 300;

    private final KeybindSetting keybind = new KeybindSetting(Keyboard.KEY_NONE);

    private final ResourceLocation enableSound1 = new ResourceLocation("meguru/sounds/enable.wav");
    private final ResourceLocation disableSound1 = new ResourceLocation("meguru/sounds/disable.wav");
    private final ResourceLocation enableSound2 = new ResourceLocation("meguru/sounds/ciallo.wav");
    private final ResourceLocation disableSound2 = new ResourceLocation("meguru/sounds/boyangyang.wav");

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        addSettings(keybind);
    }

    public boolean isInGame() {
        return mc.theWorld != null && mc.thePlayer != null;
    }

    public void addSettings(Setting... settings) {
        settingsList.addAll(Arrays.asList(settings));
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setToggled(boolean toggled) {
        this.enabled = toggled;
        if (toggled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void toggleSilent() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void toggleSilent(boolean toggled) {
        this.enabled = toggled;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void toggle() {
        toggleSilent();

        HUDMod hudMod = Meguru.INSTANCE.getModuleCollection().getModule(HUDMod.class);
        float volume = HUDMod.togglesoundVolume.getValue().floatValue();

        if (hudMod != null && HUDMod.togglesound.isEnabled() && HUDMod.togglesoundmode.is("Click")) {
            mc.theWorld.playSound(Module.mc.thePlayer.posX, Module.mc.thePlayer.posY, Module.mc.thePlayer.posZ, "random.click", 1f, enabled ? 1f : 0.7f, false);
        }

        if (hudMod != null && HUDMod.togglesound.isEnabled() && HUDMod.togglesoundmode.is("Sigma")) {
            if (enabled) {
                SoundUtils.playSound(enableSound1, volume);
            } else {
                SoundUtils.playSound(disableSound1, volume);
            }
        }

        if (hudMod != null && HUDMod.togglesound.isEnabled() && HUDMod.togglesoundmode.is("YuzuSoft")) {
            if (enabled) {
                SoundUtils.playSound(enableSound2, volume);
            } else {
                SoundUtils.playSound(disableSound2, volume);
            }
        }

        if (NotificationsMod.toggleNotifications.isEnabled()) {
            String titleToggle = "Module toggled";
            String descriptionToggleOn = this.getName() + " was " + "§aenabled\r";
            String descriptionToggleOff = this.getName() + " was " + "§cdisabled\r";

            switch (NotificationsMod.mode.getMode()) {
                case "Default":
                    if (NotificationsMod.onlyTitle.isEnabled()) titleToggle = this.getName() + " toggled";
                    break;
                case "SuicideX":
                    if (this.isEnabled()) {
                        titleToggle = "Enabled Module " + this.getName() + ". PogO";
                    } else {
                        titleToggle = "Disabled Module " + this.getName() + ". :/";
                    }
                    descriptionToggleOff = "";
                    descriptionToggleOn = "";
                    break;
            }
            if (enabled) {
                NotificationManager.post(NotificationType.SUCCESS, titleToggle, descriptionToggleOn);
            } else {
                NotificationManager.post(NotificationType.DISABLE, titleToggle, descriptionToggleOff);
            }
        }
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public String getSuffix() {
        return suffix;
    }

    public boolean hasMode() {
        return suffix != null && !suffix.isEmpty();
    }


    public void onEnable() {
        Meguru.INSTANCE.getEventProtocol().register(this);
    }

    public void onDisable() {
        if (this instanceof GlowESP) {
            GlowESP.fadeIn.setDirection(Direction.BACKWARDS);
            Multithreading.schedule(() -> Meguru.INSTANCE.getEventProtocol().unregister(this), 250, TimeUnit.MILLISECONDS);
        } else {
            Meguru.INSTANCE.getEventProtocol().unregister(this);
        }
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public void setKey(int code) {
        this.keybind.setCode(code);
    }


    @Exclude(Strategy.NAME_REMAPPING)
    public String getName() {
        return name;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public String getDescription() {
        return description;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public boolean isEnabled() {
        return enabled;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public int getKeybindCode() {
        return keybind.getCode();
    }


    @Exclude(Strategy.NAME_REMAPPING)
    public NumberSetting getNumberSetting(String name) {
        for (Setting setting : settingsList) {
            if (setting instanceof NumberSetting && setting.getName().equalsIgnoreCase(name)) {
                return (NumberSetting) setting;
            }
        }
        return null;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public BooleanSetting getBooleanSetting(String name) {
        for (Setting setting : settingsList) {
            if (setting instanceof BooleanSetting && setting.getName().equalsIgnoreCase(name)) {
                return (BooleanSetting) setting;
            }
        }
        return null;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public ModeSetting getModeSetting(String name) {
        for (Setting setting : settingsList) {
            if (setting instanceof ModeSetting && setting.getName().equalsIgnoreCase(name)) {
                return (ModeSetting) setting;
            }
        }
        return null;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public StringSetting getStringSetting(String name) {
        for (Setting setting : settingsList) {
            if (setting instanceof StringSetting && setting.getName().equalsIgnoreCase(name)) {
                return (StringSetting) setting;
            }
        }
        return null;
    }

    @Exclude(Strategy.NAME_REMAPPING)
    public MultipleBoolSetting getMultiBoolSetting(String name) {
        for (Setting setting : settingsList) {
            if (setting instanceof MultipleBoolSetting && setting.getName().equalsIgnoreCase(name)) {
                return (MultipleBoolSetting) setting;
            }
        }
        return null;
    }


    @Exclude(Strategy.NAME_REMAPPING)
    public ColorSetting getColorSetting(String name) {
        for (Setting setting : settingsList) {
            if (setting instanceof ColorSetting && setting.getName().equalsIgnoreCase(name)) {
                return (ColorSetting) setting;
            }
        }
        return null;
    }
}