package nl.juiced.guhs.feature.paleizen;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.quest.GuhAdvancements;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

/**
 * Het Worstzwijntje (bbq2, the hoglin parody of the Guhbarbecuether): a braadworstje on four stubby legs with a snout, two
 * tiny tusks, a squiggle of mustard down its back and a curly tail with a knot in it. Always lief; it never attacks anyone.
 * <p>
 * It lives three lives:
 * <ul>
 *   <li><b>a farm animal</b> (the default; the reward of the Stalknecht-guh, its babies, a spawn egg): a
 *       {@link BoerderijDier} like the animals of the Guhboerderij. Pet, brush and feed it; content, it sniffs up a little
 *       something once a day (loot table {@link #SNUFFEL_LOOT}).</li>
 *   <li><b>one of the stable's own</b> ({@link #stalNr} 0..4, from the template of guhs:mika_stal or made by Bezetting): it
 *       stays round its box, can't be hurt, fed, bred or led away, and it is "restless" for every player who has not petted
 *       it calm yet in their questline ({@link StalQuest#aai}).</li>
 *   <li><b>a runaway</b> ({@link #ontsnaptVan}): Knorretje, one per player who is looking for it. It hides at a few spots
 *       round the stable, flees to the next one when its player stomps up to it (sneaking does not startle it), is tired
 *       after {@link #MAX_VLUCHT} sprints and lets itself be picked up ({@link StalQuest#pak}). Never saved: it is gone when
 *       its player leaves, and back when they come looking again.</li>
 * </ul>
 * Model, animations and texture: tools/features/paleizen_modellen.py.
 */
