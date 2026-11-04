package nl.juiced.guhs.feature.vadsrijk.bazen.mika;

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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenEffecten;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * A decoy of the Mika-spion (fix round 1): looks and is named exactly like him (same nameplate, same HP bar), but his fake
 * moustache doesn't glitter. Hit one: it poofs into smoke and dusts you with ontvadsings-poeder (Gesporen). They vanish after
 * {@link #LEVEN} ticks or when the real one is hit hard enough to drop them ({@link MikaSpion}).
 */
public class LokMika extends VadsrijkMob implements GeoEntity {
    public static final int LEVEN = 20 * 16;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle"), WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID echte;

    public LokMika(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public void koppel(MikaSpion m) {
        echte = m.getUUID();
        spiegel(m);
    }

    @Nullable
    private MikaSpion echte() {
        return echte != null && level() instanceof ServerLevel sl && sl.getEntity(echte) instanceof MikaSpion m && !m.geplofd() ? m : null;
    }

    private void spiegel(MikaSpion m) {
        setCustomName(m.getCustomName());
        setCustomNameVisible(m.isCustomNameVisible());
        if (Math.abs(maxVads() - m.maxVads()) > 0.5 || Math.abs(vads() - m.vads()) > 0.5) {
            zetMaxVads(m.maxVads(), false);
            heal(m.vads() - vads());
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
    }

    @Override
    protected double basisVads(int level) {
        return 2400 + 150 * level;
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

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl) || isRemoved()) {
            return;
        }
        if (bron instanceof ServerPlayer p) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.mika.lok_nep").withStyle(ChatFormatting.GRAY));
            Statussen.geef(p, Status.GESPOREN, 60, Math.max(1, mobLevel() * 0.4), this);
        }
        poef(sl);
    }

    public void poef(ServerLevel sl) {
        BazenEffecten.poef(sl, position());
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        MikaSpion m = echte();
        if (m == null || tickCount > LEVEN || m.toestand() != MikaSpion.VECHT) {
            poef(sl);
            return;
        }
        if (tickCount % 10 == 0) {
            spiegel(m);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override
    protected void beloon(ServerLevel sl, java.util.Set<ServerPlayer> helpers) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 3, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
