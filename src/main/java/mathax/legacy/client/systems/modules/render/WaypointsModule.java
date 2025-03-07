package mathax.legacy.client.systems.modules.render;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import mathax.legacy.client.events.game.OpenScreenEvent;
import mathax.legacy.client.gui.GuiTheme;
import mathax.legacy.client.gui.WindowScreen;
import mathax.legacy.client.gui.renderer.GuiRenderer;
import mathax.legacy.client.gui.screens.settings.ColorSettingScreen;
import mathax.legacy.client.gui.widgets.WLabel;
import mathax.legacy.client.gui.widgets.WWidget;
import mathax.legacy.client.gui.widgets.containers.WHorizontalList;
import mathax.legacy.client.gui.widgets.containers.WTable;
import mathax.legacy.client.gui.widgets.input.WDoubleEdit;
import mathax.legacy.client.gui.widgets.input.WDropdown;
import mathax.legacy.client.gui.widgets.input.WIntEdit;
import mathax.legacy.client.gui.widgets.input.WTextBox;
import mathax.legacy.client.gui.widgets.pressable.WButton;
import mathax.legacy.client.gui.widgets.pressable.WCheckbox;
import mathax.legacy.client.gui.widgets.pressable.WMinus;
import mathax.legacy.client.systems.waypoints.Waypoint;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.player.PlayerUtils;
import mathax.legacy.client.systems.waypoints.Waypoints;
import mathax.legacy.client.utils.render.color.Color;
import mathax.legacy.client.utils.world.Dimension;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Items;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ListIterator;

import static mathax.legacy.client.utils.player.ChatUtils.formatCoords;

public class WaypointsModule extends Module {
    private static final Color GRAY = new Color(200, 200, 200);

    private final SettingGroup sgDeathPosition = settings.createGroup("Death Position");

    private final Setting<Integer> maxDeathPositions = sgDeathPosition.add(new IntSetting.Builder()
        .name("max-death-positions")
        .description("The amount of death positions to save, 0 to disable.")
        .min(0)
        .sliderMin(0)
        .sliderMax(20)
        .defaultValue(0)
        .onChanged(this::cleanDeathWPs)
        .build()
    );

    private final Setting<Boolean> dpChat = sgDeathPosition.add(new BoolSetting.Builder()
        .name("chat")
        .description("Send a chat message with your position once you die.")
        .defaultValue(false)
        .build()
    );

    public WaypointsModule() {
        super(Categories.Render, Items.BEACON, "waypoints", "Allows you to create waypoints.");
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        if (!event.isCancelled()) addDeath(mc.player.getPos());
    }

    public void addDeath(Vec3d deathPos) {
        String time = dateFormat.format(new Date());
        if (dpChat.get()) {
            BaseText text = new LiteralText("Died at ");
            text.append(formatCoords(deathPos));
            text.append(String.format(" on %s.", time));
            info(text);
        }

        // Create waypoint
        if (maxDeathPositions.get() > 0) {
            Waypoint waypoint = new Waypoint();
            waypoint.name = "Death " + time;
            waypoint.icon = "skull";
            waypoint.scale = 2;
            waypoint.x = (int) deathPos.x;
            waypoint.y = (int) deathPos.y + 2;
            waypoint.z = (int) deathPos.z;
            waypoint.maxVisibleDistance = Integer.MAX_VALUE;
            waypoint.actualDimension = PlayerUtils.getDimension();

            switch (waypoint.actualDimension) {
                case Overworld:
                    waypoint.overworld = true;
                    break;
                case Nether:
                    waypoint.nether = true;
                    break;
                case End:
                    waypoint.end = true;
                    break;
            }

            Waypoints.get().add(waypoint);
        }

        cleanDeathWPs(maxDeathPositions.get());
    }

    private void cleanDeathWPs(int max) {
        int oldWpC = 0;

        ListIterator<Waypoint> wps = Waypoints.get().iteratorReverse();
        while (wps.hasPrevious()) {
            Waypoint wp = wps.previous();
            if (wp.name.startsWith("Death ") && "skull".equals(wp.icon)) {
                oldWpC++;
                if (oldWpC > max)
                    Waypoints.get().remove(wp);
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (!Utils.canUpdate()) return theme.label("You need to be in a world.");

        WTable table = theme.table();
        fillTable(theme, table);
        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        // Create
        WButton create = table.add(theme.button("Create")).expandX().widget();
        create.action = () -> mc.setScreen(new EditWaypointScreen(theme, null, () -> {
            table.clear();
            fillTable(theme, table);
        }));
        table.row();

        // WaypointsModule
        for (Waypoint waypoint : Waypoints.get()) {
            // Icon
            table.add(new WIcon(waypoint));

            // Name
            WLabel name = table.add(theme.label(waypoint.name)).expandCellX().widget();
            boolean goodDimension = false;
            Dimension dimension = PlayerUtils.getDimension();
            if (waypoint.overworld && dimension == Dimension.Overworld) goodDimension = true;
            else if (waypoint.nether && dimension == Dimension.Nether) goodDimension = true;
            else if (waypoint.end && dimension == Dimension.End) goodDimension = true;
            if (!goodDimension) name.color = GRAY;

            // Visible
            WCheckbox visible = table.add(theme.checkbox(waypoint.visible)).widget();
            visible.action = () -> {
                waypoint.visible = visible.checked;
                Waypoints.get().save();
            };

            // Edit
            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> mc.setScreen(new EditWaypointScreen(theme, waypoint, null));

            // Remove
            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                Waypoints.get().remove(waypoint);

                table.clear();
                fillTable(theme, table);
            };

            // Goto
            if (waypoint.actualDimension == dimension) {
                WButton gotoB = table.add(theme.button("Goto")).widget();
                gotoB.action = () -> {
                    if (mc.player == null || mc.world == null) return;
                    IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                    if (baritone.getPathingBehavior().isPathing()) baritone.getPathingBehavior().cancelEverything();
                    Vec3d vec = Waypoints.get().getCoords(waypoint);
                    BlockPos pos = new BlockPos(vec.x, vec.y, vec.z);
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(pos));
                };
            }

            table.row();
        }
    }

