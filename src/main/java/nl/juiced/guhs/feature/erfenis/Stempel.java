package nl.juiced.guhs.feature.erfenis;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;

/**
 * De Erfenis: puts a {@link Base} into the world. Every function takes the level and the plot's min corner
 * ({@link Plot#oorsprong}), so game tests can stamp in their own area; {@link Erfenis#stempel} finds both for a real base.
 * The shapes themselves come from {@link GebouwVormen} (the vormen slice; a stand-in until the merge).
 * <ul>
 *   <li>{@link #stempel} everything: the island, the land (overgrown plukjes, kavels for sale), the paths, every building.</li>
 *   <li>{@link #gebouw} one building (a placement, an upgrade, a finished build, a new palette).</li>
 *   <li>{@link #wis} one footprint back to grass (the old spot of a moved building); {@link #plukje} / {@link #kavel} one patch of land.</li>
 * </ul>
 */
public final class Stempel {
    private static final int VLAG = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private Stempel() {
    }

    /** The whole base: island, land, paths, deco, all buildings. */
    public static void stempel(ServerLevel level, BlockPos o, Base b) {
        eiland(level, o, b);
        for (int x = 0; x < Plot.MAAT; x++) {
            for (int z = 0; z < Plot.MAAT; z++) {
                kolomLeeg(level, o.offset(x, 0, z));
            }
        }
        for (int n = 0; n < Plot.PLUKJES; n++) {
            if (!b.opgeruimd[n]) {
                plukje(level, o, b, n);
            }
        }
        for (int k = 0; k < Plot.KAVELS; k++) {
            if (!b.kavelGekocht(k)) {
                kavel(level, o, b, k);
            }
        }
        paden(level, o, b, 0, 0, Plot.MAAT, Plot.MAAT);
        for (Base.Deco d : b.deco) {
            GebouwVormen.stempelDeco(level, d.id(), o.offset(d.x(), 0, d.z()), d.rot());
        }
        for (Gebouw g : b.gebouwen) {
            vorm(level, o, b, g);
        }
        b.moetStempelen = false;
    }

    /** One building: its footprint (the shape clears it first), then scaffolding when an upgrade runs. */
    public static void gebouw(ServerLevel level, BlockPos o, Base b, Gebouw g) {
        vorm(level, o, b, g);
    }

    /** A footprint back to grass with air above. */
    public static void wis(ServerLevel level, BlockPos o, Base b, int x, int z, int w, int h) {
        GebouwVormen.wis(level, o.offset(x, 0, z), w, h);
        paden(level, o, b, x, z, w, h);
    }

    /** One plukje: overgrown, or (cleared) plain grass again. */
    public static void plukje(ServerLevel level, BlockPos o, Base b, int n) {
        int[] h = Plot.plukjeHoek(n);
        if (b.opgeruimd[n]) {
            GebouwVormen.wis(level, o.offset(h[0], 0, h[1]), Plot.PLUK, Plot.PLUK);
        } else {
            GebouwVormen.stempelPlukje(level, o.offset(h[0], 0, h[1]), b.plukjeSoort(n));
        }
    }

