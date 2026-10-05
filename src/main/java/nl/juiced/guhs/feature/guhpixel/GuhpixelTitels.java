package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.titels.Titels;

/**
 * The titles of the guhpixel slices. Titels.ALLE ends with this list (one shared line there). A slice adds its titles
 * ONLY between its own two markers, one {@code ALLE.add(new Titels.Titel("n_id", "gui.guhs.titels.naam.n_id", colour,
 * "guhs:icon_item", player -> ...));} per line, and writes gui.guhs.titels.naam.&lt;id&gt; and .hint.&lt;id&gt;. The predicate
 * reads saved progress; there is no "has title" flag. Fixed ids: among_sus ("Sus"), among_onterecht ("Onterecht weggestemd").
 */
public final class GuhpixelTitels {
    public static final List<Titels.Titel> ALLE = new ArrayList<>();

    static {
        // <px_lobby>
        ALLE.add(new Titels.Titel("lobby_knabbelspeurder", "gui.guhs.titels.naam.lobby_knabbelspeurder", net.minecraft.ChatFormatting.GOLD, "guhs:gouden_kaasknabbel", nl.juiced.guhs.feature.guhpixel.lobby.Knabbels::alleGevonden));
        ALLE.add(new Titels.Titel("lobby_dakhaas", "gui.guhs.titels.naam.lobby_dakhaas", net.minecraft.ChatFormatting.AQUA, "minecraft:feather", nl.juiced.guhs.feature.guhpixel.lobby.LobbyParkour::gehaald));
        ALLE.add(new Titels.Titel("lobby_mvg", "gui.guhs.titels.naam.lobby_mvg", net.minecraft.ChatFormatting.LIGHT_PURPLE, "guhs:guhpixel_netwerkkabeltje", nl.juiced.guhs.feature.guhpixel.lobby.LobbySlice::isMvgPlusPlus));
        // </px_lobby>
        // <px_grap1>
        // </px_grap1>
        // <px_grap2>
        // </px_grap2>
        // <px_among>
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
