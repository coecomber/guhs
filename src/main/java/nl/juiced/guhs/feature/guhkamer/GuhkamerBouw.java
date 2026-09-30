package nl.juiced.guhs.feature.guhkamer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Builds the Guhkamer: a cosy guest room inside your maag (cherry wood floor, pink walls with a wooden plint, the soft
 * maag ceiling with guh crystal lamps, a pink rug), with the door back to the maag in its south wall and a sign above it.
 * Like the maag itself it sits in a shell of barriers, and when it grows only the walls, ceiling and door move out:
 * everything you put inside stays.
 */
public final class GuhkamerBouw {
    private GuhkamerBouw() {
    }

    private static void zet(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2 | 16);
    }

    /** The door's lower half in the room's south wall. */
    public static BlockPos kamerDeur(BlockPos m, int breedte) {
        return new BlockPos(m.getX(), m.getY(), m.getZ() + breedte / 2);
    }

    /**
     * Builds the room of this size around m (m = the middle of the floor you stand on), taking down the old, smaller
     * shell first (oudBreedte 0 = nothing there yet).
     */
    public static void bouw(ServerLevel level, BlockPos m, int breedte, int hoogte, int oudBreedte, int oudHoogte) {
        int y0 = m.getY();
        BlockState lucht = Blocks.AIR.defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        if (oudBreedte > 0) {
            level.setBlock(kamerDeur(m, oudBreedte).above(2).north(), lucht, 2 | 16);   // (the old sign above the door)
            int h0 = oudBreedte / 2;
            for (int x = -h0 - 2; x <= h0 + 1; x++) {
                for (int z = -h0 - 2; z <= h0 + 1; z++) {
                    boolean buiten = x == -h0 - 2 || x == h0 + 1 || z == -h0 - 2 || z == h0 + 1;
                    boolean wand = !buiten && (x == -h0 - 1 || x == h0 || z == -h0 - 1 || z == h0);
                    int wx = m.getX() + x, wz = m.getZ() + z;
                    if (buiten) {
                        for (int y = y0 - 3; y <= y0 + oudHoogte + 1; y++) {
                            zet(level, wx, y, wz, lucht);
                        }
                    } else if (wand) {
                        for (int y = y0 - 2; y <= y0 + oudHoogte + 1; y++) {
                            zet(level, wx, y, wz, lucht);
                        }
                    } else {
                        zet(level, wx, y0 + oudHoogte, wz, lucht);
                        zet(level, wx, y0 + oudHoogte + 1, wz, lucht);
                    }
                }
            }
        }
        BlockState plank = Blocks.CHERRY_PLANKS.defaultBlockState();
        BlockState muur = Blocks.PINK_TERRACOTTA.defaultBlockState();
        BlockState rand = Blocks.PINK_WOOL.defaultBlockState();
        BlockState plafond = ModBlocks.MAAGWAND.get().defaultBlockState();
        BlockState lamp = ModBlocks.GUH_KRISTAL_LAMP.get().defaultBlockState();
        int h = breedte / 2;
        for (int x = -h - 2; x <= h + 1; x++) {
            for (int z = -h - 2; z <= h + 1; z++) {
                boolean buiten = x == -h - 2 || x == h + 1 || z == -h - 2 || z == h + 1;
                boolean wand = !buiten && (x == -h - 1 || x == h || z == -h - 1 || z == h);
                int wx = m.getX() + x, wz = m.getZ() + z;
                zet(level, wx, y0 - 3, wz, barrier);
                if (buiten) {
                    for (int y = y0 - 2; y <= y0 + hoogte + 1; y++) {
                        zet(level, wx, y, wz, barrier);
                    }
                    continue;
                }
                zet(level, wx, y0 + hoogte + 1, wz, barrier);
                if (wand) {
                    for (int y = y0 - 2; y <= y0 + hoogte; y++) {
                        zet(level, wx, y, wz, y <= y0 ? plank : y == y0 + hoogte - 1 ? rand : y == y0 + hoogte ? plafond : muur);
                    }
                } else {
                    boolean isLamp = Math.floorMod(x + h, 5) == 2 && Math.floorMod(z + h, 5) == 2;
                    zet(level, wx, y0 + hoogte, wz, isLamp ? lamp : plafond);
                    if (level.getBlockState(new BlockPos(wx, y0 - 1, wz)).isAir()) {
                        zet(level, wx, y0 - 2, wz, plank);
                        zet(level, wx, y0 - 1, wz, plank);
                    }
                }
            }
        }
        // the door back to the maag, a sign above it
        BlockPos deur = kamerDeur(m, breedte);
        zetDeur(level, deur, Direction.NORTH, GuhkamerDeurBlock.Kant.KAMER);
        BlockPos bord = deur.above(2).north();
        level.setBlock(bord, Blocks.CHERRY_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH), 2);
        tekst(level, bord, Component.translatable("sign.guhs.guhkamer.kamer1"), Component.translatable("sign.guhs.guhkamer.kamer2"),
                Component.translatable("sign.guhs.guhkamer.kamer3"));
        if (oudBreedte == 0) {    // a pink rug in the middle, the first time
            for (int x = -2; x <= 1; x++) {
                for (int z = -2; z <= 1; z++) {
                    BlockPos p = m.offset(x, 0, z);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlock(p, (x == -2 || x == 1 || z == -2 || z == 1 ? Blocks.MAGENTA_CARPET : Blocks.PINK_CARPET).defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** A Guhkamer door (both halves). */
    public static void zetDeur(ServerLevel level, BlockPos onder, Direction facing, GuhkamerDeurBlock.Kant kant) {
        BlockState s = GuhkamerFeature.DEUR.get().defaultBlockState().setValue(GuhkamerDeurBlock.FACING, facing).setValue(GuhkamerDeurBlock.KANT, kant);
        level.setBlock(onder, s.setValue(GuhkamerDeurBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
        level.setBlock(onder.above(), s.setValue(GuhkamerDeurBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
    }

    /**
     * Finds a free spot for the door in a maag (near the middle, away from the portals: two air blocks on a solid
     * floor, room for the sign) and puts the door there. Returns its lower half, or null when there is no room.
     */
    @Nullable
    public static BlockPos zetMaagDeur(ServerLevel level, BlockPos c) {
        BlockPos basis = c.offset(-7, 0, 7);
        for (int r = 0; r <= 12; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos p = basis.offset(dx, 0, dz);
                    int rx = p.getX() - c.getX(), rz = p.getZ() - c.getZ();
                    if (Math.abs(rx) <= 6 && rz >= -2 && rz <= 6) {
                        continue;   // (the portals, their signs and where you arrive)
                    }
                    if (vrij(level, p) && vrij(level, p.east(2)) && !level.getBlockState(p.below()).isAir()
                            && !level.getBlockState(p.east(2).below()).isAir()) {
                        zetDeur(level, p, Direction.NORTH, GuhkamerDeurBlock.Kant.MAAG);
                        BlockPos bord = p.east(2);
                        level.setBlock(bord, Blocks.CHERRY_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), 2);
                        tekst(level, bord, Component.translatable("sign.guhs.guhkamer.maag1"), Component.translatable("sign.guhs.guhkamer.maag2"),
                                Component.translatable("sign.guhs.guhkamer.maag3"));
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private static boolean vrij(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir();
    }

    private static void tekst(ServerLevel level, BlockPos pos, Component... regels) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < regels.length && i < 3; i++) {
                text = text.setMessage(i + (regels.length < 3 ? 1 : 0), regels[i]);
            }
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setWaxed(true);
            sign.setChanged();
            level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
        }
    }
}
