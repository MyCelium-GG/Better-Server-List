package my.celium.org.betterserverlist.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeTextTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long MIN = 60_000L;

    @Test
    void formats() {
        assertEquals("never", TimeText.ago(NOW, 0));
        assertEquals("just now", TimeText.ago(NOW, NOW - 5_000));
        assertEquals("1 min ago", TimeText.ago(NOW, NOW - MIN));
        assertEquals("2 hours ago", TimeText.ago(NOW, NOW - 125 * MIN));
        assertEquals("1 day ago", TimeText.ago(NOW, NOW - 25 * 60 * MIN));
        assertEquals("3 months ago", TimeText.ago(NOW, NOW - 95L * 24 * 60 * MIN));
        assertEquals("2 years ago", TimeText.ago(NOW, NOW - 800L * 24 * 60 * MIN));
        assertEquals("just now", TimeText.ago(NOW, NOW + 10_000));
    }
}
