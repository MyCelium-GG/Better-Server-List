package my.celium.org.betterserverlist.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the screen saves: the lists, which one is open, and the view
 * settings. Stored as {@code config/betterserverlist/servers.json} by
 * {@link LibraryStore}.
 */
public final class Library {
    public static final int FORMAT = 1;
    public static final String DEFAULT_LIST = "Servers";

    public int format = FORMAT;
    public String active;
    public List<SavedList> lists = new ArrayList<>();
    public Sort sort = Sort.MANUAL;
    public boolean hideOffline;
    public boolean lanCollapsed;

    /** A name and address read from vanilla's servers.dat. */
    public record Entry(String name, String address) {
    }

    /** A fresh library with one list holding {@code vanilla} in order. */
    public static Library fromVanilla(List<Entry> vanilla) {
        Library library = new Library();
        library.sanitize();
        library.importMissing(vanilla);
        return library;
    }

    public SavedList active() {
        SavedList list = list(active);
        return list != null ? list : lists.get(0);
    }

    public SavedList list(String id) {
        if (id == null) {
            return null;
        }
        for (SavedList list : lists) {
            if (list.id.equals(id)) {
                return list;
            }
        }
        return null;
    }

    public SavedList addList(String name) {
        SavedList list = new SavedList(name);
        lists.add(list);
        return list;
    }

    /** Removes a list unless it is the last one; returns whether it did. */
    public boolean removeList(String id) {
        if (lists.size() <= 1) {
            return false;
        }
        boolean removed = lists.removeIf(list -> list.id.equals(id));
        if (removed && list(active) == null) {
            active = lists.get(0).id;
        }
        return removed;
    }

    public boolean moveList(String id, int delta) {
        SavedList list = list(id);
        if (list == null) {
            return false;
        }
        int from = lists.indexOf(list);
        int to = from + Integer.signum(delta);
        if (to < 0 || to >= lists.size()) {
            return false;
        }
        lists.remove(from);
        lists.add(to, list);
        return true;
    }

    /**
     * Adds every vanilla server whose address no list has yet to the end of
     * the first list, uncategorized. Covers the first start, servers added
     * with the vanilla screen, and servers.dat files shipped by modpacks.
     * Returns how many were added.
     */
    public int importMissing(List<Entry> vanilla) {
        Set<String> known = new HashSet<>();
        for (SavedList list : lists) {
            for (SavedServer server : list.servers) {
                known.add(server.key());
            }
        }
        int added = 0;
        SavedList target = lists.get(0);
        for (Entry entry : vanilla) {
            String key = SavedServer.key(entry.address());
            if (key.isEmpty() || !known.add(key)) {
                continue;
            }
            target.add(new SavedServer(entry.name(), entry.address().trim()), SavedList.Section.NONE);
            added++;
        }
        return added;
    }

    /**
     * What servers.dat should list: every address once, in list order, under
     * the name of its first entry. Keeps the vanilla screen (and the game
     * without this mod) in step with these lists.
     */
    public List<Entry> mirror() {
        Map<String, Entry> out = new LinkedHashMap<>();
        for (SavedList list : lists) {
            for (SavedServer server : list.servers) {
                if (!server.key().isEmpty()) {
                    out.putIfAbsent(server.key(), new Entry(server.name, server.address.trim()));
                }
            }
        }
        return new ArrayList<>(out.values());
    }

    /**
     * Repairs whatever a hand-edited or older file got wrong: missing ids,
     * nulls, duplicate ids, categories that no longer exist, no lists.
     */
    public void sanitize() {
        if (lists == null) {
            lists = new ArrayList<>();
        }
        lists.removeIf(list -> list == null);
        if (lists.isEmpty()) {
            lists.add(new SavedList(DEFAULT_LIST));
        }
        Set<String> ids = new HashSet<>();
        for (SavedList list : lists) {
            list.id = unique(list.id, ids);
            list.name = list.name == null || list.name.isBlank() ? "List" : list.name;
            if (list.categories == null) {
                list.categories = new ArrayList<>();
            }
            if (list.servers == null) {
                list.servers = new ArrayList<>();
            }
            list.categories.removeIf(category -> category == null);
            for (Category category : list.categories) {
                category.id = unique(category.id, ids);
                category.name = category.name == null ? "" : category.name;
                category.color &= 0xFFFFFF;
            }
            list.servers.removeIf(server -> server == null || server.address == null);
            for (SavedServer server : list.servers) {
                server.id = unique(server.id, ids);
                server.name = server.name == null ? "" : server.name;
                server.note = server.note == null ? "" : server.note;
                if (list.category(server.category) == null) {
                    server.category = null;
                }
            }
        }
        if (list(active) == null) {
            active = lists.get(0).id;
        }
        if (sort == null) {
            sort = Sort.MANUAL;
        }
        format = FORMAT;
    }

    private static String unique(String id, Set<String> seen) {
        String out = id == null || id.isBlank() ? Ids.next() : id;
        while (!seen.add(out)) {
            out = Ids.next();
        }
        return out;
    }
}
