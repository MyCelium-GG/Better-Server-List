package my.celium.org.betterserverlist.data;

import java.util.Locale;

/**
 * One server in one list. The same address may sit in several lists; each
 * copy has its own category, pin, note and play history. Fields are public
 * and plain so Gson can store them as they are.
 */
public final class SavedServer {
    public String id;
    public String name = "";
    public String address = "";
    /** {@link Category#id} in the owning list, or {@code null} for uncategorized. */
    public String category;
    public boolean pinned;
    public String note = "";
    /** Epoch millis. */
    public long added;
    /** Epoch millis of the last join through this entry, 0 for never. */
    public long lastJoined;
    public int joins;

    public SavedServer() {
    }

    public SavedServer(String name, String address) {
        this.id = Ids.next();
        this.name = name;
        this.address = address;
        this.added = System.currentTimeMillis();
    }

    /** A copy for another list: fresh id, no category, same name, note and pin. */
    public SavedServer copy() {
        SavedServer copy = new SavedServer(name, address);
        copy.pinned = pinned;
        copy.note = note;
        copy.added = added;
        copy.lastJoined = lastJoined;
        copy.joins = joins;
        return copy;
    }

    /** The address as servers.dat and the pinger compare it: trimmed, lower case. */
    public static String key(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return key(address);
    }
}
