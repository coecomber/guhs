package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.eilanden.WolkenstroomBlock;

/**
 * biomes3 wereld, the Wolkenweide: the blocks that are not solid ground (called by {@link BioVulling} for every chunk):
 * the cloud banks and the thin cloud sea of {@link WolkTerrein} (cloud blocks of slice blokken-wolk through
 * {@link Bio#blok}, wool until that slice is merged), and the wolkenlift / wolkenstroom columns beside the higher
 * islands: 3 x 3, a {@code guhs:wolkenlift} pad in the meadow and {@code guhs:wolkenstroom} above it, exactly as in the
 * zwevende_eilanden structure; up-columns end two blocks above the island's top and puff you onto it.
 * <p>
 * Extension points for the Wolkenweide polish agent: {@link #wolk} (materials, e.g. slabs on top), more passes in
 * {@link #vul} (layered pastel undersides, crystal tips and vines under the islands: read the spans of the chunk map);
 * plants and trees on the islands are ordinary placed features in tools/features/bio_wereld_wolk.py.
 */
public final class WolkVulling {
    static BlockState wolk(boolean roze) {
        return (roze ? Bio.blok("wolkenblok_roze", Blocks.PINK_WOOL) : Bio.blok("wolkenblok_wit", Blocks.WHITE_WOOL)).defaultBlockState();
    }

    /** Places the clouds and lift columns of this chunk; returns how many blocks were set. */
    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        boolean weide = false;
        for (int o = 0; o < 256 && !weide; o++) {
            weide = k.soort[o] == Kaart.WEIDE;
        }
        if (!weide) {
            return 0;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState wit = wolk(false), roze = wolk(true);
        // the cloud banks
        int bereik = 20, cel = WolkTerrein.WOLK_CEL;
        for (int cx = Math.floorDiv(x0 - bereik, cel); cx <= Math.floorDiv(x0 + 15 + bereik, cel); cx++) {
            for (int cz = Math.floorDiv(z0 - bereik, cel); cz <= Math.floorDiv(z0 + 15 + bereik, cel); cz++) {
                for (WolkTerrein.Wolk w : WolkTerrein.wolken(m, cx, cz)) {
                    int rx = (int) Math.ceil(w.rx()), rz = (int) Math.ceil(w.rz());
                    for (int x = Math.max(x0, w.x() - rx); x <= Math.min(x0 + 15, w.x() + rx); x++) {
                        for (int z = Math.max(z0, w.z() - rz); z <= Math.min(z0 + 15, w.z() + rz); z++) {
                            double q = (x - w.x()) * (x - w.x()) / (w.rx() * w.rx()) + (z - w.z()) * (z - w.z()) / (w.rz() * w.rz());
                            if (q >= 1 || k.soort[Kaart.index(x, z)] != Kaart.WEIDE) {
                                continue;
                            }
                            double hoog = w.ry() * Math.sqrt(1 - q);
                            // (flat underneath, round on top)
                            for (int y = w.y() - (int) Math.min(1, hoog); y <= w.y() + (int) hoog; y++) {
                                if (level.isEmptyBlock(p.set(x, y, z))) {
                                    level.setBlock(p, w.roze() ? roze : wit, 2);
                                    gezet++;
                                }
                            }
                        }
                    }
                }
            }
        }
        // the cloud sea
        for (int o = 0; o < 256; o++) {
            if (k.soort[o] != Kaart.WEIDE || k.meng[o] < 1f) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), dik = WolkTerrein.wolkenzee(m, x, z);
            if (dik == 0 || Luchtruim.bezet(x, z, 2)) {
                continue;
            }
            for (int y = k.hoogte[o] + WolkTerrein.ZEE_HOOGTE; y < k.hoogte[o] + WolkTerrein.ZEE_HOOGTE + dik; y++) {
                if (level.isEmptyBlock(p.set(x, y, z))) {
                    level.setBlock(p, wit, 2);
                    gezet++;
                }
            }
        }
        // the lift and stream columns
        for (WolkTerrein.Eiland ei : WolkTerrein.bij(m, x0, z0, x0 + 16, z0 + 16)) {
            if (ei.lift != null && WolkTerrein.liftVrij(m, ei.liftX, ei.liftZ, ei.liftGrond, ei.top + 2)) {
                gezet += kolom(level, k, ei.liftX, ei.liftZ, ei.liftGrond, ei.top + 2, ei.lift.getOpposite(), false);
            }
            if (ei.stroom != null && WolkTerrein.liftVrij(m, ei.stroomX, ei.stroomZ, ei.stroomGrond, ei.top + 1)) {
                gezet += kolom(level, k, ei.stroomX, ei.stroomZ, ei.stroomGrond, ei.top + 1, ei.stroom, true);
            }
        }
        return gezet;
    }

    /** The part of a 3 x 3 lift column (around x, z) that lies in this chunk: the pad at {@code grond}, the stream up to {@code boven}. */
    static int kolom(WorldGenLevel level, Kaart k, int x, int z, int grond, int boven, Direction kijk, boolean omlaag) {
        BlockState pad = Bio.blok("wolkenlift", Blocks.WHITE_WOOL).defaultBlockState(), stroom = Bio.blok("wolkenstroom", Blocks.AIR).defaultBlockState();
        if (pad.hasProperty(HorizontalDirectionalBlock.FACING) && pad.hasProperty(WolkenstroomBlock.DOWN)) {
            pad = pad.setValue(HorizontalDirectionalBlock.FACING, kijk).setValue(WolkenstroomBlock.DOWN, omlaag);
        }
        if (stroom.hasProperty(HorizontalDirectionalBlock.FACING) && stroom.hasProperty(WolkenstroomBlock.DOWN)) {
            stroom = stroom.setValue(HorizontalDirectionalBlock.FACING, kijk).setValue(WolkenstroomBlock.DOWN, omlaag);
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int gezet = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int px = x + dx, pz = z + dz;
                if ((px >> 4) != k.cx || (pz >> 4) != k.cz) {
                    continue;
                }
                // (the meadow rolls a little: the pad lies level, on a foot where the ground is lower)
                for (int y = grond - 3; y < grond; y++) {
                    if (level.isEmptyBlock(p.set(px, y, pz))) {
                        level.setBlock(p, Blocks.PINK_WOOL.defaultBlockState(), 2);
                    }
                }
                level.setBlock(p.set(px, grond, pz), pad, 2);
                for (int y = grond + 1; y <= boven; y++) {
                    level.setBlock(p.set(px, y, pz), stroom, 2);
                    gezet++;
                }
            }
        }
        return gezet;
    }

    private WolkVulling() {
    }
}
