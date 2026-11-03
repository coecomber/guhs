package nl.juiced.guhs.feature.vadsrijk.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/** The mushroom folk of the Sporenwoud, the Druipgrotten (Glimzwam) and the Zwamtempel. Element Natuur (= Zwam). */
public final class Zwammen {
    private Zwammen() {
    }

    static double groei(double basis, int level, double perLevel) {
        return Klodders.groei(basis, level, perLevel);
    }

    /** Shared: walks (MeleeAttackGoal), squeaky mushroom sounds. */
    public abstract static class Zwamwezen extends RijkWezen {
        protected Zwamwezen(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public Element standaardElement() {
            return Element.NATUUR;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.MOOSHROOM_SHEAR;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        /** The direction it looks in (flat). */
        protected Vec3 voor() {
            double a = Math.toRadians(getYRot());
            return new Vec3(-Math.sin(a), 0, Math.cos(a));
        }
    }

    /** Hoedjesbol: crouches (the line on the ground shows where), then rams straight ahead; a wall makes it dizzy. */
    public static class Hoedjesbol extends Zwamwezen {
        public static final int LADEN = 1;
        private int ramPauze = 60, laden, rennen;
        private Vec3 richting = Vec3.ZERO;

        public Hoedjesbol(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(40, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(7, level, 0.22);
        }

        @Override
        public int xp() {
            return 16 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:hoedjesbol";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == LADEN ? "ram" : null;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || stil() || !(level() instanceof ServerLevel sl)) {
                if (stil() && houding() != 0 && !geplofd()) {
                    zetHouding(0);
                    rennen = laden = 0;
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (laden > 0) {
                getNavigation().stop();
                if (--laden == 0) {
                    rennen = 18;
                    playSound(SoundEvents.GOAT_LONG_JUMP, 1f, 1.4f);
                }
                return;
            }
            if (rennen > 0) {
                rennen--;
                setDeltaMovement(richting.x * 0.62, getDeltaMovement().y, richting.z * 0.62);
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 1, 0.1, 0, 0.1, 0);
                for (Player p : spelersIn(getBoundingBox().inflate(0.6))) {
                    klap(p, 1.8, Schade.Soort.MELEE, "ram");
                    p.setDeltaMovement(p.getDeltaMovement().add(richting.x * 0.9, 0.35, richting.z * 0.9));
                    p.hurtMarked = true;
                    rennen = 0;
                    sl.playSound(null, blockPosition(), SoundEvents.MOOSHROOM_CONVERT, SoundSource.HOSTILE, 1f, 1.3f);
                }
                if (rennen > 0 && horizontalCollision) {      // bonk! a wall: dizzy for a moment
                    rennen = 0;
                    Statussen.geef(this, Status.VERDOOFD, 50, 1, null);
                    sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 12, 0.3, 0.1, 0.3, 0.1);
                    sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1f, 1.4f);
                }
                if (rennen == 0) {
                    zetHouding(0);
                }
                return;
            }
            if (doel != null && onGround() && --ramPauze <= 0) {
                double d = distanceTo(doel);
                if (d > 3.5 && d < 12 && hasLineOfSight(doel)) {
                    ramPauze = 80 + getRandom().nextInt(30);
                    richting = doel.position().subtract(position()).multiply(1, 0, 1).normalize();
                    lookAt(doel, 90, 90);
                    laden = 16;
                    zetHouding(LADEN);
                    Telegraaf.lijn(sl, position(), position().add(richting.scale(11)), 1.4, 16, Telegraaf.ROOD);
                } else {
                    ramPauze = 20;
                }
            }
        }
    }

    /** Sporenpuffer: puffs a lingering spore cloud (Gesporen: a DoT and half healing) when you come close. */
    public static class Sporenpuffer extends Zwamwezen {
        private int pufPauze = 40;

        public Sporenpuffer(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(46, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(5, level, 0.22);
        }

        @Override
        public int xp() {
            return 18 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:sporenpuffer";
        }

        @Override
        protected String tip() {
            return "sporenpuffer";
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (doel != null && --pufPauze <= 0 && distanceTo(doel) < 5.5) {
                pufPauze = 100 + getRandom().nextInt(30);
                puf(sl);
            }
        }

        public void puf(ServerLevel sl) {
            speel("puf");
            sl.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.HOSTILE, 1.2f, 0.7f);
            double sterkte = Math.max(1, basisSchade(mobLevel()) * 0.3 * (elite() ? 1.4 : 1));
            Effecten.wolk(sl, position(), 3.2, 100, new DustParticleOptions(0x9AD86A, 2.2f),
                    p -> Statussen.geef(p, Status.GESPOREN, 60, sterkte, this));
        }
    }

