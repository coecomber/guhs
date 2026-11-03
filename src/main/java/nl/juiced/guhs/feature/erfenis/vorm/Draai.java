package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Quarter turns clockwise (seen from above, like the POC: rot 1 turns north into east) for positions and block states.
 * Block states are turned generically, so the guhs blocks that don't override {@code rotate} turn too: every horizontal
 * {@link Direction} property, every {@link Direction.Axis} property (x/z swap on odd turns), the side properties
 * north/east/south/west (fences, walls, panes, vines: their values move round) and a 16-step {@code rotation}.
 */
final class Draai {
    private static final Map<BlockState, BlockState[]> CACHE = new ConcurrentHashMap<>();
    private static final String[] ZIJDEN = {"north", "east", "south", "west"};

    private Draai() {
    }

    /** Local (x, z) in a w x d shape turned rot times: the x of the turned footprint. */
    static int x(int x, int z, int w, int d, int rot) {
        return switch (rot & 3) {
            case 1 -> d - 1 - z;
            case 2 -> w - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    static int z(int x, int z, int w, int d, int rot) {
        return switch (rot & 3) {
            case 1 -> x;
            case 2 -> d - 1 - z;
            case 3 -> w - 1 - x;
            default -> z;
        };
    }

    /** The same for a continuous point (entity positions): {x, z}. */
    static double[] punt(double x, double z, int w, int d, int rot) {
        return switch (rot & 3) {
            case 1 -> new double[] {d - z, x};
            case 2 -> new double[] {w - x, d - z};
            case 3 -> new double[] {z, w - x};
            default -> new double[] {x, z};
        };
    }

    static BlockState state(BlockState s, int rot) {
        int r = rot & 3;
        if (r == 0) {
            return s;
        }
        BlockState[] c = CACHE.computeIfAbsent(s, k -> new BlockState[4]);
        BlockState t = c[r];
        if (t == null) {
            t = s;
            for (int i = 0; i < r; i++) {
                t = kwart(t);
            }
            c[r] = t;
        }
        return t;
    }

    /** One quarter turn clockwise. */
    private static BlockState kwart(BlockState s) {
        BlockState t = s;
        Property<?>[] kant = new Property<?>[4];
        boolean zijden = false;
        for (Property<?> p : s.getProperties()) {
            int z = zijde(p.getName());
            if (z >= 0) {
                kant[z] = p;
                zijden = true;
            } else if (p.getValueClass() == Direction.class) {
                t = dir(t, p);
            } else if (p.getValueClass() == Direction.Axis.class) {
                t = as(t, p);
            } else if (p.getName().equals("rotation") && p.getPossibleValues().size() == 16) {
                t = rotatie(t, p);
            }
        }
        if (zijden) {
            for (int z = 0; z < 4; z++) {
                Property<?> van = kant[z], naar = kant[(z + 1) & 3];
                if (van != null && naar != null) {
                    t = kopie(t, s, van, naar);
                }
            }
        }
        return t;
    }

    private static int zijde(String n) {
        for (int i = 0; i < 4; i++) {
            if (ZIJDEN[i].equals(n)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState kopie(BlockState t, BlockState bron, Property<?> van, Property<?> naar) {
        if (van.getValueClass() != naar.getValueClass()) {
            return t;
        }
        Property<T> a = (Property<T>) van, b = (Property<T>) naar;
        T v = bron.getValue(a);
        return b.getPossibleValues().contains(v) ? t.setValue(b, v) : t;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState dir(BlockState t, Property<?> p) {
        Property<T> q = (Property<T>) p;
        Direction d = (Direction) t.getValue(q);
        if (d.getAxis().isVertical()) {
            return t;
        }
        T nieuw = (T) d.getClockWise();
        return q.getPossibleValues().contains(nieuw) ? t.setValue(q, nieuw) : t;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState as(BlockState t, Property<?> p) {
        Property<T> q = (Property<T>) p;
        Direction.Axis a = (Direction.Axis) t.getValue(q);
        Direction.Axis n = a == Direction.Axis.X ? Direction.Axis.Z : a == Direction.Axis.Z ? Direction.Axis.X : a;
        return q.getPossibleValues().contains((T) n) ? t.setValue(q, (T) n) : t;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState rotatie(BlockState t, Property<?> p) {
        Property<T> q = (Property<T>) p;
        Integer v = (Integer) t.getValue(q);
        return t.setValue(q, (T) Integer.valueOf((v + 4) & 15));
    }
}
