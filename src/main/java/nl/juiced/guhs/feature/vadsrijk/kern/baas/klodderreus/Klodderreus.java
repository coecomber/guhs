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
        RUST, MIKT, SPRINGT, ROLT, CHARGE
    }

    private Doet doet = Doet.RUST;
    private int timer = 40, rolHoek, zet;
    private Vec3 landing = Vec3.ZERO;
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
        return 1000 + 200 * (level - 1);   // fix round 1: ~2-3 min for a level-3 player (was 700 + 120/lv)
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
        nl.juiced.guhs.feature.vadsrijk.tmodels.KlodderGeo.speel(this, "brul");   // T-MODELS
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
        nl.juiced.guhs.feature.vadsrijk.tmodels.KlodderGeo.speel(this, "spuug");   // T-MODELS
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 2.5, getZ(), 40, 1, 0.5, 1, 0.2);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || geplofd() || stil() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase() >= 2 && --spuugTimer <= 0) {
            spuug(2);
            spuugTimer = 240;
        }
        if (inSprong() || nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.loopt(this)) {   // (T-VFX: a bouwdoos attack runs: wait)
            return;
        }
        timer--;
        switch (doet) {
            case RUST -> {
                if (onGround()) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                }
                if (timer > 0) {
                    break;
                }
                ServerPlayer t = volgendDoel();
                if (t == null) {
                    timer = 10;
                    break;
                }
                int vfx = nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.probeer(this, sl);   // T-VFX: sometimes a bouwdoos attack (slime rain, the wave)
                if (vfx > 0) {
                    timer = vfx;
                    break;
                }
                zet++;
                ServerPlayer ver = afstandsSpeler();
                if (fase() >= 3 && zet % 3 == 0) {
                    doet = Doet.ROLT;   // the circle through the camp
                    timer = 120;
                    rolHoek = (int) Math.toDegrees(Math.atan2(getZ() - midden().z, getX() - midden().x));
                } else if (fase() >= 3 && zet % 3 == 1) {
                    doet = Doet.CHARGE;   // a roll straight at someone: the red strip shows the lane
                    timer = 26;
                    Vec3 d = voetenVan(t).subtract(position()).multiply(1, 0, 1);
                    d = d.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : d.normalize();
                    landing = binnenArena(position().add(d.scale(position().distanceTo(voetenVan(t)) + 6)));
                    Telegraaf.lijn(sl, position(), landing, 3.4, timer, Telegraaf.ROOD);
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, timer);   // T-VFX: crouch + cast bar
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "rolaanval", timer);
                    sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2f, 1.8f);
                } else if (fase() >= 2 && ver != null && zet % 2 == 0) {
                    spuugBal(ver);   // spit at whoever keeps their distance
                    timer = 30;
                } else if (fase() >= 2 && zet % 4 == 3) {
                    spuugBal(t);
                    timer = 30;
                } else {
                    doet = Doet.MIKT;   // the jump: his shadow (red circle) is where he lands
                    timer = fase() >= 3 ? 20 : fase() >= 2 ? 24 : 30;
                    landing = voetenVan(t);
                    Telegraaf.cirkel(sl, landing, 3.5, timer + 14, Telegraaf.ROOD);
                    nl.juiced.guhs.feature.vadsrijk.tmodels.KlodderGeo.speel(this, "aanloop");   // T-MODELS
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, timer);   // T-VFX: he crouches for the jump (held), the cast bar names it
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "plettersprong", timer + 14);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.6f);
                }
            }
            case MIKT -> {
                if (timer <= 0) {   // (he lands where the circle was shown: dodge it)
                    springNaar(landing, 14, 5);
                    nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.spoor(sl, this, 0xB8C0B0, 14, 1.6);   // T-VFX: a trail through the air
                    nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.pose(sl, this, "treffer", 10);
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasAnims.speel(this, "vfx_treffer");   // T-INT: end the held GeckoLib wind-up (KlodderGeo)
                    doet = Doet.SPRINGT;
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.5f);
                }
            }
            case SPRINGT -> {
                doet = Doet.RUST;   // (geland() did the landing; only reached if a jump got cut short)
                timer = 20;
            }
            case ROLT -> {
                Vec3 m = midden();
                rolHoek += 5;
                double r = gevechtsStraal() * 0.6;
                double a = Math.toRadians(rolHoek);
                Vec3 doel = new Vec3(m.x + Math.cos(a) * r, getY(), m.z + Math.sin(a) * r);
                Vec3 v = doel.subtract(position()).multiply(1, 0, 1);
                if (v.length() > 0.9) {
                    v = v.normalize().scale(0.9);
                }
                move(net.minecraft.world.entity.MoverType.SELF, v);
                if (timer % 10 == 0) {
                    double b = Math.toRadians(rolHoek + 35);
                    Telegraaf.lijn(sl, position(), new Vec3(m.x + Math.cos(b) * r, getY(), m.z + Math.sin(b) * r), 3, 12, Telegraaf.ROOD);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.4f);
                }
                rolRaak(sl);
                if (timer <= 0) {
                    doet = Doet.RUST;
                    timer = 40;
                }
            }
            case CHARGE -> {
                if (timer > 0) {
                    break;
                }
                Vec3 v = landing.subtract(position()).multiply(1, 0, 1);
                if (v.length() < 1.2 || timer < -40) {
                    doet = Doet.RUST;
                    timer = 35;
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 2f, 0.5f);
                    break;
                }
                move(net.minecraft.world.entity.MoverType.SELF, v.normalize());
                sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.3, getZ(), 6, 1, 0.2, 1, 0.05);
                if (timer == -1) {
                    nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.spoor(sl, this, 0x9AD86A, 30, 2.2);   // T-VFX: the rolling trail
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasAnims.speel(this, "vfx_treffer");   // T-INT: end the held GeckoLib wind-up (KlodderGeo)
                }
                rolRaak(sl);
            }
        }
    }

    /** The spit: a grey blob lobbed at a player (red circle where it lands). */
    private void spuugBal(ServerPlayer p) {
        nl.juiced.guhs.feature.vadsrijk.tmodels.KlodderGeo.speel(this, "spuug");   // T-MODELS
        lob(p, 2.8, 26, 1.1, "spuug", null, 0, new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM,
                net.minecraft.world.item.Items.SLIME_BALL), Telegraaf.ROOD);
    }

    /** A point kept inside the arena (2 from the edge). */
    private Vec3 binnenArena(Vec3 v) {
        if (arena == null) {
            return v;
        }
        Vec3 m = midden();
        Vec3 d = v.subtract(m).multiply(1, 0, 1);
        double max = Math.max(3, arena.straal() - 2);
        return d.length() > max ? new Vec3(m.x + d.normalize().x * max, v.y, m.z + d.normalize().z * max) : v;
    }

    private void rolRaak(ServerLevel sl) {
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(0.8))) {
            if (p.invulnerableTime <= 0 && vechters().contains(p)) {
                Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * 1.2, element(), Schade.Soort.MELEE, false, "rollen"));
                Vec3 weg = p.position().subtract(position()).multiply(1, 0, 1).normalize();
                p.setDeltaMovement(weg.x * 1.4, 0.6, weg.z * 1.4);
                p.hurtMarked = true;
                p.invulnerableTime = 15;
            }
        }
    }

    @Override
    protected void geland() {
        if (level() instanceof ServerLevel sl) {
            neerkomen(sl);
        }
        doet = Doet.RUST;
        timer = fase() >= 3 ? 30 : fase() >= 2 ? 38 : 45;
    }

    private Vec3 midden() {
        return gevechtsMidden();
    }

    private void neerkomen(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 3f, 0.4f);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.7f);
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2 * i / 32;
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX() + Math.cos(a) * 3.5, getY() + 0.3, getZ() + Math.sin(a) * 3.5, 2, 0.1, 0.1, 0.1, 0.05);
            sl.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * 3, getY() + 0.2, getZ() + Math.sin(a) * 3, 1, 0, 0, 0, 0.03);
        }
        raakRond(position(), 3.6, 1.7, "plet", null, 0, 1.0);   // (fix round 1: 34 raw at lv 3, ~1/4 of a lv-3 player)
        nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.treffer(this, position(), 4.2, 0x9AD86A);   // T-VFX: the pop, a slime shockwave, slime splats
        nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.deeltjes(sl, nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje.SLIJM, position().add(0, 0.4, 0), 30, 0x9AD86A, 2.5, 0.3);
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
