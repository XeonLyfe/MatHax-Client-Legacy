package mathax.legacy.client.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import com.google.common.collect.Lists;
import mathax.legacy.client.events.game.GameJoinedEvent;
import mathax.legacy.client.events.game.GameLeftEvent;
import mathax.legacy.client.events.mathax.ActiveModulesChangedEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.systems.modules.combat.AutoLog;
import mathax.legacy.client.systems.modules.movement.GUIMove;
import mathax.legacy.client.systems.modules.movement.Jesus;
import mathax.legacy.client.systems.modules.movement.NoFall;
import mathax.legacy.client.systems.modules.player.AutoEat;
import mathax.legacy.client.systems.modules.player.AutoTool;
import mathax.legacy.client.systems.modules.player.NoBreakDelay;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.player.AntiHunger;
import mathax.legacy.client.settings.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;

import java.util.HashMap;
import java.util.List;

public class InfinityMiner extends Module {
    private final HashMap<String, Boolean> originalSettings = new HashMap<>();

    private volatile Boolean BLOCKER = false;
    private boolean baritoneRunning = false;

    private Mode currentMode = Mode.Still;
    private Mode secondaryMode;


    private int playerX;
    private int playerY;
    private int playerZ;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoToggles = settings.createGroup("Auto Toggles");
    private final SettingGroup sgExtras = settings.createGroup("Extras");

    public final Setting<Block> targetBlock = sgGeneral.add(new BlockSetting.Builder()
        .name("target-block")
        .description("The target block to mine.")
        .defaultValue(Blocks.ANCIENT_DEBRIS)
        .filter(this::filter)
        .build()
    );

    public final Setting<Block> repairBlock = sgGeneral.add(new BlockSetting.Builder()
        .name("repair-block")
        .description("The block mined to repair your pickaxe.")
        .defaultValue(Blocks.NETHER_QUARTZ_ORE)
        .filter(this::filter)
        .build()
    );

