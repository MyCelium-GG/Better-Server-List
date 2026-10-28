package my.celium.org.betterserverlist.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;

import my.celium.org.Mycel;
import my.celium.org.betterserverlist.BetterServerListMod;
import my.celium.org.betterserverlist.client.ContextMenu.Item;
import my.celium.org.betterserverlist.data.Category;
import my.celium.org.betterserverlist.data.Library;
import my.celium.org.betterserverlist.data.LibraryStore;
import my.celium.org.betterserverlist.data.Rows;
import my.celium.org.betterserverlist.data.SavedList;
import my.celium.org.betterserverlist.data.SavedList.Section;
import my.celium.org.betterserverlist.data.SavedServer;
import my.celium.org.betterserverlist.data.ServerText;
import my.celium.org.betterserverlist.data.Sort;
import my.celium.org.betterserverlist.data.TimeText;
import my.celium.org.client.MycelScreens;
import my.celium.org.client.MycelTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/**
 * The multiplayer screen. Left: the lists. Right: search, sort, the online
 * filter, and the open list's servers in sections (pinned, one per
 * category, uncategorized), then LAN worlds. Rows look like vanilla's (icon,
 * MOTD, players, ping) plus a category stripe, a pin star and a note mark.
 *
 * <p>Drag a server onto another server, a section header or a list on the
 * left (Ctrl copies to a list). Right-click anything for its menu. Keys:
 * arrows select, Shift+arrows move, Enter joins, Delete, F2 edits, F5
 * refreshes, Ctrl+F or just typing searches, Ctrl+C copies the address.
 * Client only.
 */
public final class ServerListScreen extends Screen {
    static final int TITLE_BAR = 20;
    static final int FOOTER = 28;
    static final int TOOL_Y = TITLE_BAR + 7;
    static final int TOOL_H = 16;
    static final int LIST_TOP = TOOL_Y + TOOL_H + 6;
    static final int HEADER_H = 18;
    /** 36 for the row itself plus a 2px gap. */
    static final int ROW_H = 38;
    static final int SIDE_ROWS_Y = TITLE_BAR + 22;
    static final int SIDE_ROW_H = 17;
    /** Slightly darker sidebar and the hairlines between areas. */
    static final int SIDEBAR = 0x30000000;
    static final int HAIRLINE = 0xFF2C2C2C;
    static final int HOVER = 0x0DFFFFFF;
    static final int SCROLL_STEP = 24;
    static final int DRAG_START = 4;
    static final long MESSAGE_MS = 3500;
    static final int ICON = 32;

    private static final Identifier INCOMPATIBLE = Identifier.withDefaultNamespace("server_list/incompatible");
    private static final Identifier UNREACHABLE = Identifier.withDefaultNamespace("server_list/unreachable");
    private static final Identifier JOIN = Identifier.withDefaultNamespace("server_list/join");
    private static final Identifier JOIN_HIGHLIGHTED = Identifier.withDefaultNamespace("server_list/join_highlighted");
    private static final Identifier[] PING = {
            Identifier.withDefaultNamespace("server_list/ping_1"), Identifier.withDefaultNamespace("server_list/ping_2"),
            Identifier.withDefaultNamespace("server_list/ping_3"), Identifier.withDefaultNamespace("server_list/ping_4"),
            Identifier.withDefaultNamespace("server_list/ping_5")};
    private static final Identifier[] PINGING = {
            Identifier.withDefaultNamespace("server_list/pinging_1"), Identifier.withDefaultNamespace("server_list/pinging_2"),
            Identifier.withDefaultNamespace("server_list/pinging_3"), Identifier.withDefaultNamespace("server_list/pinging_4"),
            Identifier.withDefaultNamespace("server_list/pinging_5")};

    /** One drawn line of the server panel. */
    private sealed interface Line permits HeaderLine, ServerLine, LanHeaderLine, LanLine {
    }

    private record HeaderLine(Rows.Header header) implements Line {
    }

    private record ServerLine(SavedServer server) implements Line {
    }

    private record LanHeaderLine() implements Line {
    }

    private record LanLine(LanServer server) implements Line {
    }

    /** Where a dragged server would land. */
    private record Drop(SavedServer target, boolean after, Rows.Header header, SavedList list, int lineY) {
    }

    private record Chip(String label, int x, int w, boolean on, String tooltip) {
    }

    private final Screen parent;
    private final LibraryStore store;
    private final VanillaServers vanilla = new VanillaServers();
    private final Pings pings;
    private final Library library;
    private final LanServerDetection.LanServerList lanList = new LanServerDetection.LanServerList();
    private LanServerDetection.LanServerDetector lanDetector;
    private List<LanServer> lanServers = List.of();

    private List<Line> lines = List.of();
    private int[] tops = new int[0];
    private int contentHeight;
    private boolean rowsDirty = true;
    private boolean mirrorDirty;
    private long lastMirrorSave;

    private String query = "";
    private EditBox search;
    private FlatButton joinButton;
    private FlatButton editButton;
    private FlatButton deleteButton;
    private double scroll;
    private int sideScroll;

    private String selectedId;
    private LanServer selectedLan;
    private ContextMenu menu;
    private SavedServer pressed;
    private double pressX;
    private double pressY;
    private boolean dragging;
    private String message;
    private int messageColor;
    private long messageUntil;

    public ServerListScreen(Screen parent) {
        super(Component.literal("Better Server List"));
        this.parent = parent;
        this.store = new LibraryStore(Mycel.getConfigDir().resolve(BetterServerListMod.ID + "-servers.json"));
        this.pings = new Pings(vanilla, () -> rowsDirty = true, () -> mirrorDirty = true);
        vanilla.load();
        Library loaded = null;
        try {
            loaded = store.load();
        } catch (IOException e) {
            BetterServerListMod.warn("{}", e.getMessage());
            flash("Saved lists were unreadable; started over from servers.dat", MycelTheme.WARN);
        }
        if (loaded == null) {
            library = Library.fromVanilla(vanilla.entries());
            BetterServerListMod.debug("New library from servers.dat: {} servers", library.active().servers.size());
            save();
        } else {
            library = loaded;
            int imported = library.importMissing(vanilla.entries());
            if (imported > 0) {
                flash("Added " + imported + " new server" + (imported == 1 ? "" : "s") + " from servers.dat",
                        MycelTheme.GOOD);
                save();
            }
        }
    }

    private SavedList list() {
        return library.active();
    }

    // ================================================================= state

    private void save() {
        rowsDirty = true;
        try {
            store.save(library);
        } catch (IOException e) {
            BetterServerListMod.warn("Could not save {}: {}", store.file(), e.toString());
            flash("Could not save your lists (see log)", MycelTheme.BAD);
        }
        saveMirror();
    }

    private void saveMirror() {
        try {
            vanilla.save(library);
            mirrorDirty = false;
            lastMirrorSave = Util.getMillis();
        } catch (RuntimeException e) {
            BetterServerListMod.warn("Could not update servers.dat: {}", e.toString());
        }
    }

    private void flash(String text, int color) {
        message = text;
        messageColor = color;
        messageUntil = Util.getMillis() + MESSAGE_MS;
    }

    private SavedServer selected() {
        return list().server(selectedId);
    }

    private Iterable<SavedServer> everyServer() {
        List<SavedServer> all = new ArrayList<>();
        for (SavedList each : library.lists) {
            all.addAll(each.servers);
        }
        return all;
    }

