package dev.merguru.commands.impl;

import dev.merguru.Merguru;
import dev.merguru.commands.Command;
import dev.merguru.module.Module;

public class ClearConfigCommand extends Command {

    public ClearConfigCommand() {
        super("clearconfig", "Turns off all enabled modules", ".clearconfig");
    }

    @Override
    public void execute(String[] args) {
        Merguru.INSTANCE.getModuleCollection().getModules().stream().filter(Module::isEnabled).forEach(Module::toggle);
    }
}
