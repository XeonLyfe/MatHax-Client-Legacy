package mathax.legacy.client.systems.modules.world;

import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.settings.BoolSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.player.Rotations;
import mathax.legacy.client.eventbus.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Hand;

public class AutoMount extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMount = settings.createGroup("Mount");

    // General

    private final Setting<Boolean> checkSaddle = sgGeneral.add(new BoolSetting.Builder()
        .name("check-saddle")
        .description("Checks if the entity contains a saddle before mounting.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the entity you mount.")
        .defaultValue(true)
        .build()
    );

    // Mount

    private final Setting<Boolean> horses = sgMount.add(new BoolSetting.Builder()
        .name("horse")
        .description("Mounts Horses.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> donkeys = sgMount.add(new BoolSetting.Builder()
        .name("donkey")
        .description("Mounts Donkeys.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> mules = sgMount.add(new BoolSetting.Builder()
        .name("mule")
        .description("Mounts Mules.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> skeletonHorse = sgMount.add(new BoolSetting.Builder()
        .name("skeleton-horse")
        .description("Mounts Skeleton Horses.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> llamas = sgMount.add(new BoolSetting.Builder()
        .name("llama")
        .description("Mounts Llamas.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pigs = sgMount.add(new BoolSetting.Builder()
        .name("pig")
        .description("Mounts Pigs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> boats = sgMount.add(new BoolSetting.Builder()
        .name("boat")
        .description("Mounts Boats.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> minecarts = sgMount.add(new BoolSetting.Builder()
        .name("minecart")
        .description("Mounts Minecarts.")
        .defaultValue(false)
        .build()
    );

    public AutoMount() {
        super(Categories.World, Items.SADDLE, "auto-mount", "Automatically mounts entities.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.hasVehicle()) return;

        for (Entity entity : mc.world.getEntities()){
            if (mc.player.distanceTo(entity) > 4) continue;

            if (mc.player.getMainHandStack().getItem() instanceof SpawnEggItem) return;

            if (donkeys.get() && entity instanceof DonkeyEntity && (!checkSaddle.get() || ((DonkeyEntity) entity).isSaddled())) {
                interact(entity);
            } else if (llamas.get() && entity instanceof LlamaEntity) {
                interact(entity);
            } else if (boats.get() && entity instanceof BoatEntity) {
                interact(entity);
            } else if (minecarts.get() && entity instanceof MinecartEntity) {
                interact(entity);
            } else if (horses.get() && entity instanceof HorseEntity && (!checkSaddle.get() || ((HorseEntity) entity).isSaddled())) {
                interact(entity);
            } else if (pigs.get() && entity instanceof PigEntity && ((PigEntity) entity).isSaddled()) {
                interact(entity);
            } else if (mules.get() && entity instanceof MuleEntity && (!checkSaddle.get() || ((MuleEntity) entity).isSaddled())) {
                interact(entity);
            } else if (skeletonHorse.get() && entity instanceof SkeletonHorseEntity && (!checkSaddle.get() || ((SkeletonHorseEntity) entity).isSaddled())) {
                interact(entity);
            }
        }
    }

    private void interact(Entity entity) {
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, () -> mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND));
        else mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
    }
}
