package nl.juiced.guhs.feature.erfenis;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Opa's kelder (Sherlock zaak 5, "De kelder van Opa Knabbel"): a cosy old cheese cellar under the Hoeve, reached through a
 * trapdoor in the Hoeve's floor (or, if the floor has no free spot, in its yard) with a ladder down. Inside: Opa's diary on
 * a lectern, barrels with candles, bookshelves, wheels of cheese, decorated pots for your finds, a lantern and a little sign.
 * Stamped after every stamp of the Hoeve (the shape writes the dirt layer under it), always the same for the same spot.
 * <p>
 * The room: under the footprint inset by one (walls), the inside inset by two, floor 5 below the grass, 3 high.
 */
final class OpasKelder {
    static final int VLOER = -5, PLAFOND = -1;
    private static final int VLAG = Block.UPDATE_CLIENTS;

    private OpasKelder() {
    }

    /** The cellar under a Hoeve stamped at {@code hoek} with this (turned) footprint. */
    static void stempel(ServerLevel level, BlockPos hoek, int b, int d) {
        int x0 = 2, x1 = b - 3, z0 = 2, z1 = d - 3;   // the inside
        if (x1 - x0 < 3 || z1 - z0 < 3) {
            return;
        }
        // shell: walls, floor, ceiling; air inside
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean muur = x < x0 || x > x1 || z < z0 || z > z1;
                zet(level, hoek.offset(x, VLOER, z), (x + z) % 3 == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
                zet(level, hoek.offset(x, PLAFOND, z), Blocks.DARK_OAK_PLANKS.defaultBlockState());
                for (int y = VLOER + 1; y < PLAFOND; y++) {
                    BlockState st = !muur ? Blocks.AIR.defaultBlockState()
                            : (x < x0 || x > x1) && (z < z0 || z > z1) ? Blocks.SPRUCE_LOG.defaultBlockState()
                            : (x * 7 + z * 3 + y) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                    zet(level, hoek.offset(x, y, z), st);
                }
            }
        }
        // the way down: a trapdoor where the Hoeve's floor (or yard) is free, at the edge of the inside (a wall for the ladder)
        int[] luik = luik(level, hoek, x0, x1, z0, z1);
        Set<Long> vrij = new HashSet<>();
        if (luik != null) {
            int lx = luik[0], lz = luik[1];
            Direction muur = lx == x0 ? Direction.WEST : lx == x1 ? Direction.EAST : lz == z0 ? Direction.NORTH : Direction.SOUTH;
            zet(level, hoek.offset(lx, 0, lz), Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
                    .setValue(TrapDoorBlock.FACING, muur.getOpposite()));
            for (int y = PLAFOND; y > VLOER; y--) {
                zet(level, hoek.offset(lx, y, lz), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, muur.getOpposite()));
            }
            vrij.add(sleutel(lx, lz));
            vrij.add(sleutel(lx - muur.getStepX(), lz - muur.getStepZ()));   // the spot in front of the ladder
        }
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        int y = VLOER + 1;
        // the cheese cellar: barrels with candles, cheese, bookshelves, decorated pots for the finds, the diary
        BlockState kaas = blok("guhs:block_of_kaasknabbels", Blocks.HAY_BLOCK.defaultBlockState());
        BlockState gatenkaas = blok("guhs:gatenkaas", Blocks.HONEYCOMB_BLOCK.defaultBlockState());
        Object[][] meubels = {
                {x0, z0, Blocks.BARREL.defaultBlockState(), true}, {x0 + 1, z0, Blocks.BARREL.defaultBlockState(), false},
                {x0, z0 + 1, kaas, false}, {x0, z0 + 2, gatenkaas, false},
                {x1, z0, Blocks.BOOKSHELF.defaultBlockState(), false}, {x1, z0 + 1, Blocks.BOOKSHELF.defaultBlockState(), false},
                {x1, z0 + 2, Blocks.CHISELED_BOOKSHELF.defaultBlockState(), false},
                {x0, z1, Blocks.DECORATED_POT.defaultBlockState(), false}, {x0 + 1, z1, Blocks.DECORATED_POT.defaultBlockState(), false},
                {x1, z1, Blocks.BARREL.defaultBlockState(), true}, {x1 - 1, z1, kaas, false},
                {mx, z0, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH), false},
        };
        for (Object[] m : meubels) {
            int x = (Integer) m[0], z = (Integer) m[1];
            if (vrij.contains(sleutel(x, z))) {
                continue;
            }
            zet(level, hoek.offset(x, y, z), (BlockState) m[2]);
            if ((Boolean) m[3]) {
                zet(level, hoek.offset(x, y + 1, z), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            }
        }
        // a rug in the middle, a lantern above it
        for (int x = mx - 1; x <= mx + 1; x++) {
            for (int z = mz - 1; z <= mz + 1; z++) {
                if (!vrij.contains(sleutel(x, z))) {
                    zet(level, hoek.offset(x, y, z), (x + z) % 2 == 0 ? Blocks.PINK_CARPET.defaultBlockState() : Blocks.RED_CARPET.defaultBlockState());
                }
            }
        }
        zet(level, hoek.offset(mx, PLAFOND - 1, mz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Opa's diary on the lectern
        BlockPos lessenaar = hoek.offset(mx, y, z0);
        if (!vrij.contains(sleutel(mx, z0)) && level.getBlockEntity(lessenaar) instanceof LecternBlockEntity le && le.getBook().isEmpty()) {
            le.setBook(Afwerking.boek("dagboek", 3));
            level.setBlock(lessenaar, level.getBlockState(lessenaar).setValue(LecternBlock.HAS_BOOK, true), VLAG);
        }
        // the sign on the far wall
        int sx = mx + 1;
        if (!vrij.contains(sleutel(sx, z0))) {
            BlockPos bordPos = hoek.offset(sx, y + 1, z0);
            BlockState bord = Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH);
            zet(level, bordPos, bord);
            if (level.getBlockEntity(bordPos) instanceof SignBlockEntity be) {
                SignText t = new SignText().setMessage(0, Component.translatable("erfenis.afwerking.kelder.bord.0"))
                        .setMessage(1, Component.translatable("erfenis.afwerking.kelder.bord.1"))
                        .setMessage(2, Component.translatable("erfenis.afwerking.kelder.bord.2"))
                        .setMessage(3, Component.translatable("erfenis.afwerking.kelder.bord.3")).setColor(DyeColor.BROWN);
                be.setText(t, true);
                be.setWaxed(true);
                be.setChanged();
                level.sendBlockUpdated(bordPos, bord, bord, VLAG);
            }
        }
    }

    /**
     * Where the trapdoor goes: a cell at the edge of the inside whose grass/floor block is solid with two blocks of air above
     * (inside the Hoeve on its floor first, then the yard; the middle of a side wall first, out of the furniture's way).
     * Null: no room anywhere (then the cellar is there, just closed).
     */
    static int[] luik(ServerLevel level, BlockPos hoek, int x0, int x1, int z0, int z1) {
        int[] beste = null;
        int besteScore = Integer.MIN_VALUE;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x != x0 && x != x1 && z != z0 && z != z1) {
                    continue;
                }
                BlockPos g = hoek.offset(x, 0, z);
                BlockState grond = level.getBlockState(g);
                if (!grond.isCollisionShapeFullBlock(level, g) || !level.getBlockState(g.above()).isAir() || !level.getBlockState(g.above(2)).isAir()) {
                    continue;
                }
                boolean binnen = !grond.is(Blocks.GRASS_BLOCK);
                // the middle of a side wall (the back wall has the diary and the sign, the corners the barrels and the cheese)
                boolean zij = x == x0 || x == x1;
                int score = (binnen ? 1000 : 0) + (zij ? 20 : 0) - Math.abs(x - (x0 + x1) / 2) - Math.abs(z - (z0 + z1) / 2);
                if (score > besteScore) {
                    besteScore = score;
                    beste = new int[] {x, z};
                }
            }
        }
        return beste;
    }

    private static long sleutel(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    private static BlockState blok(String id, BlockState anders) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        return b == null || b == Blocks.AIR ? anders : b.defaultBlockState();
    }

    private static void zet(ServerLevel level, BlockPos p, BlockState st) {
        if (level.getBlockState(p) != st) {
            level.setBlock(p, st, VLAG);
        }
    }
}
