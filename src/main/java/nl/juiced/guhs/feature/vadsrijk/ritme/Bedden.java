package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * U-LEVEN-RITME: the beds in the towns (the houses of the map are built with real beds). Found lazily per loaded chunk (palette
 * check first) and cached: the Vadsrijk is adventure mode, nobody moves a bed. A bed is "claimed" by a sleeper (a shopkeeper's
 * NPC id or an extra sleeper) so two guhs never share one.
 */
public final class Bedden {
    /** A bed: the head block and the direction it faces (the guh lies along it). */
    public record Bed(BlockPos hoofd, Direction richting) {
        /** Where the guh lies: between the head and foot halves, on the mattress. */
        public double x() {
            return hoofd.getX() + 0.5 - richting.getStepX() * 0.5;
        }

        public double y() {
            return hoofd.getY() + 0.5625;
        }

        public double z() {
            return hoofd.getZ() + 0.5 - richting.getStepZ() * 0.5;
        }

        public float yaw() {
            return richting.getOpposite().toYRot();
        }
    }

    private static final Map<Long, List<Bed>> PER_CHUNK = new HashMap<>();
    /** head pos -> who sleeps there. */
    private static final Map<BlockPos, String> BEZET = new HashMap<>();

    private Bedden() {
    }

    static void wis() {
        PER_CHUNK.clear();
        BEZET.clear();
    }

    private static List<Bed> chunk(ServerLevel sl, int cx, int cz) {
        long k = ChunkPos.pack(cx, cz);
        List<Bed> l = PER_CHUNK.get(k);
        if (l != null) {
            return l;
        }
        if (!sl.hasChunk(cx, cz)) {
            return List.of();   // (not loaded: ask again later, don't cache)
        }
        LevelChunk c = sl.getChunk(cx, cz);
        l = new ArrayList<>();
        LevelChunkSection[] secties = c.getSections();
        for (int i = 0; i < secties.length; i++) {
            LevelChunkSection s = secties[i];
            if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> st.getBlock() instanceof BedBlock)) {
                continue;
            }
            int by = SectionPos.sectionToBlockCoord(c.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState st = s.getBlockState(x, y, z);
                        if (st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) {
                            BlockPos p = new BlockPos(c.getPos().getMinBlockX() + x, by + y, c.getPos().getMinBlockZ() + z);
                            // room to lie: nothing solid right above the mattress
                            if (sl.getBlockState(p.above()).getCollisionShape(sl, p.above()).isEmpty()) {
                                l.add(new Bed(p, st.getValue(BedBlock.FACING)));
                            }
                        }
                    }
                }
            }
        }
        PER_CHUNK.put(k, l);
        return l;
    }

    /** The beds within r blocks (flat) of pos, |dy| <= dy, nearest first. */
    public static List<Bed> rond(ServerLevel sl, BlockPos pos, int r, int dy) {
        List<Bed> uit = new ArrayList<>();
        int c0x = (pos.getX() - r) >> 4, c1x = (pos.getX() + r) >> 4, c0z = (pos.getZ() - r) >> 4, c1z = (pos.getZ() + r) >> 4;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                for (Bed b : chunk(sl, cx, cz)) {
                    BlockPos h = b.hoofd();
                    if (Math.abs(h.getY() - pos.getY()) <= dy && h.distToCenterSqr(pos.getX() + 0.5, h.getY() + 0.5, pos.getZ() + 0.5) <= (double) r * r) {
                        uit.add(b);
                    }
                }
            }
        }
        uit.sort((a, b) -> Double.compare(a.hoofd().distSqr(pos), b.hoofd().distSqr(pos)));
        return uit;
    }

    /** Claims the nearest free bed for {@code wie} (an existing claim of wie is returned again), or null. */
    @Nullable
    public static Bed claim(ServerLevel sl, BlockPos pos, int r, String wie) {
        for (Map.Entry<BlockPos, String> e : BEZET.entrySet()) {
            if (e.getValue().equals(wie)) {
                for (Bed b : rond(sl, e.getKey(), 1, 1)) {
                    if (b.hoofd().equals(e.getKey())) {
                        return b;
                    }
                }
            }
        }
        for (Bed b : rond(sl, pos, r, 12)) {
            if (!BEZET.containsKey(b.hoofd())) {
                BEZET.put(b.hoofd(), wie);
                return b;
            }
        }
        return null;
    }

    public static void vrij(String wie) {
        BEZET.values().removeIf(w -> w.equals(wie));
    }

    public static boolean bezet(BlockPos hoofd) {
        return BEZET.containsKey(hoofd);
    }

    public static int bekend() {
        int n = 0;
        for (List<Bed> l : PER_CHUNK.values()) {
            n += l.size();
        }
        return n;
    }
}
