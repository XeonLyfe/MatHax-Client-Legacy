package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.render.Render3DEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.renderer.ShapeMode;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.entity.EntityUtils;
import mathax.legacy.client.utils.entity.SortPriority;
import mathax.legacy.client.utils.entity.TargetUtils;
import mathax.legacy.client.utils.player.FindItemResult;
import mathax.legacy.client.utils.player.InvUtils;
import mathax.legacy.client.utils.render.color.Color;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.utils.world.BlockUtils;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AutoTrap extends Module {
    private final List<BlockPos> placePositions = new ArrayList<>();

    private PlayerEntity target;

    private boolean placed;

    private int timer;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("target-range")
        .description("The range players can be targeted.")
        .defaultValue(4)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("How many ticks between block placements.")
        .defaultValue(1)
        .build()
    );

    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>()
        .name("top-blocks")
        .description("Which blocks to place on the top half of the target.")
        .defaultValue(TopMode.Full)
        .build()
    );

    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
        .name("bottom-blocks")
        .description("Which blocks to place on the bottom half of the target.")
        .defaultValue(BottomMode.Platform)
        .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Turns off after placing all blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards blocks when placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    private final Setting<SettingColor> nextSideColor = sgRender.add(new ColorSetting.Builder()
        .name("next-side-color")
        .description("The side color of the next block to be placed.")
        .defaultValue(new SettingColor(150, 150, 75, 75))
        .build()
    );

    private final Setting<SettingColor> nextLineColor = sgRender.add(new ColorSetting.Builder()
        .name("next-line-color")
        .description("The line color of the next block to be placed.")
        .defaultValue(new SettingColor(150, 150, 75))
        .build()
    );

    public AutoTrap() {
        super(Categories.Combat, Items.OBSIDIAN, "auto-trap", "Traps people in an obsidian box to prevent them from moving.");
    }

    @Override
    public void onActivate() {
        target = null;
        placePositions.clear();
        timer = 0;
        placed = false;
    }

    @Override
    public void onDeactivate() {
        placePositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (selfToggle.get() && placed && placePositions.isEmpty()) {
            placed = false;
            toggle();
            return;
        }

        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (!obsidian.isHotbar() && !obsidian.isOffhand()) {
            placePositions.clear();
            placed = false;
            return;
        }

        if (TargetUtils.isBadTarget(target, range.get())) target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;

        fillPlaceArray(target);

        if (timer >= delay.get() && placePositions.size() > 0) {
            BlockPos blockPos = placePositions.get(placePositions.size() - 1);

            if (BlockUtils.place(blockPos, obsidian, rotate.get(), 50, true)) {
                placePositions.remove(blockPos);
                placed = true;
            }

            timer = 0;
        } else {
            timer++;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;

        for (BlockPos pos : placePositions) {
            boolean isFirst = pos.equals(placePositions.get(placePositions.size() - 1));

            Color side = isFirst ? nextSideColor.get() : sideColor.get();
            Color line = isFirst ? nextSideColor.get() : sideColor.get();

            event.renderer.box(pos, side, line, shapeMode.get(), 0);
        }
    }

    private void fillPlaceArray(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();

        switch (topPlacement.get()) {
            case Full -> {
                add(targetPos.add(0, 2, 0));
                add(targetPos.add(1, 1, 0));
                add(targetPos.add(-1, 1, 0));
                add(targetPos.add(0, 1, 1));
                add(targetPos.add(0, 1, -1));
            }
            case Face -> {
                add(targetPos.add(1, 1, 0));
                add(targetPos.add(-1, 1, 0));
                add(targetPos.add(0, 1, 1));
                add(targetPos.add(0, 1, -1));
            }
            case Top -> add(targetPos.add(0, 2, 0));
        }

        switch (bottomPlacement.get()) {
            case Platform -> {
                add(targetPos.add(0, -1, 0));
                add(targetPos.add(1, -1, 0));
                add(targetPos.add(0, -1, 0));
                add(targetPos.add(0, -1, 1));
                add(targetPos.add(0, -1, -1));
            }
            case Full -> {
                add(targetPos.add(1, -1, 0));
                add(targetPos.add(-1, -1, 0));
                add(targetPos.add(0, -1, -1));
                add(targetPos.add(0, -1, 1));
                add(targetPos.add(1, 0, 0));
                add(targetPos.add(-1, 0, 0));
                add(targetPos.add(0, 0, -1));
                add(targetPos.add(0, 0, 1));
            }
            case Single -> add(targetPos.add(0, -1, 0));
        }
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && BlockUtils.canPlace(blockPos)) placePositions.add(blockPos);
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum TopMode {
        Full,
        Top,
        Face,
        None
    }

    public enum BottomMode {
        Single,
        Platform,
        Full,
        None
    }
}
