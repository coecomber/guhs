package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dev stand-ins ({@code /guhs vadsrijk z3bazen bouw}): flat discs under every arena, perch and room, ONLY into air, so the fights
 * can be tried before z3-wereld's structures are merged. On the real map they add nothing (the floors are there).
 */
public final class Z3Bouw {
    private Z3Bouw() {
    }

    private static int schijf(ServerLevel sl, BlockPos c, int r, BlockState s) {
        int n = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z <= r * r + r) {
                    BlockPos b = c.offset(x, -1, z);
                    sl.getChunk(b);
                    if (sl.getBlockState(b).isAir()) {
                        sl.setBlock(b, s, 2);
                        n++;
                    }
                }
            }
        }
        return n;
    }

    public static int alles(ServerLevel sl) {
        BlockState hout = Blocks.SPRUCE_PLANKS.defaultBlockState(), steen = Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                blad = Blocks.MOSS_BLOCK.defaultBlockState();
        int n = 0;
        n += schijf(sl, Z3BazenPlekken.ROOF_CENTRUM, Z3BazenPlekken.ROOF_STRAAL + 4, blad);
        n += schijf(sl, Z3BazenPlekken.ROOF_ZIT_1, Z3BazenPlekken.ZIT_STRAAL, blad);
        n += schijf(sl, Z3BazenPlekken.ROOF_ZIT_2, Z3BazenPlekken.ZIT_STRAAL, blad);
        n += schijf(sl, Z3BazenPlekken.TALEGGIO_CENTRUM, Z3BazenPlekken.D1_STRAAL + 3, hout);
        n += schijf(sl, Z3BazenPlekken.BROKKEL_CENTRUM, Z3BazenPlekken.D1_STRAAL + 3, hout);
        n += schijf(sl, Z3BazenPlekken.ROQUEFORT_CENTRUM, Z3BazenPlekken.ROQUEFORT_STRAAL + 2, hout);
        n += schijf(sl, Z3BazenPlekken.ROQUEFORT_WRAK, 8, hout);
        n += schijf(sl, Z3BazenPlekken.LOODS, 10, hout);
        n += schijf(sl, Z3BazenPlekken.WERF_INGANG, 4, hout);
        n += schijf(sl, Z3BazenPlekken.PIEP_CENTRUM, Z3BazenPlekken.PIEP_STRAAL + 2, hout);
        n += schijf(sl, Z3BazenPlekken.WURGER_CENTRUM, Z3BazenPlekken.D2_STRAAL + 3, steen);
        n += schijf(sl, Z3BazenPlekken.WACHTER_CENTRUM, Z3BazenPlekken.D2_STRAAL + 3, steen);
        n += schijf(sl, Z3BazenPlekken.BLEU_CENTRUM, Z3BazenPlekken.BLEU_STRAAL + 3, steen);
        n += schijf(sl, Z3BazenPlekken.KWEKERIJ, 12, steen);
        n += schijf(sl, Z3BazenPlekken.RINGEN, 14, steen);
        n += schijf(sl, Z3BazenPlekken.HART_INGANG, 4, steen);
        n += schijf(sl, Z3BazenPlekken.NEST_CENTRUM, Z3BazenPlekken.NEST_STRAAL + 4, blad);
        return n;
    }
}
