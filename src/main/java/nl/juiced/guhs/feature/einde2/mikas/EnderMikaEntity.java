package nl.juiced.guhs.feature.einde2.mikas;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.GuheindeReis;
import nl.juiced.guhs.quest.GuhAdvancements;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

/**
 * De Ender-Mika (the enderman of the Guheinde): a Mika that stood in the void so long that it got stretched, tall and thin
 * and purple-black (the tall Mika shape of the build contract; tools/features/einde2_mikas_modellen.py).
 * <ul>
 *   <li><b>Shy.</b> Look at it for half a second and it flits away ({@link #flitsWeg}).</li>
 *   <li><b>A knabbel thief.</b> Hold a knabbel ({@link GuheindeReis#isKnabbel}) where it can see you and it sneaks up behind
 *       you (it only comes while you do not look), takes exactly ONE ({@link #jat}) and flits off, holding it up. It drops
 *       it again when it is hit or shoved, when someone looks at it for two seconds, or after twenty seconds
 *       ({@link #laatVallen}); if you walk off, change dimension or it would go away, the knabbel goes straight back
 *       into your pockets ({@link #geefTerug}). While it holds one it never despawns. It never takes anything else.</li>
 *   <li><b>A Mika.</b> Hit it and it shoves you (a push, never damage: {@link MikaEntity#doHurtTarget}) and flits away.</li>
 *   <li><b>In love with Kaasmijten.</b> With a {@link KaasmijtEntity} around it forgets about knabbels and trails after it.</li>
 *   <li><b>Handelaar</b> (NBT {@code Handelaar: 1b}, {@link #setHandelaar}): stands still, never teleports or steals, can't
 *       be hurt. These are the traders of the Leegtehaven.</li>
 * </ul>
 */
public class EnderMikaEntity extends MikaEntity {
    private static final EntityDataAccessor<Boolean> HANDELAAR = SynchedEntityData.defineId(EnderMikaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> STAND = SynchedEntityData.defineId(EnderMikaEntity.class, EntityDataSerializers.BYTE);
    /** What it is up to (synced, for its pose): nothing special, holding its loot, or caught with it. */
    public static final byte GEWOON = 0, DRAAGT = 1, BETRAPT = 2;

    public static final float HEALTH = 30f;
    /** How long it holds a knabbel before it gives up by itself (ticks). */
    public static final int DRAAG_TIJD = 400;
    /** Look at a thief this long and it drops its loot (ticks). */
    public static final int KIJK_TIJD = 40;
    /** Look at one this long and it flits away (ticks). */
    public static final int VERLEGEN_TIJD = 10;
    /** Ticks before the same Ender-Mika steals again, and before anyone steals from the same player again. */
    public static final int JAT_PAUZE = 1200, SPELER_PAUZE = 600;
    /** From how far it spots a knabbel in a hand, and how near it must be to take it (blocks). */
    public static final double JAT_STRAAL = 10.0, GRIJP_AFSTAND = 2.5;
    /** The victim further away than this (or in another dimension): the knabbel goes back into their pockets. */
    public static final double TERUG_AFSTAND = 40.0;
    /** From how far it notices a Kaasmijt. */
    public static final double MIJT_STRAAL = 12.0;
    /** A look only counts within this many blocks. */
    public static final double KIJK_STRAAL = 32.0;
    /** Ticks between two flits. */
    public static final int FLITS_PAUZE = 60;
    /** Natural spawning keeps at most this many within 64 blocks of each other. */
    public static final int MAX_NABIJ = 2;

    /**
     * Players an Ender-Mika never steals from (it still flits away from their look).
     * hook fase B: the merge onto the sister update adds the bearer of a Knabbelring ({@code Ring.onzichtbaarVoorMikas(p)}).
     */
    public static final List<Predicate<Player>> LAAT_MET_RUST = new CopyOnWriteArrayList<>();
    /** When each player was last robbed (game time): a pair of Ender-Mika's does not pick anyone clean. */
    private static final Map<UUID, Long> BESTOLEN = new ConcurrentHashMap<>();

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DRAAG = RawAnimation.begin().thenLoop("draag");
    private static final RawAnimation BETRAPT_ANIM = RawAnimation.begin().thenLoop("betrapt");
    private static final RawAnimation JAT = RawAnimation.begin().thenPlay("jat");
    private static final RawAnimation FLITS = RawAnimation.begin().thenPlay("flits");
    private static final RawAnimation DUW = RawAnimation.begin().thenPlay("duw");

    private final Honk honk = new Honk();
    @Nullable
    private UUID slachtoffer;
    private int draagTijd;
    private int kijkTijd;
    private int flitsPauze;
    private int jatPauze;
    private int zoekMijt;
    @Nullable
    private KaasmijtEntity mijt;

    public EnderMikaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        this.setCustomName(Component.translatable("entity.guhs.ender_mika"));
        setDropChance(EquipmentSlot.MAINHAND, 0f);      // (what it holds is somebody's knabbel: this class gives it back)
    }

