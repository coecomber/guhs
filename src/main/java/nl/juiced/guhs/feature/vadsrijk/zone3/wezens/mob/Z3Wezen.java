package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Lantaarnlicht;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3Contracten;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3Verslagen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensPlekken;

/**
 * The base of every zone-3 creature ("De Schemerwortel", lv 30-50). Built on the zone-2 {@link Z2Wezen} (no pink plof, hidden =
 * not hittable, helpers), plus:
 * <ul>
 *   <li>{@link #wezenAard()}: ROT falls apart into grey fluff and dead leaves that blow away, MIKA flees in a smoke puff to its
 *       zeppelin ("Dit komt in mijn rapport!"), BEEST sinks into dust and leaves, GEEST fades into dark wisps, HARS breaks into
 *       amber shards ({@link Z3Verslagen}); nothing gory;</li>
 *   <li>the soft lantern rule ({@link Lantaarnlicht}): in the Wortelduister a creature outside every light is {@link #donker()}:
 *       a silhouette on the client and -50 % damage taken;</li>
 *   <li>named area leaders (spawner id {@code z3wezens:leider_<naam>}: always elite, a name of its own);</li>
 *   <li>contract elites ({@link Z3Contracten}): {@link #contract()} != "" = a named hunting target that exists only for the
 *       players of the contract (they alone see it, hit it and are attacked by it); it leaves when they are gone;</li>
 *   <li>layer-aware safe spots ({@link Z3WezensPlekken#VEILIG}): a creature that wanders into one forgets its fight and goes home.</li>
 * </ul>
 */
public abstract class Z3Wezen extends Z2Wezen {
    public enum WezenAard {
        ROT, MIKA, BEEST, GEEST, HARS
    }

