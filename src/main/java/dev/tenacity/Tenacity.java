package dev.tenacity;

import dev.tenacity.commands.CommandHandler;
import dev.tenacity.commands.impl.*;
import dev.tenacity.config.ConfigManager;
import dev.tenacity.config.DragManager;
import dev.tenacity.event.EventProtocol;
import dev.tenacity.intent.api.account.IntentAccount;
import dev.tenacity.intent.cloud.CloudDataManager;
import dev.tenacity.module.BackgroundProcess;
import dev.tenacity.module.Module;
import dev.tenacity.module.ModuleCollection;
import dev.tenacity.scripting.api.ScriptManager;
import dev.tenacity.ui.altmanager.GuiAltManager;
import dev.tenacity.ui.altmanager.helpers.KingGenApi;
import dev.tenacity.ui.searchbar.SearchBar;
import dev.tenacity.ui.sidegui.SideGUI;
import dev.tenacity.utils.Utils;
import dev.tenacity.utils.addons.rise.component.RenderSlotComponent;
import dev.tenacity.utils.addons.rise.component.RotationComponent;
import dev.tenacity.utils.client.ReleaseType;
import dev.tenacity.utils.misc.DiscordRPC;
import dev.tenacity.utils.objects.DiscordAccount;
import dev.tenacity.utils.objects.Dragging;
import dev.tenacity.utils.objects.HTTPUtil;
import dev.tenacity.utils.render.Theme;
import dev.tenacity.utils.render.WallpaperEngine;
import dev.tenacity.utils.server.PingerUtils;
import dev.tenacity.viamcp.ViaMCP;
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
public class Tenacity implements Utils {

    public static final Tenacity INSTANCE = new Tenacity();

    public static final String NAME = "Tenacity";
    public static final String VERSION = "7.21";
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
    public static final File BACKGROUND = new File(DIRECTORY, "background");

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

        final String fileName = is0721 ? "0721.mp4" : "background.mp4";
        backGroundFile = new File(BACKGROUND, fileName);

        if (!BACKGROUND.exists()) {
            LOGGER.info("Creating background directory: {}", BACKGROUND.getAbsolutePath());
            BACKGROUND.mkdirs();
        }

        if (!backGroundFile.exists()) {
            LOGGER.warn("Local background file not found. Copying from resources...");

            String resourcePath = "/assets/minecraft/Tenacity/Background/" + fileName;

            try (InputStream inputStream = Tenacity.class.getResourceAsStream(resourcePath)) {
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
            videoFile = new File(BACKGROUND, "background.mp4");
        }
        videoRenderer.setup(videoFile, 60);
    }

    @Bootstrap
    public void start() {
        // Setup Intent API access
        Tenacity.INSTANCE.setIntentAccount(new IntentAccount());

        moduleCollection = new ModuleCollection();
        eventProtocol = new EventProtocol();

        moduleCollection.init();

        Theme.init();

        Tenacity.INSTANCE.setPingerUtils(new PingerUtils());

        Tenacity.INSTANCE.setScriptManager(new ScriptManager());

        CommandHandler commandHandler = new CommandHandler();
        commandHandler.commands.addAll(Arrays.asList(
                new FriendCommand(), new CopyNameCommand(), new BindCommand(), new UnbindCommand(),
                new ScriptCommand(), new SettingCommand(), new HelpCommand(),
                new VClipCommand(), new ClearBindsCommand(), new ClearConfigCommand(),
                new LoadCommand(), new ToggleCommand(), new ConfigCommand()
        ));
        Tenacity.INSTANCE.setCommandHandler(commandHandler);
        Tenacity.INSTANCE.getEventProtocol().register(new BackgroundProcess());


        Tenacity.INSTANCE.setConfigManager(new ConfigManager());
        ConfigManager.defaultConfig = new File(Minecraft.getMinecraft().mcDataDir + "/Tenacity/Config.json");
        Tenacity.INSTANCE.getConfigManager().collectConfigs();
        if (ConfigManager.defaultConfig.exists()) {
            Tenacity.INSTANCE.getConfigManager().loadConfig(Tenacity.INSTANCE.getConfigManager().readConfigData(ConfigManager.defaultConfig.toPath()), true);
        }

        DragManager.loadDragData();

        Tenacity.INSTANCE.setAltManager(new GuiAltManager());

        Tenacity.INSTANCE.setKingGenApi(new KingGenApi());

        Tenacity.LOGGER.info("Preparing background video file...");
        Tenacity.INSTANCE.ensureBackgroundVideoExists();

        eventProtocol.register(new RotationComponent());
        eventProtocol.register(new RenderSlotComponent());

        Tenacity.LOGGER.info("Initializing video background...");
        Tenacity.INSTANCE.initVideoBackground();

        try {
            Tenacity.LOGGER.info("Starting ViaMCP...");
            ViaMCP viaMCP = ViaMCP.getInstance();
            viaMCP.start();
            viaMCP.initAsyncSlider(100, 100, 110, 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
