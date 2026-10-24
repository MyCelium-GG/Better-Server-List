package my.celium.org.betterserverlist.data;

/** "5 min ago", "3 days ago": how long since a server was last played. */
public final class TimeText {
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private TimeText() {
    }

    public static String ago(long now, long then) {
        if (then <= 0) {
            return "never";
        }
        long delta = Math.max(0, now - then);
        if (delta < MINUTE) {
            return "just now";
        }
        if (delta < HOUR) {
            return plural(delta / MINUTE, "min", "min") + " ago";
        }
        if (delta < DAY) {
            return plural(delta / HOUR, "hour", "hours") + " ago";
        }
        if (delta < 60 * DAY) {
            return plural(delta / DAY, "day", "days") + " ago";
        }
        if (delta < 730 * DAY) {
            return plural(delta / (30 * DAY), "month", "months") + " ago";
        }
        return plural(delta / (365 * DAY), "year", "years") + " ago";
    }

    private static String plural(long n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }
}
