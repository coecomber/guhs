package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * biomes3 wereld, the Klaterdal: the blocks of its rivers (called by {@link BioVulling} for every chunk).
 * <p>
 * Per river column of {@link DalTerrein}: a bed ({@link #bedding}), water up to the column's water level, and where the
 * water falls onto a lower bed ({@link Kaart#VAL}) a fluid tick, so the fall starts flowing by itself as soon as the
 * chunk ticks (water placed during world generation does not move until something updates it). A lip column gets rock.
 * The water cannot leave its bed: the model guarantees that every neighbour of a water block is water at the same level,
 * ground at least as high, or lower WATER (a fall).
 * <p>
 * Extension points for the Klaterdal polish agent: {@link #bedding} / {@link #rots} (materials), and more passes in
 * {@link #vul} (plunge pools, stepping stones, moss); trees and plants are ordinary placed features in
 * tools/features/bio_wereld_dal.py.
 */
public final class DalVulling {
    static BlockState bedding() {
        return Blocks.SAND.defaultBlockState();
    }

    static BlockState rots() {
        return Bio.blok("gladde_knuffelsteen", Bio.blok("knuffelsteen", Blocks.CALCITE)).defaultBlockState();
    }

    /** Places the rivers of this chunk; returns how many blocks were set. */
    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        BlockState water = Blocks.WATER.defaultBlockState(), bed = bedding(), rots = rots();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] < 0 || (k.vlag[o] & Kaart.RIVIER) == 0) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o];
            if ((k.vlag[o] & Kaart.LIP) != 0) {
                level.setBlock(p.set(x, h, z), rots, 2);
                level.setBlock(p.set(x, h - 1, z), rots, 2);
                gezet += 2;
                continue;
            }
            level.setBlock(p.set(x, h, z), bed, 2);
            level.setBlock(p.set(x, h - 1, z), bed, 2);
            for (int y = h + 1; y <= k.water[o]; y++) {
                level.setBlock(p.set(x, y, z), water, 2);
                gezet++;
                if ((k.vlag[o] & Kaart.VAL) != 0) {
                    level.scheduleTick(p.immutable(), Fluids.WATER, 0);
                }
            }
        }
        return gezet;
    }

    private DalVulling() {
    }
}
