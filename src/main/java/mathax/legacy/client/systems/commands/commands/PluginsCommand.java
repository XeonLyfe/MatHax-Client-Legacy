package mathax.legacy.client.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import joptsimple.internal.Strings;
import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.packets.PacketEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.systems.commands.Command;
import mathax.legacy.client.eventbus.EventHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PluginsCommand extends Command {
    private static final List<String> ANTICHEAT_LIST = Arrays.asList("nocheatplus", "anarchyexploitfixes", "negativity", "warden", "horizon","illegalstack","coreprotect","exploitsx");
    private Integer ticks = 0;

    public PluginsCommand() {
        super("plugins", "Prints server plugins.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ticks = 0;
            MatHaxLegacy.EVENT_BUS.subscribe(this);
            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(0, "/"));
            return SINGLE_SUCCESS;
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        if (ticks >= 5000) {
            error("Plugins check timed out...");
            MatHaxLegacy.EVENT_BUS.unsubscribe(this);
            ticks = 0;
        }
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket) {
                CommandSuggestionsS2CPacket packet = (CommandSuggestionsS2CPacket) event.packet;
                List<String> plugins = new ArrayList<>();
                Suggestions matches = packet.getSuggestions();

                if (matches == null) {
                    error("Invalid Packet.");
                    return;
                }

                for (Suggestion yes : matches.getList()) {
                    String[] command = yes.getText().split(":");
                    if (command.length > 1) {
                        String pluginName = command[0].replace("/", "");

                        if (!plugins.contains(pluginName)) {
                            plugins.add(pluginName);
                        }
                    }
                }

                Collections.sort(plugins);
                for (int i = 0; i < plugins.size(); i++) {
                    plugins.set(i, formatName(plugins.get(i)));
                }

                if (!plugins.isEmpty()) {
                    info("Plugins (%d): %s ", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
                } else {
                    error("No plugins found.");
                }

                ticks = 0;
                MatHaxLegacy.EVENT_BUS.unsubscribe(this);
            }

        } catch (Exception e) {
            error("An error occurred while trying to find plugins!");
            ticks = 0;
            MatHaxLegacy.EVENT_BUS.unsubscribe(this);
        }
    }

    private String formatName(String name) {
        if (ANTICHEAT_LIST.contains(name)) return String.format("%s%s(default)", Formatting.RED, name);
        else if (name.contains("exploit") || name.contains("cheat") || name.contains("illegal")) return String.format("%s%s(default)", Formatting.RED, name);

        return String.format("(highlight)%s(default)", name);
    }
}
