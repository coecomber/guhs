package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.guhpad.GuhpadPayloads.Stand;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * Het Guhpad in the Superkompas (1.4.1): what the tab Verhalen lists, split by world and progress the way the Guhdex tab
 * Verhalen is. {@link GroteVerhalen} is the one source: its worlds in path order, each world's big stories in their order
 * with their places ({@code GrootVerhaal.plekken}), then the places of the world that belong to no big story. The locks are
 * the Guhdex's ({@link Stand#open}: a world the player may not go into yet is locked and says what is missing).
 * <ul>
 *   <li>a story with one place is one entry (the place); a story with more places (the Knabbelring) gets a heading with its
 *       name and an entry per place;</li>
 *   <li>the spoiler rule: a place of a story that Guhdalfs sluier still hides for the player is {@link Plek#geheim}: the
 *       screen shows "???" with a lock and it can not be chosen ({@link #magKiezen}, asked by the server too). It becomes a
 *       normal entry once the player's story reached it;</li>
 *   <li>what the fixed tab "verhalen" of {@link SuperkompasItem} has besides (the surf beach, the Knuffeldal town, whatever a
 *       later update adds with {@code voegToe("verhalen", ...)}) stays, under the Guhmensie;</li>
 *   <li>"Het echte Guheinde" is only its locked line: no places.</li>
 * </ul>
 * Plain data, both sides: the screen (client.screen.SuperkompasScreen) draws it, the game tests read it.
 */
public final class KompasVerhalen {
    /** The id of the Superkompas tab this is the list of. */
    public static final String TAB = "verhalen";

    /** A place: its structure id, the big story it belongs to (null: none), and whether it is still a secret for the player. */
    public record Plek(String structuur, @Nullable String verhaal, boolean geheim) {
    }

    /** Places that are listed together; kop = the id of the big story whose name is their heading (null: no heading). */
    public record Blok(@Nullable String kop, List<Plek> plekken) {
        public Blok {
            plekken = List.copyOf(plekken);
        }
    }

    /** A world of the Guhpad: locked or not, what is still missing for it, and its places. */
    public record Groep(Wereld wereld, boolean opSlot, List<GuhpadPayloads.Eis> mist, List<Blok> blokken) {
        public Groep {
            mist = List.copyOf(mist);
            blokken = List.copyOf(blokken);
        }

        /** Every place of this world, in order. */
        public List<Plek> plekken() {
            return blokken.stream().flatMap(b -> b.plekken().stream()).toList();
        }
    }

    /**
     * The list for a player: {@code stand} = where they are on the Guhpad ({@code Stand.LEEG}: not known yet, nothing shows
     * as locked), {@code verborgen} = is this structure behind Guhdalfs sluier for them.
     */
    public static List<Groep> groepen(Stand stand, Predicate<String> verborgen) {
        boolean bekend = !stand.eisen().isEmpty();
        Set<String> gehad = new HashSet<>();
        List<Groep> out = new ArrayList<>();
        for (Wereld w : Wereld.values()) {
            List<Blok> blokken = new ArrayList<>();
            List<Plek> los = new ArrayList<>();
            for (GrootVerhaal v : GroteVerhalen.van(w)) {
                List<Plek> eigen = new ArrayList<>();
                for (String s : v.plekken()) {
                    if (gehad.add(s)) {
                        eigen.add(new Plek(s, v.id(), verborgen.test(s)));
                    }
                }
                if (eigen.size() <= 1) {
                    los.addAll(eigen);
                    continue;
                }
                sluit(blokken, los);
                blokken.add(new Blok(v.id(), eigen));
            }
            List<String> rest = new ArrayList<>(GroteVerhalen.plekken(w));
            if (w == Wereld.GUHMENSIE) {
                rest.addAll(tabPlekken());
            }
            for (String s : rest) {
                // (a place of a story further on the path is listed there; what the sluier hides and is no story's place is not listed)
                if (!GroteVerhalen.isVerhaalPlek(s) && !verborgen.test(s) && gehad.add(s)) {
                    los.add(new Plek(s, null, false));
                }
            }
            sluit(blokken, los);
            out.add(new Groep(w, bekend && !stand.open(w), bekend ? stand.ontbreekt(w) : List.of(), blokken));
        }
        return out;
    }

    private static void sluit(List<Blok> blokken, List<Plek> los) {
        if (!los.isEmpty()) {
            blokken.add(new Blok(null, los));
            los.clear();
        }
    }

    /** The places of the fixed Superkompas tab "verhalen" (the older stories and what {@code voegToe} added). */
    private static List<String> tabPlekken() {
        for (SuperkompasItem.Category c : SuperkompasItem.CATEGORIES) {
            if (c.id().equals(TAB)) {
                return c.structures();
            }
        }
        return List.of();
    }

    /** (server; the game tests) the list for this player. */
    public static List<Groep> groepen(ServerPlayer p) {
        return groepen(GuhpadPayloads.stand(p), s -> Sluiers.isVerborgen(p, s));
    }

    /** Every structure the tab Verhalen can list, whoever looks (so the Superkompas may be set to it). */
    public static boolean isPlek(@Nullable String structuur) {
        return GroteVerhalen.isPlek(structuur);
    }

    /**
     * The spoiler rule on the server: may this player set their Superkompas to this structure? Not while Guhdalfs sluier
     * hides it for them (the "???" entries; a client that asks anyway is ignored).
     */
    public static boolean magKiezen(ServerPlayer p, String structuur) {
        return SuperkompasItem.DOEL.equals(structuur) || !Sluiers.isVerborgen(p, structuur);
    }

    private KompasVerhalen() {
    }
}
