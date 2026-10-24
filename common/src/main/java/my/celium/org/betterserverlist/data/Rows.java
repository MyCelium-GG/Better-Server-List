package my.celium.org.betterserverlist.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import my.celium.org.betterserverlist.data.SavedList.Section;

/**
 * Turns one list into the rows the screen draws: a pinned section, one
 * section per category, then uncategorized servers. Pure logic, so search,
 * sorting and folding are unit tested without a game.
 *
 * <p>A list with no pins and no categories gets no headers at all and looks
 * like the vanilla list. While searching or hiding offline servers, folded
 * sections open and empty ones disappear, so every match is visible.
 */
public final class Rows {
    private Rows() {
    }

    /** Live ping results the rows need for sorting, filtering and search. */
    public interface Status {
        /** Players online, or -1 while unknown. */
        int players(SavedServer server);

        /** Ping in ms, or -1 while unknown. */
        long ping(SavedServer server);

        /** True once a ping has failed. */
        boolean offline(SavedServer server);

        /** The message of the day as plain text, or "". */
        String motd(SavedServer server);

        Status NONE = new Status() {
            @Override
            public int players(SavedServer server) {
                return -1;
            }

            @Override
            public long ping(SavedServer server) {
                return -1;
            }

            @Override
            public boolean offline(SavedServer server) {
                return false;
            }

            @Override
            public String motd(SavedServer server) {
                return "";
            }
        };
    }

    public enum Kind {
        PINNED, CATEGORY, UNCATEGORIZED
    }

    public sealed interface Row permits Header, ServerRow {
    }

    /** A section title; {@code category} is set for {@link Kind#CATEGORY}. */
    public record Header(Kind kind, Category category, String title, int count, boolean collapsed, int color)
            implements Row {
        public Section section() {
            return switch (kind) {
                case PINNED -> Section.PINNED;
                case CATEGORY -> Section.of(category.id);
                case UNCATEGORIZED -> Section.NONE;
            };
        }
    }

    public record ServerRow(SavedServer server, Section section) implements Row {
    }

    public static List<Row> build(SavedList list, String query, Sort sort, boolean hideOffline, Status status) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean filtering = !needle.isEmpty() || hideOffline;
        boolean headers = !list.categories.isEmpty() || list.countIn(Section.PINNED) > 0;
        List<Row> rows = new ArrayList<>();

        section(rows, list, Section.PINNED, new Header(Kind.PINNED, null, "Pinned", 0, list.pinnedCollapsed, 0xFFA657),
                headers, filtering, true, needle, sort, hideOffline, status);
        for (Category category : list.categories) {
            section(rows, list, Section.of(category.id),
                    new Header(Kind.CATEGORY, category, category.name, 0, category.collapsed, category.color),
                    headers, filtering, false, needle, sort, hideOffline, status);
        }
        boolean showNone = !list.categories.isEmpty();
        section(rows, list, Section.NONE,
                new Header(Kind.UNCATEGORIZED, null, "Uncategorized", 0, list.uncategorizedCollapsed, 0x8B949E),
                headers && showNone, filtering, !showNone, needle, sort, hideOffline, status);
        return rows;
    }

    /**
     * Adds one section. {@code hideWhenEmpty} drops the header of an empty
     * section; while {@code filtering}, every empty section is dropped.
     */
    private static void section(List<Row> rows, SavedList list, Section section, Header header, boolean headers,
            boolean filtering, boolean hideWhenEmpty, String needle, Sort sort, boolean hideOffline, Status status) {
        List<SavedServer> members = new ArrayList<>();
        for (SavedServer server : list.servers) {
            if (list.sectionOf(server).equals(section) && (!hideOffline || !status.offline(server))
                    && matches(list, server, needle, status)) {
                members.add(server);
            }
        }
        if (members.isEmpty() && (filtering || hideWhenEmpty)) {
            return;
        }
        sort(members, sort, status);
        boolean collapsed = header.collapsed() && !filtering;
        if (headers) {
            rows.add(new Header(header.kind(), header.category(), header.title(), members.size(), collapsed,
                    header.color()));
        }
        if (!collapsed || !headers) {
            for (SavedServer server : members) {
                rows.add(new ServerRow(server, section));
            }
        }
    }

    /** Name, address, note, MOTD or the category's name contains the query. */
    static boolean matches(SavedList list, SavedServer server, String needle, Status status) {
        if (needle.isEmpty()) {
            return true;
        }
        Category category = list.category(server.category);
        return contains(server.name, needle) || contains(server.address, needle) || contains(server.note, needle)
                || contains(status.motd(server), needle) || category != null && contains(category.name, needle);
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    static void sort(List<SavedServer> servers, Sort sort, Status status) {
        Comparator<SavedServer> byName = Comparator.comparing(s -> s.name.toLowerCase(Locale.ROOT));
        switch (sort) {
            case MANUAL -> {
            }
            case NAME -> servers.sort(byName);
            // Unknown counts (-1) sink to the bottom.
            case PLAYERS -> servers.sort(Comparator.comparingInt((SavedServer s) -> -status.players(s)).thenComparing(byName));
            case PING -> servers.sort(Comparator.comparingLong((SavedServer s) -> {
                long ping = status.ping(s);
                return ping < 0 ? Long.MAX_VALUE : ping;
            }).thenComparing(byName));
            case RECENT -> servers.sort(Comparator.comparingLong((SavedServer s) -> -s.lastJoined).thenComparing(byName));
        }
    }
}
