package nl.juiced.guhs.feature.kermisuitbouw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The ground of the kermis extension, one column at a time: on the fields everything goes to the kermis floor, around them a
 * band of {@link KermisUitbouw#BAND} blocks slopes back to the natural height. The top block of the column stays; below that
 * the Guhmension's pink wool. Valleys are filled whatever their depth, but nothing is built below {@link #ONDERGRENS} inside a
 * structure: under kermis A lies the Kaasmakerij.
 */
final class KermisGrond {
    /** Below this y, no ground goes into a structure ({@link #magVullen}). */
    static final int ONDERGRENS = 54;
    /** Never deeper than this below the floor. */
    private static final int DIEPTE = 64;
    private static final int VLAG = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String[] BOOM = {"leaves", "bladeren", "_blad", "naalden", "_log", "_stam", "paddenstoel"};

    private KermisGrond() {
    }

    /** May ground be put at this y? */
    static boolean magVullen(int y, boolean inStructuur) {
        return y >= ONDERGRENS || !inStructuur;
    }

    /** A part of a tree (or of a giant mushroom)? */
    static boolean boom(BlockState s) {
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) {
            return true;
        }
        if (s.isAir() || s.getBlock() == Blocks.WATER) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        for (String deel : BOOM) {
            if (id.contains(deel)) {
                return true;
            }
        }
        return false;
    }

    /** Not ground: air, water, plants, trees, thin blocks. */
    private static boolean zacht(ServerLevel level, BlockPos p, BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty() || boom(s) || !s.isCollisionShapeFullBlock(level, p);
    }

    /** Takes a block away; the things in a container a player filled are kept for the chests at the end of the street. */
    static void weg(ServerLevel level, BlockPos p, KermisUitbouw.Klus klus) {
        BlockEntity be = level.getBlockEntity(p);
        if (be != null) {
            if (be instanceof Container c && !(be instanceof RandomizableContainer r && r.getLootTable() != null)) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (!s.isEmpty()) {
                        klus.bewaar(s.copy());
                    }
                }
                c.clearContent();
            }
            level.removeBlockEntity(p);
        }
        level.setBlock(p, Blocks.AIR.defaultBlockState(), VLAG);
    }

    /** One column of the kermis frame. */
    static void kolom(ServerLevel level, KermisUitbouw.Plek plek, int lx, int lz, KermisUitbouw.Klus klus) {
        if (KermisUitbouw.VRIJ.bevat(lx, lz)) {
            return;
        }
        double t = Math.min(KermisUitbouw.HOOFD.afstand(lx, lz), KermisUitbouw.BOOSTER.afstand(lx, lz));
        if (t >= KermisUitbouw.BAND) {
            return;
        }
        BlockPos w = plek.wereld(new BlockPos(lx, 0, lz));
        int x = w.getX(), z = w.getZ();
        int vloer = plek.vloer();
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, top, z);
        int h = top;
        for (; h > level.getMinY(); h--) {
            m.setY(h);
            BlockState s = level.getBlockState(m);
            if (boom(s)) {
                klus.boomGezien(m);
            }
            if (!zacht(level, m, s)) {
                break;
            }
        }
        double f = Math.pow(t / KermisUitbouw.BAND, 1.5);
        int doel = (int) Math.round(vloer + (h - vloer) * f);
        m.setY(h);
        BlockState blad = level.getBlockState(m);
        if (blad.is(Blocks.DIRT)) {
            blad = Blocks.PINK_WOOL.defaultBlockState();
        }
        for (int y = top; y > doel; y--) {
            m.setY(y);
            if (!level.getBlockState(m).isAir()) {
                weg(level, m, klus);
            }
        }
        if (doel == h) {
            return;
        }
        m.setY(doel);
        if (level.getBlockEntity(m) != null) {
            weg(level, m, klus);
        }
        level.setBlock(m, blad, VLAG);
        BlockState wol = Blocks.PINK_WOOL.defaultBlockState();
        int bodem = Math.max(level.getMinY(), vloer - DIEPTE);
        for (int y = doel - 1; y > h && y >= bodem; y--) {
            m.setY(y);
            if (!magVullen(y, y < ONDERGRENS && level.structureManager().hasAnyStructureAt(m))) {
                break;
            }
            if (level.getBlockEntity(m) != null) {
                weg(level, m, klus);
            }
            level.setBlock(m, wol, VLAG);
        }
    }
}