    /** One kavel: for sale (wild meadow behind little posts) or (bought) plain grass. */
    public static void kavel(ServerLevel level, BlockPos o, Base b, int k) {
        int[] v = Plot.kavelVak(k);
        GebouwVormen.wis(level, o.offset(v[0], 0, v[1]), v[2], v[3]);
        if (b.kavelGekocht(k)) {
            return;
        }
        java.util.Random rng = new java.util.Random(b.id.getLeastSignificantBits() ^ k * 7919L);
        for (int x = 0; x < v[2]; x++) {
            for (int z = 0; z < v[3]; z++) {
                BlockPos g = o.offset(v[0] + x, 0, v[1] + z);
                zet(level, g, rng.nextInt(5) == 0 ? Blocks.PODZOL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                int r = rng.nextInt(10);
                if (r < 4) {
                    zet(level, g.above(), Blocks.SHORT_GRASS.defaultBlockState());
                } else if (r == 4) {
                    zet(level, g.above(), Blocks.DANDELION.defaultBlockState());
                }
                boolean rand = x == 0 || z == 0 || x == v[2] - 1 || z == v[3] - 1;
                if (rand && (x + z) % 4 == 0) {
                    zet(level, g.above(), Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
    }

    /** The paths in a rectangle (the layer of the base; null = none): every tile with its neighbour bitmask N1 E2 S4 W8. */
    static void paden(ServerLevel level, BlockPos o, Base b, int x0, int z0, int w, int h) {
        if (b == null) {
            return;
        }
        for (int x = x0; x < x0 + w; x++) {
            for (int z = z0; z < z0 + h; z++) {
                int p = pad(b, x, z);
                if (p <= 0 || b.op(x, z) != null) {
                    continue;
                }
                int v = (pad(b, x, z - 1) > 0 ? 1 : 0) | (pad(b, x + 1, z) > 0 ? 2 : 0) | (pad(b, x, z + 1) > 0 ? 4 : 0) | (pad(b, x - 1, z) > 0 ? 8 : 0);
                String soort = p - 1 < b.padSoorten.size() ? b.padSoorten.get(p - 1) : "zandpaadje";
                GebouwVormen.pad(level, o.offset(x, 0, z), soort, v);
            }
        }
    }

    private static int pad(Base b, int x, int z) {
        return Plot.opPlot(x, z) ? b.paden[x * Plot.MAAT + z] & 0xFF : 0;
    }

    private static void vorm(ServerLevel level, BlockPos o, Base b, Gebouw g) {
        BlockPos hoek = o.offset(g.x, 0, g.z);
        // 1.9 afwerking: after the court case the Detectivebureau wears its end look (the L10 shape) at any level
        int vormLevel = g.type == GebouwType.DETECTIVEBUREAU && g.level > 0 && b.beloningen.contains("bureau_eindlook")
                ? GebouwVormen.levels(g.type.id) : g.level;
        GebouwVormen.stempel(level, g.type.id, vormLevel, g.palet, hoek, g.rot);
        Afwerking.naStempel(level, b, g, hoek);   // Opa's kelder under the Hoeve, Bertha on the Muziektent
        if (g.level > 0 && b.inAanbouw(g)) {   // an upgrade runs: scaffolding at the corners
            int hoog = Math.min(Plot.MAX_HOOGTE - 1, g.type.hoogte(g.level));
            for (int[] c : new int[][] {{0, 0}, {g.b() - 1, 0}, {0, g.d() - 1}, {g.b() - 1, g.d() - 1}}) {
                for (int y = 1; y <= hoog; y++) {
                    zet(level, hoek.offset(c[0], y, c[1]), Blocks.SCAFFOLDING.defaultBlockState());
                }
            }
        }
    }

    private static void kolomLeeg(ServerLevel level, BlockPos grond) {
        zet(level, grond, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int y = 1; y <= Plot.MAX_HOOGTE; y++) {
            zet(level, grond.above(y), Blocks.AIR.defaultBlockState());
        }
    }

    private static void zet(ServerLevel level, BlockPos p, BlockState st) {
        if (level.getBlockState(p) != st) {
            level.setBlock(p, st, VLAG);
        }
    }

    /**
     * The grass island around the plot: {@value Plot#EILAND} wide with rounded corners, a cherry-plank path around the build
     * area with lanterns at the corners, a name sign at the entrance, flowers and little blossom bushes in the margin
     * (the same for a base every time), and an underside that tapers off like a floating island.
     */
    public static void eiland(ServerLevel level, BlockPos o, Base b) {
        int rand = (Plot.EILAND - Plot.MAAT) / 2;
        Random rng = new Random(b.id.getLeastSignificantBits() ^ 0x5EEDL);
        for (int x = -rand; x < Plot.MAAT + rand; x++) {
            for (int z = -rand; z < Plot.MAAT + rand; z++) {
                int krimp = 0;
                double d = hoekAfstand(x, z, rand);
                if (d > rand) {
                    continue;
                }
                boolean pad = (x >= -2 && x < Plot.MAAT + 2 && z >= -2 && z < Plot.MAAT + 2) && !(x >= 0 && x < Plot.MAAT && z >= 0 && z < Plot.MAAT);
                BlockPos g = o.offset(x, 0, z);
                if (!(x >= 0 && x < Plot.MAAT && z >= 0 && z < Plot.MAAT)) {
                    zet(level, g, pad ? Blocks.CHERRY_PLANKS.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int y = 1; y <= 4; y++) {
                        zet(level, g.above(y), Blocks.AIR.defaultBlockState());
                    }
                }
                // the underside: dirt, then stone and pink, narrowing
                for (int diep = 1; diep <= 12; diep++) {
                    krimp = Math.max(0, diep - 2) * 2;
                    boolean in = hoekAfstand(x, z, rand - krimp) <= rand - krimp && x >= -rand + krimp && x < Plot.MAAT + rand - krimp
                            && z >= -rand + krimp && z < Plot.MAAT + rand - krimp;
                    BlockState st = !in ? Blocks.AIR.defaultBlockState()
                            : diep <= 3 ? Blocks.DIRT.defaultBlockState()
                            : diep <= 8 ? Blocks.STONE.defaultBlockState() : Blocks.PINK_TERRACOTTA.defaultBlockState();
                    zet(level, g.below(diep), st);
                }
            }
        }
        // lanterns on the path corners, the name sign at the entrance
        for (int[] c : new int[][] {{-2, -2}, {Plot.MAAT + 1, -2}, {-2, Plot.MAAT + 1}, {Plot.MAAT + 1, Plot.MAAT + 1}}) {
            zet(level, o.offset(c[0], 1, c[1]), Blocks.CHERRY_FENCE.defaultBlockState());
            zet(level, o.offset(c[0], 2, c[1]), Blocks.LANTERN.defaultBlockState());
        }
        BlockPos bordPos = o.offset(Plot.MAAT / 2 - 3, 1, Plot.MAAT + 3);
        BlockState bord = Blocks.CHERRY_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0);
        zet(level, bordPos, bord);
        if (level.getBlockEntity(bordPos) instanceof SignBlockEntity be) {
            SignText t = new SignText().setMessage(0, Component.literal("~ De Erfenis ~")).setMessage(1, Component.literal(b.npc ? "van" : "van guh"))
                    .setMessage(2, Component.literal(b.naam)).setMessage(3, Component.literal(b.npc ? "(guhdorp)" : "Opknapper! Njeg!")).setColor(DyeColor.MAGENTA);
            be.setText(t, true);
            be.setWaxed(true);
            be.setChanged();
            level.sendBlockUpdated(bordPos, bord, bord, Block.UPDATE_CLIENTS);
        }
        // flowers and blossom bushes in the margin
        BlockState[] bloemen = {Blocks.PINK_TULIP.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
                Blocks.CORNFLOWER.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), Blocks.PINK_PETALS.defaultBlockState()};
        BlockState blad = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int i = 0; i < 160; i++) {
            int x = -rand + rng.nextInt(Plot.EILAND), z = -rand + rng.nextInt(Plot.EILAND);
            if (x >= -3 && x < Plot.MAAT + 3 && z >= -3 && z < Plot.MAAT + 3 || hoekAfstand(x, z, rand) > rand - 2) {
                continue;
            }
            BlockPos p = o.offset(x, 1, z);
            if (i % 9 == 0) {
                zet(level, p, blad);
                if (rng.nextBoolean()) {
                    zet(level, p.above(), blad);
                }
            } else {
                zet(level, p, bloemen[rng.nextInt(bloemen.length)]);
            }
        }
    }

    /** How far (x, z) lies from the inner rectangle that the island's rounded corners are drawn around. */
    private static double hoekAfstand(int x, int z, int rand) {
        int lo = 0, hi = Plot.MAAT - 1;
        double dx = x < lo ? lo - x : x > hi ? x - hi : 0;
        double dz = z < lo ? lo - z : z > hi ? z - hi : 0;
        if (dx > 0 && dz > 0) {
            return Math.sqrt(dx * dx + dz * dz);
        }
        return Math.max(dx, dz);
    }
}
