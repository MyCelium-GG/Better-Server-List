package my.celium.org.betterserverlist.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.betterserverlist.data.SavedList.Section;

class LibraryTest {
    private static List<Library.Entry> vanilla(String... pairs) {
        List<Library.Entry> out = new java.util.ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.add(new Library.Entry(pairs[i], pairs[i + 1]));
        }
        return out;
    }

    private static List<String> names(SavedList list) {
        return list.servers.stream().map(s -> s.name).toList();
    }

    @Test
    void firstStartImportsVanillaInOrder() {
        Library library = Library.fromVanilla(vanilla("Hypixel", "mc.hypixel.net", "Home", "192.168.1.5:25566"));
        assertEquals(1, library.lists.size());
        assertEquals(Library.DEFAULT_LIST, library.active().name);
        assertEquals(List.of("Hypixel", "Home"), names(library.active()));
    }

    @Test
    void importSkipsKnownAddressesInAnyListAndCase() {
        Library library = Library.fromVanilla(vanilla("A", "a.example"));
        SavedList second = library.addList("Second");
        second.add(new SavedServer("B", "b.example"), Section.NONE);
        int added = library.importMissing(vanilla("A again", "A.EXAMPLE ", "B", "b.example", "C", "c.example", "", ""));
        assertEquals(1, added);
        assertEquals(List.of("A", "C"), names(library.lists.get(0)));
    }

    @Test
    void mirrorListsEachAddressOnceUnderItsFirstName() {
        Library library = Library.fromVanilla(vanilla("A", "a.example", "B", "b.example"));
        SavedList second = library.addList("Second");
        second.add(new SavedServer("A elsewhere", "a.example"), Section.NONE);
        second.add(new SavedServer("D", "d.example"), Section.NONE);
        List<Library.Entry> mirror = library.mirror();
        assertEquals(List.of(new Library.Entry("A", "a.example"), new Library.Entry("B", "b.example"),
                new Library.Entry("D", "d.example")), mirror);
    }

    @Test
    void lastListCannotBeRemovedAndActiveFollows() {
        Library library = Library.fromVanilla(List.of());
        assertFalse(library.removeList(library.active().id));
        SavedList second = library.addList("Second");
        library.active = second.id;
        assertTrue(library.removeList(second.id));
        assertEquals(library.lists.get(0), library.active());
    }

    @Test
    void moveListStopsAtTheEnds() {
        Library library = Library.fromVanilla(List.of());
        SavedList b = library.addList("B");
        assertFalse(library.moveList(b.id, 1));
        assertTrue(library.moveList(b.id, -1));
        assertEquals(b, library.lists.get(0));
    }

    @Test
    void sanitizeRepairsHandEditedFiles() {
        Library library = new Library();
        library.lists = null;
        library.sort = null;
        library.sanitize();
        assertEquals(1, library.lists.size());
        assertEquals(Sort.MANUAL, library.sort);

        SavedList list = library.active();
        SavedServer a = new SavedServer("A", "a");
        SavedServer b = new SavedServer("B", "b");
        b.id = a.id;
        b.category = "gone";
        b.note = null;
        list.servers.add(a);
        list.servers.add(b);
        list.servers.add(null);
        library.active = "nope";
        library.sanitize();
        assertEquals(2, list.servers.size());
        assertFalse(a.id.equals(b.id));
        assertNull(b.category);
        assertEquals("", b.note);
        assertEquals(list.id, library.active);
    }

    @Test
    void storeRoundTripsAndKeepsBrokenFiles(@TempDir Path dir) throws IOException {
        LibraryStore store = new LibraryStore(dir.resolve("servers.json"));
        assertNull(store.load());
        Library library = Library.fromVanilla(vanilla("A", "a.example"));
        Category category = library.active().addCategory("Survival");
        library.active().servers.get(0).category = category.id;
        library.active().servers.get(0).note = "whitelist: ask Sam";
        library.sort = Sort.PING;
        store.save(library);

        Library loaded = store.load();
        assertNotNull(loaded);
        assertEquals(Sort.PING, loaded.sort);
        SavedServer server = loaded.active().servers.get(0);
        assertEquals(category.id, server.category);
        assertEquals("whitelist: ask Sam", server.note);
        assertFalse(Files.exists(dir.resolve("servers.json.tmp")));

        Files.writeString(store.file(), "{ not json");
        try {
            store.load();
        } catch (IOException expected) {
            assertTrue(Files.exists(dir.resolve("servers.json.broken")));
            assertFalse(store.exists());
            return;
        }
        throw new AssertionError("a broken file must be reported");
    }
}
