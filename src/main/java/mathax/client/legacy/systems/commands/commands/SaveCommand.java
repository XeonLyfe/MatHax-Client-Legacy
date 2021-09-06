package mathax.client.legacy.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mathax.client.legacy.systems.Systems;
import mathax.client.legacy.systems.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SaveCommand extends Command {
    public SaveCommand() {
        super("save", "");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("Saving settings...");
            Systems.save();
            info("Settings save complete!");
            return SINGLE_SUCCESS;
        });
    }
}
