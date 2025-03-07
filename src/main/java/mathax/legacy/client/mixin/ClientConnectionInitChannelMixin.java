package mathax.legacy.client.mixin;

import io.netty.channel.Channel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import mathax.legacy.client.systems.proxies.Proxy;
import mathax.legacy.client.systems.proxies.Proxies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.network.ClientConnection$1")
public class ClientConnectionInitChannelMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "initChannel", at = @At("HEAD"))
    private void onInitChannel(Channel channel, CallbackInfo info) {
        Proxy proxy = Proxies.get().getEnabled();
        if (proxy == null) return;

        switch (proxy.type) {
            case Socks4:
                channel.pipeline().addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.address, proxy.port), proxy.username));
                break;

            case Socks5:
                channel.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.address, proxy.port), proxy.username, proxy.password));
                break;
        }
    }
}
