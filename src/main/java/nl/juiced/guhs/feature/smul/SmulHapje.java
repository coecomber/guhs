package nl.juiced.guhs.feature.smul;

import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * A bit of food falling into the eetfestijn arena (only during a {@link SmulGame}): it drops slowly, lies on the floor
 * for a moment and then goes splat. The game catches it when its player walks into it. On the client it marks the spot
 * where it will land with little dots, so you can run there in time. Never saved: it only lives for one game.
 */
public class SmulHapje extends Entity {
    /** The kinds of food: points, how often they fall (the Mika-vet falls more and more often as the minute goes by). */
    public enum Soort {
        KAASKNABBEL(1, 30),
        GEFRITUURD(2, 12),
        CUPCAKE(2, 14),
        MACARON(3, 10),
        MILKSHAKE(3, 8),
        TAART(5, 5),
        GOUD(10, 3),
        MIKA_VET(-5, 12);

        public final int points;
        final int weight;

        Soort(int points, int weight) {
            this.points = points;
            this.weight = weight;
        }

        public boolean good() {
            return this != MIKA_VET;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        int weight(float progress) {
            return weight(progress, nl.juiced.guhs.feature.spelen.Niveau.MEDIUM);
        }

        /**
         * How often this falls, for this point in the game and this level (2.9): only the Mika-vet changes with the level
         * (makkelijk half as much, lastig half as much more, and it grows faster during the minute).
         */
        public int weight(float progress, nl.juiced.guhs.feature.spelen.Niveau niveau) {
            if (this != MIKA_VET) {
                return weight;
            }
            return switch (niveau) {
                case MAKKELIJK -> weight / 2 + Math.round(8 * progress);
                case MEDIUM -> weight + Math.round(16 * progress);
                case LASTIG -> weight + weight / 2 + Math.round(26 * progress);
            };
        }

        /** A random kind of food, for this point in the game (0 = start, 1 = the end). */
        public static Soort pick(RandomSource random, float progress) {
            return pick(random, progress, nl.juiced.guhs.feature.spelen.Niveau.MEDIUM);
        }

        /** A random kind of food, for this point in the game and this level. */
        public static Soort pick(RandomSource random, float progress, nl.juiced.guhs.feature.spelen.Niveau niveau) {
            int total = 0;
            for (Soort s : values()) {
                total += s.weight(progress, niveau);
            }
            int r = random.nextInt(total);
            for (Soort s : values()) {
                r -= s.weight(progress, niveau);
                if (r < 0) {
                    return s;
                }
            }
            return KAASKNABBEL;
        }

        public ItemStack stack(RandomSource random) {
            Item item = switch (this) {
                case KAASKNABBEL -> ModItems.KAAS_KNABBELS.get();
                case GEFRITUURD -> ModItems.GEFRITUURDE_KAASKNABBELS.get();
                case CUPCAKE -> ModItems.GUH_CUPCAKE.get();
                case MACARON -> ModItems.MACARONS.values().stream().skip(random.nextInt(ModItems.MACARONS.size())).findFirst().orElseThrow().get();
                case MILKSHAKE -> ModItems.KAASKNABBEL_MILKSHAKE.get();
                case TAART -> ModItems.GUH_TAART.get();
                case GOUD -> SmulFeature.GOUDEN_SMULKNABBEL.get();
                case MIKA_VET -> ModItems.MIKA_VET.get();
            };
            return new ItemStack(item);
        }
    }

    /** How long it lies on the floor (you can still catch it) before it goes splat. */
    public static final int LIE_TICKS = 16;

    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(SmulHapje.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(SmulHapje.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(SmulHapje.class, EntityDataSerializers.FLOAT);

    /** The Smulguh whose game this is (server side). */
    @Nullable
    private UUID game;
    private int landed;

    public SmulHapje(EntityType<? extends SmulHapje> type, Level level) {
        super(type, level);
    }

    public static SmulHapje create(ServerLevel level, Soort soort, float speed, UUID game) {
        SmulHapje hapje = new SmulHapje(SmulFeature.HAPJE.get(), level);
        hapje.entityData.set(DATA_SOORT, soort.ordinal());
        hapje.entityData.set(DATA_ITEM, soort.stack(level.getRandom()));
        hapje.entityData.set(DATA_SPEED, speed);
        hapje.game = game;
        return hapje;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_SOORT, 0);
        builder.define(DATA_SPEED, 0.2f);
    }

    public Soort soort() {
        int i = entityData.get(DATA_SOORT);
        return i >= 0 && i < Soort.values().length ? Soort.values()[i] : Soort.KAASKNABBEL;
    }

    public ItemStack stack() {
        return entityData.get(DATA_ITEM);
    }

    @Nullable
    public UUID game() {
        return game;
    }

    public boolean hasLanded() {
        return landed > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (game == null || !SmulGame.isRunning(game))) {
            discard();                                    // its game is over (or this is a leftover after a restart)
            return;
        }
        if (landed == 0) {
            setDeltaMovement(0, -entityData.get(DATA_SPEED), 0);
            move(MoverType.SELF, getDeltaMovement());
            if (onGround() || verticalCollisionBelow) {
                landed = 1;
                setDeltaMovement(0, 0, 0);
                if (!level().isClientSide()) {
                    level().playSound(null, getX(), getY(), getZ(), soort().good() ? SoundEvents.WOOL_PLACE : SoundEvents.HONEY_BLOCK_PLACE,
                            SoundSource.NEUTRAL, 0.35f, 1.3f);
                }
            }
        } else if (++landed > LIE_TICKS && !level().isClientSide()) {
            splat();
            return;
        }
        if (level().isClientSide()) {
            clientEffects();
        }
    }

    /** It lay on the floor too long: splat (and a missed good snack breaks your combo). */
    private void splat() {
        if (level() instanceof ServerLevel world) {
            world.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack().getItem()), getX(), getY() + 0.2, getZ(), 10, 0.25, 0.1, 0.25, 0.08);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.5f, 1.2f);
            if (soort().good() && game != null) {
                SmulGame.missed(game);
            }
        }
        discard();
    }

    /** Dots on the floor where it will land; sparkles for the golden one, smoke for Mika-vet. */
    private void clientEffects() {
        if (landed == 0 && tickCount % 3 == 0) {
            BlockPos.MutableBlockPos pos = blockPosition().mutable();
            for (int i = 0; i < 32 && pos.getY() > level().getMinY(); i++) {
                pos.move(0, -1, 0);
                if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                    int colour = switch (soort()) {
                        case GOUD -> 0xFFD933;
                        case MIKA_VET -> 0x403333;
                        default -> 0xFF73BF;
                    };
                    level().addParticle(new DustParticleOptions(colour, 1.2f), getX() + (random.nextDouble() - 0.5) * 0.5,
                            pos.getY() + 1.05, getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
                    break;
                }
            }
        }
        if (soort() == Soort.GOUD && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.4, getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
        } else if (soort() == Soort.MIKA_VET && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 0, 0.02, 0);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }
}
