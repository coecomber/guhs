package nl.juiced.guhs.feature.onderwater;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The Zeemeerguh (GuhVariant.ZEEMEERGUH): a guh with a fish tail, only in the guh seas (Guhzee, Diepe Guhzee), and rare.
 * GuhEntity asks this class what a Zeemeerguh does differently:
 * <ul>
 *   <li>in water it dives and swims fast (smooth swimming like a dolphin: {@link #moveControl}, {@link #navigation},
 *       {@link #swimGoal}, {@link #travel}); on land it waddles like any guh; it breathes under water;</li>
 *   <li>tamed, big and saddled you can ride it under water: it goes where you look, jump = up ({@link #rideVelocity}),
 *       and it keeps its rider breathing ({@link #tick});</li>
 *   <li>now and then one turns up in the water of the guh seas near a player ({@link #onLevelTick}).</li>
 * </ul>
 */
public final class Zeemeerguh {
    public static final ResourceKey<Biome> GUH_SEA = ResourceKey.create(Registries.BIOME, Guhs.id("guh_sea"));
    /** The deep guh sea, with a Guhbubbel in its middle (tools/features/diepzee.py). */
    public static final ResourceKey<Biome> DIEPE_GUHZEE = ResourceKey.create(Registries.BIOME, Guhs.id("diepe_guhzee"));
    /** Where wild Zeemeerguhs turn up: the guh sea and the deep guh sea. */
    public static final TagKey<Biome> GUHZEEEN = TagKey.create(Registries.BIOME, Guhs.id("guhzeeen"));
    /** Spawning: checked every SPAWN_EVERY ticks per player in the guh seas, 1 in SPAWN_CHANCE, at most MAX_WILD wild ones around. */
    public static final int SPAWN_EVERY = 200, SPAWN_CHANCE = 6, SPAWN_RADIUS = 64, MAX_WILD = 2;
    /** Wild ones are always big enough to ride (once tamed). */
    public static final float MIN_SCALE = GuhEntity.RIDEABLE_SCALE + 0.15f, MAX_SCALE = MIN_SCALE + 0.4f;
    /** Swimming: speed factor of the move control in water; riding: blocks per tick (sprinting faster). */
    public static final float SWIM_SPEED = 0.14f;
    public static final double RIDE_SPEED = 0.55, RIDE_SPRINT = 0.85, RIDE_UP = 0.4;

    // --- how it moves ---------------------------------------------------------------------------------------------------

    /** In water: smooth 3D swimming; on land: the guh's own walking control. */
    public static MoveControl moveControl(GuhEntity guh, MoveControl walk) {
        return new SwimControl(guh, walk);
    }

    public static PathNavigation navigation(GuhEntity guh, Level level) {
        return new AmphibiousPathNavigation(guh, level);
    }

    /** Wandering around under water (wild, or a tamed one that may wander). */
    public static Goal swimGoal(GuhEntity guh) {
        return new RandomSwimmingGoal(guh, 1.0, 30) {
            @Override
            public boolean canUse() {
                return guh.isZeemeer() && guh.isInWater() && guh.mayWander() && !guh.isOrderedToSit() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return guh.isZeemeer() && !guh.isOrderedToSit() && super.canContinueToUse();
            }
        };
    }

    /**
     * Moving in water (GuhEntity.travel): ridden it follows the rider's look, otherwise it swims like a dolphin. Returns
     * false when the guh isn't a Zeemeerguh in water (then it moves like any guh).
     */
    public static boolean travel(GuhEntity guh, Vec3 input) {
        if (!guh.isZeemeer() || !guh.isInWater() || !guh.isControlledByLocalInstance()) {
            return false;
        }
        if (guh.getControllingPassenger() instanceof Player rider) {
            Vec3 motion = rideVelocity(guh.getDeltaMovement(), rider.getLookAngle(), rider.zza, rider.xxa,
                    GuhEntity.riderJumping.getAsBoolean(), rider.isSprinting());
            guh.setDeltaMovement(motion);
            guh.move(MoverType.SELF, motion);
            guh.calculateEntityAnimation(true);
            return true;
        }
        guh.moveRelative(guh.getSpeed(), input);
        guh.move(MoverType.SELF, guh.getDeltaMovement());
        guh.setDeltaMovement(guh.getDeltaMovement().scale(0.9));
        return true;
    }

    /** The next speed of a ridden Zeemeerguh under water: towards where the rider looks (W), sideways (A/D), up (jump). */
    public static Vec3 rideVelocity(Vec3 current, Vec3 look, float forward, float strafe, boolean up, boolean sprint) {
        Vec3 left = new Vec3(look.z, 0, -look.x).normalize();
        double speed = sprint ? RIDE_SPRINT : RIDE_SPEED;
        Vec3 want = look.scale(Math.max(0, forward) * speed).add(look.scale(Math.min(0, forward) * speed * 0.4)).add(left.scale(strafe * speed * 0.5));
        if (up) {
            want = want.add(0, RIDE_UP, 0);
        }
        return current.scale(0.8).add(want.scale(0.2));
    }

    /** Every tick of a Zeemeerguh: it keeps its rider breathing under water; bubbles now and then. */
    public static void tick(GuhEntity guh) {
        if (guh.level().isClientSide()) {
            if (guh.isInWater() && guh.getRandom().nextInt(8) == 0) {
                guh.level().addParticle(ParticleTypes.BUBBLE, guh.getRandomX(0.5), guh.getY() + guh.getBbHeight() * 0.8, guh.getRandomZ(0.5), 0, 0.05, 0);
            }
            return;
        }
        if (guh.tickCount % 20 != 0 || !guh.isVehicle()) {
            return;
        }
        for (var passenger : guh.getPassengers()) {
            if (passenger instanceof LivingEntity rider && OnderwaterFeature.underWater(rider)) {
                keepBreathing(rider);
                if (rider instanceof ServerPlayer player && guh.isOwnedBy(player)) {
                    GuhAdvancements.grant(player, "onderwater_rit");
                }
            }
        }
    }

    /** The rider of a Zeemeerguh under water: a little air bubble from the guh (a few seconds of water breathing). */
    public static void keepBreathing(LivingEntity rider) {
        rider.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
        rider.setAirSupply(rider.getMaxAirSupply());
    }

    // --- where they come from -------------------------------------------------------------------------------------------

    /** Now and then a wild Zeemeerguh in the water of the guh seas, near a player (never more than MAX_WILD around). */
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.GUHMENSION
                || level.getGameTime() % SPAWN_EVERY != 0 || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || random.nextInt(SPAWN_CHANCE) != 0 || !level.getBiome(player.blockPosition()).is(GUHZEEEN)
                    || level.getEntitiesOfClass(GuhEntity.class, new AABB(player.blockPosition()).inflate(SPAWN_RADIUS),
                    g -> g.isZeemeer() && !g.isTame()).size() >= MAX_WILD) {
                continue;
            }
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 12 + random.nextDouble() * 28;
                BlockPos water = waterAt(level, (int) (player.getX() + Math.cos(angle) * dist), (int) (player.getZ() + Math.sin(angle) * dist));
                if (water != null && level.getBiome(water).is(GUHZEEEN) && spawnAt(level, water, random) != null) {
                    break;
                }
            }
        }
    }

    /** A spot in the middle of the water at this column (under the surface), or null when it isn't water there. */
    @Nullable
    public static BlockPos waterAt(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        if (!level.getFluidState(p).is(FluidTags.WATER)) {
            p.move(0, -1, 0);                                          // (a lily pad on top)
        }
        int top = p.getY(), depth = 0;
        while (level.getFluidState(p).is(FluidTags.WATER) && depth < 40) {
            p.move(0, -1, 0);
            depth++;
        }
        return depth == 0 ? null : new BlockPos(x, top - (depth - 1) / 2, z);
    }

    /** A new wild Zeemeerguh in the water at pos (null if there's no room). */
    @Nullable
    public static GuhEntity spawnAt(ServerLevel level, BlockPos pos, RandomSource random) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0);
        guh.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        become(guh, random);
        if (!level.noCollision(guh)) {
            guh.discard();
            return null;
        }
        level.addFreshEntity(guh);
        return guh;
    }

    /** Makes a (wild) guh a Zeemeerguh: no clothes, no secret note, big enough to ride. */
    public static void become(GuhEntity guh, RandomSource random) {
        guh.setVariant(GuhVariant.ZEEMEERGUH);
        guh.setSecretNote(false);
        guh.takeOffClothes();
        guh.setGuhScale(MIN_SCALE + random.nextFloat() * (MAX_SCALE - MIN_SCALE));
    }

    /** In water: the smooth swimming of a dolphin; on land it hands over to the guh's walking control. */
    static final class SwimControl extends SmoothSwimmingMoveControl {
        private final MoveControl walk;

        SwimControl(GuhEntity guh, MoveControl walk) {
            super(guh, 85, 10, SWIM_SPEED, 1.0f, false);
            this.walk = walk;
        }

        @Override
        public void setWantedPosition(double x, double y, double z, double speed) {
            super.setWantedPosition(x, y, z, speed);
            walk.setWantedPosition(x, y, z, speed);
        }

        @Override
        public void strafe(float forward, float strafe) {
            super.strafe(forward, strafe);
            walk.strafe(forward, strafe);
        }

        @Override
        public void tick() {
            if (this.mob.isInWater()) {
                super.tick();
            } else {
                walk.tick();
            }
        }
    }

    private Zeemeerguh() {
    }
}
