package my.celium.org.betterserverlist.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import my.celium.org.betterserverlist.data.SavedList.Section;

class ServerTextTest {
    @Test
    void exportThenParseRoundTrips() {
        SavedList list = new SavedList("L");
        list.add(new SavedServer("My SMP", "smp.example.org:25566"), Section.NONE);
        list.add(new SavedServer("Local", "192.168.1.20"), Section.NONE);
        assertEquals(List.of(new Library.Entry("My SMP", "smp.example.org:25566"),
                new Library.Entry("Local", "192.168.1.20")), ServerText.parse(ServerText.export(list)));
    }

    @Test
    void parsesLooseText() {
        String text = """
                # my servers
                mc.hypixel.net
                Friends SMP  play.friends.net

                not an address because spaces at end !!
                IPv6 [2001:db8::1]:25565
                """;
        assertEquals(List.of(new Library.Entry("mc.hypixel.net", "mc.hypixel.net"),
                new Library.Entry("Friends SMP", "play.friends.net"),
                new Library.Entry("IPv6", "[2001:db8::1]:25565")), ServerText.parse(text));
    }

    @Test
    void addressRules() {
        assertTrue(ServerText.isAddress("localhost"));
        assertTrue(ServerText.isAddress("a-b.c_d:1"));
        assertFalse(ServerText.isAddress(""));
        assertFalse(ServerText.isAddress("-x"));
        assertFalse(ServerText.isAddress("a b"));
        assertFalse(ServerText.isAddress("a:123456"));
        assertTrue(ServerText.parse(null).isEmpty());
    }
}
