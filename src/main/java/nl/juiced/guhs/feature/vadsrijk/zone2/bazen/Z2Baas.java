package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The base of the zone-2 bosses: kern's {@link VadsrijkBaas} (phases, intro, scaling, contribution, personal chests) plus a
 * fight engine of its own, so the bosses REALLY fight from the first second (zone-1 playtest lesson: they stood still and could
 * be shot from range):
 * <ul>
 *   <li>{@link #strijders()}: every player in the arena (also the ones shooting from the far edge) is a target; the boss picks its
 *       own target (no vanilla goal that skips creative or out-of-range players);</li>
 *   <li>an attack rotation: {@link #kiesAanval} is asked every time the cooldown {@link #rustT} runs out; between moves it chases
 *       and bites ({@link #bijt}); players that stay at range ({@link #VER}+ blocks for {@link #VER_TICKS}) are picked by
 *       {@link #afstandsDoel()} for gap closers / lobs;</li>
 *   <li>hits from players outside the fight do nothing ({@link #krijgSchade}); a boss pulled out of its arena jumps back; an arena
 *       that stays empty for 30 s resets the boss ({@link #zLeeg}).</li>
 * </ul>
 * (Names carry a {@code z} where kern's fix round may add similar methods to VadsrijkBaas, so the merge never clashes; integration
 * may later fold this engine into kern's.) Zone-2 tone: real monsters, no pink plof: crystal ones shatter into grey shards
 * ({@link #opPlof}); the Mika flees in a smoke puff (overridden there).
 */
public abstract class Z2Baas extends VadsrijkBaas implements GeoEntity {
    /** Colour language (fix round 1): red = damage, pink = safe, purple = mechanic, yellow = stack. */
    public static final int ROOD = 0xFF2A2A, ROZE = 0xFF5FC8, PAARS = 0xA040FF, GEEL = 0xFFD62A, IJSBLAUW = 0x9AD8FF;
    public static final double VER = 9;
    public static final int VER_TICKS = 50, LEEG_TICKS = 600;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Where the chests went (gametests). */
    public final List<BlockPos> kisten = new ArrayList<>();

    @Nullable
    private Vec3 thuis;
    @Nullable
    private UUID doelId;
    private int doelWissel, buitenT, leegT;
    private long teVerBericht;
    private final Map<UUID, Integer> verT = new HashMap<>();
    /** Ticks until the next move from {@link #kiesAanval} (the rotation's global cooldown). */
    protected int rustT = 30;
    /** Ticks until the next bite. */
    protected int bijtT = 20;
    // the kinematic jump
    private int sprongT = -1, sprongDuur;
    private Vec3 sprongVan = Vec3.ZERO, sprongNaar = Vec3.ZERO;
    private double sprongHoogte;
    /** For the AutoCheck/gametests: how many moves it started so far. */
    public int aanvallenGedaan;
    /** The last move it started (gametests / debug). */
    public String laatsteAanval = "";

    protected Z2Baas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** galmwolf | spiegelzucht | boormeester. */
    public abstract String baasId();

    /** Start a new move (set {@link #rustT} to its length + cooldown). Called only when not stunned, jumping or in the intro. */
    protected abstract void kiesAanval(ServerLevel sl, List<ServerPlayer> l);

    /** Every tick while fighting (after the engine): the running move. */
    protected void actieTick(ServerLevel sl, List<ServerPlayer> l) {
    }

    /** True while a move runs that must not be interrupted by chasing (channels, jumps, scenes). */
    protected boolean bezig() {
        return false;
    }

    /** True while the boss is not in a fight at all (a sleeping machine): the engine does nothing. */
    protected boolean slaapt() {
        return false;
    }

    /** Chase speed (navigation multiplier). */
    protected double jaagSnelheid() {
        return 1.1;
    }

    /** Melee reach (from the boss centre, horizontal). */
    protected double bereik() {
        return getBbWidth() / 2 + 2.2;
    }

    protected String[] actieAnimaties() {
        return new String[0];
    }

    @Nullable
    @Override
    public String lootTabel() {
        return null;
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z2bazen." + baasId();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        Tips.toon(p, "vadsrijk.z2bazen.tip." + baasId());
        if (!eersteKeer) {
            rustT = Math.min(rustT, 20);
        }
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return Z2BazenLoot.kist(baasId(), p, mobLevel(), getRandom());
    }

    @Override
    protected void kistGeplaatst(ServerPlayer p, BlockPos pos) {
        kisten.add(pos);
    }

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        kisten.clear();
        super.beloon(sl, helpers);
    }

    // ---- the fight area -----------------------------------------------------------------------------------------------------

    /** The centre of the fight: the arena centre, else where it first stood. */
    public Vec3 zMidden() {
        if (arena != null) {
            return Vec3.atBottomCenterOf(arena.centrum());
        }
        return thuis != null ? thuis : position();
    }

    public void zetThuis(Vec3 v) {
        thuis = v;
    }

    /** The radius of the fight. */
    public double zStraal() {
        return arena != null ? arena.straal() : losseStraal() > 0 ? losseStraal() : 24;
    }

    /** Is this player in the fight? Only they can hurt it and only they are chased. */
    public boolean zDoetMee(Player p) {
        if (p.isSpectator() || !p.isAlive()) {
            return false;
        }
        if (arena != null) {
            return arena.binnen(p.getX(), p.getY(), p.getZ());
        }
        Vec3 m = zMidden();
        double r = zStraal(), dx = p.getX() - m.x, dz = p.getZ() - m.z;
        return dx * dx + dz * dz <= r * r && Math.abs(p.getY() - m.y) < 24;
    }

    /**
     * Everyone the boss fights: the players in the fight that can be hurt (not just fainted, not in a scene). Creative players
     * only when nobody else is there (they can't be hurt, but the boss still shows its moves at them: dev client / AutoCheck).
     */
    public List<ServerPlayer> strijders() {
        List<ServerPlayer> l = new ArrayList<>(), creatief = new ArrayList<>();
        if (!(level() instanceof ServerLevel sl)) {
            return l;
        }
        for (ServerPlayer p : sl.players()) {
            if (!zDoetMee(p) || Flauwvallen.beschermd(p) || RijkScenes.bezig(p)) {
                continue;
            }
            (p.isCreative() ? creatief : l).add(p);
        }
        return l.isEmpty() ? creatief : l;
    }

    /** The current target (also creative players), or null. */
    @Nullable
    public ServerPlayer zDoel() {
        if (doelId != null && level() instanceof ServerLevel sl && sl.getEntity(doelId) instanceof ServerPlayer p && p.isAlive()) {
            return p;
        }
        return null;
    }

    /** A player who stood at range for a while (the furthest such), else the furthest fighter, else null. */
    @Nullable
    public ServerPlayer afstandsDoel() {
        ServerPlayer best = null, verste = null;
        double d = -1, dv = -1;
        for (ServerPlayer p : strijders()) {
            double x = p.distanceToSqr(this);
            if (x > dv) {
                dv = x;
                verste = p;
            }
            if (verT.getOrDefault(p.getUUID(), 0) >= VER_TICKS && x > d) {
                d = x;
                best = p;
            }
        }
        return best != null ? best : verste;
    }

    /** True if someone has stood at range for {@link #VER_TICKS}: then gap closers / lobs go to them first. */
    public boolean iemandOpAfstand() {
        for (ServerPlayer p : strijders()) {
            if (verT.getOrDefault(p.getUUID(), 0) >= VER_TICKS) {
                return true;
            }
        }
        return false;
    }

    /** Where a player stands, kept inside the arena (a landing spot / circle centre). */
    public Vec3 voetVan(LivingEntity p) {
        return voetVan(new Vec3(p.getX(), Math.floor(p.getY() + 0.01), p.getZ()));
    }

    /** This spot, kept inside the arena (a little from the edge). */
    public Vec3 voetVan(Vec3 v) {
        Vec3 m = zMidden();
        Vec3 d = v.subtract(m).multiply(1, 0, 1);
        double max = Math.max(3, zStraal() - 2);
        if (d.length() > max) {
            Vec3 r = m.add(d.normalize().scale(max));
            v = new Vec3(r.x, v.y, r.z);
        }
        return v;
    }

    // ---- damage --------------------------------------------------------------------------------------------------------------

    /** A normal boss hit (factor x its base hit) through the pipeline (defence counts). */
    public void zRaak(ServerPlayer p, double factor, String skill) {
        Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * factor, element(), Schade.Soort.MELEE, false, "z2bazen_" + skill));
    }

    /** A mechanic hit: this fraction of the player's max Vads, true damage (no defence: you should have dodged). */
    public void zMechaniek(ServerPlayer p, double fractie, String skill) {
        Schade.doe(new Schade.Klap(this, p, Spelers.maxVads(p) * fractie, element(), Schade.Soort.WAAR, false, "z2bazen_" + skill));
    }

    /** Hits every fighter within r of v (|dy| < 3); returns how many. */
    public int zRond(Vec3 v, double r, double factor, String skill, @Nullable Status st, int stTicks, double duw) {
        int n = 0;
        for (ServerPlayer p : strijders()) {
            double dx = p.getX() - v.x, dz = p.getZ() - v.z;
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - v.y) < 3) {
                zRaak(p, factor, skill);
                if (st != null) {
                    Statussen.geef(p, st, stTicks, Math.max(0.35, mobLevel() * 0.5), this);
                }
                if (duw > 0) {
                    duw(p, v, duw);
                }
                n++;
            }
        }
        return n;
    }

    public static void duw(ServerPlayer p, Vec3 van, double kracht) {
        Vec3 weg = p.position().subtract(van).multiply(1, 0, 1);
        weg = weg.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : weg.normalize();
        p.setDeltaMovement(weg.x * kracht, 0.45, weg.z * kracht);
        p.hurtMarked = true;
    }

    /** Is the player within a strip from a to b (half width w, |dy| < 3)? */
    public static boolean inStrook(Vec3 p, Vec3 a, Vec3 b, double w) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        double len2 = d.lengthSqr();
        if (len2 < 1e-4) {
            return false;
        }
        Vec3 ap = p.subtract(a).multiply(1, 0, 1);
        double t = Math.max(0, Math.min(1, ap.dot(d) / len2));
        Vec3 dichtst = a.multiply(1, 0, 1).add(d.scale(t));
        double dx = p.x - dichtst.x, dz = p.z - dichtst.z;
        return dx * dx + dz * dz <= w * w && Math.abs(p.y - a.y) < 3;
    }

    /** Zone-2 statuses: "Bevroren" is the systemen slice's new status; until it exists, Plakkerig (slowed) stands in. */
    public static Status bevroren() {
        try {
            return Status.valueOf("BEVROREN");
        } catch (IllegalArgumentException e) {
            return Status.PLAKKERIG;
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (bron instanceof ServerPlayer p && !zDoetMee(p)) {   // no hitting it from outside the arena
            long nu = level().getGameTime();
            if (nu > teVerBericht) {
                teVerBericht = nu + 40;
                p.sendOverlayMessage(Component.translatable("vadsrijk.z2bazen.te_ver").withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        super.krijgSchade(n, bron);
    }

    // ---- the kinematic jump ------------------------------------------------------------------------------------------------

    /** Jumps in an arc to {@code naar} (lands after {@code ticks}); then {@link #zGeland()}. Ignores water, drag and blocks. */
    public void zSpring(Vec3 naar, int ticks, double hoogte) {
        sprongVan = position();
        sprongNaar = naar;
        sprongDuur = Math.max(4, ticks);
        sprongT = 0;
        sprongHoogte = hoogte;
        setNoGravity(true);
        getNavigation().stop();
    }

    public boolean zInSprong() {
        return sprongT >= 0;
    }

    /** A {@link #zSpring} jump landed. */
    protected void zGeland() {
    }

    /** The arena stayed empty: reset (default: away; a registered arena spawns a fresh one, a loose boss decides itself). */
    protected void zLeeg() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 30, 1.5, 1, 1.5, 0.02);
        }
        discard();
    }

    // ---- the tick ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean stil() {
        return super.stil() || zInSprong();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (thuis == null) {
            thuis = position();
        }
        if (sprongT >= 0) {
            sprongT++;
            double f = Math.min(1, (double) sprongT / sprongDuur);
            Vec3 v = sprongVan.lerp(sprongNaar, f).add(0, 4 * sprongHoogte * f * (1 - f), 0);
            setPos(v.x, v.y, v.z);
            setDeltaMovement(Vec3.ZERO);
            resetFallDistance();
            if (f >= 1) {
                sprongT = -1;
                setNoGravity(false);
                for (int up = 0; up < 6 && !sl.noCollision(this); up++) {   // never end up stuck in a block
                    setPos(getX(), Math.floor(getY()) + 1, getZ());
                }
                if (!geplofd()) {
                    zGeland();
                }
            }
        }
        if (geplofd() || slaapt()) {
            return;
        }
        List<ServerPlayer> l = strijders();
        if (tickCount % 5 == 0) {
            beheer(sl, l);
        }
        if (inIntro() || Statussen.heeft(this, Status.VERDOOFD)) {
            return;
        }
        actieTick(sl, l);
        if (zInSprong() || l.isEmpty()) {
            return;
        }
        if (!bezig()) {
            ServerPlayer d = zDoel();
            if (d != null) {
                double dx = d.getX() - getX(), dz = d.getZ() - getZ();
                double afst = Math.sqrt(dx * dx + dz * dz);
                if (afst > bereik() - 0.4) {
                    if (tickCount % 10 == 0) {
                        getNavigation().moveTo(d, jaagSnelheid());
                    }
                } else {
                    getNavigation().stop();
                    if (--bijtT <= 0) {
                        bijtT = 24;
                        bijt(sl, d);
                    }
                }
            }
        }
        if (--rustT <= 0 && !bezig()) {
            rustT = 40;
            aanvallenGedaan++;
            kiesAanval(sl, l);
        }
    }

    /** A melee bite on the current target (default: 1.0 x base hit + a swing). */
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        zRaak(d, 1.0, "bijt");
    }

    private void beheer(ServerLevel sl, List<ServerPlayer> l) {
        Set<UUID> nu = new HashSet<>();
        for (ServerPlayer p : l) {
            nu.add(p.getUUID());
            verT.put(p.getUUID(), p.distanceToSqr(this) > VER * VER ? verT.getOrDefault(p.getUUID(), 0) + 5 : 0);
        }
        verT.keySet().retainAll(nu);
        ServerPlayer d = zDoel();
        if (d == null || !l.contains(d) || (++doelWissel % 16 == 0 && getRandom().nextInt(3) == 0)) {
            ServerPlayer best = null;
            double bd = Double.MAX_VALUE;
            for (ServerPlayer p : l) {
                double x = p.distanceToSqr(this);
                if (x < bd) {
                    bd = x;
                    best = p;
                }
            }
            if (best != null && d != null && l.contains(d) && getRandom().nextBoolean()) {
                best = d;
            }
            d = best;
            doelId = d == null ? null : d.getUUID();
        }
        if (d != null) {
            if (!d.isCreative() && getTarget() != d) {
                setTarget(d);
            }
            getLookControl().setLookAt(d, 30, 30);
        } else if (getTarget() instanceof Player) {
            setTarget(null);
        }
        // a leash back to the middle
        if (!zInSprong()) {
            Vec3 m = zMidden();
            double dx = getX() - m.x, dz = getZ() - m.z;
            buitenT = dx * dx + dz * dz > Math.pow(zStraal() + 2, 2) || Math.abs(getY() - m.y) > 20 ? buitenT + 5 : 0;
            if (buitenT >= 30) {
                buitenT = 0;
                zSpring(m, 20, 6);
            }
        }
        boolean iemand = false;
        for (ServerPlayer p : sl.players()) {
            iemand |= zDoetMee(p);
        }
        leegT = iemand ? 0 : leegT + 5;
        if (leegT >= LEEG_TICKS && vads() < maxVads()) {
            leegT = 0;
            zLeeg();
        }
    }

    /** Overlay text for everyone in the fight. */
    public void zeg(String key, ChatFormatting kleur, Object... args) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (zDoetMee(p)) {
                p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
            }
        }
    }

    /** A big title card for everyone in the fight (phase changes). */
    public void kaart(String key, int kleur) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (zDoetMee(p)) {
                RijkScenes.titel(p, Component.translatable(key).withStyle(ChatFormatting.BOLD), Component.translatable(key + ".onder"), 50, kleur);
            }
        }
    }

    /** Zone-2 defeat: crystal creatures shatter into grey shards that melt away (no pink plof). */
    @Override
    protected void opPlof() {
        if (level() instanceof ServerLevel sl) {
            scherven(sl, position().add(0, getBbHeight() / 2, 0), getBbWidth(), getBbHeight());
        }
    }

    public static void scherven(ServerLevel sl, Vec3 v, double w, double h) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TINTED_GLASS.defaultBlockState()), v.x, v.y, v.z, 60, w / 2, h / 2, w / 2, 0.15);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), v.x, v.y, v.z, 40, w / 2, h / 2, w / 2, 0.1);
        sl.sendParticles(new DustParticleOptions(0x9A9AA6, 1.6f), v.x, v.y, v.z, 40, w / 2, h / 2, w / 2, 0.02);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.7f);
    }

    // ---- GeckoLib -----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk");
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<Z2Baas> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String a : actieAnimaties()) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        }
        controllers.add(actie);
    }

    protected void speel(String anim) {
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

    /** Dev/gametest: is this arena's boss present (kern Arenas)? */
    @Nullable
    public static VadsrijkBaas inArena(ServerLevel sl, String arenaId) {
        return Arenas.baas(sl, arenaId);
    }
}
