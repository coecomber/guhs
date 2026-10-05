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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
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
 * ever ({@link Bescherming}). Register from your Feature.register (common code):
 * <pre>
 * Sluiers.registreer("guhvendel", 6, p -&gt; RingFeature.H1.klaar(p));
 * </pre>
 * The wall stands around the box of the structure's pieces plus {@code rand} blocks, from the bottom of the world to the
 * top (so nobody drops in from above either). Python: {@code verhaal_motor.sluier(h, structuur)} puts the structure on
 * the list of the live map ({@code data/guhs/kaart/verborgen.json}).
 */
public final class Sluiers {
    /** The message comes at most once per this many ticks. */
    public static final int BERICHT_TICKS = 100;
    /** A player hears about the walls within this many blocks (the client draws the smoke up to 48 blocks away). */
    public static final int ZICHT = 96;
    /** How deep (blocks) a player may step in and only gets a shove; deeper they are put back outside. */
    public static final double DUW_DIEPTE = 1.5;
    public static final String BERICHT = "quest.guhs.verhaal.sluier", BESCHERMD = "gui.guhs.verhaal.beschermd";

    private record Sluier(String structuur, int rand, Predicate<ServerPlayer> open) {
    }

    /** A wall in the world: the structure and the block bounds (rand included) it stands around, at any height. */
    public record Zone(String structuur, int x0, int z0, int x1, int z1) {
        public boolean binnen(double x, double z) {
            return x >= x0 && x < x1 + 1 && z >= z0 && z < z1 + 1;
        }

        public boolean binnen(BlockPos pos) {
            return pos.getX() >= x0 && pos.getX() <= x1 && pos.getZ() >= z0 && pos.getZ() <= z1;
        }

        /** How far (blocks) this spot is inside, measured to the nearest wall (negative: outside). */
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
        synchronized (ALLE) {
            ALLE.put(structuur, new Sluier(structuur, Math.max(0, rand), open));
        }
        Bescherming.registreer(structuur, rand);
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
        return dicht(p, p.level(), pos.getX() + 0.5, pos.getZ() + 0.5) == null;
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
    private static Zone dicht(ServerPlayer p, Level level, double x, double z) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        if (zones == null || zones.isEmpty()) {
            return null;
        }
        for (Zone zone : zones.values()) {
            if (zone.binnen(x, z) && !open(p, zone.structuur())) {
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
        int rand = s == null ? 0 : s.rand();
        Zone zone = new Zone(structuur, doos.minX() - rand, doos.minZ() - rand, doos.maxX() + rand, doos.maxZ() + rand);
        Map<String, Zone> zones = ZONES.computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>());
        zones.put(structuur + "#" + doos.minX() + "," + doos.minZ(), zone);
        return zone;
    }

    /** (tests) forgets the walls of this structure in this dimension. */
    public static void wisPlekken(ServerLevel level, String structuur) {
        Map<String, Zone> zones = ZONES.get(level.dimension());
        if (zones != null) {
            zones.values().removeIf(z -> z.structuur().equals(structuur));
        }
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

    /** Looks for copies of the sluier structures in the loaded chunks around the player (their own chunk included). */
    private static void ontdek(ServerPlayer p) {
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
                            BoundingBox b = ss.getBoundingBox();
                            int rand = e.getValue().rand();
                            zones.put(key, new Zone(e.getValue().structuur(), b.minX() - rand, b.minZ() - rand, b.maxX() + rand, b.maxZ() + rand));
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
        CompoundTag data = new CompoundTag();
        ListTag lijst = new ListTag();
        for (Zone z : nu) {
            CompoundTag t = new CompoundTag();
            t.putString("S", z.structuur());
            t.putInt("X0", z.x0());
            t.putInt("Z0", z.z0());
            t.putInt("X1", z.x1());
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
        Zone zone = dicht(p, p.level(), p.getX(), p.getZ());
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
            Vec3 naar = b != null && b.dim() == p.level().dimension() && dicht(p, p.level(), b.plek().x, b.plek().z) == null
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
    // nobody breaks or places there (until feature.wereld.Bescherming does it itself)
    // =====================================================================================================================

    private static boolean beschermd(Level level, BlockPos pos) {
        return !level.isClientSide() && !Bescherming.beschermd(level, pos) && zone(level, pos) != null;
    }

    private static boolean geweigerd(@Nullable Entity wie, Level level, BlockPos pos) {
        if (!beschermd(level, pos)) {
            return false;
        }
        if (wie instanceof ServerPlayer p) {
            if (passeert(p)) {
                return false;
            }
            p.sendOverlayMessage(Component.translatable(BESCHERMD).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        Player speler = event.getPlayer();
        if (geweigerd(speler, speler.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && geweigerd(event.getEntity(), level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> beschermd(level, pos));
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        ZONES.clear();
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
