package dev.meguru.ui.notifications;

import dev.meguru.utils.font.FontUtil;
import lombok.Getter;

import java.awt.*;

@Getter
public enum NotificationType {
    SUCCESS("Success", new Color(20, 250, 90), FontUtil.CHECKMARK),
    DISABLE("Disable", new Color(255, 30, 30), FontUtil.XMARK),
    INFO("Info", Color.WHITE, FontUtil.INFO),
    WARNING("Warning", Color.YELLOW, FontUtil.WARNING);

    private final String name;
    private final Color color;
    private final String icon;

    NotificationType(String name, Color color, String icon) {
        this.name = name;
        this.color = color;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }
}