    private static final EntityDataAccessor<Boolean> DONKER = SynchedEntityData.defineId(Z3Wezen.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> CONTRACT = SynchedEntityData.defineId(Z3Wezen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> EIGENAARS = SynchedEntityData.defineId(Z3Wezen.class, EntityDataSerializers.STRING);

    /** The client's own player (set by the client init; null on a dedicated server). */
    public static Supplier<UUID> LOKALE_SPELER = () -> null;

    /** Ticks a contract elite stays without its players nearby, and its life time. */
    public static final int CONTRACT_ZONDER = 600, CONTRACT_LEVEN = 20 * 60 * 12;

    private boolean benoemd;
    private int zonderEigenaar;

    protected Z3Wezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public abstract WezenAard wezenAard();

    @Override
    public Aard aard() {
        return wezenAard() == WezenAard.MIKA ? Aard.MIKA : Aard.BEEST;
    }

    // ---- numbers: the same curve as zones 1-2 (HP base x (1 + 0.3/level), damage x (1 + 0.22/level)) -----------------------

    protected static double vads3(double basis, int level) {
        return basis * (1 + 0.3 * (level - 1));
    }

    protected static double schade3(double basis, int level) {
        return basis * (1 + 0.22 * (level - 1));
    }

    @Nullable
    @Override
    public String lootTabel() {
        return contract().isEmpty() ? "z3wezens:" + naam() : "z3wezens:contract_" + contract();
    }

    /** No zone-1 hint key: zone 3 shows its own ({@code vadsrijk.z3wezens.tip.<naam>}). */
    @Override
    protected String tip() {
        return null;
    }

    // ---- synced flags ----------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DONKER, false);
        b.define(CONTRACT, "");
        b.define(EIGENAARS, "");
    }

    /** Outside every light in the Wortelduister: silhouette + -50 % damage taken. */
    public boolean donker() {
        return entityData.get(DONKER);
    }

    public void zetDonker(boolean d) {
        entityData.set(DONKER, d);
    }

    public String contract() {
        return entityData.get(CONTRACT);
    }

    /** Makes this a contract elite for these players (call after {@link #stelIn}). */
    public void maakContract(String id, List<UUID> eigenaars) {
        entityData.set(CONTRACT, id);
        StringBuilder s = new StringBuilder();
        for (UUID u : eigenaars) {
            if (s.length() > 0) {
                s.append(',');
            }
            s.append(u);
        }
        entityData.set(EIGENAARS, s.toString());
        setCustomName(Component.translatable("vadsrijk.z3wezens.contract." + id));
        setPersistenceRequired();
    }

    /** A contract elite's extra health (on top of the elite x2.5), full. */
    public void zetMaxVadsContract(double factor) {
        zetMaxVads(maxVads() * factor, false);
        heal(maxVads());
    }

    public List<UUID> eigenaars() {
        List<UUID> l = new ArrayList<>();
        String s = entityData.get(EIGENAARS);
        if (!s.isEmpty()) {
            for (String d : s.split(",")) {
                try {
                    l.add(UUID.fromString(d));
                } catch (IllegalArgumentException e) {
                    // skip
                }
            }
        }
        return l;
    }

    /** May this player see / hit / be hit by this creature? (Everyone, unless it is a contract elite.) */
    public boolean voor(@Nullable Player p) {
        if (contract().isEmpty() || p == null) {
            return true;
        }
        return eigenaars().contains(p.getUUID());
    }

    /** No nameplate now (hidden, disguised, defeated, invisible, a shadow outside the light). */
    public boolean zonderNaam() {
        return verborgen() || geplofd() || isInvisible();
    }

    /** Client: does the local player see this one? */
    public boolean zichtbaarVoorMij() {
        if (contract().isEmpty()) {
            return true;
        }
        UUID ik = LOKALE_SPELER.get();
        return ik == null || eigenaars().contains(ik);
    }

    @Override
    public boolean isPickable() {
        if (level().isClientSide() && !zichtbaarVoorMij()) {
            return false;
        }
        return super.isPickable();
    }

    @Override
    public void setTarget(@Nullable LivingEntity t) {
        if (t instanceof Player p && !voor(p)) {
            return;
        }
        super.setTarget(t);
    }

    // ---- tick ------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl) || geplofd()) {
            return;
        }
        if (!benoemd && !spawner.isEmpty() && spawner.startsWith("z3wezens:leider_")) {
            benoemd = true;
            setCustomName(Component.translatable("vadsrijk.z3wezens.leider." + spawner.substring("z3wezens:leider_".length())));
        }
        if ((tickCount + getId()) % 10 == 0) {
            zetDonker(Z3WezensPlekken.duister(getX(), getY(), getZ()) && !Lantaarnlicht.inLicht(sl, position().add(0, getBbHeight() * 0.5, 0)));
        }
        if (tickCount % 20 == 7 && !spawner.isEmpty() && contract().isEmpty() && Z3WezensPlekken.veilig(getX(), getY(), getZ(), 0)) {
            setTarget(null);
            Spawners.Spawner s = Spawners.van(spawner);
            if (s != null) {
                getNavigation().moveTo(s.centrum().getX() + 0.5, getY(), s.centrum().getZ() + 0.5, 1.2);
            } else if (tickCount > 40) {
                discard();
            }
        }
        if (!contract().isEmpty()) {
            contractTick(sl);
        }
        if (tickCount % 20 == 0 && getTarget() instanceof ServerPlayer p && distanceTo(p) < 14) {
            Tips.toon(p, "vadsrijk.z3wezens.tip." + naam());
            if (donker()) {
                Tips.toon(p, "vadsrijk.z3wezens.tip.donker");
            }
        }
    }

    /** A contract elite: leaves (a puff) when its players are gone too long or it lived its time. */
    private void contractTick(ServerLevel sl) {
        boolean iemand = false;
        for (UUID u : eigenaars()) {
            if (sl.getPlayerByUUID(u) instanceof ServerPlayer p && p.isAlive() && p.distanceTo(this) < 96) {
                iemand = true;
                break;
            }
        }
        zonderEigenaar = iemand ? 0 : zonderEigenaar + 1;
        if (getTarget() instanceof Player p && !voor(p)) {
            setTarget(null);
        }
        if (zonderEigenaar > CONTRACT_ZONDER || tickCount > CONTRACT_LEVEN) {
            Z3Verslagen.weg(sl, this);
            Z3Contracten.vertrokken(this);
            discard();
        }
    }

    // ---- defeat ----------------------------------------------------------------------------------------------------------

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        if (!contract().isEmpty()) {
            Set<ServerPlayer> eigen = new LinkedHashSet<>();
            for (ServerPlayer p : helpers) {
                if (voor(p)) {
                    eigen.add(p);
                }
            }
            helpers = eigen;
        }
        super.beloon(sl, helpers);
        if (!contract().isEmpty()) {
            for (ServerPlayer p : helpers) {
                ItemStack trofee = Z3Contracten.trofee(contract());
                if (!trofee.isEmpty()) {
                    LootBundel.laatVallen(p, position().add(0, 0.8, 0), new LootTabel.Resultaat(List.of(trofee), 0), 1);
                }
            }
            Z3Contracten.verslagen(this, helpers);
        }
    }

    /** What a Mika shouts when it flees (a key under vadsrijk.z3wezens.), or null. */
    @Nullable
    protected String vluchtRoep3() {
        return wezenAard() == WezenAard.MIKA ? "mika.weg" : null;
    }

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        float b = getBbWidth(), h = getBbHeight();
        Vec3 p = position();
        switch (wezenAard()) {
            case ROT -> Z3Verslagen.pluis(sl, p, b, h);
            case MIKA -> Z3Verslagen.rook(sl, p, h, vluchtRoep3() == null ? null
                    : Component.translatable("vadsrijk.z3wezens." + vluchtRoep3() + "." + getRandom().nextInt(4)));
            case BEEST -> Z3Verslagen.blaadjes(sl, p, b, h);
            case GEEST -> Z3Verslagen.wisp(sl, p, b, h);
            case HARS -> Z3Verslagen.hars(sl, p, b, h);
        }
        setInvisible(true);
        if (affixen.contains(Affix.SPLITSEND) && !klein() && contract().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                splits(sl, getType(), Math.max(1, mobLevel() - 1), true, i);
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------------

    /** Is this player behind me (a back attack / weak spot)? */
    protected boolean vanAchter(LivingEntity bron) {
        Vec3 d = bron.position().subtract(position()).multiply(1, 0, 1);
        return d.lengthSqr() > 0.01 && d.normalize().dot(voor()) < -0.35;
    }

    /** Makes a helper creature of a type next to this one (already angry at its target), counted by no spawner. */
    @Nullable
    protected Z3Wezen roep(ServerLevel sl, EntityType<?> type, int level, int i) {
        if (!(type.create(sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) instanceof Z3Wezen w)) {
            return null;
        }
        double a = getRandom().nextDouble() * Math.PI * 2 + i * 2.1;
        w.snapTo(getX() + Math.cos(a) * 1.6, getY() + 0.2, getZ() + Math.sin(a) * 1.6, getRandom().nextFloat() * 360, 0);
        w.stelIn(level, null, false, EnumSet.noneOf(Affix.class));
        if (!contract().isEmpty()) {
            w.maakContractHulp(eigenaars());
        }
        sl.addFreshEntity(w);
        if (getTarget() != null) {
            w.setTarget(getTarget());
        }
        return w;
    }

    /** A summoned helper of a contract elite: the same players only, no name, no trophy. */
    void maakContractHulp(List<UUID> eigenaars) {
        maakContract("hulp", eigenaars);
        setCustomName(null);
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    /** The usual ground goals, but with the zone-1 aggro range ({@link RijkWezen.Aggro}: the nearest player it can see within
     * {@link #aggroBereik}) instead of vanilla's follow range: MMO-dense areas must not pull from 30 blocks away. */
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

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("vr_contract", contract());
        out.putString("vr_eigenaars", entityData.get(EIGENAARS));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(CONTRACT, in.getStringOr("vr_contract", ""));
        entityData.set(EIGENAARS, in.getStringOr("vr_eigenaars", ""));
    }
}
