package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.storage.Nbt;

/**
 * einde2 "Sponsguh", slice sp_d2_weg (CONTRACT_E2_C 7.7): the tear game on the drying table (step 8, variant {@code 8_b}).
 * Sponsguh and Patguh lie dried on the table under the lamp; the player is "held" at {@code droogtafel_speler} (he sits on an
 * invisible seat: sneaking lets go). A tear from Patguh's eye must roll along the wet trail (a line of blue dust: across the
 * table, off its end, over the floor round the corner) to the socket on the wall ({@code stopcontact}).
 * <ul>
 *   <li>CONTROLS: W/A/S/D tilt the table away/left/towards/right of where you look; the tear rolls that way, and slows by
 *       itself. The table wobbles a little in the heat (a slow push sideways, always the same, never random);</li>
 *   <li>COUNTER: "Traan: n%" and "Tranen: n" above the hotbar, and the "droog" meter that fills and then stays full: it is
 *       only a picture of how crinkly they get (CONTRACT: it never fails; with the real music it may slow the tune: hook);</li>
 *   <li>OFF THE TRAIL (more than {@value #BREED} blocks beside it): the tear dries up with a puff, Patguh cries a new one at
 *       once at his eye; nothing else happens, no limit;</li>
 *   <li>FINISH: the tear in the socket: sparks, a title, then the scene {@code sproeier}.</li>
 * </ul>
 * {@link Bord} is the pure state machine (the game test drives it); this class puts it in the world.
 */
final class Traan {
    /** Half the width of the wet trail (blocks). */
    static final double BREED = 0.6;
    /** Tilt: speed gained per tick at full tilt, the slowing per tick, the top speed (blocks per tick). */
    static final double VERSNEL = 0.0045, WRIJVING = 0.86, MAX = 0.06;
    /** The wobble of the hot table (a push sideways, blocks per tick per tick). */
    static final double WIEBEL = 0.0016;
    /** The finish: this close to the last point. */
    static final double BINNEN = 0.32;
    /** The trail in template coordinates of Schelpenstad: Patguh's eye on the table, the table's end, the floor, the socket. */
    static final double[][] SPOOR = {{185.5, 15.08, 16.5}, {185.95, 15.08, 16.5}, {186.45, 14.05, 16.6}, {186.9, 14.05, 17.6}, {188.0, 14.05, 18.1},
            {189.0, 14.05, 17.2}, {188.9, 14.05, 15.6}, {188.0, 14.05, 14.9}, {187.5, 14.05, 14.55}};
    /** Where the tear goes in at the end (the socket's plate on the wall). */
    static final double[] IN_STOPCONTACT = {187.5, 15.35, 14.35};
    private static final DustParticleOptions NAT = new DustParticleOptions(0x6FB8FF, 0.55f);

    // =====================================================================================================================
    // the pure state machine
    // =====================================================================================================================

    enum Uit {
        NIETS, VAN_DE_RAND, BINNEN
    }

    static final class Bord {
        final List<Vec3> p;
        final double[] tot;
        double x, z, vx, vz;
        int seg, tranen = 1;
        long t;

        Bord(List<Vec3> spoor) {
            this.p = List.copyOf(spoor);
            this.tot = new double[p.size()];
            for (int i = 1; i < p.size(); i++) {
                tot[i] = tot[i - 1] + Math.hypot(p.get(i).x - p.get(i - 1).x, p.get(i).z - p.get(i - 1).z);
            }
            nieuw();
        }

        /** A new tear at Patguh's eye (the start of the trail), at rest. */
        void nieuw() {
            x = p.get(0).x;
            z = p.get(0).z;
            vx = vz = 0;
            seg = 0;
        }

        double lengte() {
            return tot[tot.length - 1];
        }

