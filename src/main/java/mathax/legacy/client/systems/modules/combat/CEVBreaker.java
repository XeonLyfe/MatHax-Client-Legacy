package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.events.render.Render3DEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.mixininterface.IVec3d;
import mathax.legacy.client.renderer.ShapeMode;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.entity.SortPriority;
import mathax.legacy.client.utils.entity.TargetUtils;
import mathax.legacy.client.utils.player.*;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.utils.world.BlockUtils;
import mathax.legacy.client.settings.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CEVBreaker extends Module {
    private static final boolean assertionsDisabled = !CEVBreaker.class.desiredAssertionStatus();

    private BlockPos pos;

    private int pause;

    private boolean isDone;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How many ticks between block placements.")
        .defaultValue(4)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The radius players can be in to be targeted.")
        .defaultValue(5.0)
        .sliderMin(0.0)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
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
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 255))
        .build()
    );

    //TODO: Explodes crystals without mining, fix :)))))

    public CEVBreaker() {
        super(Categories.Combat, Items.END_CRYSTAL, "CEV-breaker", "Places obsidian on top of people and explodes crystals on top of their heads after destroying the obsidian.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre pre) {
        if (mc.world != null && mc.player != null) {
            if (pause > 0) {
                --pause;
            } else {
                pause = delay.get();

                SortPriority sortPriority = SortPriority.LowestDistance;
                PlayerEntity target = TargetUtils.getPlayerTarget(7.0, sortPriority);

                if (target != null) {
                    BlockPos blockPos;
                    BlockPos blockPos1 = new BlockPos(target.getBlockPos().getX(), target.getBlockPos().getY() + 2, target.getBlockPos().getZ());
                    if (mc.player.distanceTo(target) <= range.get() && mc.world.getBlockState(blockPos = new BlockPos(target.getBlockPos().getX(), target.getBlockPos().getY() + 1, target.getBlockPos().getZ())).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK) {
                        BlockPos blockPos2 = new BlockPos(target.getBlockPos().getX(), target.getBlockPos().getY() + 3, target.getBlockPos().getZ());
                        if (mc.world.getBlockState(blockPos1).isAir() || mc.world.getBlockState(blockPos1).getBlock() == Blocks.OBSIDIAN) {
                            int n = EnhancedInvUtils.findItemInHotbar(Items.IRON_PICKAXE);

                            if (n == -1) {
                                n = EnhancedInvUtils.findItemInHotbar(Items.NETHERITE_PICKAXE);
                            }

                            if (n == -1) {
                                n = EnhancedInvUtils.findItemInHotbar(Items.DIAMOND_PICKAXE);
                            }

                            if (n == -1) {
                                error("Can't find any pickaxe in hotbar, disabling...", new Object[0]);
                                toggle();
                            } else {
                                if (mc.world.getBlockState(blockPos1).getBlock() != Blocks.OBSIDIAN) placeBlock(blockPos1, EnhancedInvUtils.findItemInHotbar(Items.OBSIDIAN), rotate.get());

                                if (!equalsBlockPos(pos, blockPos1)) {
                                    pos = blockPos1;
                                    EnhancedInvUtils.swap(n);
                                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
                                    isDone = false;
                                } else if (isDone) {
                                    EndCrystalEntity endCrystalEntity = null;

                                    for (Object object : mc.world.getEntities()) {
                                        if (!(object instanceof EndCrystalEntity) || !equalsBlockPos(((EndCrystalEntity) object).getBlockPos(), blockPos2)) continue;
                                        endCrystalEntity = (EndCrystalEntity)object;
                                        break;
                                    }

                                    if (endCrystalEntity != null) {
                                        if (rotate.get()) {
                                            Rotations.rotate(Rotations.getYaw(endCrystalEntity), Rotations.getPitch(endCrystalEntity));
                                        }

                                        int n2 = mc.player.getInventory().selectedSlot;
                                        EnhancedInvUtils.swap(n);
                                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
                                        EnhancedInvUtils.swap(n2);
                                        attackEntity(endCrystalEntity);
                                    } else {
                                        placeCrystal(target, blockPos1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean placeCrystal(PlayerEntity playerEntity, BlockPos blockPos) {
        BlockPos blockPos1 = new BlockPos(playerEntity.getBlockPos().getX(), playerEntity.getBlockPos().getY() + 3, playerEntity.getBlockPos().getZ());
        if (!BlockUtils.canPlace(blockPos1, true)) return false;
        if (!assertionsDisabled && mc.world == null) throw new AssertionError();
        if (!mc.world.getBlockState(blockPos1).isAir()) return false;
        int n = EnhancedInvUtils.findItemInHotbar(Items.END_CRYSTAL);
        if (n == -1) {
            error("Can't find crystals in your hotbar, disabling...", new Object[0]);
            toggle();
            return false;
        }

        interact(blockPos, n, Direction.UP);
        return true;
    }

    public boolean equalsBlockPos(BlockPos blockPos, BlockPos blockPos1) {
        if (blockPos == null || blockPos1 == null) return false;
        if (blockPos.getX() != blockPos1.getX()) return false;
        if (blockPos.getY() != blockPos1.getY()) return false;

        return blockPos.getZ() == blockPos1.getZ();
    }

    @EventHandler
    private void BlockUpdate(PacketEvent.Receive receive) {
        if (!(receive.packet instanceof BlockUpdateS2CPacket)) return;
        BlockUpdateS2CPacket blockUpdateS2CPacket = (BlockUpdateS2CPacket)receive.packet;
        if (equalsBlockPos(blockUpdateS2CPacket.getPos(), pos) && blockUpdateS2CPacket.getState().isAir()) isDone = true;
    }

    public boolean placeBlock(BlockPos blockPos, int n, boolean bl) {
        if (n == -1) return false;
        if (!BlockUtils.canPlace(blockPos, true)) return false;
        if (!assertionsDisabled && mc.player == null) throw new AssertionError();
        int n2 = mc.player.getInventory().selectedSlot;
        EnhancedInvUtils.swap(n);
        if (bl) {
            Vec3d rotationPos = new Vec3d(0.0, 0.0, 0.0);
            ((IVec3d)rotationPos).set(blockPos.getY() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            Rotations.rotate(Rotations.getYaw(rotationPos), Rotations.getPitch(rotationPos));
        }

        if (!assertionsDisabled && mc.interactionManager == null) throw new AssertionError();
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, blockPos, true));
        EnhancedInvUtils.swap(n2);
        return true;
    }

    @Override
    public void onActivate() {
        pos = null;
        isDone = false;
        pause = 0;
    }

    public void attackEntity(EndCrystalEntity endCrystalEntity) {
        if (!assertionsDisabled && mc.interactionManager == null) throw new AssertionError();
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack( endCrystalEntity, mc.player.isSneaking()));
    }

    public void interact(BlockPos blockPos, int n, Direction direction) {
        if (!assertionsDisabled && mc.player == null) throw new AssertionError();
        int n2 = mc.player.getInventory().selectedSlot;
        EnhancedInvUtils.swap(n);
        if (!assertionsDisabled && mc.interactionManager == null) throw new AssertionError();
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), direction, blockPos, true));
        EnhancedInvUtils.swap(n2);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || pos == null) return;
        event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
