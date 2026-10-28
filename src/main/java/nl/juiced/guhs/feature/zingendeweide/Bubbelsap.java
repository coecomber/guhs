package nl.juiced.guhs.feature.zingendeweide;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;

/**
 * Bubbelsap: the lief take on the Sift's ichor. A shiny rainbow fluid that flows like water, but you can't drown in it and it
 * never hurts. Standing or swimming in it makes you {@link Bubbelig}: little bubbles around you and a bit higher jumps, for a
 * while after you get out. Blubs love to bathe in it (bubbles and a happy squeak).
 */
public final class Bubbelsap {
    /** How long you stay bubbly after the sap (ticks). */
    public static final int BUBBELIG_TICKS = 20 * 12;

    private Bubbelsap() {
    }

    static FluidType type() {
        return new FluidType(FluidType.Properties.create()
                .descriptionId("block.guhs.bubbelsap")
                .canSwim(true)
                .canDrown(false)
                .canExtinguish(true)
                .supportsBoating(true)
                .pathType(PathType.WATER)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .lightLevel(4)
                .density(1000)
                .viscosity(1200)) {
            @Override
            public boolean move(LivingEntity entity, Vec3 input, double gravity) {
                return zwem(this, entity, input, gravity);
            }
        };
    }

    /**
     * Moving through bubbelsap. On 26.1 NeoForge a modded fluid whose FluidType doesn't move the entity itself gets no movement
     * at all (LivingEntity.travelInFluid only handles water and lava): you were stuck. This is water's swimming, a bit
     * floatier (less gravity, a softer drag), so wading and swimming feel bubbly and you can always climb out.
     */
    static boolean zwem(FluidType type, LivingEntity entity, Vec3 input, double gravity) {
        double y0 = entity.getY();
        boolean falling = entity.getDeltaMovement().y <= 0.0;
        float snelheid = entity.isSprinting() ? 0.9F : 0.85F;
        float kracht = 0.03F;
        if (!entity.onGround()) {
            kracht *= 0.8F;
        }
        entity.moveRelative(kracht, input);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 v = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.onClimbable()) {
            v = new Vec3(v.x, 0.2, v.z);
        }
        entity.setDeltaMovement(v.multiply(snelheid, 0.82F, snelheid));
        entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity * 0.6, falling, entity.getDeltaMovement()));
        Vec3 w = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.isFree(w.x, w.y + 0.6F - entity.getY() + y0, w.z)) {
            entity.setDeltaMovement(w.x, 0.3F, w.z);
        }
        return true;
    }

    static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(ZingendeWeideFeature.BUBBELSAP_TYPE, ZingendeWeideFeature.BUBBELSAP, ZingendeWeideFeature.BUBBELSAP_STROOM)
                .bucket(ZingendeWeideFeature.BUBBELSAP_EMMER).block(ZingendeWeideFeature.BUBBELSAP_BLOK).slopeFindDistance(4).levelDecreasePerBlock(1)
                .tickRate(6);
    }

    /** The sap's block: whoever is in it gets bubbly; a Blub in it bathes happily. */
    public static class Blok extends LiquidBlock {
        public Blok(FlowingFluid fluid, Properties properties) {
            super(fluid, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
            super.entityInside(state, level, pos, entity, effects, precise);
            if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)) {
                return;
            }
            if (living.tickCount % 10 == 0) {
                living.addEffect(new MobEffectInstance(ZingendeWeideFeature.BUBBELIG, BUBBELIG_TICKS, 0, true, true, true));
            }
            if (living instanceof BlubGuhEntity && level.getRandom().nextInt(60) == 0) {
                server.sendParticles(ParticleTypes.BUBBLE_POP, living.getX(), living.getY() + 0.6, living.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
                server.sendParticles(ParticleTypes.HEART, living.getX(), living.getY() + 0.9, living.getZ(), 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }

    /** Bubbly: little bubbles around you and a bit higher jumps (harmless). */
    public static class Bubbelig extends MobEffect {
        public Bubbelig() {
            super(MobEffectCategory.BENEFICIAL, 0xB8E8FF, ParticleTypes.BUBBLE_POP);
            addAttributeModifier(Attributes.JUMP_STRENGTH, Guhs.id("effect.bubbelig"), 0.12, AttributeModifier.Operation.ADD_VALUE);
            addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, Guhs.id("effect.bubbelig_val"), 2.0, AttributeModifier.Operation.ADD_VALUE);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 8 == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 2,
                    entity.getBbWidth() * 0.6, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.6, 0.01);
            return true;
        }
    }
}