        /** One tick with the tilt (fx, fz) in the world (each about -1..1). */
        Uit stap(double fx, double fz) {
            t++;
            Vec3 a = p.get(seg), b = p.get(seg + 1);
            double dx = b.x - a.x, dz = b.z - a.z, len = Math.max(1e-6, Math.hypot(dx, dz));
            double nx = -dz / len, nz = dx / len;                          // (across the trail)
            double w = WIEBEL * (Math.sin(t * 0.11) + 0.5 * Math.sin(t * 0.037 + 1.3));
            vx = (vx + fx * VERSNEL + nx * w) * WRIJVING;
            vz = (vz + fz * VERSNEL + nz * w) * WRIJVING;
            double v = Math.hypot(vx, vz);
            if (v > MAX) {
                vx *= MAX / v;
                vz *= MAX / v;
            }
            x += vx;
            z += vz;
            // the nearest part of the trail, only the one it is on and its two neighbours (it cannot jump a corner)
            double best = Double.MAX_VALUE;
            int beste = seg;
            for (int i = Math.max(0, seg - 1); i <= Math.min(p.size() - 2, seg + 1); i++) {
                double d = afstand(i);
                if (d < best - 1e-9) {
                    best = d;
                    beste = i;
                }
            }
            if (best > BREED) {
                return Uit.VAN_DE_RAND;
            }
            seg = beste;
            Vec3 eind = p.get(p.size() - 1);
            if (seg == p.size() - 2 && Math.hypot(x - eind.x, z - eind.z) <= BINNEN) {
                return Uit.BINNEN;
            }
            return Uit.NIETS;
        }

        /** The distance (x/z) from the tear to part i of the trail. */
        double afstand(int i) {
            Vec3 a = p.get(i), b = p.get(i + 1);
            double f = param(i);
            return Math.hypot(a.x + (b.x - a.x) * f - x, a.z + (b.z - a.z) * f - z);
        }

