package my.celium.org.betterserverlist.fabric;

import my.celium.org.betterserverlist.client.ScreenSwap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric has no "screen opening" event, so the vanilla multiplayer screen is
 * swapped right after it initialises; nothing else runs after that hook.
 */
public final class BetterServerListFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            Screen replacement = ScreenSwap.replacement(screen);
            if (replacement != null) {
                client.gui.setScreen(replacement);
                return;
            }
            Button button = ScreenSwap.backButton(screen);
            if (button != null) {
                Screens.getWidgets(screen).add(button);
            }
        });
    }
}
