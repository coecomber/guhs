package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.Karen;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhBlokken;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingOntgrendel;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 2.2, 7.5), slice sp_d1_recept: part 1 steps 7-10 of {@code sponsguh_recept} ("Het geheime
 * recept"). The skelet's line holds all progress; this class takes over its four steps ({@link SpStappen#echt}) and plays them:
 * <ul>
 *   <li><b>7 Plan Z</b>: Krabguh's alarm bell (d1_stad's last scene ends with it; the bell rings again when the step starts):
 *       walk into his office at the back of De Krokante Knabbel ({@code kantoor}) and the big scene {@code plan_z} plays (the
 *       empty safe, the flashback to Plankmika's Plan Z, "Bedankt Karen, mijn computervrouw."). Then step 8;</li>
 *   <li><b>8 De Vetemmer</b>: at the bucket's door Sponsguh explains the sneak ({@code sluip_uitleg}); stepping onto the
 *       sneak route plays wink 6 ({@code knipoog_kloon}) once, then the sneak ({@link Sluipen}); at the safe {@code recept};
 *       then variant {@code 8_b} (the recipe in your pocket, flag {@value #V_RECEPT}): Karen switched the security off for "the
 *       customer", Plankmika squeaks after you, run out. Out of the bucket = step 9;</li>
 *   <li><b>9 Het recept terug</b>: into the office again (or click Krabguh) and {@code feest} plays (the party tonight,
 *       Octoliam's challenge, Octo has no band). From here the skelet's recipe hook says yes (the grill bakes for you);</li>
 *   <li><b>10 De Bellenkom</b>: your band waits on the stage (your own copies: {@link D1rActeurs}); stand behind the drum and
 *       the drum game starts ({@link Trommel}, variant {@code 10_b} while it runs); won = {@code zoete_zege} (the song) and part
 *       1 is done: the rewards ({@link #beloon}).</li>
 * </ul>
 * Every step has a hint ({@link StapHint}: what, where, a look scene, a light column, the resume spot). Everything is per player;
 * a minigame stops when the player leaves or dies and starts again when they come back (it lives in the line's step and flags,
 * never only in memory). Clicks on the cast while the player is at one of these steps give a line about that step
 * ({@link #klik}). Texts: tools/features/einde2_sp_d1_recept.py (chunk c114).
 */
public final class E2SpD1Recept {
    public static final String S = SpVerhaal.BIKINIBODEM;
    public static final String LIJN = SpVerhaal.RECEPT;
    /** Lang prefixes: gui texts (titles, action bar, hints) and chat lines. */
    public static final String P = "gui.guhs.einde2_sp_d1_recept.", Q = "quest.guhs.einde2_sp_d1_recept.";
    public static final int PLAN_Z = SpVerhaal.R_PLAN_Z, VETEMMER = SpVerhaal.R_VETEMMER, TERUG = SpVerhaal.R_TERUG, BELLENKOM = SpVerhaal.R_BELLENKOM;
    public static final List<Integer> STAPPEN = List.of(PLAN_Z, VETEMMER, TERUG, BELLENKOM);
    /** Flags of the line (CONTRACT_E2_C 2.4: {@code <slice>_<naam>}, cleared when the step is done). */
    public static final String V_UITLEG = "sp_d1_recept_uitleg", V_KLOON = "sp_d1_recept_kloon", V_RECEPT = "sp_d1_recept_recept",
            V_GEWONNEN = "sp_d1_recept_gewonnen";
    /** Once-only (the line's {@code eenmalig}): the part-1 rewards. */
    public static final String E_BELONING = "sp_d1_recept_beloning";
    /** How many Knabbelburgers part 1 gives. */
    public static final int BURGERS = 8;
    /** The drum game starts within this many blocks of your spot behind the drum, and stops beyond that many. */
    static final double TROMMEL_START = 1.6, TROMMEL_WEG = 7.0;

    /** (tests) where the guaranteed copy of Bikinibodem stands (template 0, 0, 0 in the world). */
    static volatile Function<ServerLevel, BlockPos> oorsprong = g -> g.dimension() == GuheindeFeature.GUHEINDE ? Plekken.oorsprong(g, S) : null;

    /** player -> until when (server tick) Karen shows that face to them (her screen in the lab, CONTRACT_E2_C 7.5). */
    private static final Map<UUID, long[]> KAREN = new ConcurrentHashMap<>();
    /** player -> the last Plankmika squeak in 8_b (server tick). */
    private static final Map<UUID, Long> PIEP = new ConcurrentHashMap<>();
    /** player -> not before this server tick the song starts (the title of the won drum game is read first). */
    private static final Map<UUID, Long> ZEGE_NA = new ConcurrentHashMap<>();

    private E2SpD1Recept() {
    }

    static Verhaallijn lijn() {
        return SpVerhaal.RECEPT_LIJN;
    }

    /** The player's step of part 1 when part 1 is on for them, else -1. */
    static int stap(ServerPlayer p) {
        return SpVerhaal.actief(p, LIJN) ? lijn().stap(p) : -1;
    }

    // =====================================================================================================================
    // places
    // =====================================================================================================================

    @Nullable
    static ServerLevel guheinde(ServerPlayer p) {
        return p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
    }

    /** The world block of template 0, 0, 0 (null: not this level, or not known yet). */
    @Nullable
    static BlockPos oorsprong(Level level) {
        return level instanceof ServerLevel g ? oorsprong.apply(g) : null;
    }

    /** The world block of a marker of Bikinibodem. */
    @Nullable
    static BlockPos blok(Level level, String marker) {
        Plekken.Plek p = Plekken.plek(S, marker);
        BlockPos o = p == null ? null : oorsprong(level);
        return o == null ? null : o.offset(p.pos());
    }

    /** The middle of a marker's block, on its floor. */
    @Nullable
    static Vec3 voet(Level level, String marker) {
        BlockPos b = blok(level, marker);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** How far (horizontally; Double.MAX_VALUE when unknown or more than 4 blocks up or down) the player is from a marker. */
    static double afstand(ServerPlayer p, String marker) {
        Vec3 v = voet(p.level(), marker);
        if (v == null || Math.abs(p.getY() - v.y) > 4) {
            return Double.MAX_VALUE;
        }
        return Math.hypot(p.getX() - v.x, p.getZ() - v.z);
    }

    /** Is the player inside a zone marker of Bikinibodem (plus a margin)? */
    static boolean in(ServerPlayer p, String zone, double marge) {
        Plekken.Plek z = Plekken.plek(S, zone);
        BlockPos o = oorsprong(p.level());
        if (z == null || z.tot() == null || o == null) {
            return false;
        }
        double x = p.getX() - o.getX(), y = p.getY() - o.getY(), zz = p.getZ() - o.getZ();
        return x >= z.pos().getX() - marge && x <= z.tot().getX() + 1 + marge && y >= z.pos().getY() - marge - 1 && y <= z.tot().getY() + 1 + marge
                && zz >= z.pos().getZ() - marge && zz <= z.tot().getZ() + 1 + marge;
    }

    /** Puts the player on a marker (dev start of a step), facing yaw; false: not known. */
    static boolean zetOp(ServerPlayer p, String marker, double dx, double dy, double dz, float yaw) {
        ServerLevel g = guheinde(p);
        Vec3 v = g == null ? null : voet(g, marker);
        if (v == null) {
            return false;
        }
        p.teleportTo(g, v.x + dx, v.y + dy + 0.05, v.z + dz, Set.of(), yaw, 0f, true);
        p.resetFallDistance();
        return true;
    }

    // =====================================================================================================================
    // register
    // =====================================================================================================================

    /** From E2SpD1ReceptFeature.register (common code, both sides: the scenes must be known on the client). */
    static void register() {
        D1rScenes.registreer();
        for (int s : STAPPEN) {
            SpStappen.echt(LIJN, s);
        }
        SpStappen.sleutel(LIJN, VETEMMER, (p, s) -> lijn().vlag(p, V_RECEPT) ? "8_b" : "8");
        SpStappen.sleutel(LIJN, BELLENKOM, (p, s) -> Trommel.bezig(p) ? "10_b" : "10");
        SpStappen.doel(LIJN, VETEMMER, E2SpD1Recept::doelVetemmer);
        SpStappen.ga(LIJN, PLAN_Z, p -> ga(p, PLAN_Z));
        SpStappen.ga(LIJN, VETEMMER, p -> ga(p, VETEMMER));
        SpStappen.ga(LIJN, TERUG, p -> ga(p, TERUG));
        SpStappen.ga(LIJN, BELLENKOM, p -> ga(p, BELLENKOM));
        SpStappen.beloningen(LIJN, E2SpD1Recept::beloningen);
        // the hints: what, where (direction + landmark), a look scene, a light column, the resume spot (CONTRACT_E2_C 4.4)
        StapHint.naar(S, "kluis").tekst(P + "hint.7").kijk(D1rScenes.KIJK_KANTOOR).bijStart(50).baken().terug(S, "d1r_kk_deur")
                .zet(LIJN, PLAN_Z);
        StapHint.naar(S, "vetemmer_ingang").tekst(P + "hint.8").kijk(D1rScenes.KIJK_VETEMMER).bijStart().baken().terug(S, "vetemmer_ingang")
                .zet(LIJN, VETEMMER);
        StapHint.naar(S, "krabguh").tekst(P + "hint.9").kijk(D1rScenes.KIJK_TERUG).bijStart().baken().terug(S, "d1r_kk_deur")
                .zet(LIJN, TERUG);
        StapHint.naar(S, "podium_trommel").tekst(P + "hint.10").kijk(D1rScenes.KIJK_PODIUM).bijStart().baken().terug(S, "d1r_trommel_speler")
                .zet(LIJN, BELLENKOM);
        lijn().opStap(E2SpD1Recept::opStap);
        SponsguhWezen.BIJ_KLIK.add(E2SpD1Recept::klik);
        Schijn.registreer(E2SpD1Recept::karenSchijn);
        NeoForge.EVENT_BUS.addListener(E2SpD1Recept::opTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, E2SpD1Recept::opKlikBlok);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, E2SpD1Recept::opSlaBlok);
        NeoForge.EVENT_BUS.addListener(E2SpD1Recept::opUitloggen);
        NeoForge.EVENT_BUS.addListener(E2SpD1Recept::opRespawn);
        NeoForge.EVENT_BUS.addListener(D1rActeurs::opBinnenkomst);
        NeoForge.EVENT_BUS.addListener(D1rActeurs::opUitloggen);
        NeoForge.EVENT_BUS.addListener(D1rCommando::register);
    }

    /** The Guhdex rewards of part 1 (this slice owns the line's last step). */
    static List<nl.juiced.guhs.feature.gids.VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        boolean klaar = lijn().klaar(p);
        return List.of(
                Verhaallijn.beloning("guhs:" + GuhClothes.SPONSGUH_VIERKANTE_BROEK.id(), KledingUnlocks.heeft(p, GuhClothes.SPONSGUH_VIERKANTE_BROEK)),
                Verhaallijn.beloning("guhs:knabbelburger", "gui.guhs.verhalen.sponsguh_recept.beloning.recept", SpVerhaal.kentRecept(p)),
                Verhaallijn.beloning("guhs:knabbelburger", "gui.guhs.verhalen.sponsguh_recept.beloning.burgers", klaar));
    }

    @Nullable
    private static Doel doelVetemmer(ServerPlayer p, int stap) {
        ServerLevel g = guheinde(p);
        boolean uit = lijn().vlag(p, V_RECEPT);
        BlockPos b = g == null ? null : blok(g, uit ? "vetemmer_ingang" : "receptkluis");
        if (b == null) {
            return Doel.structuur(GuheindeFeature.GUHEINDE, S, Component.translatable("structure.guhs." + S));
        }
        return Doel.plek(GuheindeFeature.GUHEINDE, b, Component.translatable("gui.guhs.verhalen." + LIJN + ".waar." + (uit ? "8_b" : "8")));
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** The line moved for this player (any step): a step of ours begins, or ours is over. */
    private static void opStap(ServerPlayer p, int oud, int nieuw) {
        if (oud >= PLAN_Z && oud <= BELLENKOM) {
            Sluipen.stop(p);
            Trommel.stop(p);
            D1rActeurs.wegAlle(p);
            lijn().vlag(p, V_RECEPT, false);
            lijn().vlag(p, V_GEWONNEN, false);
        }
        if (nieuw == PLAN_Z) {
            // Krabguh's alarm bell (d1_stad's examen_klaar ends with it): it rings on, from the restaurant
            BlockPos bel = blok(p.level(), "krabguh");
            if (bel != null) {
                for (int i = 0; i < 3; i++) {
                    StapHint.later(p, 6 + i * 14, s -> Schijn.geluid(s, SoundEvents.BELL_BLOCK, s.blockPosition().offset(
                            Integer.signum(bel.getX() - s.getBlockX()) * 6, 2, Integer.signum(bel.getZ() - s.getBlockZ()) * 6), 1.5f, 1.0f));
                }
            }
        }
    }

    /** Every tick per player: the minigames every tick, the rest every five. */
    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) {
            return;
        }
        int stap = stap(p);
        boolean hier = GuheindeFeature.isGuheinde(p.level()) && oorsprong(p.level()) != null && SpVerhaal.open(p);
        if (!hier || stap < PLAN_Z || stap > BELLENKOM) {
            if (Sluipen.bezig(p) || Trommel.bezig(p) || D1rActeurs.heeft(p)) {
                Sluipen.stop(p);
                Trommel.stop(p);
                D1rActeurs.wegAlle(p);
            }
            return;
        }
        if (Cutscenes.bezig(p)) {
            return;
        }
        if (Sluipen.bezig(p) && Sluipen.tik(p)) {
            receptGevonden(p);
            return;
        }
        if (Trommel.bezig(p)) {
            Trommel.tik(p);
        }
        if ((p.tickCount + p.getId()) % 5 != 0) {
            return;
        }
        switch (stap) {
            case 7 -> {
                if (in(p, "kantoor", 0)) {
                    speelPlanZ(p);
                }
            }
            case 8 -> vetemmer(p);
            case 9 -> {
                if (in(p, "kantoor", 0)) {
                    speelFeest(p);
                }
            }
            case 10 -> bellenkom(p);
            default -> {
            }
        }
    }

    static boolean speelPlanZ(ServerPlayer p) {
        karenGezicht(p, Karen.BLIJ, 20 * 75);
        return D1rScenes.speelMetMuziek(p, D1rScenes.PLAN_Z, "plan_z", s -> {
            karenGezicht(s, Karen.NEUTRAAL, 0);
            lijn().verder(s, PLAN_Z);
        });
    }

    /** Step 8: the explanation at the door, the wink, the sneak, the way out. */
    private static void vetemmer(ServerPlayer p) {
        Verhaallijn l = lijn();
        if (l.vlag(p, V_RECEPT)) {
            // 8_b: the recipe in your pocket: out of the bucket = done
            if (!in(p, "vetemmer", 0.5) && !in(p, "lab", 0.5)) {
                l.vlag(p, V_RECEPT, false);
                p.sendSystemMessage(Component.translatable(Q + "buiten").withStyle(ChatFormatting.GOLD));
                SpAudio.stinger(p, "stinger.opgelost");
                l.verder(p, VETEMMER);
                return;
            }
            long nu = p.level().getServer().getTickCount();
            Long laatst = PIEP.get(p.getUUID());
            if (laatst == null || nu - laatst > 90) {
                PIEP.put(p.getUUID(), nu);
                int n = laatst == null ? 0 : (int) (nu / 90 % 4);
                stem(p, Component.translatable("entity.guhs.plankmika"), "plankmika.piep." + n);
            }
            return;
        }
        if (!l.vlag(p, V_UITLEG) && (afstand(p, "vetemmer_ingang") <= 5 || in(p, "vetemmer", 0))) {
            D1rScenes.speel(p, D1rScenes.SLUIP_UITLEG, s -> {
                lijn().vlag(s, V_UITLEG, true);
                VerhaalSync.sync(s);
            });
            return;
        }
        if (Sluipen.opDeRoute(p)) {
            if (!l.vlag(p, V_KLOON)) {
                l.vlag(p, V_KLOON, true);                        // (once: wink 6 never comes twice, also when it could not start)
                if (D1rScenes.speel(p, D1rScenes.KNIPOOG_KLOON, Sluipen::start)) {
                    return;
                }
            }
            Sluipen.start(p);
        } else if (Sluipen.bezig(p) && !in(p, "lab", 1)) {
            Sluipen.stop(p);                                      // (left the lab: it starts again when you come back)
        }
    }

    /** The safe is reached: the scene, then the way out (8_b). */
    private static void receptGevonden(ServerPlayer p) {
        boolean ok = D1rScenes.speel(p, D1rScenes.RECEPT, s -> {
            lijn().vlag(s, V_RECEPT, true);
            PIEP.remove(s.getUUID());
            VerhaalSync.sync(s);
            titel(s, Component.translatable(P + "weg.titel").withStyle(ChatFormatting.GOLD),
                    Component.translatable(P + "weg.onder").withStyle(ChatFormatting.YELLOW), 60);
            SpAudio.stinger(s, "stinger.opgelost");
        });
        if (!ok) {
            lijn().vlag(p, V_RECEPT, true);                       // (no scene possible now: the recipe is yours all the same)
            VerhaalSync.sync(p);
        }
    }

    static boolean speelFeest(ServerPlayer p) {
        return D1rScenes.speel(p, D1rScenes.FEEST, s -> lijn().verder(s, TERUG));
    }

    /** Step 10: your band on the stage while you are in the Bellenkom; the drum game behind the drum; the song when won. */
    private static void bellenkom(ServerPlayer p) {
        if (!in(p, "bellenkom", 2)) {
            if (Trommel.bezig(p) || D1rActeurs.heeft(p)) {
                Trommel.stop(p);
                D1rActeurs.wegAlle(p);
                VerhaalSync.sync(p);
            }
            return;
        }
        if (lijn().vlag(p, V_GEWONNEN)) {
            if (p.level().getServer().getTickCount() >= ZEGE_NA.getOrDefault(p.getUUID(), 0L)) {
                speelZege(p);
            }
            return;
        }
        zetBand(p);
        double d = afstand(p, "d1r_trommel_speler");
        Vec3 trommel = voet(p.level(), "podium_trommel");
        boolean opPodium = trommel != null && p.getY() > trommel.y - 0.6;
        if (!Trommel.bezig(p) && opPodium && Math.min(d, afstand(p, "podium_trommel") + 0.4) <= TROMMEL_START) {
            Trommel.start(p);
        } else if (Trommel.bezig(p) && (d > TROMMEL_WEG || !opPodium)) {
            Trommel.stop(p);
            p.sendOverlayMessage(Component.translatable(P + "trommel.weg").withStyle(ChatFormatting.YELLOW));
            VerhaalSync.sync(p);
        }
    }

    /** Your own band (only you see it): the six musicians, Octo with his clarinet, Sponsguh at the microphone, Octoliam on his stand. */
    private static void zetBand(ServerPlayer p) {
        ServerLevel level = p.level();
        if (D1rActeurs.van(p, "octo") != null) {
            return;
        }
        for (int i = 1; i <= 6; i++) {
            Vec3 v = voet(level, "band_" + i);
            if (v != null) {
                D1rActeurs.wezen(p, "band_" + i, Cast.BIKINIBODEM_GUH, level, v, 0f, i - 1);
            }
        }
        Vec3 octo = voet(level, "podium_octo"), spons = voet(level, "podium_sponsguh"), liam = voet(level, "octoliam");
        if (octo != null) {
            D1rActeurs.wezen(p, "octo", Cast.OCTO_MIKA, level, octo, 0f, -1);
        }
        if (spons != null) {
            D1rActeurs.wezen(p, "sponsguh", Cast.SPONSGUH, level, spons, 0f, -1);
        }
        if (liam != null) {
            SponsguhWezen w = D1rActeurs.wezen(p, "octoliam", Cast.OCTOLIAM, level, liam, 90f, -1);
            if (w != null) {
                w.zetAnimatie("lach");
            }
        }
    }

    /** The drum game is won (Trommel): the song. */
    static void trommelGewonnen(ServerPlayer p, int hits) {
        lijn().vlag(p, V_GEWONNEN, true);
        titel(p, Component.translatable(P + "trommel.gewonnen").withStyle(ChatFormatting.GOLD),
                Component.translatable(P + "trommel.gewonnen_onder", hits, Trommel.NOTEN).withStyle(ChatFormatting.YELLOW), 50);
        Schijn.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, p.blockPosition(), 0.8f, 1.0f);
        SpAudio.stinger(p, "stinger.opgelost");
        D1rActeurs.speel(p, "", "juich");
        VerhaalSync.sync(p);
        ZEGE_NA.put(p.getUUID(), p.level().getServer().getTickCount() + 40L);
    }

    static boolean speelZege(ServerPlayer p) {
        if (Cutscenes.bezig(p) || stap(p) != BELLENKOM) {
            return false;
        }
        return D1rScenes.speelMetMuziek(p, D1rScenes.ZOETE_ZEGE, "zoete_zege", s -> {
            D1rActeurs.wegAlle(s);
            beloon(s);
            lijn().vlag(s, V_GEWONNEN, false);
            if (lijn().verder(s, BELLENKOM)) {
                s.sendSystemMessage(Component.translatable(Q + "klaar").withStyle(ChatFormatting.GOLD));
            }
        });
    }

    /** The rewards of part 1 (once per player): Sponsguh's square trousers with the tie for your guhs, eight Knabbelburgers. */
    static void beloon(ServerPlayer p) {
        if (!lijn().eenmalig(p, E_BELONING)) {
            return;
        }
        KledingOntgrendel.ontgrendel(p, GuhClothes.SPONSGUH_VIERKANTE_BROEK);
        ItemStack burgers = new ItemStack(E2SponsguhFeature.KNABBELBURGER.get(), BURGERS);
        if (!p.getInventory().add(burgers)) {
            p.drop(burgers, false);
        }
        p.sendSystemMessage(Component.translatable(Q + "beloning", BURGERS).withStyle(ChatFormatting.YELLOW));
    }

    // =====================================================================================================================
    // clicks
    // =====================================================================================================================

    /**
     * {@code SponsguhWezen.BIJ_KLIK} (CONTRACT_E2_C 4.3): only while the clicking player's step of part 1 is one of ours. Krabguh
     * at step 7 / 9 in his office starts the scene; every other character says a line about the step. The characters of
     * sp_zij's side lines (Sandy-guh, Zeemeerman, Mosselguh, the ghost captain, Octo at home) are left alone.
     */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        int stap = stap(p);
        if (stap < PLAN_Z || stap > BELLENKOM || D1rActeurs.alle(p).contains(w)) {
            return false;
        }
        Cast c = w.cast();
        if (c == Cast.SANDY_GUH || c == Cast.ZEEMEERMAN_GUH || c == Cast.MOSSELGUH || c == Cast.SPOOKKAPITEIN
                || c == Cast.OCTO_MIKA && afstand(p, "octo") < 14) {
            return false;
        }
        if (c == Cast.MENEER_KRABGUH && (stap == PLAN_Z || stap == TERUG) && afstand(p, "krabguh") < 10) {
            return stap == PLAN_Z ? speelPlanZ(p) : speelFeest(p);
        }
        String wie = switch (c) {
            case MENEER_KRABGUH -> "krabguh";
            case SPONSGUH -> "sponsguh";
            case PATGUH -> "patguh";
            case OCTO_MIKA -> "octo";
            case PLANKMIKA -> "plankmika";
            case PAREL_GUH -> "parel";
            case GERRIT -> "gerrit";
            default -> "bewoner";
        };
        w.getLookControl().setLookAt(p);
        w.zetAnimatie(c == Cast.GERRIT ? "njeg" : "praat");
        GuhQuests.say(p, w, Q + "klik." + wie + "." + stap);
        return true;
    }

    /** Right-click on the drum while the drum game runs: a beat (cancelled, so the note block keeps its note). */
    private static void opKlikBlok(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && Trommel.bezig(p) && isTrommel(p, event.getPos()) && Trommel.klik(p)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Left-click on the drum while the drum game runs: a beat too (cancelled: nothing breaks). */
    private static void opSlaBlok(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && Trommel.bezig(p) && isTrommel(p, event.getPos())
                && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && Trommel.klik(p)) {
            event.setCanceled(true);
        }
    }

    private static boolean isTrommel(ServerPlayer p, BlockPos pos) {
        BlockPos t = blok(p.level(), "podium_trommel");
        return t != null && (pos.equals(t) || pos.equals(t.below()));
    }

    // =====================================================================================================================
    // Karen, voices, titles
    // =====================================================================================================================

    /** Karen's screen shows this face to this player for so many ticks (per player: {@link Schijn}; 0 = back to her own). */
    static void karenGezicht(ServerPlayer p, int gezicht, int ticks) {
        long nu = p.level().getServer() == null ? 0 : p.level().getServer().getTickCount();
        if (ticks <= 0) {
            KAREN.remove(p.getUUID());
        } else {
            KAREN.put(p.getUUID(), new long[] {gezicht, nu + ticks});
        }
        Schijn.straks(p);
    }

    private static void karenSchijn(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        long[] k = KAREN.get(p.getUUID());
        if (k == null) {
            return;
        }
        if (p.level().getServer().getTickCount() > k[1]) {
            KAREN.remove(p.getUUID());
            return;
        }
        BlockPos pos = blok(p.level(), "karen");
        if (pos == null || !Schijn.dichtbij(p, pos)) {
            return;
        }
        BlockState echt = p.level().getBlockState(pos);
        if (echt.hasProperty(SponsguhBlokken.KarenBlock.GEZICHT)) {
            gewenst.put(pos, echt.setValue(SponsguhBlokken.KarenBlock.GEZICHT, (int) k[0]));
        }
    }

    /** One of Karen's lines in the chat (her name, a beep). */
    static void karenZegt(ServerPlayer p, String key) {
        stem(p, Component.translatable("block.guhs.karen"), key);
    }

    /** A line in the chat from someone who is not standing next to you ("<name> text"). */
    static void stem(ServerPlayer p, Component wie, String key) {
        MutableComponent regel = Component.literal("<").append(wie).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(Q + key).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(regel);
    }

    /** A big title (and an optional subtitle) for this player only: the start and the end of a game. */
    static void titel(ServerPlayer p, Component titel, @Nullable Component onder, int blijf) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(4, blijf, 8));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(onder == null ? Component.empty() : onder));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(titel));
    }

    // =====================================================================================================================
    // dev starts, leaving
    // =====================================================================================================================

    /** {@code /guhs einde2 sponsguh ga sponsguh_recept <stap>}: the step's start (the line is already set). */
    static void ga(ServerPlayer p, int stap) {
        vergeet(p);
        Verhaallijn l = lijn();
        for (String v : List.of(V_UITLEG, V_KLOON, V_RECEPT, V_GEWONNEN)) {
            l.vlag(p, v, false);
        }
        switch (stap) {
            case 7 -> zetOp(p, "d1r_kk_deur", 0, 0, 0, 180f);
            case 8 -> zetOp(p, "vetemmer_ingang", 0, 0, -3, 0f);
            case 9 -> zetOp(p, "vetemmer_ingang", 0, 0, -1, 180f);
            case 10 -> zetOp(p, "podium_trommel", -4, -2, 7, 180f);
            default -> {
            }
        }
        VerhaalSync.sync(p);
        StapHint.toon(p, LIJN);
    }

    /** Forgets every running game and actor of this player (dev, tests, leaving). */
    static void vergeet(ServerPlayer p) {
        Sluipen.stop(p);
        Trommel.stop(p);
        D1rActeurs.wegAlle(p);
        KAREN.remove(p.getUUID());
        PIEP.remove(p.getUUID());
        ZEGE_NA.remove(p.getUUID());
    }

    private static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID u = event.getEntity().getUUID();
        Sluipen.vergeet(u);
        Trommel.vergeet(u);
        KAREN.remove(u);
        PIEP.remove(u);
        ZEGE_NA.remove(u);
    }

    /** Died (it never happens here, but a fall elsewhere): the games stop; they start again when the player is back. */
    private static void opRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            vergeet(p);
        }
    }
}