        private double param(int i) {
            Vec3 a = p.get(i), b = p.get(i + 1);
            double dx = b.x - a.x, dz = b.z - a.z, len2 = dx * dx + dz * dz;
            return len2 <= 1e-9 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * dx + (z - a.z) * dz) / len2));
        }

        /** How far along the trail (0..1). */
        double voortgang() {
            double f = param(seg);
            return Math.max(0, Math.min(1, (tot[seg] + f * (tot[seg + 1] - tot[seg])) / Math.max(1e-6, lengte())));
        }

        /** The tear's spot (its height follows the trail: on the table, down its end, on the floor). */
        Vec3 plek() {
            Vec3 a = p.get(seg), b = p.get(seg + 1);
            return new Vec3(x, a.y + (b.y - a.y) * param(seg), z);
        }

        /** (tests, dev) the tilt that rolls the tear towards the next point of the trail. */
        double[] naarVolgende() {
            Vec3 doel = p.get(Math.min(p.size() - 1, seg + 1));
            if (Math.hypot(doel.x - x, doel.z - z) < 0.25 && seg + 2 < p.size()) {
                doel = p.get(seg + 2);
            }
            double dx = doel.x - x, dz = doel.z - z, len = Math.max(1e-6, Math.hypot(dx, dz));
            return new double[] {dx / len, dz / len};
        }
    }

    // =====================================================================================================================
    // in the world
    // =====================================================================================================================

    private static final class Actief {
        final Bord bord;
        final Entity zetel, traan;
        final long begin;
        int droog;

        Actief(Bord bord, Entity zetel, Entity traan, long begin) {
            this.bord = bord;
            this.zetel = zetel;
            this.traan = traan;
            this.begin = begin;
        }
    }

    private static final Map<UUID, Actief> ACTIEF = new ConcurrentHashMap<>();
    /** Per player: the server tick until which a let-go game does not start again by itself. */
    private static final Map<UUID, Long> RUST = new ConcurrentHashMap<>();

    private Traan() {
    }

    static boolean actief(ServerPlayer p) {
        return ACTIEF.containsKey(p.getUUID());
    }

    /** The trail in the world (null: Schelpenstad not known). */
    @Nullable
    static List<Vec3> spoor(ServerLevel g) {
        if (!Plekken.bekend(g, E2SpD2Weg.S)) {
            return null;
        }
        BlockPos o = Plekken.oorsprong(g, E2SpD2Weg.S);
        List<Vec3> uit = new ArrayList<>();
        for (double[] q : SPOOR) {
            uit.add(new Vec3(o.getX() + q[0], o.getY() + q[1], o.getZ() + q[2]));
        }
        return uit;
    }

    /** Starts the tear game: the player on his seat, the companions dried on the table, a tear at Patguh's eye. */
    static boolean start(ServerPlayer p, boolean eerst) {
        ServerLevel g = E2SpD2Weg.guheinde(p);
        List<Vec3> spoor = g == null ? null : spoor(g);
        BlockPos plek = g == null ? null : E2SpD2Weg.marker(g, "droogtafel_speler");
        if (spoor == null || plek == null) {
            return false;
        }
        stop(p, false);
        Entity zetel = display(g, null, 0f);
        Entity traan = display(g, "minecraft:ghast_tear", 0.32f);
        if (zetel == null || traan == null) {
            return false;
        }
        Vec3 zit = Vec3.atBottomCenterOf(plek);
        zetel.snapTo(zit.x, zit.y, zit.z, -115f, 0f);
        E2SpD2Weg.eigen(zetel, p);
        E2SpD2Weg.eigen(traan, p);
        Bord bord = new Bord(spoor);
        Vec3 t0 = bord.plek();
        traan.snapTo(t0.x, t0.y + 0.12, t0.z, 0f, 0f);
        g.addFreshEntity(zetel);
        g.addFreshEntity(traan);
        p.stopRiding();
        p.teleportTo(g, zit.x, zit.y + 0.05, zit.z, java.util.Set.of(), -115f, 30f, true);
        if (!p.startRiding(zetel, true, false)) {
            zetel.discard();
            traan.discard();
            return false;
        }
        BlockPos s = E2SpD2Weg.marker(g, "droogtafel_sponsguh"), pt = E2SpD2Weg.marker(g, "droogtafel_patguh");
        Volgers.opTafel(p, true, s == null ? null : Vec3.atBottomCenterOf(s), pt == null ? null : Vec3.atBottomCenterOf(pt));
        ACTIEF.put(p.getUUID(), new Actief(bord, zetel, traan, g.getServer().getTickCount()));
        RUST.remove(p.getUUID());
        SpAudio.moment(p, "cycloop");
        E2SpD2Weg.titel(p, "traan.titel", "traan.regel");
        if (eerst) {
            E2SpD2Weg.zeg(p, "traan.uitleg");
        } else {
            E2SpD2Weg.zeg(p, "traan.weer");
        }
        return true;
    }

    /** The game does not start again by itself for this many ticks (a scene is about to play). */
    static void wacht(ServerPlayer p, int ticks) {
        RUST.put(p.getUUID(), (long) p.level().getServer().getTickCount() + ticks);
    }

    /** Dried on the table before (flag 8_b): the game starts again when the player stands at the drying table. */
    static void hervat(ServerPlayer p) {
        if (actief(p)) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        Long rust = RUST.get(p.getUUID());
        if (rust != null && nu < rust) {
            return;
        }
        if (E2SpD2Weg.bij(p, "droogtafel_speler", rust != null ? 1.3 : 2.5)) {
            start(p, false);
        }
    }

    @Nullable
    private static Entity display(ServerLevel g, @Nullable String item, float schaal) {
        Entity e = EntityType.ITEM_DISPLAY.create(g, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        if (item != null) {
            D2WegScenes.ding(item, schaal).accept(tag);
            tag.putString("billboard", "center");
        }
        tag.putInt("teleport_duration", 1);
        Nbt.load(e, tag);
        return e;
    }

    /** Every tick of a player in the tear game. */
    static void tick(ServerPlayer p) {
        Actief a = ACTIEF.get(p.getUUID());
        if (a == null) {
            return;
        }
        if (!p.isAlive() || a.traan.isRemoved() || E2SpD2Weg.lijn().stap(p) != E2SpD2Weg.CYCLOOP) {
            stop(p, false);
            return;
        }
        if (p.getVehicle() != a.zetel) {
            stop(p, false);                                            // (he wriggled loose: sneaking)
            RUST.put(p.getUUID(), (long) p.level().getServer().getTickCount() + 60);
            E2SpD2Weg.zeg(p, "traan.los");
            return;
        }
        Input in = p.getLastClientInput();
        double vooruit = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0), links = (in.left() ? 1 : 0) - (in.right() ? 1 : 0);
        double yaw = Math.toRadians(p.getYRot());
        double fx = -Math.sin(yaw) * vooruit + Math.cos(yaw) * links, fz = Math.cos(yaw) * vooruit + Math.sin(yaw) * links;
        double n = Math.hypot(fx, fz);
        if (n > 1) {
            fx /= n;
            fz /= n;
        }
        Bord b = a.bord;
        Uit uit = b.stap(fx, fz);
        ServerLevel g = p.level();
        long t = g.getServer().getTickCount();
        Vec3 plek = b.plek();
        a.traan.teleportTo(plek.x, plek.y + 0.12, plek.z);
        if (t % 4 == 0) {
            Schijn.deeltjes(p, ParticleTypes.DRIPPING_WATER, plek.add(0, 0.2, 0), 1, 0.02, 0.0);
        }
        if (t % 10 == 0) {
            spoor(p, b);
        }
        if (t % 40 == 0 && a.droog < 10) {
            a.droog++;
            if (a.droog == 5) {
                E2SpD2Weg.praat(p, "sponsguh", "traan.droog5");
            } else if (a.droog == 10) {
                E2SpD2Weg.praat(p, "sponsguh", "traan.droog10");
            }
        }
        if (t % 5 == 0) {
            String meter = "■".repeat(a.droog) + "□".repeat(10 - a.droog);
            p.sendOverlayMessage(Component.translatable(E2SpD2Weg.G + "traan.balk", (int) Math.round(b.voortgang() * 100), b.tranen, meter)
                    .withStyle(ChatFormatting.AQUA));
        }
        switch (uit) {
            case VAN_DE_RAND -> {
                Schijn.deeltjes(p, ParticleTypes.CLOUD, plek.add(0, 0.2, 0), 6, 0.1, 0.01);
                Schijn.geluid(p, SoundEvents.FIRE_EXTINGUISH, BlockPos.containing(plek), 0.5f, 1.8f);
                b.nieuw();
                b.tranen++;
                SpAudio.stinger(p, "stinger.mislukt");
                E2SpD2Weg.praat(p, "patguh", "traan.nieuw." + (b.tranen % 3));
                Vec3 o = b.plek();
                Schijn.deeltjes(p, ParticleTypes.FALLING_WATER, o.add(0, 0.4, 0), 3, 0.02, 0.0);
                a.traan.teleportTo(o.x, o.y + 0.12, o.z);
            }
            case BINNEN -> {
                Vec3 in0 = b.p.get(b.p.size() - 1);
                Vec3 sc = new Vec3(in0.x - SPOOR[SPOOR.length - 1][0] + IN_STOPCONTACT[0], in0.y - SPOOR[SPOOR.length - 1][1] + IN_STOPCONTACT[1],
                        in0.z - SPOOR[SPOOR.length - 1][2] + IN_STOPCONTACT[2]);
                a.traan.teleportTo(sc.x, sc.y, sc.z);
                Schijn.deeltjes(p, ParticleTypes.ELECTRIC_SPARK, sc, 20, 0.2, 0.05);
                Schijn.geluid(p, SoundEvents.REDSTONE_TORCH_BURNOUT, BlockPos.containing(sc), 1.0f, 1.0f);
                stop(p, true);
                E2SpD2Weg.titel(p, "traan.binnen", null);
                E2SpD2Weg.naTraan(p);
            }
            default -> {
            }
        }
    }

    /** The wet trail as blue dust for this player (the middle line). */
    private static void spoor(ServerPlayer p, Bord b) {
        for (int i = 1; i < b.p.size(); i++) {
            Vec3 a = b.p.get(i - 1), c = b.p.get(i);
            int stappen = (int) Math.max(1, Math.ceil(a.distanceTo(c) / 0.45));
            for (int k = 0; k < stappen; k++) {
                Vec3 q = a.lerp(c, k / (double) stappen);
                Schijn.deeltjes(p, NAT, q.add(0, 0.06, 0), 1, 0.0, 0.0);
            }
        }
    }

    /** Stops this player's game: off the seat, the tear gone, the companions up again (dried on the table when won: the scene). */
    static void stop(ServerPlayer p, boolean gewonnen) {
        Actief a = ACTIEF.remove(p.getUUID());
        if (a == null) {
            return;
        }
        if (p.getVehicle() == a.zetel) {
            p.stopRiding();
        }
        a.zetel.discard();
        a.traan.discard();
        Volgers.opTafel(p, false, null, null);
        if (!gewonnen) {
            SpAudio.stopMoment(p, 20);
        }
    }

    /** (dev) the tear as if it rolled into the socket. */
    static boolean win(ServerPlayer p) {
        if (!actief(p)) {
            return false;
        }
        stop(p, true);
        E2SpD2Weg.naTraan(p);
        return true;
    }
}
