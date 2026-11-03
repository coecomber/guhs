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
        if (bron instanceof ServerPlayer p) {
            bijdragen.schade(p.getUUID(), Math.min(n, vads()));
        }
        super.krijgSchade(n, bron);
        double[] f = fases();
        double frac = vads() / Math.max(1, maxVads());
        while (!geplofd() && fase - 1 < f.length && frac <= f[fase - 1]) {
            fase++;
            opFase(fase);
        }
    }

    @Override
    public boolean stil() {
        return super.stil() || inIntro();
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

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
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
