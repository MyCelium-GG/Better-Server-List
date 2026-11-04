package my.celium.org.betterserverlist;

import java.util.ArrayList;
import java.util.List;

import my.celium.org.Mycel;
import my.celium.org.betterserverlist.data.Library;
import my.celium.org.betterserverlist.data.LibraryStore;
import my.celium.org.betterserverlist.data.SavedList;
import my.celium.org.betterserverlist.data.SavedServer;
import my.celium.org.stats.ModStats;

/**
 * Anonymous stats for the FastStats project {@code better-server-list}, sent through Mycel under
 * its consent: how big the saved lists are, the sort mode, the settings and which features get
 * used. Server names, addresses and notes are never sent, only counts.
 */
public final class ServerListStats {
    private static final String TOKEN = "c051b093b35e33befd068fc50ad92a6d";

    private static volatile ModStats stats;
    private static volatile Summary summary;
    private static volatile boolean fileRead;

    /** Counts from the saved lists. */
    record Summary(int lists, int servers, int categories, int pinned, int notes, String sort, boolean hideOffline) {
    }

    private ServerListStats() {
    }

    static void start() {
        stats = ModStats.builder(BetterServerListMod.META, TOKEN)
                .number("lists", () -> summary() == null ? null : summary().lists())
                .number("servers", () -> summary() == null ? null : summary().servers())
                .number("categories", () -> summary() == null ? null : summary().categories())
                .number("pinned_servers", () -> summary() == null ? null : summary().pinned())
                .number("servers_with_notes", () -> summary() == null ? null : summary().notes())
                .string("sort", () -> summary() == null ? null : summary().sort())
                .bool("hide_offline", () -> summary() == null ? null : summary().hideOffline())
                .stringArray("settings_off", ServerListStats::settingsOff)
                .errors("my.celium.org.betterserverlist.")
                .start();
    }

    /**
     * Counts a feature use: {@code open}, {@code join}, {@code join_lan}, {@code direct_connect},
     * {@code search}, {@code paste}, {@code pin}, {@code sort_change}, {@code vanilla_screen}.
     */
    public static void count(String what) {
        ModStats current = stats;
        if (current != null) {
            current.count(what);
        }
    }

    /** Called whenever the lists are loaded or saved. */
    public static void update(Library library) {
        if (library == null || library.lists == null) {
            return;
        }
        int servers = 0;
        int categories = 0;
        int pinned = 0;
        int notes = 0;
        for (SavedList list : library.lists) {
            categories += list.categories == null ? 0 : list.categories.size();
            if (list.servers != null) {
                for (SavedServer server : list.servers) {
                    servers++;
                    pinned += server.pinned ? 1 : 0;
                    notes += server.note != null && !server.note.isBlank() ? 1 : 0;
                }
            }
        }
        summary = new Summary(library.lists.size(), servers, categories, pinned, notes,
                library.sort == null ? null : library.sort.name().toLowerCase(java.util.Locale.ROOT), library.hideOffline);
    }

    /** The summary, read once from the saved file when the screen has not been opened yet. */
    private static Summary summary() {
        if (summary == null && !fileRead) {
            fileRead = true;
            try {
                update(new LibraryStore(Mycel.getConfigDir().resolve(BetterServerListMod.ID + "-servers.json")).load());
            } catch (java.io.IOException | RuntimeException e) {
                // No saved lists yet: the metrics are skipped.
            }
        }
        return summary;
    }

    private static String[] settingsOff() {
        List<String> off = new ArrayList<>();
        if (!BetterServerListMod.enabled.get()) {
            off.add("enabled");
        }
        if (!BetterServerListMod.replaceScreen.get()) {
            off.add("replaceScreen");
        }
        if (!BetterServerListMod.showLan.get()) {
            off.add("showLan");
        }
        if (!BetterServerListMod.confirmDelete.get()) {
            off.add("confirmDelete");
        }
        return off.isEmpty() ? null : off.toArray(String[]::new);
    }
}
