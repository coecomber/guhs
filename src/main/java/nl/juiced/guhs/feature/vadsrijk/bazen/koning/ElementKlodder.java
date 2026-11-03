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
        if (!level().isClientSide() && tickCount > 40 && koning() == null && !geplofd()) {
            discard();   // (orphaned after a reload or a reset)
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
