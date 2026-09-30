package nl.juiced.guhs.feature.gids;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.quest.Highscores;

/**
 * What the Guhdex's Minigames and Kleding tabs show (2.9, the gids slice), kept apart from the screen so it is the same on
 * both sides and the game tests can check it:
 * <ul>
 *   <li>the Minigames tab: per {@link SpelGroepen.Groep} its Highscores rows ({@link #rijen}) with a short label
 *   ({@link #rijLabel});</li>
 *   <li>the Kleding tab: every piece (except the kapper's hairstyles, which are no unlocks) grouped per
 *   {@link KledingBronnen} source ({@link #kledingGroepen}), the sources in "kinds" ({@link Soort}: minigames, Knuffeldal,
 *   shops, places, treasure chests, jobs...).</li>
 * </ul>
 */
public final class GidsData {
    /** The group of pieces that have no source (yet): shown at the very end, only when it has pieces. */
    public static final String ZONDER_BRON = "zonder_bron";

    /** The kind of a clothing source: a small heading above its groups in the Kleding tab (gui.guhs.gids.soort.&lt;id&gt;). */
    public enum Soort {
        KLASSIEKERS, KNUFFELDAL_SPELLETJES, GROTE_GUHSPELEN, WINKELS, PLEKKEN, SCHATKISTEN, BEROEPEN,
        /** 3.0: the questlines and games of the Guhverhalen. */
        VERHALEN,
        OVERIG;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.gids.soort." + id());
        }
    }

    /** One source in the Kleding tab: its id (lang gui.guhs.kledingbron.&lt;id&gt;), kind and pieces (enum order). */
    public record KledingGroep(String bron, Soort soort, List<GuhClothes> stukken) {
        public KledingGroep {
            stukken = List.copyOf(stukken);
        }

        public Component naam() {
            return ZONDER_BRON.equals(bron) ? Component.translatable("gui.guhs.gids.zonder_bron") : Component.translatable("gui.guhs.kledingbron." + bron);
        }
    }

    /** The sources that sell or make things (not a minigame, not a place with a game). */
    private static final List<String> WINKELS = List.of("kleermaker", "crafting", "guhdex", "brococolief");
    /** 3.0: the questlines of the Guhverhalen (their outfits). */
    private static final List<String> VERHAAL_BRONNEN = List.of("timmerguh", "nomguh", "sledesprint", "mewtwo", "hemel", "guhwaii", "guhwaii_spellen");

    /** The kind of a source. */
    public static Soort soort(String bron) {
        SpelGroepen.Groep groep = SpelGroepen.van(bron);
        if (groep != null) {
            return switch (groep.tijdperk()) {
                case KLASSIEKERS -> Soort.KLASSIEKERS;
                case KNUFFELDAL -> Soort.KNUFFELDAL_SPELLETJES;
                case GROTE_GUHSPELEN -> Soort.GROTE_GUHSPELEN;
                case VERHALEN -> Soort.VERHALEN;
            };
        }
        if (VERHAAL_BRONNEN.contains(bron)) {
            return Soort.VERHALEN;
        }
        if (bron.startsWith("loot_")) {
            return Soort.SCHATKISTEN;
        }
        if (bron.startsWith("beroep_")) {
            return Soort.BEROEPEN;
        }
        if (WINKELS.contains(bron)) {
            return Soort.WINKELS;
        }
        if (ZONDER_BRON.equals(bron)) {
            return Soort.OVERIG;
        }
        return Soort.PLEKKEN;
    }

    /** Is this piece an unlock? Everything but the kapper's hairstyles (Slot.HAAR: "bij de kapper"). */
    public static boolean ontgrendelbaar(GuhClothes c) {
        return c.slot != GuhClothes.Slot.HAAR;
    }

    /** The kapper's hairstyles (shown as "bij de kapper", not counted as unlocks). */
    public static List<GuhClothes> kapsels() {
        List<GuhClothes> out = new ArrayList<>();
        for (GuhClothes c : GuhClothes.values()) {
            if (!ontgrendelbaar(c)) {
                out.add(c);
            }
        }
        return out;
    }

    /** Every piece that is an unlock. */
    public static List<GuhClothes> ontgrendelbare() {
        List<GuhClothes> out = new ArrayList<>();
        for (GuhClothes c : GuhClothes.values()) {
            if (ontgrendelbaar(c)) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * The groups of the Kleding tab: every unlockable piece exactly once, per source, sorted by kind and then by the
     * order of {@link KledingBronnen#bronnen()}; sources without pieces are left out, pieces without a source come last
     * ({@link #ZONDER_BRON}).
     */
    public static List<KledingGroep> kledingGroepen() {
        Map<String, List<GuhClothes>> per = new LinkedHashMap<>();
        for (String bron : KledingBronnen.bronnen()) {
            per.put(bron, new ArrayList<>());
        }
        per.put(ZONDER_BRON, new ArrayList<>());
        for (GuhClothes c : ontgrendelbare()) {
            String bron = KledingBronnen.bron(c);
            per.computeIfAbsent(bron == null ? ZONDER_BRON : bron, b -> new ArrayList<>()).add(c);
        }
        List<KledingGroep> out = new ArrayList<>();
        for (Soort soort : Soort.values()) {
            for (Map.Entry<String, List<GuhClothes>> e : per.entrySet()) {
                if (!e.getValue().isEmpty() && soort(e.getKey()) == soort) {
                    out.add(new KledingGroep(e.getKey(), soort, e.getValue()));
                }
            }
        }
        return out;
    }

    /** The Highscores rows of a group that really exist, in display order. */
    public static List<Highscores.Game> rijen(SpelGroepen.Groep groep) {
        List<Highscores.Game> out = new ArrayList<>();
        for (String id : groep.spellen()) {
            Highscores.Game game = Highscores.game(id);
            if (game != null) {
                out.add(game);
            }
        }
        return out;
    }

    /** The short label of a Highscores row inside its group (gui.guhs.gids.rij.&lt;id&gt;, e.g. "Lastig: snelste ronde"). */
    public static String rijLabel(String gameId) {
        return "gui.guhs.gids.rij." + gameId;
    }

    /** The group a structure belongs to (for the Superkompas: its icon and whether you've been there), or null. */
    @Nullable
    public static SpelGroepen.Groep groepVanStructuur(String structure) {
        for (SpelGroepen.Groep g : SpelGroepen.alle()) {
            if (structure.equals(g.structuur())) {
                return g;
            }
        }
        return null;
    }

    private GidsData() {
    }
}
