package my.celium.org.betterserverlist.client;

import java.lang.reflect.Field;

import my.celium.org.betterserverlist.BetterServerListMod;
import my.celium.org.client.MycelScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

/**
 * Decides when the vanilla multiplayer screen is swapped for
 * {@link ServerListScreen}. Each loader calls {@link #replacement} as a
 * screen opens (Fabric: right after it initialises) and
 * {@link #backButton} to put a "Better list" button on a vanilla screen the
 * player chose on purpose. Picking "Vanilla list" keeps vanilla until the
 * title screen shows again. Client only.
 */
public final class ScreenSwap {
    private static boolean vanillaChosen;

    private ScreenSwap() {
    }

    /** The screen to show instead of {@code screen}, or {@code null} to keep it. */
    public static Screen replacement(Screen screen) {
        try {
            if (screen instanceof TitleScreen) {
                vanillaChosen = false;
                return null;
            }
            if (screen == null || screen.getClass() != JoinMultiplayerScreen.class || vanillaChosen
                    || !BetterServerListMod.isModEnabled() || !BetterServerListMod.replaceScreen.get()) {
                return null;
            }
            BetterServerListMod.debug("Replacing the vanilla multiplayer screen");
            return new ServerListScreen(lastScreenOf(screen));
        } catch (RuntimeException e) {
            BetterServerListMod.warn("Could not open Better Server List, keeping vanilla: {}", e.toString());
            return null;
        }
    }

    /** Opens the vanilla screen and keeps it until the title screen. */
    static void openVanilla(Screen parent) {
        vanillaChosen = true;
        MycelScreens.open(new JoinMultiplayerScreen(parent));
    }

    /** A small "Better list" button for the top-right of a vanilla multiplayer screen, or {@code null}. */
    public static Button backButton(Screen screen) {
        if (screen == null || screen.getClass() != JoinMultiplayerScreen.class || !BetterServerListMod.isModEnabled()) {
            return null;
        }
        Button button = Button.builder(Component.literal("Better list"), b -> {
            vanillaChosen = false;
            MycelScreens.open(new ServerListScreen(lastScreenOf(screen)));
        }).bounds(screen.width - 84, 6, 78, 20).build();
        button.setTooltip(Tooltip.create(Component.literal("Back to Better Server List")));
        return button;
    }

    /** Where the vanilla screen's Back button leads; the title screen if that can't be read. */
    static Screen lastScreenOf(Screen screen) {
        try {
            Field field = JoinMultiplayerScreen.class.getDeclaredField("lastScreen");
            field.setAccessible(true);
            Object value = field.get(screen);
            if (value instanceof Screen last) {
                return last;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            BetterServerListMod.debug("lastScreen not readable: {}", e.toString());
        }
        return new TitleScreen();
    }
}
