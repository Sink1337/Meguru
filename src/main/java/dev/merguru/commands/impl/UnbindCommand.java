package dev.merguru.commands.impl;

import dev.merguru.Merguru;
import dev.merguru.commands.Command;
import dev.merguru.module.Module;
import org.lwjgl.input.Keyboard;

public final class UnbindCommand extends Command {

    public UnbindCommand() {
        super("unbind", "unbinds a module", ".unbind [module]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            usage();
        } else {
            String stringMod = args[0];
            try {
                Module module = Merguru.INSTANCE.getModuleCollection().getModuleByName(stringMod);
                module.getKeybind().setCode(Keyboard.KEY_NONE);
                sendChatWithPrefix("Set keybind for " + module.getName() + " to NONE");
            } catch (Exception e) {
                usage();
            }
        }
    }

}
