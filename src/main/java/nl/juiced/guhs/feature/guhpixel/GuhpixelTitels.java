package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.titels.Titels;

/**
 * The titles of the guhpixel slices. Titels.ALLE ends with this list (one shared line there). A slice adds its titles
 * ONLY between its own two markers, one {@code ALLE.add(new Titels.Titel("n_id", "gui.guhs.titels.naam.n_id", colour,
 * "guhs:icon_item", player -> ...));} per line, and writes gui.guhs.titels.naam.&lt;id&gt; and .hint.&lt;id&gt;. The predicate
 * reads saved progress; there is no "has title" flag. Fixed id: among_sus ("Sus").
 * (1.3.1) Six titles of 1.3.0 are gone again: lobby_dakhaas, lobby_mvg, among_onterecht, among_kussenkampioen, among_speurguh
 * and among_taakjesguh. Who had one of them chosen shows no title (Titels.actief). The rank [MVG++] itself stays.
 */
public final class GuhpixelTitels {
    public static final List<Titels.Titel> ALLE = new ArrayList<>();

    static {
        // <px_lobby>
        ALLE.add(new Titels.Titel("lobby_knabbelspeurder", "gui.guhs.titels.naam.lobby_knabbelspeurder", net.minecraft.ChatFormatting.GOLD, "guhs:gouden_kaasknabbel", nl.juiced.guhs.feature.guhpixel.lobby.Knabbels::alleGevonden));
        // </px_lobby>
        // <px_grap1>
        // </px_grap1>
        // <px_grap2>
        // </px_grap2>
        // <px_among>
        ALLE.add(new Titels.Titel("among_sus", "gui.guhs.titels.naam.among_sus", net.minecraft.ChatFormatting.RED, "guhs:among_noodknop", p -> nl.juiced.guhs.feature.guhpixel.among.AmongBeloning.cijfer(p, nl.juiced.guhs.feature.guhpixel.among.AmongBeloning.WEGGESTEMD) >= 3));
        // </px_among>
        // <px_guhkade>
        // </px_guhkade>
        // <px_kantoor>
        // </px_kantoor>
        // <px_bioscoop>
        // </px_bioscoop>
        // <px_reisbureau>
        // </px_reisbureau>
        // <px_parkour>
        // </px_parkour>
    }

    private GuhpixelTitels() {
    }
}
