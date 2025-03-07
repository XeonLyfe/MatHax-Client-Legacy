package mathax.legacy.client.systems.modules.world;

import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.settings.EnumSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.entity.Target;
import mathax.legacy.client.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class EndermanLook extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> lookMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("look-mode")
        .description("How this module behaves.")
        .defaultValue(Mode.LookAway)
        .build()
    );

    public EndermanLook() {
        super(Categories.World, Items.ENDER_EYE, "enderman-look", "Either looks at all Endermen or prevents you from looking at Endermen.");
    }

    public void onTick(TickEvent.Pre event) {
        if (lookMode.get() == Mode.LookAway) {
            if (mc.player.getAbilities().creativeMode || !shouldLook()) return;

            Rotations.rotate(mc.player.getYaw(), 90, -75, null);
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndermanEntity)) continue;
                EndermanEntity enderman = (EndermanEntity) entity;

                if (enderman.isAngry() || !enderman.isAlive() || !mc.player.canSee(enderman)) continue;

                Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.Head), -75, null);
                break;
            }
        }
    }

    private boolean shouldLook() {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndermanEntity)) continue;

            if (entity.isAlive() && angleCheck(entity)) return true;
        }

        return false;
    }

    private boolean angleCheck(Entity entity) {
        Vec3d vec3d = mc.player.getRotationVec(1.0F).normalize();
        Vec3d vec3d2 = new Vec3d(entity.getX() - mc.player.getX(), entity.getEyeY() - mc.player.getEyeY(), entity.getZ() - mc.player.getZ());

        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dotProduct(vec3d2);

        return e > 1.0D - 0.025D / d && mc.player.canSee(entity);
    }

    public enum Mode{
        LookAt,
        LookAway
    }
}
