package my.celium.org.betterserverlist.client;

import java.util.function.Consumer;

import my.celium.org.client.MycelTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Asks for one line of text (a name or a note), then returns to the parent. Client only. */
final class TextPromptScreen extends Screen {
    private static final int BOX_W = 240;

    private final Screen parent;
    private final String label;
    private final String initial;
    private final int maxLength;
    private final boolean allowEmpty;
    private final Consumer<String> onDone;
    private EditBox box;
    private FlatButton done;

    TextPromptScreen(Screen parent, String title, String label, String initial, int maxLength, boolean allowEmpty,
            Consumer<String> onDone) {
        super(Component.literal(title));
        this.parent = parent;
        this.label = label;
        this.initial = initial == null ? "" : initial;
        this.maxLength = maxLength;
        this.allowEmpty = allowEmpty;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        int w = Math.min(BOX_W, width - 32);
        int x = (width - w) / 2;
        int y = height / 2 - 20;
        String value = box == null ? initial : box.getValue();
        // Borderless text inside the frame drawn in extractRenderState.
        box = new EditBox(font, x + 6, y + 6, w - 12, 9, Component.literal(label));
        box.setBordered(false);
        box.setTextColor(MycelTheme.TEXT);
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(text -> updateDone());
        addRenderableWidget(box);
        int bw = (w - 4) / 2;
        done = addRenderableWidget(new FlatButton(x, y + 28, bw, CommonComponents.GUI_DONE, true, this::finish));
        addRenderableWidget(new FlatButton(x + bw + 4, y + 28, bw, CommonComponents.GUI_CANCEL, false, this::onClose));
        updateDone();
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(box);
    }

    private void updateDone() {
        if (done != null) {
            done.active = allowEmpty || !box.getValue().isBlank();
        }
    }

    private void finish() {
        if (!done.active) {
            return;
        }
        onDone.accept(box.getValue().strip());
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() && box.isFocused()) {
            finish();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float a) {
        gfx.fill(0, 0, width, height, MycelTheme.SHADE);
        int w = Math.min(BOX_W, width - 32);
        int x = (width - w) / 2;
        int y = height / 2 - 20;
        MycelTheme.panel(gfx, x - 10, y - 34, x + w + 10, y + 56);
        gfx.text(font, title, x, y - 26, MycelTheme.HEADING);
        gfx.text(font, label, x, y - 13, MycelTheme.DIM);
        boolean focused = box != null && box.isFocused();
        gfx.fill(x, y, x + w, y + 20, focused ? MycelTheme.ACCENT : MycelTheme.BORDER);
        gfx.fill(x + 1, y + 1, x + w - 1, y + 19, 0xFF1B1B1B);
        super.extractRenderState(gfx, mouseX, mouseY, a);
    }
}
