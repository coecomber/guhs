package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.network.ModNetworking;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.7): "Guhdalfs sluier": a wall of swirling smoke around a story structure, per
 * player. For a player for whom {@code open} is false: they see the smoke (client: client.SluierRook), stepping in gently
 * puts them back outside with "Guhdalf vindt dat je hier nog niet aan toe bent, njeg" (at most once per 5 s, never
 * damage, a mount comes along), the structure is not in their Superkompas and can't be a {@link Doel}. When {@code open}
 * turns true the smoke dissolves for that player. Spectators and creative operators pass. Nobody breaks or places there,
 * ever ({@link Bescherming}: the very same box, see below). Register from your Feature.register (common code):
 * <pre>
 * Sluiers.registreer("guhvendel", 6, p -&gt; RingFeature.H1.klaar(p));
 * </pre>
 * The sluier is a box of smoke: the bounding box of the structure's pieces plus {@code rand} blocks on every side (also
 * above and below: a mine under a plain does not close the plain, and whoever flies over looks at a lid of smoke). Python:
 * {@code verhaal_motor.sluier(h, structuur)} puts the structure on the list of the live map
 * ({@code data/guhs/kaart/verborgen.json}).
 * <p>
 * bbq2 (ring-kern, CONTRACT_130 13.9): one box for everything. The box of smoke IS the protected box
 * ({@link Bescherming#registreerDoos}, {@link Bescherming#zetDoos}): no gaps between the pieces, quest exceptions and
 * machines see the same blocks. And what is inside does not leak to a player for whom the sluier is closed:
 * <ul>
 *   <li>creatures: {@link #magZien(ServerPlayer, Entity)} is asked by the entity tracker (mixin RingZichtMixin), so that
 *       player's game never hears of an entity inside (no model through the smoke, no name tag, no sound of it, and a boss
 *       bar that follows who sees the boss never starts);</li>
 *   <li>boss bars that are kept by hand: {@link #balk} adds and removes the right players;</li>
 *   <li>sounds played at a spot inside: {@link #magHoren} is asked when the server sends them out (mixin
 *       RingSluierGeluidMixin).</li>
 * </ul>
 */
public final class Sluiers {
    /** The message comes at most once per this many ticks. */
    public static final int BERICHT_TICKS = 100;
    /** A player hears about the walls within this many blocks (the client draws the smoke up to 48 blocks away). */
    public static final int ZICHT = 96;
    /** How deep (blocks) a player may step in and only gets a shove; deeper they are put back outside. */
    public static final double DUW_DIEPTE = 1.5;
    /** (BESCHERMD: the old text of the sluier's own protection; Bescherming says gui.guhs.wereld.beschermd now.) */
    public static final String BERICHT = "quest.guhs.verhaal.sluier", BESCHERMD = "gui.guhs.verhaal.beschermd";

    private record Sluier(String structuur, int rand, Predicate<ServerPlayer> open) {
    }

    /** A sluier in the world: the structure and the block bounds (rand included) of its box of smoke. */
    public record Zone(String structuur, int x0, int y0, int z0, int x1, int y1, int z1) {
        /** The box of a structure's pieces with rand blocks around it. */
        public static Zone van(String structuur, BoundingBox doos, int rand) {
            return new Zone(structuur, doos.minX() - rand, doos.minY() - rand, doos.minZ() - rand, doos.maxX() + rand, doos.maxY() + rand, doos.maxZ() + rand);
        }

        public boolean binnen(double x, double y, double z) {
            return x >= x0 && x < x1 + 1 && y >= y0 && y < y1 + 1 && z >= z0 && z < z1 + 1;
        }

        public boolean binnen(BlockPos pos) {
            return pos.getX() >= x0 && pos.getX() <= x1 && pos.getY() >= y0 && pos.getY() <= y1 && pos.getZ() >= z0 && pos.getZ() <= z1;
        }

        /** How far (blocks) this spot is inside, measured sideways to the nearest wall (negative: outside). */
        public double diepte(double x, double z) {
            return Math.min(Math.min(x - x0, x1 + 1 - x), Math.min(z - z0, z1 + 1 - z));
        }

        /** The way out from here: straight to the nearest wall. */
        public Vec3 naarBuiten(double x, double z) {
            double w = x - x0, e = x1 + 1 - x, n = z - z0, s = z1 + 1 - z, m = Math.min(Math.min(w, e), Math.min(n, s));
            return m == w ? new Vec3(-1, 0, 0) : m == e ? new Vec3(1, 0, 0) : m == n ? new Vec3(0, 0, -1) : new Vec3(0, 0, 1);
        }

        double afstand(double x, double z) {
            double dx = Math.max(Math.max(x0 - x, 0), x - (x1 + 1)), dz = Math.max(Math.max(z0 - z, 0), z - (z1 + 1));
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private record Buiten(ResourceKey<Level> dim, Vec3 plek, float yaw) {
    }

    private static final Map<String, Sluier> ALLE = new LinkedHashMap<>();
    /** The walls found so far, per dimension (key: structure@start chunk, or structure#n for {@link #zetPlek}). */
    private static final Map<ResourceKey<Level>, Map<String, Zone>> ZONES = new ConcurrentHashMap<>();
    private static final Map<UUID, Buiten> BUITEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BERICHT_OP = new ConcurrentHashMap<>(), DUW_OP = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Zone>> GESTUURD = new ConcurrentHashMap<>();

    /**
     * A sluier around every copy of this structure (a guhs structure id without namespace); rand = blocks around the
     * structure's box; open = may this player in (their story is there)? Also protects the structure and hides it from
     * the Superkompas until open.
     */
    public static void registreer(String structuur, int rand, Predicate<ServerPlayer> open) {
        int r = Math.max(0, Math.min(Bescherming.MAX_RAND, rand));   // (the same cap as the protection: one box)
        synchronized (ALLE) {
            ALLE.put(structuur, new Sluier(structuur, r, open));
        }
        Bescherming.registreerDoos(structuur, r);
    }

    @Nullable
    private static Sluier van(String structuur) {
        synchronized (ALLE) {
            return ALLE.get(structuur);
        }
    }

    /** Every structure with a sluier. */
    public static List<String> structuren() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.keySet());
        }
    }

    /** May this player into that structure (true when it has no sluier)? */
    public static boolean open(ServerPlayer p, String structuur) {
        Sluier s = van(structuur);
        return s == null || s.open().test(p);
    }

    /** Is the structure still hidden for this player (the Superkompas lists and {@link Doelen} ask this)? */
    public static boolean isVerborgen(ServerPlayer p, String structuur) {
        return !open(p, structuur);
    }

    /** May this player be at pos (false: it is behind a sluier that is closed for them)? */
    public static boolean magBinnen(ServerPlayer p, BlockPos pos) {
        return dicht(p, p.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) == null;
    }

    /** Spectators and creative operators walk through. */
    public static boolean passeert(ServerPlayer p) {
        return p.isSpectator() || p.isCreative() && p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** The wall this spot is behind, whoever asks (null: none). */
    @Nullable
    public static Zone zone(Level level, BlockPos pos) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        if (zones != null) {
            for (Zone z : zones.values()) {
                if (z.binnen(pos)) {
                    return z;
                }
            }
        }
        return null;
    }

    /** The closed wall (for this player) around this spot, or null. */
    @Nullable
    private static Zone dicht(ServerPlayer p, Level level, double x, double y, double z) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        if (zones == null || zones.isEmpty()) {
            return null;
        }
        for (Zone zone : zones.values()) {
            if (zone.binnen(x, y, z) && !open(p, zone.structuur())) {
                return zone;
            }
        }
        return null;
    }

    /**
     * A wall around this box (plus the structure's rand) in this dimension, without a generated structure: for things
     * placed by code, and for the game tests. Returns the zone.
     */
    public static Zone zetPlek(ServerLevel level, String structuur, BoundingBox doos) {
        Sluier s = van(structuur);
        Zone zone = Zone.van(structuur, doos, s == null ? 0 : s.rand());
        Map<String, Zone> zones = ZONES.computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>());
        zones.put(structuur + "#" + doos.minX() + "," + doos.minZ(), zone);
        // (the same box is protected: break / place / explosions, the structure's quest exceptions, machines)
        Bescherming.zetDoos(level, structuur, new BoundingBox(zone.x0(), zone.y0(), zone.z0(), zone.x1(), zone.y1(), zone.z1()));
        return zone;
    }

    /** (tests) forgets the walls of this structure in this dimension. */
    public static void wisPlekken(ServerLevel level, String structuur) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        if (zones != null) {
            zones.values().removeIf(z -> z.structuur().equals(structuur));
        }
        Bescherming.wisDozen(level, structuur);
    }

    /** (tests) forgets a registration. */
    static void vergeet(String structuur) {
        synchronized (ALLE) {
            ALLE.remove(structuur);
        }
    }

    // =====================================================================================================================
    // finding the structures, keeping players out
    // =====================================================================================================================

    /** The walls found so far around this structure in this dimension. */
    public static List<Zone> zones(Level level, String structuur) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        return zones == null ? List.of() : zones.values().stream().filter(z -> z.structuur().equals(structuur)).toList();
    }

    /** Looks for copies of the sluier structures in the loaded chunks around the player (their own chunk included). */
    static void ontdek(ServerPlayer p) {
        List<Sluier> alle;
        synchronized (ALLE) {
            alle = List.copyOf(ALLE.values());
        }
        if (alle.isEmpty()) {
            return;
        }
        ServerLevel level = p.level();
        var register = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Map<Structure, Sluier> zoek = new java.util.IdentityHashMap<>();
        for (Sluier s : alle) {
            Structure st = register.getValue(Guhs.id(s.structuur()));
            if (st != null) {
                zoek.put(st, s);
            }
        }
        if (zoek.isEmpty()) {
            return;
        }
        ChunkPos c = p.chunkPosition();
        Map<String, Zone> zones = ZONES.computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>());
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(c.x() + dx, c.z() + dz);
                if (chunk == null) {
                    continue;
                }
                Map<Structure, LongSet> refs = chunk.getAllReferences();
                if (refs.isEmpty()) {
                    continue;
                }
                for (Map.Entry<Structure, Sluier> e : zoek.entrySet()) {
                    LongSet starts = refs.get(e.getKey());
                    if (starts == null) {
                        continue;
                    }
                    for (long start : starts) {
                        String key = e.getValue().structuur() + "@" + start;
                        if (zones.containsKey(key)) {
                            continue;
                        }
                        ChunkAccess bij = level.getChunk(ChunkPos.getX(start), ChunkPos.getZ(start), ChunkStatus.STRUCTURE_STARTS);
                        StructureStart ss = bij.getStartForStructure(e.getKey());
                        if (ss != null && ss.isValid()) {
                            zones.put(key, Zone.van(e.getValue().structuur(), ss.getBoundingBox(), e.getValue().rand()));
                        }
                    }
                }
            }
        }
    }

    /** Tells the client which smoke walls stand near the player (only when that changed). */
    private static void stuurRook(ServerPlayer p) {
        List<Zone> nu = new ArrayList<>();
        Map<String, Zone> zones = ZONES.get(p.level().dimension());
        if (zones != null) {
            for (Zone z : zones.values()) {
                if (z.afstand(p.getX(), p.getZ()) <= ZICHT && !open(p, z.structuur())) {
                    nu.add(z);
                }
            }
        }
        List<Zone> oud = GESTUURD.get(p.getUUID());
        if (nu.equals(oud) || oud == null && nu.isEmpty()) {
            return;
        }
        GESTUURD.put(p.getUUID(), nu);
        kijkOpnieuw(p);   // (a wall came or went for this player: so did the creatures behind it)
        CompoundTag data = new CompoundTag();
        ListTag lijst = new ListTag();
        for (Zone z : nu) {
            CompoundTag t = new CompoundTag();
            t.putString("S", z.structuur());
            t.putInt("X0", z.x0());
            t.putInt("Y0", z.y0());
            t.putInt("Z0", z.z0());
            t.putInt("X1", z.x1());
            t.putInt("Y1", z.y1());
            t.putInt("Z1", z.z1());
            lijst.add(t);
        }
        data.put("Zones", lijst);
        ModNetworking.sendTo(p, new VerhaalPayloads.Rook(data));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if ((p.tickCount + p.getId()) % 20 == 0) {
            ontdek(p);
            stuurRook(p);
        }
        houdBuiten(p);
    }

    /** (also for the tests) one tick of the wall for this player: remembers where they were outside, sends them back out. */
    static void houdBuiten(ServerPlayer p) {
        Zone zone = dicht(p, p.level(), p.getX(), p.getY(), p.getZ());
        if (zone == null) {
            if (p.onGround() || p.isPassenger() || !BUITEN.containsKey(p.getUUID())) {
                BUITEN.put(p.getUUID(), new Buiten(p.level().dimension(), p.position(), p.getYRot()));
            }
            return;
        }
        if (passeert(p) || Vast.is(p) || !p.isAlive()) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        double diepte = zone.diepte(p.getX(), p.getZ());
        Long geduwd = DUW_OP.get(p.getUUID());
        if (diepte <= DUW_DIEPTE && Duwtje.mag(p)) {
            if (geduwd == null || nu - geduwd >= 6) {
                DUW_OP.put(p.getUUID(), nu);
                Duwtje.duw(p, zone.naarBuiten(p.getX(), p.getZ()), 0.55);
            }
        } else {
            Buiten b = BUITEN.get(p.getUUID());
            Vec3 naar = b != null && b.dim() == p.level().dimension() && dicht(p, p.level(), b.plek().x, b.plek().y, b.plek().z) == null
                    && b.plek().distanceToSqr(p.position()) < 48 * 48 ? b.plek() : rand(p, zone);
            Duwtje.terug(p, p.level().dimension(), naar, p.getYRot());
        }
        Long gezegd = BERICHT_OP.get(p.getUUID());
        if (gezegd == null || nu - gezegd >= BERICHT_TICKS) {
            BERICHT_OP.put(p.getUUID(), nu);
            p.sendOverlayMessage(Component.translatable(BERICHT).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** A spot to stand on just outside the nearest wall (for a player who never was outside: logged in or teleported inside). */
    private static Vec3 rand(ServerPlayer p, Zone zone) {
        Vec3 uit = zone.naarBuiten(p.getX(), p.getZ());
        double d = zone.diepte(p.getX(), p.getZ()) + 1.5;
        double x = p.getX() + uit.x * d, z = p.getZ() + uit.z * d;
        ServerLevel level = p.level();
        AABB doos = p.getBoundingBox().move(x - p.getX(), 0, z - p.getZ());
        for (int i = 0; i <= 24; i++) {
            int dy = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
            BlockPos onder = BlockPos.containing(x, p.getY() + dy - 0.2, z);
            if (level.getBlockState(onder).blocksMotion() && level.noCollision(p, doos.move(0, dy, 0))) {
                return new Vec3(x, Math.floor(p.getY() + dy), z);
            }
        }
        return new Vec3(x, p.getY(), z);
    }

    // =====================================================================================================================
    // what is inside does not leak out (CONTRACT_130 13.9)
    // =====================================================================================================================

    /** The closed wall (for this player) around this spot in this level, or null (also null for who {@link #passeert}). */
    @Nullable
    private static Zone dichtVoor(ServerPlayer p, Level level, double x, double y, double z) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        if (zones == null || zones.isEmpty() || p.level() != level) {
            return null;
        }
        Zone zone = dicht(p, level, x, y, z);
        return zone == null || passeert(p) ? null : zone;
    }

    /**
     * May this player's game know about this entity? False for a creature (anything but a player) that is inside a sluier
     * that is closed for the player. Asked by the entity tracker for every (entity, player) pair whenever the player or the
     * entity moves, so a boss, an NPC or a Mika inside is simply not there for who is not that far in the story: no model
     * through the smoke, no name tag, no sounds of the entity, and {@code startSeenByPlayer} (boss bars!) never runs.
     */
    public static boolean magZien(ServerPlayer p, Entity e) {
        return e instanceof Player || dichtVoor(p, e.level(), e.getX(), e.getY(), e.getZ()) == null;
    }

    /** May this player hear / see something that happens at this spot (false: it is inside a sluier closed for them)? */
    public static boolean magHoren(ServerPlayer p, Level level, double x, double y, double z) {
        return dichtVoor(p, level, x, y, z) == null;
    }

    /** Is there any sluier at all in this dimension (the cheap question before {@link #magHoren} per player)? */
    public static boolean heeftZones(ResourceKey<Level> dim) {
        Map<String, Zone> zones = ZONES.get(dim);
        return zones != null && !zones.isEmpty();
    }

    /**
     * Keeps a boss bar that is managed by hand right: every player within {@code straal} blocks of the boss who may see it
     * ({@link #magZien}) is on the bar, everybody else is taken off. Call it about once a second from the tick of the boss.
     * (A bar that only follows {@code startSeenByPlayer} / {@code stopSeenByPlayer} needs nothing: the tracker asks
     * {@link #magZien} itself.)
     */
    public static void balk(net.minecraft.server.level.ServerBossEvent balk, Entity baas, double straal) {
        if (!(baas.level() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer p : List.copyOf(balk.getPlayers())) {
            if (p.level() != level || p.distanceToSqr(baas) > straal * straal || !magZien(p, baas)) {
                balk.removePlayer(p);
            }
        }
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(baas) <= straal * straal && magZien(p, baas)) {
                balk.addPlayer(p);
            }
        }
    }

    /** The sluier opened or closed for this player: the tracker looks again at once (else at their next step). */
    private static void kijkOpnieuw(ServerPlayer p) {
        if (p.connection != null) {
            p.level().getChunkSource().move(p);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        ZONES.clear();
        Bescherming.wisDozen();
        BUITEN.clear();
        BERICHT_OP.clear();
        DUW_OP.clear();
        GESTUURD.clear();
    }

    static void vergeet(UUID speler) {
        BUITEN.remove(speler);
        BERICHT_OP.remove(speler);
        DUW_OP.remove(speler);
        GESTUURD.remove(speler);
    }

    private Sluiers() {
    }
}
