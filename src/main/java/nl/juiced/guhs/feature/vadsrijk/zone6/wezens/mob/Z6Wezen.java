package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensApi;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;

/**
 * The base of every zone-6 creature (Het Zijdeland, lv 60-70; ZONE6_PLAN §5). Sweet, not dark: most creatures are ontvadst
 * desert animals with a grey crust of "sterrenzout" that you CHEER UP (zone-1 {@link RijkWezen}: the pink plof, then the bright
 * happy texture {@code <naam>_blij} and ten seconds of happy hops). Two other endings ({@link Afloop}):
 * <ul>
 *   <li>{@link Afloop#MIKA}: the Mika crew (Lampendovers, Ringdragers) flees in a smoke puff with a grumpy line;</li>
 *   <li>{@link Afloop#VERSLAAN}: constructs and dust (Zandvlooien, Spiegelslang, Zandgeestje, Zoutslang) burst into glittering
 *       sand/salt (no happy hops).</li>
 * </ul>
 * Numbers: the shared curve (HP base x (1 + 0.3/lv), hit x (1 + 0.22/lv)); xp on zone-3 pacing clamped below the level cap;
 * tips {@code vadsrijk.z6wezens.tip.<naam>}; loot {@code z6wezens:<naam>} (+ {@code _elite}); creatures that wander into a city /
 * the caravan camp forget the fight ({@link Z6WezensPlekken#veilig}); flyers steer themselves ({@link #vliegt()}). Area leaders
 * (spawner {@code z6wezens:leider_<naam>}) get their name ({@code vadsrijk.z6wezens.leider.<naam>}).
 */
public abstract class Z6Wezen extends RijkWezen {
    /** How a fight with it ends. */
    public enum Afloop {
        OPVROLIJKEN, MIKA, VERSLAAN
    }

    private boolean benoemd;

