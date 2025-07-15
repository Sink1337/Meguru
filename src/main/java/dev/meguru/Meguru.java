package dev.meguru;

import de.florianmichael.viamcp.ViaMCP;
import dev.meguru.commands.CommandHandler;
import dev.meguru.commands.impl.*;
import dev.meguru.config.ConfigManager;
import dev.meguru.config.DragManager;
import dev.meguru.event.EventProtocol;
import dev.meguru.intent.api.account.IntentAccount;
import dev.meguru.intent.cloud.CloudDataManager;
import dev.meguru.module.BackgroundProcess;
import dev.meguru.module.Module;
import dev.meguru.module.ModuleCollection;
import dev.meguru.scripting.api.ScriptManager;
import dev.meguru.ui.altmanager.GuiAltManager;
import dev.meguru.ui.altmanager.helpers.KingGenApi;
import dev.meguru.ui.searchbar.SearchBar;
import dev.meguru.ui.sidegui.SideGUI;
import dev.meguru.utils.Utils;
import dev.meguru.utils.addons.rise.component.RenderSlotComponent;
import dev.meguru.utils.addons.rise.component.RotationComponent;
import dev.meguru.utils.client.ReleaseType;
import dev.meguru.utils.misc.DiscordRPC;
import dev.meguru.utils.objects.DiscordAccount;
import dev.meguru.utils.objects.Dragging;
import dev.meguru.utils.render.Theme;
import dev.meguru.utils.render.WallpaperEngine;
import dev.meguru.utils.server.PingerUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import store.intent.intentguard.annotation.Bootstrap;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Setter
public class Meguru implements Utils {

    public static final Meguru INSTANCE = new Meguru();

    public static final String NAME = "Meguru";
    public static final String VERSION = "1.0";
    public static boolean is0721 = false;
    public static final ReleaseType RELEASE = ReleaseType.DEV;
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static final File DIRECTORY = new File(mc.mcDataDir, NAME);

    public EventProtocol eventProtocol;
    private final CloudDataManager cloudDataManager = new CloudDataManager();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SideGUI sideGui = new SideGUI();
    private final SearchBar searchBar = new SearchBar();
    private ModuleCollection moduleCollection;
    private ScriptManager scriptManager;
    private IntentAccount intentAccount;
    private ConfigManager configManager;
    private GuiAltManager altManager;
    private CommandHandler commandHandler;
    private PingerUtils pingerUtils;
    private DiscordRPC discordRPC;
    public KingGenApi kingGenApi;
    private DiscordAccount discordAccount;

    public WallpaperEngine videoRenderer;
    public static File backGroundFile;
    public static final File BACKGROUND = new File(DIRECTORY, "Background");

    public static boolean updateGuiScale;
    public static int prevGuiScale;

    public String getVersion() {
        return VERSION + (RELEASE != ReleaseType.PUBLIC ? " (" + RELEASE.getName() + ")" : "");
    }

    public final Color getClientColor() {
        return new Color(236, 133, 209);
    }

    public final Color getAlternateClientColor() {
        return new Color(28, 167, 222);
    }

    public boolean isEnabled(Class<? extends Module> c) {
        Module m = INSTANCE.moduleCollection.get(c);
        return m != null && m.isEnabled();
    }

    public Dragging createDrag(Module module, String name, float x, float y) {
        DragManager.draggables.put(name, new Dragging(module, name, x, y));
        return DragManager.draggables.get(name);
    }
    public void ensureBackgroundVideoExists() {
        LOGGER.info("Initializing local background...");

        final String fileName = is0721 ? "0721.mp4" : "Background.mp4";
        backGroundFile = new File(BACKGROUND, fileName);

        if (!BACKGROUND.exists()) {
            LOGGER.info("Creating background directory: {}", BACKGROUND.getAbsolutePath());
            BACKGROUND.mkdirs();
        }

        if (!backGroundFile.exists()) {
            LOGGER.warn("Local background file not found. Copying from resources...");

            String resourcePath = "/assets/minecraft/meguru/Background/" + fileName;

            try (InputStream inputStream = Meguru.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    LOGGER.error("FATAL: Resource '{}' not found inside the JAR. Please check your project setup.", resourcePath);
                    return;
                }

                Files.copy(inputStream, backGroundFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Successfully copied background to {}", backGroundFile.getAbsolutePath());

            } catch (IOException e) {
                LOGGER.error("Failed to copy background file from resources.", e);
            }
        } else {
            LOGGER.info("Local background file found: {}", backGroundFile.getAbsolutePath());
        }
    }

    public void initVideoBackground() {
        if (videoRenderer != null) {
            videoRenderer.close();
        }
        videoRenderer = new WallpaperEngine();
        File videoFile;
        if (is0721) {
            videoFile = new File(BACKGROUND, "0721.mp4");
        }else {
            videoFile = new File(BACKGROUND, "Background.mp4");
        }
        videoRenderer.setup(videoFile, 60);
    }

    @Bootstrap
    public void start() {
        // Setup Intent API access
        Meguru.INSTANCE.setIntentAccount(new IntentAccount());

        moduleCollection = new ModuleCollection();
        eventProtocol = new EventProtocol();

        moduleCollection.init();

        Theme.init();

        Meguru.INSTANCE.setPingerUtils(new PingerUtils());

        Meguru.INSTANCE.setScriptManager(new ScriptManager());

        CommandHandler commandHandler = new CommandHandler();
        commandHandler.commands.addAll(Arrays.asList(
                new FriendCommand(), new CopyNameCommand(), new BindCommand(), new UnbindCommand(),
                new ScriptCommand(), new SettingCommand(), new HelpCommand(),
                new VClipCommand(), new ClearBindsCommand(), new ClearConfigCommand(),
                new LoadCommand(), new ToggleCommand(), new ConfigCommand()
        ));
        Meguru.INSTANCE.setCommandHandler(commandHandler);
        Meguru.INSTANCE.getEventProtocol().register(new BackgroundProcess());


        Meguru.INSTANCE.setConfigManager(new ConfigManager());
        ConfigManager.defaultConfig = new File(Minecraft.getMinecraft().mcDataDir + "/Meguru/Config.json");
        Meguru.INSTANCE.getConfigManager().collectConfigs();
        if (ConfigManager.defaultConfig.exists()) {
            Meguru.INSTANCE.getConfigManager().loadConfig(Meguru.INSTANCE.getConfigManager().readConfigData(ConfigManager.defaultConfig.toPath()), true);
        }

        DragManager.loadDragData();

        Meguru.INSTANCE.setAltManager(new GuiAltManager());

        Meguru.INSTANCE.setKingGenApi(new KingGenApi());

        Meguru.LOGGER.info("Preparing background video file...");
        Meguru.INSTANCE.ensureBackgroundVideoExists();

        eventProtocol.register(new RotationComponent());
        eventProtocol.register(new RenderSlotComponent());

        Meguru.LOGGER.info("Initializing video background...");
        Meguru.INSTANCE.initVideoBackground();

        try {
            Meguru.LOGGER.info("Starting ViaMCP...");
            ViaMCP.create();
            ViaMCP.INSTANCE.initAsyncSlider();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
