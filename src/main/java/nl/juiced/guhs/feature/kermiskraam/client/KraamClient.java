package nl.juiced.guhs.feature.kermiskraam.client;

import net.minecraft.client.Minecraft;
import nl.juiced.guhs.feature.kermiskraam.Kraam;
import nl.juiced.guhs.feature.kermiskraam.KraamPayloads;

/** The client side of the game stalls: opens the game screen. */
public final class KraamClient {
    public static void init() {
        KraamPayloads.opener = p -> {
            Minecraft mc = Minecraft.getInstance();
            Kraam kraam = Kraam.van(p.kraam());
            if (kraam != null) {
                mc.execute(() -> mc.setScreen(new KraamSpelScherm(kraam, p.seed())));
            }
        };
    }

    private KraamClient() {
    }
}
