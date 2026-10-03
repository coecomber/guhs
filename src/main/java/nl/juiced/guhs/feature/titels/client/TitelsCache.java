package nl.juiced.guhs.feature.titels.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.titels.TitelsPayloads;

/**
 * Client side of the titles: the cache of the Guhdex tab "Titels" (from {@code guhs:titels}), and the titles of the other
 * players (from {@code guhs:titels_actief}: the names above their heads are refreshed when it arrives).
 */
public final class TitelsCache {
    private static List<String> behaald = List.of();
    private static String actief = "";
    private static boolean geladen;

    /** The ids of the titles this player has earned. */
    public static List<String> behaald() {
        return behaald;
    }

    /** The id of the title that shows ("" = none). */
    public static String actief() {
        return actief;
    }

    /** False until the server answered once. */
    public static boolean geladen() {
        return geladen;
    }

    /** New data (also used by AutoCheck). */
    public static void zet(List<String> nieuwBehaald, String nieuwActief) {
        behaald = List.copyOf(nieuwBehaald);
        actief = nieuwActief;
        geladen = true;   // (the tab reads the cache while it draws: nothing to rebuild)
    }

    /** Asks the server for fresh data. */
    public static void vraag() {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new TitelsPayloads.Vraag());
        }
    }

    /** Picks a title ({@link Titels#GEEN}: none). Shown at once; the server's answer follows. */
    public static void kies(String id) {
        actief = Titels.GEEN.equals(id) ? "" : id;
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new TitelsPayloads.Kies(id));
        }
    }

    private static void opClient(Runnable r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            r.run();
        } else {
            mc.execute(r);
        }
    }

    public static void init() {
        TitelsPayloads.ontvanger = p -> opClient(() -> zet(p.behaald(), p.actief()));
        TitelsPayloads.actiefOntvanger = p -> opClient(() -> {
            Titels.zetClientActief(p.titels());
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                for (Player speler : mc.level.players()) {
                    speler.refreshDisplayName();   // (the name above the head: PlayerEvent.NameFormat, TitelsEvents)
                }
            }
        });
    }

    private TitelsCache() {
    }
}
