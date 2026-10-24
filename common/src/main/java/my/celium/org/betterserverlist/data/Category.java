package my.celium.org.betterserverlist.data;

/** A collapsible group of servers inside one list. */
public final class Category {
    /** Wiki palette: red, blue, green, purple, orange, grey. RGB, no alpha. */
    public static final int[] COLORS = {0xEF5350, 0x79B8FF, 0x7EE787, 0xD2A8FF, 0xFFA657, 0x8B949E};
    public static final String[] COLOR_NAMES = {"Red", "Blue", "Green", "Purple", "Orange", "Grey"};

    public String id;
    public String name = "";
    public int color = COLORS[1];
    public boolean collapsed;

    public Category() {
    }

    public Category(String name, int color) {
        this.id = Ids.next();
        this.name = name;
        this.color = color;
    }
}
