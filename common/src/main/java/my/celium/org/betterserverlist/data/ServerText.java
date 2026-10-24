package my.celium.org.betterserverlist.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Plain-text import and export, used by "Copy servers" and "Paste servers".
 * One server per line: {@code Name<TAB>address}. Pasting also takes
 * {@code Name address} (the last word is the address) or a bare address.
 */
public final class ServerText {
    /** Host names, IPv4, bracketed IPv6, optional port. */
    private static final Pattern ADDRESS = Pattern.compile("[A-Za-z0-9._\\-]+(:\\d{1,5})?|\\[[0-9A-Fa-f:.]+](:\\d{1,5})?");
    private static final int MAX_LENGTH = 255;

    private ServerText() {
    }

    public static String export(SavedList list) {
        StringBuilder out = new StringBuilder();
        for (SavedServer server : list.servers) {
            out.append(server.name.replace('\t', ' ').replace('\n', ' ')).append('\t').append(server.address.trim())
                    .append('\n');
        }
        return out.toString();
    }

    /** Every server found in {@code text}; lines that hold no address are skipped. */
    public static List<Library.Entry> parse(String text) {
        List<Library.Entry> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || line.length() > 2 * MAX_LENGTH) {
                continue;
            }
            String name;
            String address;
            int tab = line.lastIndexOf('\t');
            int space = line.lastIndexOf(' ');
            if (tab >= 0) {
                name = line.substring(0, tab).strip();
                address = line.substring(tab + 1).strip();
            } else if (space >= 0) {
                name = line.substring(0, space).strip();
                address = line.substring(space + 1).strip();
            } else {
                name = "";
                address = line;
            }
            if (!isAddress(address)) {
                continue;
            }
            out.add(new Library.Entry(name.isEmpty() ? address : name, address));
        }
        return out;
    }

    public static boolean isAddress(String address) {
        return address != null && !address.isEmpty() && address.length() <= MAX_LENGTH
                && ADDRESS.matcher(address).matches() && !address.startsWith(".") && !address.startsWith("-");
    }
}
