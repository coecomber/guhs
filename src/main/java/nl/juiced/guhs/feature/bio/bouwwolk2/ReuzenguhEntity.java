package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * De reuzenguh (biomes3): the giant of the wolkenkasteeltje, a guh five times the size of a guh, fast asleep on his cloud
 * bed. A parody of the giant at the top of the beanstalk, and a sweet one: he never attacks, never gets up, never leaves,
 * and nothing can hurt or move him.
 * <p>
 * <b>The game</b> is to get past him to the hoard at the back of his hall ({@link SchatBlock}). He hears ({@link Lawaai}):
 * running, jumping and breaking blocks anywhere in the hall ({@link Kasteel#inHal}), and walking upright close to his head
 * ({@link Kasteel#bijOor}); sneaking is always quiet.
 * <ol>
 *   <li>{@link Staat#SLAAP}: the first noise makes him stir: {@link Staat#LOER}, one eye open, a grumbled "...njeg?". The
 *       player who made it is told to sneak. For {@link #GENADE} ticks he hears nothing more (time to stop running).</li>
 *   <li>{@link Staat#LOER}, for {@link #LOER_TICKS}: quiet until then and he sleeps on. Noise again: {@link Staat#NIES}.</li>
 *   <li>{@link Staat#NIES}: he draws breath and sneezes; {@link #BLAAS_OP} ticks in, everybody who was noisy while he peeked
 *       is blown out of the castle, onto the cloud in front of the gate ({@link Kasteel#LANDING}): set down softly, falling
 *       like a feather, unharmed. After {@link #NIES_TICKS} he is asleep again, as if nothing happened.</li>
 * </ol>
 * He keeps his place himself ({@link #thuis}); {@link Bewakers} puts a new one on the bed when he is gone and removes a
 * second one. The model is the guh's own (its ten plain bones), drawn {@link #SCHAAL} times as big; the snoring, the "zzz"
 * and the puff of his sneeze are made on the client from his synced {@link #staat()}.
 */
public class ReuzenguhEntity extends Mob implements GeoEntity {
    public enum Staat { SLAAP, LOER, NIES }

    public enum Lawaai { RENNEN, SPRINGEN, BREKEN, STAMPEN }

    private static final EntityDataAccessor<Integer> DATA_STAAT = SynchedEntityData.defineId(ReuzenguhEntity.class, EntityDataSerializers.INT);
    /** How many times a guh he is. */
    public static final float SCHAAL = 5f;
    /** He peeks this long; the first GENADE ticks of it he hears nothing. */
    public static final int LOER_TICKS = 120, GENADE = 30;
    /** The sneeze: the gust comes BLAAS_OP ticks in, and he is asleep again after NIES_TICKS. */
    public static final int NIES_TICKS = 30, BLAAS_OP = 15;
    /** Players this close are listened to (the hall is much smaller). */
    private static final double LUISTER = 28;
    /** Blown out: you float down for this long (the landing is a cloud two steps away; this is for when it is gone). */
    public static final int ZWEEF_TICKS = 100;
    public static final int WEG_REGELS = 4, SLAAP_REGELS = 3;

    private static final RawAnimation SLAAP = RawAnimation.begin().thenLoop("slaap");
    private static final RawAnimation LOER = RawAnimation.begin().thenLoop("loer");
    private static final RawAnimation NIES = RawAnimation.begin().thenPlayAndHold("nies");
    /** The giants that are awake in a level right now (server side; for the block break listener). */
    private static final Set<ReuzenguhEntity> GELADEN = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Where he lies and which way (saved); null until his first tick. */
    @Nullable
    private Vec3 thuis;
    private float thuisYaw;
    private int staatTicks;
    /** Who was noisy while he peeked (they are the ones blown out). */
    private final Set<UUID> herrie = new HashSet<>();
    /** Where each nearby player was a tick ago: x, y, z, 1 when on the ground. */
    private final Map<UUID, double[]> sporen = new HashMap<>();
    /** (client) the state a tick ago. */
    private Staat vorige = Staat.SLAAP;

    public ReuzenguhEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        setSilent(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    // --- state --------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAAT, 0);
    }

    public Staat staat() {
        return Staat.values()[Math.floorMod(entityData.get(DATA_STAAT), Staat.values().length)];
    }

    private void zetStaat(Staat staat) {
        entityData.set(DATA_STAAT, staat.ordinal());
        staatTicks = 0;
    }

    /** Where he lies (his own position until his first tick). */
    public Vec3 thuis() {
        return thuis == null ? position() : thuis;
    }

    public Rotation draai() {
        return Kasteel.draai(thuis == null ? getYRot() : thuisYaw);
    }

    /** Puts him here for good, the castle turned like this (a new giant, tests). */
    public void zetThuis(Vec3 plek, Rotation draai) {
        thuis = plek;
        thuisYaw = Kasteel.yaw(draai);
        snapTo(plek.x, plek.y, plek.z, thuisYaw, 0);
        setYBodyRot(thuisYaw);
        setYHeadRot(thuisYaw);
    }

    public boolean inHal(Vec3 plek) {
        return Kasteel.inHal(thuis(), draai(), plek);
    }

    /** Where he blows people to. */
    public Vec3 landing() {
        return Kasteel.wereld(thuis(), draai(), Kasteel.LANDING);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (thuis != null) {
            tag.putDouble("ThuisX", thuis.x);
            tag.putDouble("ThuisY", thuis.y);
            tag.putDouble("ThuisZ", thuis.z);
            tag.putFloat("ThuisYaw", thuisYaw);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        // (a giant out of a structure template has no home yet: the template is turned, so he takes it from where he lands)
        if (tag.getDoubleOr("ThuisY", Double.NaN) == tag.getDoubleOr("ThuisY", 0.0)) {
            thuis = new Vec3(tag.getDoubleOr("ThuisX", 0), tag.getDoubleOr("ThuisY", 0), tag.getDoubleOr("ThuisZ", 0));
            thuisYaw = tag.getFloatOr("ThuisYaw", Kasteel.REUS_YAW);
        }
        entityData.set(DATA_STAAT, 0);
    }

    // --- the ticking --------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientTick();
            return;
        }
        if (thuis == null) {
            // a giant from a template stands where the template put him, turned with it
            thuisYaw = Kasteel.yaw(Kasteel.draai(getYRot()));
            thuis = position();
        }
        if (position().distanceToSqr(thuis) > 1.0E-4 || getYRot() != thuisYaw) {
            snapTo(thuis.x, thuis.y, thuis.z, thuisYaw, 0);
        }
        setYBodyRot(thuisYaw);
        setYHeadRot(thuisYaw);
        setDeltaMovement(Vec3.ZERO);
        GELADEN.add(this);
        staatTicks++;
        luister();
        switch (staat()) {
            case LOER -> {
                if (staatTicks >= LOER_TICKS) {
                    herrie.clear();
                    zetStaat(Staat.SLAAP);
                }
            }
            case NIES -> {
                if (staatTicks == BLAAS_OP) {
                    blaas();
                }
                if (staatTicks >= NIES_TICKS) {
                    herrie.clear();
                    zetStaat(Staat.SLAAP);
                }
            }
            default -> {
            }
        }
    }

    /** Listens to the players in the hall: who runs, who jumps, who walks upright past his ear. */
    private void luister() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 hier = thuis();
        Rotation draai = draai();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(hier) > LUISTER * LUISTER) {
                sporen.remove(p.getUUID());
                continue;
            }
            double[] was = sporen.get(p.getUUID());
            double[] nu = {p.getX(), p.getY(), p.getZ(), p.onGround() ? 1 : 0};
            sporen.put(p.getUUID(), nu);
            if (was == null || !Kasteel.inHal(hier, draai, p.position())) {
                continue;
            }
            double dx = nu[0] - was[0], dy = nu[1] - was[1], dz = nu[2] - was[2];
            boolean loopt = dx * dx + dz * dz > 1.0E-4;
            if (loopt && dx * dx + dz * dz > 16) {
                continue;                                   // (teleported, not walked)
            }
            if (was[3] == 1 && nu[3] == 0 && dy > 0.2 && !p.getAbilities().flying) {
                hoor(p, Lawaai.SPRINGEN);
            } else if (loopt && p.isSprinting()) {
                hoor(p, Lawaai.RENNEN);
            } else if (loopt && !p.isCrouching() && !p.getAbilities().flying && Kasteel.bijOor(hier, draai, p.position())) {
                hoor(p, Lawaai.STAMPEN);
            }
        }
        if (tickCount % 200 == 0) {
            sporen.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        }
    }

    /**
     * This player made a noise in the hall. Asleep: he stirs and the player is warned. Peeking (after the grace): he
     * sneezes, and this player is one of those who go out. Sneezing: he hears nothing.
     */
    public void hoor(ServerPlayer speler, Lawaai lawaai) {
        switch (staat()) {
            case SLAAP -> {
                herrie.clear();
                zetStaat(Staat.LOER);
                GuhQuests.say(speler, this, "gui.guhs.reuzenguh.loer");
                speler.sendOverlayMessage(Component.translatable("gui.guhs.reuzenguh.loer.tip").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case LOER -> {
                if (staatTicks >= GENADE) {
                    herrie.add(speler.getUUID());
                    zetStaat(Staat.NIES);
                }
            }
            case NIES -> {
                if (staatTicks < BLAAS_OP) {
                    herrie.add(speler.getUUID());
                }
            }
        }
    }

    /** The gust: everybody who was noisy goes out of the castle. */
    private void blaas() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (UUID id : herrie) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer p && p.distanceToSqr(thuis()) < LUISTER * LUISTER) {
                blaasWeg(p);
            }
        }
        herrie.clear();
    }

    /** Sets this player down on the cloud in front of the gate: softly, unharmed, with a line about it. */
    public void blaasWeg(ServerPlayer speler) {
        Vec3 l = landing();
        speler.stopRiding();
        float kijk = Kasteel.yaw(draai()) + 90f;                       // (back to the gate: you look at where you came from)
        speler.teleportTo(speler.level(), l.x, l.y + 0.3, l.z, Set.of(), kijk, 0f, true);
        speler.setDeltaMovement(Vec3.ZERO);
        speler.resetFallDistance();
        speler.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ZWEEF_TICKS, 0, false, false));
        speler.sendSystemMessage(Component.translatable("gui.guhs.reuzenguh.weg." + getRandom().nextInt(WEG_REGELS)).withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(speler, "reuzenguh_weggeblazen");
        nl.juiced.guhs.feature.bio.systemen.Bewijzen.nies(speler);   // biomes3 systemen: the count for the title "Gezondheid!"
        sporen.remove(speler.getUUID());
    }

    /** A block was broken: in a hall, the giant there hears it. */
    public static void onBreek(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer speler) || GELADEN.isEmpty()) {
            return;
        }
        Vec3 plek = Vec3.atCenterOf(event.getPos());
        ReuzenguhEntity[] reuzen;
        synchronized (GELADEN) {
            reuzen = GELADEN.toArray(new ReuzenguhEntity[0]);
        }
        for (ReuzenguhEntity reus : reuzen) {
            if (reus.isAlive() && reus.level() == speler.level() && reus.inHal(plek)) {
                reus.hoor(speler, Lawaai.BREKEN);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        GELADEN.remove(this);
        super.remove(reason);
    }

    // --- the client: snoring, zzz, the puff ---------------------------------------------------------------------------------

    private Vec3 kop() {
        return Kasteel.wereld(position(), Kasteel.draai(getYRot()), Kasteel.KOP);
    }

    private void clientTick() {
        Staat nu = staat();
        Level level = level();
        Vec3 kop = kop();
        if (nu != vorige) {
            if (nu == Staat.LOER) {
                level.playLocalSound(kop.x, kop.y, kop.z, BouwWolk2Slice.GROM.get(), SoundSource.NEUTRAL, 0.9f, 0.9f + random.nextFloat() * 0.15f, false);
            } else if (nu == Staat.NIES) {
                level.playLocalSound(kop.x, kop.y, kop.z, BouwWolk2Slice.NIES.get(), SoundSource.NEUTRAL, 1.0f, 0.95f + random.nextFloat() * 0.1f, false);
            }
            vorige = nu;
            staatTicks = 0;
        }
        staatTicks++;
        if (nu == Staat.SLAAP) {
            int t = tickCount % 84;
            if (t == 0) {
                level.playLocalSound(kop.x, kop.y, kop.z, BouwWolk2Slice.SNURK.get(), SoundSource.NEUTRAL, 0.55f, 0.92f + random.nextFloat() * 0.12f, false);
            }
            if (t == 34 || t == 46 || t == 58) {
                // three z's on the breath out, drifting up from his nose
                level.addParticle(BouwWolk2Slice.ZZZ.get(), kop.x + (random.nextDouble() - 0.5) * 0.6, kop.y + 1.4, kop.z + (random.nextDouble() - 0.5) * 0.6,
                        (t - 34) / 12.0, 0.035, 0);
            }
        } else if (nu == Staat.NIES && staatTicks >= BLAAS_OP - 1 && staatTicks <= BLAAS_OP + 5) {
            // the gust: a roll of cloud from his nose to the gate
            Vec3 weg = Kasteel.draai(Kasteel.NAAR_POORT, Kasteel.draai(getYRot()));
            for (int i = 0; i < 14; i++) {
                double s = 0.25 + random.nextDouble() * 0.55;
                level.addParticle(ParticleTypes.CLOUD, kop.x + (random.nextDouble() - 0.5) * 2.4, kop.y - 1.0 + random.nextDouble() * 1.6,
                        kop.z + (random.nextDouble() - 0.5) * 2.4, weg.x * s + (random.nextDouble() - 0.5) * 0.12, 0.02 + random.nextDouble() * 0.05,
                        weg.z * s + (random.nextDouble() - 0.5) * 0.12);
            }
        }
    }

    // --- he cannot be hurt, pushed, led away or lost ------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer speler && hand == InteractionHand.MAIN_HAND && staat() == Staat.SLAAP) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.reuzenguh.slaapt." + getRandom().nextInt(SLAAP_REGELS)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity other) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    protected void doPush(Entity other) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {
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
    protected void registerGoals() {
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    // --- GeckoLib ----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("lig", 8, state -> state.setAndContinue(switch (staat()) {
            case LOER -> LOER;
            case NIES -> NIES;
            default -> SLAAP;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
