package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.render.Render3DEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.renderer.ShapeMode;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.player.FindItemResult;
import mathax.legacy.client.utils.player.InvUtils;
import mathax.legacy.client.utils.player.PlayerUtils;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.utils.world.BlockUtils;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SelfTrap extends Module {
    private final List<BlockPos> placePositions = new ArrayList<>();

    private boolean placed;

    private int delay;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>()
        .name("top-mode")
        .description("Which positions to place on your top half.")
        .defaultValue(TopMode.Top)
        .build()
    );

    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
        .name("bottom-mode")
        .description("Which positions to place on your bottom half.")
        .defaultValue(BottomMode.None)
        .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("How many ticks between block placements.")
        .defaultValue(1)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you on the block you are standing on before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
        .name("turn-off")
        .description("Turns off after placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
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
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    public SelfTrap(){
        super(Categories.Combat, Items.OBSIDIAN, "self-trap", "Places obsidian above your head.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
        placed = false;

        if (center.get()) PlayerUtils.centerPlayer();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (turnOff.get() && ((placed && placePositions.isEmpty()) || !obsidian.found())) {
            toggle();
            return;
        }

        if (!obsidian.found()) {
            placePositions.clear();
            return;
        }

        findPlacePos();

        if (delay >= delaySetting.get() && placePositions.size() > 0) {
            BlockPos blockPos = placePositions.get(placePositions.size() - 1);

            if (BlockUtils.place(blockPos, obsidian, rotate.get(), 50)) {
                placePositions.remove(blockPos);
                placed = true;
            }

            delay = 0;
        } else delay++;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions) event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void findPlacePos() {
        placePositions.clear();
        BlockPos pos = mc.player.getBlockPos();

        switch (topPlacement.get()) {
            case Full:
                add(pos.add(0, 2, 0));
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));
                break;
            case Top:
                add(pos.add(0, 2, 0));
                break;
            case AntiFacePlace:
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));

        }

        if (bottomPlacement.get() == BottomMode.Single) add(pos.add(0, -1, 0));
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && mc.world.getBlockState(blockPos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent())) placePositions.add(blockPos);
    }

    public enum TopMode {
        AntiFacePlace,
        Full,
        Top,
        None
    }

    public enum BottomMode {
        Single,
        None
    }
}