public class WorstzwijntjeEntity extends BoerderijDier {
    /** What a content Worstzwijntje sniffs up (once a day). */
    public static final ResourceKey<LootTable> SNUFFEL_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("gameplay/paleizen_snuffelvondst"));
    /** A runaway flees this many times before it is too tired to run. */
    public static final int MAX_VLUCHT = 3;
    /** How near a stomping player may come before a runaway bolts (blocks). */
    public static final double SCHRIK = 4.5;
    /** A flight that takes longer than this ends with a poof at the next hiding spot (ticks). */
    public static final int VLUCHT_TICKS = 70;
    /** How far a stable animal strays from its box before it is put back (blocks). */
    private static final int THUIS_STRAAL = 4, THUIS_TERUG = 12;

    private static final EntityDataAccessor<Integer> DATA_STAL = SynchedEntityData.defineId(WorstzwijntjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ONRUSTIG = SynchedEntityData.defineId(WorstzwijntjeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ONRUSTIG = RawAnimation.begin().thenLoop("onrustig");

    /** A stable piglet stays a piglet. */
    private boolean biggetje;
    /** Where a stable animal belongs. */
    @Nullable
    private BlockPos thuis;
    /** The player a runaway ran away from (null: not a runaway). */
    @Nullable
    private UUID ontsnaptVan;
    private List<Vec3> schuilplekken = List.of();
    private int schuil, gevlucht, vlucht;

    public WorstzwijntjeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    // --- its three lives -------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAL, -1);
        builder.define(DATA_ONRUSTIG, false);
    }

    /** The number of a stable animal (0..4), or -1. */
    public int stalNr() {
        return entityData.get(DATA_STAL);
    }

    public boolean isStal() {
        return stalNr() >= 0;
    }

    /** Is it restless for somebody near (the stable animals' nervous shuffle)? */
    public boolean isOnrustig() {
        return entityData.get(DATA_ONRUSTIG);
    }

    public boolean isOntsnapt() {
        return ontsnaptVan != null;
    }

    @Nullable
    public UUID ontsnaptVan() {
        return ontsnaptVan;
    }

    /** How often this runaway has fled. */
    public int gevlucht() {
        return gevlucht;
    }

    /** The hiding spot this runaway is at or on its way to. */
    public int schuilplek() {
        return schuil;
    }

    /** Makes this one of the stable's own: number {@code nr}, at home where it stands. */
    public void zetStal(int nr, boolean isBiggetje) {
        entityData.set(DATA_STAL, nr);
        biggetje = isBiggetje;
        thuis = blockPosition();
        setHomeTo(thuis, isBiggetje ? THUIS_STRAAL + 2 : THUIS_STRAAL);
        setPersistenceRequired();
        setInvulnerable(true);
        if (isBiggetje) {
            setAge(-24000);
        }
    }

    /** Makes this a runaway of this player that hides at these spots (world positions), starting at spot {@code eerste}. */
    public void zetOntsnapt(UUID speler, List<Vec3> plekken, int eerste) {
        ontsnaptVan = speler;
        schuilplekken = List.copyOf(plekken);
        schuil = Math.floorMod(eerste, Math.max(1, plekken.size()));
        gevlucht = 0;
        vlucht = 0;
        setInvulnerable(true);
        setCustomName(Component.translatable("gui.guhs.paleizen.knorretje"));
        setCustomNameVisible(true);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stal", stalNr());
        tag.putBoolean("Biggetje", biggetje);
        if (thuis != null) {
            tag.putLong("Thuis", thuis.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_STAL, tag.getIntOr("Stal", -1));
        biggetje = tag.getBooleanOr("Biggetje", false);
        thuis = tag.getLong("Thuis").map(BlockPos::of).orElse(null);
    }

    /** A runaway belongs to a player who is looking for it now: it is never written to disk. */
    @Override
    public boolean shouldBeSaved() {
        return !isOntsnapt() && super.shouldBeSaved();
    }

    // --- the farm animal -------------------------------------------------------------------------------------------------

    @Override
    public String soort() {
        return "worstzwijntje";
    }

    /** Snuf snuf: it roots up a little something. */
    @Override
    protected void geefProduct(@Nullable ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(SNUFFEL_LOOT);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.GIFT);
        for (ItemStack stack : table.getRandomItems(params)) {
            ItemEntity item = new ItemEntity(level, getX(), getY() + 0.3, getZ(), stack);
            item.setDefaultPickUpDelay();
            item.setDeltaMovement((getRandom().nextDouble() - 0.5) * 0.12, 0.28, (getRandom().nextDouble() - 0.5) * 0.12);
            level.addFreshEntity(item);
        }
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        triggerAnim("actie", "eet");
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.paleizen.snuffel").withStyle(ChatFormatting.LIGHT_PURPLE));
            GuhAdvancements.grant(player, "paleizen_snuffel");
        }
    }

    @Override
    protected void speelGeluid() {
        playSound(PaleizenFeature.KNOR.get(), 0.8f, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PaleizenFeature.KNOR.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PaleizenFeature.GIL.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PaleizenFeature.GIL.get();
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.35f : 1.0f) + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return PaleizenFeature.WORSTZWIJNTJE.get().create(level, EntitySpawnReason.BREEDING);
    }

    // --- the stable's own and the runaway: not yours -------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isStal() && !isOntsnapt()) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            if (isOntsnapt()) {
                StalQuest.pak(sp, this);
            } else {
                StalQuest.aai(sp, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return !isStal() && !isOntsnapt() && super.isFood(stack);
    }

    @Override
    public boolean canFallInLove() {
        return !isStal() && !isOntsnapt() && super.canFallInLove();
    }

    @Override
    public boolean canBeLeashed() {
        return !isStal() && !isOntsnapt() && super.canBeLeashed();
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return (isStal() || isOntsnapt()) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(level, source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !isStal() || (tickCount + getId()) % 20 != 0) {
            return;
        }
        if (thuis == null) {
            // (it came with the template: where it stands is home)
            zetStal(stalNr(), biggetje);
        } else if (!hasHome()) {
            setHomeTo(thuis, biggetje ? THUIS_STRAAL + 2 : THUIS_STRAAL);
        }
        if (thuis != null && blockPosition().distSqr(thuis) > THUIS_TERUG * THUIS_TERUG) {
            snapTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, getYRot(), 0f);
            getNavigation().stop();
        }
        if (biggetje && getAge() > -12000) {
            setAge(-24000);
        }
        entityData.set(DATA_ONRUSTIG, StalQuest.onrustig(this));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new OntsnaptGoal());
    }

    /** A runaway does nothing but hide, look at its player and bolt. */
    class OntsnaptGoal extends Goal {
        OntsnaptGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isOntsnapt();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ontsnaptTick();
        }
    }

    /** One tick of a runaway (also called by the tests, which don't run AI goals by hand). */
    void ontsnaptTick() {
        if (!(level() instanceof ServerLevel level) || ontsnaptVan == null) {
            return;
        }
        if (!(level.getPlayerByUUID(ontsnaptVan) instanceof ServerPlayer speler) || !speler.isAlive() || speler.distanceToSqr(this) > 96 * 96
                || !StalQuest.zoekt(speler)) {
            discard();   // (its player is gone, far away or no longer looking: it comes back when they do)
            return;
        }
        if (vlucht > 0) {
            Vec3 doel = schuilplekken.get(schuil);
            vlucht++;
            if (position().distanceToSqr(doel) < 2.25) {
                klaarMetVluchten(speler);
            } else if (vlucht > VLUCHT_TICKS) {
                level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                snapTo(doel.x, doel.y, doel.z, getYRot(), 0f);
                level.sendParticles(ParticleTypes.POOF, doel.x, doel.y + 0.4, doel.z, 10, 0.3, 0.3, 0.3, 0.02);
                klaarMetVluchten(speler);
            } else if (vlucht % 10 == 2) {
                getNavigation().moveTo(doel.x, doel.y, doel.z, 1.7);
            }
            return;
        }
        getLookControl().setLookAt(speler, 30f, 30f);
        if (tickCount % 30 == 0) {
            // (a little note over its head, for its own player only: this is where the knorring comes from)
            level.sendParticles(speler, ParticleTypes.NOTE, true, true, getX(), getY() + 1.1, getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
        if (gevlucht < MAX_VLUCHT && schuilplekken.size() > 1 && speler.distanceToSqr(this) < SCHRIK * SCHRIK && !speler.isShiftKeyDown()
                && !speler.isSpectator()) {
            gevlucht++;
            schuil = (schuil + 1) % schuilplekken.size();
            vlucht = 1;
            playSound(PaleizenFeature.GIL.get(), 0.9f, 1.1f);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.paleizen.schrik").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private void klaarMetVluchten(ServerPlayer speler) {
        vlucht = 0;
        getNavigation().stop();
        if (gevlucht >= MAX_VLUCHT) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.paleizen.moe").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? WALK : isOnrustig() ? ONRUSTIG : IDLE)));
        controllers.add(new AnimationController<>("actie", 2, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"))
                .triggerableAnim("geluid", RawAnimation.begin().thenPlay("geluid")));
    }
}
