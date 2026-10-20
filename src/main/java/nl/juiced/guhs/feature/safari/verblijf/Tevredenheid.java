package nl.juiced.guhs.feature.safari.verblijf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Op Safari! step 2 (S5): how happy a family is at home (0..100 %), with the checklist the board's menu shows (design 1.9:
 * "nijlpaard: ✔ water ≥ 20, ✘ modder, ✔ gras"):
 * <ul>
 *   <li>its {@link VerblijfData.Eis requirements} (blocks in the home, counted in a box around the board: from 6 below to 10
 *       above it): 60 points shared;</li>
 *   <li>room to roam (an area of at least 12): 10; fed today: 10; petted today: 10;</li>
 *   <li>its toy in the home: 5; a friend's board within 24 blocks: 5.</li>
 * </ul>
 * Counting is a few thousand block reads, so the board does it now and then ({@link VerblijfsbordBlockEntity}), not per tick.
 */
public final class Tevredenheid {
    public static final int ONDER = 6, BOVEN = 10;

    /** One requirement's result. */
    public record Regel(String id, int aantal, int min) {
        public boolean met() {
            return aantal >= min;
        }
    }

    public record Uitslag(int score, List<Regel> eisen, boolean ruimte, boolean gevoerd, boolean geaaid, boolean speeltje, boolean vrienden) {
        public CompoundTag tag() {
            CompoundTag t = new CompoundTag();
            t.putInt("Score", score);
            ListTag l = new ListTag();
            for (Regel r : eisen) {
                CompoundTag e = new CompoundTag();
                e.putString("Id", r.id());
                e.putInt("Aantal", r.aantal());
                e.putInt("Min", r.min());
                l.add(e);
            }
            t.put("Eisen", l);
            t.putBoolean("Ruimte", ruimte);
            t.putBoolean("Gevoerd", gevoerd);
            t.putBoolean("Geaaid", geaaid);
            t.putBoolean("Speeltje", speeltje);
            t.putBoolean("Vrienden", vrienden);
            return t;
        }
    }

    public static Uitslag reken(ServerLevel level, VerblijfsbordBlockEntity be) {
        VerblijfData.Dier d = VerblijfData.dier(be.soort());
        BlockPos m = be.middelpunt();
        int hx = be.rechthoek() ? be.breedte() : be.straal(), hz = be.rechthoek() ? be.diepte() : be.straal();
        Map<Block, Integer> blokken = new HashMap<>();
        int water = 0;
        boolean speeltje = false;
        Block speelBlok = d == null ? null : VerblijfFeature.speeltje(d.speeltje());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -hx; dx <= hx; dx++) {
            for (int dz = -hz; dz <= hz; dz++) {
                if (!be.rechthoek() && dx * dx + dz * dz > hx * hx) {
                    continue;
                }
                int x = m.getX() + dx, z = m.getZ() + dz;
                if (!level.isLoaded(p.set(x, m.getY(), z))) {
                    continue;
                }
                for (int y = m.getY() - ONDER; y <= m.getY() + BOVEN; y++) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (s.isAir()) {
                        continue;
                    }
                    if (s.getFluidState().is(Fluids.WATER) || s.getFluidState().is(Fluids.FLOWING_WATER)) {
                        water++;
                    }
                    blokken.merge(s.getBlock(), 1, Integer::sum);
                    if (speelBlok != null && s.is(speelBlok)) {
                        speeltje = true;
                    }
                }
            }
        }
        List<Regel> eisen = new java.util.ArrayList<>();
        int met = 0;
        if (d != null) {
            for (VerblijfData.Eis e : d.eisen()) {
                int n = switch (e.soort()) {
                    case "water" -> water;
                    case "tag" -> tel(blokken, TagKey.create(Registries.BLOCK, Identifier.parse(e.tag())));
                    default -> tel(blokken, e.blokken());
                };
                Regel r = new Regel(e.id(), n, e.min());
                eisen.add(r);
                if (r.met()) {
                    met++;
                }
            }
        }
        boolean ruimte = be.rechthoek() ? Math.min(be.breedte(), be.diepte()) >= 8 : be.straal() >= 12;
        boolean gevoerd = be.vandaagGevoerd(), geaaid = be.vandaagGeaaid();
        boolean vrienden = d != null && vriendIn(level, be, d);
        int score = (eisen.isEmpty() ? 60 : 60 * met / eisen.size()) + (ruimte ? 10 : 0) + (gevoerd ? 10 : 0) + (geaaid ? 10 : 0)
                + (speeltje ? 5 : 0) + (vrienden ? 5 : 0);
        return new Uitslag(Math.min(100, score), List.copyOf(eisen), ruimte, gevoerd, geaaid, speeltje, vrienden);
    }

    private static int tel(Map<Block, Integer> blokken, List<String> ids) {
        int n = 0;
        for (String id : ids) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
            n += blokken.getOrDefault(b, 0);
        }
        return n;
    }

    private static int tel(Map<Block, Integer> blokken, TagKey<Block> tag) {
        int n = 0;
        for (Map.Entry<Block, Integer> e : blokken.entrySet()) {
            if (e.getKey().defaultBlockState().is(tag)) {
                n += e.getValue();
            }
        }
        return n;
    }

    /** A friend's verblijfsbord within 24 blocks (the boards remember themselves in {@link VerblijfsbordBlockEntity#BORDEN}). */
    private static boolean vriendIn(ServerLevel level, VerblijfsbordBlockEntity be, VerblijfData.Dier d) {
        for (VerblijfsbordBlockEntity ander : VerblijfsbordBlockEntity.bordenBij(level, be.getBlockPos(), 24)) {
            if (ander != be && d.vriend(ander.soort())) {
                return true;
            }
        }
        return false;
    }

    /** The nearest toy of this kind within r blocks of p (8 up/down), or null. */
    @Nullable
    public static BlockPos zoekSpeeltje(ServerLevel level, BlockPos p, int r, String speeltje) {
        Block b = VerblijfFeature.speeltje(speeltje);
        if (b == null) {
            return null;
        }
        BlockPos beste = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-r, -4, -r), p.offset(r, 4, r))) {
            if (level.getBlockState(q).is(b)) {
                double d = q.distSqr(p);
                if (d < bd) {
                    bd = d;
                    beste = q.immutable();
                }
            }
        }
        return beste;
    }

    private Tevredenheid() {
    }
}
