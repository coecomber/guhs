package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfData;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;

/**
 * "Bouw passend verblijf" (design 2.9, the verblijf-assistent): a ghost of a home that fits the species, worked out from what
 * makes it happy (verblijf.json's requirements: sand, rocks, trees, water, ice...) plus its toy, inside a fence on the edge
 * of the board's home with a gate toward the board's front. The fence keeps to the escape-proof rule (design ronde 36): no
 * raise within 2 blocks of the fence, or the fence there is at least 2 higher than that raise.
 */
public final class PassendVerblijf {
    /** The fence never stands closer than this to a feature inside (so the rule mostly holds by itself). */
    static final int BUFFER = 3;

    /** The ghost blocks of a fitting home for this board (world positions). */
    public static Map<BlockPos, BlockState> maak(Klus k, VerblijfsbordBlockEntity be) {
        VerblijfData.Dier d = VerblijfData.dier(be.soort());
        if (d == null) {
            throw Klus.nee("geen_soort");
        }
        BlockPos m0 = be.middelpunt();
        boolean recht = be.rechthoek();
        int hx = recht ? be.breedte() : be.straal(), hz = recht ? be.diepte() : be.straal();
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        // the home's cells
        Set<Long> binnen = new HashSet<>();
        for (int dx = -hx; dx <= hx; dx++) {
            for (int dz = -hz; dz <= hz; dz++) {
                if (recht || dx * dx + dz * dz <= hx * hx) {
                    binnen.add(BlockPos.asLong(m0.getX() + dx, 0, m0.getZ() + dz));
                }
            }
        }
        int vrijeStraal = Math.max(2, Math.min(hx, hz) - BUFFER);
        // what the species wants
        List<BlockPos> plekken = plekken(k, m0, vrijeStraal);
        int i = 0;
        for (VerblijfData.Eis e : d.eisen()) {
            BlockPos plek = plekken.get(i++ % plekken.size());
            switch (e.soort()) {
                case "water" -> vijver(k, m, plek, e.min(), vrijeStraal, m0);
                case "tag" -> tagEis(k, m, plek, e);
                default -> blokEis(k, m, plek, e);
            }
        }
        // the toy near the middle
        Block speeltje = VerblijfFeature.speeltje(d.speeltje());
        if (speeltje != null) {
            BlockPos s = m0.offset(2, 0, 1);
            int g = k.grond(s.getX(), s.getZ());
            BlockPos op = s.atY(g + 1);
            if (!m.containsKey(op) && !m.containsKey(op.below())) {
                m.put(op, speeltje.defaultBlockState());
            }
        }
        m.remove(m0);                                   // (never over the board itself)
        hek(k, m, m0, binnen, be);
        Blok.verbind(m);
        return m;
    }

    /** Spots for the features, spread round the middle at half the free radius (the board's own column left free). */
    private static List<BlockPos> plekken(Klus k, BlockPos m0, int r) {
        List<BlockPos> uit = new ArrayList<>();
        int afstand = Math.max(2, r / 2 + 1);
        for (int i = 0; i < 6; i++) {
            double hoek = i * Math.PI / 3 + 0.4;
            int x = m0.getX() + (int) Math.round(Math.cos(hoek) * afstand), z = m0.getZ() + (int) Math.round(Math.sin(hoek) * afstand);
            uit.add(k.grondPos(x, z));
        }
        return uit;
    }

