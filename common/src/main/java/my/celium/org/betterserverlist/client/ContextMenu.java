package my.celium.org.betterserverlist.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import my.celium.org.client.MycelTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The right-click menu: a wiki-style card at the mouse. An item either runs
 * an action or opens a submenu in place (with a "Back" row). Client only.
 */
final class ContextMenu {
    static final int ITEM_H = 13;
    static final int PAD = 3;

    record Item(String label, int color, Runnable action, Supplier<List<Item>> submenu, boolean enabled) {
        static Item of(String label, Runnable action) {
            return new Item(label, MycelTheme.TEXT, action, null, true);
        }

        static Item danger(String label, Runnable action) {
            return new Item(label, MycelTheme.BAD, action, null, true);
        }

        static Item sub(String label, Supplier<List<Item>> submenu) {
            return new Item(label, MycelTheme.TEXT, null, submenu, true);
        }

        static Item off(String label) {
            return new Item(label, MycelTheme.OFF, null, null, false);
        }

        /** A marked choice in a submenu, e.g. the current category. */
        static Item current(String label) {
            return new Item("✔ " + label, MycelTheme.GOOD, null, null, false);
        }
    }

    private final String title;
    private final int anchorX;
    private final int anchorY;
    private final List<List<Item>> stack = new ArrayList<>();
    private int x;
    private int y;
    private int w;
    private int h;
    private boolean open = true;

    ContextMenu(String title, int x, int y, List<Item> items) {
        this.title = title;
        this.anchorX = x;
        this.anchorY = y;
        stack.add(items);
    }

    boolean isOpen() {
        return open;
    }

    void close() {
        open = false;
    }

    private List<Item> items() {
        return stack.get(stack.size() - 1);
    }

    private int rows() {
        return items().size() + (stack.size() > 1 ? 1 : 0);
    }

    private void layout(Font font, int screenW, int screenH) {
        int widest = font.width(title);
        for (Item item : items()) {
            widest = Math.max(widest, font.width(item.label() + (item.submenu() != null ? "  ›" : "")));
        }
        w = widest + PAD * 2 + 8;
        h = (rows() + 1) * ITEM_H + PAD * 2;
        x = Math.max(2, Math.min(anchorX, screenW - w - 4));
        y = Math.max(2, Math.min(anchorY, screenH - h - 4));
    }

    void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, int screenW, int screenH) {
        layout(font, screenW, screenH);
        gfx.nextStratum();
        MycelTheme.panel(gfx, x, y, x + w, y + h);
        gfx.text(font, MycelTheme.fit(font, title, w - PAD * 2 - 4), x + PAD + 3, y + PAD + 3, MycelTheme.DIM);
        gfx.fill(x + 2, y + PAD + ITEM_H, x + w - 2, y + PAD + ITEM_H + 1, MycelTheme.BORDER);
        int row = 1;
        if (stack.size() > 1) {
            renderRow(gfx, font, row++, "‹ Back", MycelTheme.DIM, true, false, mouseX, mouseY);
        }
        for (Item item : items()) {
            renderRow(gfx, font, row++, item.label(), item.color(), item.enabled(), item.submenu() != null, mouseX, mouseY);
        }
    }

    private void renderRow(GuiGraphicsExtractor gfx, Font font, int row, String label, int color, boolean enabled,
            boolean arrow, int mouseX, int mouseY) {
        int top = y + PAD + row * ITEM_H;
        if (enabled && rowAt(mouseX, mouseY) == row) {
            gfx.fill(x + 2, top, x + w - 2, top + ITEM_H, MycelTheme.SURFACE);
            gfx.fill(x + 2, top, x + 4, top + ITEM_H, MycelTheme.ACCENT);
        }
        gfx.text(font, label, x + PAD + 4, top + 3, color);
        if (arrow) {
            gfx.text(font, "›", x + w - PAD - 6, top + 3, MycelTheme.FAINT);
        }
    }

    private int rowAt(double mx, double my) {
        if (mx < x + 2 || mx >= x + w - 2 || my < y + PAD + ITEM_H) {
            return -1;
        }
        int row = (int) ((my - y - PAD) / ITEM_H);
        return row >= 1 && row <= rows() ? row : -1;
    }

    boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Handles a click anywhere; a click outside closes the menu. Always consumes it. */
    void click(double mx, double my) {
        if (!contains(mx, my)) {
            open = false;
            return;
        }
        int row = rowAt(mx, my);
        if (row < 0) {
            return;
        }
        int index = row - 1;
        if (stack.size() > 1) {
            if (index == 0) {
                stack.remove(stack.size() - 1);
                return;
            }
            index--;
        }
        Item item = items().get(index);
        if (!item.enabled()) {
            return;
        }
        if (item.submenu() != null) {
            stack.add(item.submenu().get());
            return;
        }
        open = false;
        item.action().run();
    }
}
