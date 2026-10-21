package nl.juiced.guhs.feature.safari.momenten;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.lobby.Beleving;
import nl.juiced.guhs.feature.safari.rit.Pad;
import nl.juiced.guhs.feature.safari.rit.RitData;
import nl.juiced.guhs.world.GuhTime;

/**
 * The pinguïnparade (test round 3, design ronde 45): once a day ({@link MomentSchema#PARADE_VAN}, on the schemabord)
 * a little row of penguins waddles in a scripted group, with a tune of note-block flutes, along two walks:
 * <ul>
 *   <li>the truck road through the IJskust (the part of the route that lies in the IJskust);</li>
 *   <li>a loop through the village: from the gate by the truck station round the fountain and back
 *       ({@link Beleving#paradeDorp}). Visitor guhs on the square walk along behind the last penguin.</li>
 * </ul>
 * The parade penguins are extra animals (tag {@link #TAG}, no home, no zone): they come with a puff when the parade
 * begins and leave with a puff when it is over (also after a restart in the middle of it).
 * <p>
 * The hook for the animal behaviour (J1): {@link #actief} (a parade is on in this level) and {@link #isParadeDier}
 * (this animal walks in it: leave its movement alone; its {@code thuis} moves along every half second).
 */
public final class Parade {
    public static final String TAG = "guhs_safari_parade";
    static final int PINGUINS = 5, ACHTER = 25, STAP = 10;
    private static final float[] DEUNTJE = {1.0f, 1.26f, 1.5f, 1.26f, 1.0f, 1.12f, 1.33f, 1.68f};

    /** A walking group: its waypoints and per penguin how far it got (index into the waypoints). */
    private static final class Groep {
        final String naam;
        final List<BlockPos> punten;
        final Map<UUID, Integer> stand = new HashMap<>();
        final Map<UUID, Integer> vast = new HashMap<>();
        boolean gemaakt;

        Groep(String naam, List<BlockPos> punten) {
            this.naam = naam;
            this.punten = punten;
        }
    }

    private static final List<Groep> GROEPEN = new ArrayList<>();
    private static long dagGestart = Long.MIN_VALUE;
    private static int noot;

    /** Is the penguin parade on in this level now? */
    public static boolean actief(net.minecraft.world.level.Level level) {
        return Safari.echt(level) && MomentSchema.parade((int) GuhTime.timeOfDay(level));
    }

