package mathax.legacy.client.systems.modules.render;

import mathax.legacy.client.events.world.ChunkOcclusionEvent;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.settings.*;
import net.minecraft.block.Block;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class WallHack extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> opacity = sgGeneral.add(new IntSetting.Builder()
        .name("opacity")
        .description("The opacity for rendered blocks.")
        .defaultValue(100)
        .min(1)
        .max(255)
        .sliderMax(255)
        .onChanged(onChanged -> {
            if(this.isActive()) {
                mc.worldRenderer.reload();
            }
        })
        .build()
    );

    public final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks should be targeted for Wall Hack.")
        .defaultValue(new ArrayList<>())
        .onChanged(onChanged -> {
            if(this.isActive()) {
                mc.worldRenderer.reload();
            }
        })
        .build()
    );

    public final Setting<Boolean> occludeChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("occlude-chunks")
        .description("Whether caves should occlude underground (may look wonky when on).")
        .defaultValue(false)
        .build()
    );

    public WallHack() {
        super(Categories.Render, Items.BARRIER, "wall-hack", "Makes blocks translucent.");
    }

    @Override
    public void onActivate() {
        if (Modules.get().isActive(Xray.class)) {
            error("Xray was enabled while enabling Wallhack, disabling Xray...");
            Modules.get().get(Xray.class).toggle();
        }
        mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        mc.worldRenderer.reload();
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        if(!occludeChunks.get()) {
            event.cancel();
        }
    }
}
