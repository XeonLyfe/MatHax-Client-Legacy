package mathax.legacy.client.systems.modules.world;

import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

public class ColorSigns extends Module {

    public ColorSigns() {
        super(Categories.World, Items.OAK_SIGN, "color-signs", "Allows you to use colors on signs on NON-PAPER servers. (use \"&\" for color symbols)");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof GameJoinS2CPacket) {
            checkWarning();
            return;
        }
        if (!(event.packet instanceof UpdateSignC2SPacket)) return;
        UpdateSignC2SPacket p = (UpdateSignC2SPacket)event.packet;
        for (int l = 0; l < p.getText().length; l++) {
            String newText = p.getText()[l].replaceAll("(?i)\u00a7|&([0-9A-FK-OR])", "\u00a7\u00a7$1$1");
            p.getText()[l] = newText;
        }
        event.packet = p;
    }

    @Override
    public void onActivate() {
        super.onActivate();
        checkWarning();
    }

    private void checkWarning() {
        String brand = mc.player.getServerBrand();
        if (brand == null) return;
        if (brand.contains("Paper")) warning("You are on a paper server. Color signs won't work here.");
    }
}
