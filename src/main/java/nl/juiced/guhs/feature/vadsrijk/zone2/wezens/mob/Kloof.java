package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;

/** De Splinterkloof (lv 30-34): Mika-mijnwerker, Mika-boorder, Kloofgier, Grauwe Kristalspin. */
public final class Kloof {
    private Kloof() {
    }

    /**
     * Mika-mijnwerker: an upright Mika with a hard hat, a head lamp and a pickaxe. Hacks at you (a telegraphed overhead blow now
     * and then). Below 30 % it panics and runs ("Mamaaa!"); 5 s later it comes back with one or two friends. Defeated = a puff
     * of smoke, off to the fort.
     */
    public static class MikaMijnwerker extends Z2Wezen {
        public static final int HAK = 1, VLUCHT = 2;
        private boolean gevlucht, hulp;
        private int vlucht = -1, hak = -1, verveeld;

        public MikaMijnwerker(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_mijnwerker";
        }

        @Override
        public Aard aard() {
            return Aard.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads(46, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(8, level);
        }

        @Override
        public int xp() {
            return (hulp ? 20 : 34) + 4 * mobLevel();
        }

        @Override
        protected String vluchtRoep() {
            return "mika.weg";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == HAK ? "hef" : h == VLUCHT ? "ren" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.05, true);
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (hak < 0 && aanvalPauze <= 0 && level() instanceof ServerLevel sl) {
                if (getRandom().nextInt(3) == 0) {
                    hak = 14;
                    aanvalPauze = 40;
                    zetHouding(HAK);
                    Telegraaf.lijn(sl, position(), position().add(voor().scale(3)), 1.6, 14, Telegraaf.ROOD);
                } else {
                    aanvalPauze = 22;
                    super.aanval(doel);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (hak >= 0) {
                getNavigation().stop();
                if (hak-- == 0) {
                    zetHouding(0);
                    speel("klap");
                    Vec3 v = voor();
                    for (Player p : spelersIn(getBoundingBox().inflate(3.5, 2, 3.5))) {
                        Vec3 d = p.position().subtract(position());
                        if (d.dot(v) > -0.3 && d.dot(v) < 3.3 && Math.abs(d.dot(new Vec3(-v.z, 0, v.x))) < 1.1) {
                            klap(p, 1.9, Schade.Soort.MELEE, "houweel");
                        }
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 1.6f);
                    sl.sendParticles(ParticleTypes.CRIT, getX() + voor().x * 2, getY() + 0.3, getZ() + voor().z * 2, 10, 0.4, 0.1, 0.4, 0.1);
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (vlucht > 0) {
                vlucht--;
                if (vlucht % 20 == 0 && geldig(doel)) {
                    Vec3 weg = position().subtract(doel.position()).multiply(1, 0, 1);
                    weg = weg.lengthSqr() < 0.01 ? voor() : weg.normalize();
                    getNavigation().moveTo(getX() + weg.x * 12, getY(), getZ() + weg.z * 12, 1.5);
                }
                if (vlucht == 0) {
                    zetHouding(0);
                    hulpRoepen(sl, doel);
                }
                return;
            }
            if (!gevlucht && !hulp && vads() < maxVads() * 0.3 && geldig(doel)) {
                gevlucht = true;
                vlucht = 100;
                zetHouding(VLUCHT);
                setTarget(null);
                Galm.zeg(sl, this, "mika.paniek." + getRandom().nextInt(3));
                sl.playSound(null, blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 1.2f, 1.5f);
                bewaarDoel = doel;
                return;
            }
            // the friends it brought (not counted by a spawner) go home when there's nobody to fight for a while
            if (hulp && !geldig(doel) && ++verveeld > 600) {
                verslaanZonderBuit(sl);
            } else if (geldig(doel)) {
                verveeld = 0;
            }
        }

        private LivingEntity bewaarDoel;

        private void hulpRoepen(ServerLevel sl, @Nullable LivingEntity eerder) {
            LivingEntity doel = geldig(bewaarDoel) ? bewaarDoel : eerder;
            Galm.zeg(sl, this, "mika.hulp." + getRandom().nextInt(3));
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.HOSTILE, 1.2f, 1.4f);
            int n = 1 + getRandom().nextInt(2);
            for (int i = 0; i < n; i++) {
                RijkWezen w = splits(sl, getType(), mobLevel(), false, i);
                if (w instanceof MikaMijnwerker m) {
                    m.hulp = true;
                    m.gevlucht = true;
                    sl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.8, m.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                    if (geldig(doel)) {
                        m.setTarget(doel);
                    }
                }
            }
            if (geldig(doel)) {
                setTarget(doel);
            }
        }

        /** The helpers' "going home": the smoke puff without xp or loot. */
        private void verslaanZonderBuit(ServerLevel sl) {
            nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Verslagen.rookpluim(sl, position(), getBbHeight(),
                    nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensPlekken.MIKA_FORT, null);
            discard();
        }

        @Override
        public void setTarget(@Nullable LivingEntity t) {
            if (t != null && vlucht > 0) {
                return;   // running away: no fighting
            }
            super.setTarget(t);
        }

        public boolean hulp() {
            return hulp;
        }

        public boolean gevlucht() {
            return gevlucht;
        }

        @Override
        protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
            super.addAdditionalSaveData(out);
            out.putBoolean("vr_hulp", hulp);
            out.putBoolean("vr_gevlucht", gevlucht);
        }

        @Override
        protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
            super.readAdditionalSaveData(in);
            hulp = in.getBooleanOr("vr_hulp", false);
            gevlucht = in.getBooleanOr("vr_gevlucht", false);
        }
    }

