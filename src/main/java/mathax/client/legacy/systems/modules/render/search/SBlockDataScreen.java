package mathax.client.legacy.systems.modules.render.search;

import mathax.client.legacy.gui.GuiTheme;
import mathax.client.legacy.gui.WindowScreen;
import mathax.client.legacy.settings.*;
import mathax.client.legacy.renderer.ShapeMode;
import mathax.client.legacy.utils.render.color.SettingColor;
import net.minecraft.block.Block;

public class SBlockDataScreen extends WindowScreen {
    public SBlockDataScreen(GuiTheme theme, SBlockData blockData, Block block, BlockDataSetting<SBlockData> setting) {
        super(theme, "Configure Block");

        Settings settings = new Settings();
        SettingGroup sgGeneral = settings.getDefaultGroup();
        SettingGroup sgTracer = settings.createGroup("Tracer");

        sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shape is rendered.")
            .defaultValue(ShapeMode.Lines)
            .onModuleActivated(shapeModeSetting -> shapeModeSetting.set(blockData.shapeMode))
            .onChanged(shapeMode -> {
                    blockData.shapeMode = shapeMode;
                    changed(blockData, block, setting);
                })
            .build()
        );

        sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color of lines.")
            .defaultValue(new SettingColor(0, 255, 200))
            .onModuleActivated(settingColorSetting -> settingColorSetting.set(blockData.lineColor))
            .onChanged(settingColor -> {
                    blockData.lineColor.set(settingColor);
                    changed(blockData, block, setting);
                })
            .build()
        );

        sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("Color of sides.")
            .defaultValue(new SettingColor(0, 255, 200, 25))
            .onModuleActivated(settingColorSetting -> settingColorSetting.set(blockData.sideColor))
            .onChanged(settingColor -> {
                    blockData.sideColor.set(settingColor);
                    changed(blockData, block, setting);
                })
            .build()
        );

        sgTracer.add(new BoolSetting.Builder()
            .name("tracer")
            .description("If tracer line is allowed to this block.")
            .defaultValue(true)
            .onModuleActivated(booleanSetting -> booleanSetting.set(blockData.tracer))
            .onChanged(aBoolean -> {
                    blockData.tracer = aBoolean;
                    changed(blockData, block, setting);
                })
            .build()
        );

        sgTracer.add(new ColorSetting.Builder()
            .name("tracer-color")
            .description("Color of tracer line.")
            .defaultValue(new SettingColor(0, 255, 200, 125))
            .onModuleActivated(settingColorSetting -> settingColorSetting.set(blockData.tracerColor))
            .onChanged(settingColor -> {
                    blockData.tracerColor = settingColor;
                    changed(blockData, block, setting);
                })
            .build()
        );

        settings.onActivated();
        add(theme.settings(settings)).expandX();
    }

    private void changed(SBlockData blockData, Block block, BlockDataSetting<SBlockData> setting) {
        if (!blockData.isChanged() && block != null && setting != null) {
            setting.get().put(block, blockData);
            setting.changed();
        }

        blockData.changed();
    }
}
