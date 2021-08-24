package mathax.client.legacy.systems.modules;

import net.minecraft.item.ItemStack;

public class Category {
    public final String name;
    public final ItemStack icon;
    private final int nameHash;

    public Category(String name, ItemStack icon) {
        this.name = name;
        this.nameHash = name.hashCode();
        this.icon = icon;
    }
    public Category(String name) {
        this(name, null);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return nameHash == category.nameHash;
    }

    @Override
    public int hashCode() {
        return nameHash;
    }
}
