package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * einde2 phase B, slice h4_vuur, finale step 2: the PLAYABLE flight on Appa through Vuurheer Mikai's burning airship fleet
 * (DESIGN 5 "Extra epische stukken", CONTRACT_E2_B 7.5). You steer Appa yourself along {@code luchtvloot_route} (from above
 * the quay of the Leeuwschildpad-guh, round the crater, to the duel arena); {@link #SCHEPEN} airships ({@link LuchtschipEntity},
 * only you see them) drift across it and fire blasts that only SHOVE. Fly close past a ship (but not into it) and Sokka-guh,
 * Toph-guh or Suki-guh jump over and take it down. Three blasts in your face, or flying into a hull: the fleet drives you
 * back to the start of the run (nothing lost but a bit of pride). All ships but one down and at the end: the arena.
 */
public final class Luchtvloot {
    public static final int SCHEPEN = 6, NODIG = 5, RAAK_MAX = 3, TIJD = 20 * 240;
    /** Blocks: a ship is taken down when you pass within NEER; flying into its hull (within BOTS) sends you back. */
    public static final double NEER = 16.0, BOTS = 4.5, RAAK = 2.4, EINDE = 12.0;
    private static final String[] HELPERS = {"sokka", "toph", "suki"};

    private static final class Schip {
        final LuchtschipEntity e;
        final Vec3 thuis, opzij;
        final int fase;
        int schot;

        Schip(LuchtschipEntity e, Vec3 thuis, Vec3 opzij, int fase) {
            this.e = e;
            this.thuis = thuis;
            this.opzij = opzij;
            this.fase = fase;
        }
    }

    private static final class Vlucht {
        final List<Vec3> route;
        final List<Schip> schepen = new ArrayList<>();
        final List<VuurbalEntity> stoten = new ArrayList<>();
        int t, raak, neer, helper, zonderAppa;

        Vlucht(List<Vec3> route) {
            this.route = route;
        }
    }

    private static final Map<UUID, Vlucht> VLUCHTEN = new ConcurrentHashMap<>();

    private Luchtvloot() {
    }

    public static boolean actief(ServerPlayer p) {
        return VLUCHTEN.containsKey(p.getUUID());
    }

