package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;
import nl.juiced.guhs.feature.einde2.geestwezens.KohEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase B, slice geest_spel: Koh's riddle round after chapter 2 ({@value GeestLijnen#KOH}, DESIGN 5 "Koh's Grot").
 * Stand at {@code koh_raadsel} and click Koh (after {@code guhvatar_water} is done; during chapter 2 Koh belongs to h2's
 * no-emotion test, which starts by position, never by a click). Each riddle begins with Koh's faces changing in front of
 * you for {@value #STIL_TICKS} ticks: do not move, do not jump. Then the riddle, three answers. A twitch or a wrong
 * answer: Koh tilts his head, and that riddle begins again (never damage, never anything lost). Three riddles: Koh's mask
 * (a cosmetic head item).
 */
public final class KohRaadsels {
    /** How long you must stand perfectly still before Koh asks (ticks). */
    public static final int STIL_TICKS = 70;
    private static final String SLEUTEL = "einde2_geest_spel_koh";
    /** The right answer (option id) of riddles 1..3. */
    static final int[] GOED = {2, 1, 2};

    private record Wacht(long sinds, Vec3 plek) {
    }

    private static final Map<UUID, Wacht> WACHT = new ConcurrentHashMap<>();

    private KohRaadsels() {
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.koh();
    }

    static void register() {
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            if (optie > 0) {
                antwoord(p, optie);
            }
        });
    }

    static boolean mag(ServerPlayer p) {
        return E2Verhaal.klaar(p, E2Verhaal.WATER) && GeestToegang.zoneOpen(p, "koh_grot");
    }

    /** A click on Koh (the resident at {@code koh}). */
    static boolean klikWezen(ServerPlayer p, GeestWezen w) {
        if (!"koh".equals(GeestPlek.markerVan(w)) || !mag(p)) {
            return false;
        }
        if (lijn().klaar(p)) {
            GeestSpel.zeg(p, w, "", "gui.guhs.einde2_geest_spel.koh.na." + p.getRandom().nextInt(3));
            return true;
        }
        if (!GeestPlek.bij(p, "koh_raadsel", 4.5) && GeestPlek.pos("koh_raadsel") != null) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.koh.dichterbij");
            return true;
        }
        if (WACHT.containsKey(p.getUUID())) {
            return true;
        }
        lijn().begin(p);
        if (lijn().stap(p) == 0 && !lijn().vlag(p, "uitleg")) {
            lijn().vlag(p, "uitleg", true);
            GeestSpel.zeg(p, w, "", "gui.guhs.einde2_geest_spel.koh.welkom");
        }
        begin(p, w);
        return true;
    }

    private static void begin(ServerPlayer p, GeestWezen koh) {
        WACHT.put(p.getUUID(), new Wacht(p.level().getGameTime(), p.position()));
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.koh.stil");
        GeestSpel.geluid(p, "geest.koh_kraak", 0.9f, 1.0f);
        if (koh instanceof KohEntity k) {
            k.zetAnimatie("praat");
        }
    }

    /** Every 10 ticks: a twitch while Koh looks at you, or the riddle when you stood still long enough. */
    static void tick(ServerPlayer p) {
        Wacht w = WACHT.get(p.getUUID());
        if (w == null) {
            return;
        }
        boolean beweegt = p.position().distanceToSqr(w.plek()) > 0.35 * 0.35 || !p.onGround() || p.isShiftKeyDown() || p.isSprinting();
        if (beweegt) {
            WACHT.remove(p.getUUID());
            GeestSpel.geluid(p, "geest.koh_kraak", 1.0f, 0.6f);
            p.level().sendParticles(p, ParticleTypes.SQUID_INK, false, false, p.getX(), p.getY() + 1.6, p.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.koh.emotie." + p.getRandom().nextInt(3));
            return;
        }
        long t = p.level().getGameTime() - w.sinds();
        if (t > 0 && t % 20 < 10) {
            GeestSpel.geluid(p, "geest.koh_stilte", 0.4f, 1.0f + (t / 20) * 0.05f);
        }
        // fix round 1: the stillness is no hidden timer: dots above the hotbar fill while you stand still
        int bolletjes = (int) Math.min(5, t * 5 / STIL_TICKS);
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.koh.stil_teller", "●".repeat(bolletjes) + "○".repeat(5 - bolletjes));
        if (t >= STIL_TICKS) {
            WACHT.remove(p.getUUID());
            vraag(p);
        }
    }

    private static void vraag(ServerPlayer p) {
        int r = lijn().stap(p) + 1;
        if (r > GeestLijnen.RAADSELS_N) {
            return;
        }
        String k = "gui.guhs.einde2_geest_spel.koh.raadsel." + r;
        var koh = GeestPlek.bewoner(p.level(), "koh");
        GeestSpel.praat(p, SLEUTEL, List.of(GeestSpel.regel(koh, "entity.guhs.koh", k + ".vraag")),
                new Praat.Optie(1, k + ".a"), new Praat.Optie(2, k + ".b"), new Praat.Optie(3, k + ".c"));
    }

    static void antwoord(ServerPlayer p, int optie) {
        int stap = lijn().stap(p);
        if (stap >= GeestLijnen.RAADSELS_N) {
            return;
        }
        var koh = GeestPlek.bewoner(p.level(), "koh");
        String k = "gui.guhs.einde2_geest_spel.koh.raadsel." + (stap + 1);
        if (optie != GOED[stap]) {
            GeestSpel.zeg(p, koh, "entity.guhs.koh", k + ".fout");
            GeestSpel.geluid(p, "geest.koh_kraak", 0.9f, 0.7f);
            if (koh instanceof GeestWezen gw) {
                begin(p, gw);
            }
            return;
        }
        GeestSpel.zeg(p, koh, "entity.guhs.koh", k + ".goed");
        lijn().zet(p, stap + 1);
        if (lijn().klaar(p)) {
            GeestSpel.geef(p, E2GeestSpelFeature.KOH_MASKER.get(), 1);
            GuhAdvancements.grant(p, "einde2_geest_spel_koh");
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.koh.masker");
            GeestSpel.geluid(p, "stinger.opgelost", 0.8f, 0.8f);
        } else {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.koh.nog", GeestLijnen.RAADSELS_N - lijn().stap(p));
            if (koh instanceof GeestWezen gw) {
                begin(p, gw);
            }
        }
    }

    static void stop(ServerPlayer p) {
        WACHT.remove(p.getUUID());
    }
}
