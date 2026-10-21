package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * The build animals' templates (design 1.3, "bouwdieren"): the woodpecker carves, the octopus furnishes a room, the gibbon
 * hangs garlands, the gecko livens up a facade, the glow-worms light a tunnel, the bowerbird decorates in a colour theme.
 */
final class SierWerk {
    static final BlockState LUCHT = Blocks.AIR.defaultBlockState();

    /** The plane of a wall: the face's blocks connected to the clicked one, each with air in front. */
    private static List<BlockPos> muur(Klus k, BlockPos p, Direction voor, int max, java.util.function.Predicate<BlockState> mag) {
        List<BlockPos> uit = new ArrayList<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        rij.add(p);
        gezien.add(p.asLong());
        while (!rij.isEmpty() && uit.size() < max) {
            BlockPos c = rij.poll();
            if (!mag.test(k.staat(c)) || !k.staat(c.relative(voor)).isAir() && !k.staat(c.relative(voor)).canBeReplaced()) {
                continue;
            }
            uit.add(c);
            for (Direction d : Direction.values()) {
                if (d.getAxis() == voor.getAxis()) {
                    continue;
                }
                BlockPos n = c.relative(d);
                if (Math.abs(n.getX() - p.getX()) + Math.abs(n.getY() - p.getY()) + Math.abs(n.getZ() - p.getZ()) <= 10 && gezien.add(n.asLong())) {
                    rij.add(n);
                }
            }
        }
        return uit;
    }

    /** Which face of the clicked block faces the open air (the side the player stands on). */
    private static Direction voorkant(Klus k, BlockPos p) {
        Direction beste = Direction.getApproximateNearest(k.bord().getX() - p.getX(), 0, k.bord().getZ() - p.getZ());
        if (k.staat(p.relative(beste)).isAir()) {
            return beste;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (k.staat(p.relative(d)).isAir()) {
                return d;
            }
        }
        throw Klus.nee("geen_muur");
    }

