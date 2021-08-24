package mathax.client.legacy.utils.misc;

import mathax.client.legacy.utils.misc.input.Input;
import mathax.client.legacy.utils.Utils;
import net.minecraft.nbt.NbtCompound;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Keybind implements ISerializable<Keybind>, ICopyable<Keybind> {
    private boolean isKey;
    private int value;

    private Keybind(boolean isKey, int value) {
        set(isKey, value);
    }

    public static Keybind none() {
        return new Keybind(true, -1);
    }

    public static Keybind fromKey(int key) {
        return new Keybind(true, key);
    }

    public static Keybind fromButton(int button) {
        return new Keybind(false, button);
    }

    public int getValue() {
        return value;
    }

    public boolean isSet() {
        return value != -1;
    }

    public boolean canBindTo(boolean isKey, int value) {
        if (isKey) return true;
        return value != GLFW_MOUSE_BUTTON_LEFT && value != GLFW_MOUSE_BUTTON_RIGHT;
    }

    public void set(boolean isKey, int value) {
        this.isKey = isKey;
        this.value = value;
    }

    @Override
    public Keybind set(Keybind value) {
        this.isKey = value.isKey;
        this.value = value.value;

        return this;
    }

    public boolean matches(boolean isKey, int value) {
        if (this.isKey != isKey) return false;
        return this.value == value;
    }

    public boolean isValid() {
        return value != -1;
    }

    public boolean isKey() {
        return isKey;
    }

    public boolean isPressed() {
        return isKey ? Input.isKeyPressed(value) : Input.isButtonPressed(value);
    }

    @Override
    public Keybind copy() {
        return new Keybind(isKey, value);
    }

    @Override
    public String toString() {
        if (value == -1) return "None";
        return isKey ? Utils.getKeyName(value) : Utils.getButtonName(value);
    }

    // Serialization

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putBoolean("isKey", isKey);
        tag.putInt("value", value);

        return tag;
    }

    @Override
    public Keybind fromTag(NbtCompound tag) {
        isKey = tag.getBoolean("isKey");
        value = tag.getInt("value");

        return this;
    }
}
