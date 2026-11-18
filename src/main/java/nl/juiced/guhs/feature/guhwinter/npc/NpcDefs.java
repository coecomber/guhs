package nl.juiced.guhs.feature.guhwinter.npc;

import static nl.juiced.guhs.entity.GuhClothes.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;

/**
 * Guhwinter (npc slice): every guh of the Winterplein, its look, how it idles, where it stands and in which phases.
 * Names and idle lines: tools/features/guhwinter_npc.py (lang {@code guhwinter.npc.naam.<naam>},
 * {@code guhwinter.npc.praat.<naam>.<fase>.<n>}). The extra look pieces ({@link Def#look}: Sinterklaas' beard, mantle and
 * staff, the pietenpakjes, Anthony's camera, Liek's nose ring) are drawn by client/GuhwinterNpcClient with the textures
 * guh_clothes/guhwinter_&lt;look&gt;.png that the same python module paints.
 */
public final class NpcDefs {
    public enum Gedrag {
        /** Stands at its spot, looks around and at players, walks back when it drifted off. */
        STAAN,
        /** Sits at its spot (Sint on his chair, oma and opa at the fire). */
        ZITTEN,
        /** Strolls a few blocks around its spot. */
        SLENTER,
        /** Finn: skates big rounds around the middle of the pond. */
        SCHAATSEN,
        /** Daan: walks slow rounds around the pond, pushing Liv on her sled in front of him. */
        DUWEN,
        /** Liv: sits on her sled, right in front of Daan (or at her spot when he isn't there). */
        SLEE
    }

    public enum Soort {
        GUH,
        /** Ozovads: a (vanilla) white horse with a guh twist. */
        PAARD
    }

    /** Where to stand: an anchor of ankers.json, a few blocks off it. */
    public record Ref(String anker, double dx, double dz) {
    }

    /**
     * One NPC. {@code kapsel}+{@code haarkleur}: hair (the HAAR slot, tinted; -1 = its own colour). {@code altijd}: clothes
     * worn in every phase; {@code perFase}: worn on top in that phase (same slot: the phase piece wins). {@code ankers}: the
     * anchors to try in order (the first one is always {@code npc_<naam>}); {@code x}/{@code z}: the fallback spot inside the
     * plaza when none exists yet. {@code fasen}: the phases it is on the plein.
     */
    public record Def(String naam, Soort soort, GuhVariant variant, float maat, Gedrag gedrag, @Nullable GuhClothes kapsel, int haarkleur,
                      List<GuhClothes> altijd, Map<NpcFase, List<GuhClothes>> perFase, @Nullable String look, List<Ref> ankers,
                      double x, double z, float yaw, Set<NpcFase> fasen) {
        public String naamKey() {
            return "guhwinter.npc.naam." + naam;
        }

        /** The clothes it wears in this phase (phase pieces replace the everyday piece of the same slot). */
        public List<GuhClothes> kleren(NpcFase fase) {
            Map<GuhClothes.Slot, GuhClothes> per = new EnumMap<>(GuhClothes.Slot.class);
            for (GuhClothes c : altijd) {
                per.put(c.slot, c);
            }
            for (GuhClothes c : perFase.getOrDefault(fase, List.of())) {
                per.put(c.slot, c);
            }
            if (kapsel != null) {
                per.put(GuhClothes.Slot.HAAR, kapsel);
            }
            return new ArrayList<>(per.values());
        }

        public boolean in(NpcFase fase) {
            return fasen.contains(fase);
        }
    }

    /** Sint, pakjesguhs, Ozovads. */
    public static final Set<NpcFase> SINT = EnumSet.of(NpcFase.SINT);
    /** Everyone else (Wijnand: from KERST on). */
    public static final Set<NpcFase> HEEL = EnumSet.of(NpcFase.SINT, NpcFase.KERST, NpcFase.OUDJAAR);
    public static final Set<NpcFase> VANAF_KERST = EnumSet.of(NpcFase.KERST, NpcFase.OUDJAAR);

    /** How many pakjesguhs (pakjesguh_0 .. pakjesguh_{n-1}). */
    public static final int PAKJESGUHS = 4;

    private static final Map<String, Def> ALLE = new LinkedHashMap<>();

    // hair colours
    private static final int BRUIN = 0x6E4B2A, DONKERBRUIN = 0x3E2A1E, LICHTBRUIN = 0xB98F5E, BLOND = 0xF3D98B, ROODBRUIN = 0xB5683A,
            GRIJS = 0xB4B0B4, WIT = 0xF2F0F0, ZANDBLOND = 0xD9B26F, KASTANJE = 0x8A4F2E;

    // ---- a small builder, so the table below stays readable --------------------------------------------------------------
    private static final class B {
        final String naam;
        Soort soort = Soort.GUH;
        GuhVariant variant = GuhVariant.NORMAL;
        float maat = 1f;
        Gedrag gedrag = Gedrag.STAAN;
        GuhClothes kapsel;
        int haar = -1;
        final List<GuhClothes> altijd = new ArrayList<>();
        final Map<NpcFase, List<GuhClothes>> perFase = new EnumMap<>(NpcFase.class);
        String look;
        final List<Ref> ankers = new ArrayList<>();
        double x, z;
        float yaw;
        Set<NpcFase> fasen = HEEL;

