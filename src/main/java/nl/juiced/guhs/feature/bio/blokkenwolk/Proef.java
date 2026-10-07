package nl.juiced.guhs.feature.bio.blokkenwolk;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.eilanden.EilandenFeature;
import nl.juiced.guhs.feature.eilanden.WolkenliftBlock;
import nl.juiced.guhs.feature.eilanden.WolkenstroomBlock;

/**
 * DEV: {@code /guhs bio blokken-wolk proef [x y z]} builds everything of this slice in one row, east of the spot (44 x 14 x
 * 16 blocks, in the air if you stand in the air), to look at and listen to in the real game:
 * a bank of white and pink cloud rounded off with slabs, a rainbow arc three wide, the furniture on a cloud floor, a pool
 * with the three densities of petals, a waterfall of 8 blocks (foam, mist, sound) next to a drop of 2 (nothing), and a
 * wolkenlift column up and one down. Gamemasters only; it only places blocks.
 * <p>
 * {@code /guhs bio blokken-wolk waterval <x y z>} says what the client's waterfall search finds in that column.
 */
public final class Proef {
    static void commando(RegisterCommandsEvent event) {
        var proef = Commands.literal("proef")
                .executes(ctx -> bouw(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
                .then(Commands.argument("plek", BlockPosArgument.blockPos())
                        .executes(ctx -> bouw(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "plek"))));
        // what the client's waterfall search would find in the column of this spot (24 blocks up and down)
        var waterval = Commands.literal("waterval").then(Commands.argument("plek", BlockPosArgument.blockPos()).executes(ctx -> {
            BlockPos p = BlockPosArgument.getBlockPos(ctx, "plek");
            Waterval.Voet v = Waterval.zoek(ctx.getSource().getLevel(), p.getX(), p.getZ(), p.getY() + 24, p.getY() - 24);
            String tekst = v == null ? "geen vallend water in deze kolom"
                    : "voet y=" + v.y() + ", valhoogte " + v.hoogte() + (v.inWater() ? " (in water)" : " (op de grond)")
                    + (v.groot() ? ": GROOT, schuim nevel en geruis" : ": klein, niets");
            ctx.getSource().sendSuccess(() -> Component.literal("[blokken-wolk] waterval " + p.getX() + " " + p.getZ() + ": " + tekst), false);
            return v != null && v.groot() ? 1 : 0;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("blokken-wolk").then(proef).then(waterval))));
    }

    private static int bouw(CommandSourceStack bron, BlockPos plek) {
        BlockPos o = plek.east(3);
        int blokken = bouw(bron.getLevel(), o);
        bron.sendSuccess(() -> Component.literal("[blokken-wolk] proef: " + blokken + " blokken vanaf " + o.toShortString()
                + " naar het oosten: wolkenbank, regenboog, meubels, blaadjes, waterval (8 en 2 hoog), wolkenlift op en neer"), true);
        return blokken;
    }

    /** Builds the row with its north-west bottom corner at o; returns how many blocks it placed. */
    public static int bouw(ServerLevel level, BlockPos o) {
        int[] n = {0};
        java.util.function.BiConsumer<BlockPos, BlockState> zet = (p, s) -> {
            level.setBlock(p, s, 3);
            n[0]++;
        };
        BlockState wit = BlokkenWolkSlice.WOLKENBLOK_WIT.get().defaultBlockState(), roze = BlokkenWolkSlice.WOLKENBLOK_ROZE.get().defaultBlockState();

        // 1. the cloud bank (x 0..12, z 0..10): two soft lumps, the rim rounded off with slabs
        double[][] bollen = {{6, 3, 5, 6.2, 3.2, 5.2, 0}, {9, 4.6, 7, 3.6, 2.4, 3.2, 1}};
        for (int x = -1; x <= 13; x++) {
            for (int z = -1; z <= 11; z++) {
                for (int y = 0; y <= 8; y++) {
                    double d = 9, kleur = 0;
                    for (double[] b : bollen) {
                        double v = sq((x + 0.5 - b[0]) / b[3]) + sq((y + 0.5 - b[1]) / b[4]) + sq((z + 0.5 - b[2]) / b[5]);
                        if (v < d) {
                            d = v;
                            kleur = b[6];
                        }
                    }
                    BlockPos p = o.offset(x, y, z);
                    if (d <= 1) {
                        zet.accept(p, kleur == 0 ? wit : roze);
                    } else if (d < 1.4) {
                        Block plaat = kleur == 0 ? BlokkenWolkSlice.WOLKENBLOK_WIT_PLAAT.get() : BlokkenWolkSlice.WOLKENBLOK_ROZE_PLAAT.get();
                        boolean boven = y + 0.5 > (kleur == 0 ? 3 : 4.6);
                        zet.accept(p, plaat.defaultBlockState().setValue(SlabBlock.TYPE, boven ? SlabType.BOTTOM : SlabType.TOP));
                    }
                }
            }
        }

        // 2. the rainbow (x 14..33, z 2..4): an arc three wide; level -> block, climbing a block -> stairs, between -> slabs
        BlockState blok = BlokkenWolkSlice.REGENBOOGBLOK.get().defaultBlockState().setValue(RegenboogBlokken.AS, Direction.Axis.X);
        BlockState plaat = BlokkenWolkSlice.REGENBOOGBLOK_PLAAT.get().defaultBlockState().setValue(RegenboogBlokken.AS, Direction.Axis.X);
        BlockState trap = BlokkenWolkSlice.REGENBOOGBLOK_TRAP.get().defaultBlockState();
        int x0 = 14, x1 = 33;
        for (int x = x0; x <= x1; x++) {
            int hier = boog(x, x0, x1), links = x > x0 ? boog(x - 1, x0, x1) : hier, rechts = x < x1 ? boog(x + 1, x0, x1) : hier;
            for (int z = 2; z <= 4; z++) {
                if (hier % 2 == 0) {
                    BlockPos p = o.offset(x, hier / 2 - 1, z);
                    zet.accept(p, links < hier && hier <= rechts ? trap.setValue(StairBlock.FACING, Direction.EAST)
                            : rechts < hier && hier <= links ? trap.setValue(StairBlock.FACING, Direction.WEST) : blok);
                } else {
                    zet.accept(o.offset(x, (hier - 1) / 2, z), plaat.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    zet.accept(o.offset(x, (hier - 1) / 2 - 1, z), plaat.setValue(SlabBlock.TYPE, SlabType.TOP));
                }
            }
        }
        // (placed one by one: every piece now looks at its neighbours, as it does when a player builds)
        for (int x = x0; x <= x1; x++) {
            for (int y = -1; y <= 9; y++) {
                for (int z = 2; z <= 4; z++) {
                    BlockPos p = o.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof RegenboogBlokken.Deel deel) {
                        level.setBlock(p, s.setValue(RegenboogBlokken.STROOK, RegenboogBlokken.strook(level, p, deel.as(s))), 3);
                    }
                }
            }
        }

        // 3. the furniture on a cloud floor (x 14..24, z 8..13)
        for (int x = 14; x <= 24; x++) {
            for (int z = 8; z <= 13; z++) {
                zet.accept(o.offset(x, 0, z), x == 14 || x == 24 || z == 8 || z == 13 ? roze : wit);
            }
        }
        BlockState bank = BlokkenWolkSlice.WOLKENBANK.get().defaultBlockState().setValue(nl.juiced.guhs.block.GuhFurnitureBlock.FACING, Direction.SOUTH);
        zet.accept(o.offset(16, 1, 9), bank);
        zet.accept(o.offset(17, 1, 9), bank);
        BlockState bed = BlokkenWolkSlice.WOLKENBED.get().defaultBlockState().setValue(WolkenbedBlock.FACING, Direction.NORTH);
        zet.accept(o.offset(20, 1, 10), bed);
        zet.accept(o.offset(20, 1, 9), bed.setValue(WolkenbedBlock.PART, BedPart.HEAD));
        zet.accept(o.offset(22, 1, 9), BlokkenWolkSlice.WOLKENLAMP.get().defaultBlockState());
        zet.accept(o.offset(18, 3, 12), BlokkenWolkSlice.WOLKENLAMP.get().defaultBlockState());

        // 4. the pool with petals (x 26..33, z 8..15): one column per density, the last two columns left open
        for (int x = 26; x <= 33; x++) {
            for (int z = 8; z <= 15; z++) {
                boolean rand = x == 26 || x == 33 || z == 8 || z == 15;
                zet.accept(o.offset(x, 0, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState());      // (not sand: in the air it would fall)
                zet.accept(o.offset(x, 1, z), rand ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
            }
        }
        for (int x = 27; x <= 30; x++) {
            for (int z = 9; z <= 14; z++) {
                int dichtheid = x <= 28 ? 1 : x - 27;       // (two columns of the loose default, then 2, then 3)
                if (dichtheid > 1 || (x + z) % 3 != 0) {
                    zet.accept(o.offset(x, 2, z), BlokkenWolkSlice.BLOESEMBLAADJES.get().defaultBlockState().setValue(BloesemblaadjesBlock.DICHTHEID, dichtheid));
                }
            }
        }

        // 5. the waterfalls (x 36..43, z 0..8): a basin, a wall of 8 with a spout (big), a wall of 2 with a spout (small)
        for (int x = 36; x <= 43; x++) {
            for (int z = 0; z <= 8; z++) {
                boolean rand = x == 36 || x == 43 || z == 0 || z == 8;
                zet.accept(o.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                if (rand) {
                    zet.accept(o.offset(x, 1, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        spuit(level, zet, o.offset(41, 1, 1), 8);
        spuit(level, zet, o.offset(38, 1, 1), 2);

        // 6. the wolkenlift (x 36..42, z 11..13): a column up and a column down, 3 x 3 as on the islands, 12 high
        for (int k = 0; k < 2; k++) {
            boolean omlaag = k == 1;
            BlockState pad = EilandenFeature.WOLKENLIFT.get().defaultBlockState().setValue(WolkenliftBlock.FACING, Direction.SOUTH)
                    .setValue(WolkenstroomBlock.DOWN, omlaag);
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    BlockPos p = o.offset(36 + k * 4 + dx, 0, 11 + dz);
                    zet.accept(p.above(13), Blocks.GLASS.defaultBlockState());       // (the column stops under it)
                    zet.accept(p, pad);
                    n[0] += WolkenliftBlock.buildColumn(level, p, pad);
                }
            }
        }
        return n[0];
    }

    /** A wall with a spout on top: the water runs off its south side and falls `hoog` blocks into the basin. */
    private static void spuit(ServerLevel level, java.util.function.BiConsumer<BlockPos, BlockState> zet, BlockPos voet, int hoog) {
        BlockState steen = Blocks.STONE_BRICKS.defaultBlockState();
        for (int y = 0; y < hoog; y++) {
            zet.accept(voet.above(y), steen);
        }
        BlockPos bron = voet.above(hoog);
        zet.accept(bron.west(), steen);
        zet.accept(bron.east(), steen);
        zet.accept(bron.north(), steen);
        zet.accept(bron.below(), steen);
        zet.accept(bron, Blocks.WATER.defaultBlockState());
    }

    /** The height of the arc's walking surface at x, in half blocks. */
    private static int boog(int x, int x0, int x1) {
        double t = (x - x0 + 0.5) / (x1 - x0 + 1);
        return (int) Math.round(2 * (2 + 6 * Math.pow(Math.sin(Math.PI * t), 0.85)));
    }

    private static double sq(double v) {
        return v * v;
    }

    private Proef() {
    }
}
