package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor;

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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * One of the Mika-boormeester's 3 drill heads (rood / geel / blauw). It can't be beaten: hits heat it up
 * ({@link Boormeester#kopGeraakt}); its nameplate is a heat bar. It rides on the machine (positioned by it every tick) and is never
 * saved.
 */
public class Boorkop extends VadsrijkMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> KLEUR = SynchedEntityData.defineId(Boorkop.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HITTE = SynchedEntityData.defineId(Boorkop.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HEET = SynchedEntityData.defineId(Boorkop.class, EntityDataSerializers.BOOLEAN);
    public static final String[] KLEUREN = {"rood", "geel", "blauw"};
    private static final ChatFormatting[] OPMAAK = {ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.AQUA};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID machine;

    public Boorkop(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KLEUR, 0);
        b.define(HITTE, 0f);
        b.define(HEET, false);
    }

    public void koppel(Boormeester m, int kleur) {
        machine = m.getUUID();
        entityData.set(KLEUR, kleur);
        stelIn(m.mobLevel(), Element.VUUR, false, Set.of());
        zetOntvadst(false);
    }

    public int kleur() {
        return entityData.get(KLEUR);
    }

    public float hitte() {
        return entityData.get(HITTE);
    }

    public void zetHitte(float h) {
        entityData.set(HITTE, Math.max(0, Math.min(100, h)));
    }

    public boolean oververhit() {
        return entityData.get(HEET);
    }

    public void zetOververhit(boolean b) {
        entityData.set(HEET, b);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected double basisVads(int level) {
        return 1000;
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

    @Nullable
    public Boormeester machine() {
        return machine != null && level() instanceof ServerLevel sl && sl.getEntity(machine) instanceof Boormeester m && !m.geplofd() ? m : null;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        Boormeester m = machine();
        if (m != null && level() instanceof ServerLevel sl) {
            m.kopGeraakt(sl, this, n, bron);
            this.hurtTime = 10;
            sl.broadcastEntityEvent(this, (byte) 2);
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide() && tickCount > 20 && machine() == null) {
            discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Component plaatje() {
        int k = Math.max(0, Math.min(2, kleur()));
        MutableComponent n = Component.translatable("vadsrijk.z2bazen.bm.kop." + KLEUREN[k]).withStyle(OPMAAK[k]);
        if (oververhit()) {
            return n.append(Component.literal(" ").append(Component.translatable("vadsrijk.z2bazen.bm.kop.heet").withStyle(ChatFormatting.GOLD)));
        }
        int balk = 10, vol = (int) Math.ceil(balk * hitte() / 100f);
        n.append(Component.literal(" " + "▌".repeat(vol)).withStyle(vol > 7 ? ChatFormatting.RED : vol > 4 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        n.append(Component.literal("▌".repeat(balk - vol)).withStyle(ChatFormatting.DARK_GRAY));
        return n;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation draai = RawAnimation.begin().thenLoop("draai");
        controllers.add(new AnimationController<>("draai", 2, state -> state.setAndContinue(draai)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /** Put on its spot on the machine (every tick). */
    public void zetOpPlek(Vec3 v, float yaw) {
        setPos(v.x, v.y, v.z);
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }
}