    protected Z6Wezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        if (vliegt()) {
            setNoGravity(true);
        }
    }

    /** The name used for loot tables, tips and textures. */
    public abstract String naam();

    protected abstract double hpBasis();

    protected abstract double klapBasis();

    public Afloop afloop() {
        return Afloop.OPVROLIJKEN;
    }

    public final boolean mika() {
        return afloop() == Afloop.MIKA;
    }

    /** Xp weight (1 = a normal creature). */
    protected double xpGewicht() {
        return 1.0;
    }

    /** Flies or floats (steers itself, no gravity until the plof). */
    public boolean vliegt() {
        return false;
    }

    /** A pet this creature can drop (id) and the chance per kill, or null. */
    @Nullable
    public String huisdier() {
        return null;
    }

    public double huisdierKans() {
        return 0.01;
    }

    /** An area leader (spawner z6wezens:leider_*)? */
    public boolean leider() {
        return spawner.startsWith("z6wezens:leider_");
    }

    // ---- numbers ----------------------------------------------------------------------------------------------------------

    @Override
    protected double basisVads(int level) {
        return hpBasis() * (1 + 0.3 * (level - 1));
    }

    @Override
    protected double basisSchade(int level) {
        return klapBasis() * (1 + 0.22 * (level - 1));
    }

    @Override
    public int xp() {
        int l = Math.max(1, Math.min(Levels.MAX - 1, mobLevel()));
        return (int) Math.max(1, Math.round(Levels.xpVoor(l) / 55.0 * xpGewicht()));
    }

    @Nullable
    @Override
    public String lootTabel() {
        return "z6wezens:" + naam();
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    public void stelIn(int level, @Nullable Element element, boolean elite, Set<Affix> affixen) {
        super.stelIn(level, element, elite, affixen);
        zetOntvadst(afloop() == Afloop.OPVROLIJKEN);   // ontvadst: Roze does x1.5
    }

    @Override
    protected String tip() {
        return null;
    }

    @Override
    protected SoundEvent piepGeluid() {
        return mika() ? SoundEvents.OCELOT_HURT : afloop() == Afloop.VERSLAAN ? SoundEvents.SAND_HIT : SoundEvents.FOX_AMBIENT;
    }

    // ---- flying -------------------------------------------------------------------------------------------------------------

    @Override
    public boolean isNoGravity() {
        return vliegt() ? !geplofd() : super.isNoGravity();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        if (!vliegt()) {
            super.checkFallDamage(y, onGround, state, pos);
        }
    }

    @Override
    protected boolean gebruiktNavigatie() {
        return !vliegt();
    }

    /** Flyers: where it wants to be this tick (target = the player it fights, or null = drift). */
    @Nullable
    protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
        return null;
    }

    protected double vliegHoogte() {
        return 2.5;
    }

    protected double vliegSnelheid() {
        return 0.16;
    }

    @Override
    protected void registerGoals() {
        if (vliegt()) {
            this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 12f));
            this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new Aggro(this));
        } else {
            doelen();
        }
    }

    /** The ground goals of a walking creature (default: the usual set at speed 1.0). */
    protected void doelen() {
        standaardDoelen(1.0, true);
    }

    private double hoek;

    private void vliegTick(ServerLevel sl) {
        if (stil()) {
            setDeltaMovement(getDeltaMovement().scale(0.5));
            return;
        }
        LivingEntity doel = geldig(getTarget()) ? getTarget() : null;
        if (doel == null && getTarget() != null) {
            setTarget(null);
        }
        Vec3 wil = stuur(sl, doel);
        double g = grond(position());
        if (wil == null) {
            Vec3 thuis = thuisDoel();
            if (tickCount % 60 == 0) {
                hoek = getRandom().nextDouble() * Math.PI * 2;
            }
            wil = thuis != null ? thuis : position().add(Math.cos(hoek) * 2.5, 0, Math.sin(hoek) * 2.5);
            wil = new Vec3(wil.x, g + vliegHoogte() + Math.sin(tickCount * 0.05) * 0.6, wil.z);
        }
        double gw = grond(wil);
        Vec3 naar = new Vec3(wil.x - getX(), Math.max(wil.y, gw + 0.8) - getY(), wil.z - getZ());
        double snel = vliegSnelheid() * (affixen.contains(Affix.SNEL) ? 1.4 : 1);
        Vec3 v = naar.lengthSqr() > snel * snel ? naar.normalize().scale(snel) : naar;
        setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.5)));
        if (doel != null) {
            lookAt(doel, 30, 30);
        } else if (naar.horizontalDistanceSqr() > 0.01) {
            setYRot((float) Math.toDegrees(Math.atan2(-naar.x, naar.z)));
        }
        setYBodyRot(getYRot());
    }

    // ---- tick ---------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (geplofd()) {
            if (afloop() != Afloop.OPVROLIJKEN && plofTicks() >= 4) {
                discard();   // the Mika is gone in its smoke, the construct crumbled (no happy hops)
            }
            return;
        }
        if (!benoemd && leider()) {
            benoemd = true;
            setCustomName(Component.translatable("vadsrijk.z6wezens.leider." + spawner.substring("z6wezens:leider_".length())));
        }
        if (vliegt() && !isNoAi()) {
            vliegTick(sl);
        }
        if (tickCount % 20 == 7 && !spawner.isEmpty() && Z6WezensPlekken.veilig(getX(), getZ())) {
            setTarget(null);   // a city / the camp: forget the fight, go home
            Vec3 t = thuisDoel();
            if (t != null && gebruiktNavigatie()) {
                getNavigation().moveTo(t.x, t.y, t.z, 1.2);
            }
        }
        if (tickCount % 20 == 0 && getTarget() instanceof ServerPlayer p && distanceTo(p) < 14) {
            Tips.toon(p, "vadsrijk.z6wezens.tip." + naam());
        }
    }

    // ---- defeat -------------------------------------------------------------------------------------------------------------

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        String id = lootTabel();
        LootTabel t = id == null || klein() ? null : elite() && LootTabellen.van(id + "_elite") != null ? LootTabellen.van(id + "_elite") : LootTabellen.van(id);
        int xp = (int) Math.round(xp() * (elite() ? 3 : 1) * (klein() ? 0.4 : 1));
        for (ServerPlayer p : helpers) {
            Spelers.geefXpGedeeld(p, xp, "mob");
            if (afloop() == Afloop.OPVROLIJKEN) {
                Spelers.data(p).opgevrolijkt++;
            }
            Z6WezensApi.verslagen(p, this);
            if (t == null) {
                continue;
            }
            LootTabel.Resultaat r = t.rol(mobLevel(), getRandom());
            List<ItemStack> items = new ArrayList<>(r.items());
            LootBundel.laatVallen(p, position().add(0, 0.5, 0), new LootTabel.Resultaat(items, r.munten()), elite() ? 1 : 0);
        }
    }

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        double w = getBbWidth(), h = getBbHeight();
        switch (afloop()) {
            case MIKA -> {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 40, w / 2 + 0.3, 0.8, w / 2 + 0.3, 0.04);
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 25, w / 2, 0.6, w / 2, 0.06);
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 1.5f);
                Getallen.tekst(sl, getX(), getY() + h + 0.5, getZ(), Component.translatable("vadsrijk.z6wezens.mika.weg." + getRandom().nextInt(4)), 0.85f);
                setInvisible(true);
            }
            case VERSLAAN -> {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), getX(), getY() + h / 2, getZ(),
                        (int) (30 * Math.max(1, w)), w / 2, h / 2, w / 2, 0.05);
                Z6Vfx.glinster(sl, position().add(0, h / 2, 0), Math.max(0.8, w));
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.2f, 1.3f);
                setInvisible(true);
            }
            default -> {
                super.opPlof();   // kern + RijkWezen: the pink plof, hearts, a happy squeak, the bright texture
                Z6Vfx.zoutKorst(sl, position().add(0, h * 0.6, 0), w);   // the grey star-salt crust flakes off
            }
        }
    }

    // ---- helpers for the creatures ------------------------------------------------------------------------------------------

    /** The usual ground goals with kern's aggro range (never vanilla's long follow range). */
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

    /** Forward (flat) unit vector. */
    protected Vec3 voor() {
        double a = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(a), 0, Math.cos(a));
    }

    /** A hit on every player within r of a point (factor on the normal hit); returns how many. */
    protected int klapRond(Vec3 midden, double r, double factor, Schade.Soort soort, String skill) {
        int n = 0;
        for (Player p : spelersRond(midden, r)) {
            klap(p, factor, soort, skill);
            n++;
        }
        return n;
    }

    /** The floor height under a point (first collision from 6 above down 30); water counts as floor. */
    protected double grond(Vec3 v) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x = Mth.floor(v.x), z = Mth.floor(v.z);
        for (int y = Mth.floor(v.y) + 6; y > v.y - 30; y--) {
            m.set(x, y - 1, z);
            if (!level().getBlockState(m).getCollisionShape(level(), m).isEmpty() || !level().getFluidState(m).isEmpty()) {
                return y;
            }
        }
        return v.y - 4;
    }

    /** Is this a valid target still (alive, not creative/spectator)? */
    protected static boolean geldig(@Nullable LivingEntity l) {
        return l != null && l.isAlive() && !(l instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** A grey grumble puff (the star-salt crust) when it attacks. */
    protected void mopper(ServerLevel sl) {
        sl.sendParticles(new DustParticleOptions(0xB8B4C4, 1.2f), getX(), getY() + getBbHeight() + 0.2, getZ(), 4, 0.2, 0.1, 0.2, 0);
    }
}
