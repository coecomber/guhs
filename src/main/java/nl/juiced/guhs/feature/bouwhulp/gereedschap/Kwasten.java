package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;
import nl.juiced.guhs.feature.bouwhulp.vormen.Familie;
import nl.juiced.guhs.feature.bouwhulp.vormen.Mix;

/**
 * What the kwasten and the verfmachine do (design 2.3/2.4), worked out as "these spots become these blocks" (the caller
 * turns it into a ghost): vervangen (keeping the shape: a stair stays a stair), mengen (70/20/10), patronen (gradient,
 * stripes, checkerboard, guh faces, the animal patterns) and verweren (moss, cracks, ivy), and the verfmachine's recolouring
 * of wool, concrete and terracotta. A stroke paints a ball around the clicked block.
 */
public final class Kwasten {
    /** At most this many blocks in one stroke. */
    public static final int MAX = 8192;

    /** The blocks of a stroke: solid, breakable, no block entities; optionally only the clicked kind / only the outside. */
    public static List<BlockPos> gebied(ServerLevel level, BlockPos midden, int straal, boolean zelfde, boolean buitenkant) {
        List<BlockPos> uit = new ArrayList<>();
        Block soort = level.getBlockState(midden).getBlock();
        double r = straal + 0.5;
        for (BlockPos p : BlockPos.betweenClosed(midden.offset(-straal, -straal, -straal), midden.offset(straal, straal, straal))) {
            if (p.distSqr(midden) > r * r || uit.size() >= MAX) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                continue;
            }
            if (!Spoken.magBouwen(level, p, s) || zelfde && s.getBlock() != soort) {
                continue;
            }
            if (buitenkant && !buiten(level, p)) {
                continue;
            }
            uit.add(p.immutable());
        }
        return uit;
    }

    private static boolean buiten(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(p.relative(d));
            if (n.isAir() || !n.isSolidRender()) {
                return true;
            }
        }
        return false;
    }

    /** Vervangkwast: every block in its own shape of the new material. */
    public static Map<BlockPos, BlockState> vervang(ServerLevel level, List<BlockPos> gebied, Block nieuw) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        for (BlockPos p : gebied) {
            BlockState n = Familie.inVorm(level.getBlockState(p), nieuw);
            if (n != null) {
                uit.put(p, n);
            }
        }
        return uit;
    }

    /** Mengkwast: like vervangen, each spot its block from the mix. */
    public static Map<BlockPos, BlockState> meng(ServerLevel level, List<BlockPos> gebied, Mix mix) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        for (BlockPos p : gebied) {
            BlockState n = Familie.inVorm(level.getBlockState(p), mix.kies(p, Blocks.STONE));
            if (n != null) {
                uit.put(p, n);
            }
        }
        return uit;
    }

    /** Patroonkwast: the pattern in the mix's blocks, or (animal patterns) in the colour of the painted wool/concrete/terracotta. */
    public static Map<BlockPos, BlockState> patroon(ServerLevel level, List<BlockPos> gebied, Patronen.Patroon pat, Mix mix) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        if (gebied.isEmpty()) {
            return uit;
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (BlockPos p : gebied) {
            x0 = Math.min(x0, p.getX());
            y0 = Math.min(y0, p.getY());
            z0 = Math.min(z0, p.getZ());
            x1 = Math.max(x1, p.getX());
            y1 = Math.max(y1, p.getY());
            z1 = Math.max(z1, p.getZ());
        }
        Patronen.Doos doos = new Patronen.Doos(x0, y0, z0, x1, y1, z1);
        boolean plat = y0 == y1;
        String kleurSoortMix = mix.isLeeg() ? null : Familie.kleurSoort(mix.eerste(Blocks.WHITE_WOOL));
        for (BlockPos p : gebied) {
            BlockState oud = level.getBlockState(p);
            BlockState n;
            if (pat.dier()) {
                DyeColor c = Patronen.kleur(pat, p, doos);
                String soort = Familie.kleurSoort(oud.getBlock());
                if (soort == null) {
                    soort = kleurSoortMix != null ? kleurSoortMix : "wool";
                }
                Block doel = Familie.gekleurd(soort, c);
                n = doel == oud.getBlock() ? null : Familie.rol(oud.getBlock()) == Familie.Rol.ANDERS && Familie.kleurSoort(oud.getBlock()) != null
                        ? doel.withPropertiesOf(oud) : doel.defaultBlockState();
            } else {
                if (mix.isLeeg()) {
                    continue;
                }
                Block b = pat == Patronen.Patroon.VERLOOP ? mix.verloop(doos.t(p.getY()), p, Blocks.STONE)
                        : mix.blokken().get(Math.min(mix.aantal() - 1, Patronen.index(pat, p, doos, mix.aantal(), plat)));
                n = Familie.inVorm(oud, b);
            }
            if (n != null && n != oud) {
                uit.put(p, n);
            }
        }
        return uit;
    }

    /** Verweerkwast: moss (mossy kin, moss carpets on top), cracks (cracked kin), ivy (vines hanging off the sides). */
    public static Map<BlockPos, BlockState> verweer(ServerLevel level, List<BlockPos> gebied, boolean mos, boolean barsten, boolean klimop, int sterkte) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        int kans = Math.max(1, Math.min(10, sterkte)) * 10;
        for (BlockPos p : gebied) {
            BlockState oud = level.getBlockState(p);
            int h = Math.floorMod(Mix.hash(p.getX(), p.getY(), p.getZ()), 100);
            if (h < kans) {
                Block doel = null;
                boolean kiesMos = mos && (!barsten || h % 2 == 0);
                if (kiesMos) {
                    doel = kin(oud.getBlock(), "mossy_");
                }
                if (doel == null && barsten) {
                    doel = kin(oud.getBlock(), "cracked_");
                    if (doel == null && oud.getBlock() == Blocks.STONE) {
                        doel = Blocks.COBBLESTONE;
                    }
                }
                if (doel == null && mos && !kiesMos) {
                    doel = kin(oud.getBlock(), "mossy_");
                }
                if (doel != null) {
                    uit.put(p, doel.withPropertiesOf(oud));
                }
            }
            if (mos && Math.floorMod(Mix.hash(p.getX() + 7, p.getY(), p.getZ()), 100) < kans / 3 && oud.isSolidRender()
                    && level.getBlockState(p.above()).isAir() && !uit.containsKey(p.above())) {
                uit.put(p.above(), Blocks.MOSS_CARPET.defaultBlockState());
            }
            if (klimop && oud.isSolidRender()) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (Math.floorMod(Mix.hash(p.getX(), p.getY() * 31 + d.ordinal(), p.getZ()), 100) >= kans / 2) {
                        continue;
                    }
                    BlockPos v = p.relative(d);
                    for (int i = 0; i < 1 + Math.floorMod(Mix.hash(v.getX(), v.getY(), v.getZ()), 3); i++) {
                        BlockPos w = v.below(i);
                        BlockState nu = uit.containsKey(w) ? uit.get(w) : level.getBlockState(w);
                        if (!nu.isAir() && !nu.is(Blocks.VINE)) {
                            break;
                        }
                        if (i > 0 && !level.getBlockState(w.relative(d.getOpposite())).isSolidRender()) {
                            break;
                        }
                        BlockState rank = nu.is(Blocks.VINE) ? nu : Blocks.VINE.defaultBlockState();
                        uit.put(w.immutable(), rank.setValue(VineBlock.getPropertyForFace(d.getOpposite()), true));
                    }
                }
            }
        }
        return uit;
    }

    /** The mossy_ / cracked_ kin of a block (mossy_cobblestone_stairs, cracked_stone_bricks...), or null. */
    @Nullable
    static Block kin(Block b, String voor) {
        var id = BuiltInRegistries.BLOCK.getKey(b);
        if (!id.getNamespace().equals("minecraft") || id.getPath().startsWith("mossy_") || id.getPath().startsWith("cracked_")) {
            return null;
        }
        return Familie.blok("minecraft:" + voor + id.getPath());
    }

    // =====================================================================================================================
    // the verfmachine (Sauskeet)
    // =====================================================================================================================

    /** The colours in rainbow order: a gradient walks along this list. */
    public static final DyeColor[] REGENBOOG = {DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.BROWN, DyeColor.RED,
            DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE,
            DyeColor.MAGENTA, DyeColor.PINK};

    /**
     * Recolours wool, concrete, terracotta (and their glass, carpets, powder) in a ball: one colour, or a gradient from
     * {@code a} to {@code b} along the height or along {@code richting}.
     */
    public static Map<BlockPos, BlockState> verf(ServerLevel level, BlockPos midden, int straal, DyeColor a, @Nullable DyeColor b, boolean langsHoogte,
                                                 Direction richting) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        double r = straal + 0.5;
        int ia = index(a), ib = b == null ? ia : index(b);
        for (BlockPos p : BlockPos.betweenClosed(midden.offset(-straal, -straal, -straal), midden.offset(straal, straal, straal))) {
            if (p.distSqr(midden) > r * r || uit.size() >= MAX) {
                continue;
            }
            BlockState oud = level.getBlockState(p);
            String soort = Familie.kleurSoort(oud.getBlock());
            if (soort == null || !Spoken.magBouwen(level, p, oud)) {
                continue;
            }
            DyeColor c = a;
            if (b != null && ia != ib) {
                double t = langsHoogte ? (p.getY() - midden.getY() + straal) / (2.0 * straal)
                        : ((p.getX() - midden.getX()) * richting.getStepX() + (p.getZ() - midden.getZ()) * richting.getStepZ() + straal) / (2.0 * straal);
                t = Math.max(0, Math.min(1, t));
                c = REGENBOOG[(int) Math.round(ia + (ib - ia) * t)];
            }
            Block doel = Familie.gekleurd(soort, c);
            if (doel != oud.getBlock()) {
                uit.put(p.immutable(), doel.withPropertiesOf(oud));
            }
        }
        return uit;
    }

    private static int index(DyeColor c) {
        for (int i = 0; i < REGENBOOG.length; i++) {
            if (REGENBOOG[i] == c) {
                return i;
            }
        }
        return 0;
    }

    private Kwasten() {
    }
}
