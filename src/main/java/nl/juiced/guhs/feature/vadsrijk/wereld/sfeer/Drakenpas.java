package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.Vahoegnir;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Slijmwezen;

/**
 * Idea 1 "Drakenpassage": now and then (every 30-45 min) Vahoegnir skims low over Zwamzoet or over the area of a player
 * (never while a player near the path is fighting or inside a boss arena). Server side: the flyover itself
 * ({@link DraakVlucht}), NPC guhs look up and hop in fright, klodders hop away. The clients get a {@link SfeerPayloads.DraakPas}
 * and add the shadow, the wind, swaying lanterns and the flowers that turn grey for 10 s.
 */
public final class Drakenpas {
    /** Ticks between passes: 30 min + up to 15 min. */
    static final int ELKE = 20 * 60 * 30, SPREIDING = 20 * 60 * 15;
    static final int DUUR = 260;
    /** A player counts as fighting for this long after their last hit given or taken. */
    private static final int GEVECHT = 20 * 15;
    private static final RandomSource R = RandomSource.create();
    private static final Map<UUID, Long> LAATSTE_KLAP = new HashMap<>();
    private static long volgende = -1;

    private record Pas(Vahoegnir draak, long eind, Set<UUID> geschrokken) {
    }

    private static final List<Pas> PASSEN = new ArrayList<>();

    private Drakenpas() {
    }

    /** Kern's damage hook: remember who fights. */
    static void klap(Schade.Uitkomst u) {
        long nu = u.klap().doel().level().getGameTime();
        if (u.klap().bron() instanceof ServerPlayer p) {
            LAATSTE_KLAP.put(p.getUUID(), nu);
        }
        if (u.klap().doel() instanceof ServerPlayer p) {
            LAATSTE_KLAP.put(p.getUUID(), nu);
        }
    }

    static boolean vecht(ServerPlayer p) {
        Long t = LAATSTE_KLAP.get(p.getUUID());
        if (t != null && p.level().getGameTime() - t < GEVECHT) {
            return true;
        }
        if (Arenas.op(p.getX(), p.getY(), p.getZ()) != null) {
            return true;
        }
        return !p.level().getEntitiesOfClass(VadsrijkBaas.class, p.getBoundingBox().inflate(40), b -> b.getTarget() != null).isEmpty();
    }

