package nl.juiced.guhs.feature.landdiertjes;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.VerhaalFeature;
import nl.juiced.guhs.registry.ModItems;

/**
 * The guh-konijntje: a round little bunny with long, soft HANGING ears (hangoortjes, round at the tips like guh ears), a guh
 * face with big glossy eyes and a twitchy snoetje, and a pompon tail. Lives in the Guhweides (and white ones in the
 * Sneeuwguhtoendra).
 * <ul>
 *   <li>Four furs ({@link Kleur}): roze, wit, choco and grijs (the Sneeuwguhtoendra only has white ones).</li>
 *   <li>It hops (little bounces while it walks). A wild one scampers away from players who walk up without sneaking.</li>
 *   <li>Tame it with a carrot (or a golden carrot / kaasknabbel), 1 in {@link #TAME_CHANCE}.</li>
 *   <li>Its big button "Hophop!": a happy binky, and you get konijnensprongetjes (Jump Boost) for a while.</li>
 * </ul>
 */
public class GuhKonijntjeEntity extends Landdiertje {
    public static final int TAME_CHANCE = 3;
    /** Seconds before "Hophop!" works again, and how long the jumps last. */
    public static final int RUST = 90, SPRONG_TICKS = 20 * 30;
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(GuhKonijntjeEntity.class, EntityDataSerializers.INT);

    public enum Kleur {
        ROZE, WIT, CHOCO, GRIJS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kleur van(int i) {
            Kleur[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    private int hopWacht;
    private long rustTot;

    public GuhKonijntjeEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
    }

    public Kleur kleur() {
        return Kleur.van(entityData.get(DATA_KLEUR));
    }

    public void setKleur(Kleur k) {
        entityData.set(DATA_KLEUR, k.ordinal());
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        if (level.getBiome(blockPosition()).is(VerhaalFeature.SNEEUWGUHTOENDRA)) {
            setKleur(Kleur.WIT);
        } else {
            int r = random.nextInt(10);
            setKleur(r < 4 ? Kleur.ROZE : r < 6 ? Kleur.WIT : r < 8 ? Kleur.CHOCO : Kleur.GRIJS);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    public String soort() {
        return "guh_konijntje";
    }

    @Override
    public Item oppakItem() {
        return LanddiertjesFeature.GUH_KONIJNTJE_ITEM.get();
    }

    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT) || stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public int temKans() {
        return TAME_CHANCE;
    }

    @Override
    protected SoundEvent geluid() {
        return LanddiertjesFeature.KONIJNTJE.get();
    }

    @Override
    protected double volgSnelheid() {
        return 1.25;
    }

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5.0f, 1.1, 1.4,
                e -> !isTame() && e instanceof Player p && !p.isCrouching() && !p.isSpectator() && !p.isCreative()
                        && !isVoer(p.getMainHandItem()) && !isVoer(p.getOffhandItem())));
    }

    /** It hops: a little bounce every few ticks while it walks. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }
        if (hopWacht > 0) {
            hopWacht--;
        } else if (onGround() && !isInWater() && getNavigation().isInProgress() && getDeltaMovement().horizontalDistanceSqr() > 0.0006) {
            setDeltaMovement(getDeltaMovement().add(0, 0.3, 0));
            hasImpulse = true;
            hopWacht = 7 + random.nextInt(4);
        }
    }

    @Override
    protected int calculateFallDamage(float distance, float multiplier) {
        return super.calculateFallDamage(distance - 2f, multiplier);   // (bunnies land softly)
    }

    // --- the big button -------------------------------------------------------------------------------------------------------------

    @Override
    public int rustSeconden() {
        return (int) Math.max(0, (rustTot - level().getGameTime() + 19) / 20);
    }

    @Override
    public void speciaal(ServerPlayer player) {
        if (rustSeconden() > 0) {
            return;
        }
        rustTot = level().getGameTime() + RUST * 20L;
        triggerAnim("actie", "binky");
        setDeltaMovement(getDeltaMovement().add(0, 0.45, 0));
        playSound(geluid(), 0.8f, 1.5f);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.6, getZ(), 5, 0.3, 0.3, 0.3, 0);
        }
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, SPRONG_TICKS, 1, false, true, true));
        player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.hophop", getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        GidsFeature.grant(player, "diertjes/landdiertjes_hophop");
    }

    @Override
    protected void extraActies(com.geckolib.animation.AnimationController<Landdiertje> actie) {
        actie.triggerableAnim("binky", com.geckolib.animation.RawAnimation.begin().thenPlay("binky"));
    }

    // --- save ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kleur", kleur().id());
        tag.putLong("HophopRust", Math.max(0, rustTot - level().getGameTime()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String k = tag.getStringOr("Kleur", "");
        for (Kleur kl : Kleur.values()) {
            if (kl.id().equals(k)) {
                setKleur(kl);
            }
        }
        rustTot = level().getGameTime() + Mth.clamp(tag.getLongOr("HophopRust", 0L), 0, RUST * 20L);
    }
}
