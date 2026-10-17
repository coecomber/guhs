package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.MeneerKrabguhEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.6), slice sp_d2_heen: the direction of part 2 ({@value SpVerhaal#KROON}) steps 0-4. This
 * slice owns those five steps ({@link SpStappen#echt}); every step moves on only in the {@code daarna} of its scene (or at the
 * end of its game), so a player who logs out in the middle plays that bit again (nothing is lost, nothing costs anything).
 * Every 10 ticks {@link #regie} looks at where the player stands and what their step asks for:
 * <ol start="0">
 *   <li>{@code K_MANAGER}: walking into De Krokante Knabbel (zone {@code d2h_knabbel}): the narrator card of part 2, then
 *       {@code manager} (Octo becomes manager, Sponsguh is "just a kid", Patguh: ice cream!). The hint comes a moment after
 *       part 1 ends ({@link #receptKlaar});</li>
 *   <li>{@code K_IJSSALON}: on your stool in the Guhfy Guhber: {@code ijssalon} (THE line, CONTRACT_E2_C 5.3), the
 *       Guhfy Guhber ice-cream hat, then wink 8 {@code knipoog_ijs} and the horn from the square;</li>
 *   <li>{@code K_KROON}: at the king's throne on the square: {@code bevroren} (big, with {@code SpAudio.moment("bevroren")});</li>
 *   <li>{@code K_RIT}: the drive ({@link Rit}): {@code rit} at the car, {@code kroeg} on arrival;</li>
 *   <li>{@code K_SLEUTEL}: in De Ruige Kroeg: {@code baby} (the first time), then the stand-still game ({@link Kroeg}); the key
 *       in your hands: {@code sleutel_terug} outside on the bench, and the step moves on to sp_d2_weg.</li>
 * </ol>
 * Visibility (CONTRACT_E2_C 4.3, this slice owns these resident spots): the frozen Krabguh at {@code bikinibodem/
 * bevroren_krabguh} (a resident of this slice, {@link Bewoners#anders}) for steps {@code K_RIT}..{@code K_GUHFY}; the bar's
 * Mikas of De Ruige Kroeg (residents of sp_cast) from {@code K_SNOR} on (before that your own copies play in there). Clicks on
 * the cast during these steps: {@link #klik}.
 */
public final class SpD2Heen {
    /** The narrator card of part 2 (with its drawn map of the road to Schelpenstad). */
    public static final String KAART = "einde2_sp_d2_heen";
    /** The text prefixes. */
    static final String G = "gui.guhs.einde2_sp_d2_heen.", Q = "quest.guhs.einde2_sp_d2_heen.";
    /** Tag of every entity this slice spawns for one player (a waiting Sponsguh, a rider, a bar Mika copy): never kept. */
    static final String TIJDELIJK = "guhs_einde2_sp_d2_heen_tijdelijk";
    /** The line's flag while the key is in your hands (the scene sleutel_terug still to come). */
    static final String SLEUTEL = "sp_d2_heen_sleutel";
    /** The resident spot of the frozen Krabguh. */
    static final String BEVROREN_PLEK = D2Plek.BB + "/bevroren_krabguh";
    /** The resident spots of De Ruige Kroeg (sp_cast makes them residents; this slice decides when they are seen). */
    static final Set<String> KROEG_PLEKKEN = Set.of(D2Plek.SP + "/barman", D2Plek.SP + "/uitsmijter", D2Plek.SP + "/stoere_mika_1",
            D2Plek.SP + "/stoere_mika_2", D2Plek.SP + "/stoere_mika_3", D2Plek.SP + "/stoere_mika_4", D2Plek.SP + "/stoere_mika_5",
            D2Plek.SP + "/stoere_mika_6", D2Plek.SP + "/slapende_mika");
    /** The clicks this slice answers: cast id -> the steps with a line. */
    static final Map<String, Set<Integer>> KLIKKEN = Map.of("sponsguh", Set.of(0, 1, 2, 3, 4), "patguh", Set.of(0, 1, 2, 3, 4),
            "meneer_krabguh", Set.of(0, 1), "octo_mika", Set.of(0, 1, 2));
    /** The scenes by name, with their structure and anchor marker (the dev command, the report). */
    static final Map<String, String[]> ANKERS = Map.of(
            "manager", new String[] {D2Plek.BB, "d2h_zaal"}, "ijssalon", new String[] {D2Plek.BB, "ijs_speler"},
            "knipoog_ijs", new String[] {D2Plek.BB, "knipoog_ijs_coupe"}, "bevroren", new String[] {D2Plek.BB, "neptuguh_troon"},
            "rit", new String[] {D2Plek.BB, "mobiel_start"}, "kroeg", new String[] {D2Plek.SP, "kroeg_parkeer"},
            "baby", new String[] {D2Plek.SP, "kroeg_sluip"}, "sleutel_terug", new String[] {D2Plek.SP, "rustpunt_1"});

    private SpD2Heen() {
    }

    static Verhaallijn kroon() {
        return SpVerhaal.KROON_LIJN;
    }

    // =====================================================================================================================
    // register (common, both sides)
    // =====================================================================================================================

    static void register() {
        String k = SpVerhaal.KROON;
        for (int s = SpVerhaal.K_MANAGER; s <= SpVerhaal.K_SLEUTEL; s++) {
            SpStappen.echt(k, s);
        }
        Verteller.registreer(KAART, 4, k);
        D2Scenes.registreer();
        D2Hints.registreer();
        SpStappen.sleutel(k, SpVerhaal.K_RIT, (p, s) -> Rit.rijdt(p) ? "3_b" : "3");
        SpStappen.sleutel(k, SpVerhaal.K_SLEUTEL, (p, s) -> Kroeg.actief(p) ? "4_b" : "4");
        SpStappen.doel(k, SpVerhaal.K_MANAGER, (p, s) -> doel(p, D2Plek.BB, "d2h_zaal", "0"));
        SpStappen.doel(k, SpVerhaal.K_RIT, (p, s) -> Rit.rijdt(p) ? doel(p, D2Plek.SP, "kroeg_parkeer", "3_b") : doel(p, D2Plek.BB, "mobiel_start", "3"));
        SpStappen.ga(k, SpVerhaal.K_MANAGER, SpD2Heen::gaManager);
        SpStappen.ga(k, SpVerhaal.K_IJSSALON, p -> wisEigen(p, false));
        SpStappen.ga(k, SpVerhaal.K_KROON, p -> wisEigen(p, false));
        SpStappen.ga(k, SpVerhaal.K_RIT, SpD2Heen::gaRit);
        SpStappen.ga(k, SpVerhaal.K_SLEUTEL, SpD2Heen::gaSleutel);
        kroon().opStap(SpD2Heen::opStap);
        SpVerhaal.RECEPT_LIJN.opStap((p, oud, nieuw) -> {
            if (nieuw >= SpVerhaal.R_STAPPEN) {
                receptKlaar(p);
            }
        });
        // the residents of this slice's spots (CONTRACT_E2_C 4.3)
        Bewoners.anders(BEVROREN_PLEK, "guhs:meneer_krabguh", -1, true, 0);
        Bewoners.BIJ_MAKEN.add(SpD2Heen::bijMaken);
        SponsguhWezen.BIJ_KLIK.add(SpD2Heen::klik);
        NeoForge.EVENT_BUS.addListener(SpD2Heen::opTick);
        NeoForge.EVENT_BUS.addListener(SpD2Heen::opWeg);
        NeoForge.EVENT_BUS.addListener(SpD2Heen::opRespawn);
        NeoForge.EVENT_BUS.addListener(SpD2Heen::opDimensie);
        NeoForge.EVENT_BUS.addListener(SpD2Heen::opBinnen);
        NeoForge.EVENT_BUS.addListener(E2SpD2HeenCommando::register);
    }

    /** The compass target and objective line of a step: a marker (or the structure while its copy is not known). */
    @Nullable
    private static Doel doel(ServerPlayer p, String structuur, String plek, String sleutel) {
        net.minecraft.server.level.ServerLevel g = p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        BlockPos pos = g == null || !nl.juiced.guhs.feature.einde2.Plekken.bekend(g, structuur) ? null : nl.juiced.guhs.feature.einde2.Plekken.wereld(g, structuur, plek);
        if (pos == null) {
            return Doel.structuur(GuheindeFeature.GUHEINDE, structuur, Component.translatable("structure.guhs." + structuur));
        }
        return Doel.plek(GuheindeFeature.GUHEINDE, pos, Component.translatable("gui.guhs.verhalen." + SpVerhaal.KROON + ".waar." + sleutel));
    }

    /** Is the player at this step of part 2 (part 1 done, part 2 not)? */
    static boolean opStap(ServerPlayer p, int stap) {
        return SpVerhaal.receptKlaar(p) && !SpVerhaal.klaar(p) && kroon().stap(p) == stap;
    }

    // =====================================================================================================================
    // the direction
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.level().getServer() == null) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        Kroeg.tick(p, nu);
        if ((nu + p.getId()) % 10 == 0) {
            regie(p);
        }
    }

    /** What the player's step asks for, where they stand now. Public for the game tests and the dev command. */
    public static void regie(ServerPlayer p) {
        if (!p.isAlive() || Cutscenes.bezig(p) || Kroeg.actief(p) || !GuheindeFeature.isGuheinde(p.level()) || !SpVerhaal.open(p)
                || !SpVerhaal.receptKlaar(p) || SpVerhaal.klaar(p)) {
            return;
        }
        switch (kroon().stap(p)) {
            case SpVerhaal.K_MANAGER -> {
                boolean binnen = nl.juiced.guhs.feature.einde2.Plekken.plek(D2Plek.BB, "d2h_knabbel") != null ? D2Plek.in(p, D2Plek.BB, "d2h_knabbel", 0)
                        : D2Plek.bij(p, D2Plek.BB, "d2h_zaal", 6);
                if (binnen) {
                    manager(p);
                }
            }
            case SpVerhaal.K_IJSSALON -> {
                if (D2Plek.bij(p, D2Plek.BB, "ijs_speler", 2.2)) {
                    ijssalon(p);
                }
            }
            case SpVerhaal.K_KROON -> {
                if (!Cutscenes.gezien(p, D2Scenes.KNIPOOG_IJS.id()) && D2Plek.in(p, D2Plek.BB, "ijssalon", 0)) {
                    knipoog(p);                                       // (logged out right after the ice cream: wink 8 still comes)
                } else if (D2Plek.bij(p, D2Plek.BB, "neptuguh_troon", 10)) {
                    bevroren(p);
                }
            }
            case SpVerhaal.K_RIT -> Rit.regie(p);
            case SpVerhaal.K_SLEUTEL -> {
                if (kroon().vlag(p, SLEUTEL)) {
                    sleutelTerug(p);
                } else if (D2Plek.in(p, D2Plek.SP, "ruige_kroeg", 0)) {
                    if (Cutscenes.gezien(p, D2Scenes.BABY.id())) {
                        Kroeg.start(p, p.level().getServer().getTickCount());
                    } else {
                        speel(p, D2Scenes.BABY, D2Plek.SP, "kroeg_sluip", q -> Kroeg.start(q, q.level().getServer().getTickCount()));
                    }
                }
            }
            default -> {
            }
        }
    }

    /** Step 0: the narrator card of part 2 (once), then the scene manager. */
    private static void manager(ServerPlayer p) {
        Consumer<ServerPlayer> scene = q -> speel(q, D2Scenes.MANAGER, D2Plek.BB, "d2h_zaal", r -> kroon().verder(r, SpVerhaal.K_MANAGER));
        if (Verteller.gezien(p, KAART)) {
            scene.accept(p);
        } else {
            Verteller.toon(p, KAART, scene);
        }
    }

    /** Step 1: the ice cream (THE line), the hat, wink 8, the horn. */
    private static void ijssalon(ServerPlayer p) {
        speel(p, D2Scenes.IJSSALON, D2Plek.BB, "ijs_speler", q -> {
            hoedje(q);
            if (kroon().verder(q, SpVerhaal.K_IJSSALON)) {
                knipoog(q);
            }
        });
    }

    /** Wink 8 (Baltoguh behind his sundae), right after the ice cream on the same visit; then the horn. */
    private static void knipoog(ServerPlayer p) {
        if (!speel(p, D2Scenes.KNIPOOG_IJS, D2Plek.BB, "knipoog_ijs_coupe", SpD2Heen::hoorn)) {
            hoorn(p);
        }
    }

    /** The horn from the square: Neptuguh is in town. */
    private static void hoorn(ServerPlayer p) {
        Schijn.geluid(p, SoundEvents.RAID_HORN.value(), p.blockPosition().above(6), 1.0f, 1.0f);
        p.sendSystemMessage(Component.translatable(Q + "hoorn").withStyle(ChatFormatting.GOLD));
    }

    /** Step 2: the crown is gone and Krabguh frozen. */
    private static void bevroren(ServerPlayer p) {
        SpAudio.moment(p, "bevroren");
        if (!speel(p, D2Scenes.BEVROREN, D2Plek.BB, "neptuguh_troon", q -> {
            SpAudio.stopMoment(q, 40);
            kroon().verder(q, SpVerhaal.K_KROON);
        })) {
            SpAudio.stopMoment(p, 0);
        }
    }

    /** The key is in your hands (the end of the stand-still game). */
    static void sleutelGepakt(ServerPlayer p) {
        if (!opStap(p, SpVerhaal.K_SLEUTEL)) {
            return;
        }
        kroon().vlag(p, SLEUTEL, true);
        StapHint.later(p, 20, SpD2Heen::sleutelTerug);
    }

    /** Step 4's end: outside on the bench: sleutel_terug; then the step (sp_d2_weg takes over). */
    private static void sleutelTerug(ServerPlayer p) {
        if (!opStap(p, SpVerhaal.K_SLEUTEL) || Cutscenes.bezig(p)) {
            return;
        }
        speel(p, D2Scenes.SLEUTEL_TERUG, D2Plek.SP, "rustpunt_1", q -> {
            Vec3 bank = D2Plek.staan(q, D2Plek.SP, "rustpunt_1");
            if (bank != null) {
                D2Plek.breng(q, bank, -90f);                         // (outside, looking east along the road)
            }
            kroon().vlag(q, SLEUTEL, false);
            kroon().verder(q, SpVerhaal.K_SLEUTEL);
        });
    }

    /** Part 1 is done: part 2 shows in the Guhdex, and a moment later its first hint (De Krokante Knabbel has news). */
    private static void receptKlaar(ServerPlayer p) {
        kroon().begin(p);
        StapHint.later(p, 100, q -> {
            if (opStap(q, SpVerhaal.K_MANAGER)) {
                StapHint.toon(q, SpVerhaal.KROON);
            }
        });
    }

    /** The step listener of part 2. */
    private static void opStap(ServerPlayer p, int oud, int nieuw) {
        Zicht.kijkOpnieuw(p);
        if (oud <= SpVerhaal.K_RIT && nieuw > SpVerhaal.K_RIT) {
            Rit.weg(p);
            kroon().vlag(p, Rit.RIJDT, false);
        }
        if (nieuw > SpVerhaal.K_SLEUTEL) {
            Kroeg.stop(p, false);
            kroon().vlag(p, SLEUTEL, false);
        }
    }

    /** The ice-cream hat (once). */
    private static void hoedje(ServerPlayer p) {
        if (kroon().eenmalig(p, "sp_d2_heen_hoedje")) {
            KledingUnlocks.ontgrendel(p, GuhClothes.SPONSGUH_IJSHOEDJE);
            p.sendSystemMessage(Component.translatable(Q + "beloning.hoedje").withStyle(ChatFormatting.GOLD));
        }
    }

    // =====================================================================================================================
    // the residents, the clicks
    // =====================================================================================================================

    private static void bijMaken(Entity e, String plek) {
        if (plek.equals(BEVROREN_PLEK)) {
            if (e instanceof MeneerKrabguhEntity k) {
                k.setBevroren(true);
                k.zetStil(true);
            }
            Zicht.alleenBij(e, SpVerhaal.KROON, SpVerhaal.K_RIT, SpVerhaal.K_GUHFY);
        } else if (KROEG_PLEKKEN.contains(plek)) {
            Zicht.alleenBij(e, SpVerhaal.KROON, SpVerhaal.K_SNOR, 99);
        }
    }

    /** A resident that stood before this slice (made by an older build of the mod): the same rules when it is loaded. */
    private static void opBinnen(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity e = event.getEntity();
        if (event.loadedFromDisk() && e.entityTags().contains(TIJDELIJK)) {
            event.setCanceled(true);
            return;
        }
        String tag = e.getPersistentData().getStringOr(Bewoners.TAG, "");
        if (tag.isEmpty() || e.entityTags().contains(Zicht.MERK)) {
            return;
        }
        int at = tag.indexOf('@');
        String naam = at < 0 ? tag : tag.substring(0, at);
        for (String s : new String[] {D2Plek.BB, D2Plek.SP}) {
            if (naam.startsWith("einde2_" + s + "_")) {
                bijMaken(e, s + "/" + naam.substring(("einde2_" + s + "_").length()));
            }
        }
    }

    /** A click on the cast during this slice's steps (CONTRACT_E2_C 4.3): a line for the step; false for everything else. */
    private static Boolean klik(SponsguhWezen w, ServerPlayer p) {
        if (!SpVerhaal.receptKlaar(p) || SpVerhaal.klaar(p)) {
            return false;
        }
        int stap = kroon().stap(p);
        if (stap > SpVerhaal.K_SLEUTEL) {
            return false;
        }
        if (w instanceof MeneerKrabguhEntity k && k.isBevroren()) {
            p.sendSystemMessage(Component.translatable(G + "klik.bevroren").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return true;
        }
        Cast c = w.cast();
        Set<Integer> stappen = KLIKKEN.get(c.id());
        if (stappen == null || !stappen.contains(stap)) {
            return false;
        }
        w.getLookControl().setLookAt(p);
        w.zetAnimatie("praat");
        GuhQuests.say(p, w, G + "klik." + c.id() + "." + stap);
        return true;
    }

    // =====================================================================================================================
    // the dev starts (SpStappen.ga: the line is set and the player stands at the step's default place)
    // =====================================================================================================================

    private static void gaManager(ServerPlayer p) {
        wisEigen(p, false);
        Verteller.vergeet(p, KAART);
    }

    private static void gaRit(ServerPlayer p) {
        wisEigen(p, false);
        Vec3 start = D2Plek.staan(p, D2Plek.BB, "mobiel_start");
        if (start != null) {
            D2Plek.breng(p, start.add(0, 0, -6), 0f);
            Rit.zetAuto(p);
        }
    }

    private static void gaSleutel(ServerPlayer p) {
        wisEigen(p, false);
        Vec3 bank = D2Plek.staan(p, D2Plek.SP, "rustpunt_1");
        Vec3 deur = D2Plek.staan(p, D2Plek.SP, "kroeg_sluip");
        if (bank != null) {
            D2Plek.breng(p, bank, deur == null ? 180f : D2Plek.yaw(bank, deur));
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** Plays a scene at a marker of one of the two builds (a leegte_bouw is never turned). False: it could not start. */
    static boolean speel(ServerPlayer p, Cutscene s, String structuur, String anker, @Nullable Consumer<ServerPlayer> daarna) {
        BlockPos a = D2Plek.blok(p, structuur, anker);
        return a != null && Cutscenes.speel(p, s, a, Rotation.NONE, daarna);
    }

    /** A temporary entity of this slice for one player: never saved over a restart ({@link #opBinnen}). */
    static <T extends Entity> T eigen(T e, ServerPlayer p) {
        e.addTag(TIJDELIJK);
        Zicht.alleenVoor(e, p.getUUID());
        return e;
    }

    private static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Kroeg.stop(p, false);
            Rit.vergeet(p.getUUID());
        }
    }

    /** After dying, the game of the old life stops; the step starts it again where you come back. */
    private static void opRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.isEndConquered()) {
            Kroeg.stop(p, false);
            Rit.weg(p);
        }
    }

    private static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getFrom() == GuheindeFeature.GUHEINDE) {
            Kroeg.stop(p, false);
            Rit.weg(p);
        }
    }

    /** (dev, tests) every bit of this slice's own state of a player that is not the step (and, with scenes, the seen scenes). */
    static void wisEigen(ServerPlayer p, boolean scenes) {
        Kroeg.stop(p, false);
        Rit.vergeet(p.getUUID());
        kroon().vlag(p, Rit.RIJDT, false);
        kroon().vlag(p, SLEUTEL, false);
        if (scenes) {
            for (Cutscene s : Cutscene.alle()) {
                if (s.id().startsWith(D2Scenes.ID)) {
                    Cutscenes.vergeet(p, s.id());
                }
            }
            Verteller.vergeet(p, KAART);
        }
    }
}