    private void rebuild() {
        rowsDirty = false;
        List<Line> out = new ArrayList<>();
        for (Rows.Row row : Rows.build(list(), query, library.sort, library.hideOffline, pings)) {
            out.add(row instanceof Rows.Header header ? new HeaderLine(header) : new ServerLine(((Rows.ServerRow) row).server()));
        }
        if (BetterServerListMod.showLan.get()) {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            List<LanServer> matching = new ArrayList<>();
            for (LanServer lan : lanServers) {
                if (needle.isEmpty() || lan.getMotd().toLowerCase(Locale.ROOT).contains(needle)
                        || lan.getAddress().contains(needle)) {
                    matching.add(lan);
                }
            }
            if (needle.isEmpty() || !matching.isEmpty()) {
                out.add(new LanHeaderLine());
                if (!library.lanCollapsed || !needle.isEmpty()) {
                    for (LanServer lan : matching) {
                        out.add(new LanLine(lan));
                    }
                }
            }
        }
        lines = out;
        tops = new int[out.size()];
        int y = 0;
        for (int i = 0; i < out.size(); i++) {
            tops[i] = y;
            y += heightOf(out.get(i));
        }
        contentHeight = y;
        clampScroll();
        for (SavedServer server : list().servers) {
            pings.start(server);
        }
        // Only what is on screen stays selected: Join never acts on a hidden server.
        if (selectedId != null && out.stream().noneMatch(line -> line instanceof ServerLine server
                && server.server().id.equals(selectedId))) {
            selectedId = null;
        }
        if (selectedLan != null && out.stream().noneMatch(line -> line instanceof LanLine lan
                && lan.server() == selectedLan)) {
            selectedLan = null;
        }
        updateButtons();
    }

    private static int heightOf(Line line) {
        return line instanceof HeaderLine || line instanceof LanHeaderLine ? HEADER_H : ROW_H;
    }

    // ================================================================ layout

    private int sideW() {
        return Math.max(92, Math.min(140, width / 5));
    }

    private int panelBottom() {
        return height - FOOTER;
    }

    private int listX0() {
        return sideW() + 10;
    }

    private int listX1() {
        return width - 12;
    }

    private int listBottom() {
        return panelBottom() - 2;
    }

