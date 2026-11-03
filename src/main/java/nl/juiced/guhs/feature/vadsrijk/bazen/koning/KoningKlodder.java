package nl.juiced.guhs.feature.vadsrijk.bazen.koning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenBaas;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;

/**
 * Koning Klodder, world boss of the Klodderweide (ronde 7), lv ~8, shared, HP scales with the players in the arena.
 * <ul>
 *   <li>Fase 1: jumps at you (red shadow = landing spot) and a shock wave (red circle around him).</li>
 *   <li>Fase 2 (60 %): he splits into 3 Kroonklodders (roze/blauw/geel). Beat all three within {@link SplitRegel#VENSTER} of the
 *       first one: he re-forms at 35 % (= fase 3). Too slow: the survivors melt back, he heals 8 % per survivor and splits
 *       again 20 s later.</li>
 *   <li>Fase 3 (35 %): jumps + shock waves, and every ~22 s the Hemelsprong: he leaps sky-high, the whole meadow becomes his
 *       shadow except 3 pink safe spots; whoever stands in the shadow when he lands gets flattened (55 % of their Vads).</li>
 * </ul>
 */
public class KoningKlodder extends BazenBaas {
    private static final EntityDataAccessor<Boolean> GESPLITST = SynchedEntityData.defineId(KoningKlodder.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions SCHADUW = new DustParticleOptions(0x2A1A30, 2.2f);

    private enum Doet {
        RUST, MIKT, SPRINGT, SCHOK, HEMEL_OP, HEMEL_NEER
    }

    private Doet doet = Doet.RUST;
    private int timer = 40;
    private Vec3 landing = Vec3.ZERO;
    @Nullable
    private Vec3 spawnPlek;
    @Nullable
    private SplitRegel regel;
    private final UUID[] klodders = new UUID[3];
    private long splitOpnieuw = -1;
    private int hemelOver = -1;
    private final List<Vec3> veilig = new ArrayList<>();

    public KoningKlodder(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GESPLITST, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public String baasId() {
        return "koning_klodder";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"spring", "land", "brul"};
    }

    @Override
    protected double basisVads(int level) {
        return 2000 + 200 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 10 + 2.5 * level;
    }

    @Override
    public int xp() {
        return 600 + 60 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.35};
    }

    public boolean gesplitst() {
        return entityData.get(GESPLITST);
    }

    @Nullable
    public SplitRegel regel() {
        return regel;
    }

