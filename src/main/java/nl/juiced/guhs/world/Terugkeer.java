package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.2.7: the one-of-a-kind inhabitants of a building come back, so every player on a server gets their turn (the Wolkguh of
 * the floating islands, Big Mika in his cave, the magere guhs in the cells of a Knabbelkelder...). Per level this remembers
 * when the inhabitant of a spot was last seen; when it has been gone for long enough, {@link #moetTerug} says a new one
 * should come. A spot that was never seen with its inhabitant (a world from before 1.2.7, where it was already taken) gets
 * a new one right away.
 */
public class Terugkeer extends SavedData {
    public static final SavedDataType<Terugkeer> TYPE = GuhSavedData.tagType("terugkeer", Terugkeer::new, Terugkeer::load, Terugkeer::save);
    /** A spot has to be seen empty twice, this many ticks apart, before anything comes back (entities load a little after their chunk). */
    public static final int BEVESTIG = 60;
    /** "Last seen" is only written again after this many ticks (so the file isn't dirty all the time). */
    private static final int STEMPEL_STAP = 200;

    private final Map<String, Long> gezien = new HashMap<>();
    /** (not saved) dimension|key -> when the spot was first seen empty. */
    private static final Map<String, Long> GEMIST = new ConcurrentHashMap<>();

    public static Terugkeer get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static String sleutel(String soort, BlockPos plek) {
        return soort + "@" + plek.getX() + "," + plek.getY() + "," + plek.getZ();
    }

    /**
     * The periodic check of one spot. {@code aanwezig}: its inhabitant is there now. Returns true when a new one should come
     * right now: the spot is loaded (entities too), it was seen empty twice, and its inhabitant was last seen more than
     * {@code wachttijd} ticks ago (or never).
     */
    public static boolean moetTerug(ServerLevel level, String soort, BlockPos plek, boolean aanwezig, long wachttijd) {
        if (!level.isPositionEntityTicking(plek) || !level.areEntitiesLoaded(ChunkPos.pack(plek))) {
            return false;
        }
        Terugkeer t = get(level);
        String key = sleutel(soort, plek);
        String g = level.dimension().identifier() + "|" + key;
        long nu = level.getGameTime();
        if (aanwezig) {
            GEMIST.remove(g);
            Long vorige = t.gezien.get(key);
            if (vorige == null || nu - vorige >= STEMPEL_STAP) {
                t.gezien.put(key, nu);
                t.setDirty();
            }
            return false;
        }
        Long eerst = GEMIST.putIfAbsent(g, nu);
        if (eerst == null || nu - eerst < BEVESTIG) {
            return false;
        }
        Long laatst = t.gezien.get(key);
        if (laatst != null && nu - laatst < wachttijd) {
            return false;
        }
        GEMIST.remove(g);
        t.gezien.put(key, nu);
        t.setDirty();
        return true;
    }

    /** The inhabitant of this spot just left (killed, tamed): the waiting time starts now. */
    public static void vertrokken(ServerLevel level, String soort, BlockPos plek) {
        Terugkeer t = get(level);
        t.gezien.put(sleutel(soort, plek), level.getGameTime());
        t.setDirty();
    }

    /** When the inhabitant of this spot was last seen (null: never). */
    @Nullable
    public static Long laatstGezien(ServerLevel level, String soort, BlockPos plek) {
        return get(level).gezien.get(sleutel(soort, plek));
    }

    /** (Tests) pretends the inhabitant of this spot was last seen at this game time (null: never), and forgets the first miss. */
    public static void zetGezien(ServerLevel level, String soort, BlockPos plek, @Nullable Long gameTime) {
        Terugkeer t = get(level);
        String key = sleutel(soort, plek);
        if (gameTime == null) {
            t.gezien.remove(key);
        } else {
            t.gezien.put(key, gameTime);
        }
        t.setDirty();
        GEMIST.remove(level.dimension().identifier() + "|" + key);
    }

    /** (Tests) pretends this spot was first seen empty this many ticks ago. */
    public static void zetGemist(ServerLevel level, String soort, BlockPos plek, long ticksGeleden) {
        GEMIST.put(level.dimension().identifier() + "|" + sleutel(soort, plek), level.getGameTime() - ticksGeleden);
    }

    /** (Tests, dev) everything that is remembered in this level: key -> the game time its inhabitant was last seen. */
    public static Map<String, Long> alles(ServerLevel level) {
        return new java.util.TreeMap<>(get(level).gezien);
    }

    /** (Tests, dev) as if this many ticks went by: every inhabitant of this level was last seen that much longer ago. */
    public static void verschuif(ServerLevel level, long ticks) {
        Terugkeer t = get(level);
        t.gezien.replaceAll((k, v) -> v - ticks);
        t.setDirty();
    }

    // --- where things are in a building ------------------------------------------------------------------------------------

    /**
     * The template pieces of the structure {@code key} that has a piece at {@code bij} (empty: no such structure there).
     * {@code stuk}: only the pieces whose template name contains this (null: all of them).
     */
    public static List<PoolElementStructurePiece> stukken(ServerLevel level, ResourceKey<Structure> key, BlockPos bij, @Nullable String stuk) {
        List<PoolElementStructurePiece> uit = new ArrayList<>();
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(key);
        if (structure == null || !level.isLoaded(bij)) {
            return uit;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(bij, structure);
        if (!start.isValid()) {
            return uit;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece p && (stuk == null || p.getElement().toString().contains(stuk))) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** The world position of a block of the piece's template (the template is placed rotated). */
    public static BlockPos wereld(PoolElementStructurePiece piece, BlockPos lokaal) {
        return piece.getPosition().offset(StructureTemplate.transform(lokaal, Mirror.NONE, piece.getRotation(), BlockPos.ZERO));
    }

    // --- saving ------------------------------------------------------------------------------------------------------------

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag g = new CompoundTag();
        gezien.forEach(g::putLong);
        tag.put("Gezien", g);
        return tag;
    }

    private static Terugkeer load(CompoundTag tag) {
        Terugkeer t = new Terugkeer();
        CompoundTag g = tag.getCompoundOrEmpty("Gezien");
        for (String key : g.keySet()) {
            t.gezien.put(key, g.getLongOr(key, 0L));
        }
        return t;
    }
}
