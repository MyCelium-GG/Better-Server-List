package my.celium.org.betterserverlist.fabric;

import my.celium.org.betterserverlist.BetterServerListMod;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint: server-safe bootstrap only. The screen swap lives in
 * {@link BetterServerListFabricClient} so dedicated servers never load
 * client classes.
 */
public final class BetterServerListFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BetterServerListMod.init();
    }
}