    /** Does this animal walk in the parade (its walking is scripted here)? */
    public static boolean isParadeDier(Entity e) {
        return e instanceof SafariDier && e.entityTags().contains(TAG);
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % STAP != 5) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null) {
            return;
        }
        boolean aan = actief(level) && !level.players().isEmpty();
        long dag = GuhTime.day(level);
        if (!aan) {
            if (!GROEPEN.isEmpty() || server.getTickCount() % 400 == 5) {
                stop(level);
            }
            return;
        }
        if (dagGestart != dag) {
            stop(level);
            dagGestart = dag;
            GROEPEN.add(new Groep("ijskust", ijskustPad()));
            GROEPEN.add(new Groep("dorp", Beleving.get().paradeDorp));
        }
        for (Groep g : GROEPEN) {
            loop(level, g);
        }
    }

    /** The waypoints of the IJskust walk: every 4th route point that lies in the IJskust, in route order. */
    static List<BlockPos> ijskustPad() {
        List<BlockPos> uit = new ArrayList<>();
        Pad route = RitData.get().route;
        ParkData d = ParkData.get();
        int zone = d.zoneIndex("ijskust");
        if (route == null || zone < 0) {
            return uit;
        }
        // the longest run of route points in the IJskust (the loop may start in the middle of it)
        int n = (int) Math.floor(route.lengte() / 1.5);
        List<BlockPos> run = new ArrayList<>();
        for (int i = 0; i < n * 2 && i < 4000; i++) {
            Vec3 p = route.op((i % n) * 1.5);
            if (d.zone((int) Math.floor(p.x), (int) Math.floor(p.z)) == zone) {
                run.add(BlockPos.containing(p));
            } else {
                if (run.size() > uit.size()) {
                    uit = run;
                }
                run = new ArrayList<>();
            }
        }
        if (run.size() > uit.size()) {
            uit = run;
        }
        List<BlockPos> om = new ArrayList<>();
        for (int i = 0; i < uit.size(); i += 4) {
            om.add(uit.get(i));
        }
        return om;
    }

    private static void loop(ServerLevel level, Groep g) {
        if (g.punten.size() < 2) {
            return;
        }
        BlockPos start = g.punten.get(0);
        if (!level.isLoaded(start)) {
            return;
        }
        if (!g.gemaakt) {
            g.gemaakt = true;
            for (int i = 0; i < PINGUINS; i++) {
                SafariDier p = SafariFeature.DIER.get().create(level, EntitySpawnReason.EVENT);
                if (p == null) {
                    return;
                }
                BlockPos plek = grond(level, start);
                p.zet("pinguin", i % 2 == 0 ? "man" : "vrouw", plek, 3, -1, -1, 0);
                p.addTag(TAG);
                p.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 0f, 0f);
                level.addFreshEntity(p);
                level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.4, p.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                // one after the other: the later ones start a little further back
                g.stand.put(p.getUUID(), -i);
            }
        }
        SafariDier eerste = null;
        SafariDier laatste = null;
        for (Map.Entry<UUID, Integer> e : g.stand.entrySet()) {
            if (!(level.getEntity(e.getKey()) instanceof SafariDier p) || p.isRemoved()) {
                continue;
            }
            int i = e.getValue();
            if (i < 0) {
                e.setValue(i + 1);
                continue;
            }
            BlockPos doel = g.punten.get(Math.min(i, g.punten.size() - 1));
            if (i < g.punten.size() - 1 && p.distanceToSqr(doel.getX() + 0.5, p.getY(), doel.getZ() + 0.5) < 2.5 * 2.5) {
                e.setValue(++i);
                g.vast.remove(e.getKey());
                doel = g.punten.get(i);
            } else if (i < g.punten.size() - 1 && g.vast.merge(e.getKey(), 1, Integer::sum) > 20) {
                // stuck for 10 seconds: hop to the waypoint
                g.vast.remove(e.getKey());
                BlockPos t = grond(level, doel);
                p.teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
            }
            p.zetThuis(doel, 3, -1);
            if (i >= g.punten.size() - 1) {
                if (p.getRandom().nextInt(4) == 0) {
                    p.speelActie("blij", 40);
                }
            } else {
                p.getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0);
            }
            if (eerste == null || i > g.stand.getOrDefault(eerste.getUUID(), 0)) {
                eerste = p;
            }
            if (laatste == null || i < g.stand.getOrDefault(laatste.getUUID(), 0)) {
                laatste = p;
            }
        }
        if (eerste != null && level.getGameTime() % 20 < STAP) {
            level.playSound(null, eerste.getX(), eerste.getY(), eerste.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS,
                    1.2f, DEUNTJE[noot++ % DEUNTJE.length]);
            level.sendParticles(ParticleTypes.NOTE, eerste.getX(), eerste.getY() + 1.2, eerste.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
        }
        if (laatste != null && "dorp".equals(g.naam)) {
            meelopen(level, laatste);
        }
    }

    /** Visitor guhs near the village walk follow the last penguin. */
    private static void meelopen(ServerLevel level, SafariDier laatste) {
        int n = 0;
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(laatste.blockPosition()).inflate(18, 6, 18),
                g -> g.entityTags().contains("guhs_safari_bezoeker") && !g.isPassenger())) {
            if (n++ >= 4) {
                break;
            }
            GuhHooks.bezig(guh, 40);
            if (guh.distanceToSqr(laatste) > 3 * 3) {
                guh.getNavigation().moveTo(laatste.getX(), laatste.getY(), laatste.getZ(), 1.0);
            }
        }
    }

    /** The parade is over (or was cut off by a restart): the penguins go home with a puff. */
    private static void stop(ServerLevel level) {
        GROEPEN.clear();
        dagGestart = Long.MIN_VALUE;
        for (Entity e : level.getAllEntities()) {
            if (isParadeDier(e)) {
                level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.4, e.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                e.discard();
            }
        }
    }

    private static BlockPos grond(ServerLevel level, BlockPos p) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
    }

    private Parade() {
    }
}
