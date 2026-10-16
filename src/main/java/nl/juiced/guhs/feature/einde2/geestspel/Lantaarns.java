package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;
import nl.juiced.guhs.feature.einde2.geestwezens.Libelkonijnguh;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase B, slice geest_spel: the side quest "Verdwaalde lantaarns" ({@value GeestLijnen#LANTAARNS}, DESIGN 5).
 * Eight lantern spirits got lost over the zones ({@code verdwaalde_lantaarn_1..8}); each player has their own eight
 * (actors only they see). A click and a lantern follows you, bobbing behind your shoulder; near the Lantaarnboom it flies up
 * to the next empty hook ({@code lantaarnboom_haak_1..8}) and hangs there, for you. The step of the line = how many are
 * home. All eight: a libelkonijnguhtje travels with you ({@link Libelkonijnguh#geef}), and the tree lights up for you
 * ({@link GeestLagen#BOOM}).
 * <p>
 * Which lantern is which: the flag {@code v_thuis_<n>} says lantern n is home (so the step and the hooks agree whatever
 * the order); a lost lantern only waits where its zone is open for you.
 */
public final class Lantaarns {
    /** How near the tree a following lantern must come to fly home. */
    public static final double THUIS = 7.0;

    /** player -> the lanterns following them now. */
    private static final Map<UUID, Set<Integer>> VOLGT = new ConcurrentHashMap<>();

    private Lantaarns() {
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.lantaarns();
    }

    /** Is lantern n home for this player? */
    static boolean thuis(ServerPlayer p, int n) {
        return lijn().vlag(p, "thuis_" + n);
    }

    /** The zone a lost lantern waits in (the zone box that holds its marker), or null. */
    @Nullable
    static String zoneVan(String marker) {
        Vec3 v = GeestPlek.midden(marker);
        if (v == null) {
            return null;
        }
        for (String z : nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld.ZONES) {
            var d = GeestPlek.doos("zone_" + z);
            if (d != null && d.contains(v)) {
                return z;
            }
        }
        return null;
    }

    /** May this player go and get lantern n (its zone open for them)? */
    static boolean bereikbaar(ServerPlayer p, int n) {
        String z = zoneVan("verdwaalde_lantaarn_" + n);
        return z == null || GeestToegang.zoneOpen(p, z);
    }

    /** The marker of the nearest lantern that is still lost and that this player can reach (null: none), for the compass. */
    @Nullable
    static String volgende(ServerPlayer p) {
        String best = null;
        double afstand = Double.MAX_VALUE;
        for (int n = 1; n <= GeestLijnen.LANTAARNS_N; n++) {
            if (thuis(p, n) || !bereikbaar(p, n)) {
                continue;
            }
            Vec3 v = GeestPlek.midden("verdwaalde_lantaarn_" + n);
            if (v != null && v.distanceToSqr(p.position()) < afstand) {
                afstand = v.distanceToSqr(p.position());
                best = "verdwaalde_lantaarn_" + n;
            }
        }
        return best;
    }

    // =====================================================================================================================
    // ticks
    // =====================================================================================================================

    /** Every tick: the followers float behind the player. */
    static void elkeTick(ServerPlayer p) {
        Set<Integer> volgt = VOLGT.get(p.getUUID());
        if (volgt == null || volgt.isEmpty()) {
            return;
        }
        int i = 0;
        for (int n : volgt) {
            Entity e = GeestActeurs.van(p, "lantaarn_" + n);
            if (e == null) {
                continue;
            }
            double hoek = Math.toRadians(p.getYRot() + 140 + i * 45) + Math.sin((p.tickCount + n * 9) * 0.08) * 0.2;
            Vec3 doel = p.position().add(-Math.sin(hoek) * 1.6, 1.6 + Math.sin((p.tickCount + n * 13) * 0.12) * 0.25, Math.cos(hoek) * 1.6);
            Vec3 nu = e.position();
            Vec3 stap = doel.subtract(nu);
            if (stap.lengthSqr() > 400) {
                e.teleportTo(doel.x, doel.y, doel.z);
            } else {
                e.setPos(nu.add(stap.scale(0.25)));
            }
            i++;
        }
    }

    /** Every 10 ticks: the lost ones wait at their spots, the home ones hang on their hooks, the followers come home. */
    static void tick(ServerPlayer p) {
        int thuisN = 0;
        Vec3 boom = GeestPlek.midden("lantaarnboom");
        Set<Integer> volgt = VOLGT.computeIfAbsent(p.getUUID(), u -> ConcurrentHashMap.newKeySet());
        for (int n = 1; n <= GeestLijnen.LANTAARNS_N; n++) {
            String naam = "lantaarn_" + n;
            if (thuis(p, n)) {
                thuisN++;
                continue;
            }
            if (volgt.contains(n)) {
                if (boom != null && p.position().closerThan(boom, THUIS)) {
                    breng(p, n);
                    thuisN++;
                }
                continue;
            }
            Vec3 plek = GeestPlek.midden("verdwaalde_lantaarn_" + n);
            boolean wil = plek != null && bereikbaar(p, n) && p.position().closerThan(plek, 48);
            if (wil) {
                GeestActeurs.zorg(p, naam, "guhs:lantaarngeest", plek.add(0, 0.8, 0), p.getRandom().nextFloat() * 360f, t -> {
                    t.putInt("Soort", (naam.hashCode() & 3));
                    t.putBoolean("Stil", true);
                });
            } else if (GeestActeurs.heeft(p, naam)) {
                GeestActeurs.weg(p, naam, false);
            }
        }
        // the ones at home, on their hooks (by the order they came home: hook k = the k-th lantern home)
        int k = 0;
        for (int n = 1; n <= GeestLijnen.LANTAARNS_N; n++) {
            if (!thuis(p, n)) {
                continue;
            }
            k++;
            String naam = "haak_" + k;
            Vec3 haak = GeestPlek.midden("lantaarnboom_haak_" + k);
            boolean wil = haak != null && p.position().closerThan(haak, 56);
            if (wil) {
                int soort = n & 3;
                GeestActeurs.zorg(p, naam, "guhs:lantaarngeest", haak.add(0, -1.1, 0), 0f, t -> {
                    t.putInt("Soort", soort);
                    t.putBoolean("Stil", true);
                });
            } else if (GeestActeurs.heeft(p, naam)) {
                GeestActeurs.weg(p, naam, false);
            }
        }
        if (thuisN > lijn().stap(p)) {
            lijn().zet(p, thuisN);
        }
    }

    /** A lost lantern of this player was clicked: it follows them. */
    static boolean klik(ServerPlayer p, GeestWezen w) {
        String naam = GeestActeurs.naam(w);
        if (naam == null || !p.getUUID().equals(GeestActeurs.eigenaar(w)) || !naam.startsWith("lantaarn_")) {
            return false;
        }
        int n;
        try {
            n = Integer.parseInt(naam.substring("lantaarn_".length()));
        } catch (NumberFormatException e) {
            return false;
        }
        Set<Integer> volgt = VOLGT.computeIfAbsent(p.getUUID(), u -> ConcurrentHashMap.newKeySet());
        if (volgt.add(n)) {
            lijn().begin(p);
            w.zetAnimatie("knipoog");
            GeestSpel.geluid(p, "geest.lantaarn_tinkel", 0.9f, 1.2f);
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lantaarn.volgt");
            if (lijn().stap(p) == 0 && volgt.size() == 1) {
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lantaarn.uitleg");
            }
        }
        return true;
    }

    /** Lantern n reached the tree: it hangs on the next hook. */
    private static void breng(ServerPlayer p, int n) {
        Set<Integer> volgt = VOLGT.get(p.getUUID());
        if (volgt != null) {
            volgt.remove(n);
        }
        Entity e = GeestActeurs.van(p, "lantaarn_" + n);
        if (e != null) {
            p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, e.getX(), e.getY() + 0.5, e.getZ(), 12, 0.2, 0.3, 0.2, 0.05);
        }
        GeestActeurs.weg(p, "lantaarn_" + n, true);
        lijn().vlag(p, "thuis_" + n, true);
        int thuis = 0;
        for (int i = 1; i <= GeestLijnen.LANTAARNS_N; i++) {
            if (thuis(p, i)) {
                thuis++;
            }
        }
        lijn().zet(p, thuis);
        GeestSpel.geluid(p, "geest.lantaarn_tinkel", 1.0f, 0.9f + thuis * 0.05f);
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lantaarn.thuis", thuis, GeestLijnen.LANTAARNS_N);
        if (thuis >= GeestLijnen.LANTAARNS_N) {
            klaar(p);
        }
    }

    /** All eight home: the tree lights up for this player, and a libelkonijnguhtje comes along. */
    static void klaar(ServerPlayer p) {
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lantaarn.klaar");
        GeestSpel.geluid(p, "stinger.opgelost", 1.0f, 1.0f);
        Vec3 boom = GeestPlek.midden("lantaarnboom");
        if (boom != null) {
            p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, boom.x, boom.y + 8, boom.z, 120, 4, 4, 4, 0.04);
        }
        if (!Libelkonijnguh.heeft(p)) {
            Libelkonijnguh.geef(p);
        }
        GuhAdvancements.grant(p, "einde2_geest_spel_lantaarns");
        nl.juiced.guhs.feature.bestaand.Schijn.straks(p);
        Mysterie.bloeiCheck(p);
    }

    static void vergeet(ServerPlayer p) {
        VOLGT.remove(p.getUUID());
    }

    /** (tests) lantern n follows this player. */
    static void testVolg(ServerPlayer p, int n) {
        VOLGT.computeIfAbsent(p.getUUID(), u -> ConcurrentHashMap.newKeySet()).add(n);
    }

    /** (tests, dev) lantern n is brought home. */
    static void testBreng(ServerPlayer p, int n) {
        breng(p, n);
    }

    static List<Integer> volgers(ServerPlayer p) {
        Set<Integer> s = VOLGT.get(p.getUUID());
        return s == null ? List.of() : List.copyOf(s);
    }
}
