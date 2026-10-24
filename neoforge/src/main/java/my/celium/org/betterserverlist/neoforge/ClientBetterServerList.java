package my.celium.org.betterserverlist.neoforge;

import my.celium.org.betterserverlist.client.ScreenSwap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBetterServerList {
    private ClientBetterServerList() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientBetterServerList::onOpening);
        NeoForge.EVENT_BUS.addListener(ClientBetterServerList::onScreenInit);
    }

    private static void onOpening(ScreenEvent.Opening event) {
        Screen replacement = ScreenSwap.replacement(event.getNewScreen());
        if (replacement != null) {
            event.setNewScreen(replacement);
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Button button = ScreenSwap.backButton(event.getScreen());
        if (button != null) {
            event.addListener(button);
        }
    }
}
