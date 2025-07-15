package dev.merguru.commands.impl;

import dev.merguru.Merguru;
import dev.merguru.commands.Command;

public final class ScriptCommand extends Command {

    public ScriptCommand() {
        super("scriptreload", "Reloads all scripts", ".scriptreload");
    }

    @Override
    public void execute(String[] args) {
        Merguru.INSTANCE.getScriptManager().reloadScripts();
    }

}
