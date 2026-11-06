package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The base of every boss (CONTRACT_VADSRIJK 6): phases by HP ({@link #fases}: the thresholds, e.g. {0.5, 0.2} = phase 2 at 50 %,
 * phase 3 at 20 %), the intro (first time a 5 s camera swoop with {@link #signatuur} + a big title card, later a 1.5 s card),
 * HP scaling with the players in the arena, contribution tracking ({@link Bijdrage}) and a personal {@link BaasKist} for
 * everyone above 5 %. {@link Arenas} spawns it, scales it and keeps the respawn board.
 */
public abstract class VadsrijkBaas extends VadsrijkMob {
    public static final int INTRO_TICKS = 100, KORT_TICKS = 30;
    protected final Bijdrage bijdragen = new Bijdrage();
    @Nullable
    protected Arena arena;
    public String arenaId = "";
    private int fase = 1;
    /** Game time until which it stands still for the intro. */
    protected long introTot;
    private int spelersGeschaald = 1;

    protected VadsrijkBaas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** HP thresholds (fractions, descending) where the next phase starts. */
    protected abstract double[] fases();

    /** Called once when a phase starts (2, 3, ...). */
    protected void opFase(int fase) {
    }

    public abstract String titelKey();

    public String ondertitelKey() {
        return titelKey() + ".onder";
    }

    /**
     * A boss without a registered {@link Arena} (e.g. the Mika-spion, started by a quest): return the radius around it in which
     * players get the boss bar (and count for HP scaling). Contribution (damage, tanking, and heals by players within this radius)
     * and the personal chests work the same. 0 = not a loose boss (default: arena bosses get their bar from {@link Arenas}).
     * Play the intro yourself with {@link #intro} when the fight starts.
     */
    public int losseStraal() {
        return 0;
    }

    private final java.util.Set<java.util.UUID> losseKijkers = new java.util.HashSet<>();

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && level() instanceof ServerLevel sl0) {
            vechtTick(sl0);
            nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.tick(this, sl0);   // T-VFX: the bouwdoos attack, soft enrage
        }
        if (level().isClientSide() || tickCount % 5 != 0 || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (arena != null || losseStraal() <= 0 || geplofd()) {
            // (integration: a loose boss that stops fighting, e.g. the Mika-spion fleeing, clears the bars it showed)
            for (java.util.UUID weg : losseKijkers) {
                if (sl.getEntity(weg) instanceof ServerPlayer p) {
                    KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, net.minecraft.network.chat.Component.empty(), 0, 0, 0));
                }
            }
            losseKijkers.clear();
            return;
        }
        double r = losseStraal();
        java.util.Set<java.util.UUID> nu = new java.util.HashSet<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.distanceToSqr(this) <= r * r) {
                nu.add(p.getUUID());
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(getId(), net.minecraft.network.chat.Component.translatable(titelKey()), fase(), (float) vads(),
                        (float) maxVads(), muziek() == null ? "" : muziek().toString()));
            }
        }
        for (java.util.UUID weg : losseKijkers) {
            if (!nu.contains(weg) && sl.getEntity(weg) instanceof ServerPlayer p) {
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, net.minecraft.network.chat.Component.empty(), 0, 0, 0));
            }
        }
        losseKijkers.clear();
        losseKijkers.addAll(nu);
        schaal(nu.size());
    }

    // ---- fighting (fix round 1: every boss really fights) -------------------------------------------------------------------
    // The bosses used to wait for vanilla's NearestAttackableTargetGoal. That never picks a creative player (26.1), loses a
    // player beyond the follow range, and the jumps were plain velocity (in the pond's water the drag ate them). Now kern
    // picks the target itself among everyone in the arena, jumps are kinematic arcs, lobs punish standing at range, hits from
    // outside the arena do nothing, and an empty arena resets the boss.

    /** A lob in flight: a blob from the boss to {@code plek}, landing after {@code duur} ticks (red circle meanwhile). */
    private record Inslag(Vec3 van, Vec3 plek, double straal, int duur, int[] t, double factor, String skill, @Nullable nl.juiced.guhs.feature.vadsrijk.kern.Status st,
            int stTicks, net.minecraft.core.particles.ParticleOptions deeltje) {
    }

    private final java.util.List<Inslag> inslagen = new java.util.ArrayList<>();
    @Nullable
    private java.util.UUID doelId;
    private int doelWissel, buitenT, leegT;
    private long teVerBericht;
    /** Per player: ticks (in steps of 5) they stood far away from the boss in a row. */
    private final java.util.Map<java.util.UUID, Integer> verT = new java.util.HashMap<>();
    @Nullable
    private Vec3 thuis;
    // the kinematic jump
    private int sprongT = -1, sprongDuur;
    private Vec3 sprongVan = Vec3.ZERO, sprongNaar = Vec3.ZERO;
    private double sprongHoogte;

    /** Distance from which standing away from the boss counts as "at range" (punished after {@link #VER_TICKS}). */
    public static final double VER = 9;
    public static final int VER_TICKS = 50;

    /** The centre of the fight: the arena centre, else where it first stood. */
    public Vec3 gevechtsMidden() {
        if (arena != null) {
            return Vec3.atBottomCenterOf(arena.centrum());
        }
        return thuis != null ? thuis : position();
    }

    /** The radius of the fight: the arena's, a loose boss's {@link #losseStraal}, else 24. */
    public double gevechtsStraal() {
        return arena != null ? arena.straal() : losseStraal() > 0 ? losseStraal() : 24;
    }

    /** Is this player in the fight (inside the arena / near a loose boss)? Only they can hurt it, only they are chased. */
    public boolean inGevecht(net.minecraft.world.entity.player.Player p) {
        if (p.isSpectator()) {
            return false;
        }
        if (arena != null) {
            return arena.binnen(p.getX(), p.getY(), p.getZ());
        }
        double r = gevechtsStraal();
        return p.distanceToSqr(this) <= r * r;
    }

    /**
     * Everyone the boss fights: players in the fight who can be hurt (not fainted/protected, not watching a scene). Creative
     * players only when nobody else is there: they can't be hurt, but the boss still shows his moves at them.
     */
    /** Gametests only: which players may count as fighters (parallel tests leave mock players all around). */
    public java.util.function.Predicate<ServerPlayer> testFilter = p -> true;

    public java.util.List<ServerPlayer> vechters() {
        java.util.List<ServerPlayer> l = new java.util.ArrayList<>(), creatief = new java.util.ArrayList<>();
        if (!(level() instanceof ServerLevel sl)) {
            return l;
        }
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || !testFilter.test(p) || !inGevecht(p) || nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.beschermd(p)
                    || nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.bezig(p)) {
                continue;
            }
            (p.isCreative() ? creatief : l).add(p);
        }
        return l.isEmpty() ? creatief : l;
    }

    /** The player it is after right now (kern's own choice, also creative players), or null. */
    @Nullable
    public ServerPlayer doel() {
        if (doelId != null && level() instanceof ServerLevel sl && sl.getEntity(doelId) instanceof ServerPlayer p && p.isAlive()) {
            return p;
        }
        return null;
    }

    /** The player furthest away from it, or null. */
    @Nullable
    public ServerPlayer verste() {
        ServerPlayer best = null;
        double d = -1;
        for (ServerPlayer p : vechters()) {
            double x = p.distanceToSqr(this);
            if (x > d) {
                d = x;
                best = p;
            }
        }
        return best;
    }

    /** A player who has stood at range ({@link #VER}+ blocks) for {@link #VER_TICKS} ticks: punish them (the furthest such), or null. */
    @Nullable
    public ServerPlayer afstandsSpeler() {
        ServerPlayer best = null;
        double d = -1;
        for (ServerPlayer p : vechters()) {
            if (verT.getOrDefault(p.getUUID(), 0) >= VER_TICKS && p.distanceToSqr(this) > d) {
                d = p.distanceToSqr(this);
                best = p;
            }
        }
        return best;
    }

    /** The target for the next move: a ranged player first (they get punished), else {@link #doel()}. */
    @Nullable
    public ServerPlayer volgendDoel() {
        ServerPlayer v = afstandsSpeler();
        return v != null && getRandom().nextInt(3) > 0 ? v : doel();
    }

    /** Where a player stands, kept inside the arena (a little from the edge): a landing spot / lob target. */
    public Vec3 voetenVan(LivingEntity p) {
        Vec3 v = new Vec3(p.getX(), Math.floor(p.getY() + 0.01), p.getZ());
        if (arena != null) {
            Vec3 m = gevechtsMidden();
            Vec3 d = v.subtract(m).multiply(1, 0, 1);
            double max = Math.max(3, arena.straal() - 2);
            if (d.length() > max) {
                Vec3 r = m.add(d.normalize().scale(max));
                v = new Vec3(r.x, v.y, r.z);
            }
        }
        return v;
    }

    /** Jumps in an arc to {@code naar} (lands after {@code ticks}); then {@link #geland()} is called. Ignores water and drag. */
    public void springNaar(Vec3 naar, int ticks, double hoogte) {
        sprongVan = position();
        sprongNaar = naar;
        sprongDuur = Math.max(4, ticks);
        sprongT = 0;
        sprongHoogte = hoogte;
        setNoGravity(true);
        getNavigation().stop();
    }

    public boolean inSprong() {
        return sprongT >= 0;
    }

    /** Called when a {@link #springNaar} jump lands (default: nothing). */
    protected void geland() {
    }

    /**
     * Lobs a blob at where this player stands: a red circle shows where it lands, a trail flies there, and after {@code ticks}
     * everyone within {@code straal} gets {@code factor} x the boss's hit (+ an optional status).
     */
    public void lob(LivingEntity p, double straal, int ticks, double factor, String skill, @Nullable nl.juiced.guhs.feature.vadsrijk.kern.Status st, int stTicks,
            net.minecraft.core.particles.ParticleOptions deeltje, int kleur) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 plek = voetenVan(p);
        Telegraaf.cirkel(sl, plek, straal, ticks, kleur);
        inslagen.add(new Inslag(position().add(0, getBbHeight() * 0.8, 0), plek, straal, ticks, new int[1], factor, skill, st, stTicks, deeltje));
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        sl.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.LLAMA_SPIT, net.minecraft.sounds.SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** Hits every fighter within r of v (|dy| < 3) with factor x the boss's hit; returns how many were hit. */
    public int raakRond(Vec3 v, double r, double factor, String skill, @Nullable nl.juiced.guhs.feature.vadsrijk.kern.Status st, int stTicks, double duw) {
        int n = 0;
        for (ServerPlayer p : vechters()) {
            double dx = p.getX() - v.x, dz = p.getZ() - v.z;
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - v.y) < 3) {
                nl.juiced.guhs.feature.vadsrijk.kern.Schade.doe(new nl.juiced.guhs.feature.vadsrijk.kern.Schade.Klap(this, p, basisSchade(mobLevel()) * factor,
                        element(), nl.juiced.guhs.feature.vadsrijk.kern.Schade.Soort.MELEE, false, skill));
                if (st != null) {
                    nl.juiced.guhs.feature.vadsrijk.kern.Statussen.geef(p, st, stTicks, Math.max(0.4, mobLevel() * 0.5), this);
                }
                if (duw > 0) {
                    Vec3 weg = new Vec3(dx, 0, dz);
                    weg = weg.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : weg.normalize();
                    p.setDeltaMovement(weg.x * duw, 0.5, weg.z * duw);
                    p.hurtMarked = true;
                }
                n++;
            }
        }
        return n;
    }

    /**
     * (integration) True for a boss with its own fight engine (zone 2's {@code Z2Baas}: own targets, leash, empty-arena reset):
     * kern then only flies the lobs and kinematic jumps, so the two engines never both leash/reset/retarget.
     */
    protected boolean eigenGevecht() {
        return false;
    }

    /** Resets the fight when the arena stayed empty (default: away, the arena spawns a fresh one). */
    protected void leegReset() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 30, 1.5, 1, 1.5, 0.02);
        }
        discard();
    }

    private void vechtTick(ServerLevel sl) {
        if (thuis == null) {
            thuis = position();
        }
        // lobs in flight (also after the plof: what's thrown lands)
        for (java.util.Iterator<Inslag> it = inslagen.iterator(); it.hasNext(); ) {
            Inslag i = it.next();
            int t = ++i.t()[0];
            double f = (double) t / i.duur();
            Vec3 v = i.van().lerp(i.plek(), f).add(0, 4 * Math.max(3, i.van().distanceTo(i.plek()) * 0.35) * f * (1 - f), 0);
            sl.sendParticles(i.deeltje(), v.x, v.y, v.z, 3, 0.15, 0.15, 0.15, 0.01);
            if (t >= i.duur()) {
                it.remove();
                sl.sendParticles(i.deeltje(), i.plek().x, i.plek().y + 0.4, i.plek().z, 40, i.straal() * 0.4, 0.3, i.straal() * 0.4, 0.05);
                sl.playSound(null, net.minecraft.core.BlockPos.containing(i.plek()), net.minecraft.sounds.SoundEvents.SLIME_SQUISH, net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 0.6f);
                if (!geplofd()) {
                    raakRond(i.plek(), i.straal(), i.factor(), i.skill(), i.st(), i.stTicks(), 0.4);
                }
            }
        }
        // the kinematic jump
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
                    geland();
                }
            }
        }
        if (geplofd() || tickCount % 5 != 0 || eigenGevecht()) {
            return;
        }
        java.util.List<ServerPlayer> l = vechters();
        // range memory
        java.util.Set<java.util.UUID> nu = new java.util.HashSet<>();
        for (ServerPlayer p : l) {
            nu.add(p.getUUID());
            verT.put(p.getUUID(), p.distanceToSqr(this) > VER * VER ? verT.getOrDefault(p.getUUID(), 0) + 5 : 0);
        }
        verT.keySet().retainAll(nu);
        // the target: keep it while it's in the fight; every few seconds the nearest (or a hitter) may take over
        ServerPlayer d = doel();
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
        } else if (getTarget() instanceof net.minecraft.world.entity.player.Player) {
            setTarget(null);
        }
        // registered arenas: a leash back to the middle, and a reset after 30 s without anyone inside
        if (arena != null && Arenas.van(arena.id()) == arena && !inSprong()) {
            Vec3 m = gevechtsMidden();
            double dx = getX() - m.x, dz = getZ() - m.z;
            buitenT = dx * dx + dz * dz > Math.pow(arena.straal() + 2, 2) ? buitenT + 5 : 0;
            if (buitenT >= 30) {
                buitenT = 0;
                springNaar(m, 20, 6);
            }
            boolean iemand = false;
            for (ServerPlayer p : sl.players()) {
                iemand |= !p.isSpectator() && arena.binnen(p.getX(), p.getY(), p.getZ());
            }
            leegT = iemand ? 0 : leegT + 5;
            if (leegT >= 600 && vads() < maxVads()) {
                leegReset();
            }
        }
    }

    /** The signature move during the first intro (default: nothing). */
    protected void signatuur() {
    }

    /**
     * The boss music (optional hook): a sound event id the client loops while the boss bar shows, only if a sound with that id
     * exists in the resources (none yet: kern registers {@code guhs:vr.kern.muziek.baas} without an asset). Null = no music.
     */
    @Nullable
    public net.minecraft.resources.Identifier muziek() {
        return KernMuziek.BAAS;
    }

    public int fase() {
        return fase;
    }

    public Bijdrage bijdragen() {
        return bijdragen;
    }

    @Nullable
    public Arena arena() {
        return arena;
    }

    public void zetArena(Arena a) {
        this.arena = a;
        this.arenaId = a.id();
    }

    public boolean inIntro() {
        return level().getGameTime() < introTot;
    }

    /** Max Vads for this many players in the arena (+75 % per extra player), keeping the HP fraction. */
    public void schaal(int spelers) {
        spelers = Math.max(1, spelers);
        if (spelers == spelersGeschaald || geplofd()) {
            return;
        }
        spelersGeschaald = spelers;
        zetMaxVads(basisVads(mobLevel()) * (1 + 0.75 * (spelers - 1)), true);
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (inIntro()) {
            return;
        }
        if (bron instanceof ServerPlayer p && !inGevecht(p)) {   // fix round 1: no hitting him from outside the arena
            long nu = level().getGameTime();
            if (nu > teVerBericht) {
                teVerBericht = nu + 40;
                p.sendOverlayMessage(Component.translatable("vadsrijk.kern.baas.te_ver").withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        if (bron instanceof ServerPlayer p) {
            bijdragen.schade(p.getUUID(), Math.min(n, vads()));
        }
        double voor = vads();   // T-VFX
        super.krijgSchade(n, bron);
        nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.geraakt(this, voor - vads());   // T-VFX: interrupt meter + hit flinch
        double[] f = fases();
        double frac = vads() / Math.max(1, maxVads());
        while (!geplofd() && fase - 1 < f.length && frac <= f[fase - 1]) {
            fase++;
            nl.juiced.guhs.feature.vadsrijk.kern.beleving.Beleving.faseMoment(this, fase);   // BELEVING (fix round 1): the phase mini-moment
            nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.fase(this, fase);   // T-VFX: rear up, shockwave, light
            opFase(fase);
        }
    }

    @Override
    public boolean stil() {
        return super.stil() || inIntro();
    }

    /**
     * (M-BAZEN) May this player be welcomed into the fight now? false = the arena treats them as not inside yet (no intro, no
     * boss bar, not counted for the HP scaling) and asks again next tick: a boss whose dungeon predecessor still stands, or a
     * player who is mid-fight with another boss (no camera theft). Default: always.
     */
    public boolean magIntro(ServerPlayer p) {
        return true;
    }

    /** The intro for one player: a camera swoop + title the first time, else a short title card. */
    public void intro(ServerPlayer p, boolean eersteKeer) {
        Component titel = Component.translatable(titelKey()).withStyle(ChatFormatting.BOLD);
        Component onder = Component.translatable(ondertitelKey());
        if (!eersteKeer) {
            RijkScenes.titel(p, titel, onder, KORT_TICKS, 0xC23A3A);
            return;
        }
        Vec3 b = position().add(0, getBbHeight() * 0.6, 0);
        Vec3 van = p.getEyePosition();
        Vec3 richting = van.subtract(b).multiply(1, 0, 1).normalize();
        Vec3 dichtbij = b.add(richting.scale(getBbWidth() * 2.2 + 5)).add(0, 2.5, 0);
        Vec3 opzij = dichtbij.add(new Vec3(-richting.z, 0, richting.x).scale(5)).add(0, 1.5, 0);
        RijkScene s = RijkScene.maak("baas_" + arenaId, INTRO_TICKS)
                .camera(0, van, b)
                .camera(30, dichtbij, b)
                .cameraVolgt(60, dichtbij.add(0, 1, 0), getId())
                .cameraVolgt(INTRO_TICKS, opzij, getId())
                .titel(titelKey(), ondertitelKey(), 45, 55, 0xC23A3A)
                .bouw();
        RijkScenes.speel(p, s);
        introTot = level().getGameTime() + INTRO_TICKS;
        signatuur();
    }

    /**
     * What goes into this player's personal chest (only called for players above 5 % contribution). Default: a roll of
     * {@link #lootTabel()} (items + Vadsmunten), or nothing without a table. Override for first-kill rewards, named items...
     * {@code eersteKill}: this player never beat this boss type before (remembered per player).
     */
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        LootTabel t = lootTabel() == null ? null : LootTabellen.van(lootTabel());
        return t == null ? new LootTabel.Resultaat(java.util.List.of(), 0) : t.rol(mobLevel(), getRandom());
    }

    /** Where the personal chests go around (default: the arena centre, else the boss). Integration hook (the Mika's cap). */
    protected Vec3 kistMidden() {
        return arena != null ? Vec3.atBottomCenterOf(arena.centrum()) : position();
    }

    /** Called after a personal chest was placed (default: nothing). */
    protected void kistGeplaatst(ServerPlayer p, net.minecraft.core.BlockPos pos) {
    }

    /** The id under which a player's first kill of this boss is remembered ({@code SpelerData.gezien}). */
    protected String killSleutel() {
        return "baaskill:" + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType());
    }

    /** T-VFX: the boss's base hit (the bouwdoos multiplies it per attack step). */
    public double tVfxSlag() {
        return basisSchade(mobLevel());
    }

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.verslagen(this);   // T-VFX: the cheer-up finale
        int kist = 0;
        for (java.util.UUID id : bijdragen.spelers()) {
            if (!(sl.getEntity(id) instanceof ServerPlayer p) || p.distanceTo(this) > 64) {
                continue;
            }
            Spelers.geefXpGedeeld(p, xp(), "baas");
            Spelers.data(p).opgevrolijkt++;
            boolean eerste = Spelers.data(p).gezien.add(killSleutel());
            if (bijdragen.verdientKist(id)) {
                LootTabel.Resultaat r = kistInhoud(p, eerste);
                if (r != null && (!r.items().isEmpty() || r.munten() > 0)) {
                    double a = kist++ * 2.4;
                    Vec3 plek = kistMidden().add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                    net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(plek);
                    BaasKist.plaats(sl, p, bp, r);
                    kistGeplaatst(p, bp);
                }
            }
        }
        for (java.util.UUID id : losseKijkers) {
            if (sl.getEntity(id) instanceof ServerPlayer p) {
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, net.minecraft.network.chat.Component.empty(), 0, 0, 0));
            }
        }
        losseKijkers.clear();
        if (arena != null) {
            Arenas.verslagen(arena);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("vr_arena", arenaId);
        out.putInt("vr_fase", fase);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        arenaId = in.getStringOr("vr_arena", "");
        fase = in.getIntOr("vr_fase", 1);
    }
}
