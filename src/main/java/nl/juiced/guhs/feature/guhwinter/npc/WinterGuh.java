package nl.juiced.guhs.feature.guhwinter.npc;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

/**
 * Guhwinter (npc slice): a guh of the Winterplein (entity guhwinter_guh). The real guh model with real guh clothes plus the
 * event's own look pieces (client/GuhwinterNpcClient), never tamed, fed, pushed, leashed, picked up or hurt. Spawned by
 * {@link GuhwinterNpcs} and never saved: a chunk that unloads takes it along and the keeper puts it back when a player
 * comes near. A click goes to {@link GuhwinterNpcs#klik}.
 * <p>Idling after its {@link NpcDefs.Gedrag}; a scene can take it over ({@link #loopNaar}, {@link #kijkNaar},
 * {@link #vasthouden}) until {@link #losLaten}.
 */
public class WinterGuh extends GuhEntity {
    /** Its NPC name (joost, sint, pakjesguh_2, ...), synced: the client picks its look pieces by it. */
    private static final EntityDataAccessor<String> DATA_NAAM = SynchedEntityData.defineId(WinterGuh.class, EntityDataSerializers.STRING);

    @Nullable
    Vec3 thuis;
    float thuisYaw;
    NpcDefs.Gedrag gedrag = NpcDefs.Gedrag.STAAN;
    /** The phase it is dressed for (null: not dressed yet). */
    @Nullable
    NpcFase gekleed;

    // --- scene control ----------------------------------------------------------------------------------------------------
    /** Walk here (null: no scene walk). */
    @Nullable
    private Vec3 sceneDoel;
    private double sceneSnelheid = 1.0;
    /** A scene holds it: no idling, no strolling home, the keeper leaves it where it is. */
    private boolean vast;
    @Nullable
    private Vec3 kijkDoel;
    @Nullable
    private Entity kijkEntity;
    private long kijkTot;

    /** The keeper's count of seconds in a row this guh was in the water (playtest 1: then it goes home). */
    int natTellen;
    private int zwaaiPauze;
    private int klusPauze = 200;