    private class EditWaypointScreen extends WindowScreen {
        private final Waypoint waypoint;
        private final boolean newWaypoint;
        private final Runnable action;

        public EditWaypointScreen(GuiTheme theme, Waypoint waypoint, Runnable action) {
            super(theme, waypoint == null ? "New Waypoint" : "Edit Waypoint");

            this.newWaypoint = waypoint == null;
            this.waypoint = newWaypoint ? new Waypoint() : waypoint;
            this.action = action;

            this.waypoint.validateIcon();

            if (newWaypoint) {
                this.waypoint.x = (int) mc.player.getX();
                this.waypoint.y = (int) mc.player.getY() + 2;
                this.waypoint.z = (int) mc.player.getZ();

                this.waypoint.actualDimension = PlayerUtils.getDimension();

                switch (PlayerUtils.getDimension()) {
                    case Overworld -> this.waypoint.overworld = true;
                    case Nether -> this.waypoint.nether = true;
                    case End -> this.waypoint.end = true;
                }
            }

            initWidgets();
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().widget();

            // Name
            table.add(theme.label("Name:"));
            WTextBox name = table.add(theme.textBox(waypoint.name)).minWidth(400).expandX().widget();
            name.action = () -> waypoint.name = name.get().trim();
            table.row();

            // Icon
            table.add(theme.label("Icon:"));
            WHorizontalList list = table.add(theme.horizontalList()).widget();
            list.add(theme.button("<")).widget().action = waypoint::prevIcon;
            list.add(new WIcon(waypoint));
            list.add(theme.button(">")).widget().action = waypoint::nextIcon;
            table.row();

            // Color:
            table.add(theme.label("Color:"));
            list = table.add(theme.horizontalList()).widget();
            list.add(theme.quad(waypoint.color));
            list.add(theme.button(GuiRenderer.EDIT)).widget().action = () -> mc.setScreen(new ColorSettingScreen(theme, new ColorSetting("", "", waypoint.color, color -> waypoint.color.set(color), null, null)));
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // X
            table.add(theme.label("X:"));
            WIntEdit x = theme.intEdit(waypoint.x, 0, 0);
            x.noSlider = false;
            x.action = () -> waypoint.x = x.get();
            table.add(x).expandX();
            table.row();

            // Y
            table.add(theme.label("Y:"));
            WIntEdit y = theme.intEdit(waypoint.y, 0, 0);
            y.noSlider = false;
            y.actionOnRelease = () -> {
                if (y.get() < 0) y.set(0);
                else if (y.get() > 255) y.set(255);

                waypoint.y = y.get();
            };
            table.add(y).expandX();
            table.row();

            // Z
            table.add(theme.label("Z:"));
            WIntEdit z = theme.intEdit(waypoint.z, 0, 0);
            z.action = () -> waypoint.z = z.get();
            table.add(z).expandX();
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Visible
            table.add(theme.label("Visible:"));
            WCheckbox visible = table.add(theme.checkbox(waypoint.visible)).widget();
            visible.action = () -> waypoint.visible = visible.checked;
            table.row();

            // Max visible distance
            table.add(theme.label("Max Visible Distance"));
            WIntEdit maxVisibleDist = table.add(theme.intEdit(waypoint.maxVisibleDistance, 0, 10000)).expandX().widget();
            maxVisibleDist.action = () -> waypoint.maxVisibleDistance = maxVisibleDist.get();
            table.row();

            // Scale
            table.add(theme.label("Scale:"));
            WDoubleEdit scale = table.add(theme.doubleEdit(waypoint.scale, 0, 4)).expandX().widget();
            scale.action = () -> waypoint.scale = scale.get();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Dimension
            table.add(theme.label("Actual Dimension:"));
            WDropdown<Dimension> dimensionDropdown = table.add(theme.dropdown(waypoint.actualDimension)).widget();
            dimensionDropdown.action = () -> waypoint.actualDimension = dimensionDropdown.get();
            table.row();

            // Overworld
            table.add(theme.label("Visible in Overworld:"));
            WCheckbox overworld = table.add(theme.checkbox(waypoint.overworld)).widget();
            overworld.action = () -> waypoint.overworld = overworld.checked;
            table.row();

            // Nether
            table.add(theme.label("Visible in Nether:"));
            WCheckbox nether = table.add(theme.checkbox(waypoint.nether)).widget();
            nether.action = () -> waypoint.nether = nether.checked;
            table.row();

            // End
            table.add(theme.label("Visible in End:"));
            WCheckbox end = table.add(theme.checkbox(waypoint.end)).widget();
            end.action = () -> waypoint.end = end.checked;
            table.row();

            // Save
            WButton save = table.add(theme.button("Save")).expandX().widget();
            save.action = () -> {
                if (newWaypoint) Waypoints.get().add(waypoint);
                else Waypoints.get().save();

                onClose();
            };

            enterAction = save.action;
        }

        @Override
        protected void onClosed() {
            if (action != null) action.run();
        }
    }

    private static class WIcon extends WWidget {
        private final Waypoint waypoint;

        public WIcon(Waypoint waypoint) {
            this.waypoint = waypoint;
        }

        @Override
        protected void onCalculateSize() {
            double s = theme.scale(32);

            width = s;
            height = s;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.post(() -> waypoint.renderIcon(x, y, 1, width));
        }
    }
}
