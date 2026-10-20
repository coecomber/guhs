package nl.juiced.guhs.feature.safari.rit;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The Struisvogelrace (S3, design 1.7 / ronde 38): an ostrich of the savanna race track. Click it at the start, wait for
 * "3, 2, 1... rennen!", then run {@link RitData#raceRondes} laps (W to run, A/D to steer, space to jump). Every lap has
 * {@link #POORTEN} checkpoints on the track; at the finish your time goes onto the world's top 3 (the Scorebord) and you
 * get a few Safarimunten. Getting off ends the race; the ostrich walks back to the start. Model: the park's own struisvogel.
 */
public class RaceStruisvogelEntity extends Entity implements GeoEntity {
    public static final String BORD = "safari_struisvogelrace";
    public static final int POORTEN = 8, AFTEL = 60;
    public static final double TOP = 0.36;
    private static final EntityDataAccessor<Float> DATA_VAART = SynchedEntityData.defineId(RaceStruisvogelEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private double snelheid;
    /** Server: the race of its rider (null: waiting at the start). */
    @Nullable
    private UUID renner;
    private long start;
    private int poort, ronde;
    private int springRust;

    public RaceStruisvogelEntity(EntityType<? extends RaceStruisvogelEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VAART, 0f);
    }

    public float vaart() {
        return entityData.get(DATA_VAART);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p && getPassengers().isEmpty() && renner == null) {
            if (p.startRiding(this)) {
                renner = p.getUUID();
                start = level().getGameTime() + AFTEL;
                poort = 1;
                ronde = 0;
                titel(p, Component.literal("3"), ChatFormatting.GOLD);
                p.sendSystemMessage(Component.translatable("gui.guhs.safari.race.uitleg", RitData.get().raceRondes).withStyle(ChatFormatting.YELLOW));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void titel(ServerPlayer p, Component tekst, ChatFormatting kleur) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 18, 4));
        p.connection.send(new ClientboundSetTitleTextPacket(tekst.copy().withStyle(kleur, ChatFormatting.BOLD)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            if (vaart() > 0.2f && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.DUST_PLUME, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            }
            return;
        }
        RitData d = RitData.get();
        ServerPlayer rijder = getFirstPassenger() instanceof ServerPlayer sp ? sp : null;
        if (renner != null && (rijder == null || !rijder.getUUID().equals(renner))) {
            stopRace(rijder, false);
        }
        float vooruit = 0, stuur = 0;
        boolean spring = false;
        long nu = level().getGameTime();
        if (rijder != null && renner != null) {
            long tot = start - nu;
            if (tot > 0) {
                if (tot == 40) {
                    titel(rijder, Component.literal("2"), ChatFormatting.GOLD);
                } else if (tot == 20) {
                    titel(rijder, Component.literal("1"), ChatFormatting.GOLD);
                }
            } else {
                if (tot == 0) {
                    titel(rijder, Component.translatable("gui.guhs.safari.race.rennen"), ChatFormatting.GREEN);
                    level().playSound(null, this, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1f, 1.5f);
                }
                Input in = rijder.getLastClientInput();
                vooruit = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0);
                stuur = (in.right() ? 1 : 0) - (in.left() ? 1 : 0);
                spring = in.jump();
                poorten(rijder, d, nu);
            }
        } else if (d.raceWacht != null && distanceToSqr(d.raceWacht) > 9) {
            // nobody riding: back to the start (a calm walk)
            Vec3 r = d.raceWacht.subtract(position());
            setYRot((float) Math.toDegrees(Math.atan2(-r.x, r.z)));
            vooruit = 0.35f;
        }
        loop(vooruit, stuur, spring);
    }

    private void loop(float vooruit, float stuur, boolean spring) {
        double top = renner != null ? TOP : 0.12;
        if (vooruit > 0.1f) {
            snelheid = Math.min(top * Math.min(1f, vooruit * 3f), snelheid + 0.03);
        } else if (vooruit < -0.1f) {
            snelheid = Math.max(-0.08, snelheid - 0.03);
        } else {
            snelheid *= 0.8;
        }
        if (stuur != 0) {
            setYRot(getYRot() + stuur * 6f);
        }
        Vec3 r = Vec3.directionFromRotation(0, getYRot()).scale(snelheid);
        double vy = getDeltaMovement().y;
        if (onGround()) {
            vy = spring && springRust <= 0 ? 0.48 : -0.08;
            if (spring && springRust <= 0) {
                springRust = 14;
            }
        } else {
            vy = Math.max(-1.5, vy - 0.08);
        }
        if (springRust > 0) {
            springRust--;
        }
        setDeltaMovement(r.x, vy, r.z);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            snelheid *= 0.4;
        }
        entityData.set(DATA_VAART, (float) Math.abs(snelheid));
    }

    /** Checkpoints: in order around the track; through the start line after the last one = a lap. */
    private void poorten(ServerPlayer rijder, RitData d, long nu) {
        Pad pad = d.race;
        if (pad == null) {
            return;
        }
        int elk = Math.max(1, pad.aantal() / POORTEN);
        Vec3 doel = pad.get((poort % POORTEN) * elk);
        if (Math.hypot(getX() - doel.x, getZ() - doel.z) > 5.0) {
            return;
        }
        if (poort % POORTEN == 0) {
            ronde++;
            if (ronde >= d.raceRondes) {
                finish(rijder, (int) (nu - start));
                return;
            }
            rijder.sendOverlayMessage(Component.translatable("gui.guhs.safari.race.ronde", ronde + 1, d.raceRondes).withStyle(ChatFormatting.AQUA));
            level().playSound(null, this, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 0.8f, 1.6f);
        }
        poort++;
    }

    private void finish(ServerPlayer rijder, int ticks) {
        int plaats = Scorebord.submit(rijder, BORD, ticks, true);
        String tijd = tijd(ticks);
        rijder.sendSystemMessage(Component.translatable(plaats > 0 ? "gui.guhs.safari.race.finish_top" : "gui.guhs.safari.race.finish", tijd, plaats)
                .withStyle(ChatFormatting.GOLD));
        titel(rijder, Component.literal(tijd), ChatFormatting.YELLOW);
        level().playSound(null, this, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
        SafariMunten.verdienDagelijks(rijder, "race", 10, 30, "race");
        stopRace(rijder, true);
    }

    public static String tijd(int ticks) {
        int ms = ticks * 50;
        return String.format(java.util.Locale.ROOT, "%d:%02d.%d", ms / 60000, ms / 1000 % 60, ms / 100 % 10);
    }

    private void stopRace(@Nullable ServerPlayer rijder, boolean klaar) {
        renner = null;
        snelheid = 0;
        if (rijder != null) {
            rijder.stopRiding();
            if (!klaar) {
                rijder.sendOverlayMessage(Component.translatable("gui.guhs.safari.race.gestopt"));
            }
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, 1.05, 0.1).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    private static final RawAnimation REN = RawAnimation.begin().thenLoop("ren");
    private static final RawAnimation LOOP = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation STIL = RawAnimation.begin().thenLoop("idle");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("loop", 4, state -> state.setAndContinue(vaart() > 0.2f ? REN : vaart() > 0.03f ? LOOP : STIL)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
