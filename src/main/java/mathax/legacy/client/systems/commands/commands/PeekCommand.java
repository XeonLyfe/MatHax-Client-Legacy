package mathax.legacy.client.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import mathax.legacy.client.systems.commands.Command;
import mathax.legacy.client.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PeekCommand extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];
    private static final SimpleCommandExceptionType NOT_HOLDING_SHULKER_BOX = new SimpleCommandExceptionType(new LiteralText("You must be holding a storage block with items in it."));

    public PeekCommand() {
        super("peek", "Lets you see what's inside storage block items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (Utils.openContainer(mc.player.getMainHandStack(), ITEMS, true)) return SINGLE_SUCCESS;
            else if (Utils.openContainer(mc.player.getOffHandStack(), ITEMS, true)) return SINGLE_SUCCESS;
            else throw NOT_HOLDING_SHULKER_BOX.create();
        });
    }
}
