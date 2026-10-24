package my.celium.org.betterserverlist.data;

/** How servers are ordered inside each section. Only {@link #MANUAL} allows dragging. */
public enum Sort {
    MANUAL("Manual"),
    NAME("Name"),
    PLAYERS("Players"),
    PING("Ping"),
    RECENT("Last played");

    public final String label;

    Sort(String label) {
        this.label = label;
    }

    public Sort next() {
        Sort[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
