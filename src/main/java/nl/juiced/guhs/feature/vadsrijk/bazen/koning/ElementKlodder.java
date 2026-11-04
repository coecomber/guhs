package nl.juiced.guhs.feature.vadsrijk.bazen.koning;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * One of Koning Klodder's three Kroonklodders (roze = Roze, blauw = Water/Slijm, geel = Vuur). Its damage counts for the king's
 * contribution; cheering it up tells the king ({@link KoningKlodder#klodderVerslagen}). Not saved: after a reload the king
 * simply stands whole again.
 */
public class ElementKlodder extends VadsrijkMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID koning;
    private int index;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SCHILD = net.minecraft.network.syncher.SynchedEntityData.defineId(
            ElementKlodder.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    /** Hits of the wrong element the shield still takes before it cracks anyway (so a solo player without the element can do it). */
    public static final int SCHILD_KLAPPEN = 8;
    private int schildKlappen;
    private int hopT = 30;

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SCHILD, false);
    }

    /** The shielded Kroonklodder (fix round 1): only its weak element breaks the shield at once. */
    public void zetSchild(boolean aan) {
        entityData.set(SCHILD, aan);
        schildKlappen = 0;
    }

    public boolean schild() {
        return entityData.get(SCHILD);
    }

    /** The element that breaks this one's shield (what beats its element; Roze: none). */
    @Nullable
    public Element zwakte() {
        for (Element e : Element.values()) {
            if (e.verslaat() == element()) {
                return e;
            }
        }
        return null;
    }

    /** Kern damage hook (registered once by BazenRegistry): a shielded Kroonklodder takes nothing except from its weak element. */
    public static void registreerSchild() {
        nl.juiced.guhs.feature.vadsrijk.kern.Schade.VOOR_KLAP.add((k, n) -> {
            if (!(k.doel() instanceof ElementKlodder e) || !e.schild() || !(e.level() instanceof ServerLevel sl)) {
                return n;
            }
            if (k.element() == e.zwakte() || ++e.schildKlappen >= SCHILD_KLAPPEN) {
                e.zetSchild(false);
                sl.playSound(null, e.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BREAK.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 1f);
                sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.8, e.getZ(), 30, 0.6, 0.6, 0.6, 0.3);
                return n;
            }
            sl.playSound(null, e.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1.2f);
            if (k.bron() instanceof ServerPlayer p) {
                Element z = e.zwakte();
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.bazen.kk.schild",
                        z == null ? net.minecraft.network.chat.Component.literal("?") : net.minecraft.network.chat.Component.translatable(z.key()).withStyle(z.kleur),
                        SCHILD_KLAPPEN - e.schildKlappen).withStyle(net.minecraft.ChatFormatting.AQUA));
            }
            return 0;
        });
    }

    public ElementKlodder(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public void koppel(KoningKlodder k, int index, double vads) {
        this.koning = k.getUUID();
        this.index = index;
        zetMaxVads(vads, false);
        heal(vads);
    }

    public int index() {
        return index;
    }

    @Override
    protected double basisVads(int level) {
        return 200 + 40 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 6 + 1.6 * level;
    }

    @Override
    public int xp() {
        return 0;
    }

    @Nullable
    @Override
    public String lootTabel() {
        return null;
    }

    @Override
    public Element standaardElement() {
        return Element.ROZE;
    }

    @Nullable
    private KoningKlodder koning() {
        return koning != null && level() instanceof ServerLevel sl && sl.getEntity(koning) instanceof KoningKlodder k ? k : null;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        KoningKlodder k = koning();
        if (k != null && bron instanceof ServerPlayer p && !geplofd()) {
            k.bijdragen().schade(p.getUUID(), Math.min(n, vads()));
        }
        super.krijgSchade(n, bron);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        KoningKlodder k = koning();
        if (!was && k != null) {
            k.klodderVerslagen(index);
        }
    }

    @Override
    protected void beloon(ServerLevel sl, java.util.Set<ServerPlayer> helpers) {
        // no xp/loot of its own: the king's chest is the reward
    }

    /** Melts back into the king (grey drops flying to him), no plof. */
    public void smelt(KoningKlodder k) {
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 8; i++) {
                double t = i / 8.0;
                sl.sendParticles(ParticleTypes.ITEM_SLIME, getX() + (k.getX() - getX()) * t, getY() + 0.5 + (k.getY() + 2 - getY()) * t,
                        getZ() + (k.getZ() - getZ()) * t, 3, 0.1, 0.1, 0.1, 0.01);
            }
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        KoningKlodder k = koning();
        if (tickCount > 40 && k == null && !geplofd()) {
            discard();   // (orphaned after a reload or a reset)
            return;
        }
        if (geplofd() || k == null || stil()) {
            return;
        }
        if (schild() && tickCount % 4 == 0) {   // the visible shield: a ring in the colour of its weak element
            Element z = zwakte();
            var stof = new net.minecraft.core.particles.DustParticleOptions(z == null ? 0xFFFFFF : z.rgb, 1.2f);
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10 + tickCount * 0.1;
                sl.sendParticles(stof, getX() + Math.cos(a) * 1.1, getY() + 0.6 + Math.sin(a * 2) * 0.3, getZ() + Math.sin(a) * 1.1, 1, 0, 0, 0, 0);
            }
        }
        // fix round 1: hop at the king's fighters (also the ones far away or in creative), not only vanilla targets
        if (onGround() && --hopT <= 0) {
            hopT = 26 + getRandom().nextInt(16);
            ServerPlayer doel = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : k.vechters()) {
                if (p.distanceToSqr(this) < best) {
                    best = p.distanceToSqr(this);
                    doel = p;
                }
            }
            if (doel != null && best > 2.5 * 2.5) {
                net.minecraft.world.phys.Vec3 d = doel.position().subtract(position()).multiply(1, 0, 1);
                double len = Math.min(1.0, d.length() / 8.0);
                d = d.normalize().scale(0.35 + 0.45 * len);
                setDeltaMovement(d.x, 0.55, d.z);
                getLookControl().setLookAt(doel, 30, 30);
            } else if (doel != null && tickCount % 2 == 0) {
                aanval(doel);
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk");
        controllers.add(new AnimationController<>("beweeg", 3, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
