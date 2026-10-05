package nl.juiced.guhs.feature.wereld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;

/**
 * bbq2: the new quest buildings stay whole, so every player on a server finds them the way they were built.
 * <ul>
 *   <li>{@link #registreer}: nobody breaks, places, blows up, pushes (pistons) or floods anything inside the boxes of the
 *       pieces of a structure (+ a rim of blocks). Opening chests and doors, pulling levers and talking go on as usual.
 *       Players in creative mode may change it (like the other protected guh buildings). Fire and fluids from outside are
 *       kept out by {@link Protected}.</li>
 *   <li>{@link #uitzondering}: the quest blocks a player may change after all (a fire to light, a block to brush away); the
 *       owner puts them back with {@link Herstel}.</li>
 *   <li>{@link #magWijzigen}: for machines that change the world on behalf of a player (Knabbelaar, Neerzetter, Oogster).</li>
 * </ul>
 * A spot is looked up through the structure references of its (loaded) chunk, so this costs next to nothing where no
 * registered structure is near. The boxes of a copy are kept once they were read.
 */
public final class Bescherming {
    private record Regel(String structuur, int rand) {
    }

    /** The boxes of one copy, grown by the rim, and the box around all of them. */
    private record Zone(String structuur, BoundingBox alles, List<BoundingBox> stukken) {
        boolean bevat(BlockPos pos) {
            if (!alles.isInside(pos)) {
                return false;
            }
            for (BoundingBox b : stukken) {
                if (b.isInside(pos)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final Map<String, Regel> REGELS = new ConcurrentHashMap<>();
    private static final Map<String, List<BiPredicate<ServerPlayer, BlockPos>>> UITZONDERINGEN = new ConcurrentHashMap<>();
    private static volatile int grootsteRand;
    /** (not saved) per level: the registered structures of its registries, and the zone per (structure, start chunk). */
    private static final Map<ServerLevel, Map<Structure, Regel>> STRUCTUREN = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerLevel, Map<Structure, Map<Long, Zone>>> ZONES = Collections.synchronizedMap(new WeakHashMap<>());

    private Bescherming() {
    }

    // --- registering -----------------------------------------------------------------------------------------------------

    /**
     * Nobody breaks, places, explodes or floods inside the boxes of the pieces (+ {@code rand} blocks, at most 64) of every
     * copy of {@code guhs:<structuur>}; the message is gui.guhs.wereld.beschermd. From Feature.register.
     */
    public static void registreer(String structuur, int rand) {
        REGELS.put(structuur, new Regel(structuur, Math.max(0, Math.min(64, rand))));
        grootsteRand = REGELS.values().stream().mapToInt(Regel::rand).max().orElse(0);
        STRUCTUREN.clear();
        ZONES.clear();
    }

    /** The quest blocks of {@code guhs:<structuur>} this player may break or place after all (asked for protected spots only). */
    public static void uitzondering(String structuur, BiPredicate<ServerPlayer, BlockPos> mag) {
        UITZONDERINGEN.computeIfAbsent(structuur, s -> new CopyOnWriteArrayList<>()).add(mag);
    }

    // --- asking ----------------------------------------------------------------------------------------------------------

    /** Is this spot inside a protected building (server side; false on the client and in chunks that aren't loaded)? */
    public static boolean beschermd(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && zone(server, pos) != null;
    }

    /** The registered structure (its name) this spot is protected by, or null. */
    @Nullable
    public static String structuurBij(ServerLevel level, BlockPos pos) {
        Zone z = zone(level, pos);
        return z == null ? null : z.structuur;
    }

    /**
     * May the block here be changed on behalf of this player (null: nobody in particular)? Not inside a protected quest
     * building, a {@link Protected} area, the spawn protection, or the area of a Guhhuisje that belongs to somebody else.
     * For machines: Knabbelaar, Neerzetter, Oogster.
     */
    public static boolean magWijzigen(ServerLevel level, BlockPos pos, @Nullable UUID namens) {
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos) || beschermd(level, pos) || Protected.at(level, pos)) {
            return false;
        }
        if (spawnBescherming(level, pos, namens)) {
            return false;
        }
        for (Huisje h : Huisjes.rond(level, pos, Huisjes.BEREIK + 8)) {
            if (h.inGebied(pos) && (namens == null || !namens.equals(h.eigenaar()))) {
                return false;
            }
        }
        return true;
    }

    /** The server's spawn protection, for a player who may be offline (then: protected for them too). */
    private static boolean spawnBescherming(ServerLevel level, BlockPos pos, @Nullable UUID namens) {
        ServerPlayer player = namens == null ? null : level.getServer().getPlayerList().getPlayer(namens);
        if (player != null) {
            return level.getServer().isUnderSpawnProtection(level, pos, player);
        }
        int straal = level.getServer() instanceof net.minecraft.server.dedicated.DedicatedServer server ? server.spawnProtectionRadius() : 0;
        LevelData.RespawnData spawn = level.getRespawnData();
        if (straal <= 0 || level.dimension() != spawn.dimension()) {
            return false;
        }
        return Math.max(Math.abs(pos.getX() - spawn.pos().getX()), Math.abs(pos.getZ() - spawn.pos().getZ())) <= straal;
    }

    /** May this player change the block here (creative players always; the quest exceptions of the structure)? */
    public static boolean mag(Player player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level) || player.getAbilities().instabuild) {
            return true;
        }
        Zone z = zone(level, pos);
        if (z == null) {
            return true;
        }
        if (player instanceof ServerPlayer sp) {
            for (BiPredicate<ServerPlayer, BlockPos> u : UITZONDERINGEN.getOrDefault(z.structuur, List.of())) {
                if (u.test(sp, pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean geweigerd(Player player, BlockPos pos) {
        if (mag(player, pos)) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.wereld.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // --- the zones -------------------------------------------------------------------------------------------------------

    /** The protected copy this spot lies in (null: none). */
    @Nullable
    private static Zone zone(ServerLevel level, BlockPos pos) {
        if (REGELS.isEmpty()) {
            return null;
        }
        Map<Structure, Regel> structuren = STRUCTUREN.computeIfAbsent(level, Bescherming::structuren);
        if (structuren.isEmpty()) {
            return null;
        }
        // (a copy's rim may reach into a chunk that the copy itself doesn't touch: the neighbours near the chunk's edge too)
        int rand = grootsteRand;
        int x0 = (pos.getX() - rand) >> 4, x1 = (pos.getX() + rand) >> 4, z0 = (pos.getZ() - rand) >> 4, z1 = (pos.getZ() + rand) >> 4;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                Map<Structure, LongSet> refs = chunk.getAllReferences();
                if (refs.isEmpty()) {
                    continue;
                }
                for (Map.Entry<Structure, LongSet> e : refs.entrySet()) {
                    Regel regel = structuren.get(e.getKey());
                    if (regel == null) {
                        continue;
                    }
                    for (long ref : e.getValue()) {
                        Zone zone = zone(level, e.getKey(), regel, ref);
                        if (zone != null && zone.bevat(pos)) {
                            return zone;
                        }
                    }
                }
            }
        }
        for (StructureStart start : Kopieen.test(level)) {
            Regel regel = structuren.get(start.getStructure());
            if (regel != null) {
                Zone zone = ZONES.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).computeIfAbsent(start.getStructure(), s -> new ConcurrentHashMap<>())
                        .computeIfAbsent(start.getChunkPos().pack(), k -> zone(regel, start));
                if (zone.bevat(pos)) {
                    return zone;
                }
            }
        }
        return null;
    }

    private static Map<Structure, Regel> structuren(ServerLevel level) {
        Map<Structure, Regel> out = new ConcurrentHashMap<>();
        for (Regel r : REGELS.values()) {
            Structure s = Kopieen.structuur(level, r.structuur);
            if (s != null) {
                out.put(s, r);
            }
        }
        return out;
    }

    @Nullable
    private static Zone zone(ServerLevel level, Structure structuur, Regel regel, long startChunk) {
        Map<Long, Zone> known = ZONES.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).computeIfAbsent(structuur, s -> new ConcurrentHashMap<>());
        Zone zone = known.get(startChunk);
        if (zone == null) {
            StructureStart start = level.getChunk(ChunkPos.getX(startChunk), ChunkPos.getZ(startChunk), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structuur);
            if (start == null || !start.isValid()) {
                return null;
            }
            zone = zone(regel, start);
            known.put(startChunk, zone);
        }
        return zone;
    }

    private static Zone zone(Regel regel, StructureStart start) {
        List<BoundingBox> stukken = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            stukken.add(piece.getBoundingBox().inflatedBy(regel.rand));
        }
        return new Zone(regel.structuur, BoundingBox.encapsulatingBoxes(stukken).orElse(new BoundingBox(0, 0, 0, 0, 0, 0)), List.copyOf(stukken));
    }

