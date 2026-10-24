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
