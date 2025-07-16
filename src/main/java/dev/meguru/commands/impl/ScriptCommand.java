package dev.meguru.commands.impl;

import dev.meguru.Meguru;
import dev.meguru.commands.Command;

public final class ScriptCommand extends Command {

    public ScriptCommand() {
        super("scriptreload", "Reloads all scripts", ".scriptreload");
    }

    @Override
    public void execute(String[] args) {
        Meguru.INSTANCE.getScriptManager().reloadScripts();
    }

}
