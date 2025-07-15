package dev.merguru.ui;

import dev.merguru.utils.Utils;

public interface Screen extends Utils {

    default void onDrag(int mouseX, int mouseY) {

    }

    void initGui();

    void keyTyped(char typedChar, int keyCode);

    void drawScreen(int mouseX, int mouseY);

    void mouseClicked(int mouseX, int mouseY, int button);

    void mouseReleased(int mouseX, int mouseY, int state);

}
