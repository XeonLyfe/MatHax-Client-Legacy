package mathax.legacy.client.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.settings.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class Chams extends Module {
    public static final Identifier BLANK = new Identifier("mathaxlegacy", "textures/blank.png");

    private final SettingGroup sgThroughWalls = settings.createGroup("Through Walls");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgHand = settings.createGroup("Hand");

    // Through walls
    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgThroughWalls.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to show through walls.")
        .defaultValue(Utils.asObject2BooleanOpenHashMap(EntityType.PLAYER))
        .build()
    );

    //Players

    public final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
        .name("players")
        .description("Enables model tweaks for players.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> ignoreSelf = sgPlayers.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself when tweaking player models.")
        .defaultValue(true)
        .visible(players::get)
        .build()
    );

    public final Setting<Double> playersScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Players scale.")
        .defaultValue(1.0)
        .min(0.0)
        .visible(players::get)
        .build()
    );

    public final Setting<Boolean> playersTexture = sgPlayers.add(new BoolSetting.Builder()
        .name("texture")
        .description("Enables player model textures.")
        .defaultValue(false)
        .visible(players::get)
        .build()
    );

    public final Setting<SettingColor> playersColor = sgPlayers.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of player models.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 100))
        .visible(players::get)
        .build()
    );

    //Crystals

    public final Setting<Boolean> crystals = sgCrystals.add(new BoolSetting.Builder()
        .name("crystals")
        .description("Enables model tweaks for end crystals.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> crystalsScale = sgCrystals.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Crystal scale.")
        .defaultValue(0.6)
        .min(0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Double> crystalsBounce = sgCrystals.add(new DoubleSetting.Builder()
        .name("bounce")
        .description("How high crystals bounce.")
        .defaultValue(0.3)
        .min(0.0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Double> crystalsRotationSpeed = sgCrystals.add(new DoubleSetting.Builder()
        .name("rotation-speed")
        .description("Multiplies the roation speed of the crystal.")
        .defaultValue(3)
        .min(0)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Boolean> crystalsTexture = sgCrystals.add(new BoolSetting.Builder()
        .name("texture")
        .description("Whether to render crystal model textures.")
        .defaultValue(false)
        .visible(crystals::get)
        .build()
    );

    public final Setting<Boolean> renderCore = sgCrystals.add(new BoolSetting.Builder()
        .name("render-core")
        .description("Enables rendering of the core of the crystal.")
        .defaultValue(false)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsCoreColor = sgCrystals.add(new ColorSetting.Builder()
        .name("core-color")
        .description("The color of end crystal models.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 100))
        .visible(renderCore::get)
        .build()
    );

    public final Setting<Boolean> renderFrame1 = sgCrystals.add(new BoolSetting.Builder()
        .name("render-inner-frame")
        .description("Enables rendering of the frame of the crystal.")
        .defaultValue(true)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsFrame1Color = sgCrystals.add(new ColorSetting.Builder()
        .name("inner-frame-color")
        .description("The color of end crystal models.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 100))
        .visible(renderFrame1::get)
        .build()
    );

    public final Setting<Boolean> renderFrame2 = sgCrystals.add(new BoolSetting.Builder()
        .name("render-outer-frame")
        .description("Enables rendering of the frame of the crystal.")
        .defaultValue(true)
        .visible(crystals::get)
        .build()
    );

    public final Setting<SettingColor> crystalsFrame2Color = sgCrystals.add(new ColorSetting.Builder()
        .name("outer-frame-color")
        .description("The color of end crystal models.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 100))
        .visible(renderFrame2::get)
        .build()
    );

    // Hand
    public final Setting<Boolean> hand = sgHand.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Enables tweaks of hand rendering.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> handTexture = sgHand.add(new BoolSetting.Builder()
        .name("texture")
        .description("Whether to render hand textures.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> handColor = sgHand.add(new ColorSetting.Builder()
        .name("hand-color")
        .description("The color of your hand.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 100))
        .build()
    );

    public Chams() {
        super(Categories.Render, Items.GREEN_STAINED_GLASS, "chams", "Tweaks rendering of entities.");
    }

    public boolean shouldRender(Entity entity) {
        return isActive() && entities.get().getBoolean(entity.getType());
    }
}
