package nl.juiced.guhs.feature.bibliotheek.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.bibliotheek.BibliotheekPayloads;

/** Client side of the bibliotheek feature: the Bibliothecaris' screen and the lectern books (only loaded on the client). */
public final class BibliotheekClient {
    public static void init(IEventBus modBus) {
        // nothing to register: the blocks bring their own render type in their models
    }

    public static void open(BibliotheekPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new BibliotheekScreen(payload));
    }

    public static void openBook(BibliotheekPayloads.OpenBook payload) {
        Minecraft.getInstance().setScreen(new LeesboekScreen(payload));
    }

    private BibliotheekClient() {
    }
}
