package dev.meguru.scripting.api.bindings;

import dev.meguru.Meguru;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.impl.render.HUDMod;
import dev.meguru.utils.player.ChatUtil;
import dev.meguru.utils.time.TimerUtil;
import dev.meguru.utils.tuples.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import store.intent.intentguard.annotation.Exclude;
import store.intent.intentguard.annotation.Strategy;

import java.awt.*;

@Exclude(Strategy.NAME_REMAPPING)
public class ClientBinding {

    public String getClientVersion() {
        return Meguru.INSTANCE.getVersion();
    }


    public TimerUtil createTimer() {
        return new TimerUtil();
    }

    public void printClientMsg(String text) {
        ChatUtil.print(text);
    }

    public float fps() {
        return Minecraft.getDebugFPS();
    }

    public EntityLivingBase getAuraTarget() {
        return TargetManager.target;
    }

    public Pair<Color, Color> getClientColors() {
        return HUDMod.getClientColors();
    }

    public boolean leftMouseButtonDown() {
        return Mouse.isButtonDown(0);
    }

    public boolean rightMouseButtonDown() {
        return Mouse.isButtonDown(1);
    }

    public boolean isKeyDown(int key) {
        return Keyboard.isCreated() && Keyboard.isKeyDown(key);
    }


    public Module getModule(String moduleName) {
        Module module = Meguru.INSTANCE.getModuleCollection().getModuleByName(moduleName);
        if (module != null) {
            return module;
        } else {
            throw new NullPointerException("Module " + moduleName + " does not exist.");
        }
    }

}
