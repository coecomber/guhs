package nl.juiced.guhs.feature.vadsrijk.bazen.mika;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The Mika-spion's escape balloon: rises slowly with him dangling below. Pop it (arrows, magic, or a quick hit while it is still
 * low) before it is {@link #ONTSNAP_TICKS} old, or he gets away. Not saved.
 */
public class SpionBallon extends VadsrijkMob implements GeoEntity {
    public static final int ONTSNAP_TICKS = 400;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID mika;
    private int leeftijd;

    public SpionBallon(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void koppel(MikaSpion m, double vads) {
        this.mika = m.getUUID();
        zetMaxVads(vads, false);
        heal(vads);
        zetOntvadst(false);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected double basisVads(int level) {
        return 150 + 20 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 0;
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
        return Element.GEEN;
    }

    @Nullable
    private MikaSpion mika() {
        return mika != null && level() instanceof ServerLevel sl && sl.getEntity(mika) instanceof MikaSpion m ? m : null;
    }

    public int leeftijd() {
        return leeftijd;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || geplofd()) {
            return;
        }
        leeftijd++;
        MikaSpion m = mika();
        if (m == null) {
            discard();
            return;
        }
        setDeltaMovement(Math.sin(leeftijd * 0.05) * 0.02, 0.045, Math.cos(leeftijd * 0.04) * 0.02);
        if (leeftijd >= ONTSNAP_TICKS) {
            m.ontsnapt();
            discard();
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        MikaSpion m = mika();
        if (!was && m != null && level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.8f);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.5, getZ(), 40, 0.8, 0.8, 0.8, 0.1);
            m.ballonKapot(laatste);
        }
    }

    @Override
    protected void beloon(ServerLevel sl, java.util.Set<net.minecraft.server.level.ServerPlayer> helpers) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle");
        controllers.add(new AnimationController<>("beweeg", 3, state -> state.setAndContinue(idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
