package mathax.legacy.client.gui.screens.clickgui;

import mathax.legacy.client.gui.GuiTheme;
import mathax.legacy.client.gui.tabs.TabScreen;
import mathax.legacy.client.gui.tabs.Tabs;
import mathax.legacy.client.gui.utils.Cell;
import mathax.legacy.client.gui.widgets.containers.WContainer;
import mathax.legacy.client.gui.widgets.containers.WSection;
import mathax.legacy.client.gui.widgets.containers.WVerticalList;
import mathax.legacy.client.gui.widgets.containers.WWindow;
import mathax.legacy.client.gui.widgets.input.WTextBox;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.systems.modules.Category;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.misc.NbtUtils;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Pair;

import java.util.List;

public class ModulesScreen extends TabScreen {

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().get(0));
    }

    @Override
    public void initWidgets() {
        add(new WCategoryController());

        // Help
        WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
        help.add(theme.label("Left click - Toggle module"));
        help.add(theme.label("Right click - Open module settings"));
    }

    // Category

    protected void createCategory(WContainer c, Category category) {
        WWindow w = theme.window(category.name);
        w.id = category.name;
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(category.icon)).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.spacing = 0;

        for (Module module : Modules.get().getGroup(category)) {
            w.add(theme.module(module)).expandX().widget().tooltip = module.description;
        }
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (!text.isEmpty()) {
            // Titles
            List<Pair<Module, Integer>> modules = Modules.get().searchTitles(text);

            if (modules.size() > 0) {
                WSection section = w.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;

                for (Pair<Module, Integer> pair : modules) {
                    section.add(theme.module(pair.getLeft())).expandX();
                }
            }

            // Settings
            modules = Modules.get().searchSettingTitles(text);

            if (modules.size() > 0) {
                WSection section = w.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;

                for (Pair<Module, Integer> pair : modules) {
                    section.add(theme.module(pair.getLeft())).expandX();
                }
            }
        }
    }

    protected void createSearch(WContainer c) {
        WWindow w = theme.window("Search");
        w.id = "search";

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.COMPASS.getDefaultStack())).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = false;
        w.view.maxHeight -= 20;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("")).minWidth(140).expandX().widget();
        text.setFocused(true);
        text.action = () -> {
            l.clear();
            createSearchW(l, text.get());
        };

        w.add(l).expandX();
        createSearchW(l, text.get());
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        NbtCompound clipboard = NbtUtils.fromClipboard(Modules.get().toTag());

        if (clipboard != null) {
            Modules.get().fromTag(clipboard);
            return true;
        }

        return false;
    }

    @Override
    public void reload() {}

    // Stuff

    protected class WCategoryController extends WContainer {
        @Override
        public void init() {
            for (Category category : Modules.loopCategories()) {
                createCategory(this, category);
            }

            createSearch(this);
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double pad = theme.scale(4);
            double h = theme.scale(40);

            double x = this.x + pad;
            double y = this.y;

            for (Cell<?> cell : cells) {
                double windowWidth = Utils.getWindowWidth();
                double windowHeight = Utils.getWindowHeight();

                if (x + cell.width > windowWidth) {
                    x = x + pad;
                    y += h;
                }

                if (x > windowWidth) {
                    x = windowWidth / 2.0 - cell.width / 2.0;
                    if (x < 0) x = 0;
                }
                if (y > windowHeight) {
                    y = windowHeight / 2.0 - cell.height / 2.0;
                    if (y < 0) y = 0;
                }

                cell.x = x;
                cell.y = y;

                cell.width = cell.widget().width;
                cell.height = cell.widget().height;

                cell.alignWidget();

                x += cell.width + pad;
            }
        }
    }
}
