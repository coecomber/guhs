package nl.juiced.guhs.feature.bio.dieren;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.world.WildeDieren;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * De koi (biomes3): a plump, calm pond fish in guh style (the guh eyes, a blush, two round guh ears as little fins on its
 * head) in five colours ({@link Kleur}). It swims slowly in a school in the ponds and river pools of the Klaterdal and in
 * the Bloesemmeertje.
 * <ul>
 *   <li>Hold {@link KoivoerItem koivoer} near the water and the koi come up to the surface and beg ({@link NaarVoerGoal}).
 *       Sprinkle it on the water (or give it to one): they eat, hearts.</li>
 *   <li>Two grown koi that were both just fed get a kleintje, but only while the pond has room and both are rested
 *       ({@link DierenRegels#kleintjeMag}): no explosion.</li>
 *   <li>A water bucket scoops one up ({@link KoiEmmerItem}, its colour and size stay); released from the bucket it is
 *       yours: it stays for good ({@code FromBucket}). A wild one a spawner brought comes and goes like the other wild
 *       animals of Guhs. A kleintje of a kept koi is kept too.</li>
 * </ul>
 * Always lief: it never flees from players (the vanilla fish do) and nothing about it hurts anyone.
 */
public class KoiEntity extends AbstractSchoolingFish implements GeoEntity {
    /** The five colours; the weight is how often a wild koi has it. */
    public enum Kleur {
        ROODWIT(30), DRIEKLEUR(24), ROZE(20), BLAUW(16), GOUD(10);

        public final int gewicht;

        Kleur(int gewicht) {
            this.gewicht = gewicht;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** lang gui.guhs.koi.kleur.&lt;id&gt;. */
        public String langKey() {
            return "gui.guhs.koi.kleur." + id();
        }

        /** The colour with this id, or null. */
        @Nullable
        public static Kleur van(String id) {
            for (Kleur k : values()) {
                if (k.id().equals(id)) {
                    return k;
                }
            }
            return null;
        }

        public static Kleur willekeurig(RandomSource random) {
            int som = 0;
            for (Kleur k : values()) {
                som += k.gewicht;
            }
            int r = random.nextInt(som);
            for (Kleur k : values()) {
                if (r < k.gewicht) {
                    return k;
                }
                r -= k.gewicht;
            }
            return ROODWIT;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(KoiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_KLEIN = SynchedEntityData.defineId(KoiEntity.class, EntityDataSerializers.BOOLEAN);
    /** A kleintje is grown after this many ticks (one Guhmensie day); every feeding takes a tenth off what is left. */
    public static final int GROEITIJD = 24000;
    /** How close a player with koivoer has to be for the koi to come up, and how far sprinkled koivoer lures them. */
    public static final double LOK_AFSTAND = 8.0;
    /** Sprinkled koivoer stays interesting this long (ticks). */
    public static final int VOER_TIJD = 20 * 12;

    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");
    private static final RawAnimation DRIJF = RawAnimation.begin().thenLoop("drijf");
    private static final RawAnimation SPARTEL = RawAnimation.begin().thenLoop("spartel");
    private static final RawAnimation HAP = RawAnimation.begin().thenPlay("hap");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int groei;
    private long gevoerdOp = Long.MIN_VALUE / 2;
    private long volgendKleintje;
    /** Sprinkled koivoer it is swimming to (until {@link #voerTot}), and who sprinkled it. */
    @Nullable
    private Vec3 voerPlek;
    private long voerTot;
    @Nullable
    private UUID voerSpeler;

    public KoiEntity(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // (slow: a koi glides; the vanilla fish dart around at 0.7)
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.42);
    }

    /** In water; by itself only in the Klaterdal and the Bloesemmeertje, and never more wild ones together than the cap. */
    public static boolean checkSpawn(EntityType<KoiEntity> type, LevelAccessor level, EntitySpawnReason reden, BlockPos pos, RandomSource random) {
        boolean water = level.getFluidState(pos).is(FluidTags.WATER);
        if (!water || !DierenRegels.natuurlijk(reden)) {
            return DierenRegels.koiMag(reden, water, false, 0);
        }
        var biome = level.getBiome(pos);
        boolean inBiome = biome.is(Bio.BLOESEMMEERTJE) || biome.is(Bio.KLATERDAL);
        int wild = inBiome ? level.getEntitiesOfClass(KoiEntity.class, new AABB(pos).inflate(DierenRegels.KOI_TEL_STRAAL), k -> !k.fromBucket()).size() : 0;
        return DierenRegels.koiMag(reden, true, inBiome, wild);
    }

    // --- state ------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
        builder.define(DATA_KLEIN, false);
    }

    public Kleur kleur() {
        Kleur[] alle = Kleur.values();
        return alle[Math.floorMod(entityData.get(DATA_KLEUR), alle.length)];
    }

    public void setKleur(Kleur kleur) {
        entityData.set(DATA_KLEUR, kleur.ordinal());
    }

    /** A kleintje: half the size, grows up in {@link #GROEITIJD}. */
    public boolean isKlein() {
        return entityData.get(DATA_KLEIN);
    }

    public void setKlein(boolean klein) {
        entityData.set(DATA_KLEIN, klein);
        if (klein && groei <= 0) {
            groei = GROEITIJD;
        }
    }

    /** (Tests) ticks until a kleintje is grown. */
    public int groei() {
        return groei;
    }

    public void setGroei(int ticks) {
        groei = ticks;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kleur", kleur().id());
        tag.putBoolean("Klein", isKlein());
        tag.putInt("Groei", groei);
        tag.putInt("KleintjeWacht", (int) Math.max(0, volgendKleintje - level().getGameTime()));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        Kleur k = Kleur.van(tag.getStringOr("Kleur", ""));
        if (k != null) {
            setKleur(k);
        }
        groei = tag.getIntOr("Groei", 0);
        setKlein(tag.getBooleanOr("Klein", false));
        volgendKleintje = level().getGameTime() + tag.getIntOr("KleintjeWacht", 0);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reden, @Nullable SpawnGroupData data) {
        if (reden != EntitySpawnReason.BREEDING) {
            setKleur(Kleur.willekeurig(random));   // (from a bucket: loadFromBucketTag puts its own colour back right after)
        }
        return super.finalizeSpawn(level, difficulty, reden, data);
    }

    // --- the bucket -------------------------------------------------------------------------------------------------------

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(DierenSlice.KOI_EMMER.get());
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putString("Kleur", kleur().id());
            tag.putBoolean("Klein", isKlein());
            tag.putInt("Groei", groei);
        });
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        Kleur k = Kleur.van(tag.getStringOr("Kleur", ""));
        if (k != null) {
            setKleur(k);
        }
        groei = tag.getIntOr("Groei", 0);
        setKlein(tag.getBooleanOr("Klein", false));
        removeTag(WildeDieren.KOM_EN_GA);   // (from a bucket it is yours: it stays)
    }

    // --- goals ------------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // a koi is tame by nature: it never darts away from a player (AbstractFish adds that for the vanilla fish)
        this.goalSelector.removeAllGoals(g -> g instanceof AvoidEntityGoal);
        this.goalSelector.addGoal(1, new NaarVoerGoal());
    }

    @Override
    public int getMaxSchoolSize() {
        return 6;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && isKlein() && --groei <= 0) {
            setKlein(false);
        }
    }

    // --- koivoer ----------------------------------------------------------------------------------------------------------

    public static boolean heeftVoer(Player player) {
        return player.getMainHandItem().is(DierenSlice.KOIVOER.get()) || player.getOffhandItem().is(DierenSlice.KOIVOER.get());
    }

    /** (KoivoerItem) koivoer was sprinkled on the water there: swim to it and eat. */
    public void lok(Vec3 plek, @Nullable ServerPlayer speler) {
        voerPlek = plek;
        voerTot = level().getGameTime() + VOER_TIJD;
        voerSpeler = speler == null ? null : speler.getUUID();
    }

    public boolean voerActief() {
        return voerPlek != null && level().getGameTime() < voerTot;
    }

    /** Was it fed within the last {@link DierenRegels#GEVOERD_GELDIG} ticks? */
    public boolean netGevoerd() {
        return level().getGameTime() - gevoerdOp <= DierenRegels.GEVOERD_GELDIG;
    }

    public boolean uitgerust() {
        return level().getGameTime() >= volgendKleintje;
    }

    /** (Tests) rested again. */
    public void vergeetKleintje() {
        volgendKleintje = 0;
    }

    /**
     * It eats (server): hearts, a happy snap, a kleintje grows a bit faster, and when another grown koi close by was
     * just fed too they may get a kleintje. Returns that kleintje, or null.
     */
    @Nullable
    public KoiEntity voer(@Nullable ServerPlayer speler) {
        if (!(level() instanceof ServerLevel level)) {
            return null;
        }
        gevoerdOp = level.getGameTime();
        voerTot = 0;
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.55, getZ(), 2, 0.25, 0.12, 0.25, 0.0);
        level.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.3, getZ(), 4, 0.15, 0.1, 0.15, 0.02);
        triggerAnim("actie", "hap");
        playSound(DierenSlice.KOI_HAP.get(), 0.6f, (isKlein() ? 1.5f : 1.1f) + (random.nextFloat() - 0.5f) * 0.2f);
        if (isKlein()) {
            groei -= groei / 10;
        }
        if (speler != null) {
            GuhAdvancements.grant(speler, "koi_gevoerd");
        }
        KoiEntity partner = partner();
        if (partner == null) {
            return null;
        }
        int inVijver = level.getEntitiesOfClass(KoiEntity.class, getBoundingBox().inflate(DierenRegels.KOI_VIJVER_STRAAL)).size();
        if (!DierenRegels.kleintjeMag(!isKlein() && !partner.isKlein(), netGevoerd() && partner.netGevoerd(), uitgerust() && partner.uitgerust(), inVijver)) {
            return null;
        }
        KoiEntity kleintje = DierenSlice.KOI.get().create(level, EntitySpawnReason.BREEDING);
        if (kleintje == null) {
            return null;
        }
        kleintje.setKleur(random.nextInt(8) == 0 ? Kleur.willekeurig(random) : random.nextBoolean() ? kleur() : partner.kleur());
        kleintje.setKlein(true);
        kleintje.snapTo((getX() + partner.getX()) / 2, (getY() + partner.getY()) / 2, (getZ() + partner.getZ()) / 2, getYRot(), 0);
        if (fromBucket() || partner.fromBucket()) {
            kleintje.setFromBucket(true);        // born in your own pond: it is yours too
        } else {
            WildeDieren.markeer(kleintje);       // born in the wild: it comes and goes like its parents
        }
        volgendKleintje = partner.volgendKleintje = level.getGameTime() + DierenRegels.KLEINTJE_WACHT;
        level.addFreshEntity(kleintje);
        level.sendParticles(ParticleTypes.HEART, kleintje.getX(), kleintje.getY() + 0.5, kleintje.getZ(), 5, 0.4, 0.2, 0.4, 0.0);
        if (speler != null) {
            GuhAdvancements.grant(speler, "koi_kleintje");
        }
        return kleintje;
    }

    /** Another grown koi close by that was just fed and is rested. */
    @Nullable
    private KoiEntity partner() {
        List<KoiEntity> lijst = level().getEntitiesOfClass(KoiEntity.class, getBoundingBox().inflate(6),
                k -> k != this && k.isAlive() && !k.isKlein() && k.netGevoerd() && k.uitgerust());
        return lijst.isEmpty() ? null : lijst.get(0);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(DierenSlice.KOIVOER.get())) {
            if (player instanceof ServerPlayer sp) {
                stack.consume(1, player);
                voer(sp);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** The y just under the water surface above it (the top of its own water column). */
    public double oppervlak() {
        BlockPos.MutableBlockPos p = blockPosition().mutable();
        for (int i = 0; i < 12 && level().getFluidState(p.above()).is(FluidTags.WATER); i++) {
            p.move(0, 1, 0);
        }
        return p.getY() + 0.45;
    }

    /**
     * Koivoer! To the sprinkled food and eat it; or up to the surface, as close to the player holding koivoer as the water
     * goes, mouth open.
     */
    private class NaarVoerGoal extends Goal {
        @Nullable
        private Player speler;
        private int ticks;

        NaarVoerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!isInWater()) {
                return false;
            }
            if (voerActief()) {
                return true;
            }
            speler = level().getNearestPlayer(KoiEntity.this.getX(), KoiEntity.this.getY(), KoiEntity.this.getZ(), LOK_AFSTAND,
                    e -> e instanceof Player p && !p.isSpectator() && heeftVoer(p));
            return speler != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!isInWater()) {
                return false;
            }
            return voerActief() || speler != null && speler.isAlive() && heeftVoer(speler) && distanceToSqr(speler) < (LOK_AFSTAND + 4) * (LOK_AFSTAND + 4);
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 doel;
            if (voerActief()) {
                doel = new Vec3(voerPlek.x, Math.min(voerPlek.y - 0.3, oppervlak()), voerPlek.z);
                double dx = doel.x - getX(), dz = doel.z - getZ();
                if (dx * dx + dz * dz < 1.4 * 1.4 && Math.abs(doel.y - getY()) < 1.6) {
                    voer(voerSpeler != null && level().getPlayerByUUID(voerSpeler) instanceof ServerPlayer sp ? sp : null);
                    return;
                }
            } else if (speler != null) {
                doel = bijSpeler(speler);
                getLookControl().setLookAt(speler, 30f, 30f);
                if (ticks % 40 == 30 && distanceToSqr(doel) < 4.0) {
                    triggerAnim("actie", "hap");   // begging at the surface
                }
            } else {
                return;
            }
            if (ticks++ % 10 == 0) {
                getNavigation().moveTo(doel.x, doel.y, doel.z, 1.25);
            }
        }

        /** The spot at the surface closest to the player that is still water (from the player towards the koi). */
        private Vec3 bijSpeler(Player p) {
            double y = oppervlak();
            Vec3 van = new Vec3(p.getX(), y, p.getZ()), naar = new Vec3(getX(), y, getZ());
            Vec3 stap = naar.subtract(van);
            double lengte = stap.length();
            if (lengte < 0.5) {
                return naar;
            }
            stap = stap.scale(0.75 / lengte);
            Vec3 q = van;
            for (double d = 0; d < lengte; d += 0.75) {
                if (level().getFluidState(BlockPos.containing(q.x, y - 0.3, q.z)).is(FluidTags.WATER)) {
                    return q;
                }
                q = q.add(stap);
            }
            return naar;
        }

        @Override
        public void stop() {
            speler = null;
            getNavigation().stop();
        }
    }

    // --- sounds -----------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zwem", 4, state -> state.setAndContinue(!isInWater() ? SPARTEL : state.isMoving() ? ZWEM : DRIJF)));
        controllers.add(new AnimationController<>("actie", 2, state -> PlayState.STOP).triggerableAnim("hap", HAP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
