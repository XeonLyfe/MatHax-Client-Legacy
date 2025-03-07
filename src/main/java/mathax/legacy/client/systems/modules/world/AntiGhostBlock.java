package mathax.legacy.client.systems.modules.world;

import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.events.entity.player.BreakBlockEvent;
import mathax.legacy.client.events.entity.player.PlaceBlockEvent;
import mathax.legacy.client.events.game.GameLeftEvent;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.settings.IntSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AntiGhostBlock extends Module {
    private final HashMap<BlockPos, Long> blocks = new HashMap<>();

    private final boolean lock = false;

    private long lastRequest = 0L;

    SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> requestDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between updating block and sending request.")
        .defaultValue(3)
        .min(1)
        .sliderMin(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> sendDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-between")
        .description("Delay between requests.")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .sliderMax(200)
        .build()
    );

    public AntiGhostBlock() {
        super(Categories.World, Items.BARRIER, "anti-ghost-block", "Automatically tries to remove ghost blocks.");
    }

    private void tick(List list, long l, BlockPos blockPos, Long l2) {
        if (list.isEmpty() && l - l2 >= (long)requestDelay.get().intValue()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            list.add(blockPos.asLong());
            lastRequest = l;
        }
    }

    @EventHandler
    private void onBlockBreak(BreakBlockEvent breakBlockEvent) {
        blocks.put(breakBlockEvent.blockPos, mc.world.getTime());
    }

    @EventHandler
    private void onGameDisconnect(GameLeftEvent gameLeftEvent) {
        blocks.clear();
        lastRequest = 0L;
    }

    @EventHandler
    private void onTick(TickEvent.Post post) {
        long l = mc.world.getTime();
        if (blocks.isEmpty() || mc.interactionManager == null || mc.interactionManager.isBreakingBlock() || l - lastRequest < (long)sendDelay.get().intValue() ||
            lock) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        blocks.forEach((argument, argument2) -> tick(arrayList, l, argument, argument2));
        arrayList.forEach(blocks::remove);
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive receive) {
        BlockUpdateS2CPacket blockUpdateS2CPacket;
        if (receive.packet instanceof BlockUpdateS2CPacket && blocks.containsKey((blockUpdateS2CPacket= (BlockUpdateS2CPacket) receive.packet).getPos())) {
            blocks.remove(blockUpdateS2CPacket.getPos());
        }
        if (receive.packet instanceof BlockUpdateS2CPacket && blocks.containsKey((blockUpdateS2CPacket = (BlockUpdateS2CPacket) receive.packet).getPos())) {
            blocks.remove(blockUpdateS2CPacket.getPos());
        }
    }

    @EventHandler
    private void onBlockPlace(PlaceBlockEvent placeBlockEvent) {
        this.blocks.put(placeBlockEvent.blockPos, mc.world.getTime());
    }
}