    public WinterGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        // (playtest 1) never plan a walk through water: the canal by the Pakjesboot is 2 deep with a kade around it
        setPathfindingMalus(PathType.WATER, -1f);
        setPathfindingMalus(PathType.WATER_BORDER, 16f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NAAM, "");
    }

    /** Its NPC name ("" before it is set up). Both sides. */
    public String naam() {
        return this.entityData.get(DATA_NAAM);
    }

    @Nullable
    public NpcDefs.Def def() {
        return NpcDefs.van(naam());
    }

    /** Set it up after its definition (server side, right after creating it). */
    void stelIn(NpcDefs.Def d, Vec3 plek, float yaw, NpcFase fase) {
        this.entityData.set(DATA_NAAM, d.naam());
        this.thuis = plek;
        this.thuisYaw = yaw;
        this.gedrag = d.gedrag();
        setVariant(d.variant());
        kleed(d, fase);
        setCustomName(Component.translatable(d.naamKey()));
        setCustomNameVisible(true);
        if (d.maat() != 1f && getAttribute(Attributes.SCALE) != null) {
            getAttribute(Attributes.SCALE).setBaseValue(d.maat());
        }
        boolean zit = d.gedrag() == NpcDefs.Gedrag.ZITTEN || d.gedrag() == NpcDefs.Gedrag.SLEE;
        setOrderedToSit(zit);
        setInSittingPose(zit);
    }

    /** Dress it for this phase (the Kerst santa hats, the Oudjaar party hats). */
    void kleed(NpcDefs.Def d, NpcFase fase) {
        takeOffClothes();
        takeOff(GuhClothes.Slot.HAAR);
        for (GuhClothes c : d.kleren(fase)) {
            wear(c);
        }
        setHaarkleur(d.haarkleur());
        gekleed = fase;
    }

    // =====================================================================================================================
    // scene control (GuhwinterNpcs.walkTo / lookAt / ...)
    // =====================================================================================================================

    /** Walk to this spot (a scene). It stands up if it sat, and stays there (held) until {@link #losLaten}. */
    public void loopNaar(Vec3 doel, double snelheid) {
        this.sceneDoel = doel;
        this.sceneSnelheid = snelheid;
        this.vast = true;
        setOrderedToSit(false);
        setInSittingPose(false);
    }

    /** Is it still walking to a scene spot? */
    public boolean loopt() {
        return sceneDoel != null;
    }

    /** Look at this spot for a while (ticks). */
    public void kijkNaar(Vec3 doel, int ticks) {
        this.kijkDoel = doel;
        this.kijkEntity = null;
        this.kijkTot = level().getGameTime() + ticks;
    }

    /** Look at this entity (a player, another guh) for a while (ticks). */
    public void kijkNaar(Entity doel, int ticks) {
        this.kijkEntity = doel;
        this.kijkDoel = null;
        this.kijkTot = level().getGameTime() + ticks;
    }

    /** A scene holds it where it is (true) or lets it go (false = {@link #losLaten}). */
    public void vasthouden(boolean v) {
        this.vast = v;
        if (v) {
            getNavigation().stop();
        }
    }

    public boolean isVast() {
        return vast;
    }

    /** The scene is over: back to idling (it walks home by itself, and sits again if it sat). */
    public void losLaten() {
        this.vast = false;
        this.sceneDoel = null;
        this.kijkDoel = null;
        this.kijkEntity = null;
        NpcDefs.Def d = def();
        if (d != null && thuis != null && (d.gedrag() == NpcDefs.Gedrag.ZITTEN || d.gedrag() == NpcDefs.Gedrag.SLEE)) {
            snapTo(thuis.x, thuis.y, thuis.z, thuisYaw, 0);
            setOrderedToSit(true);
            setInSittingPose(true);
        }
    }

    /** Sit down (true) or stand up (false) right now (scenes). */
    public void zitten(boolean zit) {
        setOrderedToSit(zit);
        setInSittingPose(zit);
    }

    // =====================================================================================================================
    // goals and ticking
    // =====================================================================================================================

    @Override
    protected void registerGoals() {
        // (playtest 1) float: without it a guh that fell in sank to the bottom of the canal and walked there for ever
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SceneGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(3, new IdleGoal());
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            GuhwinterNpcs.klik(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (kijkTot > 0) {
            if (level().getGameTime() > kijkTot || (kijkEntity != null && !kijkEntity.isAlive())) {
                kijkTot = 0;
                kijkDoel = null;
                kijkEntity = null;
            } else if (kijkEntity != null) {
                getLookControl().setLookAt(kijkEntity);
            } else if (kijkDoel != null) {
                getLookControl().setLookAt(kijkDoel.x, kijkDoel.y, kijkDoel.z);
            }
        }
        if (gedrag == NpcDefs.Gedrag.SLEE && !vast) {
            opDeSlee(sl);
        }
        if (vast) {
            return;
        }
        if (zwaaiPauze > 0) {
            zwaaiPauze--;
        }
        if (this.tickCount % 20 == 7 && zwaaiPauze == 0) {
            Player p = sl.getNearestPlayer(this, 5);
            if (p != null && getRandom().nextInt(4) == 0) {
                emotes.start(Emote.ZWAAIEN, false, GuhEmotes.Source.SELF);
                zwaaiPauze = 20 * 90;
            }
        }
        if (this.tickCount % 600 == 300 && getRandom().nextInt(3) == 0) {
            boolean zit = isInSittingPose();
            Emote[] idle = zit ? new Emote[] {Emote.GAPEN, Emote.ZINGEN, Emote.SMAKKEN, Emote.HARTJES}
                    : new Emote[] {Emote.VAHOEG, Emote.ZINGEN, Emote.DANSEN, Emote.HARTJES, Emote.GAPEN};
            emotes.start(idle[getRandom().nextInt(idle.length)], false, GuhEmotes.Source.SELF);
        }
        if (--klusPauze <= 0) {
            klusPauze = 20 * (20 + getRandom().nextInt(25));
            klusje(sl);
        }
    }

    /** Its little habit, now and then while a player is near: Anthony takes a photo, a pakjesguh strews pepernoten. */
    private void klusje(ServerLevel sl) {
        if (sl.getNearestPlayer(this, 12) == null) {
            return;
        }
        String n = naam();
        if (n.equals("anthony")) {
            // *klik*: a little flash in front of his camera
            Vec3 voor = Vec3.directionFromRotation(0, getYHeadRot()).scale(0.6 * getScale());
            sl.sendParticles(ParticleTypes.END_ROD, getX() + voor.x, getY() + 0.5 * getScale(), getZ() + voor.z, 8, 0.15, 0.15, 0.15, 0.03);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SPYGLASS_USE, SoundSource.NEUTRAL, 1f, 1.6f);
            emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
        } else if (n.startsWith("pakjesguh_")) {
            // pepernoten in a little arc (only to look at; the real ones are hidden around the plein)
            Vec3 voor = Vec3.directionFromRotation(-35, getYRot());
            ItemParticleOption noot = new ItemParticleOption(ParticleTypes.ITEM, Items.COOKIE);
            for (int i = 0; i < 10; i++) {
                double s = 0.25 + getRandom().nextDouble() * 0.2;
                sl.sendParticles(noot, getX(), getY() + 0.7, getZ(), 0, voor.x + (getRandom().nextDouble() - 0.5) * 0.6, voor.y,
                        voor.z + (getRandom().nextDouble() - 0.5) * 0.6, s);
            }
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.NEUTRAL, 0.7f, 1.4f);
            emotes.start(getRandom().nextBoolean() ? Emote.DANSEN : Emote.VAHOEG, false, GuhEmotes.Source.SELF);
        } else if (n.equals("sint") && getRandom().nextInt(3) == 0) {
            emotes.start(Emote.ZINGEN, false, GuhEmotes.Source.SELF);
        }
    }

    /** Liv: on her sled right in front of Daan, sliding along when he pushes; at her own spot when he isn't there. */
    private void opDeSlee(ServerLevel sl) {
        if (GuhwinterNpcs.get(sl, "daan") instanceof WinterGuh daan && !daan.vast && daan.distanceToSqr(this) < 12 * 12) {
            Vec3 voor = Vec3.directionFromRotation(0, daan.getYRot()).scale(1.1);
            double x = daan.getX() + voor.x, z = daan.getZ() + voor.z;
            if (distanceToSqr(x, daan.getY(), z) > 0.0025) {
                snapTo(x, daan.getY(), z, daan.getYRot(), 0);
                setYBodyRot(daan.getYRot());
            }
        }
    }

    /** A scene walk: to the spot, then stand there (held). */
    private final class SceneGoal extends Goal {
        private int opnieuw;

        SceneGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return sceneDoel != null;
        }

        @Override
        public void start() {
            opnieuw = 0;
            getNavigation().moveTo(sceneDoel.x, sceneDoel.y, sceneDoel.z, sceneSnelheid);
        }

        @Override
        public void tick() {
            if (sceneDoel == null) {
                return;
            }
            if (position().distanceToSqr(sceneDoel) < 1.2 * 1.2) {
                getNavigation().stop();
                sceneDoel = null;
            } else if (getNavigation().isDone()) {
                if (++opnieuw > 6) {
                    snapTo(sceneDoel.x, sceneDoel.y, sceneDoel.z);   // (stuck: a scene can't wait)
                    sceneDoel = null;
                } else {
                    getNavigation().moveTo(sceneDoel.x, sceneDoel.y, sceneDoel.z, sceneSnelheid);
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return sceneDoel != null;
        }
    }

    /** Idling: stand (and walk back home when it drifted), stroll around, skate rounds, push the sled. */
    private final class IdleGoal extends Goal {
        private int pauze = 40;
        private double hoek;

        IdleGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (thuis == null || vast || isInSittingPose()) {
                return false;
            }
            if (--pauze > 0) {
                return false;
            }
            switch (gedrag) {
                case SCHAATSEN -> pauze = 2 + getRandom().nextInt(10);
                case DUWEN -> pauze = 60 + getRandom().nextInt(80);
                default -> pauze = 120 + getRandom().nextInt(200);
            }
            return gedrag == NpcDefs.Gedrag.SLENTER || gedrag == NpcDefs.Gedrag.SCHAATSEN || gedrag == NpcDefs.Gedrag.DUWEN
                    || distanceToSqr(thuis.x, getY(), thuis.z) > 4;
        }

        @Override
        public void start() {
            double x = thuis.x, z = thuis.z, snel = 0.6;
            switch (gedrag) {
                case SLENTER -> {
                    // a dry spot within 4 blocks (Anthony and the pakjesguhs stand right at the kade: no strolls into the canal)
                    for (int i = 0; i < 8; i++) {
                        double px = thuis.x + getRandom().nextInt(9) - 4, pz = thuis.z + getRandom().nextInt(9) - 4;
                        if (droog(px, thuis.y, pz)) {
                            x = px;
                            z = pz;
                            break;
                        }
                    }
                }
                case SCHAATSEN -> {
                    // big rounds on the ice: the next point a bit further round the circle
                    hoek += 0.7 + getRandom().nextDouble() * 0.4;
                    x += Math.cos(hoek) * 4.5;
                    z += Math.sin(hoek) * 4.5;
                    snel = 1.15;
                }
                case DUWEN -> {
                    // slow rounds, a little piece at a time, with a rest in between (Liv sits in front of him)
                    hoek += 0.5;
                    x += Math.cos(hoek) * 3.5;
                    z += Math.sin(hoek) * 3.5;
                    snel = 0.45;
                }
                default -> {
                }
            }
            getNavigation().moveTo(x, thuis.y, z, snel);
        }

        @Override
        public boolean canContinueToUse() {
            return !getNavigation().isDone() && !vast;
        }
    }

    /** A spot to stroll to: no water in it or under it (the canal's air above the water counts as wet too). */
    private boolean droog(double x, double y, double z) {
        BlockPos p = BlockPos.containing(x, y, z);
        for (int dy = -2; dy <= 0; dy++) {
            if (!level().getFluidState(p.above(dy)).isEmpty()) {
                return false;
            }
        }
        return !level().getBlockState(p.below()).isAir() || !level().getBlockState(p.below(2)).isAir();
    }

    // --- never anybody's, never hurt, never saved ---------------------------------------------------------------------------

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public void onLaunchPressed(Player rider) {
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isSaddleable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    /** (for the keeper: an angle in [0, 360) so a yaw from the anchors file is always usable) */
    static float yaw(float y) {
        return Mth.wrapDegrees(y);
    }
}
