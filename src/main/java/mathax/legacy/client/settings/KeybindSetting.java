package mathax.legacy.client.settings;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.mathax.KeyEvent;
import mathax.legacy.client.events.mathax.MouseButtonEvent;
import mathax.legacy.client.gui.widgets.WKeybind;
import mathax.legacy.client.utils.misc.Keybind;
import mathax.legacy.client.utils.misc.input.KeyAction;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.eventbus.EventPriority;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class KeybindSetting extends Setting<Keybind> {
    private final Runnable action;
    public WKeybind widget;

    public KeybindSetting(String name, String description, Keybind defaultValue, Consumer<Keybind> onChanged, Consumer<Setting<Keybind>> onModuleActivated, IVisible visible, Runnable action) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.action = action;

        MatHaxLegacy.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (event.action == KeyAction.Release && widget != null && widget.onAction(true, event.key)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMouseButtonBinding(MouseButtonEvent event) {
        if (event.action == KeyAction.Release && widget != null && widget.onAction(false, event.button)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Release && get().matches(true, event.key) && module.isActive() && action != null) {
            action.run();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Release && get().matches(false ,event.button) && module.isActive() && action != null) {
            action.run();
        }
    }

    @Override
    public void reset(boolean callbacks) {
        if (value == null) value = defaultValue.copy();
        else value.set(defaultValue);

        if (callbacks) changed();
    }

    @Override
    protected Keybind parseImpl(String str) {
        try {
            return Keybind.fromKey(Integer.parseInt(str.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected boolean isValueValid(Keybind value) {
        return true;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();

        tag.put("value", get().toTag());

        return tag;
    }

    @Override
    public Keybind fromTag(NbtCompound tag) {
        get().fromTag(tag.getCompound("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Keybind defaultValue = Keybind.none();
        private Consumer<Keybind> onChanged;
        private Consumer<Setting<Keybind>> onModuleActivated;
        private IVisible visible;
        private Runnable action;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Keybind defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Keybind> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Keybind>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        public KeybindSetting build() {
            return new KeybindSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, action);
        }
    }
}