    public static AttributeSupplier.Builder createEnderAttributes() {
        return MikaEntity.createAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** On the ground of the outer islands, in any light, never many together (the biome modifier brings it: weight 2). */
    public static boolean checkEnderSpawnRules(EntityType<EnderMikaEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
                                               RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.STRUCTURE) {
            return true;
        }
        return level.getBlockState(pos.below()).is(Einde2Ids.EILANDGROND) && random.nextInt(4) == 0
                && level.getEntitiesOfClass(EnderMikaEntity.class, new AABB(pos).inflate(64)).size() < MAX_NABIJ;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HANDELAAR, false);
        builder.define(STAND, GEWOON);
    }

    /** (not the Mika's goals: this one does not come after you to shove, it comes for your knabbel) */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoudVastGoal());
        this.goalSelector.addGoal(2, new VolgMijtGoal());
        this.goalSelector.addGoal(3, new JatGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !isHandelaar() && !draagtKnabbel() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** (a Mika gets a random size when it spawns: the tall one keeps its own) */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        this.getAttribute(Attributes.SCALE).setBaseValue(1.0);
        this.refreshDimensions();
        return result;
    }

    // =================================================================================================================
    // the trader of the Leegtehaven
    // =================================================================================================================

    public boolean isHandelaar() {
        return this.entityData.get(HANDELAAR);
    }

    /**
     * Makes this one a trader (or an ordinary one again): it stays where it is, never teleports or steals and can't be hurt.
     * hook fase B: the trade itself (the Leegtehaven's daily offer) hangs on these.
     */
    public void setHandelaar(boolean handelaar) {
        this.entityData.set(HANDELAAR, handelaar);
        if (handelaar && !level().isClientSide()) {
            geefTerug();
            setPersistenceRequired();
            getNavigation().stop();
            this.entityData.set(STAND, GEWOON);
        }
    }

    /** {@link #GEWOON}, {@link #DRAAGT} or {@link #BETRAPT}. */
    public byte stand() {
        return this.entityData.get(STAND);
    }

    // =================================================================================================================
    // the stolen knabbel
    // =================================================================================================================

    /** Is it holding a knabbel it took from someone? */
    public boolean draagtKnabbel() {
        return slachtoffer != null && !getMainHandItem().isEmpty();
    }

    /** Whose knabbel it holds (null: nobody's). */
    @Nullable
    public UUID slachtoffer() {
        return draagtKnabbel() ? slachtoffer : null;
    }

    /** May it steal from this player right now? */
    public boolean magJatten(Player player) {
        if (isHandelaar() || draagtKnabbel() || jatPauze > 0 || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (!GuheindeReis.isKnabbel(player.getMainHandItem()) && !GuheindeReis.isKnabbel(player.getOffhandItem())) {
            return false;
        }
        Long laatst = BESTOLEN.get(player.getUUID());
        if (laatst != null && level().getGameTime() - laatst < SPELER_PAUZE && level().getGameTime() >= laatst) {
            return false;
        }
        for (Predicate<Player> rust : LAAT_MET_RUST) {
            if (rust.test(player)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes ONE knabbel out of this player's hand (the main hand first) and holds it. Nothing else is ever taken.
     *
     * @return true when it now holds the knabbel
     */
    public boolean jat(ServerPlayer player) {
        if (!magJatten(player) || level().isClientSide()) {
            return false;
        }
        InteractionHand hand = GuheindeReis.isKnabbel(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack een = player.getItemInHand(hand).split(1);
        if (een.isEmpty()) {
            return false;
        }
        setItemSlot(EquipmentSlot.MAINHAND, een);
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        slachtoffer = player.getUUID();
        draagTijd = 0;
        kijkTijd = 0;
        jatPauze = JAT_PAUZE;
        BESTOLEN.put(player.getUUID(), level().getGameTime());
        this.entityData.set(STAND, DRAAGT);
        triggerAnim("action", "jat");
        playSound(E2MikasFeature.ENDER_JAT.get(), 1f, getVoicePitch());
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.gejat").withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(player, "einde2_mikas_gejat");
        return true;
    }

    private ItemStack neemBuit() {
        ItemStack buit = getMainHandItem().copy();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        slachtoffer = null;
        draagTijd = 0;
        kijkTijd = 0;
        this.entityData.set(STAND, GEWOON);
        return buit;
    }

    @Nullable
    private ServerPlayer slachtofferSpeler() {
        return slachtoffer != null && level() instanceof ServerLevel server ? server.getServer().getPlayerList().getPlayer(slachtoffer) : null;
    }

    /** Drops the knabbel it holds at its own feet (it lies still: nothing rolls off an island). */
    public void laatVallen() {
        if (!draagtKnabbel() || !(level() instanceof ServerLevel server)) {
            return;
        }
        ServerPlayer wie = slachtofferSpeler();
        ItemStack buit = neemBuit();
        ItemEntity item = new ItemEntity(server, getX(), getY() + 0.4, getZ(), buit);
        item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(10);
        server.addFreshEntity(item);
        playSound(E2MikasFeature.ENDER_BETRAPT.get(), 0.9f, getVoicePitch());
        if (wie != null && wie.level() == level() && wie.distanceToSqr(this) < KIJK_STRAAL * KIJK_STRAAL) {
            wie.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.laat_vallen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * Gives the knabbel back for sure: into the pockets of whoever it took it from ({@link Minigames#give}); when that
     * player is not on the server it is laid down at the Ender-Mika's feet.
     */
    public void geefTerug() {
        if (!draagtKnabbel() || level().isClientSide()) {
            return;
        }
        ServerPlayer wie = slachtofferSpeler();
        if (wie == null) {
            laatVallen();
            return;
        }
        Minigames.give(wie, neemBuit());
        wie.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.terug").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =================================================================================================================
    // every tick: shy, and what happens to its loot
    // =================================================================================================================

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        honk.tick(this);
        if (flitsPauze > 0) {
            flitsPauze--;
        }
        if (jatPauze > 0) {
            jatPauze--;
        }
        if (isHandelaar()) {
            return;
        }
        if (draagtKnabbel()) {
            metBuit();
        } else {
            if (stand() != GEWOON) {
                this.entityData.set(STAND, GEWOON);
            }
            Player kijker = kijker();
            if (kijker == null) {
                kijkTijd = 0;
            } else if (++kijkTijd >= VERLEGEN_TIJD && flitsPauze <= 0) {
                kijkTijd = 0;
                flitsWeg(kijker);
            }
        }
    }

    private void metBuit() {
        draagTijd++;
        ServerPlayer wie = slachtofferSpeler();
        if (wie == null) {
            return;         // (they logged off: it keeps the knabbel safe until they are back; it does not despawn meanwhile)
        }
        if (wie.level() != level() || !wie.isAlive() || wie.distanceToSqr(this) > TERUG_AFSTAND * TERUG_AFSTAND) {
            geefTerug();    // (before it could unload or be left behind)
            return;
        }
        if (kijker() != null) {
            if (stand() != BETRAPT) {
                this.entityData.set(STAND, BETRAPT);
                getNavigation().stop();
                playSound(E2MikasFeature.ENDER_BETRAPT.get(), 0.7f, getVoicePitch() + 0.2f);
            }
            if (++kijkTijd >= KIJK_TIJD) {
                laatVallen();
                flitsPauze = 0;
                flitsWeg(wie);
                return;
            }
        } else {
            kijkTijd = Math.max(0, kijkTijd - 1);
            if (stand() != DRAAGT) {
                this.entityData.set(STAND, DRAAGT);
            }
        }
        if (draagTijd >= DRAAG_TIJD) {
            laatVallen();
        }
    }

    /** Does this player look at it (at its face, its chest or its knees; never through a wall)? */
    public boolean bekekenDoor(Player player) {
        if (player.isSpectator() || !player.isAlive() || player.level() != level() || player.distanceToSqr(this) > KIJK_STRAAL * KIJK_STRAAL) {
            return false;
        }
        return isLookingAtMe(player, 0.1, true, false, getEyeY(), getY() + getBbHeight() * 0.55, getY() + getBbHeight() * 0.2);
    }

    /** The (nearest) player that looks at it right now, or null. */
    @Nullable
    public Player kijker() {
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player player : level().players()) {
            double d = player.distanceToSqr(this);
            if (d < bestD && bekekenDoor(player)) {
                best = player;
                bestD = d;
            }
        }
        return best;
    }

    // =================================================================================================================
    // flitting
    // =================================================================================================================

    /**
     * Teleports onto the ground under this spot (never into the void, a fluid or a wall, never out of its honk, never more
     * than 12 blocks down), with sparks and a squeak at both ends.
     */
    public boolean flits(double x, double y, double z) {
        if (isHandelaar() || !isAlive() || !(level() instanceof ServerLevel server)) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (!server.hasChunkAt(pos)) {
            return false;
        }
        int diep = 0;
        while (pos.getY() > server.getMinY() && diep <= 12 && !server.getBlockState(pos).blocksMotion()) {
            pos.move(Direction.DOWN);
            diep++;
        }
        BlockState grond = server.getBlockState(pos);
        if (!grond.blocksMotion() || !server.getFluidState(pos.above()).isEmpty() || hasHome() && !isWithinHome(pos.above())) {
            return false;
        }
        Vec3 oud = position();
        if (!randomTeleport(x, pos.getY() + 1.0, z, false)) {
            return false;
        }
        getNavigation().stop();
        vonken(server, oud);
        vonken(server, position());
        server.playSound(null, oud.x, oud.y + 1.5, oud.z, E2MikasFeature.ENDER_FLITS.get(), getSoundSource(), 0.8f, 1.2f);
        playSound(E2MikasFeature.ENDER_FLITS.get(), 0.8f, 1.4f);
        triggerAnim("action", "flits");
        return true;
    }

    private void vonken(ServerLevel level, Vec3 waar) {
        level.sendParticles(E2MikasFeature.FLITS.get(), waar.x, waar.y + getBbHeight() * 0.5, waar.z, 24, 0.25, getBbHeight() * 0.3, 0.25, 0.04);
        level.sendParticles(ParticleTypes.PORTAL, waar.x, waar.y + getBbHeight() * 0.5, waar.z, 12, 0.2, getBbHeight() * 0.3, 0.2, 0.3);
    }

    /** Flits 8-16 blocks away (away from {@code van} when it can). False when it found no place to go. */
    public boolean flitsWeg(@Nullable Entity van) {
        if (isHandelaar() || level().isClientSide()) {
            return false;
        }
        flitsPauze = FLITS_PAUZE;
        double weg = van == null ? 0 : Math.atan2(getZ() - van.getZ(), getX() - van.getX());
        for (int i = 0; i < 24; i++) {
            double hoek = van != null && i < 14 ? weg + (random.nextDouble() - 0.5) * Math.PI * 0.9 : random.nextDouble() * Math.PI * 2;
            double r = 8.0 + random.nextDouble() * 8.0;
            if (flits(getX() + Math.cos(hoek) * r, getY() + 4.0, getZ() + Math.sin(hoek) * r)) {
                return true;
            }
        }
        return false;
    }

    /** Flits to just behind this player's back. */
    public boolean flitsAchter(Player player) {
        if (isHandelaar() || level().isClientSide()) {
            return false;
        }
        flitsPauze = FLITS_PAUZE;
        Vec3 kijk = player.getLookAngle().multiply(1, 0, 1);
        if (kijk.lengthSqr() < 1.0e-4) {
            kijk = new Vec3(0, 0, 1);
        }
        kijk = kijk.normalize();
        for (double r : new double[]{1.6, 2.0, 1.2}) {
            if (flits(player.getX() - kijk.x * r, player.getY() + 1.0, player.getZ() - kijk.z * r)) {
                return true;
            }
        }
        return false;
    }

    // =================================================================================================================
    // goals
    // =================================================================================================================

    /** With its loot: it stands and shows it off, and keeps its distance from the one it robbed. */
    private class HoudVastGoal extends Goal {
        HoudVastGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return draagtKnabbel();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ServerPlayer wie = slachtofferSpeler();
            if (wie == null || wie.level() != level()) {
                return;
            }
            getLookControl().setLookAt(wie, 30f, 30f);
            if (stand() == DRAAGT && flitsPauze <= 0 && wie.distanceToSqr(EnderMikaEntity.this) < 16.0) {
                flitsWeg(wie);      // (you can't just walk up and take it back: look at it)
            }
        }
    }

    /** A Kaasmijt! It forgets everything and trails after it, at a respectful three blocks. */
    private class VolgMijtGoal extends Goal {
        private int herpad;

        VolgMijtGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isHandelaar() && volgt() != null;
        }

        @Override
        public void start() {
            herpad = 0;
            playSound(E2MikasFeature.ENDER_ZWIJMEL.get(), 0.8f, getVoicePitch());
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            KaasmijtEntity m = volgt();
            if (m == null) {
                return;
            }
            getLookControl().setLookAt(m, 30f, 60f);
            if (--herpad <= 0) {
                herpad = 10;
                if (distanceToSqr(m) > 9.0) {
                    getNavigation().moveTo(m, 1.0);
                } else {
                    getNavigation().stop();
                }
            }
            if (random.nextInt(120) == 0 && level() instanceof ServerLevel server) {
                playSound(E2MikasFeature.ENDER_ZWIJMEL.get(), 0.6f, getVoicePitch());
                server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    /** The Kaasmijt it is trailing after (null: none around; it looks around for one every half second). */
    @Nullable
    public KaasmijtEntity volgt() {
        if (isHandelaar() || draagtKnabbel() || level().isClientSide()) {
            return null;
        }
        if (zoekMijt == 0 || tickCount - zoekMijt >= 10) {
            zoekMijt = Math.max(1, tickCount);
            mijt = null;
            double best = MIJT_STRAAL * MIJT_STRAAL;
            for (KaasmijtEntity m : level().getEntitiesOfClass(KaasmijtEntity.class, getBoundingBox().inflate(MIJT_STRAAL, 6, MIJT_STRAAL), Entity::isAlive)) {
                double d = m.distanceToSqr(this);
                if (d < best && isWithinHome(m.blockPosition())) {
                    mijt = m;
                    best = d;
                }
            }
        }
        return mijt != null && mijt.isAlive() && distanceToSqr(mijt) < (MIJT_STRAAL + 4) * (MIJT_STRAAL + 4) ? mijt : null;
    }

    /** Someone holds a knabbel: it comes up behind them (only while they do not look) and takes one. */
    private class JatGoal extends Goal {
        @Nullable
        private ServerPlayer doel;
        private int herpad;

        JatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private ServerPlayer zoek() {
            ServerPlayer best = null;
            double bestD = JAT_STRAAL * JAT_STRAAL;
            for (Player player : level().players()) {
                double d = player.distanceToSqr(EnderMikaEntity.this);
                if (d < bestD && player instanceof ServerPlayer sp && magJatten(sp) && isWithinHome(sp.blockPosition())) {
                    best = sp;
                    bestD = d;
                }
            }
            return best;
        }

        @Override
        public boolean canUse() {
            if (isHandelaar() || draagtKnabbel() || jatPauze > 0 || volgt() != null) {
                return false;
            }
            doel = zoek();
            return doel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && volgt() == null && magJatten(doel) && doel.level() == level()
                    && doel.distanceToSqr(EnderMikaEntity.this) < (JAT_STRAAL + 6) * (JAT_STRAAL + 6);
        }

        @Override
        public void start() {
            herpad = 0;
        }

        @Override
        public void stop() {
            doel = null;
            getNavigation().stop();
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
            getLookControl().setLookAt(doel, 30f, 30f);
            boolean gezien = bekekenDoor(doel);
            double d = doel.distanceToSqr(EnderMikaEntity.this);
            if (d <= GRIJP_AFSTAND * GRIJP_AFSTAND && !gezien) {
                ServerPlayer wie = doel;
                if (jat(wie)) {
                    flitsPauze = 0;
                    flitsWeg(wie);
                }
                return;
            }
            if (--herpad <= 0) {
                herpad = 10;
                if (!gezien && flitsPauze <= 0 && d > 9.0 && flitsAchter(doel)) {
                    return;
                }
                if (gezien) {
                    getNavigation().stop();     // (it waits until you look away; look long enough and it flits off)
                } else {
                    getNavigation().moveTo(doel, 1.2);
                }
            }
        }
    }

    // =================================================================================================================
    // a Mika: hit it and it shoves; its loot is never lost
    // =================================================================================================================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isHandelaar() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            laatVallen();
            Entity dader = source.getEntity();
            if (dader instanceof LivingEntity levend && levend.isAlive() && levend.distanceToSqr(this) < 12.25
                    && !(dader instanceof Player p && (p.isCreative() || p.isSpectator()))) {
                float yaw = (float) (Mth.atan2(levend.getZ() - getZ(), levend.getX() - getX()) * Mth.RAD_TO_DEG) - 90f;
                setYRot(yaw);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;
                doHurtTarget(level, levend);        // (the Mika's shove: a push and a hop back, never damage)
            }
            flitsPauze = 0;
            flitsWeg(dader);
        }
        return hurt;
    }

    @Override
    public void knockback(double power, double x, double z) {
        if (isHandelaar()) {
            return;
        }
        super.knockback(power, x, z);
        if (!level().isClientSide()) {
            laatVallen();           // (shoved: it lets go)
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        laatVallen();
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }

    /** (it goes away for good: whoever it robbed gets the knabbel back first) */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && draagtKnabbel()) {
            if (reason == Entity.RemovalReason.KILLED) {
                laatVallen();
            } else if (reason == Entity.RemovalReason.DISCARDED) {
                geefTerug();
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return draagtKnabbel() || isHandelaar() || super.requiresCustomPersistence();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !draagtKnabbel() && !isHandelaar() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean isPushable() {
        return !isHandelaar() && super.isPushable();
    }

    /** (nobody goes after a trader: an aggressive guh leaves it alone too) */
    @Override
    public boolean canBeSeenAsEnemy() {
        return !isHandelaar() && super.canBeSeenAsEnemy();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.35f + this.random.nextFloat() * 0.15f;     // (a long thin Mika squeaks)
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // =================================================================================================================
    // saving
    // =================================================================================================================

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Handelaar", isHandelaar());
        output.storeNullable("Slachtoffer", UUIDUtil.CODEC, slachtoffer);
        output.putInt("DraagTijd", draagTijd);
        output.putInt("JatPauze", jatPauze);
        honk.bewaar(output);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(HANDELAAR, input.getBooleanOr("Handelaar", false));
        if (isHandelaar()) {
            setPersistenceRequired();
        }
        slachtoffer = input.read("Slachtoffer", UUIDUtil.CODEC).orElse(null);
        draagTijd = input.getIntOr("DraagTijd", 0);
        jatPauze = input.getIntOr("JatPauze", 0);
        honk.lees(input);
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    /** (for the tests) */
    public void zetHonk(int straal) {
        honk.zet(straal);
    }

    /** (for the tests) */
    public void zetDraagTijd(int ticks) {
        this.draagTijd = ticks;
    }

    /** (for the tests: nobody was robbed lately, this one may steal at once) */
    public void vergeetPauzes() {
        this.jatPauze = 0;
        this.flitsPauze = 0;
        this.zoekMijt = 0;
        BESTOLEN.clear();
    }

    // =================================================================================================================
    // animations ("pounce" is what MikaEntity triggers when it shoves)
    // =================================================================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, state -> {
            byte stand = stand();
            if (stand == BETRAPT) {
                return state.setAndContinue(BETRAPT_ANIM);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(stand == DRAAGT ? DRAAG : IDLE);
        }));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP)
                .triggerableAnim("jat", JAT).triggerableAnim("flits", FLITS).triggerableAnim("pounce", DUW));
    }
}