    /**
     * Mika-boorder: a Mika with a little hand drill on a backpack engine. Keeps 7-10 blocks away; it aims (a red line for 1 s)
     * and fires a spinning drill bit along that line (Vuur, sets you Brandend). Step out of the line.
     */
    public static class MikaBoorder extends Z2Wezen {
        public static final int MIK = 1;
        private int mik = -1, mikPauze = 50;
        private Vec3 mikVan, mikNaar;

        public MikaBoorder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_boorder";
        }

        @Override
        public Aard aard() {
            return Aard.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads(38, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(7.5, level);
        }

        @Override
        public int xp() {
            return 32 + 4 * mobLevel();
        }

        @Override
        protected String vluchtRoep() {
            return "mika.weg";
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == MIK ? "mik" : null;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                if (mik >= 0) {
                    mik = -1;
                    zetHouding(0);
                }
                return;
            }
            if (mik > 0) {
                getNavigation().stop();
                lookAt(doel, 30, 30);
                if (mik % 6 == 0) {
                    sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.2, getZ(), 2, 0.1, 0.1, 0.1, 0.01);
                }
                if (--mik == 0) {
                    zetHouding(0);
                    speel("spuug");
                    sl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.2f, 0.6f);
                    sl.playSound(null, blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 1f, 1.6f);
                    Effecten.schiet(sl, mikVan, mikNaar, 0.85, new DustParticleOptions(0xFF8A3A, 1.4f), p -> {
                        klap(p, 1.7, Schade.Soort.AFSTAND, "boorkop");
                        Statussen.geef(p, Status.BRANDEND, 60, Math.max(1, mobLevel() * 0.5), this);
                    });
                    mik = -1;
                }
                return;
            }
            double d = distanceTo(doel);
            if (d < 6.5 && tickCount % 10 == 0) {
                Vec3 weg = position().subtract(doel.position()).multiply(1, 0, 1).normalize();
                getNavigation().moveTo(getX() + weg.x * 5, getY(), getZ() + weg.z * 5, 1.1);
            } else if (d > 11 && tickCount % 10 == 0) {
                getNavigation().moveTo(doel, 1.0);
            }
            if (--mikPauze <= 0 && d < 16 && hasLineOfSight(doel)) {
                mikPauze = 70 + getRandom().nextInt(30);
                mik = 20;
                zetHouding(MIK);
                mikVan = position().add(0, 1.1, 0);
                Vec3 r = doel.position().add(0, 1.0, 0).subtract(mikVan).normalize();
                mikNaar = mikVan.add(r.scale(20));
                Telegraaf.lijn(sl, position(), position().add(r.multiply(1, 0, 1).normalize().scale(16)), 1.4, 20, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 1f, 1.5f);
            }
        }
    }

    /**
     * Kloofgier: a big grey vulture with crystal-crusted wings that rides the updrafts high above the ravine. It marks you with a
     * red circle (1.25 s) and dives straight down onto it; after the dive it sits on the ground, winded, for 2 s: hit it then.
     */
    public static class Kloofgier extends Vliegwezen {
        public static final int UITGEPUT = 1;
        private int duikPauze = 80, duik = -1, uitgeput;
        private Vec3 duikPlek;

        public Kloofgier(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "kloofgier";
        }

        @Override
        public Aard aard() {
            return Aard.BEEST;
        }

        @Override
        public Element standaardElement() {
            return Element.NATUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads(44, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(8.5, level);
        }

        @Override
        public int xp() {
            return 34 + 4 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.PHANTOM_BITE;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == UITGEPUT ? "uitgeput" : h == TELEGRAAF ? "duik" : null;
        }

        @Override
        public boolean isNoGravity() {
            return !geplofd() && uitgeput <= 0;
        }

        @Override
        protected double snelheid() {
            return duik > 0 ? 0.0 : 0.17;
        }

        @Override
        protected double minHoogte() {
            return uitgeput > 0 || duik == 0 ? 0 : 2.5;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (uitgeput > 0) {
                if (--uitgeput == 0) {
                    zetHouding(0);
                    setDeltaMovement(0, 0.6, 0);
                    sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.2f, 0.7f);
                }
                return position();
            }
            if (duik > 0) {
                if (--duik == 0) {
                    // the dive itself
                    sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.4f, 0.8f);
                    speel("ram");
                }
                return position();
            }
            if (duik == 0 && duikPlek != null) {
                Vec3 naar = duikPlek.subtract(position());
                setDeltaMovement(naar.normalize().scale(Math.min(1.1, naar.length())));
                if (naar.length() < 1.3 || onGround()) {
                    duik = -1;
                    klapRond(duikPlek, 2.6, 1.8, Schade.Soort.MELEE, "duikvlucht");
                    sl.sendParticles(ParticleTypes.EXPLOSION, duikPlek.x, duikPlek.y + 0.3, duikPlek.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.CLOUD, duikPlek.x, duikPlek.y + 0.3, duikPlek.z, 20, 1, 0.2, 1, 0.05);
                    uitgeput = 40;
                    zetHouding(UITGEPUT);
                    duikPlek = null;
                }
                return null == duikPlek ? position() : duikPlek;
            }
            if (doel == null) {
                return null;
            }
            hoek += 0.03;
            if (--duikPauze <= 0 && hasLineOfSight(doel)) {
                duikPauze = 110 + getRandom().nextInt(40);
                duik = 25;
                duikPlek = new Vec3(doel.getX(), grond(doel.position()), doel.getZ());
                zetHouding(TELEGRAAF);
                Telegraaf.cirkel(sl, duikPlek, 2.6, 25, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.6f);
                return position();
            }
            return doel.position().add(Math.cos(hoek) * 7, 9, Math.sin(hoek) * 7);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide() && !geplofd() && duik == 0 && duikPlek != null) {
                // keep the dive speed (the base glide would damp it)
                Vec3 naar = duikPlek.subtract(position());
                if (naar.lengthSqr() > 0.5) {
                    setDeltaMovement(naar.normalize().scale(Math.min(1.1, naar.length())));
                }
            }
        }
    }

    /**
     * Grauwe Kristalspin: a grey spider with crystal shards on its back. It spits sticky webs (Plakkerig: slowed) and leaves a web
     * patch where it hits, then runs in to bite.
     */
    public static class Kristalspin extends Z2Wezen {
        private int webPauze = 50;

        public Kristalspin(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "kristalspin";
        }

        @Override
        public Aard aard() {
            return Aard.KRISTAL;
        }

        @Override
        public boolean versteend() {
            return true;
        }

        @Override
        public Element standaardElement() {
            return Element.GEEN;
        }

        @Override
        protected double basisVads(int level) {
            return vads(40, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(7.5, level);
        }

        @Override
        public int xp() {
            return 32 + 4 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SPIDER_AMBIENT;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.1, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (geldig(doel) && --webPauze <= 0 && distanceTo(doel) > 2.5 && distanceTo(doel) < 13 && hasLineOfSight(doel)) {
                webPauze = 80 + getRandom().nextInt(30);
                speel("spuug");
                sl.playSound(null, blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.HOSTILE, 1f, 1.5f);
                Effecten.schiet(sl, position().add(0, 0.6, 0), doel.getEyePosition().subtract(0, 0.6, 0), 0.6, new DustParticleOptions(0xEEEEF4, 1.3f), p -> {
                    klap(p, 0.8, Schade.Soort.AFSTAND, "web");
                    Galm.plakkerig(p, 60, this);
                    Effecten.wolk(sl, new Vec3(p.getX(), p.getY(), p.getZ()), 1.8, 100, new DustParticleOptions(0xDADAE6, 0.9f), q -> Galm.plakkerig(q, 30, this));
                });
            }
        }
    }
}
