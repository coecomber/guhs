package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.erfenis.koppeling.Dorpszender;

/**
 * (owner: koppeling; CONTRACT_OPKNAPPER) The vadskracht that arrives in the village of this owner: the sum of all their
 * Dorpszenders that run, each after the loss on the way (25%: 100 VK sent = 75 VK in the village). Kern caps it with the
 * Vadsmast's level. A Dorpszender whose chunk is unloaded counts as it was at the last look (the village keeps working while
 * you are in it, your base is then unloaded).
 */
public final class ErfenisVadskracht {
    private ErfenisVadskracht() {
    }

    public static int beschikbaar(MinecraftServer s, UUID eigenaar) {
        return Dorpszender.beschikbaar(s, eigenaar);
    }
}
