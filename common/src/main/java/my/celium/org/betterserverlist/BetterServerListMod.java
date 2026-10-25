package my.celium.org.betterserverlist;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.logging.MycelLog;
import my.celium.org.metadata.ModMetadata;

/**
 * Better Server List: replaces the multiplayer screen with one that has
 * several lists, collapsible categories, pinned servers, search, sorting,
 * drag and drop, notes and play history. Vanilla's servers.dat stays in
 * sync, so the vanilla screen and the game without this mod still work.
 *
 * <p>Client-side only. {@link #init()} registers metadata and config with
 * Mycel; each loader's <em>client</em> entrypoint then swaps the screen.
 */
public final class BetterServerListMod {
    public static final String ID = "betterserverlist";

    public static final ModMetadata META = ModMetadata.builder(ID, "Better Server List", "1.0.1")
            .author("MyCelium")
            .website("https://www.curseforge.com/minecraft/mc-mods/better-server-list")
            .updateUrl("https://wiki.mycelium-gg.org/updates/betterserverlist.json")
            .build();

    public static ConfigValue<Boolean> enabled;
    public static ConfigValue<Boolean> replaceScreen;
    public static ConfigValue<Boolean> showLan;
    public static ConfigValue<Boolean> confirmDelete;
    public static ConfigValue<Boolean> debug;
    public static MycelConfig config;

    private BetterServerListMod() {
    }

    /** Called once from each loader's entrypoint. */
    public static void init() {
        Mycel.initialize();
        Mycel.registerMod(META);

        ConfigBuilder builder = Mycel.config(META);
        enabled = builder.booleanValue("enabled", true,
                "Whether the mod does anything at all.");
        replaceScreen = builder.booleanValue("replaceScreen", true,
                "Open Better Server List instead of the vanilla multiplayer screen.");
        showLan = builder.booleanValue("showLan", true,
                "Look for LAN worlds and list them at the bottom.");
        confirmDelete = builder.booleanValue("confirmDelete", true,
                "Ask before deleting a server, category or list.");
        debug = builder.booleanValue("debug", false,
                "Log loading, saving, syncing and screen swaps to the game log.");
        config = builder.build();
    }

    /** Info-level log line tagged {@code [debug]}, only while the {@code debug} option is on. */
    public static void debug(String message, Object... args) {
        try {
            if (debug == null || !debug.get()) {
                return;
            }
            MycelLog.infoMod(ID, "[debug] " + message, args);
        } catch (RuntimeException e) {
            // Logging must never break the caller.
        }
    }

    public static void warn(String message, Object... args) {
        try {
            MycelLog.warnMod(ID, message, args);
        } catch (RuntimeException e) {
            // Logging must never break the caller.
        }
    }

    public static boolean isModEnabled() {
        return Mycel.isEnabled(ID) && enabled.get();
    }
}