    private static void vijver(Klus k, Map<BlockPos, BlockState> m, BlockPos plek, int min, int r, BlockPos m0) {
        int nodig = (int) Math.ceil(min * 1.25) + 2;
        int diep = nodig >= 30 ? 2 : 1;
        int cellen = (int) Math.ceil(nodig / (double) diep);
        int g = plek.getY();
        BlockPos midden = nodig >= 40 ? k.grondPos(m0.getX() + 1, m0.getZ() + 1) : plek;
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        List<BlockPos> vijver = new ArrayList<>();
        rij.add(midden.atY(0));
        gezien.add(BlockPos.asLong(midden.getX(), 0, midden.getZ()));
        while (!rij.isEmpty() && vijver.size() < cellen) {
            BlockPos c = rij.poll();
            int dx = c.getX() - m0.getX(), dz = c.getZ() - m0.getZ();
            if (dx * dx + dz * dz > r * r || dx == 0 && dz == 0) {
                continue;
            }
            vijver.add(c);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(d);
                if (gezien.add(BlockPos.asLong(n.getX(), 0, n.getZ()))) {
                    rij.add(n);
                }
            }
        }
        Set<Long> in = new HashSet<>();
        vijver.forEach(c -> in.add(BlockPos.asLong(c.getX(), 0, c.getZ())));
        for (BlockPos c : vijver) {
            for (int y = g - diep + 1; y <= g; y++) {
                m.put(c.atY(y), WaterWerk.WATER);
            }
            m.put(c.atY(g - diep), Blocks.SAND.defaultBlockState());
            for (int y = g + 1; y <= g + 2; y++) {
                BlockState s = k.staat(c.atY(y));
                if (!s.isAir() && Klus.natuur(s)) {
                    m.put(c.atY(y), Blocks.AIR.defaultBlockState());
                }
            }
            // a rim where the ground around is lower (the water stays in)
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(d);
                if (in.contains(BlockPos.asLong(n.getX(), 0, n.getZ()))) {
                    continue;
                }
                for (int y = k.grond(n.getX(), n.getZ()) + 1; y <= g; y++) {
                    m.putIfAbsent(n.atY(y), Blocks.DIRT.defaultBlockState());
                }
            }
        }
    }

    @Nullable
    private static Block eerste(VerblijfData.Eis e) {
        for (String id : e.blokken()) {
            var b = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id));
            if (b.isPresent()) {
                return b.get();
            }
        }
        return null;
    }

    private static boolean grondsoort(Block b) {
        BlockState s = b.defaultBlockState();
        return s.is(BlockTags.SAND) || s.is(BlockTags.DIRT) || b == Blocks.SNOW_BLOCK || b == Blocks.ICE || b == Blocks.PACKED_ICE
                || b == Blocks.BLUE_ICE || b == Blocks.GRAVEL || b == Blocks.MUD || b == Blocks.CLAY || b == Blocks.TERRACOTTA;
    }

    private static void blokEis(Klus k, Map<BlockPos, BlockState> m, BlockPos plek, VerblijfData.Eis e) {
        Block b = eerste(e);
        if (b == null) {
            return;
        }
        int nodig = e.min() + 3;
        BlockState s = b.defaultBlockState();
        if (b == Blocks.SNOW) {
            b = Blocks.SNOW_BLOCK;
            s = b.defaultBlockState();
        }
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) {
            // trees of that wood (a log asks for its leaves and the other way round)
            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
            String hout = id.replace("_leaves", "_log").replace("_wood", "_log"), blad = hout.replace("_log", "_leaves");
            int bomen = Math.max(1, (int) Math.ceil(e.min() / (s.is(BlockTags.LOGS) ? 4.0 : 14.0)));
            for (int t = 0; t < bomen; t++) {
                boom(k, m, k.grondPos(plek.getX() + t * 3 - bomen, plek.getZ() + (t % 2) * 3), Blok.van(hout, Blocks.OAK_LOG), Blok.blad(blad));
            }
            return;
        }
        if (grondsoort(b)) {
            // a patch in the ground
            for (BlockPos c : vlek(k, plek, nodig)) {
                m.put(c, s);
            }
            return;
        }
        if (b == Blocks.LILY_PAD || s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT)) {
            // in or on the pond (made by the water requirement)
            List<BlockPos> water = new ArrayList<>();
            m.forEach((q, st) -> {
                if (st == WaterWerk.WATER) {
                    water.add(q);
                }
            });
            int n = 0;
            for (BlockPos q : water) {
                if (n >= nodig) {
                    break;
                }
                if (b == Blocks.LILY_PAD) {
                    if (m.get(q.above()) == null && k.staat(q.above()).isAir()) {
                        m.put(q.above(), s);
                        n++;
                    }
                } else if (m.get(q.below()) != WaterWerk.WATER && m.get(q.above()) == WaterWerk.WATER) {
                    m.put(q, Blocks.SEAGRASS.defaultBlockState());
                    n++;
                }
            }
            return;
        }
        if (s.isSolid() && s.isCollisionShapeFullBlock(k.level(), plek)) {
            // a little rock (or a heap), at most 2 high
            List<BlockPos> vlek = vlek(k, plek, (nodig + 1) / 2);
            for (int j = 0; j < vlek.size(); j++) {
                BlockPos c = vlek.get(j);
                m.put(c.above(), s);
                if (j < nodig - vlek.size()) {
                    m.put(c.above(2), s);
                }
            }
            return;
        }
        // something that stands on the ground (bamboo, flowers, a bush...)
        for (BlockPos c : vlek(k, plek, nodig)) {
            if (s.canSurvive(k.level(), c.above()) || !(s.getBlock() instanceof net.minecraft.world.level.block.BushBlock)) {
                m.put(c.above(), s);
            }
        }
    }

    private static void tagEis(Klus k, Map<BlockPos, BlockState> m, BlockPos plek, VerblijfData.Eis e) {
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.parse(e.tag()));
        if (tag.equals(BlockTags.LOGS) || tag.equals(BlockTags.LEAVES)) {
            int bomen = Math.max(1, (int) Math.ceil(e.min() / (tag.equals(BlockTags.LOGS) ? 4.0 : 14.0)));
            for (int t = 0; t < bomen; t++) {
                BlockPos voet = k.grondPos(plek.getX() + t * 3 - bomen, plek.getZ() + (t % 2) * 3);
                boom(k, m, voet, Blocks.OAK_LOG.defaultBlockState(), Blok.blad("minecraft:oak_leaves"));
            }
            return;
        }
        Block b = null;
        for (Holder<Block> h : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
            b = h.value();
            break;
        }
        if (b != null) {
            for (BlockPos c : vlek(k, plek, e.min() + 3)) {
                m.put(grondsoort(b) ? c : c.above(), b.defaultBlockState());
            }
        }
    }

    /** A small tree: a trunk of 4 and a round crown of leaves that never fall off. */
    private static void boom(Klus k, Map<BlockPos, BlockState> m, BlockPos voet, BlockState hout, BlockState blad) {
        int top = voet.getY() + 4;
        for (int y = voet.getY() + 1; y <= top; y++) {
            m.put(voet.atY(y), hout);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 > 5 || dx == 0 && dz == 0 && dy <= 0) {
                        continue;
                    }
                    m.putIfAbsent(voet.offset(dx, 0, dz).atY(top + dy), blad);
                }
            }
        }
    }

    /** n ground blocks (the top of the ground) around the spot, the closest first. */
    private static List<BlockPos> vlek(Klus k, BlockPos plek, int n) {
        List<BlockPos> uit = new ArrayList<>();
        for (int r = 0; uit.size() < n && r < 6; r++) {
            for (int dx = -r; dx <= r && uit.size() < n; dx++) {
                for (int dz = -r; dz <= r && uit.size() < n; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r && !k.isWater(plek.getX() + dx, plek.getZ() + dz)) {
                        uit.add(k.grondPos(plek.getX() + dx, plek.getZ() + dz));
                    }
                }
            }
        }
        return uit;
    }

    /** The fence on the home's edge (a closed ring), as high as the rule needs, a gate toward the board's front. */
    private static void hek(Klus k, Map<BlockPos, BlockState> m, BlockPos m0, Set<Long> binnen, VerblijfsbordBlockEntity be) {
        List<BlockPos> ring = new ArrayList<>();
        Set<Long> ringSet = new HashSet<>();
        for (long l : binnen) {
            BlockPos c = BlockPos.of(l);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long n = BlockPos.asLong(c.getX() + dx, 0, c.getZ() + dz);
                    if (!binnen.contains(n) && ringSet.add(n)) {
                        ring.add(BlockPos.of(n));
                    }
                }
            }
        }
        Map<Long, Integer> tops = new HashMap<>();
        BlockState hekje = Blok.van("guhs:guh_hekje_wit", Blocks.OAK_FENCE);
        Direction voor = k.level().getBlockState(be.getBlockPos()).getValueOrElse(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockPos poortDoel = m0.relative(voor, 100);
        BlockPos poort = null;
        double poortAfstand = Double.MAX_VALUE;
        for (BlockPos c : ring) {
            int g = k.grond(c.getX(), c.getZ());
            int hoogte = hekHoogte(k, m, c, g, ringSet, tops);
            for (int y = g + 1; y <= g + hoogte; y++) {
                m.put(c.atY(y), hekje);
            }
            if (hoogte == 1 && (c.getX() == m0.getX() || c.getZ() == m0.getZ())) {
                double d = c.distSqr(poortDoel.atY(0));
                if (d < poortAfstand) {
                    poortAfstand = d;
                    poort = c.atY(g + 1);
                }
            }
        }
        if (poort != null) {
            Direction as = poort.getX() == m0.getX() ? Direction.SOUTH : Direction.EAST;
            BlockState gate = Blok.van("guhs:guh_poortje_wit", Blocks.OAK_FENCE_GATE);
            if (!(gate.getBlock() instanceof FenceGateBlock) && !gate.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                gate = Blocks.OAK_FENCE_GATE.defaultBlockState();
            }
            m.put(poort, Blok.kant(gate, as));
        }
    }

    /**
     * How many fence blocks this column needs: 1, or (when something within 2 blocks rises above the fence's ground) up to 2
     * above that raise. Counts the world's ground and what the ghost itself adds (rocks, trees, rims), not other fences.
     */
    static int hekHoogte(Klus k, Map<BlockPos, BlockState> m, BlockPos c, int g, Set<Long> ring, Map<Long, Integer> tops) {
        int hoogst = g;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                long kol = BlockPos.asLong(c.getX() + dx, 0, c.getZ() + dz);
                if (ring.contains(kol)) {
                    continue;
                }
                hoogst = Math.max(hoogst, tops.computeIfAbsent(kol, l -> top(k, m, BlockPos.of(l))));
            }
        }
        return hoogst > g ? hoogst + 2 - g : 1;
    }

    /** The top of a column: its ground, or the highest solid block of the ghost there. */
    static int top(Klus k, Map<BlockPos, BlockState> m, BlockPos kol) {
        int t = k.grond(kol.getX(), kol.getZ());
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            BlockPos q = e.getKey();
            if (q.getX() == kol.getX() && q.getZ() == kol.getZ() && e.getValue().blocksMotion() && !e.getValue().is(BlockTags.LEAVES)) {
                t = Math.max(t, q.getY());
            }
        }
        return t;
    }

    /** (Gametest) does every fence column keep the rule: no raise within 2, or at least 2 above it? */
    public static boolean regelKlopt(Klus k, Map<BlockPos, BlockState> m) {
        Map<Long, Integer> hekTop = new HashMap<>();
        Map<Long, Integer> hekVoet = new HashMap<>();
        m.forEach((q, s) -> {
            if (s.is(BlockTags.FENCES) || s.getBlock() instanceof FenceGateBlock) {
                long kol = BlockPos.asLong(q.getX(), 0, q.getZ());
                hekTop.merge(kol, q.getY(), Math::max);
                hekVoet.merge(kol, q.getY(), Math::min);
            }
        });
        for (Map.Entry<Long, Integer> e : hekTop.entrySet()) {
            BlockPos c = BlockPos.of(e.getKey());
            int g = hekVoet.get(e.getKey()) - 1;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    long kol = BlockPos.asLong(c.getX() + dx, 0, c.getZ() + dz);
                    if (hekTop.containsKey(kol)) {
                        continue;
                    }
                    int t = top(k, m, BlockPos.of(kol));
                    if (t > g && e.getValue() < t + 2) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private PassendVerblijf() {
    }
}
