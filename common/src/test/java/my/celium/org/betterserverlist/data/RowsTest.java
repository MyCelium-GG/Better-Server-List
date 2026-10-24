package my.celium.org.betterserverlist.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import my.celium.org.betterserverlist.data.Rows.Header;
import my.celium.org.betterserverlist.data.Rows.Row;
import my.celium.org.betterserverlist.data.Rows.ServerRow;
import my.celium.org.betterserverlist.data.SavedList.Section;

class RowsTest {
    /** Rows as short strings: "#Title(n)" for headers, "name" for servers. */
    private static List<String> render(List<Row> rows) {
        List<String> out = new ArrayList<>();
        for (Row row : rows) {
            if (row instanceof Header header) {
                out.add("#" + header.title() + "(" + header.count() + ")" + (header.collapsed() ? "-" : ""));
            } else {
                out.add(((ServerRow) row).server().name);
            }
        }
        return out;
    }

    private static Rows.Status status(Map<String, Integer> players, Map<String, Long> pings, String offline) {
        return new Rows.Status() {
            @Override
            public int players(SavedServer server) {
                return players.getOrDefault(server.name, -1);
            }

            @Override
            public long ping(SavedServer server) {
                return pings.getOrDefault(server.name, -1L);
            }

            @Override
            public boolean offline(SavedServer server) {
                return server.name.equals(offline);
            }

            @Override
            public String motd(SavedServer server) {
                return server.name.equals("B") ? "Welcome to Skyblock" : "";
            }
        };
    }

    private static SavedList plain() {
        SavedList list = new SavedList("L");
        list.add(new SavedServer("B", "b"), Section.NONE);
        list.add(new SavedServer("a", "a"), Section.NONE);
        list.add(new SavedServer("C", "c"), Section.NONE);
        return list;
    }

    @Test
    void plainListHasNoHeaders() {
        assertEquals(List.of("B", "a", "C"), render(Rows.build(plain(), "", Sort.MANUAL, false, Rows.Status.NONE)));
    }

    @Test
    void sectionsAppearWithPinsAndCategories() {
        SavedList list = plain();
        Category survival = list.addCategory("Survival");
        list.add(list.servers.get(2), Section.of(survival.id));
        list.add(list.servers.get(0), Section.PINNED);
        assertEquals(List.of("#Pinned(1)", "B", "#Survival(1)", "C", "#Uncategorized(1)", "a"),
                render(Rows.build(list, "", Sort.MANUAL, false, Rows.Status.NONE)));
    }

    @Test
    void emptyCategoriesStayAsDropTargetsButEmptyPinnedHides() {
        SavedList list = plain();
        list.addCategory("Empty");
        assertEquals(List.of("#Empty(0)", "#Uncategorized(3)", "B", "a", "C"),
                render(Rows.build(list, "", Sort.MANUAL, false, Rows.Status.NONE)));
    }

    @Test
    void collapsedSectionsHideServersUntilSearching() {
        SavedList list = plain();
        Category survival = list.addCategory("Survival");
        list.add(list.servers.get(0), Section.of(survival.id));
        survival.collapsed = true;
        assertEquals(List.of("#Survival(1)-", "#Uncategorized(2)", "a", "C"),
                render(Rows.build(list, "", Sort.MANUAL, false, Rows.Status.NONE)));
        assertEquals(List.of("#Survival(1)", "B"),
                render(Rows.build(list, "b", Sort.MANUAL, false, Rows.Status.NONE)));
    }

    @Test
    void searchLooksAtMotdNoteAndCategory() {
        SavedList list = plain();
        Rows.Status status = status(Map.of(), Map.of(), null);
        assertEquals(List.of("B"), render(Rows.build(list, "skyblock", Sort.MANUAL, false, status)));
        list.servers.get(2).note = "Friends' SMP";
        assertEquals(List.of("C"), render(Rows.build(list, "smp", Sort.MANUAL, false, status)));
        Category survival = list.addCategory("Survival");
        list.add(list.servers.get(1), Section.of(survival.id));
        assertEquals(List.of("#Survival(1)", "a"), render(Rows.build(list, "surv", Sort.MANUAL, false, status)));
    }

    @Test
    void hideOfflineDropsFailedPings() {
        assertEquals(List.of("B", "C"),
                render(Rows.build(plain(), "", Sort.MANUAL, true, status(Map.of(), Map.of(), "a"))));
    }

    @Test
    void sortsInsideSections() {
        Rows.Status status = status(Map.of("B", 5, "C", 900), Map.of("a", 20L, "C", 150L), null);
        assertEquals(List.of("a", "B", "C"), render(Rows.build(plain(), "", Sort.NAME, false, status)));
        assertEquals(List.of("C", "B", "a"), render(Rows.build(plain(), "", Sort.PLAYERS, false, status)));
        assertEquals(List.of("a", "C", "B"), render(Rows.build(plain(), "", Sort.PING, false, status)));
        SavedList list = plain();
        list.servers.get(2).lastJoined = 10;
        list.servers.get(0).lastJoined = 5;
        assertEquals(List.of("C", "B", "a"), render(Rows.build(list, "", Sort.RECENT, false, status)));
    }

    @Test
    void sortCycles() {
        Sort sort = Sort.MANUAL;
        for (int i = 0; i < Sort.values().length; i++) {
            sort = sort.next();
        }
        assertTrue(sort == Sort.MANUAL);
    }
}
