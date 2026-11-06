package nl.juiced.guhs.feature.vadsrijk.vriendjes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.kern.veilig.Losknuffelen;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Vahoegnir;
import nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcApi;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;

/**
 * N-EXTRA: where the pink Vahoegnir is after zone 3 (for players with {@code zone3_klaar}).
 * <ul>
 *   <li><b>Nest:</b> he rests at the nest POI ({@link #NEST_POI}, fallback {@link #NEST_POI_2}; by id, the zone-3 map may move it)
 *       while such a player is within {@link #NEST_KIJK} blocks, unless the Vahoegnir boss is there (a Droomsteen memory fight or
 *       the ending scene): then the nest is the story's.</li>
 *   <li><b>Visits:</b> every 20-40 min (per player, in memory) a player with zone3_klaar in zone 1 or 2 who is not fighting,
 *       not in a boss arena and not in a cutscene gets a visit: he flies in and lands on open, flat ground near the nearest town
 *       square or campfire POI ({@link #ankers()}), stays 60-90 s and flies off. No safe spot = try again in 2 min.</li>
 * </ul>
 */
public final class RozeDraakBeheer {
    public static final String NEST_POI = "nest_eierkuil", NEST_POI_2 = "arena_vahoegnir", NEST_RAND_POI = "nest_rand";
    public static final double NEST_KIJK = 80;
    public static final int MIN_WACHT = 20 * 60 * 20, MAX_WACHT = 40 * 60 * 20, OPNIEUW = 20 * 120;
    /** Landing: an open, flat patch within this radius (blocks) around the landing point. */
    public static final int LAND_STRAAL = 6;

    /** Dev: players who see the pink dragon without zone3_klaar. */
    static final Set<UUID> DEV_ZIEN = new HashSet<>();
    private static final Map<UUID, Long> VOLGEND_BEZOEK = new HashMap<>();
    @Nullable
    private static RozeDraak nest;
    private static final List<RozeDraak> BEZOEKEN = new ArrayList<>();

    private RozeDraakBeheer() {
    }

    /** Does this player see the pink dragon (zone 3 done, or dev)? */
    public static boolean ziet(ServerPlayer p) {
        return DEV_ZIEN.contains(p.getUUID()) || Z3NpcApi.zone3Klaar(p);
    }

    static void wis() {
        nest = null;
        BEZOEKEN.clear();
        VOLGEND_BEZOEK.clear();
    }

    // ---- the nest --------------------------------------------------------------------------------------------------------------

    @Nullable
    public static BlockPos nestPlek() {
        BlockPos b = Z3WereldPlekken.plek(NEST_POI);
        return b != null ? b : Z3WereldPlekken.plek(NEST_POI_2);
    }

    @Nullable
    public static RozeDraak nest() {
        return nest != null && !nest.isRemoved() ? nest : null;
    }

    /** Is the story's Vahoegnir (the boss: a fight or his ending) at the nest right now? */
    static boolean bazInNest(ServerLevel sl, Vec3 m) {
        return !sl.getEntitiesOfClass(Vahoegnir.class, new net.minecraft.world.phys.AABB(m, m).inflate(96)).isEmpty();
    }

