package mathax.legacy.client.mixin;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.Version;
import mathax.legacy.client.gui.GuiThemes;
import mathax.legacy.client.gui.screens.servermanager.ServerManagerScreen;
import mathax.legacy.client.mixininterface.IMultiplayerScreen;
import mathax.legacy.client.systems.modules.misc.NameProtect;
import mathax.legacy.client.systems.proxies.Proxy;
import mathax.legacy.client.utils.misc.LastServerInfo;
import mathax.legacy.client.utils.render.color.Color;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.proxies.Proxies;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*/--------------------------------------------/*/
/*/ Server Finder & CleanUp by JFronny         /*/
/*/ https://github.com/JFronny/MeteorAdditions /*/
/*/--------------------------------------------/*/

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen implements IMultiplayerScreen {
    private final int WHITE = Color.fromRGBA(255, 255, 255, 255);
    private final int GRAY = Color.fromRGBA(175, 175, 175, 255);

    @Shadow protected MultiplayerServerListWidget serverListWidget;

    @Shadow
    @Final
    private Screen parent;

    public MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        Version.didntCheckForLatest = true;

        addDrawableChild(new ButtonWidget(width - 154, 2, 75, 20, new LiteralText("Proxies"), button -> {
            client.setScreen(GuiThemes.get().proxiesScreen());
        }));

        addDrawableChild(new ButtonWidget(width - 77, 2, 75, 20, new LiteralText("Accounts"), button -> {
            client.setScreen(GuiThemes.get().accountsScreen());
        }));

        addDrawableChild(new ButtonWidget(width - 75 - 2 - 75 - 2 - 75 - 2, 2, 75, 20, new LiteralText("Servers"), button -> {
            client.setScreen(new ServerManagerScreen(GuiThemes.get(), (MultiplayerScreen) (Object) this));
        }));

        if (LastServerInfo.getLastServer() != null) {
            addDrawableChild(new ButtonWidget(width / 2 - 154, 10, 100, 20, new LiteralText("Last Server"), button -> {
                LastServerInfo.reconnect(parent);
            }));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        float x = 2;
        float y = 2;

        String space = " ";
        int spaceLength = textRenderer.getWidth(space);

        String loggedInAs = "Logged in as";
        String loggedName = Modules.get().get(NameProtect.class).getName(client.getSession().getUsername());
        String loggedOpenDeveloper = "[";
        String loggedDeveloper = "Developer";
        String loggedCloseDeveloper = "]";
        int loggedInAsLength = textRenderer.getWidth(loggedInAs);
        int loggedNameLength = textRenderer.getWidth(loggedName);
        int loggedOpenDeveloperLength = textRenderer.getWidth(loggedOpenDeveloper);
        int loggedDeveloperLength = textRenderer.getWidth(loggedDeveloper);

        // Logged in as
        drawStringWithShadow(matrices, textRenderer, loggedInAs, 2, (int) y, GRAY);
        drawStringWithShadow(matrices, textRenderer, space, loggedInAsLength + 2, (int) y, GRAY);
        drawStringWithShadow(matrices, textRenderer, loggedName, loggedInAsLength + spaceLength + 2, (int) y, WHITE);
        if (!(Modules.get() == null) && !Modules.get().isActive(NameProtect.class) && (client.getSession().getUuid().equals(MatHaxLegacy.devUUID.replace("-", "")) || client.getSession().getUuid().equals(MatHaxLegacy.devOfflineUUID.replace("-", "")))) {
            drawStringWithShadow(matrices, textRenderer, space, loggedInAsLength + spaceLength + loggedNameLength + 2, (int) y, GRAY);
            drawStringWithShadow(matrices, textRenderer, loggedOpenDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + 2, (int) y, GRAY);
            drawStringWithShadow(matrices, textRenderer, loggedDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + loggedOpenDeveloperLength + 2, (int) y, MatHaxLegacy.INSTANCE.MATHAX_COLOR_INT);
            drawStringWithShadow(matrices, textRenderer, loggedCloseDeveloper, loggedInAsLength + spaceLength + loggedNameLength + spaceLength + loggedOpenDeveloperLength + loggedDeveloperLength + 2, (int) y, GRAY);
        }

        y += textRenderer.fontHeight + 2;

        // Proxy
        Proxy proxy = Proxies.get().getEnabled();

        String proxiesleft = proxy != null ? "Using proxy" + " " : "Not using a proxy";
        String proxiesRight = proxy != null ? "(" + proxy.name + ") " + proxy.address + ":" + proxy.port : null;

        drawStringWithShadow(matrices, textRenderer, proxiesleft, (int)x, (int) y, GRAY);
        if (proxiesRight != null) drawStringWithShadow(matrices, textRenderer, proxiesRight, (int)x + textRenderer.getWidth(proxiesleft), (int) y, WHITE);
    }

    @Override
    public MultiplayerServerListWidget getServerListWidget() {
        return serverListWidget;
    }

    @Inject(at = {@At("HEAD")},
        method = {"connect(Lnet/minecraft/client/network/ServerInfo;)V"})
    private void onConnect(ServerInfo entry, CallbackInfo ci)
    {
        LastServerInfo.setLastServer(entry);
    }
}
