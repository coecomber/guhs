package nl.juiced.guhs.feature.snuffeldorp;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.Boom;
import nl.juiced.guhs.feature.snuffel.Daden;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Examen;
import nl.juiced.guhs.feature.snuffel.Geurbronnen;
import nl.juiced.guhs.feature.snuffel.Geuren;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.Keuze;
import nl.juiced.guhs.feature.snuffel.Maatjes;
import nl.juiced.guhs.feature.snuffel.Reis;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The FIRST SERIES of Het Snuffeleiland: what happens between washing ashore and the Guhstation (the steps
 * {@code STAP_STRAND} .. {@code STAP_SPOOR} of the kern's questline {@code snuffeleiland}). Everything is per player: the
 * step, the flags and counters of the questline ({@code LIJN.vlag / teller}), and the kern's own lists (scents, found
 * sources, good deeds, the tree's stage).
 * <ol>
 *   <li><b>Aangespoeld</b>: the first time a dog is on the island the scene {@link DorpScenes#WAKKER} plays on the beach;
 *   Jutje Kwispel waits at the strandpoort and sends you to the doctor.</li>
 *   <li><b>Naar de dokter</b>: Dokter Pleisterpoot hears the story: the flower is only found by a real sniffer.</li>
 *   <li><b>Snuffelles</b>: Meester Truffelneus gives three lessons with real scent sources, one at a time ({@link #LESSEN}:
 *   a buried bone close by, his whistle further off, the bees that are not buried).</li>
 *   <li><b>Er rommelt iets</b>: back on the plein the scene {@link DorpScenes#MAATJE} plays; the companion appears.</li>
 *   <li><b>Goede daden</b>: six residents lost something ({@link #KLUSSEN}): ask, sniff it up, bring it back. Each is a
 *   good deed (the tree grows a step with the kern's growth scene); four are needed, the others stay to be done.</li>
 *   <li><b>Het snuffelexamen</b>: four fresh sources, one of every kind of scent ({@link #EXAMEN_BRONNEN}); then the
 *   diploma and the rank Snuffelpup.</li>
 *   <li><b>Een spoor van papa</b>: at the tree the companion gives the blossom twig; between the stones lies father's
 *   scarf; the scene {@link DorpScenes#SPOOR}; the story is finished and the Guhstation is given ({@code Snuffel.rondAf}).</li>
 * </ol>
 * The residents' side of it: {@link DorpRollen}. The closed rest of the island: {@link Wegversperring}.
 */
public final class Dorp {
    public static final Verhaallijn LIJN = SnuffelFeature.LIJN;
    /** Flags and counters of the questline (per player). */
    public static final String WAKKER = "wakker", LES = "les", LES_GEVONDEN = "les_gevonden", GESLAAGD = "geslaagd", BLOESEM = "bloesem", SJAAL = "sjaal";
    /** The scent sources of the three lessons, in order (ids of eiland.json). */
    public static final List<String> LESSEN = List.of("dorp_les_kluifje", "dorp_les_fluitje", "dorp_les_bijen");
    /** The trainer's exam and its four sources. */
    public static final String EXAMEN = "snuffelpup";
    public static final List<String> EXAMEN_BRONNEN = List.of("dorp_examen_kaasknabbel", "dorp_examen_tweedpet", "dorp_examen_kippen", "dorp_examen_paddenstoel");
    /** Father's scarf at the tree, and the scent of the companion itself. */
    public static final String SJAAL_BRON = "dorp_papa_sjaal", GEEST = "bosgeestje";

    /** Something a resident lost: who (the resident's key), the good deed's id, the scent source's id. */
    public record Klus(String bewoner, String daad, String bron) {
        public String gevraagd() {
            return bewoner + "_gevraagd";
        }

        public String gevonden() {
            return bewoner + "_gevonden";
        }
    }

    public static final List<Klus> KLUSSEN = List.of(
            new Klus("bakker", "bakker_deegroller", "dorp_deegroller"),
            new Klus("visser", "visser_dobber", "dorp_dobber"),
            new Klus("juf", "juf_schoolbel", "dorp_schoolbel"),
            new Klus("oma", "oma_bolwol", "dorp_bolwol"),
            new Klus("tuinder", "tuinder_gietertje", "dorp_gietertje"),
            new Klus("pup", "pup_stuiterbal", "dorp_stuiterbal"));

    private static final int S_STRAND = SnuffelFeature.STAP_STRAND, S_DOKTER = SnuffelFeature.STAP_DOKTER, S_LES = SnuffelFeature.STAP_LES,
            S_MAATJE = SnuffelFeature.STAP_MAATJE, S_DADEN = SnuffelFeature.STAP_DADEN, S_EXAMEN = SnuffelFeature.STAP_EXAMEN,
            S_SPOOR = SnuffelFeature.STAP_SPOOR;

    private Dorp() {
    }

    static void init() {
        Gesprek.init();
        // the lessons: one source at a time, only while its lesson runs
        for (int i = 0; i < LESSEN.size(); i++) {
            int les = i + 1;
            Geurbronnen.voorwaarde(LESSEN.get(i), p -> LIJN.stap(p) == S_LES && LIJN.teller(p, LES) == les && !LIJN.vlag(p, LES_GEVONDEN));
            Geurbronnen.bijVondst(LESSEN.get(i), p -> {
                LIJN.vlag(p, LES_GEVONDEN, true);
                meld(p, Component.translatable("gui.guhs.snuffeldorp.les_gevonden"));
            });
        }
        // what the residents lost: in the air once the resident asked, from the good deeds on (also after the story)
        for (Klus k : KLUSSEN) {
            Geurbronnen.voorwaarde(k.bron(), p -> LIJN.stap(p) >= S_DADEN && LIJN.vlag(p, k.gevraagd()));
            Geurbronnen.bijVondst(k.bron(), p -> {
                LIJN.vlag(p, k.gevonden(), true);
                meld(p, Component.translatable("gui.guhs.snuffeldorp.breng_terug", Component.translatable("entity.guhs.snuffel_bewoner." + k.bewoner())));
            });
        }
        Examen.registreer(EXAMEN, EXAMEN_BRONNEN, p -> {
            LIJN.vlag(p, GESLAAGD, true);
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.examen_klaar").withStyle(ChatFormatting.GOLD));
        });
        Geurbronnen.voorwaarde(SJAAL_BRON, p -> LIJN.stap(p) == S_SPOOR && LIJN.vlag(p, BLOESEM));
        Geurbronnen.bijVondst(SJAAL_BRON, p -> LIJN.vlag(p, SJAAL, true));
        // the hidden advancements of the FTB quests (tools/features/snuffel_dorp.py)
        Daden.opDaad((p, daad) -> {
            for (Klus k : KLUSSEN) {
                if (k.daad().equals(daad)) {
                    GuhAdvancements.grant(p, "snuffel_dorp_daad_" + k.bewoner());
                }
            }
            if (Daden.aantal(p) >= SnuffelFeature.DADEN_NODIG) {
                GuhAdvancements.grant(p, "snuffel_dorp_boom");
            }
            if (klussenGedaan(p) >= KLUSSEN.size()) {
                GuhAdvancements.grant(p, "snuffel_dorp_alle");
            }
        });
        Snuffel.sleutel(S_STRAND, S_SPOOR, Dorp::sleutel);
        Snuffel.doel(S_STRAND, S_SPOOR, Dorp::doel);
        DorpRollen.init();
    }

    private static void meld(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.GREEN));
    }

    // =====================================================================================================================
    // what the Guhdex and the objective line say
    // =====================================================================================================================

    /** The text variant of a step for this player (null: the step's own text). */
    @Nullable
    static String sleutel(ServerPlayer p, int stap) {
        if (!Eiland.in(p)) {
            // (at home in the middle of the story: how to get back)
            return Reis.bezocht(p) ? "thuis" : null;
        }
        if (stap == S_LES) {
            int les = LIJN.teller(p, LES);
            return les <= 0 ? null : LIJN.vlag(p, LES_GEVONDEN) ? "4_terug" : les == 1 ? "4_bot" : les == 2 ? "4_fluit" : "4_bij";
        }
        if (stap == S_DADEN) {
            boolean zoek = false;
            for (Klus k : KLUSSEN) {
                if (Daden.heeft(p, k.daad())) {
                    continue;
                }
                if (LIJN.vlag(p, k.gevonden())) {
                    return "6_breng";
                }
                zoek |= LIJN.vlag(p, k.gevraagd());
            }
            return zoek ? "6_zoek" : null;
        }
        if (stap == S_EXAMEN) {
            return LIJN.vlag(p, GESLAAGD) ? "7_diploma" : Examen.bezig(p) != null ? "7_bezig" : null;
        }
        if (stap == S_SPOOR) {
            return LIJN.vlag(p, BLOESEM) ? "8_snuffel" : null;
        }
        return null;
    }

    /** Where "Mijn verhaal" points during this step: the spot on the island, or (at home) a steigerhuisje. */
    @Nullable
    static Doel doel(ServerPlayer p, int stap) {
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null) {
            return Reis.bezocht(p) ? Doel.structuur(ModDimensions.GUHMENSION, "steigerhuisje", Component.translatable("gui.guhs.snuffeldorp.doel.terug")) : null;
        }
        Plekken pl = Plekken.van(plaats);
        if (pl == null) {
            return null;
        }
        String naam = stap == S_STRAND ? Plekken.STRANDPOORT : stap == S_DOKTER ? Plekken.DOKTER : stap == S_LES || stap == S_EXAMEN ? Plekken.WEI
                : stap == S_MAATJE || stap == S_DADEN ? Plekken.PLEIN : Plekken.BOOM;
        BlockPos plek = pl.wereld(plaats, naam);
        return plek == null ? null : Doel.plek(plaats.level().dimension(), plek, Component.translatable("gui.guhs.snuffeldorp.doel." + naam));
    }

    // =====================================================================================================================
    // every tick for a dog on the island
    // =====================================================================================================================

    static void tick(ServerPlayer p) {
        if (!p.isAlive() || !Hondvorm.actief(p)) {
            return;
        }
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null) {
            return;
        }
        Plekken pl = Plekken.van(plaats);
        if (pl == null) {
            return;
        }
        Wegversperring.tick(p, plaats, pl);
        Zee.tick(p, plaats, pl);
        // (the server's clock: a game test's mock player has no tick count of its own)
        if ((p.level().getServer().getTickCount() + p.getId()) % 5 != 0 || Cutscenes.bezig(p)) {
            return;
        }
        if (!LIJN.vlag(p, WAKKER)) {
            aankomst(p, plaats, pl);
            return;
        }
        int stap = LIJN.stap(p);
        if (stap < S_STRAND) {
            LIJN.zet(p, S_STRAND);
        } else if (stap == S_MAATJE) {
            rommel(p, plaats, pl);
        } else if (stap == S_SPOOR) {
            spoor(p, plaats);
        }
    }

    /** The first time on the island: the story begins, and on the beach the waking scene plays. */
    private static void aankomst(ServerPlayer p, Eiland.Plaats plaats, Plekken pl) {
        LIJN.begin(p);
        BlockPos strand = pl.wereld(plaats, Plekken.STRAND);
        if (LIJN.stap(p) > S_STRAND || strand == null || !strand.closerToCenterThan(p.position(), 24)) {
            // (came another way, or is further in the story already: no scene)
            wakker(p);
            return;
        }
        Cutscenes.speel(p, DorpScenes.WAKKER, strand, Rotation.NONE, Dorp::wakker);
    }

    /** After the waking scene: the step "Aangespoeld" runs, and the dog hears what its paws can do. */
    static void wakker(ServerPlayer p) {
        boolean eerste = !LIJN.vlag(p, WAKKER);
        LIJN.vlag(p, WAKKER, true);
        LIJN.begin(p);
        if (LIJN.stap(p) < S_STRAND) {
            LIJN.zet(p, S_STRAND);
        }
        if (eerste && LIJN.stap(p) == S_STRAND) {
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.wakker.1").withStyle(ChatFormatting.AQUA));
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.wakker.2").withStyle(ChatFormatting.GRAY));
        }
    }

    /** "Er rommelt iets": near the well the bucket scene plays; after it the companion is there. */
    private static void rommel(ServerPlayer p, Eiland.Plaats plaats, Plekken pl) {
        BlockPos emmer = pl.wereld(plaats, Plekken.EMMER);
        if (emmer == null) {
            maatje(p);
        } else if (emmer.closerToCenterThan(p.position(), 8) && !Praat.bezig(p)) {
            Cutscenes.speel(p, DorpScenes.MAATJE, emmer, Rotation.NONE, Dorp::maatje);
        }
    }

    /** After the bucket scene: the companion appears, its own strange scent is learned, the good deeds begin. */
    static void maatje(ServerPlayer p) {
        Maatjes.geef(p);
        Geuren.leer(p, GEEST);
        GuhAdvancements.grant(p, "snuffel_dorp_maatje");
        if (LIJN.verder(p, S_MAATJE)) {
            Component naam = Honden.maatjeNaam(Keuze.vanOfStandaard(p).maatje());
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.maatje.1", naam).withStyle(ChatFormatting.GREEN));
            p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.maatje.2", naam).withStyle(ChatFormatting.GREEN));
            Maatjes.ondeugend(p, 80);
        }
    }

    /** "Een spoor van papa": the twig at the tree, then (the scarf dug up) the last scene and the end. */
    private static void spoor(ServerPlayer p, Eiland.Plaats plaats) {
        if (!LIJN.vlag(p, BLOESEM)) {
            if (plaats.boom().closerToCenterThan(p.position(), 7) && !Praat.bezig(p)) {
                bloesem(p, plaats);
            }
            return;
        }
        if (LIJN.vlag(p, SJAAL)) {
            Rotation draai = Rotation.values()[Math.floorMod(plaats.opzet().boomDraai(), Rotation.values().length)];
            Cutscenes.speel(p, DorpScenes.SPOOR, plaats.boom(), draai, Dorp::einde);
        }
    }

    /** The tree's gift: the companion's tree blossoms for you and gives a twig; now the scarf can be smelled. */
    static void bloesem(ServerPlayer p, Eiland.Plaats plaats) {
        LIJN.vlag(p, BLOESEM, true);
        Boom.geefCadeau(p);
        Vec3 kruin = Vec3.atBottomCenterOf(plaats.boom()).add(0, 1.4, 0);
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, false, kruin.x, kruin.y, kruin.z, 40, 0.7, 0.5, 0.7, 0.02);
            level.playSound(null, plaats.boom(), SnuffelFeature.GROEI_GELUID.get(), SoundSource.PLAYERS, 0.8f, 1.3f);
        }
        Maatjes.blij(p);
        Component naam = Honden.maatjeNaam(Keuze.vanOfStandaard(p).maatje());
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.bloesem.1", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.bloesem.2", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * The very end of the first series: the story is finished (the Guhpad asks for this), the Guhstation is given and with
     * it the island's music disc (the captain's present, handed over by the companion).
     */
    static void einde(ServerPlayer p) {
        if (!Snuffel.rondAf(p)) {
            return;
        }
        Component naam = Honden.maatjeNaam(Keuze.vanOfStandaard(p).maatje());
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.einde.1").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.einde.2", naam).withStyle(ChatFormatting.GREEN));
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.einde.plaat", naam).withStyle(ChatFormatting.AQUA));
        p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.einde.3").withStyle(ChatFormatting.GOLD));
        Maatjes.blij(p);
    }

    // =====================================================================================================================
    // for the residents (DorpRollen) and the dev command
    // =====================================================================================================================

    /** How many of the residents' lost things this player brought back. */
    public static int klussenGedaan(ServerPlayer p) {
        int n = 0;
        for (Klus k : KLUSSEN) {
            if (Daden.heeft(p, k.daad())) {
                n++;
            }
        }
        return n;
    }

    @Nullable
    public static Klus klusVan(String bewoner) {
        for (Klus k : KLUSSEN) {
            if (k.bewoner().equals(bewoner)) {
                return k;
            }
        }
        return null;
    }

    /** Enough good deeds: the step "Goede daden" is done (the tree is a jong boompje). True when the step moved. */
    static boolean dadenKlaar(ServerPlayer p) {
        return Daden.aantal(p) >= SnuffelFeature.DADEN_NODIG && LIJN.verder(p, S_DADEN);
    }
}
