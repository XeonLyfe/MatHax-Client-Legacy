package mathax.legacy.client.systems.modules.render;

import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.events.render.Render2DEvent;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import net.minecraft.item.Items;

public class CustomCrosshair extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    //AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA

    public CustomCrosshair() {
        super(Categories.Render, Items.COMPASS, "custom-crosshair", "Renders a customizable crosshair instead of the Minecraft one.");
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        //TODO: Make.
    }
}
