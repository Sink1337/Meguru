package dev.meguru.commands.impl;

import dev.meguru.Meguru;
import dev.meguru.commands.Command;
import dev.meguru.config.DragManager;
import dev.meguru.utils.misc.Multithreading;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manages local configurations, including module parameters and drag data.", ".config <list/save/load/delete/folder> [name]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }

        String subCommand = args[0].toLowerCase();

        Multithreading.runAsync(() -> {
            switch (subCommand) {
                case "list":
                    sendChatWithPrefix("Listing available configurations:");
                    Meguru.INSTANCE.getConfigManager().collectConfigs();
                    List<String> configs = Meguru.INSTANCE.getConfigManager().localConfigs.stream()
                            .map(localConfig -> localConfig.getName())
                            .collect(Collectors.toList());

                    if (configs.isEmpty()) {
                        sendChatWithPrefix("No configurations found.");
                    } else {
                        for (String configName : configs) {
                            sendChatWithPrefix("- " + configName);
                        }
                    }
                    break;

                case "save":
                    if (args.length != 2) {
                        sendChatError("Usage: .config save <name>");
                        return;
                    }
                    String saveName = args[1];
                    sendChatWithPrefix("Saving current configuration (module parameters and drag data) as '" + saveName + "'...");
                    if (Meguru.INSTANCE.getConfigManager().saveConfig(saveName)) {
                        DragManager.saveDragData();
                        sendChatWithPrefix("Configuration '" + saveName + "' saved successfully!");
                        Meguru.INSTANCE.getConfigManager().collectConfigs();
                    } else {
                        sendChatError("Failed to save configuration '" + saveName + "'.");
                    }
                    break;

                case "load":
                    if (args.length != 2) {
                        sendChatError("Usage: .config load <name>");
                        return;
                    }
                    String loadName = args[1];
                    sendChatWithPrefix("Loading configuration (module parameters and drag data) '" + loadName + "'...");
                    Meguru.INSTANCE.getConfigManager().collectConfigs();
                    String configData = Meguru.INSTANCE.getConfigManager().localConfigs.stream()
                            .filter(lc -> lc.getName().equalsIgnoreCase(loadName))
                            .findFirst()
                            .map(lc -> Meguru.INSTANCE.getConfigManager().readConfigData(lc.getFile().toPath()))
                            .orElse(null);

                    if (configData != null && !configData.isEmpty()) {
                        if (Meguru.INSTANCE.getConfigManager().loadConfig(configData)) {
                            DragManager.loadDragData();
                            sendChatWithPrefix("Configuration '" + loadName + "' loaded successfully!");
                        } else {
                            sendChatError("The configuration '" + loadName + "' did not load successfully!");
                        }
                    } else {
                        sendChatError("Configuration '" + loadName + "' not found or could not be read!");
                    }
                    break;

                case "delete":
                    if (args.length != 2) {
                        sendChatError("Usage: .config delete <name>");
                        return;
                    }
                    String deleteName = args[1];
                    sendChatWithPrefix("Deleting configuration '" + deleteName + "'...");
                    if (Meguru.INSTANCE.getConfigManager().delete(deleteName)) {
                        sendChatWithPrefix("Configuration '" + deleteName + "' deleted successfully!");
                        Meguru.INSTANCE.getConfigManager().collectConfigs();
                    } else {
                        sendChatError("Failed to delete configuration '" + deleteName + "'.");
                    }
                    break;

                case "folder":
                    if (args.length != 1) {
                        sendChatError("Usage: .config folder");
                        return;
                    }
                    Meguru.INSTANCE.getConfigManager().openConfigFolder();
                    break;

                default:
                    sendChatError("Invalid subcommand: '" + subCommand + "'");
                    usage();
                    break;
            }
        });
    }
}