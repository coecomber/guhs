package nl.juiced.guhs.feature.ringh3;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * bbq2 (ring-h3): de Barbecuerog (entity {@code guhs:barbecuerog}, the fixed id of CONTRACT_130 7): a towering horned demon of
 * black charcoal and glowing embers with a burning mane, wings of smoke, a whip of knotted braadworstjes and a blade of fire
 * (model, textures, animations: tools/features/ring_h3_modellen.py). He looks and sounds dangerous and he never hurts anybody:
 * his stomp SHOVES ({@link Duwtje}) and his whip puts whoever he catches back at their last rest fire
 * ({@link Ring#terugNaarRustpunt}); nothing is lost. He can't be hurt or pushed and is never saved.
 * <ul>
 *   <li><b>The chase</b> ({@link Achtervolging}): one of him per player (only that player's game knows about him), woken when
 *       that player walks into the great hall: he rises out of the Diepe Poort ({@link #OPKOMST}), strides down the nave
 *       ({@link #LOOPT}) to the edge of the chasm ({@link #STAAT_AAN_DE_RAND}), stomping and roaring on the way.</li>
 *   <li><b>In the bridge scene</b> he is an actor of the viewer's own game: the scene names his animations
 *       ({@code slaap, donker, opkomst, loop, brul, zwaard, zweep, stamp, wankel, dreig}; slaap is donker with his eyes shut), read
 *       here from {@link Cutscenes#animatie}.</li>
 * </ul>
 */
public class BarbecuerogEntity extends Mob implements GeoEntity {
    /** What he is doing (synced: the looping animation). */
    public static final int RUST = 0, LOOPT = 1, DONKER = 2;
    /** The phases of a chase. */
    public static final int SLAAPT = 0, OPKOMST = 1, JAAGT = 2, STAAT_AAN_DE_RAND = 3;
    /** Ticks of the rise out of the dark (the animation takes 7 s), blocks per tick of his walk, reach of the whip, reach of a stomp. */
    public static final int OPKOMST_TICKS = 150;
    public static final double SNELHEID = 0.11, ZWEEP_BEREIK = 7.5, STAMP_BEREIK = 13;
    /** Ticks between two stomps, and from the start of a whip lash to the catch. */
    public static final int STAMP_OM = 170, ZWEEP_TICKS = 13;
    public static final String REDEN = "barbecuerog";

    private static final EntityDataAccessor<Integer> DATA_STAAT = SynchedEntityData.defineId(BarbecuerogEntity.class, EntityDataSerializers.INT);
    /** A counter in the high bits, a strength (0..15) in the low four: every change shakes the ground under whoever is near. */
    private static final EntityDataAccessor<Integer> DATA_SCHOK = SynchedEntityData.defineId(BarbecuerogEntity.class, EntityDataSerializers.INT);

    private static final String A = "animation.barbecuerog.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle"), LOOP = RawAnimation.begin().thenLoop(A + "loop"),
            DONKER_ANIM = RawAnimation.begin().thenLoop(A + "donker"), SLAAP_ANIM = RawAnimation.begin().thenLoop(A + "slaap"),
            WANKEL = RawAnimation.begin().thenLoop(A + "wankel"), DREIG = RawAnimation.begin().thenLoop(A + "dreig");
    /** The animations that play once (on the controller "actie"). */
    public static final Set<String> EENMALIG = Set.of("brul", "stamp", "zweep", "zwaard", "opkomst");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // --- the chase (server) ---
    @Nullable
    private UUID prooi;
    @Nullable
    private Vec3 einde;
    private int fase = SLAAPT, faseTicks, stampOver = STAMP_OM, zweepOver = -1, schokTeller;
    // --- a scene (client) ---
    private String sceneNaam = "";
    private int sceneTicks, laatsteSchok;

    public BarbecuerogEntity(EntityType<? extends BarbecuerogEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAAT, RUST);
        builder.define(DATA_SCHOK, 0);
    }

    @Override
    protected void registerGoals() {
        // (no goals: a chase is scripted, see aiStep)
    }

    public int staat() {
        return this.entityData.get(DATA_STAAT);
    }

    public void zetStaat(int staat) {
        if (staat() != staat) {
            this.entityData.set(DATA_STAAT, staat);
        }
    }

    public int fase() {
        return fase;
    }

    @Nullable
    public UUID prooi() {
        return prooi;
    }

    /** Do his flames burn (then he carries light and sheds embers)? Not while he sleeps in the dark. */
    public boolean brandt() {
        String scene = level().isClientSide() ? Cutscenes.animatie(this) : "";
        if (scene.equals("donker") || scene.equals("slaap")) {
            return false;
        }
        if (scene.equals("opkomst")) {
            return Cutscenes.animatieTicks(this) > 26;
        }
        return !scene.isEmpty() || staat() != DONKER;
    }

    // =====================================================================================================================
    // the chase (server)
    // =====================================================================================================================

    /** Starts a chase of this player: he sleeps in the dark where he stands, wakes, and walks to {@code einde} (a world position). */
    public void jaag(ServerPlayer speler, Vec3 einde) {
        this.prooi = speler.getUUID();
        this.einde = einde;
        this.fase = SLAAPT;
        this.faseTicks = 0;
        zetStaat(DONKER);
        kijkNaar(einde);
    }

    @Nullable
    private ServerPlayer prooiSpeler() {
        return prooi != null && level().getPlayerByUUID(prooi) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() ? p : null;
    }

    private void kijkNaar(Vec3 doel) {
        float yaw = (float) (Mth.atan2(doel.z - getZ(), doel.x - getX()) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    private void schok(int kracht) {
        schokTeller++;
        this.entityData.set(DATA_SCHOK, (schokTeller << 4) | Mth.clamp(kracht, 0, 15));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel level) || prooi == null || einde == null) {
            return;
        }
        ServerPlayer p = prooiSpeler();
        if (p == null || p.level() != level || !Achtervolging.geldt(p, this)) {
            verdwijn();
            return;
        }
        faseTicks++;
        switch (fase) {
            case SLAAPT -> {
                // two eyes in the dark for a moment, then he wakes
                if (faseTicks >= 30) {
                    fase = OPKOMST;
                    faseTicks = 0;
                    triggerAnim("actie", "opkomst");
                    zetStaat(RUST);
                    playSound(RingH3Feature.ONTBRAND.get(), 3.0f, 0.8f);
                }
            }
            case OPKOMST -> {
                if (faseTicks == 96) {
                    playSound(RingH3Feature.BRUL.get(), 6.0f, 1.0f);
                    schok(12);
                    p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("quest.guhs.ringh3.rog.vlucht")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
                if (faseTicks >= OPKOMST_TICKS) {
                    fase = JAAGT;
                    faseTicks = 0;
                    zetStaat(LOOPT);
                }
            }
            case JAAGT -> jaagTick(level, p);
            default -> {
                kijkNaar(p.position());
                if (faseTicks % 220 == 60) {
                    triggerAnim("actie", "brul");
                    playSound(RingH3Feature.BRUL.get(), 6.0f, 0.9f + random.nextFloat() * 0.2f);
                    schok(8);
                }
            }
        }
        if (fase >= JAAGT) {
            zweepTick(p);
        }
    }

    private void jaagTick(ServerLevel level, ServerPlayer p) {
        if (stampOver > 0 && --stampOver <= 0) {
            // a stomp: he stands still for it; the shock wave shoves his prey (never anybody else, never damage)
            stampOver = -34;
            triggerAnim("actie", "stamp");
            zetStaat(RUST);
        }
        if (stampOver < 0) {
            stampOver++;
            if (stampOver == -34 + 15) {
                playSound(RingH3Feature.STAP.get(), 5.0f, 0.5f);
                schok(11);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 40, 2.4, 0.2, 2.4, 0.04);
                level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 14, 2.0, 0.1, 2.0, 0.0);
                if (p.distanceToSqr(this) <= STAMP_BEREIK * STAMP_BEREIK && Duwtje.mag(p)) {
                    Duwtje.duw(p, p.position().subtract(position()), 0.9);
                }
            }
            if (stampOver == 0) {
                stampOver = STAMP_OM;
                zetStaat(LOOPT);
            }
            return;
        }
        Vec3 naar = einde.subtract(position());
        double afstand = naar.horizontalDistance();
        if (afstand < 0.3) {
            fase = STAAT_AAN_DE_RAND;
            faseTicks = 0;
            zetStaat(RUST);
            return;
        }
        Vec3 stap = new Vec3(naar.x, 0, naar.z).normalize().scale(Math.min(SNELHEID, afstand));
        kijkNaar(einde);
        setPos(getX() + stap.x, getY(), getZ() + stap.z);
        if (faseTicks % 24 == 0) {
            playSound(RingH3Feature.STAP.get(), 3.0f, 0.7f);
            schok(4);
        }
    }

    /** The whip: his prey within reach? A lash, and a moment later it has them: back to the rest fire, nothing lost. */
    private void zweepTick(ServerPlayer p) {
        if (zweepOver >= 0) {
            if (--zweepOver <= 0) {
                zweepOver = -1;
                if (binnenBereik(p) && Duwtje.mag(p)) {
                    Achtervolging.gepakt(p, this);
                }
            }
            return;
        }
        if (stampOver >= 0 && binnenBereik(p) && Duwtje.mag(p)) {
            zweepOver = ZWEEP_TICKS;
            kijkNaar(p.position());
            triggerAnim("actie", "zweep");
            playSound(RingH3Feature.ZWEEP.get(), 4.0f, 1.0f);
        }
    }

    private boolean binnenBereik(ServerPlayer p) {
        double dx = p.getX() - getX(), dz = p.getZ() - getZ(), dy = p.getY() - getY();
        return dx * dx + dz * dz <= ZWEEP_BEREIK * ZWEEP_BEREIK && dy > -4 && dy < 9;
    }

    /** Gone in fire and smoke. */
    public void verdwijn() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 5.0, getZ(), 80, 2.0, 3.5, 2.0, 0.03);
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 5.0, getZ(), 60, 1.6, 3.0, 1.6, 0.05);
        }
        discard();
    }

    // =====================================================================================================================
    // the client: what a scene asks, fire and embers
    // =====================================================================================================================

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        String scene = Cutscenes.animatie(this);
        int ticks = Cutscenes.animatieTicks(this);
        if (!scene.equals(sceneNaam) || ticks < sceneTicks) {
            sceneNaam = scene;
            if (EENMALIG.contains(scene)) {
                triggerAnim("actie", scene);
            }
        }
        sceneTicks = ticks;
        int schok = this.entityData.get(DATA_SCHOK);
        if (schok != laatsteSchok) {
            laatsteSchok = schok;
            nl.juiced.guhs.feature.ringh3.client.RingH3Client.schok(this, schok & 15);
        }
        nl.juiced.guhs.feature.ringh3.client.RingH3Client.vuur(this);
    }

    private PlayState lus(com.geckolib.animation.state.AnimationTest<BarbecuerogEntity> state) {
        RawAnimation anim = switch (sceneNaam) {
            case "loop" -> LOOP;
            case "donker" -> DONKER_ANIM;
            case "slaap" -> SLAAP_ANIM;
            case "wankel" -> WANKEL;
            case "dreig" -> DREIG;
            case "" -> staat() == LOOPT ? LOOP : staat() == DONKER ? DONKER_ANIM : IDLE;
            default -> IDLE;
        };
        return state.setAndContinue(anim);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BarbecuerogEntity>("main", 8, this::lus));
        AnimationController<BarbecuerogEntity> actie = new AnimationController<BarbecuerogEntity>("actie", 4, state -> PlayState.STOP);
        for (String naam : EENMALIG) {
            actie.triggerableAnim(naam, RawAnimation.begin().thenPlay(A + naam));
        }
        controllers.add(actie);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- nothing hurts him, nothing moves him -----------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return staat() == DONKER ? null : RingH3Feature.GROM.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 90;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0f;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 200 * 200;
    }
}
