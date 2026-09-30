package nl.juiced.guhs.feature.gids.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.client.screen.GuhDexScreen;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.VerhalenPayloads;

/** Client cache of the Guhdex tab "Verhalen" (from {@code guhs:gids_verhalen}). */
public final class VerhalenCache {
    private static List<VerhaalStand> verhalen = List.of();
    private static int versie;

    public static List<VerhaalStand> verhalen() {
        return verhalen;
    }

    public static int versie() {
        return versie;
    }

    @Nullable
    public static VerhaalStand van(String id) {
        for (VerhaalStand v : verhalen) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return null;
    }

    /** New data (also used by AutoCheck); redraws the Guhdex when it shows the tab. */
    public static void zet(List<VerhaalStand> nieuw) {
        verhalen = List.copyOf(nieuw);
        versie++;
        if (Minecraft.getInstance().screen instanceof GuhDexScreen dex) {
            dex.verhalenVernieuwd();
        }
    }

    /** Asks the server for fresh data. */
    public static void vraag() {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new VerhalenPayloads.Vraag());
        }
    }

    static void init() {
        VerhalenPayloads.ontvanger = p -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isSameThread()) {
                zet(p.verhalen());
            } else {
                mc.execute(() -> zet(p.verhalen()));
            }
        };
    }

    private VerhalenCache() {
    }
}
