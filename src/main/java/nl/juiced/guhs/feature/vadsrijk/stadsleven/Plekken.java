package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * U-LEVEN-STAD: where town life can happen, read from the REAL world around the players (nothing generated, so every town of
 * every zone works, zone 6 too): per chunk and height band one scan of all columns, cached in memory (never saved), a few
 * chunks per second near the players.
 * <ul>
 *   <li>{@link Soort#GROND}: a standable cell (solid below, two free cells above, no water), {@link Soort#GRAS} the grassy ones;</li>
 *   <li>{@link Soort#DAK}: a standable roof cell (a roof block at least 3 above another standable surface in the column);</li>
 *   <li>{@link Soort#WATER}: a water surface with air above (ponds, canals, the harbour);</li>
 *   <li>{@link Soort#LAMP}: the top of a lantern/lamp post with air above (birds land there);</li>
 *   <li>{@link Soort#ZIT}: a Guhs bench or chair (benches face their {@code facing}).</li>
 * </ul>
 */
public final class Plekken {
    public enum Soort {
        GROND, GRAS, DAK, WATER, LAMP, ZIT
    }

    /** One spot: the cell (the air cell above the block; for water the water cell itself), the yaw a sitter faces. */
    public record Plek(BlockPos pos, float yaw) {
        public Vec3 midden() {
            return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
    }

    static final int BAND = 8;
    private static final int MAX_VAKKEN = 900;
    private static final Map<Long, Vak> CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Vak> e) {
            return size() > MAX_VAKKEN;
        }
    };
    /** Block kinds (bit flags), per state. */
    private static final Map<BlockState, Integer> AARD = new IdentityHashMap<>();
    private static final int GRAS_B = 1, DAK_B = 2, LAMP_B = 4, ZIT_B = 8, BLAD_B = 16, GEEN_VLOER_B = 32;
    /** Scans left this tick (the manager refills). */
    static int budget = 2;
    static int gescand;

    private Plekken() {
    }

    static void wis() {
        CACHE.clear();
        AARD.clear();
        gescand = 0;
    }

    static int aantalVakken() {
        return CACHE.size();
    }

    private static final class Vak {
        @SuppressWarnings("unchecked")
        final List<Plek>[] lijst = new List[Soort.values().length];

        Vak() {
            for (int i = 0; i < lijst.length; i++) {
                lijst[i] = new ArrayList<>();
            }
        }
    }

    private static long sleutel(int cx, int cz, int band) {
        return ((long) cx & 0x3FFFFFL) | (((long) cz & 0x3FFFFFL) << 22) | (((long) band & 0xFFFFFL) << 44);
    }

    private static int aard(BlockState s) {
        Integer a = AARD.get(s);
        if (a != null) {
            return a;
        }
        String n = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        int k = 0;
        if (n.contains("grass_block") || n.contains("gras") || n.contains("moss_block") || n.contains("podzol")) {
            k |= GRAS_B;
        }
        if (n.contains("dakpan") || n.contains("stairs") || n.contains("slab") || n.contains("roof") || n.contains("shingle")
                || n.contains("planks") || n.contains("terracotta") || n.contains("wool") || n.contains("dak")) {
            k |= DAK_B;
        }
        if (n.contains("lantern") || n.contains("lantaarn") || n.contains("lampion") || n.endsWith("_lamp") || n.contains("lamp_")) {
            k |= LAMP_B;
        }
        if (n.startsWith("guh_bankje") || n.equals("guh_bank") || n.equals("bank_guh") || n.equals("guh_stoel") || n.equals("guh_hoofdbank")
                || n.equals("buizenbank")) {
            k |= ZIT_B;
        }
        if (n.contains("leaves") || n.contains("blad")) {
            k |= BLAD_B;
        }
        if (n.contains("fence") || n.contains("wall") || n.contains("pane") || n.contains("bars") || n.contains("door") || n.contains("chain")
                || n.contains("ladder") || n.contains("rail") || n.contains("sign") || n.contains("torch") || n.contains("candle")
                || n.contains("slinger") || n.contains("vlaggetjes") || n.contains("vadstouw") || n.contains("bed")) {
            k |= GEEN_VLOER_B;
        }
        AARD.put(s, k);
        return k;
    }

    /** One chunk x band: every column from the band top down (band*8-12 .. band*8+20). */
    private static Vak scan(ServerLevel sl, LevelChunk c, int band) {
        Vak v = new Vak();
        int y0 = band * BAND - 12, y1 = band * BAND + 20;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ();
        List<Integer> vlakken = new ArrayList<>();
        List<Integer> daken = new ArrayList<>();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = bx + dx, z = bz + dz;
                vlakken.clear();
                daken.clear();
                boolean boven1 = false, boven2 = false;   // the cells y+1 and y+2 are free
                for (int y = y1; y >= y0; y--) {
                    m.set(x, y, z);
                    BlockState s = c.getBlockState(m);
                    boolean leeg = s.getCollisionShape(sl, m).isEmpty();
                    boolean nat = !s.getFluidState().isEmpty();
                    int a = aard(s);
                    if (nat && s.getFluidState().is(FluidTags.WATER) && boven1 && y + 1 <= y1) {
                        v.lijst[Soort.WATER.ordinal()].add(new Plek(new BlockPos(x, y, z), 0));
                    }
                    if ((a & ZIT_B) != 0 && boven1) {
                        float yaw = 0;
                        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            yaw = s.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
                        }
                        v.lijst[Soort.ZIT.ordinal()].add(new Plek(new BlockPos(x, y, z), yaw));
                    }
                    if ((a & LAMP_B) != 0 && boven1 && boven2) {
                        v.lijst[Soort.LAMP.ordinal()].add(new Plek(new BlockPos(x, y + 1, z), 0));
                    }
                    if (!leeg && !nat && boven1 && boven2 && (a & (BLAD_B | GEEN_VLOER_B | LAMP_B | ZIT_B)) == 0) {
                        vlakken.add(y);
                        if ((a & GRAS_B) != 0) {
                            v.lijst[Soort.GRAS.ordinal()].add(new Plek(new BlockPos(x, y + 1, z), 0));
                        }
                        if ((a & DAK_B) != 0) {
                            daken.add(y);   // a roof-ish surface (decided below)
                        }
                    }
                    boven2 = boven1;
                    boven1 = leeg && !nat;
                }
                // the lowest surface is the street/ground; roof-ish surfaces >= 3 higher are roofs
                if (vlakken.isEmpty()) {
                    continue;
                }
                int laagste = Integer.MAX_VALUE;
                for (int y : vlakken) {
                    laagste = Math.min(laagste, y);
                }
                for (int y : vlakken) {
                    boolean dak = y >= laagste + 3 && daken.contains(y);
                    v.lijst[(dak ? Soort.DAK : Soort.GROND).ordinal()].add(new Plek(new BlockPos(x, y + 1, z), 0));
                }
            }
        }
        return v;
    }

    @Nullable
    private static Vak vak(ServerLevel sl, int cx, int cz, int band, boolean magScannen) {
        long k = sleutel(cx, cz, band);
        Vak v = CACHE.get(k);
        if (v != null || !magScannen || budget <= 0) {
            return v;
        }
        LevelChunk c = sl.getChunkSource().getChunkNow(cx, cz);
        if (c == null) {
            return null;
        }
        budget--;
        gescand++;
        v = scan(sl, c, band);
        CACHE.put(k, v);
        return v;
    }

    public static int band(double y) {
        return Mth.floor(y) >> 3;
    }

    /** Warm the cache around a player (the manager calls this a few times per second). */
    static void verken(ServerLevel sl, Vec3 p, int straalChunks) {
        int band = band(p.y);
        int pcx = Mth.floor(p.x) >> 4, pcz = Mth.floor(p.z) >> 4;
        for (int r = 0; r <= straalChunks && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                        vak(sl, pcx + dx, pcz + dz, band, true);
                    }
                }
            }
        }
    }

    /** Spots of this kind between rMin and rMax (flat distance) from p, from the cached chunks only, shuffled. */
    public static List<Plek> rond(ServerLevel sl, Vec3 p, double rMin, double rMax, Soort s, RandomSource r) {
        List<Plek> uit = new ArrayList<>();
        int band = band(p.y);
        int c0x = Mth.floor(p.x - rMax) >> 4, c1x = Mth.floor(p.x + rMax) >> 4;
        int c0z = Mth.floor(p.z - rMax) >> 4, c1z = Mth.floor(p.z + rMax) >> 4;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                Vak v = vak(sl, cx, cz, band, false);
                if (v == null) {
                    continue;
                }
                for (Plek pl : v.lijst[s.ordinal()]) {
                    double dx = pl.pos.getX() + 0.5 - p.x, dz = pl.pos.getZ() + 0.5 - p.z;
                    double d = dx * dx + dz * dz;
                    if (d >= rMin * rMin && d <= rMax * rMax) {
                        uit.add(pl);
                    }
                }
            }
        }
        Collections.shuffle(uit, new java.util.Random(r.nextLong()));
        return uit;
    }

    // ---- walking on the ground ------------------------------------------------------------------------------------------------

    /** The ground surface under x/z near yHint (one step up, two down), or NaN. */
    public static double grondY(ServerLevel sl, double x, double yHint, double z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int top = Mth.floor(yHint) + 1, bodem = Mth.floor(yHint) - 2;
        for (int y = top; y >= bodem; y--) {
            m.set(Mth.floor(x), y, Mth.floor(z));
            BlockState s = sl.getBlockState(m);
            if (!s.getFluidState().isEmpty()) {
                return Double.NaN;
            }
            var vorm = s.getCollisionShape(sl, m);
            if (!vorm.isEmpty()) {
                double t = y + vorm.max(Direction.Axis.Y);
                if (t > yHint + 1.05) {
                    return Double.NaN;   // a wall in the way
                }
                BlockPos boven = m.above();
                BlockState b = sl.getBlockState(boven);
                if (!b.getCollisionShape(sl, boven).isEmpty() && t > boven.getY() + 0.01) {
                    return Double.NaN;
                }
                return t;
            }
        }
        return Double.NaN;
    }

    /** Can a small walker go in a straight line from a to b (steps of at most maxStap, never through water or walls)? */
    public static boolean loopbaar(ServerLevel sl, Vec3 a, Vec3 b, double maxStap) {
        double d = Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z));
        int n = Math.max(1, (int) Math.ceil(d / 0.4));
        double y = a.y;
        for (int i = 1; i <= n; i++) {
            double f = i / (double) n;
            double x = Mth.lerp(f, a.x, b.x), z = Mth.lerp(f, a.z, b.z);
            double g = grondY(sl, x, y, z);
            if (Double.isNaN(g) || Math.abs(g - y) > maxStap) {
                return false;
            }
            BlockPos hoofd = BlockPos.containing(x, g + 0.6, z);
            if (!sl.getBlockState(hoofd).getCollisionShape(sl, hoofd).isEmpty()) {
                return false;
            }
            y = g;
        }
        return Math.abs(y - b.y) <= maxStap + 0.1;
    }

    /** Is there water (a surface) here with air above? */
    public static boolean water(ServerLevel sl, BlockPos p) {
        return sl.getFluidState(p).is(FluidTags.WATER) && sl.getBlockState(p.above()).isAir();
    }
}
