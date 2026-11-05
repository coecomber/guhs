package nl.juiced.guhs.feature.vadsrijk.vriendjes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht;

/**
 * N-EXTRA: the cheered-up, pink Vahoegnir after zone 3 (the new o-draak model, the boss's fully pink texture). Two kinds:
 * <ul>
 *   <li>{@link Soort#NEST}: he rests in the old nest in the crown (calm idle breathing), leashed to the nest POI;</li>
 *   <li>{@link Soort#BEZOEK}: a visit in zone 1 or 2: he flies in along a DraakVlucht spline, lands, stays 60-90 s, flies off.</li>
 * </ul>
 * Only sent to players who finished zone 3 ({@link RozeDraakBeheer#ziet}); others still see the story state. Never hurt, never
 * a target (not attackable, projectiles pass), never saved (the manager respawns him). Right-click = petting him: hearts, a
 * happy rumble, he bows his head, a sweet line. Critters gather around him while he is on the ground ({@link DraakVriendjes}).
 */
public class RozeDraak extends Entity implements GeoEntity {
    public static final float SCHAAL = 1.6f;
    /** The sweet lines (vadsrijk.vriendjes.aai.0..N-1), rotating per player. */
    public static final int REGELS = 6;

    public enum Soort {
        NEST, BEZOEK
    }

    private static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(RozeDraak.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> anims = new HashMap<>();

    Soort soort = Soort.NEST;
    /** The visited player (BEZOEK). */
    @Nullable
    UUID gast;
    /** NEST: where he lies. BEZOEK: the landing spot. */
    Vec3 plek = Vec3.ZERO;
    float rustYaw;
    // a visit: in (path), on the ground (ticks), out (path)
    @Nullable
    List<Vec3> heen, terug;
    int heenTicks = 200, blijf = 1400, terugTicks = 180;
    int t;
    private int animTot = -1;
    private final Map<UUID, Integer> aaiTeller = new HashMap<>();
    private final Map<UUID, Integer> aaiWacht = new HashMap<>();
    /** Gametests/dev: who petted him. */
    public final Set<UUID> geaaid = new java.util.HashSet<>();

    public RozeDraak(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ANIM, "idle");
    }

    public String anim() {
        return entityData.get(ANIM);
    }

    void anim(String a) {
        entityData.set(ANIM, a);
        animTot = -1;
    }

    /** Plays a one-shot ("1:name") for this many ticks, then idle again. */
    void eenmaal(String a, int ticks) {
        entityData.set(ANIM, "1:" + a);
        animTot = t + ticks;
    }

    public Soort soort() {
        return soort;
    }

    /** On the ground (resting or landed): petting works and the critters come. */
    public boolean opGrond() {
        return soort == Soort.NEST || (t > heenTicks && t <= heenTicks + blijf);
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return RozeDraakBeheer.ziet(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        t++;
        if (animTot >= 0 && t >= animTot) {
            anim(opGrond() ? "idle" : "vlieg");
        }
        if (soort == Soort.NEST) {
            setPos(plek.x, plek.y, plek.z);   // leashed to the nest
            if (t % 10 == 0) {
                kijkNaarSpeler();
            }
            return;
        }
        // a visit
        if (t <= heenTicks && heen != null) {
            vliegLangs(heen, t / (double) heenTicks);
            if (t == heenTicks - 20) {
                eenmaal("land", 20);
            }
            if (t == heenTicks) {
                snapTo(plek.x, plek.y, plek.z, getYRot(), 0);
                rustYaw = getYRot();
                anim("idle");
                landWolk();
                DraakVriendjes.volg(this);
            }
        } else if (t <= heenTicks + blijf) {
            setPos(plek.x, plek.y, plek.z);
            if (t % 10 == 0) {
                kijkNaarSpeler();
            }
        } else if (terug != null && t <= heenTicks + blijf + terugTicks) {
            if (t == heenTicks + blijf + 1) {
                anim("vlieg");
                ((ServerLevel) level()).playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 4f, 1.1f);
            }
            vliegLangs(terug, (t - heenTicks - blijf) / (double) terugTicks);
        } else {
            discard();
        }
    }

