package mathax.legacy.client.systems.modules.render;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.render.Render3DEvent;
import mathax.legacy.client.renderer.ShapeMode;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.entity.ProjectileEntitySimulator;
import mathax.legacy.client.utils.misc.Pool;
import mathax.legacy.client.utils.misc.Vec3;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {
    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();

    private final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
    private final List<Path> paths = new ArrayList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to display trajectories for.")
        .defaultValue(getDefaultItems())
        .filter(this::itemFilter)
        .build()
    );

    private final Setting<Boolean> otherPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("other-players")
        .description("Calculates trajectories for other players.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 35))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    public Trajectories() {
        super(Categories.Render, Items.ENDER_PEARL, "trajectories", "Predicts the trajectory of throwable items.");
    }

    private boolean itemFilter(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof FishingRodItem || item instanceof TridentItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem;
    }

    private List<Item> getDefaultItems() {
        List<Item> items = new ArrayList<>();

        for (Item item : Registry.ITEM) {
            if (itemFilter(item)) items.add(item);
        }

        return items;
    }

    private Path getEmptyPath() {
        for (Path path : paths) {
            if (path.points.isEmpty()) return path;
        }

        Path path = new Path();
        paths.add(path);
        return path;
    }

    private void calculatePath(PlayerEntity player, double tickDelta) {
        // Clear paths
        for (Path path : paths) path.clear();

        // Get item
        ItemStack itemStack = player.getMainHandStack();
        if (itemStack == null) itemStack = player.getOffHandStack();
        if (itemStack == null) return;
        if (!items.get().contains(itemStack.getItem())) return;

        // Calculate paths
        if (!simulator.set(player, itemStack, 0, false, tickDelta)) return;
        getEmptyPath().calculate();

        if (itemStack.getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(Enchantments.MULTISHOT, itemStack) > 0) {
            if (!simulator.set(player, itemStack, -10, false, tickDelta)) return;
            getEmptyPath().calculate();

            if (!simulator.set(player, itemStack, 10, false, tickDelta)) return;
            getEmptyPath().calculate();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!otherPlayers.get() && player != mc.player) continue;

            calculatePath(player, event.tickDelta);
            for (Path path : paths) path.render(event);
        }
    }

    private class Path {
        private final List<Vec3> points = new ArrayList<>();

        private boolean hitQuad, hitQuadHorizontal;
        private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;

        private Entity entity;

        public void clear() {
            for (Vec3 point : points) vec3s.free(point);
            points.clear();

            hitQuad = false;
            entity = null;
        }

        public void calculate() {
            addPoint();

            while (true) {
                HitResult result = simulator.tick();

                if (result != null) {
                    processHitResult(result);
                    break;
                }

                addPoint();
            }
        }

        private void addPoint() {
            points.add(vec3s.get().set(simulator.pos));
        }

        private void processHitResult(HitResult result) {
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult r = (BlockHitResult) result;

                hitQuad = true;
                hitQuadX1 = r.getPos().x;
                hitQuadY1 = r.getPos().y;
                hitQuadZ1 = r.getPos().z;
                hitQuadX2 = r.getPos().x;
                hitQuadY2 = r.getPos().y;
                hitQuadZ2 = r.getPos().z;

                if (r.getSide() == Direction.UP || r.getSide() == Direction.DOWN) {
                    hitQuadHorizontal = true;
                    hitQuadX1 -= 0.25;
                    hitQuadZ1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadZ2 += 0.25;
                }
                else if (r.getSide() == Direction.NORTH || r.getSide() == Direction.SOUTH) {
                    hitQuadHorizontal = false;
                    hitQuadX1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadY2 += 0.25;
                }
                else {
                    hitQuadHorizontal = false;
                    hitQuadZ1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadZ2 += 0.25;
                    hitQuadY2 += 0.25;
                }

                points.add(vec3s.get().set(result.getPos()));
            } else if (result.getType() == HitResult.Type.ENTITY) {
                entity = ((EntityHitResult) result).getEntity();

                points.add(vec3s.get().set(result.getPos()).add(0, entity.getHeight() / 2, 0));
            }
        }

        public void render(Render3DEvent event) {
            // Render path
            Vec3 lastPoint = null;

            for (Vec3 point : points) {
                if (lastPoint != null) event.renderer.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, lineColor.get());
                lastPoint = point;
            }

            // Render hit quad
            if (hitQuad) {
                if (hitQuadHorizontal) event.renderer.sideHorizontal(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX1 + 0.5, hitQuadZ1 + 0.5, sideColor.get(), lineColor.get(), shapeMode.get());
                else event.renderer.sideVertical(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2, sideColor.get(), lineColor.get(), shapeMode.get());
            }

            // Render entity
            if (entity != null) {
                double x = (entity.getX() - entity.prevX) * event.tickDelta;
                double y = (entity.getY() - entity.prevY) * event.tickDelta;
                double z = (entity.getZ() - entity.prevZ) * event.tickDelta;

                Box box = entity.getBoundingBox();
                event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
