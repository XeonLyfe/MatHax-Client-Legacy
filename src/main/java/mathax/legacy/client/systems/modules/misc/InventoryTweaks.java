package mathax.legacy.client.systems.modules.misc;

import mathax.legacy.client.events.entity.DropItemsEvent;
import mathax.legacy.client.events.game.OpenScreenEvent;
import mathax.legacy.client.events.mathax.KeyEvent;
import mathax.legacy.client.events.mathax.MouseButtonEvent;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.mixin.CloseHandledScreenC2SPacketAccessor;
import mathax.legacy.client.mixin.HandledScreenAccessor;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.misc.Keybind;
import mathax.legacy.client.utils.misc.input.KeyAction;
import mathax.legacy.client.utils.network.MatHaxExecutor;
import mathax.legacy.client.utils.player.InvUtils;
import mathax.legacy.client.utils.player.InventorySorter;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.settings.*;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryTweaks extends Module {
    private InventorySorter sorter;

    private boolean invOpened;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSorting = settings.createGroup("Sorting");
    private final SettingGroup sgAutoDrop = settings.createGroup("Auto Drop");
    private final SettingGroup sgAutoSteal = settings.createGroup("Auto Steal");

    // General

    private final Setting<Boolean> mouseDragItemMove = sgGeneral.add(new BoolSetting.Builder()
        .name("mouse-drag-item-move")
        .description("Moving mouse over items while holding shift will transfer it to the other container.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Item>> antiDropItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("anti-drop-items")
        .description("Items to prevent dropping. Doesn't work in creative inventory screen.")
        .defaultValue(new ArrayList<>(0))
        .build()
    );

    private final Setting<Boolean> buttons = sgGeneral.add(new BoolSetting.Builder()
        .name("inventory-buttons")
        .description("Shows steal and dump buttons in container guis.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> xCarry = sgGeneral.add(new BoolSetting.Builder()
        .name("xcarry")
        .description("Allows you to store four extra items in your crafting grid.")
        .defaultValue(false)
        .onChanged(v -> {
            if (v || !Utils.canUpdate()) return;
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
            invOpened = false;
        })
        .build()
    );

    // Sorting

    private final Setting<Boolean> sortingEnabled = sgSorting.add(new BoolSetting.Builder()
        .name("sorting-enabled")
        .description("Automatically sorts stacks in inventory.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> sortingKey = sgSorting.add(new KeybindSetting.Builder()
        .name("sorting-key")
        .description("Key to trigger the sort.")
        .visible(sortingEnabled::get)
        .defaultValue(Keybind.fromButton(GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
        .build()
    );

    private final Setting<Integer> sortingDelay = sgSorting.add(new IntSetting.Builder()
        .name("sorting-delay")
        .description("Delay in ticks between moving items when sorting.")
        .visible(sortingEnabled::get)
        .defaultValue(1)
        .min(0)
        .build()
    );

    // Auto Drop

    private final Setting<List<Item>> autoDropItems = sgAutoDrop.add(new ItemListSetting.Builder()
        .name("auto-drop-items")
        .description("Items to drop.")
        .defaultValue(new ArrayList<>(0))
        .build()
    );

    private final Setting<Boolean> autoDropExcludeHotbar = sgAutoDrop.add(new BoolSetting.Builder()
        .name("auto-drop-exclude-hotbar")
        .description("Whether or not to drop items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    // Auto steal

    private final Setting<Boolean> autoSteal = sgAutoSteal.add(new BoolSetting.Builder()
        .name("auto-steal")
        .description("Automatically removes all possible items when you open a container.")
        .defaultValue(false)
        .onChanged(val -> checkAutoStealSetttings())
        .build()
    );

    private final Setting<Boolean> autoDump = sgAutoSteal.add(new BoolSetting.Builder()
        .name("auto-dump")
        .description("Automatically dumps all possible items when you open a container.")
        .defaultValue(false)
        .onChanged(val -> checkAutoStealSetttings())
        .build()
    );

    private final Setting<Integer> autoStealDelay = sgAutoSteal.add(new IntSetting.Builder()
        .name("delay")
        .description("The minimum delay between stealing the next stack in milliseconds.")
        .defaultValue(20)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> autoStealRandomDelay = sgAutoSteal.add(new IntSetting.Builder()
        .name("random")
        .description("Randomly adds a delay of up to the specified time in milliseconds.")
        .min(0)
        .sliderMax(1000)
        .defaultValue(50)
        .build()
    );

    public InventoryTweaks() {
        super(Categories.Misc, Items.CHEST, "inventory-tweaks", "Various inventory related utilities.");
    }

    @Override
    public void onActivate() {
        invOpened = false;
    }

    @Override
    public void onDeactivate() {
        sorter = null;

        if (invOpened) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
        }
    }

    // Sorting

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Press && sortingKey.get().matches(true, event.key)) sort();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Press && sortingKey.get().matches(false, event.button)) sort();
    }

    private void sort() {
        if (!sortingEnabled.get() || !(mc.currentScreen instanceof HandledScreen<?> screen) || sorter != null) return;

        Slot focusedSlot = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (focusedSlot == null) return;

        sorter = new InventorySorter(screen, focusedSlot);
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        sorter = null;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (sorter != null && sorter.tick(sortingDelay.get())) sorter = null;
    }

    // Auto Drop

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        // Auto Drop
        if (mc.currentScreen instanceof HandledScreen<?> || autoDropItems.get().isEmpty()) return;

        for (int i = autoDropExcludeHotbar.get() ? 9 : 0; i < mc.player.getInventory().size(); i++) {
            if (autoDropItems.get().contains(mc.player.getInventory().getStack(i).getItem())) {
                InvUtils.drop().slot(i);
            }
        }
    }

    @EventHandler
    private void onDropItems(DropItemsEvent event) {
        if (antiDropItems.get().contains(event.itemStack.getItem())) event.cancel();
    }

    // XCarry

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!xCarry.get() || !(event.packet instanceof CloseHandledScreenC2SPacket)) return;

        if (((CloseHandledScreenC2SPacketAccessor) event.packet).getSyncId() == mc.player.playerScreenHandler.syncId) {
            invOpened = true;
            event.cancel();
        }
    }

    // Auto Steal

    private void checkAutoStealSetttings() {
        if (autoSteal.get() && autoDump.get()) {
            error("You can't enable Auto Steal and Auto Dump at the same time!");
            autoDump.set(false);
        }
    }

    private int getSleepTime() {
        return autoStealDelay.get() + (autoStealRandomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, autoStealRandomDelay.get()) : 0);
    }

    private int getRows(ScreenHandler handler) {
        return (handler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler) handler).getRows() : 3);
    }

    private void moveSlots(ScreenHandler handler, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack()) continue;

            int sleep = getSleepTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exit if user closes screen
            if (mc.currentScreen == null) break;

            InvUtils.quickMove().slotId(i);
        }
    }

    public void steal(ScreenHandler handler) {
        MatHaxExecutor.execute(() -> moveSlots(handler, 0, getRows(handler) * 9));
    }

    public void dump(ScreenHandler handler) {
        int playerInvOffset = getRows(handler) * 9;
        MatHaxExecutor.execute(() -> moveSlots(handler, playerInvOffset, playerInvOffset + 4 * 9));
    }

    public boolean showButtons() {
        return isActive() && buttons.get();
    }

    public boolean autoSteal() {
        return isActive() && autoSteal.get();
    }

    public boolean autoDump() {
        return isActive() && autoDump.get();
    }

    public boolean mouseDragItemMove() {
        return isActive() && mouseDragItemMove.get();
    }
}