    /**
     * Zwamwachter: a cap shield in front blocks 90 % (hook in WezensRegistry); hit it from the side/back, or right after its
     * slam (it is then "open" for 3 seconds).
     */
    public static class Zwamwachter extends Zwamwezen {
        public static final int BLOK = 1, KLAP = 2;
        private int klap = -1, open;

        public Zwamwachter(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(70, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(8, level, 0.22);
        }

        @Override
        public int xp() {
            return 26 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:zwamwachter";
        }

        @Override
        protected String tip() {
            return "zwamwachter";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == BLOK ? "blok" : h == KLAP ? "klap" : null;
        }

        /** Open = its shield is down (after the slam). */
        public boolean open() {
            return open > 0 || klap >= 0 || geplofd() || stil();
        }

        /** Does a hit from this attacker land on the shield? (in front, within 65 degrees) */
        public boolean blokt(@Nullable LivingEntity bron) {
            if (bron == null || open()) {
                return false;
            }
            Vec3 naar = bron.position().subtract(position()).multiply(1, 0, 1);
            if (naar.lengthSqr() < 1.0E-4) {
                return true;
            }
            double a = Math.toRadians(yBodyRot);
            Vec3 voor = new Vec3(-Math.sin(a), 0, Math.cos(a));
            return voor.dot(naar.normalize()) > Math.cos(Math.toRadians(65));
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (klap < 0 && aanvalPauze <= 0 && level() instanceof ServerLevel sl) {
                klap = 16;
                aanvalPauze = 50;
                zetHouding(KLAP);
                Telegraaf.cirkel(sl, position().add(voor().scale(1.8)), 2.2, 16, Telegraaf.ROOD);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (open > 0) {
                open--;
            }
            if (klap >= 0) {
                getNavigation().stop();
                if (klap-- == 0) {
                    Vec3 m = position().add(voor().scale(1.8));
                    for (Player p : spelersRond(m, 2.2)) {
                        klap(p, 1.7, Schade.Soort.MELEE, "klap");
                        Statussen.geef(p, Status.VERDOOFD, 15, 1, this);
                    }
                    sl.sendParticles(ParticleTypes.EXPLOSION, m.x, m.y + 0.2, m.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(new DustParticleOptions(0xB07850, 1.8f), m.x, m.y + 0.2, m.z, 20, 1.2, 0.2, 1.2, 0);
                    sl.playSound(null, blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.HOSTILE, 1f, 1.2f);
                    open = 60;
                    klap = -1;
                }
            }
            zetHouding(klap >= 0 ? KLAP : open > 0 || stil() ? 0 : BLOK);
        }

        public void blokEffect(ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1f, 0.9f);
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1, getZ(), 6, 0.4, 0.4, 0.4, 0.1);
        }
    }

    /** Glimzwam: avoids you and heals the creatures around it every few seconds. Cheer it up first! */
    public static class Glimzwam extends Zwamwezen {
        private int healPauze = 40;

