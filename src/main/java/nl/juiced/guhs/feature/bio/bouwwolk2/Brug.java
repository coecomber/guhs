package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;

/**
 * The measures of the regenboogbrug template (tools/features/bio_bouw_wolk2_bouw.py; x east, z south: the arc runs along
 * x) and the check that it can be walked: a pure look at the blocks of a template, no world needed.
 */
public final class Brug {
    /** The first and last column of the arc, its north lane (three lanes), and the y of the deck blocks its feet stand on. */
    public static final int BOOG_X0 = 13, BOOG_X1 = 47, BOOG_Z = 14, BANEN = 3, DEK = 36;
    /** The pot at the east end. */
    public static final BlockPos POT = new BlockPos(53, 37, 15);
    /** What a player steps up without jumping. */
    public static final double STAP = 0.6;

    /** The blocks of a template by their place in it (air and structure voids left out). */
    public static Map<BlockPos, BlockState> blokken(ServerLevel level, String naam) {
        Map<BlockPos, BlockState> uit = new HashMap<>();
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
        if (template == null) {
            return uit;
        }
        CompoundTag tag = template.save(new CompoundTag());
        HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
        ListTag palette = tag.getListOrEmpty("palette");
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            states.add(NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i)));
        }
        ListTag entries = tag.getListOrEmpty("blocks");
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompoundOrEmpty(i);
            ListTag pos = e.getListOrEmpty("pos");
            int s = e.getIntOr("state", 0);
            if (s < 0 || s >= states.size() || states.get(s).isAir() || states.get(s).is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            uit.put(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), states.get(s));
        }
        return uit;
    }

    /** What walking a lane found. */
    public record Loop(int stappen, int gaten, double hoogsteStap, double diepsteStap, int zonderRuimte, double top) {
        /** Walkable both ways: ground everywhere, no step up or down higher than a player takes, room to stand. */
        public boolean goed() {
            return stappen > 0 && gaten == 0 && hoogsteStap <= STAP && diepsteStap <= STAP && zonderRuimte == 0;
        }
    }

    /** The height a player stands at on this spot (x, z in blocks; quarter-block precision), looking between two heights; NaN: nothing. */
    public static double grond(Map<BlockPos, BlockState> blokken, double x, double z, int yVan, int yTot) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        double fx = x - bx, fz = z - bz;
        for (int y = yTot; y >= yVan; y--) {
            BlockPos p = new BlockPos(bx, y, bz);
            BlockState s = blokken.get(p);
            if (s == null) {
                continue;
            }
            double top = Double.NaN;
            for (AABB doos : s.getCollisionShape(EmptyBlockGetter.INSTANCE, p).toAabbs()) {
                if (fx >= doos.minX - 1e-6 && fx <= doos.maxX + 1e-6 && fz >= doos.minZ - 1e-6 && fz <= doos.maxZ + 1e-6) {
                    top = Double.isNaN(top) ? doos.maxY : Math.max(top, doos.maxY);
                }
            }
            if (!Double.isNaN(top)) {
                return y + top;
            }
        }
        return Double.NaN;
    }

    /**
     * Walks one lane (the middle of the blocks at this z) from x0 to x1 in half-block steps: is there ground under every
     * step, how high is the highest step up or down, and is there room to stand (two blocks of nothing solid above)?
     */
    public static Loop loop(Map<BlockPos, BlockState> blokken, int x0, int x1, int z, int yVan, int yTot) {
        int stappen = 0, gaten = 0, zonder = 0;
        double hoogste = 0, diepste = 0, vorige = Double.NaN, top = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < (x1 - x0 + 1) * 2; i++) {
            double x = x0 + 0.25 + i * 0.5;
            double h = grond(blokken, x, z + 0.5, yVan, yTot);
            stappen++;
            if (Double.isNaN(h)) {
                gaten++;
                vorige = Double.NaN;
                continue;
            }
            top = Math.max(top, h);
            if (!Double.isNaN(vorige)) {
                hoogste = Math.max(hoogste, Math.abs(h - vorige) > 0 && h > vorige ? h - vorige : 0);
                diepste = Math.max(diepste, vorige > h ? vorige - h : 0);
            }
            vorige = h;
            // room: nothing with a collision box in the 1.8 blocks above the feet
            for (int y = (int) Math.floor(h + 0.01); y <= (int) Math.floor(h + 1.79); y++) {
                BlockPos p = new BlockPos((int) Math.floor(x), y, z);
                BlockState s = blokken.get(p);
                if (s == null) {
                    continue;
                }
                for (AABB doos : s.getCollisionShape(EmptyBlockGetter.INSTANCE, p).toAabbs()) {
                    if (y + doos.maxY > h + 0.01 && y + doos.minY < h + 1.8 && x - Math.floor(x) >= doos.minX - 1e-6 && x - Math.floor(x) <= doos.maxX + 1e-6
                            && y + doos.minY >= h - 1e-6) {
                        zonder++;
                    }
                }
            }
        }
        return new Loop(stappen, gaten, hoogste, diepste, zonder, top);
    }

    /** Is there something to land on under this column within this many blocks below y (the streak of cloud under the span)? */
    public static boolean vangnet(Map<BlockPos, BlockState> blokken, int x, int z, int y, int diep) {
        for (int d = 1; d <= diep; d++) {
            BlockState s = blokken.get(new BlockPos(x, y - d, z));
            if (s != null && !s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Brug() {
    }
}
