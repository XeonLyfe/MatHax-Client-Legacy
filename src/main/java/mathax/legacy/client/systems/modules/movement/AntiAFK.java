package mathax.legacy.client.systems.modules.movement;

import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.gui.GuiTheme;
import mathax.legacy.client.gui.widgets.WWidget;
import mathax.legacy.client.gui.widgets.containers.WTable;
import mathax.legacy.client.gui.widgets.input.WTextBox;
import mathax.legacy.client.gui.widgets.pressable.WMinus;
import mathax.legacy.client.gui.widgets.pressable.WPlus;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.player.Rotations;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.settings.*;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntiAFK extends Module {
    private final List<String> messages = new ArrayList<>();
    private int timer;
    private int messageI;
    private int strafeTimer = 0;
    private boolean direction = false;

    private final Random random = new Random();

    private float prevYaw;

    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final SettingGroup sgMessages = settings.createGroup("Messages");

    // Actions

    private final Setting<Boolean> spin = sgActions.add(new BoolSetting.Builder()
        .name("spin")
        .description("Spins.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SpinMode> spinMode = sgActions.add(new EnumSetting.Builder<SpinMode>()
        .name("spin-mode")
        .description("The method of rotating.")
        .defaultValue(SpinMode.Server)
        .visible(spin::get)
        .build()
    );

    private final Setting<Integer> spinSpeed = sgActions.add(new IntSetting.Builder()
        .name("spin-speed")
        .description("The speed to spin you.")
        .defaultValue(15)
        .min(1)
        .sliderMax(100)
        .visible(spin::get)
        .build()
    );

    private final Setting<Double> pitch = sgActions.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("The pitch to set in server mode.")
        .defaultValue(0)
        .min(-90)
        .max(90)
        .sliderMin(-90)
        .sliderMax(90)
        .visible(() -> spin.get() && spinMode.get() == SpinMode.Server)
        .build()
    );

    private final Setting<Boolean> jump = sgActions.add(new BoolSetting.Builder()
        .name("jump")
        .description("Jumps.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> click = sgActions.add(new BoolSetting.Builder()
        .name("click")
        .description("Clicks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disco = sgActions.add(new BoolSetting.Builder()
        .name("disco")
        .description("Sneaks and unsneaks quickly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> strafe = sgActions.add(new BoolSetting.Builder()
        .name("strafe")
        .description("Strafe right and left")
        .defaultValue(false)
        .onChanged(aBoolean -> {
                strafeTimer = 0;
                direction = false;

                if (isActive()) {
                    mc.options.keyLeft.setPressed(false);
                    mc.options.keyRight.setPressed(false);
                }
            })
        .build()
    );

    // Messages

    private final Setting<Boolean> sendMessages = sgMessages.add(new BoolSetting.Builder()
        .name("send-messages")
        .description("Sends messages to prevent getting kicked for AFK.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgMessages.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in seconds.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> randomMessage = sgMessages.add(new BoolSetting.Builder()
        .name("random")
        .description("Selects a random message from your message list.")
        .defaultValue(false)
        .build()
    );

    public AntiAFK() {
        super(Categories.Movement, Items.COMMAND_BLOCK, "anti-afk", "Performs different actions to prevent getting kicked for AFK reasons.");
    }

    @Override
    public void onActivate() {
        prevYaw = mc.player.getYaw();
        timer = delay.get() * 20;
    }

    @Override
    public void onDeactivate() {
        if (strafe.get()) {
            mc.options.keyLeft.setPressed(false);
            mc.options.keyRight.setPressed(false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Utils.canUpdate()) {
            //Spin
            if (spin.get()) {
                prevYaw += spinSpeed.get();
                switch (spinMode.get()) {
                    case Client:
                        mc.player.setYaw(prevYaw);
                        break;
                    case Server:
                        Rotations.rotate(prevYaw, pitch.get(), -15, null);
                        break;
                }
            }

            //Jump
            if (jump.get() && mc.options.keyJump.isPressed()) mc.options.keyJump.setPressed(false);
            if (jump.get() && mc.options.keySneak.isPressed()) mc.options.keySneak.setPressed(false);
            else if (jump.get() && random.nextInt(99) + 1 == 50) mc.options.keyJump.setPressed(true);

            //Click
            if (click.get() && random.nextInt(99) + 1 == 45) {
                mc.options.keyAttack.setPressed(true);
                Utils.leftClick();
                mc.options.keyAttack.setPressed(false);
            }

            //Disco
            if (disco.get() && random.nextInt(24) + 1 == 15) mc.options.keySneak.setPressed(true);

            //Spam
            if (sendMessages.get() && !messages.isEmpty())
                if (timer <= 0) {
                    int i;
                    if (randomMessage.get()) {
                        i = Utils.random(0, messages.size());
                    } else {
                        if (messageI >= messages.size()) messageI = 0;
                        i = messageI++;
                    }

                    mc.player.sendChatMessage(messages.get(i));

                    timer = delay.get() * 20;
                } else {
                    timer--;
                }

            //Strafe
            if (strafe.get() && strafeTimer == 20) {
                mc.options.keyLeft.setPressed(!direction);
                mc.options.keyRight.setPressed(direction);
                direction = !direction;
                strafeTimer = 0;
            } else
                strafeTimer++;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        messages.removeIf(String::isEmpty);

        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        table.add(theme.horizontalSeparator("Message List")).expandX();

        // Messages
        for (int i = 0; i < messages.size(); i++) {
            int msgI = i;
            String message = messages.get(i);

            WTextBox textBox = table.add(theme.textBox(message)).minWidth(100).expandX().widget();
            textBox.action = () -> messages.set(msgI, textBox.get());

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                messages.remove(msgI);

                table.clear();
                fillTable(theme, table);
            };

            table.row();
        }

        // New Message
        WPlus add = table.add(theme.plus()).expandCellX().right().widget();
        add.action = () -> {
            messages.add("");

            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        messages.removeIf(String::isEmpty);
        NbtList messagesTag = new NbtList();

        for (String message : messages) messagesTag.add(NbtString.of(message));
        tag.put("messages", messagesTag);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        messages.clear();

        if (tag.contains("messages")) {
            NbtList messagesTag = tag.getList("messages", 8);
            for (NbtElement messageTag : messagesTag) messages.add(messageTag.asString());
        } else {
            messages.add("This is an AntiAFK message. MatHax on top!");
        }

        return super.fromTag(tag);
    }

    public enum SpinMode {
        Server,
        Client
    }
}
