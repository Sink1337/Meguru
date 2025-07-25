package dev.meguru.ui.sidegui.utils;

import dev.meguru.Meguru;
import dev.meguru.ui.sidegui.panels.configpanel.ConfigPanel;
import dev.meguru.ui.sidegui.panels.scriptpanel.ScriptPanel;
import dev.meguru.utils.misc.MathUtils;

public class CloudDataUtils {


    public static String getLastEditedTime(String epoch) {
        long epochTime = Long.parseLong(epoch);

        //Epoch is in seconds so we convert the System time to seconds
        long timeSince = (System.currentTimeMillis() / 1000) - epochTime;

        //Now we see if the config was uploaded less than a minute ago
        if (timeSince < 60) {
            return "Just now";
        }

        //Now we convert it to minutes
        timeSince = (long) MathUtils.round(timeSince / 60f, 0);

        //If the config was uploaded less than an hour ago
        if (timeSince < 60) {
            return timeSince + ((timeSince > 1) ? " minutes " : " minute ") + "ago";
        }

        //Convert it to hours
        timeSince = (long) MathUtils.round(timeSince / 60f, 0);

        if (timeSince < 24) {
            return timeSince + ((timeSince > 1) ? " hours " : " hour ") + "ago";
        }

        timeSince = (long) MathUtils.round(timeSince / 24f, 0);

        return timeSince + ((timeSince > 1) ? " days " : " day ") + "ago";
    }


    public static void refreshCloud() {
        if (Meguru.INSTANCE.getSideGui().getPanels() != null) {
            ConfigPanel configPanel = (ConfigPanel) Meguru.INSTANCE.getSideGui().getPanels().get("Configs");
            configPanel.refresh();

            ScriptPanel scriptPanel = (ScriptPanel) Meguru.INSTANCE.getSideGui().getPanels().get("Scripts");
            scriptPanel.refresh();
        }

    }


}
