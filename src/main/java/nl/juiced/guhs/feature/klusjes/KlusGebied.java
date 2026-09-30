package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.block.KnabbelkorfBlock;
import nl.juiced.guhs.feature.bakkerij.KnabbelovenBlock;
import nl.juiced.guhs.feature.boerderij.KippennestjeBlock;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlock;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.feature.vadswoud.KnabbelbessenstruikBlock;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * What there is to do in the home base of a huisje: one scan of its whole area ({@link Huisje#inGebied}, the blue dome)
 * sorts the interesting blocks into kinds ({@link Soort}); kept for {@link #GELDIG} ticks per huisje so eight residents
 * trying ten chores don't all scan again. A chore always checks the block itself again before it works on it. Targets a
 * resident is walking to are claimed ({@link #claim}) so two guhs don't harvest the same wheat.
 */
public final class KlusGebied {
    /** How long one scan stays valid. */
    public static final int GELDIG = 100;

    public enum Soort {
        /** Ripe crops (vanilla, kaasknabbelplantjes) and ripe guhtuintjes. */
        GEWAS,
        /** Thirsty guhtuintjes. */
        DORSTIG,
        /** Grass, dirt or sand with room above: kaasknabbels can be dug up here. */
        GRAAF,
        /** Water sources with air above (fishing). */
        WATER,
        /** Knabbelbessen and sweet berries ready to pick. */
        BES,
        /** Guh flowers (kaasbloem, roze guhbloem). */
        BLOEM,
        /** Full knabbelkorven. */
        KORF,
        /** Kippennestjes with eggs. */
        NEST,
        /** Guh-molentjes. */
        MOLEN,
        /** Knabbelovens. */
        OVEN,
        /** Lamps of the tag guhs:klusjes/lampjes. */
        LAMP
    }

    private record Scan(long tot, Map<Soort, List<BlockPos>> lijsten) {
    }

    private static final Map<String, Scan> SCANS = new ConcurrentHashMap<>();
    /** Claimed targets: "dim|pos" -> game time the claim ends. */
    private static final Map<String, Long> CLAIMS = new ConcurrentHashMap<>();

    private KlusGebied() {
    }

    private static String sleutel(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "|" + pos.asLong();
    }

    /** Every block of this kind in the home base (from the last scan; maybe a few ticks old). */
    public static List<BlockPos> van(ServerLevel level, Huisje h, Soort soort) {
        String key = sleutel(level, h.pos());
        long nu = level.getGameTime();
        Scan s = SCANS.get(key);
        if (s == null || nu >= s.tot() || s.tot() - nu > GELDIG) {
            s = scan(level, h, nu);
            SCANS.put(key, s);
            if (SCANS.size() > 256) {
                SCANS.entrySet().removeIf(e -> e.getValue().tot() < nu);
            }
        }
        return s.lijsten().getOrDefault(soort, List.of());
    }

    /** Forgets every scan (tests: the world was changed on purpose). */
    public static void vergeet() {
        SCANS.clear();
        CLAIMS.clear();
    }

    /**
     * The nearest unclaimed block of this kind that still passes {@code nog} (the chore's own check of the block right
     * now), or null. {@code willekeurig}: a random one of them instead (digging spots: not always the same corner).
     */
    @Nullable
    public static BlockPos kies(ServerLevel level, Huisje h, Soort soort, Mob mob, boolean willekeurig, Predicate<BlockPos> nog) {
        List<BlockPos> lijst = new ArrayList<>(van(level, h, soort));
        if (lijst.isEmpty()) {
            return null;
        }
        if (willekeurig) {
            java.util.Collections.shuffle(lijst, new java.util.Random(mob.getRandom().nextLong()));
        } else {
            BlockPos from = mob.blockPosition();
            lijst.sort(Comparator.comparingDouble(p -> p.distSqr(from)));
        }
        int geprobeerd = 0;
        for (BlockPos p : lijst) {
            if (++geprobeerd > 48) {
                break;
            }
            if (!geclaimd(level, p) && level.isLoaded(p) && nog.test(p)) {
                return p;
            }
        }
        return null;
    }

    public static void claim(ServerLevel level, BlockPos pos, int ticks) {
        CLAIMS.put(sleutel(level, pos), level.getGameTime() + ticks);
        if (CLAIMS.size() > 512) {
            long nu = level.getGameTime();
            CLAIMS.entrySet().removeIf(e -> e.getValue() < nu);
        }
    }

    public static void vrij(ServerLevel level, BlockPos pos) {
        CLAIMS.remove(sleutel(level, pos));
    }

    public static boolean geclaimd(ServerLevel level, BlockPos pos) {
        Long tot = CLAIMS.get(sleutel(level, pos));
        return tot != null && tot > level.getGameTime();
    }

    // --- what a block is ---------------------------------------------------------------------------------------------------

    public static boolean rijpGewas(BlockState s) {
        return s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s) || TuinBlock.rijp(s);
    }

    public static boolean plukbaar(BlockState s) {
        return s.getBlock() instanceof KnabbelbessenstruikBlock && s.getValue(KnabbelbessenstruikBlock.AGE) > 1
                || s.getBlock() instanceof SweetBerryBushBlock && s.getValue(SweetBerryBushBlock.AGE) > 1;
    }

    public static boolean guhbloem(BlockState s) {
        return s.is(ModBlocks.KAASBLOEM.get()) || s.is(ModBlocks.ROZE_GUHBLOEM.get());
    }

    public static boolean volleKorf(BlockState s) {
        return s.getBlock() instanceof KnabbelkorfBlock && s.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS;
    }

    public static boolean nestMetEieren(BlockState s) {
        return s.getBlock() instanceof KippennestjeBlock && s.getValue(KippennestjeBlock.EIEREN) > 0;
    }

    public static boolean lampje(BlockState s) {
        return s.is(KlusjesFeature.LAMPJES) && s.hasProperty(BlockStateProperties.LIT);
    }

    /** Diggable ground with room above (not the huisje itself, not under a plant that would break). */
    public static boolean graafbaar(ServerLevel level, BlockPos p, BlockState s) {
        if (!s.is(KlusjesFeature.GRAAFGROND)) {
            return false;
        }
        BlockState boven = level.getBlockState(p.above());
        return boven.isAir() && level.getBlockState(p.above(2)).isAir();
    }

    public static boolean viswater(ServerLevel level, BlockPos p, BlockState s) {
        return s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource() && level.getBlockState(p.above()).isAir();
    }

    private static Scan scan(ServerLevel level, Huisje h, long nu) {
        Map<Soort, List<BlockPos>> uit = new EnumMap<>(Soort.class);
        AABB box = h.gebied();
        List<BlockPos> huisjeBlokken = h.blokken();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = (int) Math.floor(box.minX), x1 = (int) Math.ceil(box.maxX), z0 = (int) Math.floor(box.minZ), z1 = (int) Math.ceil(box.maxZ);
        int y0 = Math.max(level.getMinY(), (int) Math.floor(box.minY)), y1 = Math.min(level.getMaxY() + 1 - 3, (int) Math.ceil(box.maxY));
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, y0, z);
                if (!level.isLoaded(p)) {
                    continue;
                }
                for (int y = y0; y <= y1; y++) {
                    p.set(x, y, z);
                    if (!h.inGebied(p)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) {
                        continue;
                    }
                    Soort soort = soort(level, p, s);
                    if (soort != null && !(soort == Soort.GRAAF && naastHuisje(huisjeBlokken, p))) {
                        uit.computeIfAbsent(soort, k -> new ArrayList<>()).add(p.immutable());
                    }
                    if (TuinBlock.dorstig(s)) {
                        uit.computeIfAbsent(Soort.DORSTIG, k -> new ArrayList<>()).add(p.immutable());
                    }
                }
            }
        }
        return new Scan(nu + GELDIG, uit);
    }

    @Nullable
    private static Soort soort(ServerLevel level, BlockPos p, BlockState s) {
        if (rijpGewas(s)) {
            return Soort.GEWAS;
        }
        if (plukbaar(s)) {
            return Soort.BES;
        }
        if (guhbloem(s)) {
            return Soort.BLOEM;
        }
        if (volleKorf(s)) {
            return Soort.KORF;
        }
        if (nestMetEieren(s)) {
            return Soort.NEST;
        }
        if (s.getBlock() instanceof MolentjeBlock) {
            return Soort.MOLEN;
        }
        if (s.getBlock() instanceof KnabbelovenBlock) {
            return Soort.OVEN;
        }
        if (lampje(s)) {
            return Soort.LAMP;
        }
        if (viswater(level, p, s)) {
            return Soort.WATER;
        }
        if (graafbaar(level, p, s)) {
            return Soort.GRAAF;
        }
        return null;
    }

    private static boolean naastHuisje(List<BlockPos> blokken, BlockPos p) {
        for (BlockPos b : blokken) {
            if (Math.abs(b.getX() - p.getX()) <= 1 && Math.abs(b.getZ() - p.getZ()) <= 1 && Math.abs(b.getY() - p.getY()) <= 2) {
                return true;
            }
        }
        return false;
    }
}
