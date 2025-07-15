package dev.merguru.commands.impl;

import dev.merguru.Merguru;
import dev.merguru.commands.Command;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.KeybindSetting;
import org.lwjgl.input.Keyboard;

public class ClearBindsCommand extends Command {

    public ClearBindsCommand() {
        super("clearbinds", "Clears all of your keybinds", ".clearbinds");
    }

    @Override
    public void execute(String[] args) {
        int count = 0;
        for (Module module : Merguru.INSTANCE.getModuleCollection().getModules()) {
            KeybindSetting keybind = module.getKeybind();
            if (keybind.getCode() != Keyboard.KEY_NONE) {
                keybind.setCode(Keyboard.KEY_NONE);
                count++;
            }
        }
        sendChatWithPrefix("Binds cleared! " + count + " modules affected.");
    }

}