    private void vliegLangs(List<Vec3> pad, double f) {
        Vec3 pos = DraakVlucht.punt(pad, f), verder = DraakVlucht.punt(pad, Math.min(1, f + 0.01));
        Vec3 r = verder.subtract(pos);
        float yaw = r.horizontalDistanceSqr() > 1e-6 ? (float) Math.toDegrees(Math.atan2(-r.x, r.z)) : getYRot();
        snapTo(pos.x, pos.y, pos.z, yaw, 0);
        if (!"vlieg".equals(anim()) && animTot < 0) {
            anim("vlieg");
        }
        if (t % 30 == 0 && level() instanceof ServerLevel sl) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 3f, 1.2f);
        }
    }

    private void landWolk() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 40, 3, 0.2, 3, 0.05);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 30, 4, 1.5, 4, 0.02);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 1.5f, 1.8f);
        }
    }

    /** Slowly turns his head-and-body a little toward the nearest player who sees him (at most 35 degrees off his rest pose). */
    private void kijkNaarSpeler() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        ServerPlayer dichtst = null;
        double best = 24 * 24;
        for (ServerPlayer p : sl.players()) {
            double d = p.distanceToSqr(this);
            if (d < best && broadcastToPlayer(p)) {
                best = d;
                dichtst = p;
            }
        }
        float doel = rustYaw;
        if (dichtst != null) {
            Vec3 r = dichtst.position().subtract(position());
            float naar = (float) Math.toDegrees(Math.atan2(-r.x, r.z));
            float verschil = net.minecraft.util.Mth.wrapDegrees(naar - rustYaw);
            doel = rustYaw + Math.max(-35, Math.min(35, verschil));
        }
        float nu = getYRot();
        setYRot(nu + net.minecraft.util.Mth.wrapDegrees(doel - nu) * 0.15f);
    }

    // ---- petting -------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        return aai(sp) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** Petting: hearts, a happy rumble, a bow, a sweet line (rotating). False when not on the ground / not for this player / too soon. */
    public boolean aai(ServerPlayer p) {
        if (!opGrond() || !broadcastToPlayer(p) || !(level() instanceof ServerLevel sl)) {
            return false;
        }
        int nu = t;
        if (aaiWacht.getOrDefault(p.getUUID(), -1000) > nu) {
            return false;
        }
        aaiWacht.put(p.getUUID(), nu + 30);
        geaaid.add(p.getUUID());
        int i = aaiTeller.merge(p.getUUID(), 1, Integer::sum) - 1;
        Vec3 kop = position().add(Vec3.directionFromRotation(0, getYRot()).scale(3.5)).add(0, 4.5, 0);
        sl.sendParticles(ParticleTypes.HEART, kop.x, kop.y, kop.z, 8, 1.2, 0.8, 1.2, 0);
        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 2, 0.3, 0.2, 0.3, 0);
        sl.playSound(null, kop.x, kop.y, kop.z, SoundEvents.SNIFFER_HAPPY, SoundSource.NEUTRAL, 2.5f, 0.6f);
        sl.playSound(null, kop.x, kop.y, kop.z, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 0.6f, 1.9f);
        if (!anim().startsWith("1:")) {
            eenmaal("buig", 48);
        }
        p.sendOverlayMessage(Component.translatable("vadsrijk.vriendjes.aai." + (i % REGELS)).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // ---- never hurt, never a target, never saved ---------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return opGrond();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
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
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 320 * 320;
    }

    public AABB cullDoos() {
        return getBoundingBox().inflate(10, 6, 10);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    private RawAnimation raw(String a) {
        return anims.computeIfAbsent(a, k -> k.startsWith("1:") ? RawAnimation.begin().thenPlayAndHold(k.substring(2)) : RawAnimation.begin().thenLoop(k));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RozeDraak>("anim", 6, s -> s.setAndContinue(raw(s.animatable().anim()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
