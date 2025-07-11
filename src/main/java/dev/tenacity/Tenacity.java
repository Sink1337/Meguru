package dev.tenacity;

import dev.tenacity.commands.CommandHandler;
import dev.tenacity.config.ConfigManager;
import dev.tenacity.config.DragManager;
import dev.tenacity.event.EventProtocol;
import dev.tenacity.intent.api.account.IntentAccount;
import dev.tenacity.intent.cloud.CloudDataManager;
import dev.tenacity.module.Module;
import dev.tenacity.module.ModuleCollection;
import dev.tenacity.scripting.api.ScriptManager;
import dev.tenacity.ui.altmanager.GuiAltManager;
import dev.tenacity.ui.altmanager.helpers.KingGenApi;
import dev.tenacity.ui.searchbar.SearchBar;
import dev.tenacity.ui.sidegui.SideGUI;
import dev.tenacity.utils.Utils;
import dev.tenacity.utils.client.ReleaseType;
import dev.tenacity.utils.misc.DiscordRPC;
import dev.tenacity.utils.objects.DiscordAccount;
import dev.tenacity.utils.objects.Dragging;
import dev.tenacity.utils.objects.HTTPUtil;
import dev.tenacity.utils.render.WallpaperEngine;
import dev.tenacity.utils.server.PingerUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.MonthDay;
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

    private final EventProtocol eventProtocol = new EventProtocol();
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
    public void downloadBackGroundVideo() {
        LOGGER.info("Downloading background video");
        if (is0721) {
            backGroundFile = new File(BACKGROUND, "0721.mp4");
            if (!backGroundFile.exists()) {
                try {
                    if (backGroundFile.getParentFile().mkdirs()) {
                        backGroundFile.createNewFile();
                        backGroundFile.mkdir();
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                HTTPUtil.download("https://tenacity.cn-nb1.rains3.com/0721.mp4", backGroundFile);
            }
        } else {
            backGroundFile = new File(BACKGROUND, "background.mp4");
            if (!backGroundFile.exists()) {
                try {
                    if (backGroundFile.getParentFile().mkdirs()) {
                        backGroundFile.createNewFile();
                        backGroundFile.mkdir();
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                HTTPUtil.download("https://tenacity.cn-nb1.rains3.com/background.mp4", backGroundFile);
            }
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
}