    static void tick(ServerLevel sl) {
        long nu = sl.getGameTime();
        if (volgende < 0) {
            volgende = nu + ELKE + R.nextInt(SPREIDING);
        }
        effecten(sl);
        if (nu < volgende) {
            return;
        }
        volgende = nu + 20 * 60;     // nobody / everyone fighting: try again in a minute
        if (DraakVlucht.actief() > 0 || sl.players().isEmpty()) {
            return;
        }
        List<ServerPlayer> kandidaten = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
            if (g != null && !g.id().equals("proeftuin") && !p.isSpectator() && buiten(sl, p)) {
                kandidaten.add(p);
            }
        }
        if (kandidaten.isEmpty()) {
            return;
        }
        // the city first (the skyline is where it looks best), else the area of a random player
        ServerPlayer doel = null;
        for (ServerPlayer p : kandidaten) {
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
            if (g != null && g.id().equals("zwamzoet")) {
                doel = p;
            }
        }
        boolean stad = doel != null;
        if (doel == null) {
            doel = kandidaten.get(R.nextInt(kandidaten.size()));
        }
        List<Vec3> pad = stad ? stadPad(R.nextBoolean()) : pad(sl, doel.position(), R.nextDouble() * Math.PI * 2);
        if (start(sl, pad, false)) {
            volgende = nu + ELKE + R.nextInt(SPREIDING);
        }
    }

    /** Under the open sky (not deep in a cave or the temple). */
    private static boolean buiten(ServerLevel sl, ServerPlayer p) {
        return p.getY() > sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ()) - 6;
    }

    /** West to east (or back) low over the plein of Zwamzoet, between the southern caps. */
    static List<Vec3> stadPad(boolean omgekeerd) {
        List<Vec3> p = new ArrayList<>(List.of(new Vec3(-200, 165, 150), new Vec3(-110, 118, 100), new Vec3(-35, 100, 68), new Vec3(35, 100, 58),
                new Vec3(110, 118, 82), new Vec3(200, 165, 30)));
        if (omgekeerd) {
            java.util.Collections.reverse(p);
        }
        return p;
    }

    /** A straight skim over {@code midden}: high at both ends, ~30 blocks above the ground in the middle. */
    static List<Vec3> pad(ServerLevel sl, Vec3 midden, double hoek) {
        double dx = Math.cos(hoek), dz = Math.sin(hoek);
        double grond = Math.max(64, sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) midden.x, (int) midden.z));
        double[] af = {-190, -100, -30, 30, 100, 190}, hoog = {95, 50, 32, 32, 50, 95};
        List<Vec3> p = new ArrayList<>();
        for (int i = 0; i < af.length; i++) {
            // (balans 2026-11) the dragon is 1.5x bigger (wingspan ~31): every point stays 12 above the highest ground (hills,
            // mushroom caps, roofs) within its wings around its stretch of the line
            double van = i == 0 ? af[0] : (af[i - 1] + af[i]) / 2, tot = i == af.length - 1 ? af[i] : (af[i] + af[i + 1]) / 2;
            double top = Double.NEGATIVE_INFINITY;
            for (double t = van; t <= tot; t += 6) {
                for (int zij = -16; zij <= 16; zij += 4) {
                    int x = (int) Math.floor(midden.x + dx * t - dz * zij), z = (int) Math.floor(midden.z + dz * t + dx * zij);
                    if (sl.hasChunk(x >> 4, z >> 4)) {
                        top = Math.max(top, sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
                    }
                }
            }
            p.add(new Vec3(midden.x + dx * af[i], Math.max(grond + hoog[i], top + VRIJ), midden.z + dz * af[i]));
        }
        return p;
    }

    /** Blocks between the dragon's flight line and the highest ground under its wings (balans 2026-11). */
    static final int VRIJ = 12;

    /** Start a pass along this path. {@code dev}: ignore the fight check. Returns false when someone near the path fights. */
    static boolean start(ServerLevel sl, List<Vec3> pad, boolean dev) {
        Vec3 m = pad.get(pad.size() / 2);
        if (!dev) {
            for (ServerPlayer p : sl.players()) {
                if (p.position().distanceTo(m) < 200 && vecht(p)) {
                    return false;
                }
            }
        }
        Vahoegnir d = DraakVlucht.vlieg(sl, pad, DUUR, true);
        if (d == null) {
            return false;
        }
        PASSEN.add(new Pas(d, sl.getGameTime() + DUUR, new HashSet<>()));
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceTo(m) < 260) {
                PacketDistributor.sendToPlayer(p, new SfeerPayloads.DraakPas(d.getId(), DUUR));
            }
        }
        return true;
    }

    /** NPC guhs look up and hop in fright; klodders hop away from the shadow. */
    private static void effecten(ServerLevel sl) {
        if (PASSEN.isEmpty()) {
            return;
        }
        long nu = sl.getGameTime();
        PASSEN.removeIf(p -> nu > p.eind() || p.draak().isRemoved());
        if (nu % 4 != 0) {
            return;
        }
        for (Pas pas : PASSEN) {
            Vahoegnir d = pas.draak();
            AABB onder = new AABB(d.getX() - 34, d.getY() - 110, d.getZ() - 34, d.getX() + 34, d.getY() + 4, d.getZ() + 34);
            for (Entity e : sl.getEntities((Entity) null, onder, x -> x instanceof RijkGuh || x instanceof Slijmwezen)) {
                if (e instanceof RijkGuh g) {
                    g.getLookControl().setLookAt(d, 40f, 60f);
                    if (pas.geschrokken().add(g.getUUID()) && g.onGround()) {
                        g.setDeltaMovement(g.getDeltaMovement().add(0, 0.42, 0));
                        sl.playSound(null, g.getX(), g.getY(), g.getZ(), SoundEvents.ALLAY_HURT, SoundSource.NEUTRAL, 0.5f, 1.6f);
                    }
                } else if (e instanceof Slijmwezen s && s.getTarget() == null && s.onGround()) {
                    Vec3 weg = s.position().subtract(d.getX(), s.getY(), d.getZ());
                    weg = weg.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : weg.normalize();
                    s.setDeltaMovement(weg.x * 0.45, 0.42, weg.z * 0.45);
                    s.setYRot((float) Math.toDegrees(Math.atan2(-weg.x, weg.z)));
                    s.setYBodyRot(s.getYRot());
                }
            }
        }
    }

    /** Dev command: a pass now, over the city or over this player. */
    static boolean nu(ServerLevel sl, @Nullable LivingEntity boven, boolean stad) {
        List<Vec3> pad;
        if (stad || boven == null) {
            pad = stadPad(R.nextBoolean());
        } else {
            // crossing in front of the player, left to right (yaw: facing = (-sin, cos); the path runs along (cos, sin))
            double yaw = Math.toRadians(boven.getYRot());
            Vec3 voor = boven.position().add(-Math.sin(yaw) * 22, 0, Math.cos(yaw) * 22);
            pad = pad(sl, voor, yaw);
        }
        volgende = sl.getGameTime() + ELKE + R.nextInt(SPREIDING);
        return start(sl, pad, true);
    }

    static long volgende() {
        return volgende;
    }

    static void vergeet(UUID id) {
        LAATSTE_KLAP.remove(id);
    }

    static void wis() {
        PASSEN.clear();
        LAATSTE_KLAP.clear();
        volgende = -1;
    }
}