    @Override
    protected void signatuur() {
        speel("brul");
        setDeltaMovement(0, 1.0, 0);
        doet = Doet.RUST;
        timer = 60;
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.3f);
        }
    }

    Vec3 midden() {
        if (arena != null) {
            return Vec3.atBottomCenterOf(arena.centrum());
        }
        return spawnPlek != null ? spawnPlek : position();
    }

    double straal() {
        return arena != null ? arena.straal() : 20;
    }

    List<ServerPlayer> spelers(ServerLevel sl) {
        Vec3 m = midden();
        double r = straal() + 4;
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(m.x, p.getY(), m.z) <= r * r && Math.abs(p.getY() - m.y) < 30) {
                l.add(p);
            }
        }
        return l;
    }

    private void zeg(ServerLevel sl, Component c) {
        for (ServerPlayer p : spelers(sl)) {
            p.sendOverlayMessage(c);
        }
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1.6f);
        speel("brul");
        if (fase == 2) {
            splits(sl);
        } else if (fase == 3) {
            hemelOver = 60;
        }
    }

    // ---- the split --------------------------------------------------------------------------------------------------------

    /** Splits into the three Kroonklodders now (public for the dev command and the gametest). */
    public void splits(ServerLevel sl) {
        if (gesplitst() || geplofd()) {
            return;
        }
        entityData.set(GESPLITST, true);
        setInvisible(true);
        doet = Doet.RUST;
        regel = new SplitRegel(sl.getGameTime());
        Element[] el = {Element.ROZE, Element.WATER, Element.VUUR};
        double elk = maxVads() * 0.25 / 3;
        for (int i = 0; i < 3; i++) {
            ElementKlodder k = BazenRegistry.KK_KLODDER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (k == null) {
                continue;
            }
            double a = Math.PI * 2 * i / 3;
            k.snapTo(getX(), getY() + 1.5, getZ(), (float) Math.toDegrees(a), 0);
            k.stelIn(mobLevel(), el[i], false, Set.of());
            k.koppel(this, i, elk);
            k.setDeltaMovement(Math.cos(a) * 0.8, 0.7, Math.sin(a) * 0.8);
            sl.addFreshEntity(k);
            klodders[i] = k.getUUID();
        }
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1.5, getZ(), 120, 2, 1.5, 2, 0.3);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 3f, 0.4f);
        zeg(sl, Component.translatable("vadsrijk.bazen.kk.splitst").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    void klodderVerslagen(int i) {
        if (regel != null) {
            regel.verslagen(i, level().getGameTime());
        }
    }

    /** Checks the split rule at this game time (the tick calls it with the real time; the gametest with a later one). */
    public void splitControle(long nu) {
        if (!gesplitst() || regel == null || !(level() instanceof ServerLevel sl)) {
            return;
        }
        SplitRegel.Uitslag u = regel.uitslag(nu);
        if (u == SplitRegel.Uitslag.BEZIG) {
            int r = regel.resterend(nu);
            if (r >= 0 && r % 20 == 0) {
                zeg(sl, Component.translatable("vadsrijk.bazen.kk.venster", r / 20).withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        herenig(sl, u == SplitRegel.Uitslag.GELUKT, nu);
    }

    private void herenig(ServerLevel sl, boolean gelukt, long nu) {
        int over = regel == null ? 3 : regel.overlevers();
        for (UUID id : klodders) {
            if (id != null && sl.getEntity(id) instanceof ElementKlodder k && !k.geplofd()) {
                k.smelt(this);
            }
        }
        java.util.Arrays.fill(klodders, null);
        regel = null;
        entityData.set(GESPLITST, false);
        setInvisible(false);
        speel("land");
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 3f, 0.5f);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1.5, getZ(), 80, 2, 1.5, 2, 0.2);
        if (gelukt) {
            zeg(sl, Component.translatable("vadsrijk.bazen.kk.gelukt").withStyle(ChatFormatting.GREEN));
            double doel = maxVads() * 0.35;
            if (vads() > doel) {
                super.krijgSchade(vads() - doel + 0.01, null);
            }
            Statussen.geef(this, Status.VERDOOFD, 60, 1, null);
            splitOpnieuw = -1;
        } else {
            zeg(sl, Component.translatable("vadsrijk.bazen.kk.mislukt", over).withStyle(ChatFormatting.RED));
            heal(maxVads() * 0.08 * over);
            splitOpnieuw = nu + 400;
        }
        timer = 50;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (gesplitst() || doet == Doet.HEMEL_OP || doet == Doet.HEMEL_NEER) {
            return;
        }
        super.krijgSchade(n, bron);
    }

    @Override
    public boolean isPickable() {
        return !gesplitst() && super.isPickable();
    }

    @Override
    public void naamplaatje() {
        super.naamplaatje();
        if (!level().isClientSide() && gesplitst()) {
            setCustomNameVisible(false);
        }
    }

    // ---- the fight ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (spawnPlek == null) {
            spawnPlek = position();
        }
        if (geplofd()) {
            return;
        }
        long nu = sl.getGameTime();
        if (gesplitst()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            splitControle(nu);
            return;
        }
        if (stil() && doet != Doet.HEMEL_OP && doet != Doet.HEMEL_NEER) {
            return;
        }
        if (fase() == 2 && splitOpnieuw > 0 && nu >= splitOpnieuw && doet == Doet.RUST) {
            splitOpnieuw = -1;
            splits(sl);
            return;
        }
        LivingEntity t = getTarget();
        timer--;
        if (hemelOver > 0) {
            hemelOver--;
        }
        switch (doet) {
            case RUST -> {
                if (onGround()) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                }
                if (timer > 0) {
                    break;
                }
                if (fase() >= 3 && hemelOver == 0) {
                    hemelOp(sl);
                } else if (t != null && getRandom().nextInt(3) > 0) {
                    doet = Doet.MIKT;
                    timer = fase() >= 2 ? 22 : 30;
                    landing = new Vec3(t.getX(), Math.floor(t.getY()), t.getZ());
                    Telegraaf.cirkel(sl, landing, 4.5, timer + 16, Telegraaf.ROOD);
                } else if (t != null) {
                    doet = Doet.SCHOK;
                    timer = 32;
                    speel("brul");
                    Telegraaf.cirkel(sl, position(), 8, timer, Telegraaf.ROOD);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2.5f, 0.3f);
                }
            }
            case MIKT -> {
                if (timer <= 0) {
                    doet = Doet.SPRINGT;
                    timer = 16;
                    speel("spring");
                    Vec3 d = landing.subtract(position());
                    setDeltaMovement(d.x / 16.0, 0.64 + d.y / 16.0, d.z / 16.0);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.4f);
                }
            }
            case SPRINGT -> {
                if (timer < 14) {
                    Vec3 d = landing.subtract(position()).multiply(1, 0, 1);
                    if (d.length() > 0.5) {
                        setDeltaMovement(d.x / Math.max(1, timer), getDeltaMovement().y, d.z / Math.max(1, timer));
                    }
                }
                if ((timer <= 0 && onGround()) || timer < -30) {
                    plet(sl, 4.6, 2.0);
                    doet = Doet.RUST;
                    timer = fase() >= 2 ? 40 : 55;
                }
            }
            case SCHOK -> {
                if (timer <= 0) {
                    plet(sl, 8, 1.5);
                    doet = Doet.RUST;
                    timer = 45;
                }
            }
            case HEMEL_OP -> {
                if (timer > 64) {
                    setDeltaMovement(0, 1.4, 0);
                } else {
                    setDeltaMovement(0, 0, 0);
                }
                schaduw(sl);
                if (timer <= 0) {
                    doet = Doet.HEMEL_NEER;
                    Vec3 m = midden();
                    snapTo(m.x, getY(), m.z, getYRot(), 0);
                    setNoGravity(false);
                    setDeltaMovement(0, -3, 0);
                    timer = 60;
                }
            }
            case HEMEL_NEER -> {
                setDeltaMovement(0, Math.min(getDeltaMovement().y, -2.5), 0);
                if (onGround() || timer <= 0) {
                    hemelLand(sl);
                }
            }
        }
    }

    private void plet(ServerLevel sl, double r, double factor) {
        speel("land");
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 3f, 0.4f);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.7f);
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40;
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX() + Math.cos(a) * r, getY() + 0.3, getZ() + Math.sin(a) * r, 2, 0.1, 0.1, 0.1, 0.05);
        }
        for (ServerPlayer p : spelers(sl)) {
            double dx = p.getX() - getX(), dz = p.getZ() - getZ();
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - getY()) < 3) {
                Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * factor, element(), Schade.Soort.MELEE, false, "bazen_plet"));
                Vec3 weg = new Vec3(dx, 0, dz).normalize();
                p.setDeltaMovement(weg.x, 0.6, weg.z);
                p.hurtMarked = true;
            }
        }
    }

    private void hemelOp(ServerLevel sl) {
        doet = Doet.HEMEL_OP;
        timer = 80;
        speel("spring");
        setNoGravity(true);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 4f, 0.25f);
        veilig.clear();
        Vec3 m = midden();
        double r = straal() * 0.65;
        for (int tries = 0; veilig.size() < 3 && tries < 60; tries++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = r * (0.3 + 0.7 * getRandom().nextDouble());
            Vec3 v = new Vec3(m.x + Math.cos(a) * d, m.y, m.z + Math.sin(a) * d);
            if (veilig.stream().allMatch(o -> o.distanceTo(v) > 8)) {
                veilig.add(v);
            }
        }
        for (Vec3 v : veilig) {
            Telegraaf.cirkel(sl, v, 3, 82, Telegraaf.ROZE);
            sl.sendParticles(ParticleTypes.END_ROD, v.x, v.y + 0.5, v.z, 20, 1.2, 0.8, 1.2, 0.02);
        }
        Telegraaf.cirkel(sl, m, straal(), 82, Telegraaf.ROOD);
        zeg(sl, Component.translatable("vadsrijk.bazen.kk.hemel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    private boolean veiligOp(Vec3 p) {
        for (Vec3 v : veilig) {
            double dx = p.x - v.x, dz = p.z - v.z;
            if (dx * dx + dz * dz <= 3.3 * 3.3) {
                return true;
            }
        }
        return false;
    }

    private void schaduw(ServerLevel sl) {
        if (timer % 4 != 0) {
            return;
        }
        Vec3 m = midden();
        double r = straal();
        for (int i = 0; i < 140; i++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(getRandom().nextDouble()) * r;
            Vec3 v = new Vec3(m.x + Math.cos(a) * d, m.y + 0.15, m.z + Math.sin(a) * d);
            if (!veiligOp(v)) {
                sl.sendParticles(SCHADUW, v.x, v.y, v.z, 1, 0.1, 0, 0.1, 0);
            }
        }
    }

    private void hemelLand(ServerLevel sl) {
        doet = Doet.RUST;
        timer = 60;
        hemelOver = 440;
        speel("land");
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, 0.5f);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 4f, 0.3f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 2, 1, 0.5, 1, 0);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 200, straal() / 3, 0.5, straal() / 3, 0.3);
        for (ServerPlayer p : spelers(sl)) {
            if (!veiligOp(p.position())) {
                Schade.doe(new Schade.Klap(this, p, Spelers.maxVads(p) * 0.55, element(), Schade.Soort.WAAR, false, "bazen_hemelsprong"));
                p.setDeltaMovement(0, 0.9, 0);
                p.hurtMarked = true;
                p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.kk.hemel_raak").withStyle(ChatFormatting.RED));
            }
        }
        veilig.clear();
    }

    @Override
    protected void opPlof() {
        super.opPlof();
        setNoGravity(false);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 250, 3, 2, 3, 0.1);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 4, getZ(), 30, 2, 1, 2, 0);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 2, getZ(), 120, 2, 2, 2, 0.4);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 2f, 0.6f);
            for (UUID id : klodders) {
                if (id != null && sl.getEntity(id) instanceof ElementKlodder k) {
                    k.discard();
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && level() instanceof ServerLevel sl) {
            for (UUID id : klodders) {
                if (id != null && sl.getEntity(id) instanceof ElementKlodder k) {
                    k.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        setInvisible(false);   // a reload mid-split: he simply stands whole again
        setNoGravity(false);
    }
}