        B(String naam, double x, double z, float yaw) {
            this.naam = naam;
            this.x = x;
            this.z = z;
            this.yaw = yaw;
            ankers.add(new Ref("npc_" + naam, 0, 0));
        }

        B variant(GuhVariant v) {
            variant = v;
            return this;
        }

        B maat(float m) {
            maat = m;
            return this;
        }

        B doet(Gedrag g) {
            gedrag = g;
            return this;
        }

        B haar(GuhClothes k, int kleur) {
            kapsel = k;
            haar = kleur;
            return this;
        }

        B draagt(GuhClothes... c) {
            altijd.addAll(List.of(c));
            return this;
        }

        B in(NpcFase f, GuhClothes... c) {
            perFase.put(f, List.of(c));
            return this;
        }

        B look(String l) {
            look = l;
            return this;
        }

        B bij(String anker, double dx, double dz) {
            ankers.add(new Ref(anker, dx, dz));
            return this;
        }

        B alleen(Set<NpcFase> f) {
            fasen = f;
            return this;
        }

        B paard() {
            soort = Soort.PAARD;
            return this;
        }

        void klaar() {
            ALLE.put(naam, new Def(naam, soort, variant, maat, gedrag, kapsel, haar, List.copyOf(altijd), Map.copyOf(perFase), look,
                    List.copyOf(ankers), x, z, yaw, fasen));
        }
    }

    private static B npc(String naam, double x, double z, float yaw) {
        return new B(naam, x, z, yaw);
    }

