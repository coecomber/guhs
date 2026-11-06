package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;

/**
 * The base of every zone-5 creature (Hoogvads, lv 56-60). Built on the zone-2 {@link Z2Wezen} (no pink plof, hidden = not hittable,
 * helpers), plus:
 * <ul>
 *   <li>{@link #soort()}: GRAUW (grey fluff blows out of it: possessed dummies, mites, imps, mist), MIKA (flees in a smoke puff: "Dit
 *       komt in mijn rapport!"), GUH (the sparring knights: never defeated, they bow);</li>
 *   <li>own tips {@code vadsrijk.z5wezens.tip.<naam>}, own loot tables {@code z5wezens:<naam>} (+ "_elite");</li>
 *   <li>named area leaders (spawner id {@code z5wezens:leider_<naam>}: always elite, a name of its own).</li>
 * </ul>
 * Numbers: the zones 1-3 curve (HP base x (1 + 0.3/lv), damage x (1 + 0.22/lv)); xp = z3-systemen's pacing (1/55 of a level at the
 * creature's level, clamped by kern's level cap) times a weight per kind.
 */
public abstract class Z5Wezen extends Z2Wezen {
    public enum Soort {
        GRAUW, MIKA, GUH
    }

    private boolean benoemd;

    protected Z5Wezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public abstract Soort soort();

    @Override
    public Aard aard() {
        return soort() == Soort.MIKA ? Aard.MIKA : Aard.BEEST;
    }

    protected static double vads5(double basis, int level) {
        return basis * (1 + 0.3 * (level - 1));
    }

    protected static double schade5(double basis, int level) {
        return basis * (1 + 0.22 * (level - 1));
    }

    /** Xp: 1/55 of a level at this creature's level (kern's curve, clamped to the cap) times a weight. */
    protected int xp5(double factor) {
        int l = Math.max(1, Math.min(Levels.MAX - 1, mobLevel()));
        return (int) Math.max(1, Math.round(Levels.xpVoor(l) / 55.0 * factor));
    }

    @Override
    public int xp() {
        return xp5(1.0);
    }

    @Nullable
    @Override
    public String lootTabel() {
        return "z5wezens:" + naam();
    }

    /** No zone-1 hint (zone 5 shows its own). */
    @Override
    protected String tip() {
        return null;
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl) || geplofd()) {
            return;
        }
        if (!benoemd && spawner.startsWith("z5wezens:leider_")) {
            benoemd = true;
            setCustomName(Component.translatable("vadsrijk.z5wezens.leider." + spawner.substring("z5wezens:leider_".length())));
        }
        if (tickCount % 20 == 0 && getTarget() instanceof ServerPlayer p && distanceTo(p) < 14) {
            Tips.toon(p, "vadsrijk.z5wezens.tip." + naam());
        }
    }

    /** The usual ground goals with kern's aggro range (not vanilla's follow range). */
    @Override
    protected void standaardDoelen(double snelheid, boolean alarm) {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, snelheid, true));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 10f));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        var h = new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this);
        this.targetSelector.addGoal(1, alarm ? h.setAlertOthers() : h);
        this.targetSelector.addGoal(2, new Aggro(this));
    }

    // ---- defeat: no pink plof (only the guhs are pink) ---------------------------------------------------------------------------

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 v = position();
        float b = getBbWidth(), h = getBbHeight();
        switch (soort()) {
            case MIKA -> {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 0.8, v.z, 40, b / 2 + 0.3, 0.8, b / 2 + 0.3, 0.04);
                sl.sendParticles(ParticleTypes.POOF, v.x, v.y + 0.8, v.z, 25, b / 2, 0.6, b / 2, 0.06);
                sl.playSound(null, v.x, v.y, v.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 1.5f);
                Getallen.tekst(sl, v.x, v.y + h + 0.5, v.z, Component.translatable("vadsrijk.z5wezens.mika.weg." + getRandom().nextInt(4)), 0.85f);
            }
            case GRAUW -> grauwePluis(sl, v.add(0, h / 2, 0), b, h);
            case GUH -> sl.sendParticles(ParticleTypes.HEART, v.x, v.y + h, v.z, 6, 0.4, 0.3, 0.4, 0);
        }
        setInvisible(true);
        if (affixen.contains(Affix.SPLITSEND) && !klein()) {
            for (int i = 0; i < 2; i++) {
                splits(sl, getType(), Math.max(1, mobLevel() - 1), true, i);
            }
        }
    }

    /** Grey fluff blows out and away on the wind, a little pink sparkle stays (what it held is free again). */
    public static void grauwePluis(ServerLevel sl, Vec3 v, double w, double h) {
        sl.sendParticles(new DustParticleOptions(0x9A9AA6, 2.0f), v.x, v.y, v.z, 60, w / 2, h / 2, w / 2, 0.05);
        sl.sendParticles(ParticleTypes.WHITE_ASH, v.x, v.y, v.z, 50, w / 2, h / 2, w / 2, 0.1);
        sl.sendParticles(ParticleTypes.CLOUD, v.x, v.y, v.z, 12, w / 3, h / 3, w / 3, 0.04);
        sl.sendParticles(new DustParticleOptions(0xFF8AD0, 1.0f), v.x, v.y, v.z, 8, w / 3, h / 3, w / 3, 0);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 2f, 0.7f);
    }

    // ---- small helpers -------------------------------------------------------------------------------------------------------------

    /** A floating line of text above it (a key under vadsrijk.z5wezens.). */
    protected void zegBoven(ServerLevel sl, String key) {
        Getallen.tekst(sl, getX(), getY() + getBbHeight() + 0.5, getZ(), Component.translatable("vadsrijk.z5wezens." + key), 0.85f);
    }

    protected static void status(LivingEntity p, Status s, int ticks, double sterkte, LivingEntity bron) {
        Statussen.geef(p, s, ticks, sterkte, bron);
    }

    /** Is a point inside a flat strip from {@code van} along {@code richting} (flat, normalised), this long and wide? */
    protected static boolean inStrook(Vec3 van, Vec3 richting, double lengte, double breedte, Vec3 punt) {
        Vec3 d = punt.subtract(van).multiply(1, 0, 1);
        double langs = d.dot(richting);
        double dwars = Math.abs(d.x * -richting.z + d.z * richting.x);
        return langs > -0.5 && langs < lengte + 0.5 && dwars < breedte / 2 + 0.4 && Math.abs(punt.y - van.y) < 3;
    }

    protected static Vec3 richting(Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        return d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
    }

    protected static void duw(LivingEntity p, Vec3 v) {
        p.setDeltaMovement(v);
        p.hurtMarked = true;
    }
}
