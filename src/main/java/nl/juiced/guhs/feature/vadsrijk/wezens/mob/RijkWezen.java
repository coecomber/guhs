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

import net.minecraft.core.BlockPos;
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
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensSpawners;

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
        var volg = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (volg != null) {
            volg.setBaseValue(Math.max(volg.getBaseValue(), LEIBAND + 4));   // the leash decides when a chase ends, not the follow range
        }
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
        if ((tickCount + getId()) % 10 == 0 && leiband(sl)) {
            return;
        }
        LivingEntity nuDoel = getTarget();
        if (nuDoel instanceof Player p && vorigDoel == null) {
            roepGroep(p);
        }
        vorigDoel = nuDoel;
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

    // ---- leven (fix round 1): aggro range, leash, patrols, social aggro ------------------------------------------------------

    /** How far a chase may take it from its spawner before it gives up, heals and walks home. */
    public static final int LEIBAND = 30;
    /** Aggro range per kind (simple class name); 0 = never starts a fight itself. */
    private static final Map<String, Double> AGGRO = Map.ofEntries(Map.entry("Klodder", 9.0), Map.entry("Bubbelklodder", 12.0),
            Map.entry("Stuiterklodder", 10.0), Map.entry("Moederklodder", 10.0), Map.entry("Druipslijm", 8.0), Map.entry("Kristalklodder", 9.0),
            Map.entry("Hoedjesbol", 13.0), Map.entry("Sporenpuffer", 9.0), Map.entry("Zwamwachter", 8.0), Map.entry("Glimzwam", 0.0),
            Map.entry("Tempelridder", 12.0), Map.entry("Sporenpriester", 15.0), Map.entry("Mosgeest", 14.0), Map.entry("Knaag", 14.0));

    /** Until this tickCount it is walking home after a chase (ignores players that do not hit it). */
    protected int terugTot;
    @Nullable
    private LivingEntity vorigDoel;

    /** How close a player may come before it attacks on its own. */
    public double aggroBereik() {
        return AGGRO.getOrDefault(getClass().getSimpleName(), 10.0);
    }

    /** Cheered-up creatures stay ~10 s, hopping around happily (fix round 1: the user wants to see them be happy). */
    @Override
    protected int plofDuur() {
        return 200;
    }

    public boolean opWegTerug() {
        return terugTot > tickCount;
    }

    /**
     * Every half second: lost mobs (far from home, or an old spawner id) vanish; a chase too far from home ends (heal, walk
     * back); an idle mob outside its camp or on patrol walks to where it belongs. True = it was removed.
     */
    private boolean leiband(ServerLevel sl) {
        if (spawner.isEmpty()) {
            return false;
        }
        Spawners.Spawner sp = Spawners.van(spawner);
        if (sp == null) {
            if (spawner.startsWith("wezens:") && !spawner.startsWith("wezens:test")) {
                discard();   // a creature of a spawner that no longer exists (an older plan)
                return true;
            }
            return false;
        }
        double dx = getX() - (sp.centrum().getX() + 0.5), dz = getZ() - (sp.centrum().getZ() + 0.5), d = Math.sqrt(dx * dx + dz * dz);
        LivingEntity doel = getTarget();
        if (doel == null && d > sp.straal() + LEIBAND + 20 && tickCount > 100) {
            discard();
            return true;
        }
        if (doel != null && d > sp.straal() + LEIBAND) {
            setTarget(null);
            bijdrage.clear();
            heal(maxVads());
            terugTot = tickCount + 160;
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(), getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
        if (getTarget() == null) {
            Vec3 thuis = thuisDoel();
            if (thuis != null && gebruiktNavigatie()) {
                getNavigation().moveTo(thuis.x, thuis.y, thuis.z, opWegTerug() ? 1.25 : 0.8);
            }
        }
        return false;
    }

    /** Walks with the vanilla navigation (slimes hop and the Mosgeest floats: they steer themselves with {@link #thuisDoel}). */
    protected boolean gebruiktNavigatie() {
        return true;
    }

    /**
     * Where an idle creature should go now: the current end of its patrol route (the pair turns every 15 s), its camp when it
     * strayed out of it, or null (wander freely).
     */
    @Nullable
    public Vec3 thuisDoel() {
        Spawners.Spawner sp = spawner.isEmpty() ? null : Spawners.van(spawner);
        if (sp == null) {
            return null;
        }
        BlockPos[] route = WezensSpawners.route(spawner);
        if (route != null) {
            BlockPos e = route[(int) ((level().getGameTime() / 300) % 2)];
            double ex = e.getX() + 0.5 - getX(), ez = e.getZ() + 0.5 - getZ();
            return ex * ex + ez * ez > 6 ? new Vec3(e.getX() + 0.5, getY(), e.getZ() + 0.5) : null;
        }
        double dx = getX() - (sp.centrum().getX() + 0.5), dz = getZ() - (sp.centrum().getZ() + 0.5);
        if (dx * dx + dz * dz > (sp.straal() + 3.0) * (sp.straal() + 3.0) || opWegTerug() && dx * dx + dz * dz > 4) {
            return new Vec3(sp.centrum().getX() + 0.5, getY(), sp.centrum().getZ() + 0.5);
        }
        return null;
    }

    /** Social aggro: the idle members of its group (same spawner group) within 12 blocks join the fight. */
    private void roepGroep(Player p) {
        if (spawner.isEmpty()) {
            return;
        }
        String groep = WezensSpawners.groep(spawner);
        for (RijkWezen o : level().getEntitiesOfClass(RijkWezen.class, getBoundingBox().inflate(12, 6, 12),
                o -> o != this && !o.geplofd() && o.getTarget() == null && !o.spawner.isEmpty() && groep.equals(WezensSpawners.groep(o.spawner)))) {
            o.vorigDoel = p;   // (no chain: it was called, it does not call again)
            o.setTarget(p);
        }
    }

    /** The target goal of every wezen: the nearest survival player within {@link #aggroBereik} that it can see (not while walking home). */
    public static class Aggro extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final RijkWezen w;
        @Nullable
        private Player kandidaat;

        public Aggro(RijkWezen w) {
            super(w, false);
            this.w = w;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            double r = w.aggroBereik();
            if (r <= 0 || w.opWegTerug() || w.geplofd() || w.getRandom().nextInt(5) != 0) {
                return false;
            }
            kandidaat = null;
            double best = r * r;
            for (Player p : w.level().getEntitiesOfClass(Player.class, w.getBoundingBox().inflate(r, Math.min(r, 8), r),
                    p -> !p.isCreative() && !p.isSpectator() && p.isAlive())) {
                double d = p.distanceToSqr(w);
                if (d <= best && w.hasLineOfSight(p)) {
                    best = d;
                    kandidaat = p;
                }
            }
            return kandidaat != null;
        }

        @Override
        public void start() {
            w.setTarget(kandidaat);
            super.start();
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
