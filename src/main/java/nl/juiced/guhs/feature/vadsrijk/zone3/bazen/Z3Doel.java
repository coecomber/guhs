package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * A mechanic object of a zone-3 boss: a cage lock (Graaf Piepvel), a balloon segment (Roquefort), a feeding root (Wortelwurger),
 * a deck cannon (Brokkel, right-click), a mould kettle (Schimmelkwekerij, right-click). It hangs where its owner puts it, has its
 * own Vads (hits from players in the fight; ranged and magic reach it too), tells its owner when hit / broken / used, and is
 * never saved. Its owner is a {@link Z3Baas} or a dungeon room ({@link Doelhouder}).
 */
public class Z3Doel extends VadsrijkMob implements GeoEntity {
    /** Whoever owns the object (a boss or a room). */
    public interface Doelhouder {
        /** May this player hit/use it? */
        boolean magRaken(Player p);

        default void geraakt(Z3Doel d, double n, @Nullable LivingEntity bron) {
        }

        void kapot(Z3Doel d, @Nullable LivingEntity bron);

        default void gebruikt(Z3Doel d, ServerPlayer p) {
        }

        boolean bestaat();
    }

    private static final EntityDataAccessor<Integer> STAND = SynchedEntityData.defineId(Z3Doel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RAAKBAAR = SynchedEntityData.defineId(Z3Doel.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID baas;
    @Nullable
    public Doelhouder houder;
    public int nummer;
    private double hp = 1000;
    @Nullable
    private Vec3 plek;

    public Z3Doel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(STAND, 0);
        b.define(RAAKBAAR, true);
    }

    /** Ties it to a boss (hits go to {@link Z3Baas#doelGeraakt}...), with this many Vads, at this spot. */
    public Z3Doel koppel(Z3Baas b, int nummer, double vads, Vec3 waar) {
        baas = b.getUUID();
        return koppel((Doelhouder) null, nummer, Math.min(50, b.mobLevel()), vads, waar);
    }

    public Z3Doel koppel(@Nullable Doelhouder h, int nummer, int level, double vads, Vec3 waar) {
        houder = h;
        this.nummer = nummer;
        this.hp = vads;
        stelIn(level, Element.GEEN, false, Set.of());
        zetOntvadst(false);
        plek = waar;
        snapTo(waar.x, waar.y, waar.z, getYRot(), 0);
        return this;
    }

    @Override
    protected double basisVads(int level) {
        return hp;
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
    protected void registerGoals() {
    }

    public int stand() {
        return entityData.get(STAND);
    }

    public void zetStand(int s) {
        entityData.set(STAND, s);
    }

    public boolean raakbaar() {
        return entityData.get(RAAKBAAR);
    }

    public void zetRaakbaar(boolean r) {
        entityData.set(RAAKBAAR, r);
    }

    /** Turns it (body + head + yaw: mobs render with the body rotation). */
    public void kijk(float yaw) {
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        yBodyRotO = yaw;
    }

    public void zetPlek(Vec3 v) {
        plek = v;
    }

    @Nullable
    public Z3Baas baas() {
        return baas != null && level() instanceof ServerLevel sl && sl.getEntity(baas) instanceof Z3Baas b && !b.geplofd() ? b : null;
    }

    private Doelhouder eigenaar() {
        if (houder != null) {
            return houder;
        }
        Z3Baas b = baas();
        return b == null ? null : new Doelhouder() {
            @Override
            public boolean magRaken(Player p) {
                return b.zDoetMee(p);
            }

            @Override
            public void geraakt(Z3Doel d, double n, @Nullable LivingEntity bron) {
                b.doelGeraakt(d, n, bron);
            }

            @Override
            public void kapot(Z3Doel d, @Nullable LivingEntity bron) {
                b.doelKapot(d, bron);
            }

            @Override
            public void gebruikt(Z3Doel d, ServerPlayer p) {
                b.doelGebruikt(d, p);
            }

            @Override
            public boolean bestaat() {
                return true;
            }
        };
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        Doelhouder h = eigenaar();
        if (h == null || !raakbaar() || geplofd() || (bron instanceof Player p && !h.magRaken(p))) {
            return;
        }
        double voor = vads();
        double echt = Math.min(n, voor);
        heal(-echt);
        this.hurtTime = 10;
        if (level() instanceof ServerLevel sl) {
            sl.broadcastEntityEvent(this, (byte) 2);
        }
        h.geraakt(this, echt, bron);
        if (vads() <= 0.001) {
            entityData.set(RAAKBAAR, false);
            h.kapot(this, bron);
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        // (never through the normal plof: krijgSchade above stops at the last bit)
    }

    /** Sets its Vads directly (a room's own logic, no owner calls). */
    public void zetVads(double v) {
        heal(Math.max(0, v) - vads());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        Doelhouder h = eigenaar();
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp && h != null && h.magRaken(sp)) {
            h.gebruikt(this, sp);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide()) {
            return;
        }
        if (plek != null && position().distanceToSqr(plek) > 0.0004) {
            setPos(plek.x, plek.y, plek.z);
        }
        Doelhouder h = eigenaar();
        if (tickCount > 20 && (h == null || !h.bestaat())) {
            discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Component plaatje() {
        String soort = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        MutableComponent n = Component.translatable("entity.guhs." + soort).withStyle(ChatFormatting.LIGHT_PURPLE);
        if (!raakbaar()) {
            return n;
        }
        int balk = 10, vol = (int) Math.ceil(balk * vads() / Math.max(1, maxVads()));
        n.append(Component.literal(" " + "▌".repeat(vol)).withStyle(ChatFormatting.RED));
        n.append(Component.literal("▌".repeat(balk - vol)).withStyle(ChatFormatting.DARK_GRAY));
        return n;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle");
        controllers.add(new AnimationController<>("idle", 3, state -> state.setAndContinue(idle)));
        AnimationController<Z3Doel> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String a : new String[] {"open", "klop", "schiet", "pomp", "grijp"}) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlayAndHold(a));
        }
        controllers.add(actie);
    }

    public void speel(String anim) {
        triggerAnim("actie", anim);
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
