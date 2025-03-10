package mathax.legacy.client.utils.network;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.misc.CapesModule;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

import static mathax.legacy.client.utils.Utils.mc;

public class Capes {
    public static final Map<UUID, String> OWNERS = new HashMap<>();
    private static final Map<String, String> URLS = new HashMap<>();
    private static final Map<String, Cape> TEXTURES = new HashMap<>();

    private static final List<Cape> TO_REGISTER = new ArrayList<>();
    private static final List<Cape> TO_RETRY = new ArrayList<>();
    private static final List<Cape> TO_REMOVE = new ArrayList<>();

    public static void init() {
        OWNERS.clear();
        URLS.clear();
        TEXTURES.clear();
        TO_REGISTER.clear();
        TO_RETRY.clear();
        TO_REMOVE.clear();

        if (Modules.get().isActive(CapesModule.class)) {
            MatHaxExecutor.execute(() -> {
                // Cape owners
                Stream<String> lines = HTTP.get(MatHaxLegacy.API_URL + "Cape/capeowners").sendLines();
                if (lines != null) lines.forEach(s -> {
                    String[] split = s.split(" ");

                    if (split.length >= 2) {
                        OWNERS.put(UUID.fromString(split[0]), split[1]);
                        if (!TEXTURES.containsKey(split[1])) TEXTURES.put(split[1], new Cape(split[1]));
                    }
                });

                // Capes
                lines = HTTP.get(MatHaxLegacy.API_URL + "Cape/capes").sendLines();
                if (lines != null) lines.forEach(s -> {
                    String[] split = s.split(" ");

                    if (split.length >= 2) {
                        if (!URLS.containsKey(split[0])) URLS.put(split[0], split[1]);
                    }
                });
            });
        }
    }

    public static void disable() {
        OWNERS.clear();
        URLS.clear();
        TEXTURES.clear();
        TO_REGISTER.clear();
        TO_RETRY.clear();
        TO_REMOVE.clear();
    }

    public static Identifier get(PlayerEntity player) {
        if (Modules.get().isActive(CapesModule.class)) {
            String capeName = OWNERS.get(player.getUuid());
            if (capeName != null) {
                Cape cape = TEXTURES.get(capeName);
                if (cape == null) return null;

                if (cape.isDownloaded()) return cape;

                cape.download();
                return null;
            }

            return null;
        }
        return null;
    }

    public static void tick() {
        if (Modules.get().isActive(CapesModule.class)) {
            synchronized (TO_REGISTER) {
                for (Cape cape : TO_REGISTER) cape.register();
                TO_REGISTER.clear();
            }

            synchronized (TO_RETRY) {
                TO_RETRY.removeIf(Cape::tick);
            }

            synchronized (TO_REMOVE) {
                for (Cape cape : TO_REMOVE) {
                    URLS.remove(cape.name);
                    TEXTURES.remove(cape.name);
                    TO_REGISTER.remove(cape);
                    TO_RETRY.remove(cape);
                }

                TO_REMOVE.clear();
            }
        }
    }

    private static class Cape extends Identifier {

        private static int COUNT = 0;

        private final String name;

        private boolean downloaded;
        private boolean downloading;

        private NativeImage img;

        private int retryTimer;

        public Cape(String name) {
            super("mathaxlegacy", "capes/" + COUNT++);

            this.name = name;
        }

        public void download() {
            if (Modules.get().isActive(CapesModule.class)) {
                if (downloaded || downloading || retryTimer > 0) return;
                downloading = true;

                MatHaxExecutor.execute(() -> {
                    try {
                        String url = URLS.get(name);
                        if (url == null) {
                            synchronized (TO_RETRY) {
                                TO_REMOVE.add(this);
                                downloading = false;
                                return;
                            }
                        }

                        InputStream in = HTTP.get(url).sendInputStream();
                        if (in == null) {
                            synchronized (TO_RETRY) {
                                TO_RETRY.add(this);
                                retryTimer = 10 * 20;
                                downloading = false;
                                return;
                            }
                        }

                        img = NativeImage.read(in);

                        synchronized (TO_REGISTER) {
                            TO_REGISTER.add(this);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        public void register() {
            if (Modules.get().isActive(CapesModule.class)) {
                mc.getTextureManager().registerTexture(this, new NativeImageBackedTexture(img));
                img = null;

                downloading = false;
                downloaded = true;
            }
        }

        public boolean tick() {
            if (retryTimer > 0) {
                retryTimer--;
            } else {
                download();
                return true;
            }

            return false;
        }

        public boolean isDownloaded() {
            return downloaded;
        }
    }
}
