package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import nl.juiced.guhs.feature.bouwhulp.vormen.Mix;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * The patterns of the patroonkwast (design 2.4 / 2.9, ronde 30): a gradient, stripes, a checkerboard and little guh faces
 * in the kwast's own blocks, and the animal patterns in the colours of wool, concrete and terracotta: zebra, giraffe,
 * luipaard, jachtluipaard, pinguïn, panda, rode panda and sabeltandtijger. An animal pattern opens once that animal is in
 * your safari dex (photographed).
 */
public final class Patronen {
    public enum Patroon {
        VERLOOP(null), STREEP(null), DAMBORD(null), GUHGEZICHT(null),
        ZEBRA("zebra"), GIRAFFE("giraffe"), LUIPAARD("luipaard"), JACHTLUIPAARD("jachtluipaard"), PINGUIN("pinguin"),
        PANDA("reuzenpanda"), RODE_PANDA("rode_panda"), SABELTAND("sabeltandtijger");

        /** The dex species that opens it (null: always open). */
        @Nullable
        public final String soort;

        Patroon(@Nullable String soort) {
            this.soort = soort;
        }

        public static Patroon van(int i) {
            return values()[Math.floorMod(i, values().length)];
        }

        public boolean dier() {
            return soort != null;
        }

        /** Open for this player (in tests without a player: open)? */
        public boolean open(@Nullable ServerPlayer p) {
            return soort == null || p == null || SafariSpeler.gefotografeerd(p.level().getServer(), p.getUUID()).contains(soort);
        }
    }

    public static final List<String> IDS = Arrays.stream(Patroon.values()).map(p -> p.name().toLowerCase(java.util.Locale.ROOT)).toList();

    /** The area painted (for the patterns that look at its shape: the gradient, the penguin). */
    public record Doos(int x0, int y0, int z0, int x1, int y1, int z1) {
        double t(int y) {
            return y1 == y0 ? 0.5 : (y - y0) / (double) (y1 - y0);
        }
    }

    /** The guh face tile (8 × 8, top row first). */
    private static final String[] GEZICHT = {
            "........",
            "..#..#..",
            "..#..#..",
            "........",
            ".#....#.",
            "..####..",
            "........",
            "........"};

    /** For the kwast's own blocks: which of the mix's blocks (index) goes here. */
    public static int index(Patroon p, BlockPos pos, Doos doos, int n, boolean plat) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return switch (p) {
            case STREEP -> Math.floorMod(y, n);
            case DAMBORD -> Math.floorMod(x + y + z, n);
            case GUHGEZICHT -> {
                int u = plat ? x : x + z, v = plat ? z : y;
                char c = GEZICHT[7 - Math.floorMod(v, 8)].charAt(Math.floorMod(u, 8));
                yield c == '#' ? Math.min(1, n - 1) : 0;
            }
            default -> 0;
        };
    }

    /** For an animal pattern: the colour of this spot. */
    public static DyeColor kleur(Patroon p, BlockPos pos, Doos doos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        double t = doos.t(y);
        return switch (p) {
            case ZEBRA -> Math.floorMod((int) Math.floor(((x + z) * 0.5 + Math.sin(y * 0.45) * 1.3) / 1.4), 2) == 0 ? DyeColor.BLACK : DyeColor.WHITE;
            case GIRAFFE -> {
                double[] v = voronoi(x, y, z, 4.0);
                yield v[1] - v[0] < 0.7 ? DyeColor.WHITE : (Math.floorMod((int) v[2], 3) == 0 ? DyeColor.BROWN : DyeColor.ORANGE);
            }
            case LUIPAARD -> {
                double d = voronoi(x, y, z, 3.5)[0];
                yield d < 0.6 ? DyeColor.ORANGE : d < 1.15 ? DyeColor.BLACK : DyeColor.YELLOW;
            }
            case JACHTLUIPAARD -> voronoi(x, y, z, 2.6)[0] < 0.55 ? DyeColor.BLACK : DyeColor.YELLOW;
            case PINGUIN -> {
                double cx = (doos.x0 + doos.x1) / 2.0, cz = (doos.z0 + doos.z1) / 2.0;
                double breed = Math.max(1, Math.max(doos.x1 - doos.x0, doos.z1 - doos.z0) / 2.0);
                if (t < 0.08) {
                    yield DyeColor.ORANGE;
                }
                yield t > 0.68 || (Math.abs(x - cx) + Math.abs(z - cz)) / breed > 0.75 ? DyeColor.BLACK : DyeColor.WHITE;
            }
            case PANDA -> voronoi(x, y, z, 6.0)[0] < 1.6 ? DyeColor.BLACK : DyeColor.WHITE;
            case RODE_PANDA -> t < 0.12 ? DyeColor.BLACK : Math.floorMod(y, 4) < 2 ? DyeColor.ORANGE : DyeColor.BROWN;
            case SABELTAND -> {
                if (t < 0.15) {
                    yield DyeColor.WHITE;
                }
                yield Math.floorMod((int) Math.floor((x + z) * 0.6 + Math.sin(y * 0.5) * 1.5), 4) == 0 ? DyeColor.BLACK : DyeColor.ORANGE;
            }
            default -> DyeColor.WHITE;
        };
    }

    /** Cellular noise: distance to the nearest and second nearest feature point (in cells), and the nearest cell's hash. */
    static double[] voronoi(int x, int y, int z, double cel) {
        double fx = x / cel, fy = y / cel, fz = z / cel;
        int cx = (int) Math.floor(fx), cy = (int) Math.floor(fy), cz = (int) Math.floor(fz);
        double d1 = 9, d2 = 9;
        int beste = 0;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int c = -1; c <= 1; c++) {
                    int h = Mix.hash(cx + a, cy + b, cz + c);
                    double px = cx + a + (h & 255) / 255.0, py = cy + b + (h >> 8 & 255) / 255.0, pz = cz + c + (h >> 16 & 255) / 255.0;
                    double d = Math.sqrt((px - fx) * (px - fx) + (py - fy) * (py - fy) + (pz - fz) * (pz - fz)) * cel / 2.0;
                    if (d < d1) {
                        d2 = d1;
                        d1 = d;
                        beste = h;
                    } else if (d < d2) {
                        d2 = d;
                    }
                }
            }
        }
        return new double[] {d1, d2, Math.abs(beste % 1000)};
    }

    private Patronen() {
    }
}
