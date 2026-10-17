package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.7), slice sp_d2_weg: part 2 steps 5-8 of {@value SpVerhaal#KROON} on the road from De
 * Ruige Kroeg to Schelpenstad. This class is the direction: what each of the four steps asks, where, and what happens when the
 * player gets there. The scripts of the scenes are {@link D2WegScenes}, the two minigames {@link Jacht} (Dennis-Mika's chase)
 * and {@link Traan} (the tear on the drying table), the walking companions {@link Volgers}.
 * <ol start="5">
 *   <li>{@code K_SNOR}: walk to the west rim of the loopgraaf: wink 9 ({@code knipoog_appa}, once per player), then
 *       {@code snor} (Mindy-guh's knabbelkoordraad moustaches; the reward: the moustache for your guhs).</li>
 *   <li>{@code K_LOOPGRAAF}: down the zigzag path; at the bottom {@code monsters} (they roar... and sing); variant {@code 6_b}
 *       = climb up on the east side. Done on the east rim.</li>
 *   <li>{@code K_DENNIS}: on the east rim {@code dennis_snor} (THE joke), then the chase ({@code 7_b}) along {@code dennis_route}
 *       to the town gate, where {@code laars} flattens Dennis.</li>
 *   <li>{@code K_CYCLOOP}: into the souvenir shop: {@code cycloop}, the tear game ({@code 8_b}), {@code sproeier}. Then the step
 *       moves on to sp_d2_finale (David Hasselguh at the pier).</li>
 * </ol>
 * Everything is per player and lives in the line ({@link SpVerhaal#KROON_LIJN}): a step moves on only in the {@code daarna}
 * of a scene or at the end of a minigame; the slice's own flags ({@value #V_GEZONGEN}, {@value #V_JACHT}, {@value #V_DROOG})
 * are the reserved variants and are cleared when their step is done. A player who logs out, dies or flies off in the middle
 * simply starts that bit again where he comes back; nothing ever costs anything.
 */
public final class E2SpD2Weg {
    public static final String S = SpVerhaal.SCHELPENSTAD;
    public static final String KROON = SpVerhaal.KROON;
    public static final int SNOR = SpVerhaal.K_SNOR, LOOPGRAAF = SpVerhaal.K_LOOPGRAAF, DENNIS = SpVerhaal.K_DENNIS, CYCLOOP = SpVerhaal.K_CYCLOOP;
    /** The slice's own flags of the line (CONTRACT_E2_C 2.4): the monsters sang (6_b), the chase is on (7_b), dried on the table (8_b). */
    public static final String V_GEZONGEN = "sp_d2_weg_gezongen", V_JACHT = "sp_d2_weg_jacht", V_DROOG = "sp_d2_weg_droog";
    /** Text prefixes. */
    public static final String G = "gui.guhs.einde2_sp_d2_weg.";
    /** Tag of every entity this slice spawns for one player (the companions, Dennis, the tear, the seat): never kept over a restart. */
    public static final String TIJDELIJK = "guhs_einde2_sp_d2_weg_tijdelijk";
    /** The clothes source of this slice. */
    public static final String BRON = "einde2_sp_d2_weg";
    /** The point of {@code dennis_route} under the town gate of Schelpenstad (template (166, 14, 42): the chase's finish). */
    public static final int POORT_PUNT = 9;

    private E2SpD2Weg() {
    }

    static Verhaallijn lijn() {
        return SpVerhaal.KROON_LIJN;
    }

    // =====================================================================================================================
    // register (common, both sides)
    // =====================================================================================================================

    static void register() {
        D2WegScenes.registreer();
        for (int s = SNOR; s <= CYCLOOP; s++) {
            SpStappen.echt(KROON, s);
            SpStappen.doel(KROON, s, (p, stap) -> doel(p, stap));
        }
        SpStappen.sleutel(KROON, LOOPGRAAF, (p, s) -> lijn().vlag(p, V_GEZONGEN) ? "6_b" : "6");
        SpStappen.sleutel(KROON, DENNIS, (p, s) -> lijn().vlag(p, V_JACHT) ? "7_b" : "7");
        SpStappen.sleutel(KROON, CYCLOOP, (p, s) -> lijn().vlag(p, V_DROOG) ? "8_b" : "8");
        SpStappen.ga(KROON, SNOR, p -> breng(p, "rustpunt_2", -90f));
        SpStappen.ga(KROON, LOOPGRAAF, p -> brengNaar(p, padPunt(p, "pad_omlaag", -1), 200f));
        SpStappen.ga(KROON, DENNIS, p -> breng(p, "loopgraaf_rand_oost", -90f));
        SpStappen.ga(KROON, CYCLOOP, p -> breng(p, "rustpunt_3", 180f));
        hints();
        lijn().opStap(E2SpD2Weg::opStap);
        SponsguhWezen.BIJ_KLIK.add(E2SpD2Weg::klik);
        NeoForge.EVENT_BUS.addListener(E2SpD2Weg::opTick);
        NeoForge.EVENT_BUS.addListener(E2SpD2Weg::opWeg);
        NeoForge.EVENT_BUS.addListener(E2SpD2Weg::opRespawn);
        NeoForge.EVENT_BUS.addListener(E2SpD2Weg::opDimensie);
        NeoForge.EVENT_BUS.addListener(E2SpD2Weg::opBinnen);
        NeoForge.EVENT_BUS.addListener(E2SpD2WegCommando::register);
    }

    // =====================================================================================================================
    // hints (StapHint: what, where, a look scene, a column of light, where "Verder met het verhaal" puts you)
    // =====================================================================================================================

    static Cutscene kijkRand, kijkPad, kijkWinkel;

    private static void hints() {
        kijkRand = StapHint.kijkScene(D2WegScenes.ID + "kijk_rand", true);
        kijkPad = StapHint.kijkScene(D2WegScenes.ID + "kijk_pad", true);
        kijkWinkel = StapHint.kijkScene(D2WegScenes.ID + "kijk_winkel", true);
        // 5: from De Ruige Kroeg east along the road to the rim (Mindy-guh waits there)
        StapHint.naar(S, "snor_plek").tekst(G + "hint.rand").kijk(kijkRand).bijStart().baken().terug(S, "rustpunt_2").zet(KROON, SNOR);
        // 6: the zigzag path down; once the monsters sang, the path up on the east wall (the step's own texts 6 / 6_b)
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> lijn().vlag(p, V_GEZONGEN) ? marker(level, "loopgraaf_rand_oost")
                        : padBlok(level, "pad_omlaag", 0)))
                .kijk(kijkPad).bijStart().baken()
                .terug(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> lijn().vlag(p, V_GEZONGEN) ? padBlok(level, "pad_omhoog", 0)
                        : marker(level, "rustpunt_2")))
                .zet(KROON, LOOPGRAAF);
        // 7: the east rim (Dennis comes at once); in the chase the town gate. No look scene: it would stop the chase.
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> lijn().vlag(p, V_JACHT) ? padBlok(level, "dennis_route", POORT_PUNT)
                        : marker(level, "loopgraaf_rand_oost")))
                .zonderKijk().bijStart().baken().terug(S, "loopgraaf_rand_oost").zet(KROON, DENNIS);
        // 8: the souvenir shop on the square; dried, the drying table at the back
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> marker(level, lijn().vlag(p, V_DROOG) ? "droogtafel_speler" : "rustpunt_3")))
                .kijk(kijkWinkel).bijStart().baken().terug(S, "rustpunt_3").zet(KROON, CYCLOOP);
    }

    /** The compass target and objective line of a step: the same spot as its hint. */
    @Nullable
    private static Doel doel(ServerPlayer p, int stap) {
        ServerLevel g = p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (g == null || !Plekken.bekend(g, S)) {
            return Doel.structuur(GuheindeFeature.GUHEINDE, S, Component.translatable("structure.guhs." + S));
        }
        StapHint.Hint h = StapHint.hint(KROON, stap);
        BlockPos b = h == null ? null : h.doel().blok(p);
        if (b == null) {
            return Doel.structuur(GuheindeFeature.GUHEINDE, S, Component.translatable("structure.guhs." + S));
        }
        return Doel.plek(GuheindeFeature.GUHEINDE, b, Component.translatable("gui.guhs.verhalen." + KROON + ".waar." + lijn().sleutel(p)));
    }

    // =====================================================================================================================
    // the step listener
    // =====================================================================================================================

    private static void opStap(ServerPlayer p, int oud, int nieuw) {
        if (nieuw < SNOR || nieuw > CYCLOOP || lijn().klaar(p)) {
            Volgers.weg(p);
            Jacht.stop(p);
            Traan.stop(p, false);
        }
        if (nieuw == SNOR && oud < SNOR) {
            // (travel banter: the reason of the step is d2_heen's sleutel_terug; the walk itself in two lines)
            StapHint.later(p, 50, q -> {
                if (lijn().stap(q) == SNOR && !lijn().klaar(q)) {
                    praat(q, "sponsguh", "banter.lopen");
                    StapHint.later(q, 50, r -> praat(r, "patguh", "banter.benen"));
                }
            });
        }
        Zicht.kijkOpnieuw(p);
    }

    // =====================================================================================================================
    // the direction: every few ticks, what the step of this player asks for where he stands
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.level().getServer() == null) {
            return;
        }
        Jacht.tick(p);
        Traan.tick(p);
        if ((p.level().getServer().getTickCount() + p.getId()) % 10 == 0) {
            regie(p);
        }
    }

    /** Is the player busy with something of this slice (or any scene)? */
    static boolean bezig(ServerPlayer p) {
        return Cutscenes.bezig(p) || Jacht.actief(p) || Traan.actief(p);
    }

    /** What the player's step asks for, where he stands now. Public for the dev command and the tests. */
    public static void regie(ServerPlayer p) {
        Verhaallijn l = lijn();
        int stap = l.stap(p);
        boolean mijn = l.begonnen(p) && !l.klaar(p) && stap >= SNOR && stap <= CYCLOOP;
        ServerLevel g = guheinde(p);
        if (!mijn || g == null || !Plekken.bekend(g, S) || !p.isAlive()) {
            Volgers.weg(p);
            return;
        }
        Volgers.regie(p, g, stap);
        if (bezig(p)) {
            return;
        }
        switch (stap) {
            case 5 -> stapSnor(p);
            case 6 -> stapLoopgraaf(p);
            case 7 -> stapDennis(p);
            case 8 -> stapCycloop(p);
            default -> {
            }
        }
    }

    // --- step 5: the west rim ------------------------------------------------------------------------------------------------

    private static void stapSnor(ServerPlayer p) {
        if (!bij(p, "snor_plek", 6) && !bij(p, "loopgraaf_rand_west", 4) && !bij(p, "knipoog_appa_sponsguh", 4)) {
            return;
        }
        if (!Cutscenes.gezien(p, D2WegScenes.KNIPOOG_APPA.id())) {
            // wink 9 first (once per player), then the moustaches
            speel(p, D2WegScenes.KNIPOOG_APPA, "knipoog_appa_sponsguh", E2SpD2Weg::snor);
        } else {
            snor(p);
        }
    }

    private static void snor(ServerPlayer p) {
        if (lijn().stap(p) != SNOR) {
            return;
        }
        speel(p, D2WegScenes.SNOR, "snor_plek", E2SpD2Weg::naSnor);
    }

    private static void naSnor(ServerPlayer p) {
        if (!lijn().verder(p, SNOR)) {
            return;
        }
        SpAudio.stinger(p, "stinger.snor");
        if (KledingUnlocks.ontgrendel(p, GuhClothes.EINDE2_SP_D2_WEG_SNOR)) {
            p.sendSystemMessage(Component.translatable(G + "beloning.snor").withStyle(ChatFormatting.GOLD));
        }
    }

    // --- step 6: the loopgraaf -----------------------------------------------------------------------------------------------

    private static void stapLoopgraaf(ServerPlayer p) {
        boolean gezongen = lijn().vlag(p, V_GEZONGEN);
        if (!gezongen && opDeBodem(p)) {
            monsters(p, false);
        } else if (bij(p, "loopgraaf_rand_oost", 4)) {
            if (gezongen) {
                naLoopgraaf(p);
            } else {
                monsters(p, true);                                  // (up without passing the bottom: the song plays anyway)
            }
        }
    }

    /** At the bottom of the loopgraaf (the zone, and at most two blocks above its floor: not halfway down the path). */
    static boolean opDeBodem(ServerPlayer p) {
        ServerLevel g = guheinde(p);
        Plekken.Plek z = Plekken.plek(S, "loopgraaf_bodem");
        return g != null && z != null && in(p, "loopgraaf_bodem", 1) && p.getY() <= Plekken.oorsprong(g, S).getY() + z.pos().getY() + 2.5;
    }

    private static void monsters(ServerPlayer p, boolean boven) {
        SpAudio.moment(p, "monsters");
        boolean ok = speel(p, D2WegScenes.MONSTERS, D2WegScenes.MONSTERS_ANKER, q -> {
            SpAudio.stopMoment(q, 40);
            if (lijn().stap(q) == LOOPGRAAF) {
                lijn().vlag(q, V_GEZONGEN, true);
                if (boven) {
                    naLoopgraaf(q);
                } else {
                    balk(q, "monsters.omhoog");
                    StapHint.toon(q, KROON, false);
                }
            }
        });
        if (!ok) {
            SpAudio.stopMoment(p, 0);
        }
    }

    private static void naLoopgraaf(ServerPlayer p) {
        if (lijn().verder(p, LOOPGRAAF)) {
            lijn().vlag(p, V_GEZONGEN, false);
        }
    }

    // --- step 7: Dennis-Mika -------------------------------------------------------------------------------------------------

    private static void stapDennis(ServerPlayer p) {
        if (!lijn().vlag(p, V_JACHT)) {
            if (bij(p, "loopgraaf_rand_oost", 9) || bij(p, "dennis", 9)) {
                speel(p, D2WegScenes.DENNIS_SNOR, "loopgraaf_rand_oost", E2SpD2Weg::naDennisSnor);
            }
            return;
        }
        // the chase was on (logged out, died, ran off): it goes on when the player is back on the road
        Jacht.hervat(p);
    }

    private static void naDennisSnor(ServerPlayer p) {
        if (lijn().stap(p) != DENNIS) {
            return;
        }
        lijn().vlag(p, V_JACHT, true);
        Jacht.start(p, true);
    }

    /** The chase reached the town gate: the lead boot, then the next step. */
    static void naPoort(ServerPlayer p) {
        naPoort(p, 0);
    }

    private static void naPoort(ServerPlayer p, int poging) {
        if (lijn().stap(p) != DENNIS) {
            return;
        }
        SpAudio.stopMoment(p, 20);
        if (!speel(p, D2WegScenes.LAARS, D2WegScenes.LAARS_ANKER, E2SpD2Weg::naLaars)) {
            // (another scene still on the screen: try again in a second; after half a minute go on without the boot)
            if (poging < 30) {
                StapHint.later(p, 20, q -> naPoort(q, poging + 1));
            } else {
                naLaars(p);
            }
        }
    }

    private static void naLaars(ServerPlayer p) {
        if (lijn().verder(p, DENNIS)) {
            lijn().vlag(p, V_JACHT, false);
        }
    }

    // --- step 8: the souvenir shop -------------------------------------------------------------------------------------------

    private static void stapCycloop(ServerPlayer p) {
        if (!lijn().vlag(p, V_DROOG)) {
            if (in(p, "souvenirwinkel", 0) || bij(p, "rustpunt_3", 2)) {
                SpAudio.moment(p, "cycloop");
                boolean ok = speel(p, D2WegScenes.CYCLOOP, "droogtafel", q -> {
                    if (lijn().stap(q) == CYCLOOP) {
                        lijn().vlag(q, V_DROOG, true);
                        Traan.start(q, true);
                    }
                });
                if (!ok) {
                    SpAudio.stopMoment(p, 0);
                }
            }
            return;
        }
        Traan.hervat(p);
    }

    /** The tear reached the socket: the sprinklers, the crown, then sp_d2_finale's step. */
    static void naTraan(ServerPlayer p) {
        naTraan(p, 0);
    }

    private static void naTraan(ServerPlayer p, int poging) {
        if (lijn().stap(p) != CYCLOOP) {
            return;
        }
        SpAudio.stopMoment(p, 20);
        Traan.wacht(p, 40);                                          // (the game must not start again while the scene waits)
        if (!speel(p, D2WegScenes.SPROEIER, "droogtafel", E2SpD2Weg::naSproeier)) {
            if (poging < 30) {
                StapHint.later(p, 20, q -> naTraan(q, poging + 1));
            } else {
                naSproeier(p);
            }
        }
    }

    private static void naSproeier(ServerPlayer p) {
        SpAudio.stinger(p, "stinger.opgelost");
        if (lijn().verder(p, CYCLOOP)) {
            lijn().vlag(p, V_DROOG, false);
            Volgers.weg(p);
        }
    }

    // =====================================================================================================================
    // clicks: only for this slice's own companions and characters while the clicking player's step is one of ours (4.3)
    // =====================================================================================================================

    private static boolean klik(SponsguhWezen w, ServerPlayer p) {
        Verhaallijn l = lijn();
        int stap = l.stap(p);
        if (l.klaar(p) || !l.begonnen(p) || stap < SNOR || stap > CYCLOOP) {
            return false;
        }
        String soort = w.cast().id();
        if (w.entityTags().contains(Volgers.TAG) && p.getUUID().equals(Zicht.eigenaar(w))) {
            String wie = soort.equals("patguh") ? "patguh" : "sponsguh";
            GuhQuests.say(p, w, G + "klik." + wie + "." + l.sleutel(p) + "." + p.getRandom().nextInt(2));
            w.zetAnimatie("praat");
            return true;
        }
        if (soort.equals("loopgraafmonster") && stap >= LOOPGRAAF) {
            GuhQuests.say(p, w, G + "klik.monster." + p.getRandom().nextInt(3));
            w.zetAnimatie("zing");
            return true;
        }
        if (soort.equals("cycloop_mika") && stap == CYCLOOP && !l.vlag(p, V_DROOG)) {
            GuhQuests.say(p, w, G + "klik.cycloop");
            return true;
        }
        return false;
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    private static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            stopAlles(p);
        }
    }

    private static void opRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.isEndConquered()) {
            stopAlles(p);
        }
    }

    private static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getFrom() == GuheindeFeature.GUHEINDE) {
            stopAlles(p);
        }
    }

    /** Stops every game of this slice and takes the companions away (nothing is lost: the step simply starts it again). */
    static void stopAlles(ServerPlayer p) {
        Jacht.stop(p);
        Traan.stop(p, false);
        Volgers.weg(p);
    }

    /** A temporary entity that was saved by mistake (a crash in the middle): gone when it is loaded again. */
    private static void opBinnen(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.loadedFromDisk() && event.getEntity().entityTags().contains(TIJDELIJK)) {
            event.setCanceled(true);
        }
    }

    /** (dev, tests) every bit of this slice's own state of a player that is not the step itself. */
    static void wisEigen(ServerPlayer p) {
        stopAlles(p);
        for (String v : List.of(V_GEZONGEN, V_JACHT, V_DROOG)) {
            lijn().vlag(p, v, false);
        }
        List<String> scenes = new ArrayList<>();
        for (Cutscene s : Cutscene.alle()) {
            if (s.id().startsWith(D2WegScenes.ID)) {
                scenes.add(s.id());
            }
        }
        scenes.forEach(id -> Cutscenes.vergeet(p, id));
        StapHint.vergeet(p.getUUID());
    }

    // =====================================================================================================================
    // places (Schelpenstad is a leegte_bouw: never turned, world = origin + template)
    // =====================================================================================================================

    @Nullable
    static ServerLevel guheinde(ServerPlayer p) {
        return p.level().dimension() == GuheindeFeature.GUHEINDE ? p.level() : null;
    }

    @Nullable
    static BlockPos marker(ServerLevel level, String id) {
        return Plekken.bekend(level, S) ? Plekken.wereld(level, S, id) : null;
    }

    @Nullable
    static BlockPos blok(ServerPlayer p, String id) {
        ServerLevel g = guheinde(p);
        return g == null ? null : marker(g, id);
    }

    /** The world block of a template spot of Schelpenstad. */
    @Nullable
    static BlockPos sjabloon(ServerLevel level, BlockPos t) {
        return Plekken.bekend(level, S) ? Plekken.oorsprong(level, S).offset(t) : null;
    }

    /** Point {@code i} of a pad marker (negative: from the end), in the world. */
    @Nullable
    static BlockPos padBlok(ServerLevel level, String pad, int i) {
        Plekken.Plek pl = Plekken.plek(S, pad);
        if (pl == null || pl.punten().isEmpty() || !Plekken.bekend(level, S)) {
            return null;
        }
        int n = pl.punten().size();
        return sjabloon(level, pl.punten().get(Math.floorMod(i, n)));
    }

    @Nullable
    static Vec3 padPunt(ServerPlayer p, String pad, int i) {
        ServerLevel g = guheinde(p) != null ? guheinde(p) : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        BlockPos b = g == null ? null : padBlok(g, pad, i);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** Every point of a pad marker in the world (empty: not known). */
    static List<Vec3> pad(ServerLevel level, String pad) {
        Plekken.Plek pl = Plekken.plek(S, pad);
        List<Vec3> uit = new ArrayList<>();
        if (pl == null || !Plekken.bekend(level, S)) {
            return uit;
        }
        BlockPos o = Plekken.oorsprong(level, S);
        for (BlockPos t : pl.punten()) {
            uit.add(Vec3.atBottomCenterOf(o.offset(t)));
        }
        return uit;
    }

    /** Within r blocks across (and 4 up or down) of a marker. */
    static boolean bij(ServerPlayer p, String id, double r) {
        BlockPos b = blok(p, id);
        if (b == null) {
            return false;
        }
        double dx = p.getX() - (b.getX() + 0.5), dz = p.getZ() - (b.getZ() + 0.5), dy = p.getY() - b.getY();
        return dx * dx + dz * dz <= r * r && Math.abs(dy) <= 4;
    }

    /** Inside a zone marker (grown by {@code rand} blocks). */
    static boolean in(ServerPlayer p, String zone, int rand) {
        ServerLevel g = guheinde(p);
        Plekken.Plek z = Plekken.plek(S, zone);
        if (g == null || z == null || z.tot() == null || !Plekken.bekend(g, S)) {
            return false;
        }
        BlockPos o = Plekken.oorsprong(g, S);
        AABB box = new AABB(Vec3.atLowerCornerOf(o.offset(z.pos())), Vec3.atLowerCornerOf(o.offset(z.tot())).add(1, 1, 1)).inflate(rand);
        return box.contains(p.position());
    }

    /** Is the player inside the box of Schelpenstad (the road, the Kroeg, the loopgraaf, the town)? */
    static boolean inSchelpenstad(ServerPlayer p, ServerLevel g) {
        return Plekken.bekend(g, S) && Plekken.doos(g, S).isInside(p.blockPosition());
    }

    /** Plays a scene at a marker (or a named anchor of {@link D2WegScenes}). False: could not start. */
    static boolean speel(ServerPlayer p, Cutscene s, String anker, @Nullable Consumer<ServerPlayer> daarna) {
        ServerLevel g = guheinde(p);
        BlockPos a = g == null ? null : D2WegScenes.anker(g, anker);
        return a != null && Cutscenes.speel(p, s, a, Rotation.NONE, daarna);
    }

    /** Puts the player on a marker, facing yaw. */
    static void breng(ServerPlayer p, String marker, float yaw) {
        ServerLevel g = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        BlockPos b = g == null ? null : marker(g, marker);
        if (b != null) {
            brengNaar(p, Vec3.atBottomCenterOf(b), yaw);
        }
    }

    static void brengNaar(ServerPlayer p, @Nullable Vec3 v, float yaw) {
        ServerLevel g = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (v == null || g == null) {
            return;
        }
        p.stopRiding();
        p.teleportTo(g, v.x, v.y + 0.05, v.z, java.util.Set.of(), yaw, 0f, true);
        p.resetFallDistance();
    }

    /** A companion's line in the chat (only for this player): "&lt;Sponsguh&gt; ...". */
    static void praat(ServerPlayer p, String wie, String key, Object... args) {
        Entity e = Volgers.van(p, wie);
        Component naam = Component.translatable(G + "naam." + wie);
        if (e != null) {
            GuhQuests.say(p, e, G + key, args);
        } else {
            p.sendSystemMessage(Component.literal("<").append(naam).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable(G + key, args).withStyle(ChatFormatting.WHITE)));
        }
    }

    /** A gold line of the story in the chat. */
    static void zeg(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(G + key, args).withStyle(ChatFormatting.GOLD));
    }

    /** A short line above the hotbar. */
    static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(G + key, args).withStyle(ChatFormatting.GOLD));
    }

    /** A big title in the middle of the screen (a start or a win), with a subtitle. */
    static void titel(ServerPlayer p, String key, @Nullable String sub, Object... args) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(8, 60, 14));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(sub == null ? Component.empty()
                : Component.translatable(G + sub, args).withStyle(ChatFormatting.YELLOW)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable(G + key).withStyle(ChatFormatting.GOLD)));
    }

    /** A temporary entity of this slice for one player: never saved over a restart ({@link #opBinnen}). */
    static <T extends Entity> T eigen(T e, ServerPlayer p) {
        e.addTag(TIJDELIJK);
        Zicht.alleenVoor(e, p.getUUID());
        return e;
    }

    /** The ground at a spot: the first free block over a solid one (searched 3 up and 5 down), on top of a slab or a step. */
    static Vec3 grond(ServerLevel level, Vec3 v) {
        BlockPos b = BlockPos.containing(v.x, v.y + 0.2, v.z);
        for (int i = 0; i < 4 && !vrij(level, b); i++) {
            b = b.above();
        }
        for (int i = 0; i < 5 && hoogte(level, b.below()) <= 0; i++) {
            b = b.below();
        }
        double eigen = hoogte(level, b);
        if (eigen > 0) {
            return new Vec3(v.x, b.getY() + eigen, v.z);                 // (standing on a slab in this block)
        }
        double onder = hoogte(level, b.below());
        return new Vec3(v.x, onder > 0 && onder < 1 ? b.getY() - 1 + onder : b.getY(), v.z);
    }

    /** The top of a block's collision shape (0: nothing to stand on). */
    private static double hoogte(ServerLevel level, BlockPos b) {
        net.minecraft.world.phys.shapes.VoxelShape vorm = level.getBlockState(b).getCollisionShape(level, b);
        return vorm.isEmpty() ? 0 : vorm.max(net.minecraft.core.Direction.Axis.Y);
    }

    /** A block one can stand in: no collision, or at most a half block (a slab, a carpet). */
    private static boolean vrij(ServerLevel level, BlockPos b) {
        return hoogte(level, b) <= 0.5;
    }
}
