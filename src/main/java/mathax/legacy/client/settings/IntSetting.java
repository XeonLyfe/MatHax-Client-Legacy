package mathax.legacy.client.settings;

import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class IntSetting extends Setting<Integer> {
    public final Integer min, max;
    public final boolean noSlider;
    private final Integer sliderMin, sliderMax;

    private IntSetting(String name, String description, Integer defaultValue, Consumer<Integer> onChanged, Consumer<Setting<Integer>> onModuleActivated, IVisible visible, Integer min, Integer max, Integer sliderMin, Integer sliderMax, Boolean noSlider) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.noSlider = noSlider;
    }

    @Override
    protected Integer parseImpl(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected boolean isValueValid(Integer value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    public int getSliderMin() {
        return sliderMin != null ? sliderMin : 0;
    }

    public int getSliderMax() {
        return sliderMax != null ? sliderMax : 10;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();
        tag.putInt("value", get());
        return tag;
    }

    @Override
    public Integer fromTag(NbtCompound tag) {
        set(tag.getInt("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Integer defaultValue;
        private Consumer<Integer> onChanged;
        private Consumer<Setting<Integer>> onModuleActivated;
        private IVisible visible;
        private Integer min, max;
        private Integer sliderMin, sliderMax;
        private Boolean noSlider = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(int defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Integer> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Integer>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder sliderMin(int min) {
            sliderMin = min;
            return this;
        }

        public Builder sliderMax(int max) {
            sliderMax = max;
            return this;
        }

        public Builder noSlider() {
            noSlider = true;
            return this;
        }

        public IntSetting build() {
            return new IntSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, min, max, sliderMin, sliderMax, noSlider);
        }
    }
}