    /** The route of the run: the build's pad, or (a build without it) a curve from above the quay to above the arena. */
    static List<Vec3> route(ServerPlayer p) {
        List<Vec3> pad = Plek.pad(p, "luchtvloot_route");
        if (pad.size() >= 2) {
            return pad;
        }
        Vec3 a = Plek.staan(p, "leeuwschildpad_kade"), b = Plek.midden(p, "duel_arena");
        if (a == null || b == null) {
            return List.of();
        }
        a = a.add(0, 50, 20);
        b = b.add(0, 40, 0);
        List<Vec3> uit = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            double f = i / 5.0;
            uit.add(a.lerp(b, f).add(0, Math.sin(f * Math.PI) * 10, 0));
        }
        return uit;
    }

    /** At the quay: Appa comes, you climb on, the fleet appears. */
    static void start(ServerPlayer p) {
        if (actief(p)) {
            return;
        }
        List<Vec3> route = route(p);
        ServerLevel level = Plek.guheinde(p);
        if (route.isEmpty() || level == null) {
            return;
        }
        AppaGuhEntity appa = Appa.heeft(p) ? Appa.roep(p) : Appa.geef(p);
        if (appa == null) {
            E2H4Vuur.balk(p, "vloot.geen_appa");
            return;
        }
        Vlucht v = new Vlucht(route);
        VLUCHTEN.put(p.getUUID(), v);
        opDeStart(p, v, appa);
        E2Audio.moment(p, "luchtvloot");
        E2H4Vuur.zeg(p, "vloot.start");
    }

    /** Appa and the player at the start of the run, the fleet in its place (again). */
    private static void opDeStart(ServerPlayer p, Vlucht v, @Nullable AppaGuhEntity appa) {
        ServerLevel level = p.level();
        opruimen(v);
        v.t = 0;
        v.raak = 0;
        v.neer = 0;
        Vec3 start = v.route.get(0);
        Vec3 richting = v.route.get(1).subtract(start).normalize();
        float yaw = nl.juiced.guhs.feature.verhaal.Cutscene.yawVan(richting);
        if (appa == null) {
            appa = Appa.roep(p);
        }
        if (appa != null) {
            if (p.getVehicle() != appa) {
                p.stopRiding();
            }
            appa.snapTo(start.x, start.y, start.z, yaw, 0);
            appa.setDeltaMovement(Vec3.ZERO);
            if (p.getVehicle() != appa) {
                p.startRiding(appa, true, false);
            }
        } else {
            Plek.breng(p, level, start, yaw);
        }
        // the ships: spread over the route, beside it, drifting across it
        for (int i = 0; i < SCHEPEN; i++) {
            double f = (i + 1.0) / (SCHEPEN + 1.0);
            Vec3 op = punt(v.route, f);
            Vec3 voor = punt(v.route, Math.min(1, f + 0.05)).subtract(op);
            Vec3 opzij = new Vec3(-voor.z, 0, voor.x).normalize();
            Vec3 thuis = op.add(opzij.scale(i % 2 == 0 ? 9 : -9)).add(0, (i % 3) * 3 - 3, 0);
            LuchtschipEntity e = E2H4VuurFeature.LUCHTSCHIP.get().create(level, EntitySpawnReason.TRIGGERED);
            if (e == null) {
                continue;
            }
            e.snapTo(thuis.x, thuis.y, thuis.z, nl.juiced.guhs.feature.verhaal.Cutscene.yawVan(voor.scale(-1)), 0);
            E2H4Vuur.eigen(e, p);
            level.addFreshEntity(e);
            v.schepen.add(new Schip(e, thuis, opzij, i * 37));
        }
    }

    /** The point at fraction f (0..1) of the route (by segments). */
    static Vec3 punt(List<Vec3> route, double f) {
        double plek = Math.max(0, Math.min(1, f)) * (route.size() - 1);
        int i = Math.min(route.size() - 2, (int) Math.floor(plek));
        return route.get(i).lerp(route.get(i + 1), plek - i);
    }

    static void tick(ServerPlayer p) {
        Vlucht v = VLUCHTEN.get(p.getUUID());
        if (v == null) {
            return;
        }
        v.t++;
        ServerLevel level = p.level();
        Entity rijdier = p.getRootVehicle();
        Vec3 jij = rijdier.position().add(0, 1.2, 0);
        // off Appa: a few seconds to climb back on, then back to the start
        if (!(rijdier instanceof AppaGuhEntity)) {
            if (++v.zonderAppa == 1) {
                E2H4Vuur.balk(p, "vloot.op_appa");
            }
            if (v.zonderAppa > 100) {
                terug(p, v, "vloot.van_appa");
            }
            return;
        }
        v.zonderAppa = 0;
        if (v.t > TIJD) {
            terug(p, v, "vloot.te_laat");
            return;
        }
        // the ships drift and fire; flown close past, a member of the team takes one down
        for (Schip s : v.schepen) {
            if (s.e.isRemoved() || s.e.zinkt()) {
                continue;
            }
            double golf = Math.sin((v.t + s.fase) / 45.0);
            Vec3 waar = s.thuis.add(s.opzij.scale(golf * 6));
            s.e.setDeltaMovement(waar.subtract(s.e.position()).scale(0.05));
            double afstand = s.e.position().add(0, 1.5, 0).distanceTo(jij);
            if (afstand < BOTS) {
                terug(p, v, "vloot.botsing");
                return;
            }
            if (afstand < NEER) {
                s.e.zink();
                v.neer++;
                String wie = HELPERS[v.helper++ % HELPERS.length];
                p.sendSystemMessage(Component.translatable(E2H4Vuur.Q + "vloot.neer." + wie).withStyle(ChatFormatting.WHITE));
                Schijn.geluid(p, SoundEvents.GENERIC_EXPLODE.value(), s.e.blockPosition(), 0.5f, 1.4f);
                E2H4Vuur.balk(p, "vloot.stand", v.neer, NODIG);
                continue;
            }
            if (afstand < 48 && v.t - s.schot > 55 + (s.fase % 30)) {
                s.schot = v.t;
                schiet(p, v, s.e.position().add(0, 1.0, 0), jij.add(rijdier.getDeltaMovement().scale(12)));
            }
        }
        // the blasts: a hit only shoves (Appa and you), three in a row and the fleet drives you back
        for (VuurbalEntity stoot : List.copyOf(v.stoten)) {
            if (stoot.isRemoved()) {
                v.stoten.remove(stoot);
                continue;
            }
            if (stoot.position().distanceTo(jij) < RAAK) {
                v.stoten.remove(stoot);
                stoot.discard();
                v.raak++;
                Duwtje.duw(p, stoot.getDeltaMovement(), 1.1);
                Schijn.geluid(p, SoundEvents.FIRECHARGE_USE, p.blockPosition(), 0.8f, 0.8f);
                level.sendParticles(p, ParticleTypes.LARGE_SMOKE, true, false, jij.x, jij.y, jij.z, 12, 0.6, 0.6, 0.6, 0.02);
                if (v.raak >= RAAK_MAX) {
                    terug(p, v, "vloot.geraakt");
                    return;
                }
                E2H4Vuur.balk(p, "vloot.au", RAAK_MAX - v.raak);
            }
        }
        // the end of the run
        if (jij.distanceTo(v.route.get(v.route.size() - 1)) < EINDE) {
            if (v.neer >= NODIG) {
                gelukt(p);
            } else if (v.t % 40 == 0) {
                E2H4Vuur.balk(p, "vloot.nog_niet", v.neer, NODIG);
            }
        } else if (v.t % 100 == 0) {
            E2H4Vuur.balk(p, "vloot.stand", v.neer, NODIG);
        }
    }

    private static void schiet(ServerPlayer p, Vlucht v, Vec3 van, Vec3 naar) {
        VuurbalEntity stoot = E2H4VuurFeature.VUURBAL.get().create(p.level(), EntitySpawnReason.TRIGGERED);
        if (stoot == null) {
            return;
        }
        stoot.snapTo(van.x, van.y, van.z, 0, 0);
        stoot.setDeltaMovement(naar.subtract(van).normalize().scale(0.75));
        stoot.leven = 100;
        E2H4Vuur.eigen(stoot, p);
        p.level().addFreshEntity(stoot);
        v.stoten.add(stoot);
        Schijn.geluid(p, SoundEvents.BLAZE_SHOOT, net.minecraft.core.BlockPos.containing(van), 0.6f, 0.7f);
    }

    /** Back to the start of the run (never anything lost). */
    private static void terug(ServerPlayer p, Vlucht v, String waarom) {
        E2H4Vuur.zeg(p, waarom);
        E2H4Vuur.zeg(p, "vloot.opnieuw");
        AppaGuhEntity appa = p.getRootVehicle() instanceof AppaGuhEntity a ? a : null;
        opDeStart(p, v, appa);
    }

    private static void gelukt(ServerPlayer p) {
        stop(p, true);
        E2Audio.stopMoment(p, 60);
        if (E2H4Vuur.FINALE.verder(p, E2H4Vuur.F_VLOOT)) {
            E2H4Vuur.zeg(p, "vloot.gelukt");
            p.stopRiding();
            Vec3 plein = Plek.staan(p, "agni_kai_midden");
            if (plein == null) {
                plein = Plek.midden(p, "kroning");
            }
            ServerLevel g = Plek.guheinde(p);
            if (plein != null && g != null) {
                Plek.breng(p, g, plein.add(0, 0, 4), 180f);
                Appa.roep(p);
            }
        }
    }

    private static void opruimen(Vlucht v) {
        v.schepen.forEach(s -> s.e.discard());
        v.schepen.clear();
        v.stoten.forEach(Entity::discard);
        v.stoten.clear();
    }

    static void stop(ServerPlayer p, boolean gelukt) {
        Vlucht v = VLUCHTEN.remove(p.getUUID());
        if (v != null) {
            opruimen(v);
            if (!gelukt) {
                E2Audio.stopMoment(p, 20);
            }
        }
    }

    /** (dev, tests) the flight as if it went well. */
    static void winVoorTest(ServerPlayer p) {
        stop(p, true);
        E2H4Vuur.FINALE.verder(p, E2H4Vuur.F_VLOOT);
    }
}