    private static boolean hout(BlockState s) {
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.LOGS);
    }

    /** Wood carving (variant: guh faces, hearts, diamonds) in the clicked wooden wall: darker and cherry wood make the picture. */
    static Map<BlockPos, BlockState> houtsnijwerk(Klus k, BlockPos p) {
        if (!hout(k.staat(p))) {
            throw Klus.nee("geen_hout");
        }
        Direction voor = voorkant(k, p);
        List<BlockPos> wand = muur(k, p, voor, 240, SierWerk::hout);
        if (wand.size() < 9) {
            throw Klus.nee("te_klein");
        }
        Direction rechts = voor.getCounterClockWise();
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState donker = Blocks.DARK_OAK_PLANKS.defaultBlockState(), roze = Blocks.CHERRY_PLANKS.defaultBlockState(),
                licht = Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState();
        int variant = Math.floorMod(k.variant(), 3);
        for (BlockPos c : wand) {
            int u = (c.getX() - p.getX()) * rechts.getStepX() + (c.getZ() - p.getZ()) * rechts.getStepZ();
            int v = c.getY() - p.getY();
            int tu = Math.floorMod(u, 6), tv = Math.floorMod(v, 5);
            BlockState zet = null;
            switch (variant) {
                case 0 -> {        // a guh face every 6 by 5: two eyes, pink cheeks, a little mouth
                    if (tv == 3 && (tu == 1 || tu == 4)) {
                        zet = donker;
                    } else if (tv == 2 && (tu == 0 || tu == 5)) {
                        zet = roze;
                    } else if (tv == 1 && (tu == 2 || tu == 3)) {
                        zet = donker;
                    }
                }
                case 1 -> {        // hearts
                    int[] rij = {0b0110110, 0b1111111, 0b0111110, 0b0011100, 0b0001000};
                    int hu = Math.floorMod(u, 8), hv = 4 - Math.floorMod(v, 6);
                    if (hv >= 0 && hv < 5 && hu < 7 && (rij[hv] >> (6 - hu) & 1) == 1) {
                        zet = roze;
                    }
                }
                default -> {       // diamonds
                    if (Math.floorMod(u + v, 4) == 0 || Math.floorMod(u - v, 4) == 0) {
                        zet = licht;
                    }
                }
            }
            if (zet != null && k.staat(c) != zet) {
                m.put(c, zet);
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("te_klein");
        }
        return m;
    }

    static BlockPos houtZoek(Klus k) {
        for (BlockPos c : k.rondBord(Math.min(24, k.zoekStraal()))) {
            if (!k.level().isLoaded(c)) {
                continue;
            }
            int g = k.grond(c.getX(), c.getZ());
            for (int y = g + 1; y <= g + 3; y++) {
                BlockPos q = c.atY(y);
                if (k.staat(q).is(BlockTags.PLANKS)) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (k.staat(q.relative(d)).isAir() && k.staat(q.above()).is(BlockTags.PLANKS)) {
                            return q;
                        }
                    }
                }
            }
        }
        throw Klus.nee("wijs_aan");
    }

    /** The floor of a room (enclosed by walls, a ceiling within 6) at the clicked floor block; empty: not a room. */
    private static List<BlockPos> kamer(Klus k, BlockPos vloer) {
        List<BlockPos> uit = new ArrayList<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        BlockPos start = vloer.above();
        rij.add(start);
        gezien.add(start.asLong());
        while (!rij.isEmpty()) {
            BlockPos c = rij.poll();
            if (!k.staat(c).isAir() || !k.staat(c.above()).isAir() || k.staat(c.below()).isAir()) {
                continue;
            }
            if (Math.abs(c.getX() - start.getX()) > 8 || Math.abs(c.getZ() - start.getZ()) > 8 || uit.size() > 200) {
                return List.of();              // it runs on: no room, outside
            }
            boolean dak = false;
            for (int y = 2; y <= 6 && !dak; y++) {
                dak = !k.staat(c.above(y)).isAir();
            }
            if (!dak) {
                return List.of();
            }
            uit.add(c);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(d);
                if (gezien.add(n.asLong())) {
                    rij.add(n);
                }
            }
        }
        return uit;
    }

    /** The octopus furnishes the room (variant: knus, safari, keuken, slaapkamer): a carpet, furniture along the walls, lights. */
    static Map<BlockPos, BlockState> inrichten(Klus k, BlockPos p) {
        List<BlockPos> vloer = kamer(k, p);
        if (vloer.size() < 6) {
            throw Klus.nee("geen_kamer");
        }
        int stijl = Math.floorMod(k.variant(), 4);
        DyeColor kleur = switch (stijl) {
            case 0 -> DyeColor.PINK;
            case 1 -> DyeColor.ORANGE;
            case 2 -> DyeColor.WHITE;
            default -> DyeColor.LIGHT_BLUE;
        };
        Set<Long> vrij = new HashSet<>();
        vloer.forEach(c -> vrij.add(c.asLong()));
        int sx = 0, sz = 0;
        for (BlockPos c : vloer) {
            sx += c.getX();
            sz += c.getZ();
        }
        BlockPos midden = new BlockPos(sx / vloer.size(), vloer.get(0).getY(), sz / vloer.size());
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        Block tapijt = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                net.minecraft.resources.Identifier.withDefaultNamespace(kleur.getName() + "_carpet"));
        List<BlockPos> randen = new ArrayList<>();
        for (BlockPos c : vloer) {
            boolean rand = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                rand |= !vrij.contains(c.relative(d).asLong());
            }
            if (rand) {
                randen.add(c);
            } else {
                m.put(c, tapijt.defaultBlockState());
            }
        }
        List<BlockState> meubels = switch (stijl) {
            case 0 -> List.of(Blok.zitzak(DyeColor.PINK), Blok.kussen(DyeColor.MAGENTA), Blok.van("guhs:guh_kast", Blocks.BARREL),
                    Blok.lampion(DyeColor.PINK), Blok.zitzak(DyeColor.LIGHT_BLUE), Blocks.POTTED_AZALEA.defaultBlockState());
            case 1 -> List.of(Blok.van("guhs:guh_bank", Blocks.OAK_STAIRS), Blocks.POTTED_BAMBOO.defaultBlockState(), Blok.lampion(DyeColor.YELLOW),
                    Blok.van("guhs:knuffelblok_zebra", Blocks.WHITE_WOOL), Blok.kussen(DyeColor.ORANGE), Blocks.POTTED_FERN.defaultBlockState());
            case 2 -> List.of(Blocks.SMOKER.defaultBlockState(), Blocks.CRAFTING_TABLE.defaultBlockState(), Blocks.BARREL.defaultBlockState(),
                    Blok.van("guhs:guh_stoel", Blocks.OAK_STAIRS), Blok.lampion(DyeColor.YELLOW), Blocks.FURNACE.defaultBlockState());
            default -> List.of(Blok.van("guhs:guh_kast", Blocks.BARREL), Blok.kussen(DyeColor.LIGHT_BLUE), Blok.lampion(DyeColor.PINK),
                    Blok.zitzak(DyeColor.WHITE), Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState());
        };
        int i = 0;
        boolean bed = stijl == 3;
        for (BlockPos c : randen) {
            Direction naarMidden = Direction.getApproximateNearest(midden.getX() - c.getX(), 0, midden.getZ() - c.getZ());
            if (bed) {
                // the bed: its foot here, its head against the wall
                BlockPos kop = c.relative(naarMidden.getOpposite());
                if (vrij.contains(kop.asLong()) && !m.containsKey(kop)) {
                    Direction f = naarMidden.getOpposite();
                    m.put(c, Blocks.PINK_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f).setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
                    m.put(kop, Blocks.PINK_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f).setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
                    bed = false;
                    continue;
                }
            }
            if (m.containsKey(c) || i++ % 2 != 0) {
                continue;
            }
            m.put(c, Blok.kant(meubels.get(k.r().nextInt(meubels.size())), naarMidden));
        }
        // a table in the middle (the kitchen and the cosy room), a light from the ceiling
        if (stijl == 0 || stijl == 2) {
            m.put(midden, Blok.van("guhs:guh_tafel", Blocks.OAK_PLANKS));
        }
        BlockPos plafond = midden;
        while (k.staat(plafond.above()).isAir() && plafond.getY() < midden.getY() + 6) {
            plafond = plafond.above();
        }
        if (plafond.getY() > midden.getY() + 1) {
            m.put(plafond, Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "true"));
        }
        return m;
    }

    /** The gibbon's garland between two points: flags (variant 0), or chains with pearly lights (variant 1). */
    static Map<BlockPos, BlockState> slingers(Klus k, BlockPos a, BlockPos b) {
        int dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
        int stappen = Math.max(Math.abs(dx), Math.abs(dz));
        if (stappen < 3 || stappen > 32) {
            throw Klus.nee(stappen < 3 ? "te_kort" : "te_ver");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        Direction.Axis as = Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        double zak = Math.max(1, stappen / 8.0);
        for (int s = 1; s < stappen; s++) {
            double t = s / (double) stappen;
            int x = a.getX() + (int) Math.round(dx * t), z = a.getZ() + (int) Math.round(dz * t);
            int y = (int) Math.round(a.getY() + (b.getY() - a.getY()) * t - zak * 4 * t * (1 - t));
            BlockPos q = new BlockPos(x, y, z);
            if (!k.staat(q).isAir() && !k.staat(q).is(BlockTags.LEAVES)) {
                continue;
            }
            BlockState zet = k.variant() == 1 ? (s % 3 == 0 ? (s % 6 == 0 ? Blocks.PEARLESCENT_FROGLIGHT : Blocks.VERDANT_FROGLIGHT).defaultBlockState()
                    : Blok.as(Blocks.IRON_CHAIN.defaultBlockState(), as)) : Blok.as(Blok.van("guhs:vlaggetjes", Blocks.WHITE_CARPET), as);
            m.put(q, zet);
        }
        return m;
    }

    /** Two tall points near the board for a garland: the tops of two posts or trunks 4..20 apart. */
    static List<BlockPos> tweeToppen(Klus k) {
        List<BlockPos> toppen = new ArrayList<>();
        for (BlockPos c : k.rondBord(Math.min(24, k.zoekStraal()))) {
            if (!k.level().isLoaded(c) || (c.getX() + c.getZ()) % 2 != 0) {
                continue;
            }
            int g = k.grond(c.getX(), c.getZ());
            BlockState s1 = k.staat(c.atY(g + 1)), s3 = k.staat(c.atY(g + 3));
            if ((s1.is(BlockTags.LOGS) || s1.is(BlockTags.FENCES) || s1.is(BlockTags.WALLS)) && (s3.is(BlockTags.LOGS) || s3.is(BlockTags.FENCES))) {
                toppen.add(c.atY(g + 3));
                for (BlockPos t : toppen) {
                    int d2 = (int) t.distSqr(c.atY(g + 3));
                    if (d2 >= 16 && d2 <= 400) {
                        return List.of(t, c.atY(g + 3));
                    }
                }
            }
        }
        throw Klus.nee("wijs_aan_twee");
    }

    /** The gecko on a facade: ivy here and there, a little planter with flowers under each window. */
    static Map<BlockPos, BlockState> gevel(Klus k, BlockPos p) {
        Direction voor = voorkant(k, p);
        List<BlockPos> wand = muur(k, p, voor, 220, s -> s.isSolid() || s.is(BlockTags.IMPERMEABLE) || s.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock);
        if (wand.size() < 6) {
            throw Klus.nee("geen_muur");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState[] bloemen = {Blocks.POTTED_POPPY.defaultBlockState(), Blocks.POTTED_DANDELION.defaultBlockState(),
                Blocks.POTTED_PINK_TULIP.defaultBlockState(), Blocks.POTTED_CORNFLOWER.defaultBlockState()};
        String zijde = voor.getOpposite().getSerializedName();
        for (BlockPos c : wand) {
            BlockPos ervoor = c.relative(voor);
            BlockState s = k.staat(c);
            if (s.is(BlockTags.IMPERMEABLE) && !s.isSolid() || s.is(BlockTags.IMPERMEABLE)) {
                // a window: a ledge with a flower pot in front of the block under it
                BlockPos onder = c.below().relative(voor);
                if (k.staat(onder).isAir() && !m.containsKey(onder) && !k.staat(c.below()).is(BlockTags.IMPERMEABLE)) {
                    m.put(onder, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP)
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, voor));
                }
                continue;
            }
            if (k.r().nextInt(100) < 30 && !m.containsKey(ervoor)) {
                m.put(ervoor, Blok.met(Blocks.VINE.defaultBlockState(), zijde, "true"));
            }
        }
        // the pots on the ledges
        List<BlockPos> richels = new ArrayList<>();
        m.forEach((q, s) -> {
            if (s.is(Blocks.SPRUCE_TRAPDOOR)) {
                richels.add(q);
            }
        });
        for (BlockPos r : richels) {
            m.put(r.above(), bloemen[k.r().nextInt(bloemen.length)]);
        }
        return m;
    }

    /** The glow-worms light a tunnel, cave or cellar: soft glowing lichen on the ceiling, glow mushrooms on the floor. */
    static Map<BlockPos, BlockState> licht(Klus k, BlockPos p) {
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        BlockPos start = k.staat(p).isAir() ? p : p.above();
        rij.add(start);
        gezien.add(start.asLong());
        int r = 10 + 4 * k.grootte();
        List<BlockPos> lucht = new ArrayList<>();
        int buiten = 0;
        while (!rij.isEmpty() && lucht.size() < 2500) {
            BlockPos c = rij.poll();
            if (!k.staat(c).isAir() || c.distSqr(start) > r * r) {
                continue;
            }
            if (k.level().canSeeSky(c)) {
                buiten++;
                continue;
            }
            lucht.add(c);
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (gezien.add(n.asLong())) {
                    rij.add(n);
                }
            }
        }
        if (lucht.size() < 8 || buiten > lucht.size()) {
            throw Klus.nee("geen_tunnel");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState paddenstoel = Blok.van("guhs:glimpaddenstoel", Blocks.GLOW_LICHEN);
        List<BlockPos> gezet = new ArrayList<>();
        for (BlockPos c : lucht) {
            boolean ver = true;
            for (BlockPos g : gezet) {
                if (g.distSqr(c) < 9) {
                    ver = false;
                    break;
                }
            }
            if (!ver) {
                continue;
            }
            if (k.staat(c.above()).isFaceSturdy(k.level(), c.above(), Direction.DOWN)) {
                m.put(c, Blok.met(Blocks.GLOW_LICHEN.defaultBlockState(), "up", "true"));
                gezet.add(c);
            } else if (k.staat(c.below()).isFaceSturdy(k.level(), c.below(), Direction.UP) && k.r().nextInt(3) == 0
                    && paddenstoel.canSurvive(k.level(), c)) {
                m.put(c, paddenstoel);
                gezet.add(c);
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("geen_tunnel");
        }
        return m;
    }

    static BlockPos lichtZoek(Klus k) {
        for (BlockPos c : k.rondBord(Math.min(32, k.zoekStraal()))) {
            if ((c.getX() + c.getZ()) % 3 != 0 || !k.level().isLoaded(c)) {
                continue;
            }
            int g = k.grond(c.getX(), c.getZ());
            for (int y = g - 2; y >= g - 12; y--) {
                BlockPos q = c.atY(y);
                if (k.staat(q).isAir() && k.staat(q.above()).isAir() && !k.level().canSeeSky(q) && !k.staat(q.below()).isAir()) {
                    return q.below();
                }
            }
        }
        throw Klus.nee("geen_tunnel");
    }

    /** The bowerbird decorates around the spot in a colour theme: cushions, beanbags, lanterns and flower pots. */
    static Map<BlockPos, BlockState> decoreren(Klus k, BlockPos p) {
        DyeColor[][] themas = {{DyeColor.PINK, DyeColor.MAGENTA}, {DyeColor.LIGHT_BLUE, DyeColor.BLUE}, {DyeColor.YELLOW, DyeColor.ORANGE},
                {DyeColor.PURPLE, DyeColor.MAGENTA}, DyeColor.values()};
        DyeColor[] thema = themas[Math.floorMod(k.variant(), themas.length)];
        BlockState[] potten = {Blocks.POTTED_ALLIUM.defaultBlockState(), Blocks.POTTED_PINK_TULIP.defaultBlockState(),
                Blocks.POTTED_BLUE_ORCHID.defaultBlockState(), Blocks.POTTED_DANDELION.defaultBlockState(), Blocks.POTTED_CORNFLOWER.defaultBlockState()};
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 4 + k.grootte();
        List<BlockPos> gezet = new ArrayList<>();
        BlockPos basis = k.staat(p).isAir() ? p.below() : p;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos q = basis.offset(dx, dy + 1, dz);
                    if (!k.staat(q).isAir() || !k.staat(q.above()).isAir() || !k.staat(q.below()).isFaceSturdy(k.level(), q.below(), Direction.UP)) {
                        continue;
                    }
                    boolean ver = true;
                    for (BlockPos g : gezet) {
                        ver &= g.distSqr(q) >= 5;
                    }
                    if (!ver || k.r().nextInt(4) != 0) {
                        continue;
                    }
                    DyeColor c = thema[k.r().nextInt(thema.length)];
                    BlockState zet = switch (k.r().nextInt(4)) {
                        case 0 -> Blok.kussen(c);
                        case 1 -> Blok.zitzak(c);
                        case 2 -> Blok.lampion(c);
                        default -> potten[Math.floorMod(c.ordinal(), potten.length)];
                    };
                    m.put(q, Blok.kant(zet, Direction.Plane.HORIZONTAL.getRandomDirection(k.r())));
                    gezet.add(q);
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("geen_plek");
        }
        return m;
    }

    private SierWerk() {
    }
}