    private int viewHeight() {
        return listBottom() - LIST_TOP;
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewHeight())));
    }

    private int sideVisibleRows() {
        return Math.max(1, (panelBottom() - 30 - SIDE_ROWS_Y) / SIDE_ROW_H);
    }

    private void clampSideScroll() {
        sideScroll = Math.max(0, Math.min(sideScroll, library.lists.size() + 1 - sideVisibleRows()));
    }

    /** Index of a list in the sidebar under the mouse, {@code lists.size()} for "+ New list", or -1. */
    private int sideRowAt(double mx, double my) {
        if (mx < 4 || mx >= sideW() - 4 || my < SIDE_ROWS_Y) {
            return -1;
        }
        int row = (int) ((my - SIDE_ROWS_Y) / SIDE_ROW_H);
        if (row >= sideVisibleRows()) {
            return -1;
        }
        int index = row + sideScroll;
        return index <= library.lists.size() ? index : -1;
    }

    private int lineAt(double mx, double my) {
        if (mx < listX0() - 2 || mx >= listX1() + 2 || my < LIST_TOP || my >= listBottom()) {
            return -1;
        }
        double y = my - LIST_TOP + scroll;
        for (int i = 0; i < lines.size(); i++) {
            if (y >= tops[i] && y < tops[i] + heightOf(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int lineY(int index) {
        return LIST_TOP + tops[index] - (int) scroll;
    }

    private List<Chip> chips() {
        List<Chip> out = new ArrayList<>();
        String[] labels = {"+ Category", "Online only", "Sort: " + library.sort.label};
        String[] tips = {"New category in this list", "Show only servers that answer, or all of them",
                "Change the order inside each section. Dragging works with Manual."};
        int x = listX1();
        for (int i = 0; i < labels.length; i++) {
            int w = font.width(labels[i]) + (i == 1 ? 20 : 12);
            x -= w;
            out.add(new Chip(labels[i], x, w, i == 1 && library.hideOffline || i == 2 && library.sort != Sort.MANUAL,
                    tips[i]));
            x -= 4;
        }
        return out;
    }

    private int chipsLeft() {
        List<Chip> chips = chips();
        return chips.get(chips.size() - 1).x();
    }

    // =============================================================== widgets

    @Override
    protected void init() {
        // A borderless box inside a themed frame drawn in renderToolbar.
        int boxW = Math.max(40, chipsLeft() - 6 - listX0());
        search = new EditBox(font, listX0() + 6, TOOL_Y + 4, boxW - 12, 9, search, Component.literal("Search"));
        search.setBordered(false);
        search.setTextColor(MycelTheme.TEXT);
        // The hint is not clipped to the box, so it must fit.
        String hint = "Search name, address, MOTD, note…";
        if (font.width(hint) > boxW - 14) {
            hint = font.width("Search…") <= boxW - 14 ? "Search…" : "";
        }
        search.setHint(Component.literal(hint).withColor(MycelTheme.FAINT));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            scroll = 0;
            rowsDirty = true;
        });
        addRenderableWidget(search);

        int y = height - FOOTER + (FOOTER - FlatButton.HEIGHT) / 2;
        int x = 8;
        joinButton = footerButton(x, y, Component.literal("Join"), true, this::joinSelected);
        x += joinButton.getWidth() + 4;
        x += footerButton(x, y, Component.literal("Add"), false, () -> addServer(null)).getWidth() + 4;
        x += footerButton(x, y, Component.literal("Direct"), false, this::directConnect).getWidth() + 4;
        editButton = footerButton(x, y, Component.literal("Edit"), false, () -> {
            SavedServer server = selected();
            if (server != null) {
                editServer(server);
            }
        });
        x += editButton.getWidth() + 4;
        deleteButton = footerButton(x, y, Component.literal("Delete"), false, () -> {
            SavedServer server = selected();
            if (server != null) {
                deleteServer(server);
            }
        });
        int right = width - 8;
        Component back = CommonComponents.GUI_BACK;
        right -= Math.max(36, FlatButton.widthFor(font, back));
        footerButton(right, y, back, false, this::onClose);
        Component vanillaLabel = Component.literal("Vanilla");
        right -= Math.max(36, FlatButton.widthFor(font, vanillaLabel)) + 4;
        footerButton(right, y, vanillaLabel, false, () -> ScreenSwap.openVanilla(parent))
                .setTooltip(Tooltip.create(Component.literal("Open the vanilla server list")));
        Component refreshLabel = Component.literal("Refresh");
        right -= Math.max(36, FlatButton.widthFor(font, refreshLabel)) + 4;
        footerButton(right, y, refreshLabel, false, this::refresh)
                .setTooltip(Tooltip.create(Component.literal("Ping every server again (F5)")));
        rowsDirty = true;
        clampSideScroll();
        updateButtons();
    }

    private FlatButton footerButton(int x, int y, Component label, boolean primary, Runnable action) {
        return addRenderableWidget(new FlatButton(x, y, Math.max(36, FlatButton.widthFor(font, label)), label, primary,
                action));
    }

    private void updateButtons() {
        if (joinButton == null) {
            return;
        }
        boolean server = selected() != null;
        joinButton.active = server || selectedLan != null;
        editButton.active = server;
        deleteButton.active = server;
    }

    @Override
    public void added() {
        super.added();
        if (BetterServerListMod.showLan.get() && lanDetector == null) {
            try {
                lanDetector = new LanServerDetection.LanServerDetector(lanList);
                lanDetector.start();
            } catch (Exception e) {
                BetterServerListMod.warn("Unable to start LAN server detection: {}", e.toString());
            }
        }
        rowsDirty = true;
    }

    @Override
    public void removed() {
        if (lanDetector != null) {
            lanDetector.interrupt();
            lanDetector = null;
        }
        pings.stop(everyServer());
        if (mirrorDirty) {
            saveMirror();
        }
        dragging = false;
        pressed = null;
        menu = null;
    }

    @Override
    public void tick() {
        super.tick();
        pings.tick();
        List<LanServer> found = lanList.takeDirtyServers();
        if (found != null) {
            lanServers = List.copyOf(found);
            rowsDirty = true;
        }
        if (mirrorDirty && Util.getMillis() - lastMirrorSave > 2000) {
            saveMirror();
        }
        if (rowsDirty) {
            rebuild();
        }
    }

    @Override
    public void onClose() {
        MycelScreens.open(parent);
    }

    // =============================================================== actions

    private void joinSelected() {
        SavedServer server = selected();
        if (server != null) {
            join(server);
        } else if (selectedLan != null) {
            joinLan(selectedLan);
        }
    }

    private void join(SavedServer server) {
        server.lastJoined = System.currentTimeMillis();
        server.joins++;
        save();
        connect(pings.data(server));
    }

    private void joinLan(LanServer lan) {
        connect(new ServerData(lan.getMotd(), lan.getAddress(), ServerData.Type.LAN));
    }

    private void connect(ServerData data) {
        BetterServerListMod.debug("Joining {}", data.ip);
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip.trim()), data, false, null);
    }

    private void directConnect() {
        ServerData typed = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
        MycelScreens.open(new DirectJoinServerScreen(this, ok -> {
            if (ok) {
                connect(vanilla.rememberDirect(typed));
            } else {
                MycelScreens.open(this);
            }
        }, typed));
    }

    /** Adds a server to {@code section}, or next to the selection's category when {@code null}. */
    private void addServer(Section section) {
        ServerData typed = new ServerData("", "", ServerData.Type.OTHER);
        MycelScreens.open(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), ok -> {
            if (ok && !typed.ip.isBlank()) {
                SavedServer server = new SavedServer(typed.name.isBlank() ? typed.ip.trim() : typed.name, typed.ip.trim());
                Section target = section;
                if (target == null) {
                    SavedServer near = selected();
                    target = near == null ? Section.NONE : Section.of(near.category);
                }
                list().add(server, target);
                ServerData data = pings.data(server);
                data.setResourcePackStatus(typed.getResourcePackStatus());
                selectedId = server.id;
                selectedLan = null;
                save();
                flash("Added " + server.name, MycelTheme.GOOD);
            }
            MycelScreens.open(this);
        }, typed));
    }

    private void editServer(SavedServer server) {
        ServerData current = pings.data(server);
        ServerData editing = new ServerData(server.name, server.address, ServerData.Type.OTHER);
        editing.setResourcePackStatus(current.getResourcePackStatus());
        MycelScreens.open(new ManageServerScreen(this, Component.translatable("manageServer.edit.title"), ok -> {
            if (ok && !editing.ip.isBlank()) {
                server.name = editing.name.isBlank() ? editing.ip.trim() : editing.name;
                server.address = editing.ip.trim();
                pings.data(server).setResourcePackStatus(editing.getResourcePackStatus());
                save();
            }
            MycelScreens.open(this);
        }, editing));
    }

    private void deleteServer(SavedServer server) {
        confirm("Delete " + server.name + "?", "It is removed from \"" + list().name + "\" only.", "Delete", () -> {
            list().remove(server.id);
            if (server.id.equals(selectedId)) {
                selectedId = null;
            }
            save();
            flash("Deleted " + server.name, MycelTheme.DIM);
        });
    }

    private void confirm(String title, String detail, String yes, Runnable action) {
        if (!BetterServerListMod.confirmDelete.get()) {
            action.run();
            return;
        }
        MycelScreens.open(new ConfirmScreen(ok -> {
            if (ok) {
                action.run();
            }
            MycelScreens.open(this);
        }, Component.literal(title), Component.literal(detail), Component.literal(yes), CommonComponents.GUI_CANCEL));
    }

    private void prompt(String title, String label, String initial, int max, boolean allowEmpty,
            java.util.function.Consumer<String> done) {
        MycelScreens.open(new TextPromptScreen(this, title, label, initial, max, allowEmpty, done));
    }

    private void refresh() {
        pings.refresh(list().servers);
        rowsDirty = true;
        flash("Refreshing " + list().servers.size() + " servers", MycelTheme.DIM);
    }

    private void togglePin(SavedServer server) {
        if (server.pinned) {
            server.pinned = false;
            list().add(server, Section.of(server.category));
        } else {
            list().add(server, Section.PINNED);
        }
        save();
    }

    private void newList() {
        prompt("New list", "Name", "", 32, false, name -> {
            SavedList created = library.addList(name);
            library.active = created.id;
            selectedId = null;
            scroll = 0;
            sideScroll = library.lists.size();
            clampSideScroll();
            save();
        });
    }

    private void newCategory(SavedServer moveIn) {
        prompt("New category", "Name", "", 32, false, name -> {
            Category category = list().addCategory(name);
            if (moveIn != null) {
                moveToCategory(moveIn, category.id);
            }
            save();
        });
    }

    private void moveToCategory(SavedServer server, String categoryId) {
        if (server.pinned) {
            server.category = categoryId;
        } else {
            list().add(server, Section.of(categoryId));
        }
        save();
    }

    private void switchList(SavedList target) {
        if (target == list()) {
            return;
        }
        library.active = target.id;
        selectedId = null;
        scroll = 0;
        save();
    }

    private void sendToList(SavedServer server, SavedList target, boolean copy) {
        SavedServer moving = server;
        if (copy) {
            moving = server.copy();
        } else {
            list().remove(server.id);
            if (server.id.equals(selectedId)) {
                selectedId = null;
            }
        }
        boolean pinned = moving.pinned;
        moving.category = null;
        target.add(moving, pinned ? Section.PINNED : Section.NONE);
        save();
        flash((copy ? "Copied " : "Moved ") + server.name + " to " + target.name, MycelTheme.GOOD);
    }

    private void copyServers(SavedList source) {
        minecraft.keyboardHandler.setClipboard(ServerText.export(source));
        flash("Copied " + source.servers.size() + " servers as text", MycelTheme.GOOD);
    }

    private void pasteServers(SavedList target) {
        List<Library.Entry> entries = ServerText.parse(minecraft.keyboardHandler.getClipboard());
        int added = 0;
        for (Library.Entry entry : entries) {
            if (!target.containsAddress(entry.address())) {
                target.add(new SavedServer(entry.name(), entry.address()), Section.NONE);
                added++;
            }
        }
        if (entries.isEmpty()) {
            flash("No server addresses on the clipboard", MycelTheme.WARN);
            return;
        }
        save();
        int skipped = entries.size() - added;
        flash("Pasted " + added + " server" + (added == 1 ? "" : "s") + (skipped > 0 ? ", " + skipped + " already here" : ""),
                MycelTheme.GOOD);
    }

    // ================================================================= menus

    private void openServerMenu(SavedServer server, int x, int y) {
        List<Item> items = new ArrayList<>();
        items.add(Item.of("Join", () -> join(server)));
        items.add(Item.of("Edit…", () -> editServer(server)));
        items.add(Item.of(server.pinned ? "Unpin" : "Pin to top", () -> togglePin(server)));
        items.add(Item.sub("Move to category", () -> categoryItems(server)));
        if (library.lists.size() > 1) {
            items.add(Item.sub("Move to list", () -> listItems(server, false)));
            items.add(Item.sub("Copy to list", () -> listItems(server, true)));
        }
        items.add(Item.of(server.note.isEmpty() ? "Add note…" : "Edit note…", () -> prompt("Note for " + server.name,
                "Shown when you hover the server's name", server.note, 200, true, note -> {
                    server.note = note;
                    save();
                })));
        items.add(Item.of("Copy address", () -> {
            minecraft.keyboardHandler.setClipboard(server.address);
            flash("Copied " + server.address, MycelTheme.GOOD);
        }));
        items.add(Item.danger("Delete", () -> deleteServer(server)));
        menu = new ContextMenu(server.name, x, y, items);
    }

    private List<Item> categoryItems(SavedServer server) {
        List<Item> items = new ArrayList<>();
        items.add(server.category == null ? Item.current("Uncategorized") : Item.of("Uncategorized", () -> moveToCategory(server, null)));
        for (Category category : list().categories) {
            items.add(category.id.equals(server.category) ? Item.current(category.name)
                    : new Item(category.name, 0xFF000000 | category.color, () -> moveToCategory(server, category.id), null, true));
        }
        items.add(new Item("+ New category…", MycelTheme.LINK, () -> newCategory(server), null, true));
        return items;
    }

    private List<Item> listItems(SavedServer server, boolean copy) {
        List<Item> items = new ArrayList<>();
        for (SavedList each : library.lists) {
            if (each != list()) {
                items.add(Item.of(each.name, () -> sendToList(server, each, copy)));
            }
        }
        return items;
    }

    private void openHeaderMenu(Rows.Header header, int x, int y) {
        List<Item> items = new ArrayList<>();
        items.add(Item.of(header.collapsed() ? "Expand" : "Collapse", () -> toggleCollapsed(header)));
        items.add(Item.of("Add server here…", () -> addServer(header.section())));
        switch (header.kind()) {
            case PINNED -> items.add(Item.of("Unpin all", () -> {
                for (SavedServer server : new ArrayList<>(list().servers)) {
                    if (server.pinned) {
                        server.pinned = false;
                        list().add(server, Section.of(server.category));
                    }
                }
                save();
            }));
            case CATEGORY -> {
                Category category = header.category();
                items.add(Item.of("Rename…", () -> prompt("Rename category", "Name", category.name, 32, false, name -> {
                    category.name = name;
                    save();
                })));
                items.add(Item.sub("Color", () -> {
                    List<Item> colors = new ArrayList<>();
                    for (int i = 0; i < Category.COLORS.length; i++) {
                        int color = Category.COLORS[i];
                        colors.add(new Item((color == category.color ? "✔ " : "■ ") + Category.COLOR_NAMES[i],
                                0xFF000000 | color, () -> {
                                    category.color = color;
                                    save();
                                }, null, true));
                    }
                    return colors;
                }));
                int index = list().categories.indexOf(category);
                items.add(index > 0 ? Item.of("Move up", () -> {
                    list().moveCategory(category.id, -1);
                    save();
                }) : Item.off("Move up"));
                items.add(index < list().categories.size() - 1 ? Item.of("Move down", () -> {
                    list().moveCategory(category.id, 1);
                    save();
                }) : Item.off("Move down"));
                items.add(Item.danger("Delete category", () -> confirm("Delete category " + category.name + "?",
                        "Its servers stay in the list, uncategorized.", "Delete", () -> {
                            list().removeCategory(category.id);
                            save();
                        })));
            }
            case UNCATEGORIZED -> {
            }
        }
        menu = new ContextMenu(header.title(), x, y, items);
    }

    private void openListMenu(SavedList target, int x, int y) {
        List<Item> items = new ArrayList<>();
        items.add(Item.of("Rename…", () -> prompt("Rename list", "Name", target.name, 32, false, name -> {
            target.name = name;
            save();
        })));
        int index = library.lists.indexOf(target);
        items.add(index > 0 ? Item.of("Move up", () -> {
            library.moveList(target.id, -1);
            save();
        }) : Item.off("Move up"));
        items.add(index < library.lists.size() - 1 ? Item.of("Move down", () -> {
            library.moveList(target.id, 1);
            save();
        }) : Item.off("Move down"));
        items.add(Item.of("Copy servers as text", () -> copyServers(target)));
        items.add(Item.of("Paste servers", () -> pasteServers(target)));
        items.add(library.lists.size() > 1 ? Item.danger("Delete list", () -> confirm("Delete list " + target.name + "?",
                target.servers.size() + " servers in it are deleted with it.", "Delete", () -> {
                    library.removeList(target.id);
                    selectedId = null;
                    clampSideScroll();
                    save();
                })) : Item.off("Delete list (last one)"));
        menu = new ContextMenu(target.name, x, y, items);
    }

    private void openBlankMenu(int x, int y) {
        List<Item> items = new ArrayList<>();
        items.add(Item.of("Add server…", () -> addServer(Section.NONE)));
        items.add(Item.of("New category…", () -> newCategory(null)));
        items.add(Item.of("Paste servers", () -> pasteServers(list())));
        items.add(Item.of("Copy servers as text", () -> copyServers(list())));
        menu = new ContextMenu(list().name, x, y, items);
    }

    private void toggleCollapsed(Rows.Header header) {
        switch (header.kind()) {
            case PINNED -> list().pinnedCollapsed = !list().pinnedCollapsed;
            case CATEGORY -> header.category().collapsed = !header.category().collapsed;
            case UNCATEGORIZED -> list().uncategorizedCollapsed = !list().uncategorizedCollapsed;
        }
        save();
    }

    // ================================================================ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float a) {
        if (rowsDirty) {
            rebuild();
        }
        if (dragging) {
            autoScroll(mouseY);
        }
        boolean blocked = menu != null && menu.isOpen();
        int mx = blocked || dragging ? -1 : mouseX;
        int my = blocked || dragging ? -1 : mouseY;
        // Chrome first: widgets draw on top of it.
        gfx.fill(0, 0, width, height, 0xE6171717);
        gfx.fill(0, 0, width, TITLE_BAR, MycelTheme.ACCENT_DARK);
        gfx.fill(0, TITLE_BAR - 1, width, TITLE_BAR, MycelTheme.HEADER_EDGE);
        gfx.text(font, title, 8, 6, MycelTheme.HEADING);
        renderTitleInfo(gfx);
        gfx.fill(0, TITLE_BAR, sideW(), panelBottom(), SIDEBAR);
        gfx.fill(sideW(), TITLE_BAR, sideW() + 1, panelBottom(), HAIRLINE);
        gfx.fill(0, panelBottom(), width, panelBottom() + 1, HAIRLINE);
        gfx.fill(0, panelBottom() + 1, width, height, SIDEBAR);
        renderToolbar(gfx, mx, my);
        super.extractRenderState(gfx, mx, my, a);
        renderSidebar(gfx, mx, my, mouseX, mouseY);
        renderLines(gfx, mx, my);
        if (dragging) {
            renderDrag(gfx, mouseX, mouseY);
        }
        if (menu != null) {
            if (menu.isOpen()) {
                menu.render(gfx, font, mouseX, mouseY, width, height);
            } else {
                menu = null;
            }
        }
    }

    private void renderTitleInfo(GuiGraphicsExtractor gfx) {
        String text;
        int color;
        if (message != null && Util.getMillis() < messageUntil) {
            text = message;
            color = messageColor;
        } else {
            int total = list().servers.size();
            int online = 0;
            for (SavedServer server : list().servers) {
                if (pings.ping(server) >= 0) {
                    online++;
                }
            }
            text = total + (total == 1 ? " server" : " servers") + " · " + online + " online";
            color = 0xFFFFCDD2;
        }
        int max = width - font.width(title) - 32;
        text = MycelTheme.fit(font, text, max);
        gfx.text(font, text, width - 8 - font.width(text), 6, color);
    }

    private void renderSidebar(GuiGraphicsExtractor gfx, int mx, int my, int rawX, int rawY) {
        gfx.text(font, "LISTS", 10, TITLE_BAR + 9, MycelTheme.FAINT);
        int x0 = 4;
        int x1 = sideW() - 4;
        int dropRow = dragging ? sideRowAt(rawX, rawY) : -1;
        for (int row = 0; row < sideVisibleRows(); row++) {
            int index = row + sideScroll;
            if (index > library.lists.size()) {
                break;
            }
            int y = SIDE_ROWS_Y + row * SIDE_ROW_H;
            int h = SIDE_ROW_H - 1;
            boolean hover = sideRowAt(mx, my) == index;
            if (index == library.lists.size()) {
                if (hover) {
                    gfx.fill(x0, y, x1, y + h, HOVER);
                }
                gfx.text(font, "+ New list", x0 + 6, y + 4, hover ? MycelTheme.HEADING : MycelTheme.LINK);
                continue;
            }
            SavedList each = library.lists.get(index);
            boolean active = each == list();
            boolean target = dropRow == index && !active;
            if (active) {
                gfx.fill(x0, y, x1, y + h, MycelTheme.SURFACE);
                gfx.fill(x0, y, x0 + 2, y + h, MycelTheme.ACCENT);
            } else if (target || hover) {
                gfx.fill(x0, y, x1, y + h, HOVER);
            }
            if (target) {
                gfx.outline(x0, y, x1 - x0, h, MycelTheme.ACCENT);
            }
            String count = String.valueOf(each.servers.size());
            gfx.text(font, count, x1 - 5 - font.width(count), y + 4, active ? MycelTheme.DIM : MycelTheme.FAINT);
            gfx.text(font, MycelTheme.fit(font, each.name, x1 - x0 - 16 - font.width(count)), x0 + 6, y + 4,
                    active ? MycelTheme.HEADING : MycelTheme.TEXT);
        }
        int total = library.lists.size() + 1;
        if (total > sideVisibleRows()) {
            drawScrollbar(gfx, sideW() - 3, SIDE_ROWS_Y, sideVisibleRows() * SIDE_ROW_H, sideScroll,
                    total - sideVisibleRows());
        }
        if (dragging) {
            gfx.text(font, MycelTheme.fit(font, "Drop on a list", x1 - x0 - 4), x0 + 6, panelBottom() - 24,
                    MycelTheme.FAINT);
            gfx.text(font, MycelTheme.fit(font, "Ctrl: copy", x1 - x0 - 4), x0 + 6, panelBottom() - 13,
                    MycelTheme.FAINT);
        }
    }

    /** The search field's frame and the chips beside it. */
    private void renderToolbar(GuiGraphicsExtractor gfx, int mx, int my) {
        int x0 = listX0();
        int x1 = chipsLeft() - 6;
        boolean focused = search != null && search.isFocused();
        gfx.fill(x0, TOOL_Y, x1, TOOL_Y + TOOL_H, focused ? MycelTheme.ACCENT : MycelTheme.BORDER);
        gfx.fill(x0 + 1, TOOL_Y + 1, x1 - 1, TOOL_Y + TOOL_H - 1, 0xFF1B1B1B);
        for (Chip chip : chips()) {
            boolean hover = inside(mx, my, chip.x(), TOOL_Y, chip.w(), TOOL_H);
            int edge = chip.on() ? MycelTheme.ACCENT : hover ? 0xFF5A5A5A : MycelTheme.BORDER;
            int fill = chip.on() ? MycelTheme.ACCENT_DARK : hover ? MycelTheme.BORDER : MycelTheme.SURFACE;
            gfx.fill(chip.x(), TOOL_Y, chip.x() + chip.w(), TOOL_Y + TOOL_H, edge);
            gfx.fill(chip.x() + 1, TOOL_Y + 1, chip.x() + chip.w() - 1, TOOL_Y + TOOL_H - 1, fill);
            int tx = chip.x() + 6;
            if (chip.label().equals("Online only")) {
                // A small switch: green dot when on.
                gfx.fill(tx, TOOL_Y + 6, tx + 5, TOOL_Y + 11, chip.on() ? MycelTheme.GOOD : MycelTheme.OFF);
                tx += 8;
            }
            gfx.text(font, chip.label(), tx, TOOL_Y + 4, chip.on() || hover ? MycelTheme.HEADING : MycelTheme.DIM);
            if (hover) {
                gfx.setTooltipForNextFrame(font, Component.literal(chip.tooltip()), mx, my);
            }
        }
    }

    private void renderLines(GuiGraphicsExtractor gfx, int mx, int my) {
        int x0 = listX0();
        int x1 = listX1();
        if (lines.stream().noneMatch(line -> line instanceof ServerLine || line instanceof HeaderLine)) {
            String empty = !query.isBlank() || library.hideOffline ? "No servers match"
                    : "No servers in this list yet";
            int y = LIST_TOP + contentHeight + 20;
            gfx.centeredText(font, empty, (x0 + x1) / 2, y, MycelTheme.TEXT);
            gfx.centeredText(font, MycelTheme.fit(font, "Add Server, or right-click to paste a list", x1 - x0),
                    (x0 + x1) / 2, y + 12, MycelTheme.DIM);
        }
        if (lines.isEmpty()) {
            return;
        }
        gfx.enableScissor(x0 - 2, LIST_TOP, x1 + 6, listBottom());
        int hovered = lineAt(mx, my);
        for (int i = 0; i < lines.size(); i++) {
            int y = lineY(i);
            int h = heightOf(lines.get(i));
            if (y + h < LIST_TOP || y > listBottom()) {
                continue;
            }
            switch (lines.get(i)) {
                case HeaderLine header -> renderHeader(gfx, header.header(), x0, x1, y, i == hovered);
                case ServerLine server -> renderServer(gfx, server.server(), i, x0, x1, y, i == hovered, mx, my);
                case LanHeaderLine lan -> renderLanHeader(gfx, x0, x1, y, i == hovered);
                case LanLine lan -> renderLan(gfx, lan.server(), x0, x1, y, i == hovered, mx, my);
            }
        }
        gfx.disableScissor();
        drawScrollbar(gfx, x1 + 4, LIST_TOP, viewHeight(), (int) scroll, Math.max(0, contentHeight - viewHeight()));
    }

    private void renderHeader(GuiGraphicsExtractor gfx, Rows.Header header, int x0, int x1, int y, boolean hover) {
        if (hover) {
            gfx.fill(x0 - 2, y + 1, x1 + 2, y + HEADER_H - 1, HOVER);
        }
        int ty = y + 6;
        chevron(gfx, x0 + 1, ty + 1, header.collapsed());
        int x = x0 + 10;
        int color = 0xFF000000 | header.color();
        switch (header.kind()) {
            case PINNED -> {
                gfx.text(font, "★", x, ty, MycelTheme.WARN);
                x += font.width("★") + 4;
            }
            case CATEGORY -> {
                gfx.fill(x, ty + 1, x + 6, ty + 7, color);
                x += 10;
            }
            case UNCATEGORIZED -> {
            }
        }
        String title = MycelTheme.fit(font, header.title(), Math.max(20, x1 - x - 40));
        gfx.text(font, title, x, ty, header.kind() == Rows.Kind.UNCATEGORIZED ? MycelTheme.DIM : MycelTheme.HEADING);
        x += font.width(title) + 6;
        x = countPill(gfx, String.valueOf(header.count()), x, ty) + 6;
        if (x < x1) {
            gfx.fill(x, ty + 4, x1, ty + 5, HAIRLINE);
        }
    }

    /** A small triangle: pointing right when collapsed, down when open. */
    private static void chevron(GuiGraphicsExtractor gfx, int x, int y, boolean collapsed) {
        for (int i = 0; i < 3; i++) {
            if (collapsed) {
                gfx.fill(x + 1 + i, y + i, x + 2 + i, y + 6 - i, MycelTheme.DIM);
            } else {
                gfx.fill(x + i, y + 1 + i, x + 6 - i, y + 2 + i, MycelTheme.DIM);
            }
        }
    }

    /** A number in a dark pill; returns its right edge. */
    private int countPill(GuiGraphicsExtractor gfx, String count, int x, int ty) {
        int w = font.width(count) + 6;
        gfx.fill(x, ty - 2, x + w, ty + 9, MycelTheme.SURFACE);
        gfx.text(font, count, x + 3, ty, MycelTheme.DIM);
        return x + w;
    }

    private void renderServer(GuiGraphicsExtractor gfx, SavedServer server, int index, int x0, int x1, int y,
            boolean hover, int mx, int my) {
        ServerData data = pings.data(server);
        boolean picked = server.id.equals(selectedId);
        int bottom = y + ROW_H - 2;
        if (picked) {
            gfx.fill(x0 - 2, y, x1 + 2, bottom, MycelTheme.SURFACE);
            gfx.fill(x0 - 2, y, x0, bottom, MycelTheme.ACCENT);
        } else if (hover) {
            gfx.fill(x0 - 2, y, x1 + 2, bottom, HOVER);
        }
        int ix = x0 + 4;
        int iy = y + 2;
        gfx.blit(RenderPipelines.GUI_TEXTURED, pings.icon(server), ix, iy, 0.0F, 0.0F, ICON, ICON, ICON, ICON);

        // Right side: players, then the ping bars.
        int pingX = x1 - 12;
        Identifier status = statusSprite(data, index);
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, status, pingX, y + 4, 10, 8);
        Component players = data.state() == ServerData.State.INCOMPATIBLE
                ? data.version.copy().withStyle(ChatFormatting.RED) : data.status;
        int playersW = players == null ? 0 : font.width(players);
        int playersX = pingX - 4 - playersW;
        if (players != null) {
            gfx.text(font, players, playersX, y + 4, MycelTheme.DIM);
        }

        int tx = ix + ICON + 6;
        String note = server.note.isEmpty() ? "" : " ✎";
        int nameW = Math.max(10, playersX - 6 - tx - font.width(note));
        String name = MycelTheme.fit(font, server.name, nameW);
        gfx.text(font, name, tx, y + 4, MycelTheme.HEADING);
        if (!note.isEmpty()) {
            gfx.text(font, note, tx + font.width(name), y + 4, MycelTheme.VALUE);
        }
        List<FormattedCharSequence> motd = font.split(data.motd == null ? Component.empty() : data.motd, x1 - tx - 14);
        for (int i = 0; i < Math.min(2, motd.size()); i++) {
            gfx.text(font, motd.get(i), tx, y + 15 + 9 * i, MycelTheme.DIM);
        }

        // Pin star: only on hover (pinned servers already sit under "Pinned").
        int starX = x1 - 9;
        int starY = y + ROW_H - 14;
        boolean overStar = inside(mx, my, starX - 2, starY - 2, 12, 12);
        if (hover && !dragging) {
            gfx.text(font, server.pinned ? "★" : "☆", starX, starY,
                    server.pinned ? MycelTheme.WARN : overStar ? MycelTheme.TEXT : MycelTheme.FAINT);
        }

        if (dragging && pressed == server) {
            gfx.fill(x0 - 2, y, x1 + 2, bottom, 0xA0171717);
        }
        if (hover && !dragging) {
            boolean overIcon = inside(mx, my, ix, iy, ICON, ICON);
            gfx.fill(ix, iy, ix + ICON, iy + ICON, 0xA0909090);
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, overIcon ? JOIN_HIGHLIGHTED : JOIN, ix, iy, ICON, ICON);
            if (overStar) {
                gfx.setTooltipForNextFrame(font, Component.literal(server.pinned ? "Unpin" : "Pin to top"), mx, my);
            } else if (inside(mx, my, pingX, y + 3, 10, 8)) {
                gfx.setTooltipForNextFrame(font, pingTooltip(data), mx, my);
            } else if (players != null && inside(mx, my, playersX, y + 2, playersW, 10) && !data.playerList.isEmpty()) {
                gfx.setTooltipForNextFrame(font, Lists.transform(data.playerList, Component::getVisualOrderText), mx, my);
            } else if (inside(mx, my, tx, y + 2, font.width(name) + font.width(note), 10)) {
                gfx.setTooltipForNextFrame(font, Lists.transform(details(server, data), Component::getVisualOrderText),
                        mx, my);
            }
        }
    }

    private Identifier statusSprite(ServerData data, int index) {
        return switch (data.state()) {
            case INITIAL -> PING[0];
            case PINGING -> {
                int frame = (int) (Util.getMillis() / 100L + index * 2L & 7L);
                yield PINGING[frame > 4 ? 8 - frame : frame];
            }
            case INCOMPATIBLE -> INCOMPATIBLE;
            case UNREACHABLE -> UNREACHABLE;
            case SUCCESSFUL -> data.ping < 150 ? PING[4] : data.ping < 300 ? PING[3] : data.ping < 600 ? PING[2]
                    : data.ping < 1000 ? PING[1] : PING[0];
        };
    }

    private static Component pingTooltip(ServerData data) {
        return switch (data.state()) {
            case INITIAL, PINGING -> Component.translatable("multiplayer.status.pinging");
            case INCOMPATIBLE -> Component.translatable("multiplayer.status.incompatible");
            case UNREACHABLE -> Component.translatable("multiplayer.status.no_connection");
            case SUCCESSFUL -> Component.translatable("multiplayer.status.ping", data.ping);
        };
    }

    /** The name tooltip: address, category, note and play history. */
    private List<Component> details(SavedServer server, ServerData data) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal(server.name).withColor(MycelTheme.HEADING));
        if (!minecraft.options.hideServerAddress) {
            out.add(Component.literal(server.address).withColor(MycelTheme.LINK));
        }
        Category category = list().category(server.category);
        if (category != null) {
            out.add(Component.literal("Category: ").withColor(MycelTheme.DIM)
                    .append(Component.literal(category.name).withColor(0xFF000000 | category.color)));
        }
        if (!server.note.isEmpty()) {
            out.add(Component.literal("Note: ").withColor(MycelTheme.DIM)
                    .append(Component.literal(server.note).withColor(MycelTheme.VALUE)));
        }
        String played = server.lastJoined <= 0 ? "Never played"
                : "Played " + TimeText.ago(System.currentTimeMillis(), server.lastJoined) + " · " + server.joins
                        + (server.joins == 1 ? " join" : " joins");
        out.add(Component.literal(played).withColor(MycelTheme.FAINT));
        if (data.state() == ServerData.State.SUCCESSFUL || data.state() == ServerData.State.INCOMPATIBLE) {
            out.add(Component.literal("Version: ").withColor(MycelTheme.FAINT).append(data.version));
        }
        return out;
    }

    private void renderLanHeader(GuiGraphicsExtractor gfx, int x0, int x1, int y, boolean hover) {
        if (hover) {
            gfx.fill(x0 - 2, y + 1, x1 + 2, y + HEADER_H - 1, HOVER);
        }
        int ty = y + 6;
        chevron(gfx, x0 + 1, ty + 1, library.lanCollapsed && query.isBlank());
        int x = x0 + 10;
        gfx.text(font, "LAN worlds", x, ty, MycelTheme.LINK);
        x += font.width("LAN worlds") + 6;
        if (lanServers.isEmpty()) {
            String scanning = "scanning" + ".".repeat((int) (Util.getMillis() / 400 % 4));
            gfx.text(font, scanning, x, ty, MycelTheme.FAINT);
            x += font.width("scanning...") + 6;
        } else {
            x = countPill(gfx, String.valueOf(lanServers.size()), x, ty) + 6;
        }
        if (x < x1) {
            gfx.fill(x, ty + 4, x1, ty + 5, HAIRLINE);
        }
    }

    private void renderLan(GuiGraphicsExtractor gfx, LanServer lan, int x0, int x1, int y, boolean hover, int mx,
            int my) {
        boolean picked = lan == selectedLan;
        int bottom = y + ROW_H - 2;
        if (picked) {
            gfx.fill(x0 - 2, y, x1 + 2, bottom, MycelTheme.SURFACE);
            gfx.fill(x0 - 2, y, x0, bottom, MycelTheme.ACCENT);
        } else if (hover) {
            gfx.fill(x0 - 2, y, x1 + 2, bottom, HOVER);
        }
        int ix = x0 + 4;
        int tx = ix + ICON + 6;
        gfx.fill(ix, y + 2, ix + ICON, y + 2 + ICON, MycelTheme.SURFACE);
        gfx.centeredText(font, "LAN", ix + ICON / 2, y + 14, MycelTheme.LINK);
        gfx.text(font, Component.translatable("lanServer.title"), tx, y + 3, MycelTheme.HEADING);
        gfx.text(font, MycelTheme.fit(font, lan.getMotd(), x1 - tx), tx, y + 14, MycelTheme.DIM);
        String address = minecraft.options.hideServerAddress ? I18n.get("selectServer.hiddenAddress") : lan.getAddress();
        gfx.text(font, MycelTheme.fit(font, address, x1 - tx), tx, y + 23, MycelTheme.FAINT);
        if (hover && !dragging) {
            boolean overIcon = inside(mx, my, ix, y + 2, ICON, ICON);
            gfx.fill(ix, y + 2, ix + ICON, y + 2 + ICON, 0xA0909090);
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, overIcon ? JOIN_HIGHLIGHTED : JOIN, ix, y + 2, ICON, ICON);
        }
    }

    private void renderDrag(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        Drop drop = dropAt(mouseX, mouseY);
        gfx.nextStratum();
        if (drop != null && drop.list() == null) {
            if (drop.header() != null) {
                gfx.outline(listX0() - 2, drop.lineY(), listX1() - listX0() + 4, HEADER_H, MycelTheme.ACCENT);
            } else {
                int y = drop.after() ? drop.lineY() + ROW_H - 2 : drop.lineY() - 2;
                gfx.fill(listX0() - 2, y, listX1() + 2, y + 2, MycelTheme.ACCENT);
            }
        }
        String label = (hasCopyModifier() && drop != null && drop.list() != null ? "Copy " : "") + pressed.name;
        int w = Math.min(160, font.width(label) + 12);
        int x = mouseX + 8;
        int y = mouseY + 4;
        MycelTheme.panel(gfx, x, y, x + w, y + 16);
        gfx.text(font, MycelTheme.fit(font, label, w - 10), x + 5, y + 4, MycelTheme.HEADING);
    }

    private void autoScroll(int mouseY) {
        if (mouseY < LIST_TOP + 12 && mouseY > LIST_TOP - 20) {
            scroll -= 4;
        } else if (mouseY > listBottom() - 12 && mouseY < listBottom() + 20) {
            scroll += 4;
        }
        clampScroll();
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx, int x, int top, int height, int value, int max) {
        if (max <= 0) {
            return;
        }
        int thumb = Math.max(8, height * height / (height + max));
        int y = top + (int) ((long) (height - thumb) * value / max);
        gfx.fill(x, top, x + 2, top + height, MycelTheme.SURFACE);
        gfx.fill(x, y, x + 2, y + thumb, MycelTheme.OFF);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean hasCopyModifier() {
        return Keys.isDown(InputConstants.KEY_LCONTROL)
                || Keys.isDown(InputConstants.KEY_RCONTROL);
    }

    // ================================================================= input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (menu != null && menu.isOpen()) {
            menu.click(mx, my);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        // 26.x mouse input is SDL-based: left is 1 there, not GLFW's 0.
        boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        boolean right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        if (!left && !right) {
            return false;
        }
        if (search.isFocused()) {
            search.setFocused(false);
            setFocused(null);
        }

        int side = sideRowAt(mx, my);
        if (side >= 0) {
            if (side == library.lists.size()) {
                newList();
                return true;
            }
            SavedList target = library.lists.get(side);
            if (right) {
                openListMenu(target, (int) mx, (int) my);
            } else if (doubleClick && target == list()) {
                prompt("Rename list", "Name", target.name, 32, false, name -> {
                    target.name = name;
                    save();
                });
            } else {
                switchList(target);
            }
            return true;
        }

        for (Chip chip : chips()) {
            if (left && inside(mx, my, chip.x(), TOOL_Y, chip.w(), TOOL_H)) {
                clickChip(chips().indexOf(chip));
                return true;
            }
        }

        int index = lineAt(mx, my);
        if (index < 0) {
            if (inside(mx, my, listX0() - 2, LIST_TOP, listX1() - listX0() + 4, viewHeight())) {
                if (right) {
                    openBlankMenu((int) mx, (int) my);
                } else {
                    selectedId = null;
                    selectedLan = null;
                    updateButtons();
                }
                return true;
            }
            return false;
        }
        int y = lineY(index);
        int iconX = listX0() + 4;
        switch (lines.get(index)) {
            case ServerLine line -> {
                SavedServer server = line.server();
                selectedId = server.id;
                selectedLan = null;
                updateButtons();
                if (right) {
                    openServerMenu(server, (int) mx, (int) my);
                } else if (inside(mx, my, listX1() - 11, y + ROW_H - 16, 12, 12)) {
                    togglePin(server);
                } else if (inside(mx, my, iconX, y + 2, ICON, ICON) || doubleClick) {
                    join(server);
                } else {
                    pressed = server;
                    pressX = mx;
                    pressY = my;
                }
            }
            case HeaderLine line -> {
                if (right) {
                    openHeaderMenu(line.header(), (int) mx, (int) my);
                } else {
                    toggleCollapsed(line.header());
                }
            }
            case LanHeaderLine line -> {
                if (left) {
                    library.lanCollapsed = !library.lanCollapsed;
                    save();
                }
            }
            case LanLine line -> {
                selectedLan = line.server();
                selectedId = null;
                updateButtons();
                if (left && (inside(mx, my, iconX, y + 2, ICON, ICON) || doubleClick)) {
                    joinLan(line.server());
                }
            }
        }
        return true;
    }

    private void clickChip(int index) {
        switch (index) {
            case 0 -> newCategory(null);
            case 1 -> {
                library.hideOffline = !library.hideOffline;
                scroll = 0;
                save();
            }
            default -> {
                library.sort = library.sort.next();
                save();
                flash("Sorted by " + library.sort.label.toLowerCase(Locale.ROOT), MycelTheme.DIM);
            }
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (pressed != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (!dragging && Math.hypot(event.x() - pressX, event.y() - pressY) > DRAG_START) {
                dragging = true;
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            Drop drop = dropAt(event.x(), event.y());
            if (drop != null) {
                applyDrop(drop, event.hasControlDown());
            }
            dragging = false;
            pressed = null;
            return true;
        }
        pressed = null;
        return super.mouseReleased(event);
    }

    /** Whether reordering by drag makes sense: manual order, nothing hidden by a filter. */
    private boolean manualOrder() {
        return library.sort == Sort.MANUAL && query.isBlank() && !library.hideOffline;
    }

    private Drop dropAt(double mx, double my) {
        if (pressed == null) {
            return null;
        }
        int side = sideRowAt(mx, my);
        if (side >= 0 && side < library.lists.size() && library.lists.get(side) != list()) {
            return new Drop(null, false, null, library.lists.get(side), 0);
        }
        int index = lineAt(mx, my);
        if (index < 0) {
            return null;
        }
        int y = lineY(index);
        return switch (lines.get(index)) {
            case HeaderLine line -> new Drop(null, false, line.header(), null, y);
            case ServerLine line -> line.server() == pressed ? null
                    : new Drop(line.server(), my >= y + ROW_H / 2.0, null, null, y);
            default -> null;
        };
    }

    private void applyDrop(Drop drop, boolean copy) {
        if (drop.list() != null) {
            sendToList(pressed, drop.list(), copy);
            return;
        }
        if (drop.header() != null) {
            list().add(pressed, drop.header().section());
        } else if (manualOrder()) {
            list().moveNextTo(pressed, drop.target(), drop.after());
        } else {
            // Sorted or filtered: only the section changes.
            list().add(pressed, list().sectionOf(drop.target()));
            flash("Sorted by " + library.sort.label.toLowerCase(Locale.ROOT) + ": moved to the section only",
                    MycelTheme.DIM);
        }
        selectedId = pressed.id;
        save();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (menu != null && menu.isOpen()) {
            return true;
        }
        if (x < sideW()) {
            sideScroll -= (int) Math.signum(scrollY);
            clampSideScroll();
        } else {
            scroll -= scrollY * SCROLL_STEP;
            clampScroll();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (menu != null && menu.isOpen()) {
            menu.close();
            return true;
        }
        if (dragging) {
            if (event.isEscape()) {
                dragging = false;
                pressed = null;
            }
            return true;
        }
        int key = event.key();
        if (event.hasControlDown() && key == InputConstants.KEY_F) {
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (key == InputConstants.KEY_F5) {
            refresh();
            return true;
        }
        if (search.isFocused()) {
            if (event.isEscape() && !query.isEmpty()) {
                search.setValue("");
                return true;
            }
            if (event.isDown()) {
                search.setFocused(false);
                setFocused(null);
                step(1);
                return true;
            }
            if (event.isConfirmation()) {
                if (selected() == null && selectedLan == null) {
                    step(1);
                }
                joinSelected();
                return true;
            }
            return super.keyPressed(event);
        }
        if (event.isUp() || event.isDown()) {
            int delta = event.isUp() ? -1 : 1;
            SavedServer server = selected();
            if (event.hasShiftDown() && server != null) {
                if (manualOrder() && list().nudge(server, delta)) {
                    save();
                    rebuild();
                    reveal(server);
                }
                return true;
            }
            step(delta);
            return true;
        }
        if (event.isConfirmation()) {
            joinSelected();
            return true;
        }
        // Typing starts a search. Characters only arrive while a text box has
        // focus, so the first one comes from the key itself.
        int code = Keys.typedKey(event);
        if (!event.hasControlDown() && !event.hasAltDown() && code > ' ' && code < 127) {
            setFocused(search);
            search.setFocused(true);
            char typed = (char) code;
            search.insertText(String.valueOf(event.hasShiftDown() ? Character.toUpperCase(typed) : typed));
            return true;
        }
        SavedServer server = selected();
        if (server != null && key == InputConstants.KEY_DELETE) {
            deleteServer(server);
            return true;
        }
        if (server != null && key == InputConstants.KEY_F2) {
            editServer(server);
            return true;
        }
        if (server != null && event.hasControlDown() && key == InputConstants.KEY_C) {
            minecraft.keyboardHandler.setClipboard(server.address);
            flash("Copied " + server.address, MycelTheme.GOOD);
            return true;
        }
        return super.keyPressed(event);
    }

    /** Moves the selection to the next or previous server or LAN world. */
    private void step(int delta) {
        List<Integer> selectable = new ArrayList<>();
        int current = -1;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (line instanceof ServerLine server) {
                if (server.server().id.equals(selectedId)) {
                    current = selectable.size();
                }
                selectable.add(i);
            } else if (line instanceof LanLine lan) {
                if (lan.server() == selectedLan) {
                    current = selectable.size();
                }
                selectable.add(i);
            }
        }
        if (selectable.isEmpty()) {
            return;
        }
        int next = current < 0 ? (delta > 0 ? 0 : selectable.size() - 1)
                : Math.max(0, Math.min(selectable.size() - 1, current + delta));
        int index = selectable.get(next);
        if (lines.get(index) instanceof ServerLine server) {
            selectedId = server.server().id;
            selectedLan = null;
        } else if (lines.get(index) instanceof LanLine lan) {
            selectedLan = lan.server();
            selectedId = null;
        }
        revealLine(index);
        updateButtons();
    }

    private void reveal(SavedServer server) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i) instanceof ServerLine line && line.server() == server) {
                revealLine(i);
                return;
            }
        }
    }

    private void revealLine(int index) {
        int top = tops[index];
        int bottom = top + heightOf(lines.get(index));
        if (top < scroll) {
            scroll = top;
        } else if (bottom > scroll + viewHeight()) {
            scroll = bottom - viewHeight();
        }
        clampScroll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
