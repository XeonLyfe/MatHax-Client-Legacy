package mathax.legacy.client.gui.screens.settings;

import mathax.legacy.client.gui.GuiTheme;
import mathax.legacy.client.gui.WindowScreen;
import mathax.legacy.client.gui.utils.Cell;
import mathax.legacy.client.gui.widgets.WWidget;
import mathax.legacy.client.gui.widgets.containers.WSection;
import mathax.legacy.client.gui.widgets.containers.WTable;
import mathax.legacy.client.gui.widgets.containers.WVerticalList;
import mathax.legacy.client.gui.widgets.input.WTextBox;
import mathax.legacy.client.gui.widgets.pressable.WCheckbox;
import mathax.legacy.client.settings.EntityTypeListSetting;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.entity.EntityUtils;
import mathax.legacy.client.utils.misc.Names;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class EntityTypeListSettingScreen extends WindowScreen {
    private final EntityTypeListSetting setting;

    private WVerticalList list;
    private final WTextBox filter;

    private String filterText = "";

    private WSection animals, waterAnimals, monsters, ambient, misc;
    private WTable animalsT, waterAnimalsT, monstersT, ambientT, miscT;
    int hasAnimal = 0, hasWaterAnimal = 0, hasMonster = 0, hasAmbient = 0, hasMisc = 0;

    public EntityTypeListSettingScreen(GuiTheme theme, EntityTypeListSetting setting) {
        super(theme, "Select entities");
        this.setting = setting;

        // Filter
        filter = super.add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            list.clear();
            initWidgets();
        };

        list = super.add(theme.verticalList()).expandX().widget();

    }

    @Override
    public <W extends WWidget> Cell<W> add(W widget) {
        return list.add(widget);
    }

    @Override
    public void initWidgets() {
        hasAnimal = hasWaterAnimal = hasMonster = hasAmbient = hasMisc = 0;

        for (EntityType<?> entityType : setting.get().keySet()) {
            if (!setting.get().getBoolean(entityType)) continue;

            if (!setting.onlyAttackable || EntityUtils.isAttackable(entityType)) {
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> hasAnimal++;
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE -> hasWaterAnimal++;
                    case MONSTER -> hasMonster++;
                    case AMBIENT -> hasAmbient++;
                    case MISC -> hasMisc++;
                }
            }
        }

        boolean first = animals == null;

        // Animals
        List<EntityType<?>> animalsE = new ArrayList<>();
        WCheckbox animalsC = theme.checkbox(hasAnimal > 0);

        animals = theme.section("Animals", animals != null && animals.isExpanded(), animalsC);
        animalsC.action = () -> tableChecked(animalsE, animalsC.checked);

        Cell<WSection> animalsCell = add(animals).expandX();
        animalsT = animals.add(theme.table()).expandX().widget();

        // Water animals
        List<EntityType<?>> waterAnimalsE = new ArrayList<>();
        WCheckbox waterAnimalsC = theme.checkbox(hasWaterAnimal > 0);

        waterAnimals = theme.section("Water Animals", waterAnimals != null && waterAnimals.isExpanded(), waterAnimalsC);
        waterAnimalsC.action = () -> tableChecked(waterAnimalsE, waterAnimalsC.checked);

        Cell<WSection> waterAnimalsCell = add(waterAnimals).expandX();
        waterAnimalsT = waterAnimals.add(theme.table()).expandX().widget();

        // Monsters
        List<EntityType<?>> monstersE = new ArrayList<>();
        WCheckbox monstersC = theme.checkbox(hasMonster > 0);

        monsters = theme.section("Monsters", monsters != null && monsters.isExpanded(), monstersC);
        monstersC.action = () -> tableChecked(monstersE, monstersC.checked);

        Cell<WSection> monstersCell = add(monsters).expandX();
        monstersT = monsters.add(theme.table()).expandX().widget();

        // Ambient
        List<EntityType<?>> ambientE = new ArrayList<>();
        WCheckbox ambientC = theme.checkbox(hasAmbient > 0);

        ambient = theme.section("Ambient", ambient != null && ambient.isExpanded(), ambientC);
        ambientC.action = () -> tableChecked(ambientE, ambientC.checked);

        Cell<WSection> ambientCell = add(ambient).expandX();
        ambientT = ambient.add(theme.table()).expandX().widget();

        // Misc
        List<EntityType<?>> miscE = new ArrayList<>();
        WCheckbox miscC = theme.checkbox(hasMisc > 0);

        misc = theme.section("Misc", misc != null && misc.isExpanded(), miscC);
        miscC.action = () -> tableChecked(miscE, miscC.checked);

        Cell<WSection> miscCell = add(misc).expandX();
        miscT = misc.add(theme.table()).expandX().widget();

        Consumer<EntityType<?>> entityTypeForEach = entityType -> {
            if (!setting.onlyAttackable || EntityUtils.isAttackable(entityType)) {
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> {
                        animalsE.add(entityType);
                        addEntityType(animalsT, animalsC, entityType);
                    }
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE -> {
                        waterAnimalsE.add(entityType);
                        addEntityType(waterAnimalsT, waterAnimalsC, entityType);
                    }
                    case MONSTER -> {
                        monstersE.add(entityType);
                        addEntityType(monstersT, monstersC, entityType);
                    }
                    case AMBIENT -> {
                        ambientE.add(entityType);
                        addEntityType(ambientT, ambientC, entityType);
                    }
                    case MISC -> {
                        miscE.add(entityType);
                        addEntityType(miscT, miscC, entityType);
                    }
                }
            }
        };

        // Sort all entities
        if (filterText.isEmpty()) {
            Registry.ENTITY_TYPE.forEach(entityTypeForEach);
        } else {
            List<Pair<EntityType<?>, Integer>> entities = new ArrayList<>();
            Registry.ENTITY_TYPE.forEach(entity -> {
                int words = Utils.search(Names.get(entity), filterText);
                if (words > 0) entities.add(new Pair<>(entity, words));
            });
            entities.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<EntityType<?>, Integer> pair : entities) entityTypeForEach.accept(pair.getLeft());
        }

        if (animalsT.cells.size() == 0) list.cells.remove(animalsCell);
        if (waterAnimalsT.cells.size() == 0) list.cells.remove(waterAnimalsCell);
        if (monstersT.cells.size() == 0) list.cells.remove(monstersCell);
        if (ambientT.cells.size() == 0) list.cells.remove(ambientCell);
        if (miscT.cells.size() == 0) list.cells.remove(miscCell);

        if (first) {
            int totalCount = (hasWaterAnimal + waterAnimals.cells.size() + monsters.cells.size() + ambient.cells.size() + misc.cells.size()) / 2;

            if (totalCount <= 20) {
                if (animalsT.cells.size() > 0) animals.setExpanded(true);
                if (waterAnimalsT.cells.size() > 0) waterAnimals.setExpanded(true);
                if (monstersT.cells.size() > 0) monsters.setExpanded(true);
                if (ambientT.cells.size() > 0) ambient.setExpanded(true);
                if (miscT.cells.size() > 0) misc.setExpanded(true);
            } else {
                if (animalsT.cells.size() > 0) animals.setExpanded(false);
                if (waterAnimalsT.cells.size() > 0) waterAnimals.setExpanded(false);
                if (monstersT.cells.size() > 0) monsters.setExpanded(false);
                if (ambientT.cells.size() > 0) ambient.setExpanded(false);
                if (miscT.cells.size() > 0) misc.setExpanded(false);
            }
        }
    }

    private void tableChecked(List<EntityType<?>> entityTypes, boolean checked) {
        boolean changed = false;

        for (EntityType<?> entityType : entityTypes) {
            if (checked) {
                setting.get().put(entityType, true);
                changed = true;
            } else {
                if (setting.get().removeBoolean(entityType)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            list.clear();
            initWidgets();
            setting.changed();
        }
    }

    private void addEntityType(WTable table, WCheckbox tableCheckbox, EntityType<?> entityType) {
        table.add(theme.label(Names.get(entityType)));

        WCheckbox a = table.add(theme.checkbox(setting.get().getBoolean(entityType))).expandCellX().right().widget();
        a.action = () -> {
            if (a.checked) {
                setting.get().put(entityType, true);
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> {
                        if (hasAnimal == 0) tableCheckbox.checked = true;
                        hasAnimal++;
                    }
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE -> {
                        if (hasWaterAnimal == 0) tableCheckbox.checked = true;
                        hasWaterAnimal++;
                    }
                    case MONSTER -> {
                        if (hasMonster == 0) tableCheckbox.checked = true;
                        hasMonster++;
                    }
                    case AMBIENT -> {
                        if (hasAmbient == 0) tableCheckbox.checked = true;
                        hasAmbient++;
                    }
                    case MISC -> {
                        if (hasMisc == 0) tableCheckbox.checked = true;
                        hasMisc++;
                    }
                }
            } else {
                if (setting.get().removeBoolean(entityType)) {
                    switch (entityType.getSpawnGroup()) {
                        case CREATURE -> {
                            hasAnimal--;
                            if (hasAnimal == 0) tableCheckbox.checked = false;
                        }
                        case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE -> {
                            hasWaterAnimal--;
                            if (hasWaterAnimal == 0) tableCheckbox.checked = false;
                        }
                        case MONSTER -> {
                            hasMonster--;
                            if (hasMonster == 0) tableCheckbox.checked = false;
                        }
                        case AMBIENT -> {
                            hasAmbient--;
                            if (hasAmbient == 0) tableCheckbox.checked = false;
                        }
                        case MISC -> {
                            hasMisc--;
                            if (hasMisc == 0) tableCheckbox.checked = false;
                        }
                    }
                }
            }

            setting.changed();
        };

        table.row();
    }
}
