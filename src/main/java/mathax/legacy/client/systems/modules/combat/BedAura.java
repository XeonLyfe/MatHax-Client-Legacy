package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.render.Render3DEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.renderer.ShapeMode;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.world.AntiGhostBlock;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.entity.EntityUtils;
import mathax.legacy.client.utils.entity.SortPriority;
import mathax.legacy.client.utils.entity.TargetUtils;
import mathax.legacy.client.utils.player.*;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.utils.world.BlockUtils;
import mathax.legacy.client.utils.world.CardinalDirection;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BedAura extends Module {
    private CardinalDirection direction;

    private PlayerEntity target;

    private BlockPos placePos, breakPos;

    private Item slotItem;

    private boolean safetyToggled;

    private int timer;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgAutoMove = settings.createGroup("Inventory");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");
    // General

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between placing beds in ticks.")
        .defaultValue(7)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
        .name("strict-direction")
        .description("Only places beds in the directions specified.")
        .defaultValue(false)
        .build()
    );

    // Targeting

    public final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The range at which players can be targeted.")
        .defaultValue(4)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<SortPriority> targetPriority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to filter the players to target.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Boolean> targetFeet = sgTargeting.add(new BoolSetting.Builder()
        .name("target-feet")
        .description("Targets player feet.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> minDamage = sgTargeting.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("The minimum damage to inflict on your target.")
        .defaultValue(7)
        .min(0).max(36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgTargeting.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("The maximum damage to inflict on yourself.")
        .defaultValue(7)
        .min(0).max(36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgTargeting.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break beds if they will kill you.")
        .defaultValue(true)
        .build()
    );

    // Auto move

    private final Setting<Boolean> autoMove = sgAutoMove.add(new BoolSetting.Builder()
        .name("auto-move")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> autoMoveSlot = sgAutoMove.add(new IntSetting.Builder()
        .name("auto-move-slot")
        .description("The slot auto move moves beds to.")
        .defaultValue(9)
        .min(1).max(9)
        .sliderMin(1).sliderMax(9)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgAutoMove.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to and from beds automatically.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> restoreOnDisable = sgAutoMove.add(new BoolSetting.Builder()
        .name("restore-on-disable")
        .description("Put whatever was in your auto move slot back after disabling.")
        .defaultValue(true)
        .build()
    );

    // Automation

    private final Setting<Boolean> disableOnNoBeds = sgAutomation.add(new BoolSetting.Builder()
        .name("disable-on-no-beds")
        .description("Disable if you run out of beds.")
        .defaultValue(true)
        .build()
    );

    // Safety

    private final Setting<Boolean> disableOnSafety = sgSafety.add(new BoolSetting.Builder()
        .name("disable-on-safety")
        .description("Disable BedAura+ when safety activates.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> safetyHP = sgSafety.add(new DoubleSetting.Builder()
        .name("safety-hp")
        .description("What health safety activates at.")
        .defaultValue(7)
        .min(1)
        .max(36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Boolean> safetyGapSwap = sgSafety.add(new BoolSetting.Builder()
        .name("swap-to-gap")
        .description("Swap to egaps after activating safety.")
        .defaultValue(false)
        .build()
    );

    // Pause

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses while eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses while drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses while mining.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Whether to swing hand clientside clientside.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the block where it is placing a bed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color for positions to be placed.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b,75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color for positions to be placed.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    public BedAura() {
        super(Categories.Combat, Items.RED_BED, "bed-aura", "Automatically places and explodes beds in the Nether and the End.");
    }

    @Override
    public void onActivate() {
        timer = delay.get();
        direction = CardinalDirection.North;
        safetyToggled = false;
        slotItem = InvUtils.getItemFromSlot(autoMoveSlot.get() - 1);
        if (slotItem instanceof BedItem) slotItem = null;
    }

    @Override
    public void onDeactivate() {
        target = null;
        placePos = null;
        breakPos = null;

        if (safetyToggled) {
            warning("Your health is too low!");
            if (safetyGapSwap.get()) {
                FindItemResult gap = InvUtils.findEgap();
                if (gap.found()) mc.player.getInventory().selectedSlot = gap.getSlot();
            }
        }

        if (!safetyToggled && restoreOnDisable.get() && slotItem != null) {
            FindItemResult slotItemInv = InvUtils.find(slotItem);
            if (slotItemInv.found()) InvUtils.move().from(slotItemInv.getSlot()).toHotbar(autoMoveSlot.get() - 1);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Check if beds can explode
        if (mc.world.getDimension().isBedWorking()) {
            error("You can't blow up beds in this dimension, disabling...");
            toggle();
            return;
        }

        // Safety
        if (PlayerUtils.getTotalHealth() <= safetyHP.get()) {
            if (disableOnSafety.get()) {
                safetyToggled = true;
                toggle();
            }
            return;
        }

        // Pause
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        // Find a target
        target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
        if (target == null) {
            placePos = null;
            breakPos = null;
            return;
        }

        // Auto move
        if (autoMove.get()) {
            FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);

            if (bed.found() && bed.getSlot() != autoMoveSlot.get() - 1) InvUtils.move().from(bed.getSlot()).toHotbar(autoMoveSlot.get() - 1);
            if (!bed.found() && disableOnNoBeds.get()) {
                warning("You have run out of beds, disabling...");
                toggle();
                return;
            }
        }

        if (breakPos == null) {
            placePos = findPlace(target);
        }

        // Place bed
        if (timer <= 0 && placeBed(placePos)) {
            timer = delay.get();
        } else {
            timer--;
        }

        // Break bed
        if (breakPos == null) breakPos = findBreak();
        breakBed(breakPos);
    }

    private BlockPos findPlace(PlayerEntity target) {
        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return null;

        for (int index = 0; index < 3; index++) {
            int i = index == 0 ? 1 : index == 1 ? 0 : 2;

            for (CardinalDirection dir : CardinalDirection.values()) {
                if (strictDirection.get()
                    && dir.toDirection() != mc.player.getHorizontalFacing()
                    && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;

                BlockPos centerPos = target.getBlockPos().up(i);
                BlockPos feetPos = target.getBlockPos();

                BlockPos underCenterPos = target.getBlockPos();
                BlockPos underFeetPos = target.getBlockPos().down(1);

                if (targetFeet.get()) {
                    double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(feetPos));
                    double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(feetPos.offset(dir.toDirection())));

                    if (!mc.world.getBlockState(underFeetPos).getMaterial().equals(Material.AIR)) {
                        if (!mc.world.getBlockState(underFeetPos.offset((direction = dir).toDirection())).getMaterial().equals(Material.AIR)) {
                            if (mc.world.getBlockState(feetPos).getMaterial().isReplaceable()
                                && BlockUtils.canPlace(feetPos.offset(dir.toDirection()))
                                && DamageUtils.bedDamage(target, Utils.vec3d(feetPos)) >= minDamage.get()
                                && offsetSelfDamage < maxSelfDamage.get()
                                && headSelfDamage < maxSelfDamage.get()
                                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                                return feetPos.offset((direction = dir).toDirection());
                            }
                        }
                    }
                } else {
                    double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                    double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir.toDirection())));

                    if (!mc.world.getBlockState(centerPos).getMaterial().equals(Material.AIR)) {
                        if (!mc.world.getBlockState(underCenterPos.offset((direction = dir).toDirection())).getMaterial().equals(Material.AIR)) {
                            if (mc.world.getBlockState(centerPos).getMaterial().isReplaceable()
                                && BlockUtils.canPlace(centerPos.offset(dir.toDirection()))
                                && DamageUtils.bedDamage(target, Utils.vec3d(centerPos)) >= minDamage.get()
                                && offsetSelfDamage < maxSelfDamage.get()
                                && headSelfDamage < maxSelfDamage.get()
                                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                                return centerPos.offset((direction = dir).toDirection());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private BlockPos findBreak() {
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof BedBlockEntity)) continue;

            BlockPos bedPos = blockEntity.getPos();
            Vec3d bedVec = Utils.vec3d(bedPos);

            if (Modules.get().isActive(AntiGhostBlock.class)) {
                if (PlayerUtils.distanceTo(bedPos) <= mc.interactionManager.getReachDistance()) {
                    return bedPos;
                }
            }

            if (PlayerUtils.distanceTo(bedVec) <= mc.interactionManager.getReachDistance()
                && DamageUtils.bedDamage(target, bedVec) >= minDamage.get()
                && DamageUtils.bedDamage(mc.player, bedVec) < maxSelfDamage.get()
                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - DamageUtils.bedDamage(mc.player, bedVec) > 0)) {
                return bedPos;
            }
        }

        return null;
    }

    private boolean placeBed(BlockPos pos) {
        if (pos == null) return false;

        FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (bed.getHand() == null && !autoSwitch.get()) return false;

        double yaw = switch (direction) {
            case East -> 0;
            case South -> 180;
            case West -> -90;
            default -> 0;
        };

        Rotations.rotate(yaw, Rotations.getPitch(pos), () -> {
            BlockUtils.place(pos, bed, false, 0, swing.get(), true);
            breakPos = pos;
        });

        return true;
    }

    private void breakBed(BlockPos pos) {
        if (pos == null) return;
        breakPos = null;

        if (!(mc.world.getBlockState(pos).getBlock() instanceof BedBlock)) return;

        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.setSneaking(false);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false));

        mc.player.setSneaking(wasSneaking);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && placePos != null && breakPos == null) {
            int x = placePos.getX();
            int y = placePos.getY();
            int z = placePos.getZ();

            switch (direction) {
                case North -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case South -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case East -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case West -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
