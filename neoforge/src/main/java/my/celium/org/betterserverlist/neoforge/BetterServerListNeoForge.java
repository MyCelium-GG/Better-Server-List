package my.celium.org.betterserverlist.neoforge;

import my.celium.org.betterserverlist.BetterServerListMod;
import my.celium.org.client.ClientHooks;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge entrypoint: server-safe bootstrap; client hooks (config screen,
 * screen swap) are isolated in {@link ClientBetterServerList} so dedicated
 * servers never load client classes.
 */
@Mod(BetterServerListMod.ID)
public final class BetterServerListNeoForge {
    public BetterServerListNeoForge(@SuppressWarnings("unused") IEventBus modBus,
            @SuppressWarnings("unused") ModContainer container) {
        BetterServerListMod.init();
        if (FMLEnvironment.getDist().isClient()) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (ModContainer ignored, Screen parent) -> ClientHooks.createConfigScreen(parent,
                            BetterServerListMod.ID));
            ClientBetterServerList.register();
        }
    }
}
