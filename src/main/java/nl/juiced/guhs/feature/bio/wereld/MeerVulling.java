package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * biomes3 wereld, the Bloesemmeertje: the blocks of the lake (called by {@link BioVulling} for every chunk, step "lakes",
 * before any building).
 * <p>
 * Per lake column of {@link MeerTerrein}: the bed and the water up to {@link MeerTerrein#WATER}; sand on the beaches
 * ({@link MeerTerrein#STRAND}); smooth knuffelsteen for the boulders and stepping stones ({@link MeerTerrein#STEEN}).
 * The bed gets bluer and darker with depth ({@link #bodem}): sand, then the lake's own sediment ({@link MeerBodem}:
 * meerzand and three tones of meerslib), mixed block by block over about two blocks of depth so no contour
 * lines show. On every large island the BIG tree stands on the island's tree spot ({@link MeerTerrein.Eiland#boomX});
 * it is placed here, before the buildings, because the structure spot {@code meer_boom} is "under that tree".
 * <p>
 * Everything else that lives here (the other trees, flowers, reeds, lilies, petals, seagrass) is {@link MeerLeven}, a
 * later step that keeps clear of buildings.
 */
public final class MeerVulling {
    /** The bed under this many blocks of water at (x, z); the mix between two kinds is spread over about two blocks of depth. */
    static BlockState bodem(int diepte, int x, int z) {
        long h = BioModel.mix(x * 0x9E3779B97F4A7C15L ^ BioModel.mix(z * 0xC2B2AE3D27D4EB4FL + 77));
        // a triangular draw in -1..1: mostly near 0, so the middle of a band is nearly pure
        double v = diepte + (BioModel.kans(h, 0) + BioModel.kans(h, 1) - 1.0);
        if (v < 1.7) {
            return Blocks.SAND.defaultBlockState();
        }
        // (biomes3 fix-klein: sediment of our own instead of calcite, wool and concrete; the bands are unchanged)
        if (v < 3.1) {
            return MeerBodem.MEERZAND.get().defaultBlockState();
        }
        if (v < 4.9) {
            return MeerBodem.SLIB_LICHT.get().defaultBlockState();
        }
        if (v < 6.3) {
            return MeerBodem.SLIB.get().defaultBlockState();
        }
        return MeerBodem.SLIB_DIEP.get().defaultBlockState();
    }

    /** Places the lake of this chunk; returns how many blocks were set. */
    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        BlockState water = Blocks.WATER.defaultBlockState(), zand = Blocks.SAND.defaultBlockState();
        BlockState steen = Bio.blok("gladde_knuffelsteen", Blocks.CALCITE).defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        boolean meer = false;
        long t0 = System.nanoTime();
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] >= 0 || k.soort[o] == Kaart.WEIDE || k.soort[o] == Kaart.BUITEN || k.meng[o] < 1f) {
                continue;
            }
            meer = true;
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o];
            if (k.water[o] == Kaart.GEEN) {
                if ((k.vlag[o] & MeerTerrein.STEEN) != 0) {
                    // a boulder or a stepping stone: stone down to below the bed around it
                    for (int y = h; y >= h - 4; y--) {
                        level.setBlock(p.set(x, y, z), steen, 2);
                    }
                    gezet += 5;
                } else if ((k.vlag[o] & MeerTerrein.STRAND) != 0) {
                    level.setBlock(p.set(x, h, z), zand, 2);
                    level.setBlock(p.set(x, h - 1, z), zand, 2);
                    gezet += 2;
                }
                continue;
            }
            BlockState bodem = bodem(k.water[o] - h, x, z);
            level.setBlock(p.set(x, h, z), bodem, 2);
            level.setBlock(p.set(x, h - 1, z), bodem, 2);
            for (int y = h + 1; y <= k.water[o]; y++) {
                level.setBlock(p.set(x, y, z), water, 2);
                gezet++;
            }
        }
        if (meer) {
            for (MeerTerrein.Eiland ei : MeerTerrein.eilandenBij(m, x0, z0, x0 + 16, z0 + 16)) {
                if (ei.groot() && (ei.boomX() >> 4) == k.cx && (ei.boomZ() >> 4) == k.cz) {
                    gezet += groteBoom(level, m, ei);
                }
            }
            MeerLeven.VUL_NS.add(System.nanoTime() - t0);
            MeerLeven.VUL_N.increment();
        }
        return gezet;
    }

    /** The big tree of a large island, on its tree spot: the rare giant, or a large leaning one. */
    static int groteBoom(WorldGenLevel level, BioModel m, MeerTerrein.Eiland ei) {
        if (!m.droog(ei.boomX(), ei.boomZ())) {
            return 0;
        }
        return BloesemBoom.bouw(level, ei.bomen()[0], m.hoogte(ei.boomX(), ei.boomZ()) + 1);
    }

    private MeerVulling() {
    }
}
