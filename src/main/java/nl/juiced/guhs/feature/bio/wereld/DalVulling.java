package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * biomes3 wereld, the Klaterdal: the blocks of its water and rock (called by {@link BioVulling} for every chunk), then its
 * plants ({@link DalPlanten}).
 * <p>
 * Everything follows {@link DalTerrein}'s columns and never adds or removes ground or water the model does not know:
 * <ul>
 *   <li>a water column: a bed of light sand with a pebble here and there, water sources up to the column's level, and
 *       where the water falls onto a lower bed ({@link Kaart#VAL}) a fluid tick, so the fall starts flowing by itself as
 *       soon as the chunk ticks (water placed during world generation does not move until something updates it);</li>
 *   <li>a lip or a stepping stone ({@link Kaart#LIP}): rock up to the water's level;</li>
 *   <li>a sculpted dry column ({@link DalTerrein#VORM}: boulder, rounded foot or shoulder of a face): white rock instead
 *       of the two blocks of pink ground, some with a pad of moss;</li>
 *   <li>a natural step ({@link DalTerrein#TREDE}): a stair block that climbs towards the next step;</li>
 *   <li>biomes3 fix-dal: every dry column that stands two or more above a neighbour shows a FACE: white rock from one
 *       under its top down to that neighbour, with soft patches of moss in it, and where a face is four or more high a
 *       strand of blossom or pale leaves hangs over its edge here and there. A ledge of the sculpted rock (a column with a
 *       neighbour at its own height) is topped with moss in patches, some with a small plant.</li>
 * </ul>
 * The water cannot leave its bed: the model guarantees that every neighbour of a water block is water at the same level,
 * ground at least as high, or lower WATER (a fall of two blocks or more).
 */
public final class DalVulling {
    static BlockState bedding() {
        return Blocks.SAND.defaultBlockState();
    }

    static BlockState rots() {
        return Bio.blok("gladde_knuffelsteen", Bio.blok("knuffelsteen", Blocks.CALCITE)).defaultBlockState();
    }

    /** A block state with a property set by name (blocks of other slices are named, never imported). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static BlockState met(BlockState state, String eigenschap, String waarde) {
        Property p = state.getBlock().getStateDefinition().getProperty(eigenschap);
        if (p == null) {
            return state;
        }
        var w = p.getValue(waarde);
        return w.isPresent() ? state.setValue(p, (Comparable) w.get()) : state;
    }

    /** How many chunks got their Klaterdal blocks and plants, and the nanoseconds that took (dev command "kosten"). */
    static final java.util.concurrent.atomic.LongAdder CHUNKS = new java.util.concurrent.atomic.LongAdder(), CHUNK_NS = new java.util.concurrent.atomic.LongAdder();

    /** Places the Klaterdal's part of this chunk; returns how many blocks were set. */
    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        long t0 = System.nanoTime();
        int gezet = vulChunk(level, m, k);
        if (gezet > 0) {
            CHUNKS.increment();
            CHUNK_NS.add(System.nanoTime() - t0);
        }
        return gezet;
    }

    private static int vulChunk(WorldGenLevel level, BioModel m, Kaart k) {
        boolean dal = false;
        for (int o = 0; o < 256 && !dal; o++) {
            dal = k.terras[o] >= 0 && k.meng[o] >= 1f;
        }
        if (!dal) {
            return 0;
        }
        BlockState water = Blocks.WATER.defaultBlockState(), bed = bedding(), rots = rots();
        BlockState glans = Bio.blok("parelmoer", rots.getBlock()).defaultBlockState();
        BlockState trede = Bio.blok("gladde_knuffelsteen_trap", Blocks.QUARTZ_STAIRS).defaultBlockState();
        BlockState tapijt = DalBlokken.MOS_TAPIJT.get().defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        BlockState mos = DalBlokken.MOS.get().defaultBlockState();
        BlockState hangRoze = DalPlanten.blad(Bio.blok("guhbloesem_leaves", Blocks.CHERRY_LEAVES).defaultBlockState());
        BlockState hangGroen = DalPlanten.blad(DalBlokken.BONSAIBLAD.get().defaultBlockState());
        boolean gebouwen = !level.getChunk(k.cx, k.cz).getAllReferences().isEmpty();
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] < 0 || k.meng[o] < 1f) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o], vlag = k.vlag[o];
            long hash = m.hash(x, z, 6001);
            if (k.water[o] != Kaart.GEEN) {
                BlockState bodem = BioModel.kans(hash, 0) < 0.06 ? rots : bed;
                level.setBlock(p.set(x, h, z), bodem, 2);
                level.setBlock(p.set(x, h - 1, z), bed, 2);
                for (int y = h + 1; y <= k.water[o]; y++) {
                    level.setBlock(p.set(x, y, z), water, 2);
                    gezet++;
                }
                if ((vlag & Kaart.VAL) != 0) {
                    level.scheduleTick(new BlockPos(x, k.water[o], z), Fluids.WATER, 0);
                }
                continue;
            }
            // biomes3 fix-dal: the neighbours: how far the lowest lies under this column (a face), is one level with it (a ledge)
            int laagst = h, laagX = 0, laagZ = 0;
            boolean richel = false, laagTrede = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int bx = x + d.getStepX(), bz = z + d.getStepZ();
                if (m.meng(bx, bz) < 1f) {
                    continue;
                }
                int bw = m.water(bx, bz), peil = bw != Kaart.GEEN ? bw : m.hoogte(bx, bz);
                richel |= bw == Kaart.GEEN && peil == h;
                if (peil < laagst) {
                    laagst = peil;
                    laagX = bx;
                    laagZ = bz;
                    laagTrede = bw != Kaart.GEEN || (m.vlag(bx, bz) & DalTerrein.TREDE) != 0;
                }
            }
            if ((vlag & Kaart.LIP) != 0) {
                level.setBlock(p.set(x, h, z), rots, 2);
                level.setBlock(p.set(x, h - 1, z), rots, 2);
                gezet += 2;
                // (a boulder in the river: now and then a pad of moss)
                if ((vlag & DalTerrein.VORM) != 0 && BioModel.kans(hash, 98) < 0.35 && level.isEmptyBlock(p.set(x, h + 1, z))) {
                    level.setBlock(p, tapijt, 2);
                    gezet++;
                }
                continue;
            }
            if ((vlag & DalTerrein.TREDE) != 0) {
                // a stair that climbs to the neighbour one higher (a step of the same flight, or the terrace at its top)
                BlockState blok = rots;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int bx = x + d.getStepX(), bz = z + d.getStepZ();
                    if (m.water(bx, bz) == Kaart.GEEN && m.hoogte(bx, bz) == h + 1) {
                        blok = met(met(trede, "facing", d.getName()), "half", "bottom");
                        break;
                    }
                }
                level.setBlock(p.set(x, h, z), blok, 2);
                level.setBlock(p.set(x, h - 1, z), rots, 2);
                gezet += 2;
                continue;
            }
            if (gebouwen && BouwRuimte.inBuilding(level, p.set(x, h, z))) {
                continue;
            }
            double mosRuis = (vlag & DalTerrein.VORM) != 0 || laagst <= h - 2 ? m.ruis(BioModel.R_DETAIL, x * 0.45 + 640, z * 0.45 - 220) : 0;
            if ((vlag & DalTerrein.VORM) != 0) {
                // sculpted rock: white, a fleck of parelmoer; a ledge carries moss in patches, some of it with a small plant
                boolean bemost = richel && mosRuis > -0.05 && BioModel.kans(hash, 99) < 0.85;
                level.setBlock(p.set(x, h, z), bemost ? mos : BioModel.kans(hash, h) < 0.13 ? glans : rots, 2);
                level.setBlock(p.set(x, h - 1, z), BioModel.kans(hash, h - 1) < 0.13 ? glans : rots, 2);
                gezet += 2;
                if (level.isEmptyBlock(p.set(x, h + 1, z))) {
                    double worp = BioModel.kans(hash, 97);
                    if (bemost && worp < 0.07) {
                        level.setBlock(p, Bio.blok(DalPlanten.BLOEMEN[(int) (BioModel.kans(hash, 96) * DalPlanten.BLOEMEN.length)], Blocks.PINK_TULIP).defaultBlockState(), 2);
                        gezet++;
                    } else if (!bemost && mosRuis > 0.05 && worp < 0.45) {
                        level.setBlock(p, tapijt, 2);
                        gezet++;
                    }
                }
            }
            if (laagst <= h - 2) {
                // the face under this column's top: rock, with moss in soft patches
                for (int y = h - 1; y > laagst; y--) {
                    double vlek = m.ruis(BioModel.R_DETAIL, x * 0.5 + y * 0.45 + 333, z * 0.5 - y * 0.35 - 111);
                    level.setBlock(p.set(x, y, z), vlek > 0.1 ? mos : BioModel.kans(hash, y) < 0.10 ? glans : rots, 2);
                    gezet++;
                }
                // a strand hanging over the edge of a high face (not over a flight of steps or the water)
                if (laagst <= h - 4 && !laagTrede && m.ruis(BioModel.R_DETAIL, x * 0.3 + 7000, z * 0.3 - 9000) > -0.12 && BioModel.kans(hash, 55) < 0.6) {
                    BlockState hang = m.ruis(BioModel.R_DETAIL, x * 0.08 - 4100, z * 0.08 + 2600) > 0 ? hangRoze : hangGroen;
                    // (a low bush on the rim itself, and its strands down the face)
                    if ((vlag & DalTerrein.TREDE) == 0 && BioModel.kans(hash, 57) < 0.7 && level.isEmptyBlock(p.set(x, h + 1, z))) {
                        level.setBlock(p, hang, 2);
                        gezet++;
                    }
                    int lang = 1 + (int) (BioModel.kans(hash, 56) * 3.4);
                    for (int i = 0; i < lang; i++) {
                        // (two blocks of air stay under it: you walk along the foot of the face)
                        if (h - i < laagst + 3 || !level.isEmptyBlock(p.set(laagX, h - i, laagZ)) || gebouwen && BouwRuimte.inBuilding(level, p)) {
                            break;
                        }
                        level.setBlock(p, hang, 2);
                        gezet++;
                    }
                }
            }
        }
        return gezet + DalPlanten.vul(level, m, k);
    }

    private DalVulling() {
    }
}
