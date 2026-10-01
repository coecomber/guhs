package nl.juiced.guhs.feature.piep;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * Poepschilly: a green sea turtle plush from the guhzee coasts of the Guhmensie. It swims well and crawls on land. Always lief.
 * <p>
 * Tame it with zeewier (kelp, dried kelp, seagrass) or a kaasknabbel. Right-click it (empty hand) for its menu
 * ({@link PiepMenu}); sneak + right-click picks it up ({@link PiepDierItem}). The menu's big button "Kontje poetsen!" makes it
 * ready; then right-click one of your tamed guhs: the <b>poetsbeurt</b> ({@link Fase}):
 * <ol>
 *   <li>LOPEN: Poepschilly waddles to the guh's back (the kontje) (the guh waits);</li>
 *   <li>KRUIPEN: it shrinks and slides head first into the kontje, plop, gone;</li>
 *   <li>BINNEN: the guh wiggles and giggles, soap bubbles, sparkles and poetspluisjes pop out, scrub-scrub;</li>
 *   <li>UIT: Poepschilly pops out again (plop, a sparkle burst) and grows back to its size next to the guh;</li>
 * </ol>
 * then the guh is "Fris van binnen" for a few minutes ({@link PiepEffecten.FrisVanBinnen}), blushes happily (emote VERLEGEN:
 * hearts) and the owner reads "Kontje weer blinkend schoon, VAHOEG!". Poepschilly then rests for {@link #COOLDOWN} ticks.
 * <p>
 * Robust: the poetsbeurt is saved ("Poets": phase, tick, the guh's and the player's UUID), so after a chunk unload or a
 * server restart it goes on (the guh is looked up again) or, when the guh is gone (died, left, not loaded within
 * {@link #ZOEK_TICKS}), Poepschilly simply pops out on a safe spot where it is. It is never removed or copied, so it can't be
 * lost or doubled; while busy it can't be picked up.
 */
public class PoepschillyEntity extends TamableAnimal implements GeoEntity, PiepMaatje {
    public static final int TAME_CHANCE = 3;
    /** How long it is ready after you asked, and its rest afterwards. */
    public static final int KLAAR_TICKS = 20 * 20, COOLDOWN = 20 * 60 * 5;
    /** The phases of the poetsbeurt: at most this long waddling (then it hops), crawling in, cleaning, popping out. */
    public static final int LOOP_TICKS = 100, KRUIP_TICKS = 16, POETS_TICKS = 100, UIT_TICKS = 12;
    /** After loading, this long to find its guh again (then it pops out where it is). */
    public static final int ZOEK_TICKS = 100;
    private static final EntityDataAccessor<Boolean> DATA_BINNEN = SynchedEntityData.defineId(PoepschillyEntity.class, EntityDataSerializers.BOOLEAN);
    /** Its size (1 = normal; it shrinks into the kontje and grows back when it pops out). */
    private static final EntityDataAccessor<Float> DATA_KRIMP = SynchedEntityData.defineId(PoepschillyEntity.class, EntityDataSerializers.FLOAT);
    /** The menu settings that are off (PiepInstelling bits; saved as "PiepUit"). */
    private static final EntityDataAccessor<Integer> DATA_UIT = SynchedEntityData.defineId(PoepschillyEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public enum Fase { LOPEN, KRUIPEN, BINNEN, UIT }

    /** Ready for this player until {@link #klaarTot}. */
    @Nullable
    private UUID klaarVoor;
    private long klaarTot, rustTot;
    /** The poetsbeurt going on (null: none): its phase and the ticks in it, the guh (and its UUID) and the player who asked. */
    @Nullable
    private Fase fase;
    private int faseTick, zoekTicks;
    @Nullable
    private GuhEntity poetsGuh;
    @Nullable
    private UUID poetsGuhId, poetsSpeler;

    public PoepschillyEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1f, 0.5f, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
        this.setPathfindingMalus(PathType.WATER, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** Spawns on the guhzee coasts: at the water's surface, with land close by (spawn eggs and commands: anywhere). */
    public static boolean checkSpawn(LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos) {
        if (spawnType != EntitySpawnReason.NATURAL && spawnType != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        int sea = level instanceof Level l ? l.getSeaLevel() : 63;
        if (Math.abs(pos.getY() - sea) > 3 || !level.getFluidState(pos.below()).is(FluidTags.WATER) && !level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-5, -1, -5), pos.offset(5, 0, 5))) {
            if (level.getFluidState(p).isEmpty() && level.getBlockState(p).isSolid()) {
                return true;                                   // land nearby: the coast
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BINNEN, false);
        builder.define(DATA_KRIMP, 1f);
        builder.define(DATA_UIT, 0);
    }

    /** Inside a guh right now (hidden). */
    public boolean isBinnen() {
        return entityData.get(DATA_BINNEN);
    }

    /**
     * 2.10: its name floats above its head, like a guh's ("Schilly", "Poepschilly" or the name you gave it), but not while
     * it is inside a guh, crawling into or out of one (then it's smaller), or otherwise hidden.
     */
    @Override
    public boolean shouldShowName() {
        return !isBinnen() && !isInvisible() && krimp() >= 0.999f;
    }

    /** Its size now (1: normal; smaller while it crawls into or out of a kontje). */
    public float krimp() {
        return entityData.get(DATA_KRIMP);
    }

    private void setKrimp(float krimp) {
        if (Math.abs(krimp() - krimp) > 1e-3f) {
            entityData.set(DATA_KRIMP, krimp);
        }
    }

    void setBinnen(boolean binnen) {   // (package: the tests)
        entityData.set(DATA_BINNEN, binnen);
        setInvisible(binnen);
        setNoGravity(binnen);
        noPhysics = binnen;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new BlijfGoal(this));             // (rondvadsen uit: it stays put)
        goalSelector.addGoal(2, new TemptGoal(this, 1.2, this::isFood, false));
        goalSelector.addGoal(3, new VolgGoal());
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 30) {
            @Override
            public boolean canUse() {
                return aan(PiepInstelling.ZWEMMEN) && fase == null && super.canUse();
            }
        });
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8, 80) {
            @Override
            public boolean canUse() {
                return fase == null && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (isLocalInstanceAuthoritative() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    /** Follows its owner (over land and through water; vanilla's FollowOwnerGoal can't swim): too far away, it hops over. */
    class VolgGoal extends Goal {
        private int herberekenen;

        VolgGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = getOwner();
            return isTame() && owner != null && !owner.isSpectator() && !isOrderedToSit() && !isBinnen() && fase == null
                    && aan(PiepInstelling.VOLGEN) && distanceToSqr(owner) > 6 * 6;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = getOwner();
            return owner != null && !getNavigation().isDone() && fase == null && !isOrderedToSit() && aan(PiepInstelling.VOLGEN)
                    && distanceToSqr(owner) > 2.5 * 2.5;
        }

        @Override
        public void start() {
            herberekenen = 0;
        }

        @Override
        public void tick() {
            LivingEntity owner = getOwner();
            if (owner == null) {
                return;
            }
            getLookControl().setLookAt(owner, 10f, getMaxHeadXRot());
            if (--herberekenen > 0) {
                return;
            }
            herberekenen = adjustedTickDelay(10);
            if (distanceToSqr(owner) > 16 * 16) {
                BlockPos at = owner.blockPosition();
                for (int i = 0; i < 10; i++) {
                    BlockPos p = at.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                    if (level().getBlockState(p).getCollisionShape(level(), p).isEmpty()
                            && (!level().getBlockState(p.below()).getCollisionShape(level(), p.below()).isEmpty() || level().getFluidState(p).is(FluidTags.WATER))) {
                        snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, getYRot(), getXRot());
                        getNavigation().stop();
                        return;
                    }
                }
            } else {
                getNavigation().moveTo(owner, 1.3);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.KELP) || stack.is(Items.DRIED_KELP) || stack.is(Items.SEAGRASS) || stack.is(ModItems.KAAS_KNABBELS.get());
    }

    // --- clicks: taming, the menu, picking up ---------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean voer = isFood(stack);
        boolean leeg = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
        if (!voer && !leeg) {
            return super.mobInteract(player, hand);
        }
        if (fase != null || isBinnen()) {
            return InteractionResult.SUCCESS;     // busy with a kontje: leave it be
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        ServerLevel level = (ServerLevel) level();
        PiepVoortgang.pagina(sp, soort());
        if (voer) {
            stack.consume(1, player);
            playSound(SoundEvents.TURTLE_AMBIENT_LAND, 1f, 1.3f);
            if (!isTame()) {
                if ((player.hasEffect(PiepFeature.LIEF_KIJKEN) || random.nextInt(TAME_CHANCE) == 0)
                        && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    temmen(sp);
                } else {
                    level.broadcastEntityEvent(this, (byte) 6);
                }
            } else {
                heal(4);
                level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.4, getZ(), 2, 0.2, 0.1, 0.2, 0);
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player) && player.isSecondaryUseActive()) {
            PiepDierItem.pakOp(this, sp);                               // sneak + empty hand (owner): picked up
            return InteractionResult.SUCCESS;
        }
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.4, getZ(), 1, 0.2, 0.1, 0.2, 0);
        triggerAnim("actie", "blij");
        if (isTame() && isOwnedBy(player)) {
            PiepMenu.open(sp, this);                                    // empty hand (owner): its menu
        } else if (isTame() && player.isSecondaryUseActive()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.niet_jouw_maatje").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    public void temmen(ServerPlayer player) {
        tame(player);
        getNavigation().stop();
        level().broadcastEntityEvent(this, (byte) 7);
        triggerAnim("actie", "blij");
        player.sendOverlayMessage(Component.translatable("gui.guhs.piep." + soort() + "_getamed").withStyle(ChatFormatting.LIGHT_PURPLE));
        PiepVoortgang.tel(player, "piep." + soort() + "_getamed", 1, adv() + "_getamed");
        PiepVoortgang.pagina(player, soort());
    }

    /** What kind of turtle: "poepschilly" (the kontpoetser) or "schilly" (the minihoofdje-bestie, {@link SchillyEntity}). */
    @Override
    public String soort() {
        return "poepschilly";
    }

    /** The start of its quest advancements (guhs:quest/&lt;adv&gt;_gevonden, _getamed). */
    protected String adv() {
        return "piep_schilly";
    }

    // --- the menu (PiepMaatje) ----------------------------------------------------------------------------------------------

    @Override
    public TamableAnimal dier() {
        return this;
    }

    @Override
    public List<PiepInstelling> instellingen() {
        return PiepInstelling.POEPSCHILLY;
    }

    @Override
    public Item oppakItem() {
        return PiepFeature.POEPSCHILLY_ITEM.get();
    }

    @Override
    public int uitVlaggen() {
        return entityData.get(DATA_UIT);
    }

    @Override
    public void setUitVlaggen(int vlaggen) {
        entityData.set(DATA_UIT, vlaggen);
        setPathfindingMalus(PathType.WATER, aan(PiepInstelling.ZWEMMEN) ? 0.0f : 8.0f);   // (zwemmen uit: rather on land)
    }

    @Override
    public boolean isBezig() {
        return fase != null || isBinnen();
    }

    @Override
    public int rustSeconden() {
        return (int) Math.max(0, (rustTot - level().getGameTime() + 19) / 20);
    }

    /** "Kontje poetsen!" (Schilly: "Bestie-moment!"): ready for one of your guhs for {@link #KLAAR_TICKS}, unless it rests. */
    @Override
    public void speciaal(ServerPlayer player) {
        if (level().getGameTime() < rustTot) {
            long sec = (rustTot - level().getGameTime()) / 20;
            player.sendOverlayMessage(Component.translatable("gui.guhs.piep." + soort() + "_rust", sec / 60, String.format(Locale.ROOT, "%02d", sec % 60))
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        klaarVoor = player.getUUID();
        klaarTot = level().getGameTime() + KLAAR_TICKS;
        triggerAnim("actie", "blij");
        playSound(SoundEvents.TURTLE_AMBIENT_LAND, 1f, 1.5f);
        ModNetworking.sendTo(player, new PiepPayloads.Klaar(getId(), KLAAR_TICKS));
        player.sendOverlayMessage(Component.translatable("gui.guhs.piep." + soort() + "_klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Rests this many ticks (no poetsbeurt or bestie-moment). */
    protected void rust(int ticks) {
        rustTot = level().getGameTime() + ticks;
    }

    /** Is it ready (asked by this player a moment ago) and free? */
    public boolean klaarVoor(Player player) {
        return isTame() && player.getUUID().equals(klaarVoor) && level().getGameTime() <= klaarTot && fase == null && !isBinnen();
    }

    /** One of this player's turtles that is ready for a guh (nearby), or null. */
    @Nullable
    static PoepschillyEntity klaarVoorSpeler(Player player) {
        for (PoepschillyEntity s : player.level().getEntitiesOfClass(PoepschillyEntity.class, player.getBoundingBox().inflate(24))) {
            if (s.klaarVoor(player)) {
                return s;
            }
        }
        return null;
    }

    /**
     * (GuhHooks.klik) Right-click a guh while one of your turtles is ready: Poepschilly's poetsbeurt, or Schilly's bestie-moment
     * ({@link #opGuh}). This vanilla click only reaches the server for guhs that are not your own tamed ones (wild guhs: "only
     * tamed guhs"); for your own guhs the client sends guhs:piep_op_guh instead ({@link #opGuhGeklikt}).
     */
    static InteractionResult klikOpGuh(GuhEntity guh, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        PoepschillyEntity schilly = klaarVoorSpeler(player);
        if (schilly == null) {
            return InteractionResult.PASS;
        }
        if (guh.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        schilly.klaarVoor = null;
        schilly.opGuh(guh, (ServerPlayer) player);
        return InteractionResult.SUCCESS;
    }

    /**
     * (guhs:piep_op_guh) The client says: I right-clicked this guh with an empty hand while my turtle was ready. The guh's own
     * tap/hold handler (client.GuhInteractHandler) swallows empty-hand clicks on your own tamed guh and only sends "tap", so
     * the poetsbeurt never reached {@link #klikOpGuh}: that was the bug ("on my own guh nothing happens"). When no turtle is
     * ready after all (it just ran out), the click does what it would have done: the guh's tap (a pet, or riding).
     */
    public static boolean opGuhGeklikt(GuhEntity guh, ServerPlayer player) {
        double bereik = 8.0 + guh.getBbWidth() * 2;
        if (!guh.isAlive() || guh.level() != player.level() || player.distanceToSqr(guh) > bereik * bereik
                || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        PoepschillyEntity schilly = klaarVoorSpeler(player);
        if (schilly == null) {
            if (guh.isTame() && guh.isOwnedBy(player)) {
                guh.onOwnerTap(player);
            }
            return false;
        }
        schilly.klaarVoor = null;
        schilly.opGuh(guh, player);
        return true;
    }

    /** Asked, and then the player clicked this guh: the poetsbeurt (Schilly: a bestie-moment). */
    protected void opGuh(GuhEntity guh, ServerPlayer sp) {
        ModNetworking.sendTo(sp, new PiepPayloads.Klaar(getId(), 0));
        if (!guh.isTame()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.schilly_alleen_tam").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (guh.hasEffect(PiepFeature.FRIS_VAN_BINNEN)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.al_fris", guh.getDisplayName()).withStyle(ChatFormatting.GRAY));
            return;
        }
        startPoets(guh, sp);
    }

    // --- the poetsbeurt ------------------------------------------------------------------------------------------------------

    /** Starts the poetsbeurt on this guh (the tests call it directly). */
    public void startPoets(GuhEntity guh, @Nullable ServerPlayer player) {
        poetsGuh = guh;
        poetsGuhId = guh.getUUID();
        poetsSpeler = player == null ? null : player.getUUID();
        fase = Fase.LOPEN;
        faseTick = 0;
        zoekTicks = 0;
        klaarVoor = null;
        GuhHooks.bezig(guh, LOOP_TICKS + KRUIP_TICKS + POETS_TICKS + UIT_TICKS + 20);   // (the day rhythm leaves it alone)
        guh.getNavigation().stop();
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.piep.schilly_waggelt", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public boolean isAanHetPoetsen() {
        return fase != null;
    }

    @Nullable
    public Fase fase() {
        return fase;
    }

    private void poetsTick() {
        ServerLevel level = (ServerLevel) level();
        if (poetsGuh == null) {
            // (loaded from disk, or the guh was not there yet) find it again; if it doesn't come, pop out here
            Entity e = poetsGuhId == null ? null : level.getEntity(poetsGuhId);
            if (e instanceof GuhEntity g && g.isAlive()) {
                poetsGuh = g;
                if (fase == Fase.BINNEN) {
                    PiepPayloads.naarKijkers(g, new PiepPayloads.Wiebel(g.getId(), Math.max(10, POETS_TICKS - faseTick)));
                }
            } else {
                if (++zoekTicks > ZOEK_TICKS || poetsGuhId == null) {
                    stopPoets(false);
                } else if (isBinnen()) {
                    setDeltaMovement(Vec3.ZERO);
                }
                return;
            }
        }
        GuhEntity guh = poetsGuh;
        if (!guh.isAlive() || guh.isRemoved() || guh.level() != level) {
            stopPoets(false);                                     // the guh died, left or was put away: out we come
            return;
        }
        faseTick++;
        guh.getNavigation().stop();
        switch (fase) {
            case LOPEN -> lopen(level, guh);
            case KRUIPEN -> kruipen(level, guh);
            case BINNEN -> binnen(level, guh);
            case UIT -> uit(guh);
        }
    }

    /** Waddles to the guh's back; too slow or too far: a big turtle-hop. */
    private void lopen(ServerLevel level, GuhEntity guh) {
        Vec3 doel = achter(guh, 0.45);
        getLookControl().setLookAt(guh);
        double dx = doel.x - getX(), dz = doel.z - getZ();
        boolean daar = dx * dx + dz * dz < 0.45 * 0.45 && Math.abs(doel.y - getY()) < 1.0;
        if (!daar && (faseTick >= LOOP_TICKS || distanceToSqr(guh) > 20 * 20)) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 4, 0.15, 0.1, 0.15, 0.01);
            snapTo(doel.x, guh.getY(), doel.z, getYRot(), 0);
            daar = true;
        }
        if (daar) {
            getNavigation().stop();
            fase = Fase.KRUIPEN;
            faseTick = 0;
            triggerAnim("actie", "kruip");
            level.playSound(null, blockPosition(), SoundEvents.TURTLE_SHAMBLE, SoundSource.NEUTRAL, 1f, 1.4f);
            return;
        }
        if (faseTick % 10 == 1 || getNavigation().isDone()) {
            getNavigation().moveTo(doel.x, doel.y, doel.z, 1.3);
        }
    }

    /** Head first into the kontje: it slides in and shrinks; at the end, plop, it is inside. */
    private void kruipen(ServerLevel level, GuhEntity guh) {
        getNavigation().stop();
        float t = Mth.clamp(faseTick / (float) KRUIP_TICKS, 0f, 1f);
        Vec3 nu = achter(guh, 0.45).lerp(achter(guh, -0.05), t);
        float yaw = guh.yBodyRot;                                  // (facing the same way as the guh: nose into the kontje)
        snapTo(nu.x, guh.getY() + guh.getBbHeight() * 0.25 * t, nu.z, yaw, 0);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        setDeltaMovement(Vec3.ZERO);
        setKrimp(1f - 0.85f * t);
        if (faseTick % 4 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, nu.x, getY() + 0.2, nu.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        if (faseTick < KRUIP_TICKS) {
            return;
        }
        setBinnen(true);
        setKrimp(0.15f);
        fase = Fase.BINNEN;
        faseTick = 0;
        level.playSound(null, guh.blockPosition(), PiepFeature.SCHILLY_PLOP.get(), SoundSource.NEUTRAL, 1f, 0.7f);
        level.playSound(null, guh.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.4f);
        PiepPayloads.naarKijkers(guh, new PiepPayloads.Wiebel(guh.getId(), POETS_TICKS));
        guh.triggerAnim("action", "happy");
        ServerPlayer speler = speler(level);
        if (speler != null) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.piep.schilly_kruipt", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Inside: it rides along with the guh; the guh wiggles and giggles, bubbles, sparkles and poetspluisjes pop out. */
    private void binnen(ServerLevel level, GuhEntity guh) {
        snapTo(guh.getX(), guh.getY() + 0.2, guh.getZ(), guh.getYRot(), 0);
        setDeltaMovement(Vec3.ZERO);
        Vec3 kont = achter(guh, 0.05);
        double hoog = guh.getY() + guh.getBbHeight() * 0.35;
        if (faseTick % 4 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, kont.x, hoog, kont.z, 3, 0.18, 0.15, 0.18, 0.03);
            level.sendParticles(PiepFeature.FRIS_SPARKEL.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.6, guh.getZ(), 2,
                    guh.getBbWidth() * 0.4, guh.getBbHeight() * 0.3, guh.getBbWidth() * 0.4, 0);
        }
        if (faseTick % 7 == 0) {                                   // poetspluisjes: little white cleaning puffs from the kontje
            Vec3 weg = kont.subtract(guh.position()).normalize().scale(0.06);
            level.sendParticles(ParticleTypes.CLOUD, kont.x, hoog, kont.z, 0, weg.x, 0.03, weg.z, 1.0);
            level.sendParticles(ParticleTypes.SPLASH, kont.x, hoog, kont.z, 4, 0.15, 0.1, 0.15, 0.1);
        }
        if (faseTick % 20 == 0) {
            level.playSound(null, guh.blockPosition(), PiepFeature.SCHILLY_POETS.get(), SoundSource.NEUTRAL, 0.9f, 0.9f + random.nextFloat() * 0.3f);
        }
        if (faseTick % 30 == 15) {
            guh.triggerAnim("action", "happy");                    // it tickles! (giggle)
            level.playSound(null, guh.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f,
                    1.3f + random.nextFloat() * 0.3f);
        }
        if (faseTick >= POETS_TICKS) {
            fase = Fase.UIT;
            faseTick = 0;
            uitkomen(level, guh);
        }
    }

    /** Plop! Out of the kontje, onto a safe spot behind the guh. */
    private void uitkomen(ServerLevel level, GuhEntity guh) {
        setBinnen(false);
        setKrimp(0.25f);
        Vec3 plek = veiligePlek(achter(guh, 0.35), guh.position());
        snapTo(plek.x, plek.y, plek.z, guh.yBodyRot + 180f, 0);
        Vec3 weg = new Vec3(plek.x - guh.getX(), 0, plek.z - guh.getZ());
        setDeltaMovement((weg.lengthSqr() > 1e-4 ? weg.normalize().scale(0.12) : Vec3.ZERO).add(0, 0.25, 0));
        level.playSound(null, blockPosition(), PiepFeature.SCHILLY_PLOP.get(), SoundSource.NEUTRAL, 1.1f, 1.2f);
        level.sendParticles(PiepFeature.FRIS_SPARKEL.get(), getX(), getY() + 0.3, getZ(), 16, 0.35, 0.3, 0.35, 0);
        level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.3, getZ(), 8, 0.25, 0.2, 0.25, 0.05);
    }

    /** Growing back to its own size; then the guh is fris van binnen. */
    private void uit(GuhEntity guh) {
        float t = Mth.clamp(faseTick / (float) UIT_TICKS, 0f, 1f);
        setKrimp(0.25f + 0.75f * t);
        if (faseTick >= UIT_TICKS) {
            stopPoets(true);
        }
    }

    /** A spot just behind the guh (where Poepschilly goes in and comes out: that's where the kontje is). */
    private static Vec3 achter(GuhEntity guh, double extra) {
        float yaw = guh.yBodyRot * Mth.DEG_TO_RAD;
        double d = guh.getBbWidth() * 0.5 + extra;
        return new Vec3(guh.getX() + Mth.sin(yaw) * d, guh.getY(), guh.getZ() - Mth.cos(yaw) * d);
    }

    /** A spot near {@code bij} where Poepschilly fits (not in a wall), else near {@code anders}, else on top of the ground there. */
    Vec3 veiligePlek(Vec3 bij, Vec3 anders) {
        for (Vec3 basis : new Vec3[] {bij, anders, position()}) {
            for (int r = 0; r <= 2; r++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                                continue;
                            }
                            Vec3 v = r == 0 ? basis.add(0, dy, 0) : new Vec3(Math.floor(basis.x + dx) + 0.5, basis.y + dy, Math.floor(basis.z + dz) + 0.5);
                            if (level().noCollision(this, getType().getDimensions().makeBoundingBox(v))) {
                                return v;
                            }
                        }
                    }
                }
            }
        }
        BlockPos top = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(anders));
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    @Nullable
    private ServerPlayer speler(ServerLevel level) {
        return poetsSpeler == null ? null : level.getServer().getPlayerList().getPlayer(poetsSpeler);
    }

    /** The end of the poetsbeurt: done (the guh is fris van binnen) or broken off (it just pops out, safely). */
    private void stopPoets(boolean klaar) {
        ServerLevel level = (ServerLevel) level();
        GuhEntity guh = poetsGuh;
        boolean wasBinnen = isBinnen();
        setBinnen(false);
        setKrimp(1f);
        fase = null;
        faseTick = 0;
        zoekTicks = 0;
        poetsGuh = null;
        poetsGuhId = null;
        ServerPlayer speler = speler(level);
        poetsSpeler = null;
        if (guh != null) {
            GuhHooks.bezig(guh, 0);
        }
        if (!klaar) {
            if (wasBinnen) {
                Vec3 plek = guh != null && guh.isAlive() && guh.level() == level ? veiligePlek(achter(guh, 0.35), guh.position())
                        : veiligePlek(position(), position());
                snapTo(plek.x, plek.y, plek.z, getYRot(), 0);
                setDeltaMovement(Vec3.ZERO);
                level.playSound(null, blockPosition(), PiepFeature.SCHILLY_PLOP.get(), SoundSource.NEUTRAL, 1f, 1.2f);
                level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.3, getZ(), 6, 0.25, 0.2, 0.25, 0.05);
            }
            if (speler != null) {
                speler.sendOverlayMessage(Component.translatable("gui.guhs.piep.schilly_afgebroken").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (guh == null) {
            return;
        }
        guh.addEffect(new MobEffectInstance(PiepFeature.FRIS_VAN_BINNEN, PiepEffecten.FRIS_TICKS, 0));
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 5, 0.3, 0.2, 0.3, 0);
        level.sendParticles(PiepFeature.FRIS_SPARKEL.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.6, guh.getZ(), 14, 0.4, 0.3, 0.4, 0.02);
        level.playSound(null, guh.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        if (!guh.emotes.start(nl.juiced.guhs.feature.emotes.Emote.VERLEGEN, false, nl.juiced.guhs.feature.emotes.GuhEmotes.Source.SELF)) {
            guh.triggerAnim("action", "happy");                    // (blushing, hearts: or at least a happy hop)
        }
        triggerAnim("actie", "blij");
        rust(COOLDOWN);
        if (speler != null) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.piep.fris", guh.getDisplayName()).withStyle(ChatFormatting.AQUA));
            PiepVoortgang.tel(speler, PiepVoortgang.POETSBEURTEN, 1, "piep_poetsbeurt");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (fase != null) {
            poetsTick();
        } else if (isBinnen()) {
            stopPoets(false);                                       // (never stays hidden without a poetsbeurt)
        }
        if ((tickCount + getId()) % 20 == 0) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(5))) {
                if (p instanceof ServerPlayer sp) {
                    nl.juiced.guhs.quest.GuhAdvancements.grant(sp, adv() + "_gevonden");
                    PiepVoortgang.pagina(sp, soort());
                }
            }
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (isBinnen() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return !isBinnen() && super.isPushable();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBinnen() ? null : isInWater() ? SoundEvents.TURTLE_AMBIENT_LAND : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.4f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("RustTot", rustTot);
        tag.putInt("PiepUit", uitVlaggen());
        if (fase != null) {
            CompoundTag poets = new CompoundTag();
            poets.putString("Fase", fase.name());
            poets.putInt("Tick", faseTick);
            if (poetsGuhId != null) {
                poets.store("Guh", UUIDUtil.CODEC, poetsGuhId);
            }
            if (poetsSpeler != null) {
                poets.store("Speler", UUIDUtil.CODEC, poetsSpeler);
            }
            tag.store("Poets", CompoundTag.CODEC, poets);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        rustTot = tag.getLongOr("RustTot", 0L);
        setUitVlaggen(tag.getIntOr("PiepUit", 0));
        fase = null;
        poetsGuh = null;
        poetsGuhId = null;
        poetsSpeler = null;
        zoekTicks = 0;
        faseTick = 0;
        CompoundTag poets = tag.read("Poets", CompoundTag.CODEC).orElse(null);
        if (poets != null) {
            try {
                fase = Fase.valueOf(poets.getStringOr("Fase", ""));
            } catch (IllegalArgumentException e) {
                fase = null;
            }
            faseTick = poets.getIntOr("Tick", 0);
            poetsGuhId = poets.read("Guh", UUIDUtil.CODEC).isPresent() ? poets.read("Guh", UUIDUtil.CODEC).orElseThrow() : null;
            poetsSpeler = poets.read("Speler", UUIDUtil.CODEC).isPresent() ? poets.read("Speler", UUIDUtil.CODEC).orElseThrow() : null;
        }
        // hidden only while it is really inside a guh (this also frees a Poepschilly that was saved invisible by an older version)
        setBinnen(fase == Fase.BINNEN);
        setKrimp(fase == Fase.BINNEN ? 0.15f : 1f);
    }

    /** (Tests) no rest. */
    void vergeetRust() {
        rustTot = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(
                isInWater() ? SWIM : state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("kruip", RawAnimation.begin().thenPlay("kruip")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
