package mathax.legacy.client.systems.modules.combat;

import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.settings.BoolSetting;
import mathax.legacy.client.settings.Setting;
import mathax.legacy.client.settings.SettingGroup;
import mathax.legacy.client.settings.StatusEffectListSetting;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.player.FindItemResult;
import mathax.legacy.client.utils.player.InvUtils;
import mathax.legacy.client.eventbus.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.potion.PotionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quiver extends Module {
    private final List<Integer> arrowSlots = new ArrayList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<StatusEffect>> effects = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("effects")
        .description("Which effects to shoot you with.")
        .defaultValue(Collections.singletonList(StatusEffects.STRENGTH))
        .build()
    );

    private final Setting<Boolean> checkEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("check-existing-effects")
        .description("Won't shoot you with effects you already have.")
        .defaultValue(true)
        .build()
    );

    public Quiver() {
        super(Categories.Combat, Items.BOW, "quiver", "Shoots arrows at yourself.");
    }

    @Override
    public void onActivate() {

        FindItemResult bow = InvUtils.findInHotbar(Items.BOW);

        if (!bow.isHotbar()) {
            error("No bow found, disabling...");
            toggle();
        }

        mc.options.keyUse.setPressed(false);
        mc.interactionManager.stopUsingItem(mc.player);

        InvUtils.swap(bow.getSlot(), true);

        arrowSlots.clear();

        List<StatusEffect> usedEffects = new ArrayList<>();

        for (int i = mc.player.getInventory().size(); i > 0; i--) {
            if (i == mc.player.getInventory().selectedSlot) continue;

            ItemStack item = mc.player.getInventory().getStack(i);

            if (item.getItem() != Items.TIPPED_ARROW)  continue;

            List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(item);

            if (effects.isEmpty()) continue;

            StatusEffect effect = effects.get(0).getEffectType();

            if (this.effects.get().contains(effect)
                && !usedEffects.contains(effect)
                && (!hasEffect(effect) || !checkEffects.get())) {
                usedEffects.add(effect);
                arrowSlots.add(i);
            }
        }
    }

    private boolean hasEffect(StatusEffect effect) {
        for (StatusEffectInstance statusEffect : mc.player.getStatusEffects()) {
            if (statusEffect.getEffectType() == effect) return true;
        }

        return false;
    }

    @Override
    public void onDeactivate() {
        InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (arrowSlots.isEmpty() || !InvUtils.findInHotbar(Items.BOW).isMainHand()) {
            toggle();
            return;
        }

        boolean charging = mc.options.keyUse.isPressed();

        if (!charging) {
            InvUtils.move().from(arrowSlots.get(0)).to(9);
            mc.options.keyUse.setPressed(true);
        } else {
            if (BowItem.getPullProgress(mc.player.getItemUseTime()) >= 0.12) {
                int targetSlot = arrowSlots.get(0);
                arrowSlots.remove(0);

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), -90, mc.player.isOnGround()));
                mc.options.keyUse.setPressed(false);
                mc.interactionManager.stopUsingItem(mc.player);
                if (targetSlot != 9) InvUtils.move().from(9).to(targetSlot);
            }
        }
    }
}
