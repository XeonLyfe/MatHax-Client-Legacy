package mathax.legacy.client.systems.waypoints;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.game.GameJoinedEvent;
import mathax.legacy.client.events.game.GameLeftEvent;
import mathax.legacy.client.events.render.Render2DEvent;
import mathax.legacy.client.renderer.Renderer2D;
import mathax.legacy.client.renderer.text.TextRenderer;
import mathax.legacy.client.systems.System;
import mathax.legacy.client.systems.Systems;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.render.WaypointsModule;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.files.StreamUtils;
import mathax.legacy.client.utils.misc.NbtUtils;
import mathax.legacy.client.utils.misc.Vec3;
import mathax.legacy.client.utils.player.PlayerUtils;
import mathax.legacy.client.utils.render.NametagUtils;
import mathax.legacy.client.utils.render.color.Color;
import mathax.legacy.client.utils.world.Dimension;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.eventbus.EventPriority;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Waypoints extends System<Waypoints> implements Iterable<Waypoint> {
    private static final String[] BUILTIN_ICONS = {"square", "circle", "triangle", "star", "diamond", "skull"};

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    public final Map<String, AbstractTexture> icons = new HashMap<>();

    private List<Waypoint> waypoints = new ArrayList<>();
    private final Vec3 pos = new Vec3();

    public Waypoints() {
        super(null);
    }

    public static Waypoints get() {
        return Systems.get(Waypoints.class);
    }

    @Override
    public void init() {
        File iconsFolder = new File(MatHaxLegacy.FOLDER, "WayPoint-Icons");
        iconsFolder.mkdirs();

        for (String builtinIcon : BUILTIN_ICONS) {
            File iconFile = new File(iconsFolder, builtinIcon + ".png");
            if (!iconFile.exists()) copyIcon(iconFile);
        }

        File[] files = iconsFolder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".png")) {
                try {
                    String name = file.getName().replace(".png", "");
                    AbstractTexture texture = new NativeImageBackedTexture(NativeImage.read(new FileInputStream(file)));
                    icons.put(name, texture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public void remove(Waypoint waypoint) {
        if (waypoints.remove(waypoint)) {
            save();
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        load();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onGameDisconnected(GameLeftEvent event) {
        waypoints.clear();
    }

    private boolean checkDimension(Waypoint waypoint) {
        Dimension dimension = PlayerUtils.getDimension();

        if (waypoint.overworld && dimension == Dimension.Overworld) return true;
        if (waypoint.nether && dimension == Dimension.Nether) return true;
        return waypoint.end && dimension == Dimension.End;
    }

    public Vec3d getCoords(Waypoint waypoint) {
        double x = waypoint.x;
        double y = waypoint.y;
        double z = waypoint.z;

        if (waypoint.actualDimension == Dimension.Overworld && PlayerUtils.getDimension() == Dimension.Nether) {
            x = waypoint.x / 8f;
            z = waypoint.z / 8f;
        } else if (waypoint.actualDimension == Dimension.Nether && PlayerUtils.getDimension() == Dimension.Overworld) {
            x = waypoint.x * 8;
            z = waypoint.z * 8;
        }

        return new Vec3d(x, y, z);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!Modules.get().isActive(WaypointsModule.class)) return;

        TextRenderer text = TextRenderer.get();

        for (Waypoint waypoint : this) {
            if (!waypoint.visible || !checkDimension(waypoint)) continue;

            Camera camera = Utils.mc.gameRenderer.getCamera();

            double x = getCoords(waypoint).x;
            double y = getCoords(waypoint).y;
            double z = getCoords(waypoint).z;

            // Compute scale
            double dist = PlayerUtils.distanceToCamera(x, y, z);
            if (dist > waypoint.maxVisibleDistance) continue;
            double scale = /*0.01 * */waypoint.scale;
            //if(dist > 8) scale *= dist / 8;

            double a = 1;
            if (dist < 10) {
                a = dist / 10;
                if (a < 0.1) continue;
            }

            double maxViewDist = Utils.mc.options.viewDistance * 16;
            if (dist > maxViewDist) {
                double dx = x - camera.getPos().x;
                double dy = y - camera.getPos().y;
                double dz = z - camera.getPos().z;

                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= length;
                dy /= length;
                dz /= length;

                dx *= maxViewDist;
                dy *= maxViewDist;
                dz *= maxViewDist;

                x = camera.getPos().x + dx;
                y = camera.getPos().y + dy;
                z = camera.getPos().z + dz;

                scale /= dist / 15;
                scale *= maxViewDist / 15;
            }

            // Setup the rotation
            pos.set(x, y, z);
            if (!NametagUtils.to2D(pos, scale)) continue;

            NametagUtils.begin(pos);

            int preBgA = BACKGROUND.a;
            int preTextA = TEXT.a;
            BACKGROUND.a *= a;
            TEXT.a *= a;

            String distText = Math.round(dist) + " blocks";

            // Render background
            text.beginBig();
            double w = text.getWidth(waypoint.name) / 2.0;
            double w2 = text.getWidth(distText) / 2.0;
            double h = text.getHeight();

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-w, -h, w * 2, h * 2, BACKGROUND);
            Renderer2D.COLOR.render(null);

            waypoint.renderIcon(-16, h + 1, a, 32);

            // Render name text
            text.render(waypoint.name, -w, -h + 1, TEXT);
            text.render(distText, -w2, 0, TEXT);

            text.end();
            NametagUtils.end();

            BACKGROUND.a = preBgA;
            TEXT.a = preTextA;
        }
    }

    @Override
    public File getFile() {
        if (!Utils.canUpdate()) return null;
        return new File(new File(MatHaxLegacy.VERSION_FOLDER, "WayPoints"), Utils.getWorldName() + ".nbt");
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("waypoints", NbtUtils.listToTag(waypoints));
        return tag;
    }

    @Override
    public Waypoints fromTag(NbtCompound tag) {
        waypoints = NbtUtils.listFromTag(tag.getList("waypoints", 10), tag1 -> new Waypoint().fromTag((NbtCompound) tag1));

        return this;
    }

    @Override
    public Iterator<Waypoint> iterator() {
        return waypoints.iterator();
    }

    public ListIterator<Waypoint> iteratorReverse() {
        return waypoints.listIterator(waypoints.size());
    }

    private void copyIcon(File file) {
        StreamUtils.copy(Waypoints.class.getResourceAsStream("/assets/mathaxlegacy/textures/icons/waypoints/" + file.getName()), file);
    }
}
