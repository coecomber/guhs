package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel;

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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas;

/**
 * One of Vrouwe Spiegelzucht's mirror images: looks exactly like her (same model, no nameplate) but it HAS a shadow when the
 * light falls on it. Hitting it shatters it and the mirror strikes back at you ({@link Spiegelzucht#beeldGeraakt}). It throws
 * shards like she does, so you can't tell them apart by what they do. Never saved.
 */
public class Spiegelbeeld extends VadsrijkMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID vrouwe;
    private int worpT;

    public Spiegelbeeld(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public void koppel(Spiegelzucht s) {
        vrouwe = s.getUUID();
        stelIn(s.mobLevel(), s.element(), false, Set.of());
        zetOntvadst(false);
        worpT = 50 + getRandom().nextInt(60);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected double basisVads(int level) {
        return 100;
    }

    @Override
    protected double basisSchade(int level) {
        return 10 + 1.8 * level;
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

    @Nullable
    public Spiegelzucht vrouwe() {
        return vrouwe != null && level() instanceof ServerLevel sl && sl.getEntity(vrouwe) instanceof Spiegelzucht s && !s.geplofd() ? s : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        Spiegelzucht s = vrouwe();
        if (tickCount > 10 && (s == null || !s.gesplitst())) {
            breek(sl);
            return;
        }
        if (s == null) {
            return;
        }
        ServerPlayer d = s.zDoel();
        if (d != null) {
            getLookControl().setLookAt(d, 30, 30);
        }
        if (--worpT <= 0) {
            worpT = 70 + getRandom().nextInt(50);
            s.worpVan(sl, this);
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        Spiegelzucht s = vrouwe();
        if (geplofd() || s == null || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (bron instanceof ServerPlayer p) {
            if (!s.zDoetMee(p)) {
                return;
            }
            p.sendOverlayMessage(Component.translatable("vadsrijk.z2bazen.sz.mis").withStyle(ChatFormatting.RED));
            s.beeldGeraakt(sl, this, p);
        }
        breek(sl);
    }

    /** Shatters into grey shards (no loot, no xp). */
    public void breek(ServerLevel sl) {
        Z2Baas.scherven(sl, position().add(0, getBbHeight() / 2, 0), getBbWidth(), getBbHeight());
        discard();
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        if (level() instanceof ServerLevel sl) {
            breek(sl);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk");
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
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
