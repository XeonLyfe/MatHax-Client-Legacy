package mathax.legacy.client.systems.modules.movement;

import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.gui.WidgetScreen;
import mathax.legacy.client.mixin.CreativeInventoryScreenAccessor;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.render.Freecam;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.misc.input.Input;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;

import static org.lwjgl.glfw.GLFW.*;

public class GUIMove extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Screens> screens = sgGeneral.add(new EnumSetting.Builder<Screens>()
        .name("GUIs")
        .description("Which GUIs to move in.")
        .defaultValue(Screens.Inventory)
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Allows you to jump while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
                if (isActive() && !aBoolean) mc.options.keyJump.setPressed(false);
            })
        .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Allows you to sneak while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
                if (isActive() && !aBoolean) mc.options.keySneak.setPressed(false);
            })
        .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Allows you to sprint while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
                if (isActive() && !aBoolean) mc.options.keySprint.setPressed(false);
            })
        .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("arrows-rotate")
        .description("Allows you to use your arrow keys to rotate while in GUIs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate-speed")
        .description("Rotation speed while in GUIs.")
        .defaultValue(4)
        .min(0)
        .build()
    );

    public GUIMove() {
        super(Categories.Movement, Items.DIAMOND_BOOTS, "gui-move", "Allows you to perform various actions while in GUIs.");
    }

    @Override
    public void onDeactivate() {
        mc.options.keyForward.setPressed(false);
        mc.options.keyBack.setPressed(false);
        mc.options.keyLeft.setPressed(false);
        mc.options.keyRight.setPressed(false);

        if (jump.get()) mc.options.keyJump.setPressed(false);
        if (sneak.get()) mc.options.keySneak.setPressed(false);
        if (sprint.get()) mc.options.keySprint.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (skip()) return;
        if (screens.get() == Screens.GUI && !(mc.currentScreen instanceof WidgetScreen)) return;
        if (screens.get() == Screens.Inventory && mc.currentScreen instanceof WidgetScreen) return;

        mc.options.keyForward.setPressed(Input.isPressed(mc.options.keyForward));
        mc.options.keyBack.setPressed(Input.isPressed(mc.options.keyBack));
        mc.options.keyLeft.setPressed(Input.isPressed(mc.options.keyLeft));
        mc.options.keyRight.setPressed(Input.isPressed(mc.options.keyRight));

        if (jump.get()) mc.options.keyJump.setPressed(Input.isPressed(mc.options.keyJump));
        if (sneak.get()) mc.options.keySneak.setPressed(Input.isPressed(mc.options.keySneak));
        if (sprint.get()) mc.options.keySprint.setPressed(Input.isPressed(mc.options.keySprint));

        if (arrowsRotate.get()) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            for (int i = 0; i < (rotateSpeed.get() * 2); i++) {
                if (Input.isKeyPressed(GLFW_KEY_LEFT)) yaw -= 0.5;
                if (Input.isKeyPressed(GLFW_KEY_RIGHT)) yaw += 0.5;
                if (Input.isKeyPressed(GLFW_KEY_UP)) pitch -= 0.5;
                if (Input.isKeyPressed(GLFW_KEY_DOWN)) pitch += 0.5;
            }

            pitch = Utils.clamp(pitch, -90, 90);

            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private boolean skip() {
        return mc.currentScreen == null || Modules.get().isActive(Freecam.class) || (mc.currentScreen instanceof CreativeInventoryScreen && ((CreativeInventoryScreenAccessor) mc.currentScreen).getSelectedTab() == ItemGroup.SEARCH.getIndex()) || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof AnvilScreen || mc.currentScreen instanceof AbstractCommandBlockScreen || mc.currentScreen instanceof StructureBlockScreen;
    }

    public enum Screens {
        GUI,
        Inventory,
        Both
    }
}
