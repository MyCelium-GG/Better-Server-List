package my.celium.org.betterserverlist.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import my.celium.org.betterserverlist.data.SavedList.Section;

class SavedListTest {
    private SavedList list;
    private Category survival;
    private SavedServer a;
    private SavedServer b;
    private SavedServer c;
    private SavedServer d;

    @BeforeEach
    void setUp() {
        list = new SavedList("Test");
        survival = list.addCategory("Survival");
        a = new SavedServer("A", "a");
        b = new SavedServer("B", "b");
        c = new SavedServer("C", "c");
        d = new SavedServer("D", "d");
        list.add(a, Section.NONE);
        list.add(b, Section.of(survival.id));
        list.add(c, Section.NONE);
        list.add(d, Section.of(survival.id));
    }

    private List<String> order() {
        return list.servers.stream().map(s -> s.name).toList();
    }

    @Test
    void addGoesToTheEndOfItsSection() {
        // C lands after A (the last uncategorized), D after B (the last in Survival).
        assertEquals(List.of("A", "C", "B", "D"), order());
        SavedServer e = new SavedServer("E", "e");
        list.add(e, Section.of(survival.id));
        assertEquals(List.of("A", "C", "B", "D", "E"), order());
        assertEquals(survival.id, e.category);
    }

    @Test
    void pinningKeepsTheCategory() {
        list.add(b, Section.PINNED);
        assertTrue(b.pinned);
        assertEquals(survival.id, b.category);
        assertEquals(Section.PINNED, list.sectionOf(b));
        assertEquals(1, list.countIn(Section.PINNED));
        assertEquals(1, list.countIn(Section.of(survival.id)));
    }

    @Test
    void dropNextToAnotherServerTakesItsSection() {
        list.moveNextTo(a, d, true);
        assertEquals(List.of("C", "B", "D", "A"), order());
        assertEquals(survival.id, a.category);

        list.moveNextTo(c, a, false);
        assertEquals(List.of("B", "D", "C", "A"), order());
        assertEquals(survival.id, c.category);
    }

    @Test
    void dropOnAPinnedServerPinsButKeepsCategory() {
        list.add(a, Section.PINNED);
        list.moveNextTo(d, a, false);
        assertTrue(d.pinned);
        assertEquals(survival.id, d.category);
    }

    @Test
    void nudgeSkipsOtherSections() {
        assertTrue(list.nudge(c, -1));
        assertEquals(List.of("C", "A", "B", "D"), order());
        assertFalse(list.nudge(c, -1));
        assertTrue(list.nudge(d, -1));
        assertEquals(List.of("C", "A", "D", "B"), order());
    }

    @Test
    void removingACategoryKeepsItsServers() {
        list.removeCategory(survival.id);
        assertEquals(4, list.servers.size());
        assertNull(b.category);
        assertNull(d.category);
        assertTrue(list.categories.isEmpty());
    }

    @Test
    void categoriesMove() {
        Category creative = list.addCategory("Creative");
        assertFalse(list.moveCategory(survival.id, -1));
        assertTrue(list.moveCategory(creative.id, -1));
        assertEquals(creative, list.categories.get(0));
    }

    @Test
    void copyIsIndependent() {
        a.note = "hi";
        a.pinned = true;
        SavedServer copy = a.copy();
        assertFalse(copy.id.equals(a.id));
        assertEquals("hi", copy.note);
        assertTrue(copy.pinned);
        assertNull(copy.category);
    }

    @Test
    void addressesCompareTrimmedAndCaseless() {
        assertTrue(list.containsAddress(" A "));
        assertFalse(list.containsAddress("z"));
    }
}
