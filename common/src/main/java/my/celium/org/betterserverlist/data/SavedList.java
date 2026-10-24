package my.celium.org.betterserverlist.data;

import java.util.ArrayList;
import java.util.List;

/**
 * One server list (a tab on the left of the screen). {@link #servers} holds
 * the manual order of every server in the list; categories and the pinned
 * section show the servers that belong to them in that same order.
 */
public final class SavedList {
    public String id;
    public String name = "";
    public List<Category> categories = new ArrayList<>();
    public List<SavedServer> servers = new ArrayList<>();
    public boolean pinnedCollapsed;
    public boolean uncategorizedCollapsed;

    public SavedList() {
    }

    public SavedList(String name) {
        this.id = Ids.next();
        this.name = name;
    }

    /**
     * Where a server shows up: the pinned section, a category, or
     * uncategorized ({@code category == null}). Pinned wins over the category.
     */
    public record Section(boolean pinned, String category) {
        public static final Section PINNED = new Section(true, null);
        public static final Section NONE = new Section(false, null);

        public static Section of(String category) {
            return new Section(false, category);
        }
    }

    public Section sectionOf(SavedServer server) {
        return server.pinned ? Section.PINNED : Section.of(category(server.category) == null ? null : server.category);
    }

    // ------------------------------------------------------------ categories

    public Category category(String id) {
        if (id == null) {
            return null;
        }
        for (Category category : categories) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return null;
    }

    public Category addCategory(String name) {
        Category category = new Category(name, Category.COLORS[categories.size() % Category.COLORS.length]);
        categories.add(category);
        return category;
    }

    /** Removes the category; its servers become uncategorized and stay in the list. */
    public void removeCategory(String id) {
        categories.removeIf(category -> category.id.equals(id));
        for (SavedServer server : servers) {
            if (id.equals(server.category)) {
                server.category = null;
            }
        }
    }

    /** Moves a category up ({@code delta < 0}) or down; returns whether it moved. */
    public boolean moveCategory(String id, int delta) {
        Category category = category(id);
        if (category == null) {
            return false;
        }
        int from = categories.indexOf(category);
        int to = from + Integer.signum(delta);
        if (to < 0 || to >= categories.size()) {
            return false;
        }
        categories.remove(from);
        categories.add(to, category);
        return true;
    }

    public int countIn(Section section) {
        int count = 0;
        for (SavedServer server : servers) {
            if (sectionOf(server).equals(section)) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------ servers

    public SavedServer server(String id) {
        if (id == null) {
            return null;
        }
        for (SavedServer server : servers) {
            if (server.id.equals(id)) {
                return server;
            }
        }
        return null;
    }

    public boolean containsAddress(String address) {
        String key = SavedServer.key(address);
        for (SavedServer server : servers) {
            if (server.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds {@code server} at the end of {@code section}: right after the last
     * server already there, or at the end of the list.
     */
    public void add(SavedServer server, Section section) {
        servers.remove(server);
        server.pinned = section.pinned();
        if (!section.pinned()) {
            server.category = category(section.category()) == null ? null : section.category();
        }
        int last = -1;
        for (int i = 0; i < servers.size(); i++) {
            if (sectionOf(servers.get(i)).equals(sectionOf(server))) {
                last = i;
            }
        }
        servers.add(last < 0 ? servers.size() : last + 1, server);
    }

    public boolean remove(String id) {
        return servers.removeIf(server -> server.id.equals(id));
    }

    /**
     * Drag and drop: puts {@code moving} just before or after {@code target}
     * and into {@code target}'s section.
     */
    public void moveNextTo(SavedServer moving, SavedServer target, boolean after) {
        if (moving == target || !servers.contains(target)) {
            return;
        }
        servers.remove(moving);
        moving.pinned = target.pinned;
        if (!target.pinned) {
            moving.category = target.category;
        }
        int index = servers.indexOf(target);
        servers.add(after ? index + 1 : index, moving);
    }

    /**
     * Swaps a server with its neighbour in the same section
     * ({@code delta < 0} is up). Returns whether it moved.
     */
    public boolean nudge(SavedServer server, int delta) {
        int from = servers.indexOf(server);
        if (from < 0 || delta == 0) {
            return false;
        }
        Section section = sectionOf(server);
        int step = Integer.signum(delta);
        for (int i = from + step; i >= 0 && i < servers.size(); i += step) {
            if (sectionOf(servers.get(i)).equals(section)) {
                servers.set(from, servers.get(i));
                servers.set(i, server);
                return true;
            }
        }
        return false;
    }
}
