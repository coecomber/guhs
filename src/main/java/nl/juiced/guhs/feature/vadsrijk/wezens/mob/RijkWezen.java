package nl.juiced.guhs.feature.vadsrijk.wezens.mob;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The base of every wezens-slice creature: a {@link VadsrijkMob} with a GeckoLib model, the shared animation set (idle / walk
 * or hop / attack / happy after the plof, plus triggered specials), a synced elite flag (the renderer makes elites bigger and
 * gold-glowing) and a synced "klein" flag (the small copies a SPLITSEND elite leaves behind), elite particles per affix,
 * better elite loot (its own table "<table>_elite"), and the first-time hint of the creature.
 */
public abstract class RijkWezen extends VadsrijkMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> KLEIN = SynchedEntityData.defineId(RijkWezen.class, EntityDataSerializers.BOOLEAN);
    /** A held pose (0 = none): the zwamwachter's block, the druipslijm on the ceiling, the hoedjesbol's charge... */
    private static final EntityDataAccessor<Integer> HOUDING = SynchedEntityData.defineId(RijkWezen.class, EntityDataSerializers.INT);

    private static final Map<String, RawAnimation> LOOP = new HashMap<>();
    private static final Map<String, RawAnimation> EENMAAL = new HashMap<>();
    /** Every triggered animation any wezen may use (each controller registers all of them; unknown names are skipped by GeckoLib). */
    public static final String[] ACTIES = {"aanval", "spuug", "stuiter", "speciaal", "ram", "puf", "klap", "uithaal", "spreuk", "glim", "ontwijk"};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected int aanvalPauze;

    protected RijkWezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    protected static RawAnimation loop(String naam) {
        return LOOP.computeIfAbsent(naam, n -> RawAnimation.begin().thenLoop(n));
    }

    protected static RawAnimation eenmaal(String naam) {
        return EENMAAL.computeIfAbsent(naam, n -> RawAnimation.begin().thenPlay(n));
    }

    // ---- what a subclass gives (besides the VadsrijkMob numbers) -----------------------------------------------------------

    /** The walking animation: "hop" (slimes), "loop" (the rest). */
    protected String loopAnim() {
        return "loop";
    }

    /** The looping animation of a held pose ({@link #houding()} != 0), or null. */
    @Nullable
    protected String houdingAnim(int houding) {
        return null;
    }

    /** The hint shown the first time a player fights this creature (a key under vadsrijk.wezens.tip.), or null. */
    @Nullable
    protected String tip() {
        return null;
    }

    protected SoundEvent piepGeluid() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    // ---- synced flags ----------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KLEIN, false);
        b.define(HOUDING, 0);
    }

    @Override
    public void stelIn(int level, @Nullable Element element, boolean elite, Set<Affix> affixen) {
        super.stelIn(level, element, elite, affixen);
        if (klein()) {
            zetMaxVads(maxVads() * 0.4, false);
            heal(maxVads());
        }
    }

    /** Kern's elite flag is synced since contract 13 (integration dropped the own copy). */
    public boolean eliteZichtbaar() {
        return elite();
    }

    public boolean klein() {
        return entityData.get(KLEIN);
    }

    public void zetKlein(boolean k) {
        entityData.set(KLEIN, k);
    }

    public int houding() {
        return entityData.get(HOUDING);
    }

    public void zetHouding(int h) {
        entityData.set(HOUDING, h);
    }

    /** Server: plays a triggered animation on every client (see {@link #ACTIES}). */
    public void speel(String actie) {
        if (!level().isClientSide()) {
            triggerAnim("actie", actie);
        }
    }

    // ---- GeckoLib --------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RijkWezen>("main", 4, state -> {
            RijkWezen w = state.animatable();
            if (w.geplofd()) {
                return state.setAndContinue(loop("blij"));
            }
            String h = w.houding() != 0 ? w.houdingAnim(w.houding()) : null;
            if (h != null) {
                return state.setAndContinue(loop(h));
            }
            boolean beweegt = state.isMoving() || ("hop".equals(w.loopAnim()) && !w.onGround());
            return state.setAndContinue(loop(beweegt ? w.loopAnim() : "idle"));
        }));
        AnimationController<RijkWezen> actie = new AnimationController<>("actie", 0, state -> PlayState.STOP);
        for (String a : ACTIES) {
            actie.triggerableAnim(a, eenmaal(a));
        }
        controllers.add(actie);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---- behaviour shared by all ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (aanvalPauze > 0) {
            aanvalPauze--;
        }
        if (geplofd()) {
            return;
        }
        if (eliteZichtbaar() && tickCount % 10 == 0) {
            eliteDeeltjes(sl);
        }
        if (tickCount % 20 == 0 && getTarget() instanceof ServerPlayer p && distanceTo(p) < 12) {
            if (tip() != null) {
                Tips.toon(p, "vadsrijk.wezens.tip." + tip());
            }
            if (eliteZichtbaar()) {
                Tips.toon(p, "vadsrijk.wezens.tip.elite");
            }
        }
    }

    private void eliteDeeltjes(ServerLevel sl) {
        double x = getX(), y = getY() + getBbHeight() * 0.6, z = getZ(), w = getBbWidth() * 0.6;
        sl.sendParticles(new DustParticleOptions(0xFFD24A, 1.2f), x, y, z, 2, w, getBbHeight() * 0.4, w, 0);
        if (affixen.contains(Affix.GLOEIEND)) {
            sl.sendParticles(ParticleTypes.FLAME, x, y, z, 2, w, 0.3, w, 0.01);
        }
        if (affixen.contains(Affix.SNEL)) {
            sl.sendParticles(ParticleTypes.CLOUD, x, getY() + 0.1, z, 1, w * 0.5, 0.05, w * 0.5, 0.01);
        }
        if (affixen.contains(Affix.SCHILDEND)) {
            sl.sendParticles(ParticleTypes.ENCHANT, x, y, z, 3, w, 0.4, w, 0.3);
        }
        if (affixen.contains(Affix.SPLITSEND)) {
            sl.sendParticles(new DustParticleOptions(0xB98CFF, 1.0f), x, y, z, 2, w, 0.4, w, 0);
        }
    }

    /** A hit through the damage pipeline with a factor on this creature's normal hit (elites x1.4, small copies x0.6). */
    public Schade.Uitkomst klap(LivingEntity doel, double factor, Schade.Soort soort, String skill) {
        double n = basisSchade(mobLevel()) * factor * (elite() ? 1.4 : 1) * (klein() ? 0.6 : 1);
        return Schade.doe(new Schade.Klap(this, doel, n, element(), soort, false, skill));
    }

    /** The players (not creative/spectator) within this box (attacks that hit an area). */
    protected java.util.List<Player> spelersIn(AABB box) {
        return level().getEntitiesOfClass(Player.class, box, p -> !p.isSpectator() && !p.isCreative() && p.isAlive());
    }

    /** The players within r blocks of a point. */
    protected java.util.List<Player> spelersRond(Vec3 midden, double r) {
        return level().getEntitiesOfClass(Player.class, new AABB(midden, midden).inflate(r), p -> !p.isSpectator() && !p.isCreative()
                && p.isAlive() && p.position().distanceTo(midden) <= r + 0.4);
    }

    @Override
    public void aanval(LivingEntity doel) {
        super.aanval(doel);
        speel("aanval");
        playSound(piepGeluid(), 0.8f, 0.9f + getRandom().nextFloat() * 0.3f);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (klein() ? 1.4f : 1f);
    }

    // ---- defeat -----------------------------------------------------------------------------------------------------------

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        String id = lootTabel();
        LootTabel t = id == null || klein() ? null : elite() && LootTabellen.van(id + "_elite") != null ? LootTabellen.van(id + "_elite") : LootTabellen.van(id);
        int xp = (int) Math.round(xp() * (elite() ? 3 : 1) * (klein() ? 0.4 : 1));
        for (ServerPlayer p : helpers) {
            Spelers.geefXpGedeeld(p, xp, "mob");
            Spelers.data(p).opgevrolijkt++;
            if (t != null) {
                LootTabel.Resultaat r = t.rol(mobLevel(), getRandom());
                LootBundel.laatVallen(p, position().add(0, 0.5, 0), r, elite() ? 1 : 0);
            }
        }
    }

    /** The plof: kern's pink particles and squeak, plus a happy sound of its own; SPLITSEND elites leave two small copies. */
    @Override
    protected void opPlof() {
        super.opPlof();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, blockPosition(), piepGeluid(), SoundSource.NEUTRAL, 1f, 1.5f);
        sl.sendParticles(new DustParticleOptions(0xFF8AD0, 1.6f), getX(), getY() + getBbHeight() * 0.5, getZ(), 16, getBbWidth() * 0.5,
                getBbHeight() * 0.4, getBbWidth() * 0.5, 0);
        if (affixen.contains(Affix.SPLITSEND) && !klein()) {
            for (int i = 0; i < 2; i++) {
                splits(sl, getType(), Math.max(1, mobLevel() - 1), true, i);
            }
        }
    }

    /** Spawns a small (or a normal) copy of a type next to this one, already angry at the same target. */
    @Nullable
    protected RijkWezen splits(ServerLevel sl, EntityType<?> type, int level, boolean klein, int i) {
        if (!(type.create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof RijkWezen w)) {
            return null;
        }
        double a = getRandom().nextDouble() * Math.PI * 2 + i * 2.1;
        w.snapTo(getX() + Math.cos(a) * 0.8, getY() + 0.2, getZ() + Math.sin(a) * 0.8, getRandom().nextFloat() * 360, 0);
        w.zetKlein(klein);
        w.stelIn(level, element(), false, EnumSet.noneOf(Affix.class));
        w.setDeltaMovement(Math.cos(a) * 0.3, 0.45, Math.sin(a) * 0.3);
        sl.addFreshEntity(w);
        return w;
    }

    // ---- saving -----------------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("vr_klein", klein());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        zetKlein(in.getBooleanOr("vr_klein", false));
        super.readAdditionalSaveData(in);
    }
}