    public final Setting<Double> durabilityThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("durability-threshold")
        .description("The durability at which to start repairing as a percent of maximum durability.")
        .defaultValue(.15)
        .max(.95)
        .min(.05)
        .sliderMin(.05)
        .sliderMax(.95)
        .build()
    );

    public final Setting<Boolean> smartModuleToggle = sgAutoToggles.add(new BoolSetting.Builder()
        .name("smart-module-toggle")
        .description("Will automatically enable helpful modules.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> autoWalkHome = sgExtras.add(new BoolSetting.Builder()
        .name("walk-home")
        .description("Will walk 'home' when your inventory is full.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> autoLogOut = sgExtras.add(new BoolSetting.Builder()
        .name("log-out")
        .description("Logs out when your inventory is full. Will walk home FIRST if walk home is enabled.")
        .defaultValue(false)
        .build()
    );

    public InfinityMiner() {
        super(Categories.World, Items.DIAMOND_PICKAXE, "infinity-miner", "Allows you to essentially mine forever.");
    }

    private boolean filter(Block block) {
        return block != Blocks.AIR && block.getDefaultState().getHardness(mc.world, null) != -1 && !(block instanceof FluidBlock);
    }

    @Override
    public void onActivate() {
        if (smartModuleToggle.get()) {
            BLOCKER = true;
            for (Module module : getToggleModules()) {
                originalSettings.put(module.name, module.isActive());
                if (!module.isActive()) module.toggle();
            }
            BLOCKER = false;
        }
        if (mc.player != null) {
            playerX = (int) mc.player.getX();
            playerY = (int) mc.player.getY();
            playerZ = (int) mc.player.getZ();
        }
    }

    @Override
    public void onDeactivate() {
        if (smartModuleToggle.get()) {
            BLOCKER = true;
            try {
                for (Module module : getToggleModules()) {
                    if (module != null && originalSettings.get(module.name) != module.isActive()) {
                        module.toggle();
                    }
                }
            } catch (NullPointerException ignored) {
            }
            originalSettings.clear();
            BLOCKER = false;
        }
        if (!BaritoneAPI.getSettings().mineScanDroppedItems.value)
            BaritoneAPI.getSettings().mineScanDroppedItems.value = true;
        baritoneRequestStop();
        baritoneRunning = false;
        currentMode = Mode.Still;
        secondaryMode = null;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (mc.player == null) return;
            if (!baritoneRunning && currentMode == Mode.Still) {
                if (autoWalkHome.get() && isInventoryFull() && secondaryMode != Mode.Home) {
                    baritoneRequestPathHome();
                    return;
                }
                currentMode = (isTool() && getCurrentDamage() <= durabilityThreshold.get()) ? Mode.Repair : Mode.Target;
                if (currentMode == Mode.Repair) baritoneRequestMineRepairBlock();
                else baritoneRequestMineTargetBlock();
            } else if (autoWalkHome.get() && isInventoryFull() && secondaryMode != Mode.Home)
                baritoneRequestPathHome();
            else if (!autoWalkHome.get() && isInventoryFull() && autoLogOut.get()) {
                toggle();
                requestLogout(currentMode);
            } else if (currentMode == Mode.Repair) {
                int REPAIR_BUFFER = 15;
                if (BaritoneAPI.getSettings().mineScanDroppedItems.value)
                    BaritoneAPI.getSettings().mineScanDroppedItems.value = false;
                if (isTool() && getCurrentDamage() >= mc.player.getMainHandStack().getMaxDamage() - REPAIR_BUFFER) {
                    if (secondaryMode != Mode.Home) {
                        currentMode = Mode.Target;
                        baritoneRequestMineTargetBlock();
                    } else {
                        currentMode = Mode.Home;
                        baritoneRequestPathHome();
                    }
                }
            } else if (currentMode == Mode.Target) {
                if (!BaritoneAPI.getSettings().mineScanDroppedItems.value)
                    BaritoneAPI.getSettings().mineScanDroppedItems.value = true;
                if (isTool() && getCurrentDamage() <= durabilityThreshold.get() * mc.player.getMainHandStack().getMaxDamage()) {
                    currentMode = Mode.Repair;
                    baritoneRequestMineRepairBlock();
                } else if (autoWalkHome.get() && isInventoryFull()) baritoneRequestPathHome();
                else if (!autoWalkHome.get() && isInventoryFull() && autoWalkHome.get()) requestLogout(currentMode);
            } else if (currentMode == Mode.Home) {
                if (Math.abs(mc.player.getY() - playerY) <= .5 && Math.abs(mc.player.getX() - playerX) <= .5 && Math.abs(mc.player.getZ() - playerZ) <= .5) {
                    if (autoLogOut.get()) requestLogout(currentMode);
                    toggle();
                } else if (isTool() && getCurrentDamage() <= durabilityThreshold.get()) currentMode = Mode.Repair;
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void moduleChange(ActiveModulesChangedEvent event) {
        if (!BLOCKER) {
            for (Module module : getToggleModules()) {
                if (module != null && !module.isActive()) originalSettings.remove(module.name);
            }
        }
    }

    private void baritoneRequestMineTargetBlock() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(targetBlock.get());
            baritoneRunning = true;
        } catch (Exception ignored) {
        }
    }

    private void baritoneRequestMineRepairBlock() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(repairBlock.get());
            baritoneRunning = true;
        } catch (Exception ignored) {
        }
    }

    private void baritoneRequestStop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        baritoneRunning = false;
        currentMode = Mode.Still;
    }

    private void baritoneRequestPathHome() {
        if (autoWalkHome.get()) {
            baritoneRequestStop();
            secondaryMode = Mode.Home;
            currentMode = Mode.Home;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(playerX, playerY, playerZ));
        }
    }

    private Boolean isInventoryFull() {
        if (mc.player == null) return false;
        if (mc.player.getInventory().getEmptySlot() != -1) return false;
        for (int i = 0; i < mc.player.getInventory().size(); i++)
            if (mc.player.getInventory().getStack(i).getItem() == targetBlock.get().asItem() &&
                    mc.player.getInventory().getStack(i).getCount() < targetBlock.get().asItem().getMaxCount()) return false;
        return true;
    }

    private List<Module> getToggleModules() {
        return Lists.newArrayList(
                Modules.get().get(Jesus.class),
                Modules.get().get(NoBreakDelay.class),
                Modules.get().get(AntiHunger.class),
                Modules.get().get(AutoEat.class),
                Modules.get().get(NoFall.class),
                Modules.get().get(AutoLog.class),
                Modules.get().get(AutoTool.class),
                Modules.get().get(GUIMove.class)
        );
    }

    private void requestLogout(Mode mode) {
        if (mc.player != null) {
            if (mode == Mode.Home)
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[Infinity Miner] Inventory is Full and You Are Home")));
            else
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[Infinity Miner] Inventory is Full")));
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onGameDisconnect(GameLeftEvent event) {
        baritoneRequestStop();
        if (!BaritoneAPI.getSettings().mineScanDroppedItems.value)
            BaritoneAPI.getSettings().mineScanDroppedItems.value = true;
        if (this.isActive()) toggle();
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        baritoneRequestStop();
        if (!BaritoneAPI.getSettings().mineScanDroppedItems.value)
            BaritoneAPI.getSettings().mineScanDroppedItems.value = true;
        if (this.isActive()) toggle();
    }

    public Mode getMode() {
        return currentMode;
    }

    public Block getCurrentTarget() {
        return (currentMode == Mode.Repair) ? repairBlock.get() : targetBlock.get();
    }

    public int[] getHomeCoords() {
        return new int[]{playerX, playerY, playerX};
    }

    private boolean isTool() {
        return mc.player != null && mc.player.getMainHandStack() != null && mc.player.getMainHandStack().getItem() instanceof ToolItem;
    }

    private int getCurrentDamage() {
        return (mc.player != null) ? mc.player.getMainHandStack().getItem().getMaxDamage() - mc.player.getMainHandStack().getDamage() : -1;
    }

    @Override
    public String getInfoString() {
        switch (getMode()) {
            case Home:
                int[] coords = getHomeCoords();
                return "Heading Home: " + coords[0] + " " + coords[1] + " " + coords[2];
            case Target:
                return "Mining: " + getCurrentTarget().getName().getString();
            case Repair:
                return "Repair-Mining: " + getCurrentTarget().getName().getString();
            case Still:
                return "Resting";
            default:
                return "";
        }
    }

    public enum Mode {
        Target,
        Repair,
        Still,
        Home
    }
}
