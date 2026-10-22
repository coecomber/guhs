package nl.juiced.guhs.feature.safari.bus;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * 1.6.0 (F1b): het safari-busstation in de Guhmension (structure {@code guhs:safari_busstation}, built by
 * tools/features/safari_busstation.py: the coordinates here are the template's, keep them the same as there). One copy is
 * guaranteed 500-2000 blocks from 0,0 (new chunks only), more are rare; on a world whose land around spawn already exists
 * an op places one by hand ({@link #plaats}, {@code /guhs safari busstation plaats}).
 */
public final class Busstation {
    public static final String STRUCTUUR = "safari_busstation";
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, Guhs.id(STRUCTUUR));
    /** The template's ground (people stand at G + 1). */
    public static final int G = 6;
    /** The bus body (template), nose to the south. */
    public static final BlockPos BUS_MIN = new BlockPos(50, G + 1, 26), BUS_MAX = new BlockPos(55, G + 9, 41);
    /** Chauffeur Toeter beside the bus door (also the anchor of the scene back), the bell at the stop, the shop guh. */
    public static final BlockPos CHAUFFEUR = new BlockPos(57, G + 1, 38), BEL = new BlockPos(56, G + 1, 44), VERKOPER = new BlockPos(41, G + 1, 54);

    /** An animal of the mini-enclosures: its kind, variant and home (template x, z). */
    public record Dier(String soort, String variant, int x, int z) {
    }

    public static final List<Dier> DIEREN = List.of(
            new Dier("stokstaartje", "vrouw", 5, 31), new Dier("stokstaartje", "man", 9, 35), new Dier("stokstaartje", "vrouw", 4, 35),
            new Dier("stokstaartje", "baby", 6, 34), new Dier("stokstaartje", "vrouw", 10, 31),
            new Dier("flamingo", "vrouw", 5, 44), new Dier("flamingo", "man", 8, 46), new Dier("flamingo", "vrouw", 9, 49), new Dier("flamingo", "baby", 6, 48),
            new Dier("pinguin", "vrouw", 6, 58), new Dier("pinguin", "man", 4, 60), new Dier("pinguin", "baby", 5, 61), new Dier("pinguin", "baby", 7, 60));
    /** How far an animal of the station strolls from its home (the pens are 12 x 10). */
    public static final int DIER_STRAAL = 3;
    /** The visitor guhs (template x, z). */
    public static final List<BlockPos> BEZOEKERS = List.of(new BlockPos(30, G + 1, 30), new BlockPos(33, G + 1, 29), new BlockPos(31, G + 1, 44),
            new BlockPos(32, G + 1, 52), new BlockPos(16, G + 1, 41), new BlockPos(19, G + 1, 32), new BlockPos(40, G + 1, 46), new BlockPos(25, G + 1, 30));

    private Busstation() {
    }

    /** A copy in the world: the anchor of its scene (the chauffeur's spot), its turn. */
    public record Oord(StructureStart start, BlockPos anker, Rotation draai) {
        /** Is this world block part of the bus (or its bell)? */
        public boolean isBus(BlockPos wereld) {
            BlockPos l = Kopieen.lokaal(start, null, wereld);
            if (l == null) {
                return false;
            }
            return l.equals(BEL) || l.getX() >= BUS_MIN.getX() && l.getX() <= BUS_MAX.getX() && l.getY() >= BUS_MIN.getY() && l.getY() <= BUS_MAX.getY()
                    && l.getZ() >= BUS_MIN.getZ() && l.getZ() <= BUS_MAX.getZ();
        }

        /** A template position (relative to the anchor, like the scenes) in the world. */
        public Vec3 wereld(double x, double y, double z) {
            return Cutscene.wereld(anker, draai, new Vec3(x, y, z));
        }
    }

    /** The copy at this spot (a piece within {@link Bezetting#BEREIK}), or null. */
    @Nullable
    public static Oord oord(ServerLevel level, BlockPos bij) {
        StructureStart start = Bezetting.start(level, STRUCTUUR, bij);
        if (start == null || start.getPieces().isEmpty()) {
            return null;
        }
        BlockPos anker = Kopieen.wereld(start, null, CHAUFFEUR);
        return anker == null ? null : new Oord(start, anker, Kopieen.draai(start, null));
    }

    /**
     * (ops) Places a busstation here, like /place structure, and also writes it into the chunks as a real copy (start +
     * references), so it is protected, its inhabitants come and the bus finds it. Where the ground is not flat enough for
     * worldgen (or another guh building claimed the room) it is placed anyway: the op chose the spot. False when even
     * the jigsaw cannot start here.
     */
    public static boolean plaats(ServerLevel level, BlockPos bij) {
        Holder.Reference<Structure> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(KEY).orElse(null);
        if (holder == null) {
            return false;
        }
        Structure structure = holder.value();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos chunk = ChunkPos.containing(bij);
        StructureStart gemaakt = structure.generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level, b -> true);
        if (!gemaakt.isValid() && structure instanceof nl.juiced.guhs.world.FlatJigsawStructure flat) {
            // the op chose the spot: skip the flatness and room checks, the jigsaw inside makes the same piece
            StructureStart binnen = flat.jigsaw().generate(Holder.direct(flat.jigsaw()), level.dimension(), level.registryAccess(), generator,
                    generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level, b -> true);
            if (binnen.isValid()) {
                gemaakt = new StructureStart(structure, chunk, 0, new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(binnen.getPieces()));
            }
        }
        if (!gemaakt.isValid()) {
            return false;
        }
        StructureStart start = gemaakt;
        BoundingBox box = start.getBoundingBox();
        BekendeStations.onthoud(level, box.getCenter());
        ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
        ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
        level.getChunk(chunk.x(), chunk.z()).setStartForStructure(structure, start);
        ChunkPos.rangeClosed(min, max).forEach(c -> {
            ChunkAccess ca = level.getChunk(c.x(), c.z());
            ca.addReferenceForStructure(structure, chunk.pack());
            start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                    new BoundingBox(c.getMinBlockX(), level.getMinY(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxY() + 1, c.getMaxBlockZ()), c);
        });
        return true;
    }

    /**
     * The nearest busstation the world knows ({@link BekendeStations}), or null. 1.6.1: no structure search any more: on the
     * server thread it loaded chunks and hung the live server until the watchdog stopped it.
     */
    @Nullable
    public static BlockPos dichtstbij(ServerLevel level, BlockPos bij) {
        return BekendeStations.dichtstbij(level, bij);
    }
}
