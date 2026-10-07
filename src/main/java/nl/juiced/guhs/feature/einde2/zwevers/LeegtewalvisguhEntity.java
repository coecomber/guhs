package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhTime;
import nl.juiced.guhs.world.WildeDieren;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * The Leegtewalvisguh: a huge, slow whale guh that floats between the outer islands of the Guheinde and sings. Its back is
 * flat and crusty, knabbelkoor grows on it, and you can land and walk on it while it carries you along. Feed it a
 * knabbelkoorvrucht and it gives a {@code leegteparel} (one per player per Minecraft day). It can't be tamed, hurt or pushed.
 * <p>
 * <b>Only there for players with the flag of {@link Walviszicht}</b> (default: nobody):
 * <ul>
 *   <li>{@link #broadcastToPlayer} sends it only to them, so nobody else's client knows it exists;</li>
 *   <li>its back ({@link Rug}: five boxes along its spine, NeoForge part entities, found by collision checks wherever
 *       they are) only carries them, what they ride and their own animals ({@link #draagt});</li>
 *   <li>for everything else in the world it is not there at all ({@link #isSpectator}: no search for "what is here?"
 *       finds it, so no mob looks at it, nothing pushes it, and the Guhdex doesn't notice it for a passer-by; the boxes
 *       of its back are of a kind of their own, {@code leegtewalvisguh_rug}, for the same reason). Its page, its song and
 *       its hearts go to the players who see it, one by one ({@link #kijkers}).</li>
 * </ul>
 * It flies level (a back that moves up or down under a player's feet fights with the player's own client), steers around
 * islands and clouds as high as its belly, never leaves the part of the world that is awake, and keeps {@link #MIJD}
 * blocks away from the two big places of the deep band (Bikinibodem and Schelpenstad reach up to its height). Whoever
 * stands on its back is carried and turned along with it by the side that moves them: a player by their own client, a guh
 * by the server ({@link #draagMee}).
 * <p>
 * The model is small (tools/features/einde2_zwevers_modellen.py: 1 model pixel = half a block) and the whale lives at
 * scale {@link #SCHAAL} (the vanilla scale attribute): drawn that big in the world, and at its model size in the Guhdex
 * (a picture of an entity ignores the scale), where it fits on its page.
 */
public class LeegtewalvisguhEntity extends Mob implements GeoEntity {
    /** Its scale attribute (= einde2_zwevers_modellen.SCHAAL_WALVIS). */
    public static final float SCHAAL = 8f;
    /** The top of its back, in blocks above its feet (= RUG_Y of the model, scaled). */
    public static final double RUG = 6.0;
    /** The boxes of its back: this wide and high, at these distances ahead of its middle (the head is in front). */
    public static final float DEEL_BREED = 8.5f, DEEL_HOOG = 5.5f;
    public static final double[] DEEL_PLEK = {7.0, 3.5, 0.0, -3.5, -7.0};
    /** It flies with its feet between these heights (its back then lies at 96-116: inside the natural band, under the high one). */
    public static final int MIN_Y = 90, MAX_Y = 110;
    /** Blocks per tick, and degrees per tick it can turn. */
    public static final double VAART = 0.045;
    public static final float DRAAI = 0.3f;
    /** A song lasts this long; a note sounds every {@link #NOOT_TICKS} ticks, at these pitches. */
    public static final int ZANG_TICKS = 160, NOOT_TICKS = 40;
    public static final float[] ZANG = {0.6f, 0.75f, 0.67f, 0.5f};
    /** It keeps this far from the boxes of the deep band's big places. */
    public static final int MIJD = 32;
    /** A come-and-go whale leaves when no player who sees it is within this many blocks. */
    public static final int WEG = 320;
    /** Player key: the Minecraft day of the last pearl. */
    public static final String PAREL_DAG = "guhs_einde2_zwevers_parel_dag";

    private static final EntityDataAccessor<Boolean> DATA_ZINGT = SynchedEntityData.defineId(LeegtewalvisguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ZING = RawAnimation.begin().thenLoop("zing");
    /** The boxes of the deep band's big places, per level (pure functions of the world: looked up once). */
    private static final Map<ServerLevel, List<BoundingBox>> MIJD_DOZEN = new WeakHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Rug[] delen = new Rug[DEEL_PLEK.length];
    /** Where it heads and where it wants to head (degrees, like a yaw), and which way it turns for an obstacle. */
    private float koers, doelKoers;
    private boolean koersGezet;
    private int koersTijd, draaiKant = 1;
    private int zangRest, volgendeZang = 200;
    /** (Both sides) where it was a tick ago: what stands on it moves by the difference. */
    @Nullable
    private Vec3 vorigePlek;
    private float vorigeKoers;
    /** (Client) how far the song is. */
    private int zangTik;

    public LeegtewalvisguhEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        for (int i = 0; i < delen.length; i++) {
            delen[i] = new Rug(this);
        }
        // (as the ender dragon: the ids of the parts follow the whale's own, so a click on a part finds it on the server)
        setId(ENTITY_COUNTER.getAndAdd(delen.length + 1) + 1);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (delen != null) {
            for (int i = 0; i < delen.length; i++) {
                if (delen[i] != null) {
                    delen[i].setId(id + i + 1);
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.SCALE, SCHAAL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ZINGT, false);
    }

    @Override
    protected void registerGoals() {
    }

    // --- only for who may see it ------------------------------------------------------------------------------------------------

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return Walviszicht.ziet(player);
    }

    /** For the world it is not there: searches skip it, nothing targets, pushes or notices it. Its back is ({@link Rug}). */
    @Override
    public boolean isSpectator() {
        return true;
    }

    /**
     * A player's flag changed: every whale of this level is taken out of the entity tracker and put back, so it appears
     * for (or vanishes from) that player at once instead of at the next chunk border.
     */
    public static void herzie(ServerLevel level) {
        for (LeegtewalvisguhEntity walvis : alle(level)) {
            level.getChunkSource().removeEntity(walvis);
            level.getChunkSource().addEntity(walvis);
        }
    }

    /** Every whale of this level (a plain search for entities skips them: see {@link #isSpectator}). */
    public static List<? extends LeegtewalvisguhEntity> alle(ServerLevel level) {
        return level.getEntities(E2ZweversFeature.LEEGTEWALVISGUH.get(), w -> w.isAlive());
    }

    /** Does its back carry this one? A player who sees it, what such a player rides, and their own animals close by. */
    public boolean draagt(@Nullable Entity e) {
        if (e == null || e instanceof Rug) {
            return false;
        }
        if (level().isClientSide()) {
            return true;                                          // (it only exists on the clients of players who see it)
        }
        if (e instanceof Player p) {
            return Walviszicht.ziet(p);
        }
        if (e.distanceToSqr(this) > 24 * 24) {
            return false;
        }
        for (Entity ruiter : e.getPassengers()) {
            if (ruiter instanceof Player p && Walviszicht.ziet(p)) {
                return true;
            }
        }
        return e instanceof OwnableEntity dier && dier.getOwner() instanceof Player baas && Walviszicht.ziet(baas);
    }

    /** The players close by who see it. */
    public void kijkers(double bereik, Consumer<ServerPlayer> doe) {
        if (level() instanceof ServerLevel level) {
            for (ServerPlayer p : level.players()) {
                if (Walviszicht.ziet(p) && p.distanceToSqr(this) <= bereik * bereik) {
                    doe.accept(p);
                }
            }
        }
    }

    /** A sound only those players hear (a level sound would reach everybody). */
    private void geluid(Holder<SoundEvent> geluid, float volume, float pitch) {
        kijkers(16 * Math.max(1f, volume), p -> p.connection.send(new ClientboundSoundPacket(geluid, SoundSource.NEUTRAL, getX(), getY() + RUG, getZ(),
                volume, pitch, random.nextLong())));
    }

    // --- its back -----------------------------------------------------------------------------------------------------------------

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return delen;
    }

    /** The direction it heads in (a yaw in degrees), as a vector. */
    public static Vec3 vooruit(float koers) {
        float r = koers * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    private void plaatsDelen() {
        Vec3 f = vooruit(getYRot());
        for (int i = 0; i < delen.length; i++) {
            Rug deel = delen[i];
            Vec3 oud = deel.position();
            deel.setPos(getX() + f.x * DEEL_PLEK[i], getY() + RUG - DEEL_HOOG, getZ() + f.z * DEEL_PLEK[i]);
            deel.xo = deel.xOld = oud.x;
            deel.yo = deel.yOld = oud.y;
            deel.zo = deel.zOld = oud.z;
        }
    }

    /** The height of its back in the world. */
    public double rugY() {
        return getY() + RUG;
    }

    /** Does this one stand on its back (feet on the top of one of the boxes)? */
    public boolean staatOp(Entity e) {
        double voet = e.getBoundingBox().minY;
        if (voet < rugY() - 0.3 || voet > rugY() + 0.2) {
            return false;
        }
        double half = e.getBbWidth() / 2 + 0.05;
        for (Rug deel : delen) {
            AABB b = deel.getBoundingBox();
            if (e.getX() > b.minX - half && e.getX() < b.maxX + half && e.getZ() > b.minZ - half && e.getZ() < b.maxZ + half) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whatever stands on its back goes where the back goes (and turns around its middle when it turns). Each side moves
     * only what it moves itself: a player's own client the player (and what the player steers), the server the rest.
     */
    private void draagMee() {
        Vec3 nu = position();
        if (vorigePlek == null) {
            vorigePlek = nu;
            vorigeKoers = getYRot();
            return;
        }
        Vec3 stap = nu.subtract(vorigePlek);
        float draai = Mth.wrapDegrees(getYRot() - vorigeKoers);
        if (stap.lengthSqr() > 1e-10 || Math.abs(draai) > 1e-4f) {
            AABB zoek = new AABB(vorigePlek.x - 13, vorigePlek.y + RUG - 0.5, vorigePlek.z - 13, vorigePlek.x + 13, vorigePlek.y + RUG + 1.0, vorigePlek.z + 13);
            for (Entity e : level().getEntities(this, zoek, x -> !(x instanceof Rug) && !x.isPassenger() && x.isLocalInstanceAuthoritative())) {
                double voet = e.getBoundingBox().minY - (vorigePlek.y + RUG);
                if (voet < -0.3 || voet > 0.2 || !draagt(e) || !opOudeRug(e)) {
                    continue;
                }
                Vec3 rel = e.position().subtract(vorigePlek).yRot(-draai * Mth.DEG_TO_RAD);
                e.setPos(nu.x + rel.x, Math.max(nu.y + rel.y, rugY()), nu.z + rel.z);
            }
        }
        vorigePlek = nu;
        vorigeKoers = getYRot();
    }

    /** Stood this one on the back where the back was a tick ago? (The boxes have moved on already.) */
    private boolean opOudeRug(Entity e) {
        Vec3 f = vooruit(vorigeKoers);
        double half = DEEL_BREED / 2 + e.getBbWidth() / 2 + 0.05;
        for (double plek : DEEL_PLEK) {
            if (Math.abs(e.getX() - (vorigePlek.x + f.x * plek)) < half && Math.abs(e.getZ() - (vorigePlek.z + f.z * plek)) < half) {
                return true;
            }
        }
        return false;
    }

    // --- flying -------------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        draagMee();
        plaatsDelen();
        if (level().isClientSide()) {
            zingClient();
        }
    }

    @Override
    public void travel(Vec3 input) {
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (!koersGezet) {
            koers = doelKoers = getYRot();
            draaiKant = random.nextBoolean() ? 1 : -1;
            koersGezet = true;
        }
        stuur(level);
        koers = Mth.approachDegrees(koers, doelKoers, DRAAI);
        Vec3 f = vooruit(koers);
        setPos(getX() + f.x * VAART, getY(), getZ() + f.z * VAART);
        setYRot(koers);
        setXRot(0);
        yBodyRot = koers;
        yHeadRot = koers;
        setDeltaMovement(Vec3.ZERO);
        if (zangRest > 0) {
            if (--zangRest == 0) {
                entityData.set(DATA_ZINGT, false);
            }
        } else if (--volgendeZang <= 0) {
            volgendeZang = 500 + random.nextInt(700);
            if (iemandZiet(96)) {
                zing();
            }
        }
        if (tickCount % 20 == 0) {
            kijkers(48, p -> {
                GuhDex.zie(p, GuhVariant.LEEGTEWALVISGUH);
                if (staatOp(p)) {
                    GuhAdvancements.grant(p, "einde2_zwevers_walvisrug");
                }
            });
        }
    }

    /** Now and then a new heading; and away from what lies ahead. */
    private void stuur(ServerLevel level) {
        if (--koersTijd <= 0) {
            doelKoers = koers + (random.nextFloat() - 0.5f) * 120f;
            koersTijd = 300 + random.nextInt(500);
        }
        if (tickCount % 10 != 0) {
            return;
        }
        Vec3 f = vooruit(koers);
        for (int d : new int[]{14, 28}) {
            if (inDeWeg(level, getX() + f.x * d, getZ() + f.z * d)) {
                doelKoers = koers + draaiKant * 50f;
                koersTijd = 80;
                return;
            }
        }
    }

    /**
     * Should it stay away from this spot at its own height? The edge of the world that is awake (where entities tick: beyond
     * it the whale would hang still), an island or a cloud as high as its belly, and the big places of the deep band with
     * {@link #MIJD} blocks around them.
     */
    public boolean inDeWeg(ServerLevel level, double x, double z) {
        BlockPos p = BlockPos.containing(x, getY(), z);
        if (!level.isPositionEntityTicking(p) || level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()) > getY() - 4) {
            return true;
        }
        for (BoundingBox doos : mijd(level)) {
            if (x >= doos.minX() - MIJD && x <= doos.maxX() + MIJD && z >= doos.minZ() - MIJD && z <= doos.maxZ() + MIJD) {
                return true;
            }
        }
        return false;
    }

    /** The boxes it keeps away from: Bikinibodem and Schelpenstad (they reach up to its height). */
    public static List<BoundingBox> mijd(ServerLevel level) {
        return MIJD_DOZEN.computeIfAbsent(level, l -> {
            List<BoundingBox> uit = new ArrayList<>();
            if (GuheindeFeature.isGuheinde(l)) {
                for (String structuur : Einde2Ids.SPONS_STRUCTUREN) {
                    BoundingBox doos = Plekken.doos(l, structuur);
                    if (doos != null) {
                        uit.add(doos);
                    }
                }
            }
            return uit;
        });
    }

    /** (Tests) the boxes of this level. */
    static void zetMijd(ServerLevel level, List<BoundingBox> dozen) {
        MIJD_DOZEN.put(level, dozen);
    }

    public boolean iemandZiet(double bereik) {
        boolean[] ja = {false};
        kijkers(bereik, p -> ja[0] = true);
        return ja[0];
    }

    /** A come-and-go whale of the top-up leaves when nobody who sees it is around any more. */
    @Override
    public void checkDespawn() {
        if (WildeDieren.isKomEnGa(this) && !iemandZiet(WEG)) {
            discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- singing ------------------------------------------------------------------------------------------------------------------

    public boolean isZingend() {
        return entityData.get(DATA_ZINGT);
    }

    /** Starts a song (the clients of the players who see it play the notes). */
    public void zing() {
        entityData.set(DATA_ZINGT, true);
        zangRest = ZANG_TICKS;
    }

    private void zingClient() {
        if (!isZingend()) {
            zangTik = 0;
            return;
        }
        if (zangTik % NOOT_TICKS == 0) {
            float toon = ZANG[(zangTik / NOOT_TICKS) % ZANG.length];
            level().playLocalSound(getX(), rugY(), getZ(), E2ZweversFeature.WALVIS_ZANG.get(), SoundSource.NEUTRAL, 5f, toon, false);
            Vec3 gat = position().add(vooruit(getYRot()).scale(8.5)).add(0, RUG + 0.3, 0);   // (the blowhole on its head)
            for (int i = 0; i < 5; i++) {
                level().addParticle(ParticleTypes.NOTE, gat.x + random.nextGaussian() * 0.6, gat.y + i * 0.5, gat.z + random.nextGaussian() * 0.6,
                        random.nextInt(25) / 24.0, 0, 0);
            }
        }
        zangTik++;
    }

    // --- feeding ------------------------------------------------------------------------------------------------------------------

    public static boolean isVoer(ItemStack stack) {
        return stack.is(E2BiomesFeature.KNABBELKOORVRUCHT.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return klik(player, hand);
    }

    /** A click on the whale or on its back: a knabbelkoorvrucht from a player who sees it is eaten. */
    public InteractionResult klik(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isVoer(stack)) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !Walviszicht.ziet(sp)) {
            return InteractionResult.PASS;
        }
        voer(sp, stack);
        return InteractionResult.SUCCESS;
    }

    /**
     * It eats the fruit and sings; the first time on a Minecraft day this player gets a leegteparel (true), later that day
     * a thank-you only (false).
     */
    public boolean voer(ServerPlayer player, ItemStack vrucht) {
        vrucht.consume(1, player);
        triggerAnim("actie", "eet");
        geluid(E2ZweversFeature.WALVIS_HAP, 2f, 0.7f + random.nextFloat() * 0.2f);
        ServerLevel level = (ServerLevel) level();
        kijkers(64, p -> level.sendParticles(p, ParticleTypes.HEART, true, false, player.getX(), player.getY() + 2.2, player.getZ(), 6, 0.8, 0.5, 0.8, 0));
        zing();
        GuhAdvancements.grant(player, "einde2_zwevers_walvis_gevoerd");
        CompoundTag saved = GuhQuests.saved(player);
        long dag = GuhTime.day(level);
        if (saved.getLongOr(PAREL_DAG, Long.MIN_VALUE) == dag) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_zwevers.walvis.morgen").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        saved.putLong(PAREL_DAG, dag);
        Minigames.give(player, new ItemStack(E2ZweversFeature.LEEGTEPAREL.get()));
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_zwevers.walvis.parel").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // --- can't be hurt, pushed, leashed, moved ----------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void knockback(double power, double xd, double zd) {
    }

    @Override
    public boolean isPickable() {
        return false;                                             // (you point at its back: the parts)
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    /** Seen from as far as it is sent (its own box is small: the default would hide it beyond a few dozen blocks). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 320.0 * 320.0;
    }

    // --- save ---------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Koers", koers);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        float k = tag.getFloatOr("Koers", Float.NaN);
        if (!Float.isNaN(k)) {
            koers = doelKoers = k;
            koersGezet = true;
        }
    }

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LeegtewalvisguhEntity>("beweeg", 20, state -> state.setAndContinue(isZingend() ? ZING : IDLE)));
        controllers.add(new AnimationController<LeegtewalvisguhEntity>("actie", 4, state -> PlayState.STOP)
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- a box of its back ----------------------------------------------------------------------------------------------------------

    /**
     * One box of the whale's back (NeoForge part entity: never added to the world or sent by itself, but every collision
     * check and every aim meets it, however far its box lies from the whale's own middle). It carries who {@link #draagt}
     * says, and a click on it is a click on the whale.
     */
    public static class Rug extends PartEntity<LeegtewalvisguhEntity> {
        private static final EntityDimensions MAAT = EntityDimensions.fixed(DEEL_BREED, DEEL_HOOG);

        Rug(LeegtewalvisguhEntity walvis) {
            super(walvis);
            refreshDimensions();
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
        }

        /** Not a Leegtewalvisguh: a search by kind for the whale never answers with its back. */
        @Override
        public EntityType<?> getType() {
            return E2ZweversFeature.LEEGTEWALVISGUH_RUG.get();
        }

        @Override
        public Component getName() {
            return getParent().getName();
        }

        @Override
        public boolean canBeCollidedWith(@Nullable Entity other) {
            return getParent().draagt(other);
        }

        @Override
        public boolean isPickable() {
            return true;
        }

        @Override
        public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
            return getParent().klik(player, hand);
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
            return false;
        }

        @Override
        public boolean is(Entity other) {
            return this == other || getParent() == other;
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return MAAT;
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }
    }
}