    static {
        // The fallback spots lie inside the plaza (x -350..-276, z -735..-661); the portal is to the south (-300, -655), so most
        // guhs look south (yaw 0) towards the people coming in. Real spots: the plein slice's anchors npc_<naam>.

        // --- the six (spread over the plein; together in the scenes) -----------------------------------------------------------
        npc("joost", -306, -671, 0).doet(Gedrag.SLENTER).haar(KAPSEL_KUIFJE, BRUIN)
                .in(NpcFase.SINT, WINTER_SCARF).in(NpcFase.KERST, SANTA_HAT, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, FEESTHOEDJE, SLINGER_SJAALTJE)
                .bij("kerstboom", 2, 2).klaar();
        // the bow on her head, like her statue in Het Knabbelpark
        npc("suus", -304, -672, 0).maat(0.95f).doet(Gedrag.SLENTER).haar(KAPSEL_KRULLEN, KASTANJE).draagt(OORSTRIKJE_ROZE)
                .in(NpcFase.SINT, KNUS_SJAALTJE).in(NpcFase.KERST, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, FEESTHOEDJE, KNUS_SJAALTJE)
                .bij("kerstboom", 3, 2).klaar();
        // her golden nose ring, like her statue in Het Knabbelpark (the look piece "liek")
        npc("liek", -298, -682, 0).doet(Gedrag.SLENTER).haar(KAPSEL_KNOTJES, DONKERBRUIN).look("liek")
                .in(NpcFase.SINT, ELFTOCHT_SJAAL).in(NpcFase.KERST, SANTA_HAT, ELFTOCHT_SJAAL).in(NpcFase.OUDJAAR, FEESTHOEDJE)
                .bij("oliebollenkraam", -2, 2).klaar();
        npc("lin", -296, -683, 0).maat(0.95f).doet(Gedrag.SLENTER).haar(KAPSEL_PLUISBOL, ROODBRUIN)
                .in(NpcFase.SINT, KNUS_SJAALTJE).in(NpcFase.KERST, ELFTOCHT_OORWARMERS, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, FEESTHOEDJE)
                .bij("oliebollenkraam", -1, 2).klaar();
        // tall and blond, with her Japanese bow (Evivads of het weebhuisje is 1.12 too)
        npc("evi", -312, -699, 0).maat(1.12f).doet(Gedrag.SLENTER).haar(KAPSEL_VLECHTJES, BLOND).draagt(JAPAN_STRIKJE)
                .in(NpcFase.KERST, SANTA_HAT).in(NpcFase.OUDJAAR, FEESTHOEDJE).bij("prikbord", -1, 2).klaar();
        // glasses, light-brown hair, a train conductor's cap (Nielsvads is 1.2, a little taller than Evi)
        npc("niels", -310, -700, 0).maat(1.2f).doet(Gedrag.SLENTER).haar(KAPSEL_KUIFJE, LICHTBRUIN).draagt(BIEB_LEESBRIL, KARRETJES_CONDUCTEURSPETJE)
                .in(NpcFase.KERST, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, SLINGER_SJAALTJE).bij("prikbord", 1, 2).klaar();

        // --- the family ----------------------------------------------------------------------------------------------------------
        npc("krijn", -331, -716, 0).maat(1.1f).haar(KAPSEL_MATJE, GRIJS).draagt(BIEB_VEST)
                .in(NpcFase.KERST, SANTA_HAT, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, FEESTHOEDJE).bij("huis_deur", -1.5, 1.5).klaar();
        npc("corine", -328, -716, 0).maat(1.05f).haar(KAPSEL_KNOTJES, BRUIN).draagt(SMUL_SCHORT)
                .in(NpcFase.KERST, SANTA_HAT).in(NpcFase.OUDJAAR, FEESTHOEDJE).bij("huis_deur", 1.5, 1.5).klaar();
        npc("marrie", -333, -720, 0).variant(GuhVariant.SNOW).maat(0.92f).doet(Gedrag.ZITTEN).haar(KAPSEL_KRULLEN, WIT)
                .draagt(BIEB_LEESBRIL, KNUS_SJAALTJE).in(NpcFase.KERST, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, FEESTHOEDJE)
                .bij("huis_haard", -1.5, 1).klaar();
        npc("wijnand", -330, -720, 0).variant(GuhVariant.SNOW).maat(0.95f).doet(Gedrag.ZITTEN).draagt(GOLF_PET, WINTER_SCARF)
                .alleen(VANAF_KERST).bij("huis_haard", 1.5, 1).klaar();
        npc("rins", -292, -706, 0).maat(1.05f).haar(KAPSEL_VLECHTJES, KASTANJE).draagt(KNUS_SJAALTJE)
                .in(NpcFase.KERST, SANTA_HAT).in(NpcFase.OUDJAAR, FEESTHOEDJE).bij("kade_boot", -1.5, 2).klaar();
        // from Guhmerika, with a camera round his neck (the look piece "anthony") and a cap
        npc("anthony", -290, -707, 0).maat(1.1f).doet(Gedrag.SLENTER).haar(KAPSEL_KUIFJE, ZANDBLOND).draagt(DETECTIVE_PET).look("anthony")
                .in(NpcFase.KERST, CHRISTMAS_SWEATER).in(NpcFase.OUDJAAR, FEESTHOEDJE).bij("kade_boot", 1.5, 2).klaar();
        // Daan's family at the pond: Finn skates, Liv sits on her sled, Daan pushes
        npc("daan", -322, -677, 0).maat(1.1f).doet(Gedrag.DUWEN).haar(KAPSEL_KUIFJE, BRUIN).draagt(ELFTOCHT_SCHAATSMUTS, ELFTOCHT_SJAAL)
                .bij("vijver_midden", 3, 0).klaar();
        npc("kim", -327, -673, 0).haar(KAPSEL_VLECHTJES, ZANDBLOND).draagt(ELFTOCHT_OORWARMERS, KNUS_SJAALTJE)
                .bij("vijver_midden", -4, 4).klaar();
        npc("finn", -325, -680, 0).maat(0.6f).doet(Gedrag.SCHAATSEN).draagt(ELFTOCHT_SCHAATSMUTS, ELFTOCHT_TRUITJE)
                .bij("vijver_midden", 0, 0).klaar();
        npc("liv", -321, -678, 0).maat(0.5f).doet(Gedrag.SLEE).draagt(BALTO_SNEEUWMUTS, BALTO_WANTJES, OORSTRIKJE_ROZE)
                .bij("vijver_midden", 3, -1).klaar();

        // --- Sinterklaas and his helpers (until 6 December) ---------------------------------------------------------------------
        // his own mitre; the look piece "sint": the big white beard, the red mantle with gold, the golden staff
        npc("sint", -313, -690, 0).maat(1.2f).doet(Gedrag.ZITTEN).draagt(SINT_MITRE).look("sint").alleen(SINT)
                .bij("sint_stoel", 0, 0).klaar();
        double[][] pakje = {{-318, -686}, {-308, -686}, {-317, -694}, {-309, -694}};
        for (int i = 0; i < PAKJESGUHS; i++) {
            // pink guhs in a colourful pietenpakje with a beret and a feather, and one tiny soot smudge (the look "pakje_<i>")
            npc("pakjesguh_" + i, pakje[i][0], pakje[i][1], 0).maat(0.9f).doet(Gedrag.SLENTER).look("pakje_" + i).alleen(SINT)
                    .bij("sint_stoel", i % 2 == 0 ? -3 - i : 3 + i, 2 + i).klaar();
        }
        npc("ozovads", -309, -690, 0).paard().alleen(SINT).bij("sint_stoel", 3, -1).klaar();

        // --- the shop --------------------------------------------------------------------------------------------------------------
        npc("winkelguh", -287, -671, 0).haar(KAPSEL_KRULLEN, KASTANJE).draagt(KNUS_SJAALTJE, BAKKERSSCHORTJE)
                .in(NpcFase.KERST, SANTA_HAT).in(NpcFase.OUDJAAR, FEESTHOEDJE).bij("winkel", 0, 0).klaar();
    }

    private NpcDefs() {
    }

    public static Map<String, Def> alle() {
        return Collections.unmodifiableMap(ALLE);
    }

    @Nullable
    public static Def van(String naam) {
        return ALLE.get(naam);
    }
}
