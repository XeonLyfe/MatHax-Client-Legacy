package mathax.legacy.client.systems.modules.render.hud.modules;

import mathax.legacy.client.renderer.Renderer2D;
import mathax.legacy.client.systems.modules.render.hud.HUD;
import mathax.legacy.client.systems.modules.render.hud.HUDElement;
import mathax.legacy.client.systems.modules.render.hud.HUDRenderer;
import mathax.legacy.client.utils.misc.FakeClientPlayer;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.settings.*;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class PlayerModelHUD extends HUDElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderMin(1).sliderMax(5)
        .build()
    );

    private final Setting<Boolean> copyYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-yaw")
        .description("Makes the player model's yaw equal to yours.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> copyPitch = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-pitch")
        .description("Makes the player model's pitch equal to yours.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> customYaw = sgGeneral.add(new IntSetting.Builder()
        .name("custom-yaw")
        .description("Custom yaw for when copy yaw is off.")
        .defaultValue(0)
        .min(-180).max(180)
        .sliderMin(-180).sliderMax(180)
        .visible(() -> !copyYaw.get())
        .build()
    );

    private final Setting<Integer> customPitch = sgGeneral.add(new IntSetting.Builder()
        .name("custom-pitch")
        .description("Custom pitch for when copy pitch is off.")
        .defaultValue(0)
        .min(-180).max(180)
        .sliderMin(-180).sliderMax(180)
        .visible(() -> !copyPitch.get())
        .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background behind the player model.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of background.")
        .defaultValue(new SettingColor(0, 0, 0, 64))
        .visible(background::get)
        .build()
    );

    public PlayerModelHUD(HUD hud) {
        super(hud, "player-model", "Displays a model of your player", true);
    }

    @Override
    public void update(HUDRenderer renderer) {
        box.setSize(50 * scale.get(), 75 * scale.get());
    }

    @Override
    public void render(HUDRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (background.get()) {
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quadRounded(x, y, box.width, box.height, backgroundColor.get(), renderer.roundAmount(), true);
            Renderer2D.COLOR.render(null);
        }

        PlayerEntity player = mc.player;
        if (isInEditor()) player = FakeClientPlayer.getPlayer();

        float yaw = copyYaw.get() ? MathHelper.wrapDegrees(player.prevYaw + (player.getYaw() - player.prevYaw) * mc.getTickDelta()) : (float) customYaw.get();
        float pitch = copyPitch.get() ? player.getPitch() : (float) customPitch.get();

        InventoryScreen.drawEntity((int) (x + (25 * scale.get())), (int) (y + (66 * scale.get())), (int) (30 * scale.get()), -yaw, -pitch, player);
    }
}
