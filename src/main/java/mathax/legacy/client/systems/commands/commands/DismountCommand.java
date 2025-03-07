package mathax.legacy.client.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mathax.legacy.client.systems.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DismountCommand extends Command {

    public DismountCommand() {
        super("dismount", "Dismounts you from entity you are riding.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0, 0, false, true));
            return SINGLE_SUCCESS;
        });
    }
}
