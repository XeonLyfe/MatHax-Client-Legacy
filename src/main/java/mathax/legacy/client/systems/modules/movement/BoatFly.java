package mathax.legacy.client.systems.modules.movement;

import mathax.legacy.client.events.entity.BoatMoveEvent;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.mixininterface.IVec3d;
import mathax.legacy.client.settings.BoolSetting;
import mathax.legacy.client.settings.DoubleSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.player.PlayerUtils;
import mathax.legacy.client.eventbus.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.Vec3d;

public class BoatFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you fall in blocks per second.")
        .defaultValue(0.1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> cancelServerPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-server-packets")
        .description("Cancels incoming boat move packets.")
        .defaultValue(false)
        .build()
    );

    public BoatFly() {
        super(Categories.Movement, Items.OAK_BOAT, "boat-fly", "Transforms your boat into a plane.");
    }

    @EventHandler
    private void onBoatMove(BoatMoveEvent event) {
        if (event.boat.getPrimaryPassenger() != mc.player) return;

        event.boat.setYaw(mc.player.getYaw());

        // Horizontal movement
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        double velX = vel.getX();
        double velY = 0;
        double velZ = vel.getZ();

        // Vertical movement
        if (mc.options.keyJump.isPressed()) velY += verticalSpeed.get() / 20;
        if (mc.options.keySprint.isPressed()) velY -= verticalSpeed.get() / 20;
        else velY -= fallSpeed.get() / 20;

        // Apply velocity
        ((IVec3d) event.boat.getVelocity()).set(velX, velY, velZ);
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof VehicleMoveS2CPacket && cancelServerPackets.get()) {
            event.cancel();
        }
    }
}
