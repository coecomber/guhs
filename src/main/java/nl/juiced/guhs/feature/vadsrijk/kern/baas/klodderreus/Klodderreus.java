package nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;

/**
 * De Grauwe Klodderreus (ronde 7), kern's reference boss:
 * <ul>
 *   <li>Fase 1: he jumps at you; his shadow (a red circle) is where he lands: get out.</li>
 *   <li>Fase 2 (50 %): he spits 4 grey kloddertjes (Kussenklap on the group!), and keeps jumping (2 more every 12 s, max 6).</li>
 *   <li>Fase 3 (20 %): he rolls in a circle through the arena (a red strip shows where he goes), between the rolls a jump.</li>
 *   <li>End: a big pink plof; personal chests for everyone above 5 %.</li>
 * </ul>
 * All attacks are a small state machine in {@link #tick} (no vanilla goals except targeting).
 */
public class Klodderreus extends VadsrijkBaas {
    private enum Doet {
        RUST, MIKT, SPRINGT, ROLT
    }

    private Doet doet = Doet.RUST;
    private int timer = 40, rolHoek;
    private Vec3 landing = Vec3.ZERO, sprongVan = Vec3.ZERO;
    private int spuugTimer;

    public Klodderreus(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public int grootte() {
        return 6;
    }

    @Override
    protected double basisVads(int level) {
        return 700 + 120 * (level - 1);
    }

    @Override
    protected double basisSchade(int level) {
        return 14 + 3 * (level - 1);
    }

    @Override
    public int xp() {
        return 180 + 20 * mobLevel();
    }

    @Nullable
    @Override
    public String lootTabel() {
        return "kern:klodderreus";
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.5, 0.2};
    }

    @Override
    public String titelKey() {
        return "vadsrijk.kern.baas.klodderreus";
    }

