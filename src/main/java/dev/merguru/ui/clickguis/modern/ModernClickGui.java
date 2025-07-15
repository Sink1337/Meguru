package dev.merguru.ui.clickguis.modern;

import dev.merguru.Merguru;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.ModuleCollection;
import dev.merguru.module.impl.movement.InventoryMove;
import dev.merguru.module.impl.render.ClickGUIMod;
import dev.merguru.module.settings.Setting;
import dev.merguru.ui.clickguis.modern.components.CategoryButton;
import dev.merguru.ui.clickguis.modern.components.ClickCircle;
import dev.merguru.ui.clickguis.modern.components.Component;
import dev.merguru.ui.clickguis.modern.components.ModuleRect;
import dev.merguru.ui.searchbar.SearchBar;
import dev.merguru.ui.sidegui.SideGUI;
import dev.merguru.utils.animations.Animation;
import dev.merguru.utils.animations.Direction;
import dev.merguru.utils.animations.impl.DecelerateAnimation;
import dev.merguru.utils.font.FontUtil;
import dev.merguru.utils.misc.HoveringUtil;
import dev.merguru.utils.objects.Drag;
import dev.merguru.utils.render.RoundedUtil;
import dev.merguru.utils.render.StencilUtil;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ModernClickGui extends GuiScreen {

    public static final Drag drag = new Drag(40, 40);
    public static boolean searching = false;
    private final Color backgroundColor = new Color(30, 31, 35);
    private final Color categoryColor = new Color(47, 49, 54);
    private final Color lighterGray = new Color(68, 71, 78);
    private final List<ClickCircle> circleClicks = new ArrayList<>();
    private final List<Component> categories = new ArrayList() {{
        for (Category category : Category.values()) {
            add(new CategoryButton(category));
        }
    }};
    public float rectHeight = 255, rectWidth = 370;
    private Category currentCategory = Category.COMBAT;
    private Animation openingAnimation;
    private Animation expandedAnimation;
    private ModulesPanel modpanel;
    private HashMap<Category, ArrayList<ModuleRect>> moduleRects;
    private boolean firstOpen = true;
    public boolean typing;

    public void drawBigRect() {
        float x = drag.getX(), y = drag.getY();

        if (!openingAnimation.isDone()) {
            x -= width + rectWidth / 2f;
            x += (width + rectWidth / 2f) * openingAnimation.getOutput().floatValue();
        } else if (openingAnimation.getDirection().equals(Direction.BACKWARDS)) {
            return;
        }

        RoundedUtil.drawRound(x, y, rectWidth, rectHeight, 10, Color.BLACK);
    }

    @Override
    public void onDrag(int mouseX, int mouseY) {
        if (firstOpen) {
            drag.setX(width / 2F - rectWidth / 2F);
            drag.setY(height / 2F - rectHeight / 2F);
            firstOpen = false;
        }

        drag.onDraw(mouseX, mouseY);
        Merguru.INSTANCE.getSideGui().onDrag(mouseX, mouseY);
    }

    @Override
    public void initGui() {
        if (firstOpen) {
            drag.setX(width / 2F - rectWidth / 2F);
            drag.setY(height / 2F - rectHeight / 2F);
            firstOpen = false;
        }
        if (modpanel == null) {
            modpanel = new ModulesPanel();
        }

        Merguru.INSTANCE.getSideGui().initGui();
        Merguru.INSTANCE.getSearchBar().initGui();
        ClickGUIMod clickMod = Merguru.INSTANCE.getModuleCollection().getModule(ClickGUIMod.class);
        currentCategory = clickMod.getActiveCategory();
        categories.forEach(Component::initGui);
        openingAnimation = new DecelerateAnimation(300, 1);
        expandedAnimation = new DecelerateAnimation(250, 1);

        if (moduleRects != null) {
            moduleRects.forEach((cat, list) -> list.forEach(ModuleRect::initGui));
        }
        modpanel.initGui();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1 && !typing) {

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
            openingAnimation.setDirection(Direction.BACKWARDS);
            ClickGUIMod clickMod = Merguru.INSTANCE.getModuleCollection().getModule(ClickGUIMod.class);
            clickMod.setActiveCategory(currentCategory);
        }

        Merguru.INSTANCE.getSideGui().keyTyped(typedChar, keyCode);
        Merguru.INSTANCE.getSearchBar().keyTyped(typedChar, keyCode);
        modpanel.keyTyped(typedChar, keyCode);
    }

    private float adjustment = 0;
    private final List<ModuleRect> searchResults = new ArrayList<>();

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (ModuleCollection.reloadModules || moduleRects == null) {
            if (moduleRects == null) {
                moduleRects = new HashMap<>();
            } else moduleRects.clear();
            for (Category category : Category.values()) {
                ArrayList<ModuleRect> modules = new ArrayList<>();
                for (Module module : Merguru.INSTANCE.getModuleCollection().getModulesInCategory(category)) {
                    modules.add(new ModuleRect(module));
                }

                moduleRects.put(category, modules);
            }
            moduleRects.forEach((cat, list) -> list.forEach(ModuleRect::initGui));
            modpanel.refreshSettingMap();
            ModuleCollection.reloadModules = false;
            return;
        }

        typing = modpanel.isTyping() || (Merguru.INSTANCE.getSideGui().isFocused() && Merguru.INSTANCE.getSideGui().isTyping()) || Merguru.INSTANCE.getSearchBar().isTyping();

        if (ClickGUIMod.walk.isEnabled() && !typing) {
            InventoryMove.updateStates();
        }

        boolean focusedConfigGui = Merguru.INSTANCE.getSideGui().isFocused();
        int fakeMouseX = focusedConfigGui ? 0 : mouseX, fakeMouseY = focusedConfigGui ? 0 : mouseY;

        adjustment = 0;

        drag.onDraw(fakeMouseX, fakeMouseY);
        float x = drag.getX(), y = drag.getY();


        if (!openingAnimation.isDone()) {
            x -= width + rectWidth / 2f;
            x += (width + rectWidth / 2f) * openingAnimation.getOutput().floatValue();
        } else if (openingAnimation.getDirection().equals(Direction.BACKWARDS)) {
            mc.displayGuiScreen(null);
            return;
        }


        RoundedUtil.drawRound(x, y, rectWidth, rectHeight, 10, backgroundColor);

        float catWidth = (100 - (55 * expandedAnimation.getOutput().floatValue()));
        boolean hoveringCat = HoveringUtil.isHovering(x, y, catWidth, rectHeight, fakeMouseX, fakeMouseY);
        boolean searching = Merguru.INSTANCE.getSearchBar().isFocused();
        if (expandedAnimation.isDone()) {
            expandedAnimation.setDirection(hoveringCat && !searching ? Direction.BACKWARDS : Direction.FORWARDS);
        }


        RoundedUtil.drawRound(x, y, (float) (100 - (55 * expandedAnimation.getOutput().floatValue())), rectHeight, 10, categoryColor);
        //   RenderUtil.renderRoundedRect(x, y, (float) (100 - (55 * expandedAnimation.getOutput().floatValue())), rectHeight, 10, categoryColor.getRGB());


        adjustWidth(55 - (55 * expandedAnimation.getOutput().floatValue()));

        StencilUtil.initStencilToWrite();
        Gui.drawRect2(x, y, (float) (100 - (55 * expandedAnimation.getOutput().floatValue())), rectHeight, -1);
        StencilUtil.readStencilBuffer(1);


        GL11.glEnable(GL11.GL_BLEND);
        mc.getTextureManager().bindTexture(new ResourceLocation("Merguru/modernlogo.png"));
        Gui.drawModalRectWithCustomSizedTexture((float) (x + 9 + (3 * expandedAnimation.getOutput().floatValue())), y + 6, 0, 0, 20.5f, 20.5f, 20.5f, 20.5f);
        GL11.glDisable(GL11.GL_BLEND);

        Gui.drawRect2(x + 10, y + 35, 80 - (55 * expandedAnimation.getOutput().floatValue()), 1, lighterGray.getRGB());


        float xAdjust = 10 * expandedAnimation.getOutput().floatValue();
        FontUtil.tenacityFont20.drawString("Meguru", x + 35 + xAdjust, y + 13, -1);

        FontUtil.tenacityFont14.drawString(Merguru.VERSION, x + 41 + FontUtil.tenacityFont18.getStringWidth("Meguru") + xAdjust, y + 15.5f,
                new Color(98, 98, 98));


        int spacing = 0;
        for (Component category : categories) {
            category.x = x + 8 + (4 * expandedAnimation.getOutput().floatValue());
            category.y = y + 50 + spacing;
            CategoryButton currentCatego = ((CategoryButton) category);
            currentCatego.expandAnimation = expandedAnimation;
            currentCatego.currentCategory = searching ? null : currentCategory;
            category.drawScreen(fakeMouseX, fakeMouseY);
            spacing += 30;
        }

        StencilUtil.uninitStencilBuffer();


        float recWidth = 100 - (55 * expandedAnimation.getOutput().floatValue());
        StencilUtil.initStencilToWrite();
        RoundedUtil.drawRound(x, y, rectWidth, rectHeight, 10, backgroundColor);
        StencilUtil.readStencilBuffer(1);

        /*+ ((rectWidth - 50) * searchingAnimation.getOutput().floatValue())*/
        modpanel.x = x + recWidth + 10;
        modpanel.y = y + 20;
        modpanel.bigRecty = y;
        modpanel.modules = getModuleRects(currentCategory);
        modpanel.currentCategory = searching ? null : currentCategory;
        modpanel.expandAnim = expandedAnimation;
        modpanel.drawScreen(fakeMouseX, fakeMouseY);

        StencilUtil.uninitStencilBuffer();


        SideGUI sideGUI = Merguru.INSTANCE.getSideGui();
        sideGUI.getOpenAnimation().setDirection(openingAnimation.getDirection());
        sideGUI.drawScreen(mouseX, mouseY);

        SearchBar searchBar = Merguru.INSTANCE.getSearchBar();
        searchBar.setAlpha(openingAnimation.getOutput().floatValue() * (1 - sideGUI.getClickAnimation().getOutput().floatValue()));
        searchBar.drawScreen(fakeMouseX, fakeMouseY);

        for (ClickCircle clickCircle : circleClicks) {
            clickCircle.drawScreen(fakeMouseX, fakeMouseY);
        }

        rectWidth = 370 + adjustment;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float rectWidth = 400;
        double x = drag.getX(), y = drag.getY();
        final boolean canDrag = HoveringUtil.isHovering((float) x, (float) y, rectWidth, 20f, mouseX, mouseY);

        if (!Merguru.INSTANCE.getSideGui().isFocused()) {
            drag.onClick(mouseX, mouseY, mouseButton, canDrag);


            circleClicks.removeIf(clickCircle1 -> clickCircle1.fadeAnimation.isDone() && clickCircle1.fadeAnimation.getDirection().equals(Direction.BACKWARDS));
            ClickCircle clickCircle = new ClickCircle();
            clickCircle.x = mouseX;
            clickCircle.y = mouseY;
            circleClicks.add(clickCircle);


            for (Component category : categories) {
                category.mouseClicked(mouseX, mouseY, mouseButton);
                if (category.hovering) {
                    currentCategory = ((CategoryButton) category).category;
                    return;
                }
            }
            modpanel.mouseClicked(mouseX, mouseY, mouseButton);
        }
        Merguru.INSTANCE.getSideGui().mouseClicked(mouseX, mouseY, mouseButton);
        Merguru.INSTANCE.getSearchBar().mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (!Merguru.INSTANCE.getSideGui().isFocused()) {
            drag.onRelease(state);
            modpanel.mouseReleased(mouseX, mouseY, state);
        }
        Merguru.INSTANCE.getSideGui().mouseReleased(mouseX, mouseY, state);
        Merguru.INSTANCE.getSearchBar().mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }


    public void adjustWidth(float adjustment) {
        this.adjustment += adjustment;
    }

    private final List<String> searchTerms = new ArrayList<>();
    private String searchText;

    public List<ModuleRect> getModuleRects(Category category) {
        if (!Merguru.INSTANCE.getSearchBar().isFocused()) {
            return moduleRects.get(category);
        }

        String search = Merguru.INSTANCE.getSearchBar().getSearchField().getText();

        if (search.equals(searchText)) {
            return searchResults;
        } else {
            searchText = search;
        }

        List<ModuleRect> moduleRects1 = moduleRects.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        searchResults.clear();
        moduleRects1.forEach(moduleRect -> {
            searchTerms.clear();
            Module module = moduleRect.module;

            searchTerms.add(module.getName());
            searchTerms.add(module.getCategory().name);
            if (!module.getAuthor().isEmpty()) {
                searchTerms.add(module.getAuthor());
            }
            for (Setting setting : module.getSettingsList()) {
                searchTerms.add(setting.name);
            }

            moduleRect.setSearchScore(FuzzySearch.extractOne(search, searchTerms).getScore());
        });

        searchResults.addAll(moduleRects1.stream().filter(moduleRect -> moduleRect.getSearchScore() > 60)
                .sorted(Comparator.comparingInt(ModuleRect::getSearchScore).reversed()).collect(Collectors.toList()));

        return searchResults;
    }

}
