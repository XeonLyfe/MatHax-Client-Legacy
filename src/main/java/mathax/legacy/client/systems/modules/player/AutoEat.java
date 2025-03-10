package mathax.legacy.client.systems.modules.player;

import baritone.api.BaritoneAPI;
import mathax.legacy.client.events.entity.player.ItemUseCrosshairTargetEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.systems.modules.combat.BedAura;
import mathax.legacy.client.systems.modules.combat.KillAura;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.combat.AnchorAura;
import mathax.legacy.client.systems.modules.combat.CrystalAura;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.player.InvUtils;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.eventbus.EventPriority;
import mathax.legacy.client.settings.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AutoEat extends Module {
    public boolean eating;
    private int slot, prevSlot;

    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private boolean wasBaritone;

    private static final Class<? extends Module>[] AURAS = new Class[] { KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class };

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHunger = settings.createGroup("Hunger");

    // General

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Which items to not eat.")
        .defaultValue(getDefaultBlacklist())
        .filter(Item::isFood)
        .build()
    );

    private final Setting<Boolean> pauseAuras = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-auras")
        .description("Pauses all auras when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Pause baritone when eating.")
        .defaultValue(true)
        .build()
    );

    // Hunger

    private final Setting<Integer> hungerThreshold = sgHunger.add(new IntSetting.Builder()
        .name("hunger-threshold")
        .description("The level of hunger you eat at.")
        .defaultValue(16)
        .min(1)
        .max(19)
        .sliderMin(1)
        .sliderMax(19)
        .build()
    );

    public AutoEat() {
        super(Categories.Player, Items.APPLE, "auto-eat", "Automatically eats food.");
    }

    @Override
    public void onDeactivate() {
        if (eating) stopEating();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onTick(TickEvent.Pre event) {
        // Skip if Auto Gap is already eating
        if (Modules.get().get(AutoGap.class).isEating()) return;

        if (eating) {
            // If we are eating check if we should still be still eating
            if (shouldEat()) {
                // Check if the item in current slot is not food
                if (!mc.player.getInventory().getStack(slot).isFood()) {
                    // If not try finding a new slot
                    int slot = findSlot();

                    // If no valid slot was found then stop eating
                    if (slot == -1) {
                        stopEating();
                        return;
                    }
                    // Otherwise change to the new slot
                    else {
                        changeSlot(slot);
                    }
                }

                // Continue eating
                eat();
            }
            // If we shouldn't be eating anymore then stop
            else {
                stopEating();
            }
        } else {
            // If we are not eating check if we should start eating
            if (shouldEat()) {
                // Try to find a valid slot
                slot = findSlot();

                // If slot was found then start eating
                if (slot != -1) startEating();
            }
        }
    }

    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (eating) event.target = null;
    }

    private void startEating() {
        prevSlot = mc.player.getInventory().selectedSlot;
        eat();

        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }

        // Pause baritone
        wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    }

    private void eat() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();

        eating = true;
    }

    private void stopEating() {
        changeSlot(prevSlot);
        setPressed(false);

        eating = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }

        // Resume baritone
        if (pauseBaritone.get() && wasBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }
    }

    private void setPressed(boolean pressed) {
        mc.options.keyUse.setPressed(pressed);
    }

    private void changeSlot(int slot) {
        InvUtils.swap(slot, false);
        this.slot = slot;
    }

    private boolean shouldEat() {
        return mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.get();
    }

    private int findSlot() {
        int slot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            // Skip if item isn't food
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (!item.isFood()) continue;

            // Check if hunger value is better
            int hunger = item.getFoodComponent().getHunger();
            if (hunger > bestHunger) {
                // Skip if item is in blacklist
                if (blacklist.get().contains(item)) continue;

                // Select the current item
                slot = i;
                bestHunger = hunger;
            }
        }

        return slot;
    }

    private static List<Item> getDefaultBlacklist() {
        List<Item> l = new ArrayList<>(9);

        l.add(Items.ENCHANTED_GOLDEN_APPLE);
        l.add(Items.GOLDEN_APPLE);
        l.add(Items.CHORUS_FRUIT);
        l.add(Items.POISONOUS_POTATO);
        l.add(Items.PUFFERFISH);
        l.add(Items.CHICKEN);
        l.add(Items.ROTTEN_FLESH);
        l.add(Items.SPIDER_EYE);
        l.add(Items.SUSPICIOUS_STEW);

        return l;
    }
}