    /** (tests) forget what was read for this level. */
    static void wis(ServerLevel level) {
        STRUCTUREN.remove(level);
        ZONES.remove(level);
    }

    /** (tests) forget a registration. */
    public static void vergeet(String structuur) {
        REGELS.remove(structuur);
        UITZONDERINGEN.remove(structuur);
        grootsteRand = REGELS.values().stream().mapToInt(Regel::rand).max().orElse(0);
        STRUCTUREN.clear();
        ZONES.clear();
    }

    // --- the events ------------------------------------------------------------------------------------------------------

    static void opBreek(BreakBlockEvent event) {
        if (geweigerd(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    static void opPlaats(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? geweigerd(player, event.getPos()) : entity != null && beschermd(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Using an item on a block (buckets, flint and steel, an axe on a log, a hoe...): not here. Opening things is fine. */
    static void opGebruik(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty() || REGELS.isEmpty()) {
            return;
        }
        BlockPos naast = event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace());
        if ((beschermd(event.getLevel(), event.getPos()) && !mag(event.getEntity(), event.getPos()))
                || (beschermd(event.getLevel(), naast) && !mag(event.getEntity(), naast))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    static void opExplosie(ExplosionEvent.Detonate event) {
        if (!REGELS.isEmpty() && event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().removeIf(pos -> beschermd(level, pos));
        }
    }

    static void opMobGriefing(EntityMobGriefingEvent event) {
        if (!REGELS.isEmpty() && event.getEntity() != null && !(event.getEntity() instanceof Player)
                && beschermd(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    /** A piston never pushes or pulls blocks of a protected building, nor blocks into one. */
    static void opZuiger(PistonEvent.Pre event) {
        if (REGELS.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (beschermd(level, pos)) {
            return;   // (a piston of the building itself: part of a puzzle)
        }
        for (int i = 1; i <= 13; i++) {
            if (beschermd(level, pos.relative(event.getDirection(), i))) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
