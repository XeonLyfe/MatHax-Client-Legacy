package mathax.legacy.client.systems.modules.world;

import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.misc.Pool;
import mathax.legacy.client.utils.world.BlockIterator;
import mathax.legacy.client.utils.world.BlockUtils;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    private final Pool<BlockPos.Mutable> blockPosPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();

    private boolean firstBlock;

    private int timer;
    private int noBlockTimer;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way the blocks are broken.")
        .defaultValue(Mode.Flatten)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The break range.")
        .defaultValue(5)
        .min(0)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between breaking blocks.")
        .defaultValue(0)
        .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("The blocks you want to mine first.")
        .defaultValue(SortMode.Closest)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client side.")
        .defaultValue(true)
        .build()
    );

    // Whitelist

    private final Setting<Boolean> whitelistEnabled = sgWhitelist.add(new BoolSetting.Builder()
        .name("whitelist-enabled")
        .description("Only mines selected blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The blocks you want to mine.")
        .visible(whitelistEnabled::get)
        .defaultValue(new ArrayList<>(0))
        .build()
    );

    public Nuker() {
        super(Categories.World, Items.DIAMOND_PICKAXE, "nuker", "Breaks blocks around you.");
    }

    @Override
    public void onActivate() {
        firstBlock = true;

        timer = 0;
        noBlockTimer = 0;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Update timer
        if (timer > 0) {
            timer--;
            return;
        }

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double rangeSq = Math.pow(range.get(), 2);

        // Find blocks to break
        BlockIterator.register((int) Math.ceil(range.get()), (int) Math.ceil(range.get()), (blockPos, blockState) -> {
            // Check for air, unbreakable blocks and distance
            if (blockState.getHardness(mc.world, blockPos) < 0 || Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq || blockState.getOutlineShape(mc.world, blockPos) == VoxelShapes.empty()) return;

            // Flatten
            if (mode.get() == Mode.Flatten && blockPos.getY() < mc.player.getY()) return;

            // Smash
            if (mode.get() == Mode.Smash && blockState.getHardness(mc.world, blockPos) != 0) return;

            // Check for selected
            if (whitelistEnabled.get() && !whitelist.get().contains(blockState.getBlock())) return;

            // Add block
            blocks.add(blockPosPool.get().set(blockPos));
        });

        // Break block if found
        BlockIterator.after(() -> {
            // Sort blocks
            if (sortMode.get() != SortMode.None) {
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));
            }

            // Check if some block was found
            if (blocks.isEmpty()) {
                // If no block was found for long enough then set firstBlock flag to true to not wait before breaking another again
                if (noBlockTimer++ >= delay.get()) firstBlock = true;
                return;
            } else {
                noBlockTimer = 0;
            }

            // Update timer
            if (!firstBlock && !lastBlockPos.equals(blocks.get(0))) {
                timer = delay.get();

                firstBlock = false;
                lastBlockPos.set(blocks.get(0));

                if (timer > 0) return;
            }

            // Break
            BlockUtils.breakBlock(blocks.get(0), swingHand.get());

            firstBlock = false;
            lastBlockPos.set(blocks.get(0));

            // Clear current block positions
            for (BlockPos.Mutable blockPos : blocks) blockPosPool.free(blockPos);
            blocks.clear();
        });
    }

    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest
    }
}
