package dev.meguru.commands.impl;

import dev.meguru.Meguru;
import dev.meguru.commands.Command;
import dev.meguru.module.Module;

public class ClearConfigCommand extends Command {

    public ClearConfigCommand() {
        super("clearconfig", "Turns off all enabled modules", ".clearconfig");
    }

    @Override
    public void execute(String[] args) {
        Meguru.INSTANCE.getModuleCollection().getModules().stream().filter(Module::isEnabled).forEach(Module::toggle);
    }
}
