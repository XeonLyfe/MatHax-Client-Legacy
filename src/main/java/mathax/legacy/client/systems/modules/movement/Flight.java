package mathax.legacy.client.systems.modules.movement;

import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.mixin.PlayerMoveC2SPacketAccessor;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;

public class Flight extends Module {
    private boolean flip;
    private float lastYaw;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode for Flight.")
        .defaultValue(Mode.Abilities)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed when flying.")
        .defaultValue(0.1)
        .min(0.0)
        .build()
    );
    private final Setting<Boolean> verticalSpeedMatch = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical-speed-match")
        .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
        .defaultValue(false)
        .build()
    );

    // Anti Kick

    private final Setting<AntiKickMode> antiKickMode = sgAntiKick.add(new EnumSetting.Builder<AntiKickMode>()
        .name("mode")
        .description("The mode for anti kick.")
        .defaultValue(AntiKickMode.Packet)
        .build()
    );

    private final Setting<Integer> delay = sgAntiKick.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between toggles in normal mode.")
        .defaultValue(80)
        .min(1)
        .max(5000)
        .sliderMax(200)
        .visible(() -> antiKickMode.get() == AntiKickMode.Normal)
        .build()
    );

    private final Setting<Integer> offTime = sgAntiKick.add(new IntSetting.Builder()
        .name("off-time")
        .description("The amount of delay, in ticks, that Flight is toggled off for in normal mode.")
        .defaultValue(5)
        .min(1)
        .max(20)
        .sliderMax(10)
        .visible(() -> antiKickMode.get() == AntiKickMode.Normal)
        .build()
    );

    private int delayLeft = delay.get();
    private int offLeft = offTime.get();

    public Flight() {
        super(Categories.Movement, Items.COMMAND_BLOCK, "flight", "FLYYYyy! No Fall is recommended with this module.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = true;
            if (mc.player.getAbilities().creativeMode) return;
            mc.player.getAbilities().allowFlying = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().setFlySpeed(0.05f);
            if (mc.player.getAbilities().creativeMode) return;
            mc.player.getAbilities().allowFlying = false;
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        float currentYaw = mc.player.getYaw();
        if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getVelocity().length() < 0.003d) {
            mc.player.setYaw(currentYaw + (flip ? 1 : -1));
            flip = !flip;
        }
        lastYaw = currentYaw;
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (antiKickMode.get() == AntiKickMode.Normal && delayLeft > 0) delayLeft --;

        else if (antiKickMode.get() == AntiKickMode.Normal && delayLeft <= 0 && offLeft > 0) {
            offLeft --;

            if (mode.get() == Mode.Abilities) {
                mc.player.getAbilities().flying = false;
                mc.player.getAbilities().setFlySpeed(0.05f);
                if (mc.player.getAbilities().creativeMode) return;
                mc.player.getAbilities().allowFlying = false;
            }

            return;
        }

        else if (antiKickMode.get() == AntiKickMode.Normal && delayLeft <=0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }

        if (mc.player.getYaw() != lastYaw) mc.player.setYaw(lastYaw);

        switch (mode.get()) {
            case Velocity:

                 /*TODO: deal with underwater movement, find a way to "spoof" not being in water
                also, all of the multiplication below is to get the speed to roughly match the speed
                you get when using vanilla fly*/

                mc.player.getAbilities().flying = false;
                mc.player.flyingSpeed = speed.get().floatValue() * (mc.player.isSprinting() ? 15f : 10f);

                mc.player.setVelocity(0, 0, 0);
                Vec3d initialVelocity = mc.player.getVelocity();

                if (mc.options.keyJump.isPressed()) mc.player.setVelocity(initialVelocity.add(0, speed.get() * (verticalSpeedMatch.get() ? 10f : 5f), 0));
                if (mc.options.keySneak.isPressed()) mc.player.setVelocity(initialVelocity.subtract(0, speed.get() * (verticalSpeedMatch.get() ? 10f : 5f), 0));
                break;
            case Abilities:
                if (mc.player.isSpectator()) return;
                mc.player.getAbilities().setFlySpeed(speed.get().floatValue());
                mc.player.getAbilities().flying = true;
                if (mc.player.getAbilities().creativeMode) return;
                mc.player.getAbilities().allowFlying = true;
                break;
        }
    }

    private long lastModifiedTime = 0;
    private double lastY = Double.MAX_VALUE;

    /**
     * @see ServerPlayNetworkHandler#onPlayerMove(PlayerMoveC2SPacket)
     */
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket) || antiKickMode.get() != AntiKickMode.Packet) return;

        PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;
        long currentTime = System.currentTimeMillis();
        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            // maximum time we can be "floating" is 80 ticks, so 4 seconds max
            if (currentTime - lastModifiedTime > 1000
                    && lastY != Double.MAX_VALUE
                    && mc.world.getBlockState(mc.player.getBlockPos().down()).isAir()) {
                // actual check is for >= -0.03125D but we have to do a bit more than that
                // probably due to compression or some shit idk
                ((PlayerMoveC2SPacketAccessor) packet).setY(lastY - 0.03130D);
                lastModifiedTime = currentTime;
            } else {
                lastY = currentY;
            }
        }
    }

    public enum Mode {
        Abilities,
        Velocity
    }

    public enum AntiKickMode {
        Normal,
        Packet,
        None
    }
}
