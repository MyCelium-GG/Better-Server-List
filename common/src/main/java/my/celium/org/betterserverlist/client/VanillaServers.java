package my.celium.org.betterserverlist.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import my.celium.org.betterserverlist.BetterServerListMod;
import my.celium.org.betterserverlist.data.Library;
import my.celium.org.betterserverlist.data.SavedServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

/**
 * The bridge to vanilla's {@code servers.dat}. It still holds what the game
 * remembers per server (icon, resource pack choice, accepted code of
 * conduct), and the game updates it on join, so every address gets exactly
 * one {@link ServerData} here, shared by all lists. {@link #save} rewrites
 * the visible entries from the library and keeps vanilla's hidden
 * direct-connect history as it is. Client only.
 */
final class VanillaServers {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final Map<String, ServerData> byKey = new HashMap<>();
    private final List<Library.Entry> entries = new ArrayList<>();

    void load() {
        ServerList list = new ServerList(minecraft);
        list.load();
        entries.clear();
        for (int i = 0; i < list.size(); i++) {
            ServerData data = list.get(i);
            entries.add(new Library.Entry(data.name, data.ip));
            byKey.putIfAbsent(SavedServer.key(data.ip), data);
        }
        BetterServerListMod.debug("servers.dat: {} servers", entries.size());
    }

    /** The visible servers.dat entries, in order. */
    List<Library.Entry> entries() {
        return entries;
    }

    /** The one shared {@link ServerData} for the server's address. */
    ServerData data(SavedServer server) {
        return byKey.computeIfAbsent(server.key(),
                key -> new ServerData(server.name, server.address.trim(), ServerData.Type.OTHER));
    }

    /** Replaces the visible entries of servers.dat with the library's servers, one per address. */
    void save(Library library) {
        ServerList list = new ServerList(minecraft);
        list.load();
        while (list.size() > 0) {
            list.remove(list.get(0));
        }
        for (Library.Entry entry : library.mirror()) {
            ServerData data = byKey.computeIfAbsent(SavedServer.key(entry.address()),
                    key -> new ServerData(entry.name(), entry.address(), ServerData.Type.OTHER));
            data.name = entry.name();
            data.ip = entry.address();
            list.add(data, false);
        }
        list.save();
    }

    /** Remembers a direct connection the way vanilla does: hidden, newest first. */
    ServerData rememberDirect(ServerData typed) {
        ServerList list = new ServerList(minecraft);
        list.load();
        ServerData known = list.get(typed.ip);
        if (known != null) {
            return byKey.getOrDefault(SavedServer.key(known.ip), known);
        }
        list.add(typed, true);
        list.save();
        return typed;
    }
}
