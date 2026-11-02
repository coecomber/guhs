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

/**
 * De Erfenis (POC): puts a {@link Base} into the world. Every function takes the level and the plot's min corner
 * ({@link Plot#oorsprong}), so game tests can stamp in their own area; {@link Erfenis#stempel} finds both for a real base.
 * <ul>
 *   <li>{@link #stempel} everything: the island, the plot cleared, every building (a visit to a base that is behind).</li>
 *   <li>{@link #gebouw} one building: its footprint cleared, then its shape (a placement, an upgrade, a finished build).</li>
 *   <li>{@link #wis} one footprint back to grass (the old spot of a moved building).</li>
 * </ul>
 * Blocks are set without neighbour updates and only where they differ, so stamping again is cheap and changes nothing.
 * Setting a block in an unloaded chunk loads it (the dimension is void, so that is quick).
 */
public final class Stempel {
    private static final int VLAG = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    static final String[] OPA = {"Van opa-guh,", "met liefde.", "Njeg. <3", ""};

    private Stempel() {
    }

    /** The whole base: island, clean plot, all buildings. */
    public static void stempel(ServerLevel level, BlockPos o, Base b) {
        eiland(level, o, b);
        for (int x = 0; x < Plot.MAAT; x++) {
            for (int z = 0; z < Plot.MAAT; z++) {
                kolomLeeg(level, o.offset(x, 0, z));
            }
        }
        for (Gebouw g : b.gebouwen) {
            vorm(level, o, b, g);
        }
        b.moetStempelen = false;
    }

    /** One building: its footprint cleared, then its current shape. */
    public static void gebouw(ServerLevel level, BlockPos o, Base b, Gebouw g) {
        wis(level, o, g.x, g.z, g.type.maat);
        vorm(level, o, b, g);
    }

    /** A footprint back to grass with air above. */
    public static void wis(ServerLevel level, BlockPos o, int x, int z, int maat) {
        for (int dx = 0; dx < maat; dx++) {
            for (int dz = 0; dz < maat; dz++) {
                kolomLeeg(level, o.offset(x + dx, 0, z + dz));
            }
        }
    }

    private static void vorm(ServerLevel level, BlockPos o, Base b, Gebouw g) {
        BlockPos hoek = o.offset(g.x, 0, g.z);
        Vormen.vorm(g.type, g.level, g.rot, b.inAanbouw(g) && g.level > 0, (x, y, z, st) -> {
            BlockPos p = hoek.offset(x, y, z);
            zet(level, p, st);
            if (st.getBlock() instanceof StandingSignBlock && level.getBlockEntity(p) instanceof SignBlockEntity bord) {
                SignText tekst = new SignText();
                for (int i = 0; i < 4; i++) {
                    tekst = tekst.setMessage(i, Component.literal(OPA[i]));
                }
                tekst = tekst.setColor(DyeColor.MAGENTA);
                bord.setText(tekst, true);
                bord.setWaxed(true);
                bord.setChanged();
                level.sendBlockUpdated(p, st, st, Block.UPDATE_CLIENTS);
            }
        });
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
                    .setMessage(2, Component.literal(b.naam)).setMessage(3, Component.literal(b.npc ? "(guhdorp)" : "Njeg!")).setColor(DyeColor.MAGENTA);
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
