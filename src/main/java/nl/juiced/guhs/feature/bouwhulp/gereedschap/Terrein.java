package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;
import nl.juiced.guhs.feature.bouwhulp.vormen.Vormen;

/**
 * The Zoutkeet's egaliseerder and the Sauskeet's tunnelboor (design 2.3), worked out as world spots for a ghost.
 * <ul>
 *   <li>egaliseerder: vlak (flat at the clicked height, the top layer stays), helling (a slope along where you look), kuil
 *       (a bowl, with water if wanted), heuvel (a mound, with a roughness slider);</li>
 *   <li>tunnelboor: a round, square or vaulted tunnel along where you look, lined with a block, lamps every 6 blocks.
 *       What it digs out goes back through the undo list (to you or the Bank Guh), there is no mining mode.</li>
 * </ul>
 */
public final class Terrein {
    public enum Stand { VLAK, HELLING, KUIL, HEUVEL }

    /** How far above the ground the egaliseerder clears (trees and all). */
    private static final int BOVEN = 24, ONDER = 12;

    public static Map<BlockPos, BlockState> egaliseer(ServerLevel level, BlockPos geklikt, Stand stand, int straal, int hoogte, boolean omlaag,
                                                      boolean water, int ruwheid, Direction kijk) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        BlockState bovenlaag = bovenlaag(level.getBlockState(geklikt));
        BlockState grond = grond(bovenlaag);
        int h0 = geklikt.getY();
        double r = straal + 0.5;
        for (int dx = -straal; dx <= straal; dx++) {
            for (int dz = -straal; dz <= straal; dz++) {
                double a = Math.sqrt(dx * dx + dz * dz);
                if (a > r) {
                    continue;
                }
                double t = a / r;
                int x = geklikt.getX() + dx, z = geklikt.getZ() + dz;
                double doel = switch (stand) {
                    case VLAK -> h0;
                    case HELLING -> {
                        double langs = (dx * kijk.getStepX() + dz * kijk.getStepZ()) / (double) straal;
                        yield h0 + langs * hoogte * (omlaag ? -1 : 1);
                    }
                    case KUIL -> h0 - hoogte * (1 - t * t);
                    case HEUVEL -> h0 + hoogte * (1 - t * t) + (Vormen.ruis(x, 0, z, 3) - 0.5) * ruwheid * 0.6 * (1 - t);
                };
                int top = (int) Math.round(doel);
                for (int y = h0 - ONDER; y <= h0 + BOVEN; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState nu = level.getBlockState(p);
                    if (!Spoken.magBouwen(level, p, nu)) {
                        continue;
                    }
                    if (y <= top) {
                        if (nu.isAir() || nu.canBeReplaced() || !nu.getFluidState().isEmpty()) {
                            uit.put(p, y == top ? bovenlaag : grond);
                        } else if (y == top && stand != Stand.VLAK && grondBlok(nu) && nu != bovenlaag) {
                            uit.put(p, bovenlaag);
                        }
                    } else if (stand == Stand.KUIL && water && y <= h0 - 1) {
                        uit.put(p, Blocks.WATER.defaultBlockState());
                    } else if (!nu.isAir()) {
                        uit.put(p, Blocks.AIR.defaultBlockState());
                    }
                }
                // the ground under a lowered spot gets its top layer back
                BlockPos onder = new BlockPos(x, top, z);
                BlockState o = level.getBlockState(onder);
                if (!uit.containsKey(onder) && grondBlok(o) && o != bovenlaag && !(stand == Stand.KUIL && water && top < h0)) {
                    uit.put(onder, bovenlaag);
                }
            }
        }
        return uit;
    }

    /** The top block of the ground (grass on dirt, sand on sand...). */
    private static BlockState bovenlaag(BlockState geklikt) {
        if (geklikt.isAir() || geklikt.canBeReplaced() || !geklikt.getFluidState().isEmpty()) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return geklikt.getBlock().defaultBlockState();
    }

    private static BlockState grond(BlockState boven) {
        Block b = boven.getBlock();
        if (b == Blocks.GRASS_BLOCK || b == Blocks.PODZOL || b == Blocks.MYCELIUM || b == Blocks.SNOW_BLOCK || b == Blocks.MOSS_BLOCK) {
            return Blocks.DIRT.defaultBlockState();
        }
        return boven;
    }

    private static boolean grondBlok(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    // =====================================================================================================================

    public enum Doorsnede { ROND, VIERKANT, GEWELFD }

    /**
     * A tunnel from the clicked block on, {@code lengte} blocks along {@code richting} (up or down for a shaft): the inside
     * dug out, the ring around it of {@code bekleding} (only where there is something to line: not in open air), lamps
     * hanging every 6 blocks.
     */
    public static Map<BlockPos, BlockState> tunnel(ServerLevel level, BlockPos start, Direction richting, Doorsnede vorm, int breedte, int hoogte,
                                                   int lengte, Block bekleding, boolean lampjes) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        // the cross section: u sideways, v up (for a shaft: u along x, v along z)
        Direction u = richting.getAxis().isVertical() ? Direction.EAST : richting.getClockWise();
        Direction v = richting.getAxis().isVertical() ? Direction.SOUTH : Direction.UP;
        int hb = vorm == Doorsnede.ROND ? breedte : hoogte;
        double cu = (breedte - 1) / 2.0;
        for (int l = 0; l < lengte; l++) {
            BlockPos rij = start.relative(richting, l);
            for (int a = -1; a <= breedte; a++) {
                for (int b = -1; b <= hb; b++) {
                    boolean binnen = binnen(vorm, a, b, breedte, hb, cu);
                    boolean rand = !binnen && (binnen(vorm, a + 1, b, breedte, hb, cu) || binnen(vorm, a - 1, b, breedte, hb, cu)
                            || binnen(vorm, a, b + 1, breedte, hb, cu) || binnen(vorm, a, b - 1, breedte, hb, cu));
                    if (!binnen && !rand) {
                        continue;
                    }
                    int du = (int) Math.round(a - cu);
                    BlockPos p = rij.relative(u, du).relative(v, b);
                    BlockState nu = level.getBlockState(p);
                    if (!Spoken.magBouwen(level, p, nu)) {
                        continue;
                    }
                    if (binnen) {
                        uit.put(p, Blocks.AIR.defaultBlockState());
                    } else if (!nu.isAir()) {
                        uit.put(p, Vormen.staat(bekleding));
                    }
                }
            }
            if (lampjes && l % 6 == 3 && !richting.getAxis().isVertical()) {
                int top = -1;
                for (int b = 0; b < hb; b++) {
                    if (binnen(vorm, (int) Math.round(cu), b, breedte, hb, cu)) {
                        top = b;
                    }
                }
                if (top > 0) {
                    BlockPos lamp = rij.relative(v, top);
                    uit.put(lamp, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                    uit.putIfAbsent(lamp.relative(v), Vormen.staat(bekleding));
                }
            }
        }
        return uit;
    }

    /** Is (a, b) inside the cross section (a 0..breedte-1 across, b 0..hoogte-1 up)? */
    private static boolean binnen(Doorsnede vorm, int a, int b, int breedte, int hoogte, double cu) {
        if (a < 0 || a >= breedte || b < 0 || b >= hoogte) {
            return false;
        }
        double du = a - cu;
        return switch (vorm) {
            case VIERKANT -> true;
            case ROND -> {
                double r = breedte / 2.0, dv = b - (hoogte - 1) / 2.0;
                yield du * du + dv * dv <= r * r - 0.3;
            }
            case GEWELFD -> {
                double r = breedte / 2.0, boog = hoogte - r;
                yield b < boog || du * du + (b - boog) * (b - boog) <= r * r - 0.3;
            }
        };
    }

    private Terrein() {
    }
}
