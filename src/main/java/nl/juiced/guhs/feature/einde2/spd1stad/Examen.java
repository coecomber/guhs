package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh", slice sp_d1_stad: part 1 step {@value D1Stad#EXAMEN}, the boating exam (CONTRACT_E2_C 7.4, DESIGN 5b deel
 * 1.3, wink 5).
 * <ol>
 *   <li><b>To juf Pufguh</b> (sleutel {@code 6}): she waits at the start line of her course on the west side of town (the
 *       resident). Come within {@value #BIJ_PUFGUH} blocks or click her: the scene {@code pufguh} (exam number thirty-eight, you
 *       are Sponsguh's rij-maatje), the flag {@value D1Stad#V_EXAMEN}.</li>
 *   <li><b>Sail</b> (sleutel {@code 6_b}): your own lesbootje ({@link LesbootEntity}) waits at {@code examen_start}, your own
 *       Sponsguh beside it ({@code Zicht.alleenVoor}; the resident Sponsguh stays at his grill). Click the boat: you take the
 *       helm, Sponsguh hops into the front seat, a title says the rule. The course is {@code examen_route}: the next gate shines
 *       (a column of light, and a smaller one at the gate after it); a counter "Poortje n/12 - Geraakt m/8" stays over the
 *       hotbar. A knock at speed is a bump: Sponsguh screams a line. More than {@value #MAX_GERAAKT} bumps = juf Pufguh sighs and
 *       the boat is back at the start at once (no timer, nothing lost). The first time at the bridge: wink 5
 *       ({@code knipoog_guhdalf}: Guhdalf blocks it, the boat goes round him past the pond) and the boat goes on behind the
 *       bridge.</li>
 *   <li><b>Through the finish</b>: a title and a sound, the scene {@code examen_klaar} (the "voorlopig" pass, Krabguh's alarm bell
 *       in the distance), the line moves on.</li>
 * </ol>
 * Getting out on the way is fine: the boat waits and the counters stay; walk {@value #WEG} blocks away (or die, log out, leave
 * the dimension) and the boat and its Sponsguh go; back at the start they wait again for a new try.
 */
public final class Examen {
    /** How close (blocks) to juf Pufguh starts the scene; how close to a gate counts as through it (horizontally). */
    public static final double BIJ_PUFGUH = 6.5, STRAAL = 3.6;
    /** The gates: route points 1 .. {@value #FINISH} ({@code examen_route}: 0 = the start, 12 = the finish line, 13 = the start). */
    public static final int EERSTE = 1, FINISH = 12, BRUG = 5, NA_BRUG = 6;
    /** More bumps than this and the try starts again. */
    public static final int MAX_GERAAKT = 8;
    /** Further than this from the boat (blocks) and the boat goes; within this of the start a new one waits. */
    public static final double WEG = 64, WACHT = 48;

    /** One try on the course (pure state: the game tests drive it without a world). */
    public static final class Rit {
        int poort = EERSTE;
        int geraakt;

        public int poort() {
            return poort;
        }

        public int geraakt() {
            return geraakt;
        }

        void opnieuw() {
            poort = EERSTE;
            geraakt = 0;
        }
    }

    public enum Uitslag { NIETS, POORT, BRUG, FINISH, OPNIEUW }

    /** The boat is here: through the next gate? (BRUG: just before the bridge; FINISH: through the finish line.) */
    public static Uitslag poort(Rit r, Vec3 boot, List<Vec3> route) {
        if (r.poort > FINISH || r.poort >= route.size()) {
            return Uitslag.NIETS;
        }
        Vec3 doel = route.get(r.poort);
        double dx = boot.x - doel.x, dz = boot.z - doel.z;
        if (dx * dx + dz * dz > STRAAL * STRAAL || Math.abs(boot.y - doel.y) > 4) {
            return Uitslag.NIETS;
        }
        r.poort++;
        return r.poort > FINISH ? Uitslag.FINISH : r.poort == BRUG ? Uitslag.BRUG : Uitslag.POORT;
    }

    /** A bump: counted; more than {@value #MAX_GERAAKT} = a new try from the start (OPNIEUW). */
    public static Uitslag botsing(Rit r) {
        r.geraakt++;
        if (r.geraakt > MAX_GERAAKT) {
            r.opnieuw();
            return Uitslag.OPNIEUW;
        }
        return Uitslag.POORT;
    }

    /** Per player, not saved (the step and its flag are in the line). */
    static final class Spel {
        @Nullable
        UUID boot, sponsguh;
        final Rit rit = new Rit();
        boolean aan, klaar;
        int gil;
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();
    /** (tests) the route instead of the build's. */
    static volatile List<Vec3> PROEF_ROUTE;

    private Examen() {
    }

    static Spel spel(ServerPlayer p) {
        return SPELLEN.computeIfAbsent(p.getUUID(), u -> new Spel());
    }

    static List<Vec3> route(@Nullable ServerLevel g) {
        List<Vec3> t = PROEF_ROUTE;
        return t != null ? t : D1Stad.pad(g, "examen_route");
    }

    // =====================================================================================================================
    // the step
    // =====================================================================================================================

    /** Every other tick for a player at step 6 (E2SpD1StadFeature). */
    static void tick(ServerPlayer p, long nu) {
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        if (g == null || p.level() != g || Cutscenes.bezig(p)) {
            return;
        }
        if (!D1Stad.tweedeHelft(p, D1Stad.EXAMEN)) {
            Vec3 puf = D1Stad.midden(g, "pufguh");
            if (nu % 10 == 0 && puf != null && puf.distanceToSqr(p.position()) < BIJ_PUFGUH * BIJ_PUFGUH && p.isAlive()) {
                begin(p);
            }
            return;
        }
        Spel s = spel(p);
        if (D1Stad.lijn().vlag(p, D1Stad.V_GESLAAGD)) {
            if (!s.klaar) {                                 // (through the finish, but the scene after it was not seen: once more)
                s.klaar = true;
                ruimOp(p, g);
                D1Stad.speel(p, D1StadScenes.EXAMEN_KLAAR, "examen_finish", Examen::afronden);
            }
            return;
        }
        Vec3 start = D1Stad.midden(g, "examen_start");
        if (start == null) {
            return;
        }
        LesbootEntity boot = D1Stad.levend(g, s.boot) instanceof LesbootEntity b ? b : null;
        if (boot == null) {
            if (start.distanceToSqr(p.position()) < WACHT * WACHT && p.isAlive()) {
                zetKlaar(p, g, s, start);
            }
            return;
        }
        if (boot.position().distanceToSqr(p.position()) > WEG * WEG || !p.isAlive()) {
            ruimOp(p, g);
            return;
        }
        boolean aanBoord = p.getVehicle() == boot;
        if (aanBoord && !s.aan) {
            start(p, boot, s);
        }
        if (!s.aan) {
            return;
        }
        Entity spons = D1Stad.levend(g, s.sponsguh);
        if (aanBoord && spons != null && spons.getVehicle() != boot) {
            boot.zetMee(spons);
        }
        if (aanBoord) {
            switch (poort(s.rit, boot.position(), route(g))) {
                case POORT -> D1Stad.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.0f + 0.05f * s.rit.poort);
                case BRUG -> {
                    D1Stad.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.2f);
                    if (!Cutscenes.gezien(p, D1StadScenes.KNIPOOG_GUHDALF.id())) {
                        knipoog(p, g, boot, s);
                        return;
                    }
                }
                case FINISH -> {
                    finish(p, g, s);
                    return;
                }
                default -> {
                }
            }
        }
        if (nu % 10 == 0) {
            licht(p, g, s.rit);
        }
        if (nu % 20 == 0) {
            if (aanBoord) {
                D1Stad.balk(p, "examen.teller", Math.min(FINISH, s.rit.poort - 1), FINISH, s.rit.geraakt, MAX_GERAAKT);
            } else {
                D1Stad.balk(p, "examen.instappen");
            }
        }
    }

    /** The scene {@code pufguh}, then the boat waits. */
    static void begin(ServerPlayer p) {
        if (!D1Stad.aanZet(p, D1Stad.EXAMEN) || D1Stad.tweedeHelft(p, D1Stad.EXAMEN) || Cutscenes.bezig(p)) {
            return;
        }
        D1Stad.speel(p, D1StadScenes.PUFGUH, "pufguh", q -> {
            if (!D1Stad.aanZet(q, D1Stad.EXAMEN)) {
                return;
            }
            D1Stad.lijn().vlag(q, D1Stad.V_EXAMEN, true);
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(q);
            D1Stad.hint(q, D1Stad.EXAMEN, D1StadScenes.KIJK_BOOT, "examen_start", "hint.boot", 20);
        });
    }

    /** The boat at the start (facing up the east leg, north) and Sponsguh beside it, for this player. */
    static void zetKlaar(ServerPlayer p, ServerLevel g, Spel s, Vec3 start) {
        LesbootEntity boot = E2SpD1StadFeature.LESBOOT.get().create(g, EntitySpawnReason.TRIGGERED);
        if (boot == null) {
            return;
        }
        boot.snapTo(start.x, start.y, start.z, 180f, 0f);
        boot.zetVoor(p.getUUID());
        D1Stad.eigen(boot, p);
        g.addFreshEntity(boot);
        s.boot = boot.getUUID();
        s.aan = false;
        s.rit.opnieuw();
        D1Stad.weg(g, s.sponsguh);
        SponsguhWezen spons = D1Stad.wezen(g, E2SponsguhFeature.SPONSGUH.get(), start.add(2.4, 0, 0.5), 90f, p);
        s.sponsguh = spons == null ? null : spons.getUUID();
        g.sendParticles(p, ParticleTypes.BUBBLE_POP, true, false, start.x, start.y + 0.6, start.z, 20, 0.7, 0.4, 0.7, 0.02);
    }

    /** Aboard: the title, the rule, Sponsguh in the front seat, the tune. */
    static void start(ServerPlayer p, LesbootEntity boot, Spel s) {
        s.aan = true;
        s.klaar = false;
        s.rit.opnieuw();
        Entity spons = D1Stad.levend(D1Stad.guheinde(p.level().getServer()), s.sponsguh);
        if (spons != null) {
            boot.zetMee(spons);
            D1Stad.zeg(p, spons, "examen.start_sponsguh");
        }
        D1Stad.titel(p, "examen.titel", "examen.regel", MAX_GERAAKT);
        SpAudio.stinger(p, "deuntje.examen");
        D1Stad.geluid(p, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 1.2f);
    }

    /** The next gate shines for this player: a column of light, and a small one at the gate after it. */
    static void licht(ServerPlayer p, ServerLevel g, Rit r) {
        List<Vec3> route = route(g);
        for (int i = 0; i < 2; i++) {
            int n = r.poort + i;
            if (n > FINISH || n >= route.size()) {
                return;
            }
            Vec3 d = route.get(n);
            if (i == 0) {
                g.sendParticles(p, ParticleTypes.END_ROD, true, true, d.x, d.y + 4.0, d.z, 8, 0.05, 2.4, 0.05, 0.0);
                g.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, true, d.x, d.y + 0.6, d.z, 10, 1.4, 0.2, 1.4, 0.0);
            } else {
                g.sendParticles(p, ParticleTypes.END_ROD, true, true, d.x, d.y + 1.5, d.z, 3, 0.05, 0.8, 0.05, 0.0);
            }
        }
    }

    /** A knock of the boat (LesbootEntity.BOTSING): counted, Sponsguh screams; too many and the try starts again. */
    static void bots(LesbootEntity boot) {
        if (boot.voor() == null || !(boot.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(boot.voor());
        if (p == null || !D1Stad.aanZet(p, D1Stad.EXAMEN)) {
            return;
        }
        Spel s = spel(p);
        if (!s.aan || s.klaar) {
            return;
        }
        Entity spons = D1Stad.levend(level, s.sponsguh);
        if (botsing(s.rit) == Uitslag.OPNIEUW) {
            opnieuw(p, level, boot, s);
            return;
        }
        s.gil = (s.gil % 6) + 1;
        if (spons != null) {
            D1Stad.zeg(p, spons, "examen.gil." + s.gil);
            if (spons instanceof SponsguhWezen w) {
                w.zetAnimatie("schrik");
            }
        }
        SpAudio.stinger(p, "stinger.mislukt");
        D1Stad.balk(p, "examen.geraakt", s.rit.geraakt, MAX_GERAAKT);
    }

    /** More than eight bumps: juf Pufguh sighs, the boat is back at the start at once. */
    static void opnieuw(ServerPlayer p, ServerLevel g, LesbootEntity boot, Spel s) {
        Vec3 start = D1Stad.midden(g, "examen_start");
        D1Stad.titel(p, "examen.mislukt", "examen.opnieuw");
        D1Stad.zegAls(p, "entity.guhs.juf_pufguh", "examen.zucht");
        SpAudio.stinger(p, "stinger.mislukt");
        s.rit.opnieuw();
        if (start != null) {
            boot.stop();
            boot.setYRot(180f);
            boot.teleportTo(start.x, start.y, start.z);
        }
    }

    /** Wink 5 at the bridge (the first time): out of the boat, the scene, then on behind the bridge. */
    static void knipoog(ServerPlayer p, ServerLevel g, LesbootEntity boot, Spel s) {
        boot.stop();
        Entity spons = D1Stad.levend(g, s.sponsguh);
        p.stopRiding();
        if (spons != null) {
            spons.stopRiding();
        }
        D1Stad.speel(p, D1StadScenes.KNIPOOG_GUHDALF, "knipoog_guhdalf_brug", q -> {
            ServerLevel lv = D1Stad.guheinde(q.level().getServer());
            LesbootEntity b = D1Stad.levend(lv, s.boot) instanceof LesbootEntity l ? l : null;
            List<Vec3> omweg = D1Stad.pad(lv, "knipoog_guhdalf_omweg");
            if (b == null || omweg.size() < 4 || !D1Stad.aanZet(q, D1Stad.EXAMEN)) {
                return;
            }
            Vec3 na = omweg.get(3);
            b.stop();
            b.setYRot(0f);
            b.teleportTo(na.x, na.y, na.z);
            s.rit.poort = Math.max(s.rit.poort, NA_BRUG);
            q.teleportTo(lv, na.x, na.y + 0.6, na.z, Set.of(), 0f, 10f, true);
            q.startRiding(b);
            Entity sp = D1Stad.levend(lv, s.sponsguh);
            if (sp != null) {
                b.zetMee(sp);
            }
        });
    }

    /** Through the finish: title, sound, out of the boat, the scene examen_klaar, then step 7. */
    static void finish(ServerPlayer p, ServerLevel g, Spel s) {
        if (s.klaar) {
            return;
        }
        s.klaar = true;
        s.aan = false;
        D1Stad.lijn().vlag(p, D1Stad.V_GESLAAGD, true);
        D1Stad.titel(p, "examen.geslaagd", "examen.geslaagd_onder", s.rit.geraakt);
        SpAudio.stinger(p, "stinger.opgelost");
        D1Stad.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        p.stopRiding();
        ruimOp(p, g);
        if (D1Stad.PROEF) {
            afronden(p);
        } else {
            nl.juiced.guhs.feature.einde2.verhaal.StapHint.later(p, 30,
                    q -> D1Stad.speel(q, D1StadScenes.EXAMEN_KLAAR, "examen_finish", Examen::afronden));
        }
    }

    /** After the scene: the flag of the step gone, on to Plan Z (d1_recept hears Krabguh's bell). */
    static void afronden(ServerPlayer p) {
        if (!D1Stad.aanZet(p, D1Stad.EXAMEN)) {
            return;
        }
        D1Stad.lijn().vlag(p, D1Stad.V_EXAMEN, false);
        D1Stad.lijn().vlag(p, D1Stad.V_GESLAAGD, false);
        D1Stad.lijn().verder(p, D1Stad.EXAMEN);
    }

    /** The boat and the Sponsguh of this player go (the try is forgotten; the step stays). */
    static void ruimOp(ServerPlayer p, @Nullable ServerLevel g) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        D1Stad.weg(g, s.sponsguh);
        D1Stad.weg(g, s.boot);
        s.sponsguh = null;
        s.boot = null;
        s.aan = false;
        if (!s.klaar) {
            SPELLEN.remove(p.getUUID());
        }
    }

    static void vergeet(UUID u) {
        SPELLEN.remove(u);
    }
}
