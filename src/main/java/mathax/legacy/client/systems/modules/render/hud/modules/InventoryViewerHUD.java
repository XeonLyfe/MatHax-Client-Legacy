package mathax.legacy.client.systems.modules.render.hud.modules;

import mathax.legacy.client.gui.GuiThemes;
import mathax.legacy.client.renderer.GL;
import mathax.legacy.client.renderer.Renderer2D;
import mathax.legacy.client.systems.modules.render.hud.HUD;
import mathax.legacy.client.systems.modules.render.hud.HUDElement;
import mathax.legacy.client.systems.modules.render.hud.HUDRenderer;
import mathax.legacy.client.utils.render.RenderUtils;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.settings.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class InventoryViewerHUD extends HUDElement {
    private final ItemStack[] editorInv;

    private static final Identifier TEXTURE = new Identifier("mathaxlegacy", "textures/container/container.png");
    private static final Identifier TEXTURE_TRANSPARENT = new Identifier("mathaxlegacy", "textures/container/container-transparent.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderMin(1).sliderMax(5)
        .build()
    );

    private final Setting<Background> background = sgGeneral.add(new EnumSetting.Builder<Background>()
        .name("background")
        .description("Background of inventory viewer.")
        .defaultValue(Background.Texture)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> background.get() != Background.None)
        .build()
    );

    public InventoryViewerHUD(HUD hud) {
        super(hud, "inventory-viewer", "Displays your inventory", true);

        editorInv = new ItemStack[9 * 3];
        editorInv[0] = Items.TOTEM_OF_UNDYING.getDefaultStack();
        editorInv[5] = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 6);
        editorInv[19] = new ItemStack(Items.OBSIDIAN, 64);
        editorInv[editorInv.length - 1] = Items.NETHERITE_AXE.getDefaultStack();
    }

    @Override
    public void update(HUDRenderer renderer) {
        box.setSize(background.get().width * scale.get(), background.get().height * scale.get());
    }

    @Override
    public void render(HUDRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (background.get() != Background.None) {
            drawBackground((int) x, (int) y);
        }

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = getStack(9 + row * 9 + i);
                if (stack == null) continue;

                int itemX = background.get() == Background.Texture ? (int) (x + (8 + i * 18) * scale.get()) : (int) (x + (1 + i * 18) * scale.get());
                int itemY = background.get() == Background.Texture ? (int) (y + (7 + row * 18) * scale.get()) : (int) (y + (1 + row * 18) * scale.get());

                RenderUtils.drawItem(stack, itemX, itemY, scale.get(), true);
            }
        }
    }

    private ItemStack getStack(int i) {
        if (isInEditor()) return editorInv[i - 9];
        return mc.player.getInventory().getStack(i);
    }

    private void drawBackground(int x, int y) {
        int w = (int) box.width;
        int h = (int) box.height;

        switch (background.get()) {
            case Texture, Outline -> {
                GL.bindTexture(background.get() == Background.Texture ? TEXTURE : TEXTURE_TRANSPARENT);

                Renderer2D.TEXTURE.begin();
                Renderer2D.TEXTURE.texQuad(x, y, w, h, color.get());
                Renderer2D.TEXTURE.render(null);
            }
            case Flat -> {
                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quadRounded(x, y, w, h, color.get(), GuiThemes.get().roundAmount(), true);
                Renderer2D.COLOR.render(null);
            }
        }
    }

    public enum Background {
        None(162, 54),
        Texture(176, 67),
        Outline(162, 54),
        Flat(162, 54);

        private int width, height;

        Background(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
