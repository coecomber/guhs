package nl.juiced.guhs.feature.bio.dieren;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * biomes3: the wild guhs of the three new biomes.
 * <ul>
 *   <li><b>The biome guhs</b> (the pattern of the Pluisguh and the Kaasmoerasguh): a plain wild guh born in the
 *       Bloesemmeertje may be a {@link GuhVariant#BLOESEMGUH Bloesemguh}, in the Klaterdal a
 *       {@link GuhVariant#TANUKIGUH Tanukiguh}, and in the Wolkenweide a {@link GuhVariant#WOLK Wolkguh} (until now there
 *       was only the one on the floating guh islands). Nowhere else: {@link #variantVoor}, {@link #kies}.</li>
 *   <li><b>The evening nap</b> ({@link AvonddutjeGoal}): in the evening and at night a wild guh in these biomes strolls
 *       to water close by and curls up beside it (the looping SLAPEN emote, eyes closed); it wakes in the morning, or
 *       when something disturbs it.</li>
 * </ul>
 * Cheap: the nap goal asks the clock first, and looks for water with a handful of random probes once in a while.
 */
public final class BiomeGuhs {
    /** Out of the plain wild guhs born in their biome, this many become the biome's guh. */
    public static final float BLOESEMGUH_KANS = 0.25f, TANUKIGUH_KANS = 0.25f, WOLKGUH_KANS = 0.12f;
    static final String GEKEKEN = "guhs_bio_dieren_gekeken", DUTJE = "guhs_bio_dieren_dutje";
    /** How far (sideways) a sleepy guh looks for water. */
    public static final int WATER_ZOEK = 10;

    private BiomeGuhs() {
    }

    /** (DierenSlice.register) */
    static void hooks() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(6, new AvonddutjeGoal(guh)));
        GuhHooks.tick(BiomeGuhs::tick);
    }

    // --- the biome guhs ---------------------------------------------------------------------------------------------------

    /** The guh of this biome, or null. */
    @Nullable
    public static GuhVariant variantVoor(@Nullable ResourceKey<Biome> biome) {
        if (biome == Bio.BLOESEMMEERTJE) {
            return GuhVariant.BLOESEMGUH;
        }
        if (biome == Bio.KLATERDAL) {
            return GuhVariant.TANUKIGUH;
        }
        return biome == Bio.WOLKENWEIDE ? GuhVariant.WOLK : null;
    }

    public static float kans(GuhVariant variant) {
        return switch (variant) {
            case BLOESEMGUH -> BLOESEMGUH_KANS;
            case TANUKIGUH -> TANUKIGUH_KANS;
            case WOLK -> WOLKGUH_KANS;
            default -> 0f;
        };
    }

    /** Which of the three new biomes is this spot in (null: none)? */
    @Nullable
    public static ResourceKey<Biome> nieuwBiome(Level level, BlockPos pos) {
        var b = level.getBiome(pos);
        return b.is(Bio.BLOESEMMEERTJE) ? Bio.BLOESEMMEERTJE : b.is(Bio.KLATERDAL) ? Bio.KLATERDAL : b.is(Bio.WOLKENWEIDE) ? Bio.WOLKENWEIDE : null;
    }

    /**
     * Decides, once per guh, whether a new wild guh born in this biome becomes the biome's guh: only a plain, wild,
     * grown, unnamed guh. Returns true when it did.
     */
    public static boolean kies(GuhEntity guh, @Nullable ResourceKey<Biome> biome) {
        CompoundTag data = guh.getPersistentData();
        if (guh.getClass() != GuhEntity.class || data.getBooleanOr(GEKEKEN, false)) {
            return false;
        }
        data.putBoolean(GEKEKEN, true);
        GuhVariant variant = variantVoor(biome);
        if (variant == null || guh.isTame() || guh.isBaby() || guh.hasCustomName() || guh.getVariant() != GuhVariant.NORMAL || GuhHooks.isBewoner(guh)) {
            return false;
        }
        if (guh.getRandom().nextFloat() < kans(variant)) {
            guh.setVariant(variant);
            return true;
        }
        return false;
    }

    // --- the evening nap --------------------------------------------------------------------------------------------------

    /** Sleepy time for the wild guhs by the water: the evening and the night. */
    public static boolean dutjesTijd(Dagdeel dagdeel) {
        return dagdeel == Dagdeel.AVOND || dagdeel == Dagdeel.NACHT;
    }

    /** A wild guh that may take the evening nap: nobody's, grown or not, not busy, not a town resident. */
    public static boolean magDutten(GuhEntity guh) {
        return guh.getClass() == GuhEntity.class && !guh.isTame() && !guh.isNoAi() && !GuhHooks.isBewoner(guh) && !GuhHooks.isBezig(guh)
                && !guh.isVehicle() && !guh.isPassenger() && !guh.isLeashed() && !guh.isInLove();
    }

    public static boolean dut(GuhEntity guh) {
        return guh.getPersistentData().getBooleanOr(DUTJE, false);
    }

    /** Curls up for the evening nap (the looping SLAPEN emote). */
    public static boolean slaap(GuhEntity guh) {
        if (!guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF)) {
            return false;
        }
        guh.getPersistentData().putBoolean(DUTJE, true);
        return true;
    }

    public static void wakker(GuhEntity guh) {
        guh.getPersistentData().remove(DUTJE);
        if (guh.emotes.current() == Emote.SLAPEN) {
            guh.emotes.stop();
        }
    }

    /** (GuhHooks.tick, every 2 s per guh) a napping guh wakes when the night is over, or when it was woken already. */
    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 40 != 0 || !dut(guh)) {
            return;
        }
        if (guh.emotes.current() != Emote.SLAPEN || !dutjesTijd(Dagdeel.huidig(guh.level())) || !magDutten(guh)) {
            wakker(guh);
        }
    }

    /** Water (with air above it) within {@link #WATER_ZOEK} blocks: a few random probes. */
    @Nullable
    public static BlockPos waterDichtbij(GuhEntity guh) {
        Level level = guh.level();
        RandomSource r = guh.getRandom();
        BlockPos hier = guh.blockPosition();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            int x = hier.getX() + r.nextInt(2 * WATER_ZOEK + 1) - WATER_ZOEK, z = hier.getZ() + r.nextInt(2 * WATER_ZOEK + 1) - WATER_ZOEK;
            for (int dy = 1; dy >= -3; dy--) {
                q.set(x, hier.getY() + dy, z);
                if (level.getFluidState(q).is(FluidTags.WATER) && level.getFluidState(q.above()).isEmpty()) {
                    return q.immutable();
                }
            }
        }
        return null;
    }

    /** In the evening: to the water's edge, and sleep. */
    static class AvonddutjeGoal extends Goal {
        private final GuhEntity guh;
        @Nullable
        private BlockPos water;
        private long volgende;
        private int ticks;
        private boolean klaar;

        AvonddutjeGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Level level = guh.level();
            if (guh.isTame() || !dutjesTijd(Dagdeel.huidig(level))) {
                return false;
            }
            long nu = level.getGameTime();
            if (nu < volgende) {
                return false;
            }
            volgende = nu + 100 + guh.getRandom().nextInt(200);
            if (dut(guh) || !magDutten(guh) || guh.emotes.current() != null || !GuhEmotes.canStart(guh) || !Bio.inNieuw(level, guh.blockPosition())) {
                return false;
            }
            water = waterDichtbij(guh);
            return water != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !klaar && water != null && ticks < 20 * 15 && magDutten(guh);
        }

        @Override
        public void start() {
            ticks = 0;
            klaar = false;
            guh.getNavigation().moveTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0.8);
        }

        @Override
        public void tick() {
            ticks++;
            // at the edge (the path ends where the land does), or as close as it gets: curl up
            boolean dichtbij = guh.distanceToSqr(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5) < 3.0 * 3.0;
            if ((dichtbij || guh.getNavigation().isDone() && ticks > 20) && !guh.isInWater() && guh.onGround()) {
                guh.getNavigation().stop();
                klaar = slaap(guh) || ticks > 20 * 10;
            }
        }

        @Override
        public void stop() {
            water = null;
        }
    }
}