    @Override
    protected void signatuur() {
        setDeltaMovement(0, 1.1, 0);
        doet = Doet.RUST;
        timer = 60;
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.4f);
        }
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1.4f);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 40, 1.5, 1.5, 1.5, 0.05);
        if (fase == 2) {
            spuug(4);
            spuugTimer = 240;
        }
        if (fase == 3) {
            doet = Doet.RUST;
            timer = 20;
        }
    }

    private void spuug(int n) {
        ServerLevel sl = (ServerLevel) level();
        int levend = sl.getEntitiesOfClass(ProefKlodder.class, getBoundingBox().inflate(30), m -> m.klein() && !m.geplofd()).size();
        for (int i = 0; i < n && levend + i < 6; i++) {
            ProefKlodder k = KernRegistry.PROEFKLODDERTJE.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (k == null) {
                return;
            }
            double a = Math.PI * 2 * i / n + getRandom().nextDouble();
            k.snapTo(getX(), getY() + 2, getZ(), (float) Math.toDegrees(a), 0);
            k.stelIn(mobLevel(), Element.WATER, false, Set.of());
            k.setDeltaMovement(Math.cos(a) * 0.6, 0.6, Math.sin(a) * 0.6);
            sl.addFreshEntity(k);
        }
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.5f);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 2.5, getZ(), 40, 1, 0.5, 1, 0.2);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || geplofd() || stil() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity t = getTarget();
        if (fase() >= 2 && --spuugTimer <= 0) {
            spuug(2);
            spuugTimer = 240;
        }
        timer--;
        switch (doet) {
            case RUST -> {
                if (onGround()) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                }
                if (timer <= 0 && t != null) {
                    if (fase() >= 3 && getRandom().nextInt(2) == 0) {
                        doet = Doet.ROLT;
                        timer = 160;
                        rolHoek = (int) Math.toDegrees(Math.atan2(getZ() - midden().z, getX() - midden().x));
                    } else {
                        doet = Doet.MIKT;
                        timer = fase() >= 2 ? 22 : 30;
                        landing = new Vec3(t.getX(), Math.floor(t.getY()), t.getZ());
                        Telegraaf.cirkel(sl, landing, 3.5, timer + 16, Telegraaf.ROOD);
                        sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.6f);
                    }
                }
            }
            case MIKT -> {
                if (timer <= 0) {
                    doet = Doet.SPRINGT;
                    timer = 16;
                    sprongVan = position();
                    Vec3 d = landing.subtract(position());
                    // land at "landing" after ~16 ticks (gravity 0.08/t, drag ignored): vy = 0.08*16/2 + dy/16
                    setDeltaMovement(d.x / 16.0, 0.64 + d.y / 16.0, d.z / 16.0);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.5f);
                }
            }
            case SPRINGT -> {
                if (timer < 14) {   // steer towards the shadow so drag and blocks don't make it miss
                    Vec3 d = landing.subtract(position()).multiply(1, 0, 1);
                    if (d.length() > 0.5) {
                        Vec3 v = getDeltaMovement();
                        setDeltaMovement(d.x / Math.max(1, timer), v.y, d.z / Math.max(1, timer));
                    }
                }
                if ((timer <= 0 && onGround()) || timer < -30) {
                    neerkomen(sl);
                    doet = Doet.RUST;
                    timer = fase() >= 2 ? 40 : 55;
                }
            }
            case ROLT -> {
                Vec3 m = midden();
                rolHoek += 6;
                double r = arena != null ? arena.straal() * 0.6 : 10;
                double a = Math.toRadians(rolHoek);
                Vec3 doel = new Vec3(m.x + Math.cos(a) * r, getY(), m.z + Math.sin(a) * r);
                Vec3 v = doel.subtract(position()).multiply(1, 0, 1);
                setDeltaMovement(v.x * 0.5, getDeltaMovement().y, v.z * 0.5);
                if (timer % 10 == 0) {
                    double b = Math.toRadians(rolHoek + 35);
                    Telegraaf.lijn(sl, position(), new Vec3(m.x + Math.cos(b) * r, getY(), m.z + Math.sin(b) * r), 3, 12, Telegraaf.ROOD);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.4f);
                }
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(0.6))) {
                    if (p.invulnerableTime <= 0) {
                        Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * 1.2, element(), Schade.Soort.MELEE, false, "rollen"));
                        Vec3 weg = p.position().subtract(position()).multiply(1, 0, 1).normalize();
                        p.setDeltaMovement(weg.x * 1.4, 0.6, weg.z * 1.4);
                        p.hurtMarked = true;
                        p.invulnerableTime = 15;
                    }
                }
                if (timer <= 0) {
                    doet = Doet.RUST;
                    timer = 50;
                }
            }
        }
    }

    private Vec3 midden() {
        return arena != null ? Vec3.atBottomCenterOf(arena.centrum()) : position();
    }

    private void neerkomen(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 3f, 0.4f);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.7f);
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2 * i / 32;
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX() + Math.cos(a) * 3.5, getY() + 0.3, getZ() + Math.sin(a) * 3.5, 2, 0.1, 0.1, 0.1, 0.05);
            sl.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * 3, getY() + 0.2, getZ() + Math.sin(a) * 3, 1, 0, 0, 0, 0.03);
        }
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(BlockPos.containing(position())).inflate(3.6, 2.5, 3.6))) {
            Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * 2, element(), Schade.Soort.MELEE, false, "plet"));
            Vec3 weg = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            p.setDeltaMovement(weg.x * 1.0, 0.5, weg.z * 1.0);
            p.hurtMarked = true;
        }
    }

    @Override
    protected void opPlof() {
        super.opPlof();
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 200, 3, 2, 3, 0.1);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 4, getZ(), 20, 2, 1, 2, 0);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 2, getZ(), 80, 2, 2, 2, 0.4);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 2f, 0.6f);
            for (ProefKlodder k : sl.getEntitiesOfClass(ProefKlodder.class, getBoundingBox().inflate(32), m -> m.klein() && !m.geplofd())) {
                k.verslagen(null);   // the little ones cheer up too
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
