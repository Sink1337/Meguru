package dev.merguru.ui.clickguis.dropdown;

import dev.merguru.Merguru;
import dev.merguru.module.Category;
import dev.merguru.module.impl.movement.InventoryMove;
import dev.merguru.module.impl.render.ClickGUIMod;
import dev.merguru.ui.searchbar.SearchBar;
import dev.merguru.ui.sidegui.SideGUI;
import dev.merguru.utils.animations.Animation;
import dev.merguru.utils.animations.Direction;
import dev.merguru.utils.animations.impl.EaseBackIn;
import dev.merguru.utils.render.RenderUtil;
import dev.merguru.utils.render.Theme;
import dev.merguru.utils.tuples.Pair;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class DropdownClickGUI extends GuiScreen {

    private final Pair<Animation, Animation> openingAnimations = Pair.of(
            new EaseBackIn(400, 1, 2f),
            new EaseBackIn(400, .4f, 2f));


    private List<CategoryPanel> categoryPanels;

    public boolean binding;


    public static boolean gradient;

    @Override
    public void onDrag(int mouseX, int mouseY) {
        for (CategoryPanel catPanels : categoryPanels) {
            catPanels.onDrag(mouseX, mouseY);
        }
        Merguru.INSTANCE.getSideGui().onDrag(mouseX, mouseY);
    }

    @Override
    public void initGui() {
        openingAnimations.use((fade, opening) -> {
            fade.setDirection(Direction.FORWARDS);
            opening.setDirection(Direction.FORWARDS);
        });


        if (categoryPanels == null) {
            categoryPanels = new ArrayList<>();
            for (Category category : Category.values()) {
                categoryPanels.add(new CategoryPanel(category, openingAnimations));
            }
        }

        Merguru.INSTANCE.getSideGui().initGui();
        Merguru.INSTANCE.getSearchBar().initGui();


        for (CategoryPanel catPanels : categoryPanels) {
            catPanels.initGui();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE && !binding) {
            if (Merguru.INSTANCE.getSearchBar().isFocused()) {
                Merguru.INSTANCE.getSearchBar().getSearchField().setText("");
                Merguru.INSTANCE.getSearchBar().getSearchField().setFocused(false);
                return;
            }

            if (Merguru.INSTANCE.getSideGui().isFocused()) {
                Merguru.INSTANCE.getSideGui().setFocused(false);
                return;
            }

            Merguru.INSTANCE.getSearchBar().getOpenAnimation().setDirection(Direction.BACKWARDS);
            openingAnimations.use((fade, opening) -> {
                fade.setDirection(Direction.BACKWARDS);
                opening.setDirection(Direction.BACKWARDS);
            });
        }
        Merguru.INSTANCE.getSideGui().keyTyped(typedChar, keyCode);
        Merguru.INSTANCE.getSearchBar().keyTyped(typedChar, keyCode);
        categoryPanels.forEach(categoryPanel -> categoryPanel.keyTyped(typedChar, keyCode));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        binding = categoryPanels.stream().anyMatch(CategoryPanel::isTyping) ||
                (Merguru.INSTANCE.getSideGui().isFocused() && Merguru.INSTANCE.getSideGui().typing) || Merguru.INSTANCE.getSearchBar().isTyping();


        //  Gui.drawRect2(0,0, width, height, ColorUtil.applyOpacity(0, Tenacity.INSTANCE.getSearchBar().getFocusAnimation().getOutput().floatValue() * .25f));
        if (ClickGUIMod.walk.isEnabled() && !binding) {
            InventoryMove.updateStates();
        }

        // If the closing animation finished then change the gui screen to null
        if (openingAnimations.getSecond().finished(Direction.BACKWARDS)) {
            mc.displayGuiScreen(null);
            return;
        }

        gradient = Theme.getCurrentTheme().isGradient() || ClickGUIMod.gradient.isEnabled();


        boolean focusedConfigGui = Merguru.INSTANCE.getSideGui().isFocused() || Merguru.INSTANCE.getSearchBar().isTyping();
        int fakeMouseX = focusedConfigGui ? 0 : mouseX, fakeMouseY = focusedConfigGui ? 0 : mouseY;
        ScaledResolution sr = new ScaledResolution(mc);


        RenderUtil.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f, openingAnimations.getSecond().getOutput().floatValue() + .6f);

        for (CategoryPanel catPanels : categoryPanels) {
            catPanels.drawScreen(fakeMouseX, fakeMouseY);
        }

        RenderUtil.scaleEnd();
        categoryPanels.forEach(categoryPanel -> categoryPanel.drawToolTips(fakeMouseX, fakeMouseY));

        //Draw Side GUI

        SideGUI sideGUI = Merguru.INSTANCE.getSideGui();
        sideGUI.getOpenAnimation().setDirection(openingAnimations.getFirst().getDirection());
        sideGUI.drawScreen(mouseX, mouseY);

        SearchBar searchBar = Merguru.INSTANCE.getSearchBar();
        searchBar.setAlpha(openingAnimations.getFirst().getOutput().floatValue() * (1 - sideGUI.getClickAnimation().getOutput().floatValue()));
        searchBar.drawScreen(fakeMouseX, fakeMouseY);
    }

    public void renderEffects() {
        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtil.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f, openingAnimations.getSecond().getOutput().floatValue() + .6f);
        for (CategoryPanel catPanels : categoryPanels) {
            catPanels.renderEffects();
        }
        RenderUtil.scaleEnd();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean focused = Merguru.INSTANCE.getSideGui().isFocused();
        Merguru.INSTANCE.getSideGui().mouseClicked(mouseX, mouseY, mouseButton);
        Merguru.INSTANCE.getSearchBar().mouseClicked(mouseX, mouseY, mouseButton);
        if (!focused) {
            categoryPanels.forEach(cat -> cat.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        boolean focused = Merguru.INSTANCE.getSideGui().isFocused();
        Merguru.INSTANCE.getSideGui().mouseReleased(mouseX, mouseY, state);
        Merguru.INSTANCE.getSearchBar().mouseReleased(mouseX, mouseY, state);
        if (!focused) {
            categoryPanels.forEach(cat -> cat.mouseReleased(mouseX, mouseY, state));
        }
    }

    @Override
    public void onGuiClosed() {
        if (ClickGUIMod.rescale.isEnabled()) {
            mc.gameSettings.guiScale = ClickGUIMod.prevGuiScale;
        }
    }


}
