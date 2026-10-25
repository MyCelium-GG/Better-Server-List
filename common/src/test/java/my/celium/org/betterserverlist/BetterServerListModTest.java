package my.celium.org.betterserverlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.Mycel;
import my.celium.org.betterserverlist.client.ScreenSwap;
import my.celium.org.testing.MycelTesting;

/** The mod boots on Mycel with a small, well-defaulted config. */
class BetterServerListModTest {
    @BeforeAll
    static void initOnce(@TempDir Path tempDir) {
        MycelTesting.useConfigDir(tempDir);
        BetterServerListMod.init();
    }

    @Test
    void registersWithMycel() {
        assertTrue(Mycel.getRegisteredMods().contains(BetterServerListMod.META));
        assertEquals("betterserverlist", BetterServerListMod.META.id());
    }

    @Test
    void configDefaults() {
        assertTrue(BetterServerListMod.enabled.get());
        assertTrue(BetterServerListMod.replaceScreen.get());
        assertTrue(BetterServerListMod.showLan.get());
        assertTrue(BetterServerListMod.confirmDelete.get());
        assertFalse(BetterServerListMod.debug.get());
    }

    @Test
    void hooksTolerateNulls() {
        // Loader screen hooks must never crash on odd input.
        assertNull(ScreenSwap.replacement(null));
        assertNull(ScreenSwap.backButton(null));
    }
}
