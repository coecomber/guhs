package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * biomes3 wereld, the Bloesemmeertje: the blocks of the lake (called by {@link BioVulling} for every chunk).
 * <p>
 * Per lake column of {@link MeerTerrein}: a sandy floor and water up to {@link MeerTerrein#WATER}; the beach ring of an
 * island and a shoal at the water line get sand. On every large island a PLACEHOLDER big tree stands at the island's tree
 * spot ({@link MeerTerrein.Eiland#boomX}), which is where the structure spot {@code meer_boom} points.
 * <p>
 * Extension points for the Bloesemmeertje polish agent: {@link #bodem} (the floor that gets bluer with depth),
 * {@link #groteBoom} (replace the placeholder by the real overhanging tree; keep its foot on the tree spot), and more
 * passes in {@link #vul} (stepping stones, boulders); lilies, reeds, seagrass and the other trees are ordinary placed
 * features in tools/features/bio_wereld_meer.py.
 */
public final class MeerVulling {
    static BlockState bodem(int diepte) {
        return Blocks.SAND.defaultBlockState();
    }

    /** Places the lake of this chunk; returns how many blocks were set. */
    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        boolean meer = false;
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] >= 0 || k.soort[o] == Kaart.WEIDE || k.soort[o] == Kaart.BUITEN || k.meng[o] < 1f) {
                continue;
            }
            meer = true;
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o];
            if (k.water[o] == Kaart.GEEN) {
                if (h <= MeerTerrein.WATER + 1) {
                    // a beach or a shoal
                    level.setBlock(p.set(x, h, z), bodem(0), 2);
                    level.setBlock(p.set(x, h - 1, z), bodem(0), 2);
                    gezet += 2;
                }
                continue;
            }
            BlockState bodem = bodem(k.water[o] - h);
            level.setBlock(p.set(x, h, z), bodem, 2);
            level.setBlock(p.set(x, h - 1, z), bodem, 2);
            for (int y = h + 1; y <= k.water[o]; y++) {
                level.setBlock(p.set(x, y, z), water, 2);
                gezet++;
            }
        }
        if (meer) {
            for (MeerTerrein.Eiland ei : MeerTerrein.bij(m, x0, z0, x0 + 16, z0 + 16)) {
                if (ei.groot() && (ei.boomX() >> 4) == k.cx && (ei.boomZ() >> 4) == k.cz) {
                    gezet += groteBoom(level, m, ei);
                }
            }
        }
        return gezet;
    }

    /** PLACEHOLDER: a plain guhbloesem tree on the island's tree spot (a trunk of six with a round crown). */
    static int groteBoom(WorldGenLevel level, BioModel m, MeerTerrein.Eiland ei) {
        BlockState stam = Bio.blok("guhbloesem_log", Blocks.CHERRY_LOG).defaultBlockState();
        BlockState blad = Bio.blok("guhbloesem_leaves", Blocks.CHERRY_LEAVES).defaultBlockState();
        if (blad.hasProperty(LeavesBlock.PERSISTENT)) {
            blad = blad.setValue(LeavesBlock.PERSISTENT, true);
        }
        int x = ei.boomX(), z = ei.boomZ(), y = m.hoogte(x, z) + 1, gezet = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 <= 11 && level.isEmptyBlock(p.set(x + dx, y + 6 + dy, z + dz))) {
                        level.setBlock(p, blad, 2);
                        gezet++;
                    }
                }
            }
        }
        for (int dy = 0; dy < 6; dy++) {
            level.setBlock(p.set(x, y + dy, z), stam, 2);
        }
        return gezet + 6;
    }

    private MeerVulling() {
    }
}
