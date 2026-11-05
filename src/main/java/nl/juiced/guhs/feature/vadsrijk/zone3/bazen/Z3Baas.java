package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;

import javax.annotation.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas;

/**
 * The base of every zone-3 boss: zone 2's fight engine ({@link Z2Baas}: every player in the arena is a target, also the ones
 * shooting from the far edge; an attack rotation; no hits from outside; a leash; an empty arena resets) plus what zone 3 adds:
 * <ul>
 *   <li>mechanic hits are fractions of the player's max Vads ({@link #mech}): careless players faint after two or three mistakes,
 *       never one-shot (every fraction stays at or below 60 %);</li>
 *   <li>{@link #immuun}/{@link #vadsVloer}: a boss waits at a threshold until its mechanic is done (cages, splinters, balloons),
 *       with a clear line to whoever hits it;</li>
 *   <li>small tasks that run per tick ({@link #taak}: rolling cheese wheels, delayed blasts, stack circles {@link #stapel});</li>
 *   <li>an enrage timer ({@link #enrageTicks}) and the Vahoeg mode ({@link VahoegModus}: lv 52, more HP and damage, one extra
 *       mechanic per boss, and the week seal);</li>
 *   <li>a boss bar for fighters who are not in the registered arena (the Kruinroofvogel's chase), and the zone-3 tone:
 *       rot creatures fall apart in grey fluff and dry leaves ({@link #pluis}), Mika's flee in smoke.</li>
 * </ul>
 * Colours as in zone 2: {@link #ROOD} damage, {@link #ROZE} safe, {@link #PAARS} mechanic, {@link #GEEL} stack.
 */
public abstract class Z3Baas extends Z2Baas {
    public static final int GROEN = 0x6AD06A;
    private final List<IntPredicate> taken = new ArrayList<>();
    private boolean vahoeg;
    private int gevechtT;
    private boolean enraged;
    private long immuunBericht;
    private final Set<UUID> buitenBalk = new HashSet<>();
    /** Gametests: mechanic hits done (count) and the last fraction. */
    public int mechanieken;

    protected Z3Baas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    // ---- identity -------------------------------------------------------------------------------------------------------

    @Override
    public String titelKey() {
        return "vadsrijk.z3bazen." + baasId();
    }

    /** Which dungeon this boss belongs to ("werf" | "hart"), or null (world boss, Pelszaal, nest). */
    @Nullable
    public String kerker() {
        return null;
    }

    /** The last boss of its dungeon (a Vahoeg run ends with it). */
    public boolean eindbaas() {
        return false;
    }

    /** Is this a Mika (flees in smoke) rather than a rot creature (falls apart in fluff)? */
    protected boolean isMika() {
        return false;
    }

    /** HP and hit at this level (Vahoeg multiplies on top). */
    protected abstract double hp(int level);

    protected abstract double klap(int level);

    @Override
    protected final double basisVads(int level) {
        return hp(level) * (vahoeg ? 1.5 : 1);
    }

