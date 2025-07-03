package dev.tenacity.commands.impl;

import dev.tenacity.Tenacity; // 假设 Tenacity 类是存在的，并且包含 getConfigManager() 方法
import dev.tenacity.commands.Command;
import dev.tenacity.utils.misc.Multithreading; // 引入 Multithreading 以便在异步线程中执行操作
import dev.tenacity.config.DragManager; // 引入 DragManager
import java.util.List; // 用于处理配置列表
import java.util.stream.Collectors; // 用于流操作

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manages local configurations, including module parameters and drag data.", ".config <list/save/load/delete> [name]");
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
                    Tenacity.INSTANCE.getConfigManager().collectConfigs();
                    List<String> configs = Tenacity.INSTANCE.getConfigManager().localConfigs.stream()
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
                    if (Tenacity.INSTANCE.getConfigManager().saveConfig(saveName)) {
                        DragManager.saveDragData(); // 保存拖拽数据
                        sendChatWithPrefix("Configuration '" + saveName + "' saved successfully!");
                        Tenacity.INSTANCE.getConfigManager().collectConfigs();
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
                    Tenacity.INSTANCE.getConfigManager().collectConfigs();
                    String configData = Tenacity.INSTANCE.getConfigManager().localConfigs.stream()
                            .filter(lc -> lc.getName().equalsIgnoreCase(loadName))
                            .findFirst()
                            .map(lc -> Tenacity.INSTANCE.getConfigManager().readConfigData(lc.getFile().toPath()))
                            .orElse(null);

                    if (configData != null && !configData.isEmpty()) {
                        if (Tenacity.INSTANCE.getConfigManager().loadConfig(configData)) {
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
                    if (Tenacity.INSTANCE.getConfigManager().delete(deleteName)) {
                        sendChatWithPrefix("Configuration '" + deleteName + "' deleted successfully!");
                        Tenacity.INSTANCE.getConfigManager().collectConfigs();
                    } else {
                        sendChatError("Failed to delete configuration '" + deleteName + "'.");
                    }
                    break;

                default:
                    sendChatError("Invalid subcommand: '" + subCommand + "'");
                    usage();
                    break;
            }
        });
    }
}