    static void tickNest(ServerLevel sl) {
        BlockPos b = nestPlek();
        if (b == null) {
            return;
        }
        Vec3 m = Vec3.atBottomCenterOf(b);
        boolean kijkers = false;
        for (ServerPlayer p : sl.players()) {
            if (ziet(p) && p.position().distanceTo(m) < NEST_KIJK) {
                kijkers = true;
                break;
            }
        }
        RozeDraak d = nest();
        boolean moet = kijkers && nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, b) && !bazInNest(sl, m);   // U-DUBBEL (was isLoaded)
        if (d != null && !moet) {
            d.discard();
            nest = null;
        } else if (d == null && moet) {
            nest = maakNest(sl, m);
        }
    }

    @Nullable
    static RozeDraak maakNest(ServerLevel sl, Vec3 m) {
        RozeDraak d = DraakVriendjes.ROZE_DRAAK.get().create(sl, EntitySpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        d.soort = RozeDraak.Soort.NEST;
        d.plek = m;
        BlockPos rand = Z3WereldPlekken.plek(NEST_RAND_POI);
        float yaw = 0;
        if (rand != null) {   // he faces the way in (whoever climbs up is greeted)
            Vec3 r = Vec3.atBottomCenterOf(rand).subtract(m);
            if (r.horizontalDistanceSqr() > 1) {
                yaw = (float) Math.toDegrees(Math.atan2(-r.x, r.z));
            }
        }
        d.rustYaw = yaw;
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(d, "draak:nest");   // U-DUBBEL
        d.snapTo(m.x, m.y, m.z, yaw, 0);
        sl.addFreshEntity(d);
        DraakVriendjes.volg(d);
        return d;
    }

    // ---- visits ------------------------------------------------------------------------------------------------------------------

    /** The anchors for a visit: town squares and campfires of zones 1 and 2, by POI id. */
    static List<BlockPos> ankers() {
        List<BlockPos> l = new ArrayList<>();
        for (var e : WereldPlekken.ALLE.entrySet()) {
            if (anker(e.getKey())) {
                l.add(e.getValue());
            }
        }
        for (var e : Z2WereldPlekken.ALLE.entrySet()) {
            if (anker(e.getKey())) {
                l.add(e.getValue());
            }
        }
        return l;
    }

    static boolean anker(String id) {
        return id.equals("plein") || id.endsWith("_plein") || id.startsWith("kampvuur_");
    }

    /** May this player get a visit now? (zone 3 done, zone 1/2, not fighting/arena/cutscene, no visit of his already near). */
    public static boolean magBezoek(ServerPlayer p) {
        if (!ziet(p) || !Vadsrijk.in(p) || p.isSpectator() || p.isSleeping() || !(p.level() instanceof ServerLevel sl)) {
            return false;
        }
        if (Z3WereldPlekken.inZone3(p.getX(), p.getZ()) || RijkScenes.bezig(p)) {
            return false;
        }
        if (Arenas.op(p.getX(), p.getY(), p.getZ()) != null || Losknuffelen.inGevecht(sl, p.position())) {
            return false;
        }
        int n = p.tickCount;
        if (n - p.getLastHurtByMobTimestamp() < 400 || n - p.getLastHurtMobTimestamp() < 400) {
            return false;   // (just fought)
        }
        for (RozeDraak d : BEZOEKEN) {
            if (!d.isRemoved() && d.level() == p.level() && d.plek.distanceTo(p.position()) < 200) {
                return false;
            }
        }
        return true;
    }

    /** Is this patch good to land on: flat (within 1.5 blocks), open sky, solid non-liquid ground, no leaves/roofs? */
    static boolean landbaar(ServerLevel sl, int x, int z) {
        if (!sl.hasChunkAt(new BlockPos(x, 0, z))) {
            return false;
        }
        int midden = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int dx = -LAND_STRAAL; dx <= LAND_STRAAL; dx += 3) {
            for (int dz = -LAND_STRAAL; dz <= LAND_STRAAL; dz += 3) {
                if (!sl.hasChunkAt(new BlockPos(x + dx, 0, z + dz))) {
                    return false;
                }
                int h = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz);
                int zonder = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (Math.abs(h - midden) > 1 || h != zonder) {
                    return false;   // a slope, a wall, a roof or a tree
                }
                BlockPos grond = new BlockPos(x + dx, h - 1, z + dz);
                BlockState s = sl.getBlockState(grond);
                if (!s.getFluidState().isEmpty() || s.getCollisionShape(sl, grond).isEmpty()) {
                    return false;   // water, lava, or only grass/flowers on top of something soft
                }
            }
        }
        return true;
    }

    /** The landing spot near the anchor nearest to the player (8-60 blocks from the player), or null. */
    @Nullable
    static Vec3 landplek(ServerLevel sl, ServerPlayer p) {
        BlockPos best = null;
        double bd = 140 * 140;
        for (BlockPos a : ankers()) {
            double d = a.distToCenterSqr(p.getX(), a.getY(), p.getZ());
            if (d < bd) {
                bd = d;
                best = a;
            }
        }
        if (best == null) {
            return null;
        }
        var r = sl.getRandom();
        for (int poging = 0; poging < 40; poging++) {
            double hoek = r.nextDouble() * Math.PI * 2, afstand = 10 + r.nextDouble() * 22;
            int x = best.getX() + (int) Math.round(Math.cos(hoek) * afstand), z = best.getZ() + (int) Math.round(Math.sin(hoek) * afstand);
            Vec3 v = new Vec3(x + 0.5, sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5);
            double tot = v.distanceTo(p.position());
            if (tot < 8 || tot > 60 || Math.abs(v.y - best.getY()) > 12 || !landbaar(sl, x, z)) {
                continue;
            }
            if (!sl.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(v, v).inflate(5)).isEmpty()) {
                continue;
            }
            return v;
        }
        return null;
    }

    /** A spline from far and high to the landing spot, at least 6 blocks above the ground all the way (except the touchdown). */
    @Nullable
    static List<Vec3> aanvlucht(ServerLevel sl, Vec3 land, boolean terug) {
        var r = sl.getRandom();
        for (int poging = 0; poging < 10; poging++) {
            double hoek = r.nextDouble() * Math.PI * 2;
            Vec3 uit = new Vec3(Math.cos(hoek), 0, Math.sin(hoek));
            List<Vec3> pad = List.of(land.add(uit.scale(130)).add(0, 55, 0), land.add(uit.scale(60)).add(0, 32, 0),
                    land.add(uit.scale(18)).add(0, 9, 0), land.add(uit.scale(4)).add(0, 2, 0), land);
            if (terug) {
                pad = pad.reversed();
            }
            if (vrij(sl, pad)) {
                return pad;
            }
        }
        return null;
    }

    static boolean vrij(ServerLevel sl, List<Vec3> pad) {
        for (int i = 0; i <= 40; i++) {
            double f = i / 40.0;
            Vec3 v = DraakVlucht.punt(pad, f);
            Vec3 land = pad.get(0).y < pad.get(pad.size() - 1).y ? pad.get(0) : pad.get(pad.size() - 1);
            if (v.distanceTo(land) < 10) {
                continue;   // the touchdown itself
            }
            int x = (int) Math.floor(v.x), z = (int) Math.floor(v.z);
            if (!sl.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;   // far away and unloaded: nobody sees it there
            }
            if (v.y < sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 6) {
                return false;
            }
        }
        return true;
    }

    /** Starts a visit for this player (null = no safe spot). */
    @Nullable
    public static RozeDraak bezoek(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return null;
        }
        Vec3 land = landplek(sl, p);
        if (land == null) {
            return null;
        }
        List<Vec3> heen = aanvlucht(sl, land, false), terug = aanvlucht(sl, land, true);
        if (heen == null || terug == null) {
            return null;
        }
        RozeDraak d = DraakVriendjes.ROZE_DRAAK.get().create(sl, EntitySpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        d.soort = RozeDraak.Soort.BEZOEK;
        d.gast = p.getUUID();
        d.plek = land;
        d.heen = heen;
        d.terug = terug;
        d.blijf = 1200 + sl.getRandom().nextInt(600);
        d.anim("vlieg");
        Vec3 a = heen.get(0);
        d.snapTo(a.x, a.y, a.z, 0, 0);
        sl.addFreshEntity(d);
        BEZOEKEN.add(d);
        p.sendSystemMessage(Component.translatable("vadsrijk.vriendjes.bezoek").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        Vec3 r = a.subtract(p.position()).normalize().scale(16);
        sl.playSound(null, p.getX() + r.x, p.getY() + r.y, p.getZ() + r.z, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 1.2f, 1.7f);
        return d;
    }

    static void tickBezoeken(ServerLevel sl, long nu) {
        BEZOEKEN.removeIf(RozeDraak::isRemoved);
        if (nu % 100 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (!ziet(p)) {
                continue;
            }
            long volgend = VOLGEND_BEZOEK.computeIfAbsent(p.getUUID(), u -> nu + wacht(sl));
            if (nu < volgend) {
                continue;
            }
            if (magBezoek(p) && bezoek(p) != null) {
                VOLGEND_BEZOEK.put(p.getUUID(), nu + wacht(sl));
            } else {
                VOLGEND_BEZOEK.put(p.getUUID(), nu + OPNIEUW);
            }
        }
    }

    private static long wacht(ServerLevel sl) {
        return MIN_WACHT + sl.getRandom().nextInt(MAX_WACHT - MIN_WACHT);
    }

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long nu = sl.getGameTime();
        if (nu % 40 == 0) {
            tickNest(sl);
        }
        tickBezoeken(sl, nu);
    }

    /** Dev: the next visit timer for this player now. */
    static void bezoekNu(ServerPlayer p) {
        VOLGEND_BEZOEK.put(p.getUUID(), 0L);
    }
}
