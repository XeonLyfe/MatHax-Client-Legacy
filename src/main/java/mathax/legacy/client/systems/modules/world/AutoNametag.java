package mathax.legacy.client.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.entity.SortPriority;
import mathax.legacy.client.utils.entity.TargetUtils;
import mathax.legacy.client.utils.player.*;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoNametag extends Module {
    private Entity target;
    private boolean offHand;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Which entities to nametag.")
        .defaultValue(new Object2BooleanOpenHashMap<>(0))
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range an entity can be to be nametagged.")
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("Priority sort.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Boolean> renametag = sgGeneral.add(new BoolSetting.Builder()
        .name("renametag")
        .description("Allows already nametagged entities to be renamed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the mob being nametagged.")
        .defaultValue(true)
        .build()
    );

    public AutoNametag() {
        super(Categories.World, Items.NAME_TAG, "auto-nametag", "Automatically uses nametags on entities without a nametag. WILL nametag ALL entities in the specified distance.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Nametag in hobar
        FindItemResult findNametag = InvUtils.findInHotbar(Items.NAME_TAG);

        if (!findNametag.found()) {
            error("No Nametag in Hotbar");
            toggle();
            return;
        }


        // Target
        target = TargetUtils.get(entity -> {
            if (PlayerUtils.distanceTo(entity) > range.get()) return false;
            if (!entities.get().getBoolean(entity.getType())) return false;
            if (entity.hasCustomName()) {
                return renametag.get() && entity.getCustomName() != mc.player.getInventory().getStack(findNametag.getSlot()).getName();
            }
            return false;
        }, priority.get());

        if (target == null) return;


        // Swapping slots
        InvUtils.swap(findNametag.getSlot(), true);

        offHand = findNametag.isOffhand();


        // Interaction
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), -100, this::interact);
        else interact();
    }

    private void interact() {
        mc.interactionManager.interactEntity(mc.player, target, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        InvUtils.swapBack();
    }
}
