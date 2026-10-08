package nl.juiced.guhs.feature.bio.dieren;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.kaasmoeras.KikkerguhEntity;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.registry.ModItems;

/**
 * biomes3: the kikkerguh of the lake. The existing {@link KikkerguhEntity} (no second frog) gets three habits, only in
 * the three new biomes (and for a kikkerguh with the entity tag {@link #BLADKIKKER}, for structures and tests), so the
 * Kaasmoeras stays exactly as it was:
 * <ul>
 *   <li>{@link OpBladGoal}: it looks for a lily pad (a vanilla one, a guh-waterlelie or drijvende bloesemblaadjes),
 *       hops onto it and sits there for a good while;</li>
 *   <li>{@link SpringWegGoal}: a player who comes close ({@link #SCHRIK} blocks, sneaking: {@link #SCHRIK_SLUIP}) makes
 *       it leap into the water and paddle off; when the coast is clear it comes back to a leaf. Holding kaasknabbels
 *       (its favourite) it trusts you;</li>
 *   <li>{@link #kwaakInterval}: in the evening it croaks much more often (a chorus at dusk), at night a bit more.</li>
 * </ul>
 * KikkerguhEntity calls {@link #doelen}, {@link #kwaakInterval} and {@link #spawnMag} (its three lines marked biomes3).
 * Cheap: a leaf is looked for in a small box around it once every few seconds, water to flee into with a few probes.
 */
public final class KikkerBlad {
    /** An entity tag: this kikkerguh behaves like a lake frog wherever it is. */
    public static final String BLADKIKKER = "guhs_bio_dieren_bladkikker";
    /** How close a player may come before it hops off (walking, sneaking). */
    public static final double SCHRIK = 4.5, SCHRIK_SLUIP = 2.0;
    /** How far (blocks, sideways) it looks for a leaf. */
    public static final int ZOEK = 6;
    /** Ticks between two croaks: by day (the kikkerguh's own), in the evening, at night. */
    public static final int KWAAK_AVOND = 55, KWAAK_NACHT = 110;
    /** After a fright it stays in the water at least this long (ticks). */
    public static final int VLUCHT_TIJD = 20 * 4;
    static final String VLUCHT_TOT = "guhs_bio_dieren_vlucht_tot";

    private KikkerBlad() {
    }

    /** Does this kikkerguh live the lake life (a new biome, or tagged)? */
    public static boolean inGebied(KikkerguhEntity k) {
        return k.entityTags().contains(BLADKIKKER) || Bio.inNieuw(k.level(), k.blockPosition());
    }

    /** A leaf a kikkerguh sits on. */
    public static boolean isBlad(BlockState state) {
        if (state.is(Blocks.LILY_PAD) || state.is(Bio.blok("drijvende_bloesemblaadjes", Blocks.LILY_PAD))) {
            return true;
        }
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(Guhs.id("guh_waterlelie"));
    }

    /** (KikkerguhEntity.registerGoals) */
    public static void doelen(KikkerguhEntity k, GoalSelector goals) {
        goals.addGoal(1, new SpringWegGoal(k));
        goals.addGoal(5, new OpBladGoal(k));
    }

    /** (KikkerguhEntity.getAmbientSoundInterval) evening: a chorus. */
    public static int kwaakInterval(KikkerguhEntity k, int standaard) {
        return inGebied(k) ? kwaakInterval(Dagdeel.huidig(k.level()), standaard) : standaard;
    }

    public static int kwaakInterval(Dagdeel dagdeel, int standaard) {
        return dagdeel == Dagdeel.AVOND ? KWAAK_AVOND : dagdeel == Dagdeel.NACHT ? KWAAK_NACHT : standaard;
    }

