package my.celium.org.betterserverlist.forge;

import my.celium.org.betterserverlist.BetterServerListMod;
import my.celium.org.client.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entrypoint: server-safe bootstrap; client hooks (config screen,
 * screen swap) are isolated in {@link ClientBetterServerList} so dedicated
 * servers never load client classes.
 */
@Mod(BetterServerListMod.ID)
public final class BetterServerListForge {
    public BetterServerListForge(@SuppressWarnings("unused") FMLJavaModLoadingContext context) {
        if (!net.minecraftforge.fml.ModList.isLoaded("mycel")) {
            my.celium.org.betterserverlist.MissingMycel.logOnce();
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                net.minecraftforge.event.TickEvent.ClientTickEvent.Post.BUS.addListener(
                        event -> my.celium.org.betterserverlist.MissingMycel.tick(net.minecraft.client.Minecraft.getInstance()));
            }
            return;
        }
        my.celium.org.betterserverlist.ModGate.open();
        BetterServerListMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (Minecraft minecraft, Screen parent) -> ClientHooks.createConfigScreen(parent,
                                    BetterServerListMod.ID)));
            ClientBetterServerList.register();
        }
    }
}