        public Glimzwam(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(30, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(3, level, 0.22);
        }

        @Override
        public int xp() {
            return 22 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:glimzwam";
        }

        @Override
        protected String tip() {
            return "glimzwam";
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6f, 1.0, 1.25,
                    p -> !((Player) p).isCreative() && !p.isSpectator()));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (--healPauze <= 0) {
                healPauze = 80;
                boolean iets = false;
                for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(8),
                        m -> m != this && !m.geplofd() && m.vads() < m.maxVads())) {
                    Schade.heal(this, m, m.maxVads() * 0.12 * (elite() ? 1.5 : 1));
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 6, 0.3, 0.3, 0.3, 0);
                    Vec3 a = position().add(0, 0.8, 0), b = m.position().add(0, m.getBbHeight() * 0.6, 0);
                    for (int i = 1; i < 8; i++) {
                        Vec3 p = a.lerp(b, i / 8.0);
                        sl.sendParticles(new DustParticleOptions(0x8CF0FF, 0.9f), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                    iets = true;
                }
                if (iets) {
                    speel("glim");
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2f, 1.5f);
                }
            }
        }
    }

    /** Tempelzwam-ridder: telegraphed sword swings (a red strip in front), a long wind-up and a heavy hit. */
    public static class Tempelridder extends Zwamwezen {
        public static final int UITHAAL = 1;
        private int zwaai = -1;

        public Tempelridder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(80, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(9, level, 0.22);
        }

        @Override
        public int xp() {
            return 34 + 4 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:tempelridder";
        }

        @Override
        protected String tip() {
            return "tempelridder";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == UITHAAL ? "uithaal" : null;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.ARMOR_EQUIP_IRON.value();
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (zwaai < 0 && aanvalPauze <= 0 && level() instanceof ServerLevel sl) {
                zwaai = 18;
                aanvalPauze = 45;
                lookAt(doel, 90, 90);
                setYBodyRot(getYRot());
                zetHouding(UITHAAL);
                Telegraaf.lijn(sl, position(), position().add(voor().scale(4.5)), 3.2, 18, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.HOSTILE, 1f, 0.7f);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (zwaai >= 0) {
                getNavigation().stop();
                setYRot(yBodyRot);
                if (zwaai-- == 0) {
                    Vec3 v = voor(), z = new Vec3(-v.z, 0, v.x);
                    for (Player p : spelersIn(getBoundingBox().inflate(5, 2, 5))) {
                        Vec3 d = p.position().subtract(position());
                        double vooruit = d.dot(v), opzij = Math.abs(d.dot(z));
                        if (vooruit > -0.5 && vooruit < 4.8 && opzij < 1.9) {
                            klap(p, 2.2, Schade.Soort.MELEE, "uithaal");
                        }
                    }
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + v.x * 2, getY() + 1, getZ() + v.z * 2, 3, 0.8, 0.1, 0.8, 0);
                    sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.8f);
                    zetHouding(0);
                }
            } else if (houding() != 0) {
                zetHouding(0);
            }
        }
    }

    /** Sporenpriester: keeps its distance, shoots spore bolts and buffs the creatures around it (Vahoeg, a Knuffelschild). */
    public static class Sporenpriester extends Zwamwezen {
        private int schietPauze = 40, buffPauze = 80;

        public Sporenpriester(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(55, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(7, level, 0.22);
        }

        @Override
        public int xp() {
            return 32 + 4 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:sporenpriester";
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (doel == null) {
                return;
            }
            double d = distanceTo(doel);
            getLookControl().setLookAt(doel, 30, 30);
            if (tickCount % 10 == 0) {
                if (d < 6) {
                    Vec3 weg = position().add(position().subtract(doel.position()).multiply(1, 0, 1).normalize().scale(5));
                    getNavigation().moveTo(weg.x, weg.y, weg.z, 1.1);
                } else if (d > 13) {
                    getNavigation().moveTo(doel, 1.0);
                } else {
                    getNavigation().stop();
                }
            }
            if (--schietPauze <= 0 && d < 16 && hasLineOfSight(doel)) {
                schietPauze = 50 + getRandom().nextInt(15);
                speel("spreuk");
                sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 1.5f);
                Effecten.schiet(sl, position().add(0, 2.2, 0), doel.getEyePosition().subtract(0, 0.4, 0), 0.6, new DustParticleOptions(0xA0FF70, 1.3f), p -> {
                    klap(p, 1.0, Schade.Soort.MAGIE, "sporenschicht");
                    Statussen.geef(p, Status.GESPOREN, 40, Math.max(1, basisSchade(mobLevel()) * 0.2), this);
                });
            }
            if (--buffPauze <= 0) {
                buffPauze = 160;
                VadsrijkMob zwakste = null;
                for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(10), m -> !m.geplofd())) {
                    Statussen.geef(m, Status.VAHOEG, 120, 0.25, this);
                    sl.sendParticles(ParticleTypes.WITCH, m.getX(), m.getY() + m.getBbHeight() + 0.2, m.getZ(), 5, 0.3, 0.2, 0.3, 0);
                    if (m != this && (zwakste == null || m.vads() / m.maxVads() < zwakste.vads() / zwakste.maxVads())) {
                        zwakste = m;
                    }
                }
                if (zwakste != null) {
                    Statussen.geef(zwakste, Status.KNUFFELSCHILD, 160, zwakste.maxVads() * 0.2, this);
                }
                speel("spreuk");
                sl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.HOSTILE, 0.8f, 1.6f);
            }
        }
    }
}
