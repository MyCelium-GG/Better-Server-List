package my.celium.org.betterserverlist.client;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;

import my.celium.org.betterserverlist.BetterServerListMod;
import my.celium.org.betterserverlist.data.Rows;
import my.celium.org.betterserverlist.data.SavedServer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;

/**
 * Pings servers the way the vanilla list does (same pinger, same five
 * background threads) and keeps their icons. One ping per address, however
 * many lists hold it. Also answers {@link Rows.Status} for sorting, the
 * online filter and search. Client only.
 */
final class Pings implements Rows.Status {
    private static final ThreadPoolExecutor POOL = new ScheduledThreadPoolExecutor(5,
            new ThreadFactoryBuilder().setNameFormat("Better Server List Pinger #%d").setDaemon(true).build());
    /** Icons outlive screens: reopening the list shows them at once. */
    private static final Map<String, Icon> ICONS = new HashMap<>();

    static final Component CANT_RESOLVE = Component.translatable("multiplayer.status.cannot_resolve").withColor(0xFFEF5350);
    static final Component CANT_CONNECT = Component.translatable("multiplayer.status.cannot_connect").withColor(0xFFEF5350);

    private final Minecraft minecraft = Minecraft.getInstance();
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final VanillaServers vanilla;
    /** Runs on the game thread when a result arrives (re-sort, re-filter). */
    private final Runnable onResult;
    /** Runs on the game thread when an icon changes (save servers.dat). */
    private final Runnable onIconChange;
    /** Pings still running, checked each tick for the failures the pinger reports only as a MOTD. */
    private final Set<ServerData> running = new LinkedHashSet<>();

    private static final class Icon {
        final FaviconTexture texture;
        byte[] bytes;

        Icon(FaviconTexture texture) {
            this.texture = texture;
        }
    }

    Pings(VanillaServers vanilla, Runnable onResult, Runnable onIconChange) {
        this.vanilla = vanilla;
        this.onResult = onResult;
        this.onIconChange = onIconChange;
    }

    ServerData data(SavedServer server) {
        return vanilla.data(server);
    }

    /** Starts a ping unless one ran or is running for this address. */
    void start(SavedServer server) {
        ServerData data = data(server);
        if (data.state() != ServerData.State.INITIAL) {
            return;
        }
        data.setState(ServerData.State.PINGING);
        running.add(data);
        data.motd = Component.empty();
        data.status = Component.empty();
        POOL.submit(() -> {
            try {
                pinger.pingServer(data,
                        () -> minecraft.execute(onIconChange),
                        () -> {
                            data.setState(data.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                                    ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                            minecraft.execute(onResult);
                        },
                        EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
            } catch (UnknownHostException e) {
                fail(data, CANT_RESOLVE);
            } catch (Exception e) {
                BetterServerListMod.debug("Ping of {} failed: {}", data.ip, e.toString());
                fail(data, CANT_CONNECT);
            }
        });
    }

    private void fail(ServerData data, Component motd) {
        data.setState(ServerData.State.UNREACHABLE);
        data.motd = motd;
        data.status = Component.empty();
        minecraft.execute(onResult);
    }

    /** Pings everything again, e.g. after Refresh. */
    void refresh(Iterable<SavedServer> servers) {
        pinger.removeAll();
        for (SavedServer server : servers) {
            data(server).setState(ServerData.State.INITIAL);
        }
        for (SavedServer server : servers) {
            start(server);
        }
    }

    /**
     * Ticks the pinger. A refused connection or unknown host only sets the
     * "Can't connect" MOTD and leaves the state at PINGING (vanilla's list
     * animates forever); those count as unreachable here.
     */
    void tick() {
        pinger.tick();
        if (running.isEmpty()) {
            return;
        }
        String cantConnect = CANT_CONNECT.getString();
        String unknownHost = ConnectScreen.UNKNOWN_HOST_MESSAGE.getString();
        boolean changed = false;
        for (var it = running.iterator(); it.hasNext();) {
            ServerData data = it.next();
            if (data.state() != ServerData.State.PINGING) {
                it.remove();
                continue;
            }
            String motd = data.motd == null ? "" : data.motd.getString();
            if (motd.equals(cantConnect) || motd.equals(unknownHost)) {
                data.setState(ServerData.State.UNREACHABLE);
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            onResult.run();
        }
    }

    /**
     * Drops open connections when the screen goes away (a child screen or
     * leaving). Unfinished pings start over when the list shows again.
     */
    void stop(Iterable<SavedServer> servers) {
        pinger.removeAll();
        for (SavedServer server : servers) {
            ServerData data = data(server);
            if (data.state() == ServerData.State.PINGING) {
                data.setState(ServerData.State.INITIAL);
            }
        }
    }

    /** The server's icon, uploaded when its bytes changed; vanilla's "unknown server" picture without one. */
    Identifier icon(SavedServer server) {
        ServerData data = data(server);
        Icon icon = ICONS.computeIfAbsent(server.key(), key -> new Icon(
                FaviconTexture.forServer(minecraft.getTextureManager(), BetterServerListMod.ID + "/" + key)));
        byte[] bytes = data.getIconBytes();
        if (!Arrays.equals(bytes, icon.bytes)) {
            icon.bytes = bytes;
            if (bytes == null) {
                icon.texture.clear();
            } else {
                try {
                    icon.texture.upload(NativeImage.read(bytes));
                } catch (Throwable t) {
                    BetterServerListMod.warn("Invalid icon for server {} ({}): {}", data.name, data.ip, t.toString());
                    data.setIconBytes(null);
                    icon.bytes = null;
                    icon.texture.clear();
                }
            }
        }
        return icon.texture.textureLocation();
    }

    // ------------------------------------------------------------ Rows.Status

    @Override
    public int players(SavedServer server) {
        ServerData data = data(server);
        boolean answered = data.state() == ServerData.State.SUCCESSFUL || data.state() == ServerData.State.INCOMPATIBLE;
        return answered && data.players != null ? data.players.online() : -1;
    }

    @Override
    public long ping(SavedServer server) {
        ServerData data = data(server);
        return data.state() == ServerData.State.SUCCESSFUL ? data.ping : -1;
    }

    @Override
    public boolean offline(SavedServer server) {
        return data(server).state() == ServerData.State.UNREACHABLE;
    }

    @Override
    public String motd(SavedServer server) {
        Component motd = data(server).motd;
        return motd == null ? "" : motd.getString();
    }
}
