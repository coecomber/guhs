package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;

/** De Kruinzee (lv 30-34): Bladrover, Grauwe Rups, Bijtbloesem. */
public final class Kruin {
    private Kruin() {
    }

    /**
     * Bladrover: a Mika sky pirate with a boarding hook (Vuur). It throws the hook along a red line (1 s); whoever is still in the
     * line is yanked to it and gets a slash. Groups of 3 around the zeppelin dock. Leader: Kapitein-Bladrover.
     */
    public static class Bladrover extends Z3Wezen {
        public static final int HAAK = 1;
        private int haak = -1, haakPauze = 60;
        private Vec3 haakVan, haakRichting;
        private LivingEntity getrokken;
        private int trekTicks;

        public Bladrover(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "bladrover";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.VUUR;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(40, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(7.5, level);
        }

        @Override
        public int xp() {
            return xp3(1.0);
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == HAAK ? "mik" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.05, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (trekTicks > 0 && getrokken != null) {
                if (--trekTicks == 0 && geldig(getrokken) && distanceTo(getrokken) < 3.5) {
                    speel("aanval");
                    klap(getrokken, 1.3, Schade.Soort.MELEE, "enterhaak");
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (haak >= 0) {
                getNavigation().stop();
                if (haak-- == 0) {
                    zetHouding(0);
                    speel("spuug");
                    sl.playSound(null, blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 1.3f, 0.7f);
                    for (int i = 1; i <= 10; i++) {
                        Vec3 p = haakVan.add(haakRichting.scale(i * 1.1)).add(0, 1.1, 0);
                        sl.sendParticles(new DustParticleOptions(0x8A8A90, 0.9f), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                    }
                    for (Player p : spelersIn(getBoundingBox().inflate(12, 3, 12))) {
                        if (Hulp.inStrook(haakVan, haakRichting, 11, 1.6, p.position()) && voor(p)) {
                            Vec3 naar = position().subtract(p.position()).multiply(1, 0, 1);
                            Hulp.duw(p, naar.normalize().scale(Math.min(1.6, naar.length() * 0.28)).add(0, 0.35, 0));
                            klap(p, 0.5, Schade.Soort.AFSTAND, "enterhaak");
                            getrokken = p;
                            trekTicks = 8;
                            sl.playSound(null, p.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1f, 0.8f);
                            break;
                        }
                    }
                }
                return;
            }
            if (geldig(doel) && --haakPauze <= 0) {
                double d = distanceTo(doel);
                if (d > 4 && d < 11 && hasLineOfSight(doel)) {
                    haakPauze = 110 + getRandom().nextInt(50);
                    haak = 20;
                    zetHouding(HAAK);
                    haakVan = position();
                    haakRichting = Hulp.richting(position(), doel.position());
                    Telegraaf.lijn(sl, haakVan, haakVan.add(haakRichting.scale(11)), 1.6, 20, Telegraaf.ROOD);
                    sl.playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1f, 1.2f);
                } else {
                    haakPauze = 20;
                }
            }
        }
    }

    /**
     * Grauwe Rups: a big grey caterpillar with mouldy tufts. Slow; when it spots you it curls into a ball (a red line shows the
     * way, 1 s) and rolls straight along it; then it lies dizzy for 2 s and takes x1.5 (see Z3WezensRegistry). Grootvader Rups
     * (contract) rolls three times in a row.
     */
    public static class GrauweRups extends Z3Wezen {
        public static final int BAL = 1, ROL = 2, DUIZELIG = 3;
        private int fase = -1, rolPauze = 60, rollen;
        private Vec3 rolRichting;
        private final java.util.Set<java.util.UUID> geraakt = new java.util.HashSet<>();

        public GrauweRups(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "grauwe_rups";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.ROT;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(54, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(8, level);
        }

        @Override
        public int xp() {
            return xp3(1.2);
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SILVERFISH_AMBIENT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == BAL ? "bal" : h == ROL ? "rol" : h == DUIZELIG ? "duizelig" : null;
        }

        public boolean duizelig() {
            return houding() == DUIZELIG;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(0.9, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (fase >= 0) {
                getNavigation().stop();
                fase--;
                if (houding() == BAL && fase == 0) {
                    zetHouding(ROL);
                    fase = 20;
                    geraakt.clear();
                    sl.playSound(null, blockPosition(), SoundEvents.ARMADILLO_ROLL, SoundSource.HOSTILE, 1.4f, 0.6f);
                } else if (houding() == ROL) {
                    setDeltaMovement(rolRichting.x * 0.62, getDeltaMovement().y, rolRichting.z * 0.62);
                    setYRot((float) Math.toDegrees(Math.atan2(-rolRichting.x, rolRichting.z)));
                    sl.sendParticles(new DustParticleOptions(0x9C9AA8, 1.2f), getX(), getY() + 0.2, getZ(), 3, 0.4, 0.1, 0.4, 0);
                    for (Player p : spelersIn(getBoundingBox().inflate(0.6, 0.5, 0.6))) {
                        if (voor(p) && geraakt.add(p.getUUID())) {
                            klap(p, 1.7, Schade.Soort.MELEE, "rol");
                            Hulp.duw(p, rolRichting.scale(0.9).add(0, 0.45, 0));
                        }
                    }
                    if (horizontalCollision || fase == 0) {
                        if (horizontalCollision) {
                            sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1f, 0.6f);
                        }
                        if ("grootvader_rups".equals(contract()) && ++rollen < 3 && geldig(getTarget())) {
                            krul(sl, getTarget(), 12);
                        } else {
                            rollen = 0;
                            zetHouding(DUIZELIG);
                            fase = 40;
                            sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 8, 0.3, 0.1, 0.3, 0.05);
                        }
                    }
                } else if (houding() == DUIZELIG && fase == 0) {
                    zetHouding(0);
                    fase = -1;
                }
                if (fase == 0 && houding() == 0) {
                    fase = -1;
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (geldig(doel) && --rolPauze <= 0 && distanceTo(doel) > 3.5 && distanceTo(doel) < 14 && hasLineOfSight(doel)) {
                rolPauze = 140 + getRandom().nextInt(60);
                krul(sl, doel, 20);
            }
        }

        private void krul(ServerLevel sl, LivingEntity doel, int ticks) {
            zetHouding(BAL);
            fase = ticks;
            rolRichting = Hulp.richting(position(), doel.position());
            Telegraaf.lijn(sl, position(), position().add(rolRichting.scale(12)), 1.8, ticks, Telegraaf.ROOD);
            sl.playSound(null, blockPosition(), SoundEvents.ARMADILLO_HURT_REDUCED, SoundSource.HOSTILE, 1.2f, 0.7f);
        }
    }

    /**
     * Bijtbloesem: a rotten flower that passes for ordinary blossom until you are close (no nameplate while disguised). Bites
     * up close and blows a cone of spores (yellow-grey cone, 0.9 s; Gesporen). It never moves: from afar it is harmless.
     */
    public static class Bijtbloesem extends Z3Wezen {
        public static final int VERMOMD = 1, BLAAS = 2;
        private int blaas = -1, blaasPauze = 40;
        private Vec3 blaasRichting;

        public Bijtbloesem(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "bijtbloesem";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.ROT;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(36, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(8, level);
        }

        @Override
        public int xp() {
            return xp3(1.0);
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.BIG_DRIPLEAF_TILT_DOWN;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERMOMD ? "vermomd" : h == BLAAS ? "blaas" : null;
        }

        @Override
        public boolean zonderNaam() {
            return houding() == VERMOMD || super.zonderNaam();
        }

        @Override
        public double aggroBereik() {
            return 6;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new Aggro(this));
        }

        @Override
        protected boolean gebruiktNavigatie() {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            setDeltaMovement(0, getDeltaMovement().y, 0);
            LivingEntity doel = geldig(getTarget()) ? getTarget() : null;
            if (doel != null && distanceTo(doel) > 14) {
                setTarget(null);
                doel = null;
            }
            if (doel == null) {
                if (houding() != VERMOMD && blaas < 0) {
                    zetHouding(VERMOMD);
                }
                return;
            }
            if (houding() == VERMOMD) {
                zetHouding(0);
                speel("speciaal");
                sl.playSound(null, blockPosition(), SoundEvents.BIG_DRIPLEAF_TILT_UP, SoundSource.HOSTILE, 1.3f, 0.6f);
            }
            lookAt(doel, 30, 30);
            if (blaas >= 0) {
                if (blaas-- == 0) {
                    zetHouding(0);
                    speel("spuug");
                    sl.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.HOSTILE, 1.2f, 0.7f);
                    for (int i = 1; i <= 6; i++) {
                        Vec3 p = position().add(blaasRichting.scale(i)).add(0, 0.8, 0);
                        sl.sendParticles(new DustParticleOptions(0xC8C070, 1.3f), p.x, p.y, p.z, 4, i * 0.12, 0.2, i * 0.12, 0);
                    }
                    for (Player p : spelersIn(getBoundingBox().inflate(7, 2, 7))) {
                        Vec3 d = p.position().subtract(position()).multiply(1, 0, 1);
                        if (voor(p) && d.length() < 6.5 && d.normalize().dot(blaasRichting) > 0.7) {
                            klap(p, 0.9, Schade.Soort.MAGIE, "stuifmeel");
                            Hulp.status(p, Status.GESPOREN, 100, Math.max(1, mobLevel() * 0.4), this);
                        }
                    }
                }
                return;
            }
            double d = distanceTo(doel);
            if (--blaasPauze <= 0 && d < 6.5) {
                blaasPauze = 90 + getRandom().nextInt(40);
                blaas = 18;
                zetHouding(BLAAS);
                blaasRichting = Hulp.richting(position(), doel.position());
                Telegraaf.lijn(sl, position(), position().add(blaasRichting.scale(6)), 4.5, 18, Telegraaf.GEEL);
                return;
            }
            if (d < 2.8 && aanvalPauze <= 0) {
                aanvalPauze = 25;
                aanval(doel);
            }
        }
    }
}
