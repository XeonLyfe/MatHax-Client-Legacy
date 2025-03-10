package mathax.legacy.client.systems.waypoints;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.renderer.GL;
import mathax.legacy.client.renderer.Renderer2D;
import mathax.legacy.client.utils.misc.ISerializable;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.utils.world.Dimension;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public class Waypoint implements ISerializable<Waypoint> {
    public String name = "popbob's base";
    public String icon = "Square";
    public SettingColor color = new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b);

    public int x, y, z;

    public boolean visible = true;
    public int maxVisibleDistance = 1000;
    public double scale = 1;

    public boolean overworld, nether, end;

    public Dimension actualDimension;

    public void validateIcon() {
        Map<String, AbstractTexture> icons = Waypoints.get().icons;

        AbstractTexture texture = icons.get(icon);
        if (texture == null && !icons.isEmpty()) {
            icon = icons.keySet().iterator().next();
        }
    }

    public void renderIcon(double x, double y, double a, double size) {
        validateIcon();

        AbstractTexture texture = Waypoints.get().icons.get(icon);
        if (texture == null) return;

        int preA = color.a;
        color.a *= a;

        GL.bindTexture(texture.getGlId());
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, size, size, color);
        Renderer2D.TEXTURE.render(null);

        color.a = preA;
    }

    private int findIconIndex() {
        int i = 0;
        for (String icon : Waypoints.get().icons.keySet()) {
            if (this.icon.equals(icon)) return i;
            i++;
        }

        return -1;
    }

    private int correctIconIndex(int i) {
        if (i < 0) return Waypoints.get().icons.size() + i;
        else if (i >= Waypoints.get().icons.size()) return i - Waypoints.get().icons.size();
        return i;
    }

    private String getIcon(int i) {
        i = correctIconIndex(i);

        int _i = 0;
        for (String icon : Waypoints.get().icons.keySet()) {
            if (_i == i) return icon;
            _i++;
        }

        return "Square";
    }

    public void prevIcon() {
        icon = getIcon(findIconIndex() - 1);
    }

    public void nextIcon() {
        icon = getIcon(findIconIndex() + 1);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putString("icon", icon);
        tag.put("color", color.toTag());

        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);

        tag.putBoolean("visible", visible);
        tag.putInt("maxVisibleDistance", maxVisibleDistance);
        tag.putDouble("scale", scale);

        tag.putString("dimension", actualDimension.name());

        tag.putBoolean("overworld", overworld);
        tag.putBoolean("nether", nether);
        tag.putBoolean("end", end);

        return tag;
    }

    @Override
    public Waypoint fromTag(NbtCompound tag) {
        name = tag.getString("name");
        icon = tag.getString("icon");
        color.fromTag(tag.getCompound("color"));

        x = tag.getInt("x");
        y = tag.getInt("y");
        z = tag.getInt("z");

        visible = tag.getBoolean("visible");
        maxVisibleDistance = tag.getInt("maxVisibleDistance");
        scale = tag.getDouble("scale");

        actualDimension = Dimension.valueOf(tag.getString("dimension"));

        overworld = tag.getBoolean("overworld");
        nether = tag.getBoolean("nether");
        end = tag.getBoolean("end");

        if (!Waypoints.get().icons.containsKey(icon)) icon = "Square";

        return this;
    }
}