    @Override
    protected final double basisSchade(int level) {
        return klap(level) * (vahoeg ? 1.25 : 1);
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        Z3Toegang.start(p, this);   // (M-BAZEN) this player is in OUR fight now: other bosses leave them alone
        super.intro(p, eersteKeer);
        Tips.toon(p, "vadsrijk.z3bazen.tip." + baasId());
        if (vahoeg) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.vahoeg.intro", Component.translatable(VahoegModus.zegel().key()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- Vahoeg mode ----------------------------------------------------------------------------------------------------

    public boolean vahoeg() {
        return vahoeg;
    }

    /** Turns this boss into its Vahoeg form (lv 52-equivalent, x1.5 HP, x1.25 damage) with the week seal. */
    public void zetVahoeg(boolean v) {
        if (v == vahoeg) {
            return;
        }
        vahoeg = v;
        stelIn(v ? 52 : mobLevel(), element(), false, Set.of());
        if (v && VahoegModus.zegel() == VahoegModus.Zegel.TAAI) {
            Statussen.geef(this, Status.KNUFFELSCHILD, 20 * 120, maxVads() * 0.15, this);
        }
    }

    /** A telegraph's warning time, shortened under the week seal "Donker". */
    public int tg(int ticks) {
        return vahoeg && VahoegModus.zegel() == VahoegModus.Zegel.DONKER ? Math.max(8, ticks * 4 / 5) : ticks;
    }

    /** How many adds a summon makes (doubled under "Splitsend"). */
    public int adds(int n) {
        return vahoeg && VahoegModus.zegel() == VahoegModus.Zegel.SPLITSEND ? n * 2 : n;
    }

    // ---- damage helpers ---------------------------------------------------------------------------------------------------

    /** A normal boss hit (factor x its hit) through the pipeline (defence counts). */
    public void raak(ServerPlayer p, double factor, String skill) {
        Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * factor * (enraged ? 1.5 : 1), element(), Schade.Soort.MELEE, false, "z3bazen_" + skill));
    }

    /** A mechanic hit: this fraction of the player's max Vads, true damage. Kept at 60 % or less: careless = faint, never a one-shot. */
    public void mech(ServerPlayer p, double fractie, String skill) {
        mechanieken++;
        double f = Math.min(0.6, fractie * (enraged ? 1.3 : 1) * (vahoeg ? 1.1 : 1));
        Schade.doe(new Schade.Klap(this, p, Spelers.maxVads(p) * f, element(), Schade.Soort.WAAR, false, "z3bazen_" + skill));
    }

    /** Mechanic hit on every fighter within r of v (|dy| < 3); returns the players hit. */
    public List<ServerPlayer> mechRond(Vec3 v, double r, double fractie, String skill) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : strijders()) {
            double dx = p.getX() - v.x, dz = p.getZ() - v.z;
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - v.y) < 3.5) {
                mech(p, fractie, skill);
                l.add(p);
            }
        }
        return l;
    }

    public List<ServerPlayer> inCirkel(Vec3 v, double r) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : strijders()) {
            double dx = p.getX() - v.x, dz = p.getZ() - v.z;
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - v.y) < 3.5) {
                l.add(p);
            }
        }
        return l;
    }

    /** Is the boss immune right now (waiting for its mechanic)? Then hits do nothing and the hitter is told {@link #immuunKey}. */
    protected boolean immuun() {
        return false;
    }

    protected String immuunKey() {
        return "vadsrijk.z3bazen.immuun";
    }

    /** The HP fraction it can't drop below right now (0 = none): a threshold that waits for a mechanic. */
    protected double vadsVloer() {
        return 0;
    }

    /** Damage factor for this hit (armour plates, the lantern-light rule...). */
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return 1;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (geplofd()) {
            return;
        }
        if (immuun()) {
            if (bron instanceof ServerPlayer p && level().getGameTime() > immuunBericht) {
                immuunBericht = level().getGameTime() + 30;
                p.sendOverlayMessage(Component.translatable(immuunKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        n *= schadeFactor(bron);
        double vloer = vadsVloer() * maxVads();
        if (vloer > 0 && vads() - n < vloer) {
            n = vads() - vloer;
            if (n <= 0.01) {
                if (bron instanceof ServerPlayer p && level().getGameTime() > immuunBericht) {
                    immuunBericht = level().getGameTime() + 30;
                    p.sendOverlayMessage(Component.translatable(immuunKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return;
            }
        }
        super.krijgSchade(n, bron);
    }

    /**
     * (M-BAZEN) In the arena AND allowed ({@link Z3Toegang}): not mid-fight with another zone-3 boss, and this boss's dungeon
     * predecessor is beaten. Otherwise it does not target them, and their hits do nothing.
     */
    @Override
    public boolean zDoetMee(net.minecraft.world.entity.player.Player p) {
        return super.zDoetMee(p) && (!(p instanceof ServerPlayer sp) || Z3Toegang.mag(sp, this));
    }

    /** (M-BAZEN) kern asks before the intro: only when allowed; else a short "why" overlay, and it asks again next tick. */
    @Override
    public boolean magIntro(ServerPlayer p) {
        if (Z3Toegang.mag(p, this)) {
            return true;
        }
        Z3Toegang.waarschuw(p, this);
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        Z3Toegang.klaar(this);
        super.remove(reason);
    }

    /** Like zone 2's, plus kern's {@link #testFilter} (gametests run side by side: each boss only fights its own mock players). */
    @Override
    public List<ServerPlayer> strijders() {
        List<ServerPlayer> l = new ArrayList<>(), creatief = new ArrayList<>();
        if (!(level() instanceof ServerLevel sl)) {
            return l;
        }
        for (ServerPlayer p : sl.players()) {
            if (!testFilter.test(p) || !zDoetMee(p) || nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.beschermd(p)
                    || nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.bezig(p)) {
                continue;
            }
            (p.isCreative() ? creatief : l).add(p);
        }
        return l.isEmpty() ? creatief : l;
    }

    // ---- hooks for mechanic objects -------------------------------------------------------------------------------------

    /** One of its {@link Z3Doel}s was hit (n = damage). */
    public void doelGeraakt(Z3Doel d, double n, @Nullable LivingEntity bron) {
    }

    /** One of its {@link Z3Doel}s broke. */
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
    }

    /** A player right-clicked one of its {@link Z3Doel}s. */
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
    }

    /** A player right-clicked an unlit lantern post in its arena: true = it lights (the block is set by the caller). */
    public boolean paalAan(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        return false;
    }

    /** A held player ({@link Z3Vast}) got loose by clicking / with help. */
    public void losgekomen(ServerPlayer p, String soort) {
    }

    // ---- small per-tick tasks --------------------------------------------------------------------------------------------

    /** Runs {@code t} every tick (argument = ticks since start) until it returns true. Dropped on defeat/reset. */
    public void taak(IntPredicate t) {
        taken.add(new IntPredicate() {
            int i;

            @Override
            public boolean test(int ignored) {
                return t.test(i++);
            }
        });
    }

    /** Gametests: tasks still waiting. */
    public int takenOpen() {
        return taken.size();
    }

    /** Runs {@code r} after {@code ticks}. */
    public void later(int ticks, Runnable r) {
        taak(t -> {
            if (t >= ticks) {
                r.run();
                return true;
            }
            return false;
        });
    }

    /**
     * A rolling thing (fluff ball, cheese wheel, falling crate): a red strip a->b for {@code waarschuw} ticks, then a ball rolls
     * from a to b in {@code duur} ticks and hits every fighter it passes (once) with {@code fractie} of their max Vads.
     */
    public void rol(Vec3 a, Vec3 b, double breed, int waarschuw, int duur, double fractie, String skill, ParticleOptions deeltje, @Nullable Status st) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        int w = tg(waarschuw);
        Telegraaf.lijn(sl, a, b, breed, w, ROOD);
        Set<UUID> geraakt = new HashSet<>();
        taak(t -> {
            if (t < w) {
                return false;
            }
            double f = Math.min(1, (double) (t - w) / duur);
            Vec3 v = a.lerp(b, f);
            sl.sendParticles(deeltje, v.x, v.y + 0.6, v.z, 8, breed * 0.25, 0.3, breed * 0.25, 0.02);
            if ((t - w) % 6 == 0) {
                sl.playSound(null, v.x, v.y, v.z, SoundEvents.WOOL_STEP, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
            for (ServerPlayer p : strijders()) {
                if (!geraakt.contains(p.getUUID()) && p.position().distanceToSqr(v.x, p.getY(), v.z) < breed * breed / 4 + 0.6 && Math.abs(p.getY() - v.y) < 2.5) {
                    geraakt.add(p.getUUID());
                    mech(p, fractie, skill);
                    if (st != null) {
                        Statussen.geef(p, st, 50, 0.4, this);
                    }
                    duw(p, v, 0.7);
                }
            }
            return f >= 1;
        });
    }

    /** A red circle that goes off after {@code ticks}: everyone inside takes {@code fractie}. */
    public void inslag(Vec3 v, double r, int ticks, double fractie, String skill, @Nullable Status st, ParticleOptions deeltje) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        int w = tg(ticks);
        Telegraaf.cirkel(sl, v, r, w, ROOD);
        later(w, () -> {
            sl.sendParticles(deeltje, v.x, v.y + 0.4, v.z, 30, r * 0.4, 0.3, r * 0.4, 0.05);
            sl.playSound(null, v.x, v.y, v.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.3f);
            for (ServerPlayer p : mechRond(v, r, fractie, skill)) {
                if (st != null) {
                    Statussen.geef(p, st, 60, 0.4, this);
                }
            }
        });
    }

    /**
     * A yellow stack circle: after {@code ticks}, the hit ({@code fractie} of max Vads) is shared by everyone inside. Fewer than
     * {@code nodig} players inside (capped at the number of fighters, so solo = 1) = the circle "fails" ({@code mislukt} runs and
     * everyone in the fight takes {@code straf}). Returns nothing; the outcome goes to {@code klaar} (true = held).
     */
    public void stapel(Vec3 v, double r, int ticks, double fractie, double straf, int nodig, String skill, java.util.function.Consumer<Boolean> klaar) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        int w = tg(ticks);
        Telegraaf.cirkel(sl, v, r, w, GEEL);
        later(w, () -> {
            List<ServerPlayer> in = inCirkel(v, r);
            int moet = Math.max(1, Math.min(nodig, strijders().size()));
            boolean ok = in.size() >= moet;
            sl.sendParticles(new DustParticleOptions(GEEL, 2.2f), v.x, v.y + 0.3, v.z, 60, r * 0.5, 0.2, r * 0.5, 0);
            if (ok) {
                for (ServerPlayer p : in) {
                    mech(p, fractie / in.size(), skill);
                }
            } else {
                for (ServerPlayer p : strijders()) {
                    mech(p, straf, skill + "_mis");
                }
            }
            klaar.accept(ok);
        });
    }

    // ---- enrage -----------------------------------------------------------------------------------------------------------

    /** Fight time until the enrage (default 7 min; 25 % earlier under the week seal "Haastig"). */
    protected int enrageTicks() {
        return 20 * 60 * 7;
    }

    public boolean enraged() {
        return enraged;
    }

    /** The enrage starts (default: a title + a pulse every 2 s on everyone, 8 %). */
    protected void enrage(ServerLevel sl) {
        kaart("vadsrijk.z3bazen.enrage", ROOD);
    }

    /** Every 40 ticks while enraged (default: 8 % on everyone, grey-blue mould). */
    protected void enragePuls(ServerLevel sl, List<ServerPlayer> l) {
        for (ServerPlayer p : l) {
            mech(p, 0.08, "enrage");
            sl.sendParticles(new DustParticleOptions(0x7A8AA0, 2f), p.getX(), p.getY() + 1, p.getZ(), 20, 0.6, 0.6, 0.6, 0);
        }
    }

    // ---- the tick -----------------------------------------------------------------------------------------------------------

    @Override
    protected void actieTick(ServerLevel sl, List<ServerPlayer> l) {
        if (!l.isEmpty()) {
            gevechtT++;
            int e = enrageTicks();
            if (vahoeg && VahoegModus.zegel() == VahoegModus.Zegel.HAASTIG) {
                e = e * 3 / 4;
            }
            if (!enraged && gevechtT >= e) {
                enraged = true;
                enrage(sl);
            }
            if (enraged && gevechtT % 40 == 0) {
                enragePuls(sl, l);
            }
        }
        if (l.isEmpty() && !vechtZonderStrijders()) {
            return;   // (K-INT) nobody fighting (e.g. an arena ticking in loaded chunks far away): the bosses pick l.get(random) in vecht
        }
        vecht(sl, l);
    }

    /** (K-INT) Must vecht run although nobody fights right now? Default no (vecht picks random fighters). */
    protected boolean vechtZonderStrijders() {
        return false;
    }

    /** The boss's own per-tick logic (instead of actieTick). */
    protected abstract void vecht(ServerLevel sl, List<ServerPlayer> l);

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && !geplofd()) {
            for (Iterator<IntPredicate> it = new ArrayList<>(taken).iterator(); it.hasNext(); ) {
                IntPredicate t = it.next();
                if (t.test(0)) {
                    taken.remove(t);
                }
            }
            if (tickCount % 5 == 0) {
                buitenBalk(sl);
            }
        }
    }

    /** Fighters outside the registered arena (a chase, a loose fight) get the boss bar from here. */
    private void buitenBalk(ServerLevel sl) {
        Set<UUID> nu = new HashSet<>();
        if (arena() != null) {
            for (ServerPlayer p : strijders()) {
                if (!arena().binnen(p.getX(), p.getY(), p.getZ())) {
                    nu.add(p.getUUID());
                    KernPayloads.stuur(p, new KernPayloads.BaasBalk(getId(), Component.translatable(titelKey()), fase(), (float) vads(), (float) maxVads(),
                            muziek() == null ? "" : muziek().toString()));
                }
            }
        }
        for (UUID weg : buitenBalk) {
            if (!nu.contains(weg) && sl.getEntity(weg) instanceof ServerPlayer p && (arena() == null || !arena().binnen(p.getX(), p.getY(), p.getZ()))) {
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, Component.empty(), 0, 0, 0));
            }
        }
        buitenBalk.clear();
        buitenBalk.addAll(nu);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            taken.clear();
            for (UUID weg : buitenBalk) {
                if (sl.getEntity(weg) instanceof ServerPlayer p) {
                    KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, Component.empty(), 0, 0, 0));
                }
            }
            for (ServerPlayer p : sl.players()) {   // (M-BAZEN) the dungeon order: the next boss wakes for everyone who helped
                if (bijdragen().spelers().contains(p.getUUID()) && !arenaId.isEmpty()) {
                    Z3Toegang.verslagen(p, arenaId, sl.getGameTime());
                }
            }
            Z3Toegang.klaar(this);
            if (vahoeg && eindbaas() && kerker() != null) {
                VahoegModus.runKlaar(sl, kerker());
            }
            Z3BazenApi.verslagen(this);
        }
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return Z3BazenLoot.kist(baasId(), p, Math.min(50, mobLevel()), getRandom(), vahoeg, eindbaas());
    }

    // ---- the zone-3 tone ------------------------------------------------------------------------------------------------

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (isMika()) {
            rook(sl, position(), getBbWidth());
        } else {
            pluis(sl, position().add(0, getBbHeight() / 2, 0), getBbWidth(), getBbHeight());
        }
    }

    /** A rot creature falls apart in grey fluff and dry leaves that blow away. */
    public static void pluis(ServerLevel sl, Vec3 v, double w, double h) {
        sl.sendParticles(new DustParticleOptions(0x9A9AA6, 2.0f), v.x, v.y, v.z, 70, w / 2, h / 2, w / 2, 0.04);
        sl.sendParticles(ParticleTypes.WHITE_ASH, v.x, v.y, v.z, 60, w / 2, h / 2, w / 2, 0.08);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LEAVES.defaultBlockState()), v.x, v.y, v.z, 40, w / 2, h / 2, w / 2, 0.15);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, v.x, v.y, v.z, 6, w / 3, h / 3, w / 3, 0.01);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 2f, 0.7f);
    }

    /** A Mika flees in a smoke puff ("Dit komt in mijn rapport!"). */
    public static void rook(ServerLevel sl, Vec3 v, double w) {
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 0.8, v.z, 40, w / 2 + 0.3, 0.8, w / 2 + 0.3, 0.04);
        sl.sendParticles(ParticleTypes.POOF, v.x, v.y + 0.8, v.z, 25, w / 2, 0.6, w / 2, 0.06);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 1.5f);
    }

    public static Vec3 ring(Vec3 c, double r, double hoek) {
        return c.add(Math.cos(hoek) * r, 0, Math.sin(hoek) * r);
    }

    public BlockPos blok(int[] o) {
        return BlockPos.containing(zMidden()).offset(o[0], o[1], o[2]);
    }

    public Vec3 punt(int[] o) {
        return Vec3.atBottomCenterOf(blok(o));
    }

    /** Overlay for every fighter (shorthand). */
    public void zegAlle(String key, ChatFormatting kleur, Object... args) {
        zeg(key, kleur, args);
    }

    /** A chat line ("speech") for every fighter: the boss's name + the text. */
    public void spreek(String key, Object... args) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Component c = Component.translatable(titelKey()).withStyle(ChatFormatting.GOLD).append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 60 * 60) {
                p.sendSystemMessage(c);
            }
        }
    }

    // ---- GeckoLib (own animation names per model) -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk");
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<Z3Baas> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String a : actieAnimaties()) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        }
        controllers.add(actie);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("vr_z3_vahoeg", vahoeg);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        vahoeg = in.getBooleanOr("vr_z3_vahoeg", false);
    }
}
