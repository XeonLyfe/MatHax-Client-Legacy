package mathax.legacy.client.systems.modules.world;

import mathax.legacy.client.settings.BoolSetting;
import mathax.legacy.client.settings.ColorSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.render.color.SettingColor;
import net.minecraft.client.render.SkyProperties;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class Ambience extends Module {
    private final SettingGroup sgSky = settings.createGroup("Sky");
    private final SettingGroup sgWorld = settings.createGroup("World");

    // Sky

    public final Setting<Boolean> endSky = sgSky.add(new BoolSetting.Builder()
        .name("end-sky")
        .description("Makes the sky like the end.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> customSkyColor = sgSky.add(new BoolSetting.Builder()
        .name("custom-sky-color")
        .description("Whether the sky color should be changed.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> skyColor = sgSky.add(new ColorSetting.Builder()
        .name("sky-color")
        .description("The color of the sky.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customSkyColor::get)
        .build()
    );

    public final Setting<Boolean> customCloudColor = sgSky.add(new BoolSetting.Builder()
        .name("custom-cloud-color")
        .description("Whether the clouds color should be changed.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> cloudColor = sgSky.add(new ColorSetting.Builder()
        .name("cloud-color")
        .description("The color of the clouds.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customCloudColor::get)
        .build()
    );

    public final Setting<Boolean> changeLightningColor = sgSky.add(new BoolSetting.Builder()
        .name("custom-lightning-color")
        .description("Whether the lightning color should be changed.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> lightningColor = sgSky.add(new ColorSetting.Builder()
        .name("lightning-color")
        .description("The color of the lightning.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(changeLightningColor::get)
        .build()
    );

    // World

    public final Setting<Boolean> customGrassColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-grass-color")
        .description("Whether the grass color should be changed.")
        .defaultValue(false)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<SettingColor> grassColor = sgWorld.add(new ColorSetting.Builder()
        .name("grass-color")
        .description("The color of the grass.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customGrassColor::get)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<Boolean> customFoliageColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-foliage-color")
        .description("Whether the foliage color should be changed.")
        .defaultValue(false)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<SettingColor> foliageColor = sgWorld.add(new ColorSetting.Builder()
        .name("foliage-color")
        .description("The color of the foliage.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customFoliageColor::get)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<Boolean> customWaterColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-water-color")
        .description("Whether the water color should be changed.")
        .defaultValue(false)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<SettingColor> waterColor = sgWorld.add(new ColorSetting.Builder()
        .name("water-color")
        .description("The color of the water.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customWaterColor::get)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<Boolean> customLavaColor = sgWorld.add(new BoolSetting.Builder()
        .name("custom-lava-color")
        .description("Whether the lava color should be changed.")
        .defaultValue(false)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public final Setting<SettingColor> lavaColor = sgWorld.add(new ColorSetting.Builder()
        .name("lava-color")
        .description("The color of the lava.")
        .defaultValue(new SettingColor(102, 0, 0))
        .visible(customLavaColor::get)
        .onChanged(val -> mc.worldRenderer.reload())
        .build()
    );

    public Ambience() {
        super(Categories.World, Items.RED_STAINED_GLASS, "ambience", "Change the color of various pieces of the environment.");
    }

    @Override
    public void onActivate() {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public static class Custom extends SkyProperties {
        public Custom() {
            super(Float.NaN, true, SkyProperties.SkyType.END, true, false);
        }

        @Override
        public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
            return color.multiply(0.15000000596046448D);
        }

        @Override
        public boolean useThickFog(int camX, int camY) {
            return false;
        }

        @Override
        public float[] getFogColorOverride(float skyAngle, float tickDelta) {
            return null;
        }
    }
}