    /** (KikkerguhEntity.checkKikkerguhSpawnRules) in a new biome: only near water, and not too many together. */
    public static boolean spawnMag(LevelAccessor level, EntitySpawnReason reden, BlockPos pos) {
        if (!DierenRegels.natuurlijk(reden)) {
            return true;
        }
        var biome = level.getBiome(pos);
        if (!(biome.is(Bio.BLOESEMMEERTJE) || biome.is(Bio.KLATERDAL) || biome.is(Bio.WOLKENWEIDE))) {
            return true;
        }
        boolean water = false;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(q).is(FluidTags.WATER)) {
                water = true;
                break;
            }
        }
        int dichtbij = water ? level.getEntitiesOfClass(KikkerguhEntity.class, new AABB(pos).inflate(DierenRegels.KIKKER_TEL_STRAAL)).size() : 0;
        return DierenRegels.kikkerMag(reden, true, water, dichtbij);
    }

    /** Is it sitting on a leaf right now? */
    public static boolean opBlad(KikkerguhEntity k) {
        return bladOnder(k) != null;
    }

    @Nullable
    static BlockPos bladOnder(KikkerguhEntity k) {
        Level level = k.level();
        BlockPos p = k.blockPosition();
        if (isBlad(level.getBlockState(p))) {
            return p;
        }
        return isBlad(level.getBlockState(p.below())) && k.onGround() ? p.below() : null;
    }

    public static boolean opDeVlucht(KikkerguhEntity k) {
        return k.getPersistentData().getLongOr(VLUCHT_TOT, 0L) > k.level().getGameTime();
    }

    /** The player that makes this kikkerguh hop off, or null. */
    @Nullable
    public static Player schrik(KikkerguhEntity k) {
        Player p = k.level().getNearestPlayer(k.getX(), k.getY(), k.getZ(), SCHRIK, e -> e instanceof Player q && !q.isSpectator() && eng(k, q));
        return p;
    }

    /** Does this player scare it? Too close (sneaking: much closer), and no kaasknabbels in hand. */
    public static boolean eng(KikkerguhEntity k, Player p) {
        if (p.getMainHandItem().is(ModItems.KAAS_KNABBELS.get()) || p.getOffhandItem().is(ModItems.KAAS_KNABBELS.get())) {
            return false;
        }
        double d = p.isShiftKeyDown() ? SCHRIK_SLUIP : SCHRIK;
        return k.distanceToSqr(p) < d * d;
    }

    /** A hop: up and this way (blocks per tick, sideways). */
    static void spring(KikkerguhEntity k, double dx, double dz, double kracht, double omhoog) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            dx = 1;
            dz = 0;
            len = 1;
        }
        k.setDeltaMovement(dx / len * kracht, omhoog, dz / len * kracht);
        k.needsSync = true;
        k.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        k.yBodyRot = k.getYRot();
        k.playSound(SoundEvents.FROG_LONG_JUMP, 0.5f, 1.3f);
    }

    /** Hops to a leaf close by and sits on it. */
    static class OpBladGoal extends Goal {
        private final KikkerguhEntity k;
        @Nullable
        private BlockPos blad;
        private long volgendeZoek;
        private int ticks, zit, zitMax, sprongWacht;

        OpBladGoal(KikkerguhEntity k) {
            this.k = k;
            setFlags(EnumSet.of(Flag.MOVE)); // biomes3 merge: not JUMP (it never uses the jump control). With JUMP the kikkerguh's FloatGoal (priority 0, flag JUMP) stopped this goal for as long as the frog floated: a hop that fell short left it bobbing beside its leaf until a random stroll brought it ashore (the flaky bioDierenKikkerOpBlad)
        }

        @Override
        public boolean canUse() {
            long nu = k.level().getGameTime();
            if (nu < volgendeZoek) {
                return false;
            }
            volgendeZoek = nu + 40 + k.getRandom().nextInt(40);
            if (!inGebied(k) || opDeVlucht(k) || k.isInLove() || schrik(k) != null) {
                return false;
            }
            blad = bladOnder(k);
            if (blad == null) {
                blad = zoek();
            }
            return blad != null;
        }

        /**
         * The nearest leaf within {@link #ZOEK} blocks (a block up or down), free above and with no other kikkerguh on it.
         * A small box, looked through once every few seconds: no leaf is ever far on a pond.
         */
        @Nullable
        private BlockPos zoek() {
            Level level = k.level();
            BlockPos hier = k.blockPosition();
            BlockPos beste = null;
            double besteD = Double.MAX_VALUE;
            for (BlockPos q : BlockPos.betweenClosed(hier.offset(-ZOEK, -1, -ZOEK), hier.offset(ZOEK, 1, ZOEK))) {
                double d = q.distSqr(hier);
                if (d < besteD && isBlad(level.getBlockState(q)) && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                        && level.getEntitiesOfClass(KikkerguhEntity.class, new AABB(q).inflate(0.3, 0.6, 0.3), o -> o != k).isEmpty()) {
                    beste = q.immutable();
                    besteD = d;
                }
            }
            return beste;
        }

        @Override
        public boolean canContinueToUse() {
            if (blad == null || opDeVlucht(k) || !isBlad(k.level().getBlockState(blad))) {
                return false;
            }
            return zit > 0 ? zit < zitMax : ticks < 20 * 20;
        }

        @Override
        public void start() {
            ticks = 0;
            zit = 0;
            sprongWacht = 0;
            zitMax = 20 * 40 + k.getRandom().nextInt(20 * 80);
            k.getNavigation().moveTo(blad.getX() + 0.5, blad.getY() + 0.1, blad.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (blad == null) {
                return;
            }
            ticks++;
            double dx = blad.getX() + 0.5 - k.getX(), dz = blad.getZ() + 0.5 - k.getZ();
            double d2 = dx * dx + dz * dz;
            BlockPos onder = bladOnder(k);
            if (onder != null && onder.equals(blad) && d2 < 0.3 * 0.3) {
                // on its leaf: sit still, look around
                if (zit++ == 0) {
                    k.getNavigation().stop();
                }
                Vec3 v = k.getDeltaMovement();
                k.setDeltaMovement(0, v.y, 0);
                return;
            }
            if (sprongWacht > 0) {
                sprongWacht--;
            }
            double dy = blad.getY() + 0.1 - k.getY();
            if (d2 < 0.6 * 0.6 && Math.abs(dy) < 0.6) {
                // right at it: the last little shuffle to the middle
                k.getNavigation().stop();
                k.getMoveControl().setWantedPosition(blad.getX() + 0.5, blad.getY() + 0.1, blad.getZ() + 0.5, 0.8);
            } else if (d2 < 3.2 * 3.2 && dy < 1.3 && dy > -1.6 && sprongWacht == 0 && (k.isInWater() || k.onGround())) {
                // close enough: one hop onto the leaf (the reach of a hop: about 11 x its speed, air drag 0.91)
                double afstand = Math.sqrt(d2);
                spring(k, dx, dz, Math.min(0.42, 0.075 * afstand + 0.03), 0.36 + Math.max(0, dy) * 0.12);
                k.getNavigation().stop();
                sprongWacht = 24;
            } else if (ticks % 20 == 0 && sprongWacht == 0) {
                k.getNavigation().moveTo(blad.getX() + 0.5, blad.getY() + 0.1, blad.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void stop() {
            blad = null;
            k.getNavigation().stop();
        }
    }

    /** A player comes close: a leap into the water, away from them, and paddle off. */
    static class SpringWegGoal extends Goal {
        private final KikkerguhEntity k;
        @Nullable
        private Player speler;
        @Nullable
        private Vec3 doel;
        private int ticks;

        SpringWegGoal(KikkerguhEntity k) {
            this.k = k;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!inGebied(k) || opDeVlucht(k)) {
                return false;
            }
            speler = schrik(k);
            if (speler == null) {
                return false;
            }
            doel = water(speler);
            return doel != null;
        }

        /** Water to leap into: away from the player, a few blocks off (some random probes in that half). */
        @Nullable
        private Vec3 water(Player van) {
            Level level = k.level();
            RandomSource r = k.getRandom();
            double hoek = Math.atan2(k.getZ() - van.getZ(), k.getX() - van.getX());
            BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 14; i++) {
                double a = hoek + (r.nextDouble() - 0.5) * (i < 6 ? 1.2 : 2.6);
                double afstand = 2.0 + r.nextDouble() * 3.5;
                double x = k.getX() + Math.cos(a) * afstand, z = k.getZ() + Math.sin(a) * afstand;
                for (int dy = 0; dy >= -2; dy--) {
                    q.set(BlockPos.containing(x, k.getY() + dy, z));
                    if (level.getFluidState(q).is(FluidTags.WATER) && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()) {
                        return new Vec3(x, q.getY() + 0.6, z);
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && ticks < VLUCHT_TIJD;
        }

        @Override
        public void start() {
            ticks = 0;
            k.getNavigation().stop();
            k.getPersistentData().putLong(VLUCHT_TOT, k.level().getGameTime() + VLUCHT_TIJD + 20 + k.getRandom().nextInt(40));
            double dx = doel.x - k.getX(), dz = doel.z - k.getZ();
            spring(k, dx, dz, Math.min(0.5, 0.08 * Math.sqrt(dx * dx + dz * dz) + 0.12), 0.4);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (doel == null) {
                return;
            }
            ticks++;
            if (k.isInWater() && speler != null) {
                // paddle on, away from the player
                double dx = k.getX() - speler.getX(), dz = k.getZ() - speler.getZ();
                double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                Vec3 verder = new Vec3(k.getX() + dx / len * 3, k.getY(), k.getZ() + dz / len * 3);
                if (k.level().getFluidState(BlockPos.containing(verder)).is(FluidTags.WATER)) {
                    k.getMoveControl().setWantedPosition(verder.x, verder.y, verder.z, 1.3);
                }
            }
        }

        @Override
        public void stop() {
            speler = null;
            doel = null;
        }
    }
}
