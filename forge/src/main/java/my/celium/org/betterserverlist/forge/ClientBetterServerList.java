package my.celium.org.betterserverlist.forge;

import my.celium.org.betterserverlist.client.ScreenSwap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;

public final class ClientBetterServerList {
    private ClientBetterServerList() {
    }

    public static void register() {
        ScreenEvent.Opening.BUS.addListener(ClientBetterServerList::onOpening);
        ScreenEvent.Init.Post.BUS.addListener(ClientBetterServerList::onScreenInit);
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
