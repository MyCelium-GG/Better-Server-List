package my.celium.org.betterserverlist.data;

import java.util.UUID;

/** Short random ids for lists, categories and servers. */
public final class Ids {
    private Ids() {
    }

    public static String next() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
