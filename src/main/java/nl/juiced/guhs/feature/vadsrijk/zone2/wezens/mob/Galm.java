package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;

/** De Galmhellingen (lv 20-25): Splinterling, Galmvleermuis, Sneeuwknabbelaar, Kristalnaaldkruiper. */
public final class Galm {
    private Galm() {
    }

    /**
     * Splinterling: a heavy grey crystal golem. Every hit it takes makes it spit a fan of shards back at whoever hit it
     * (the hook in Z2WezensRegistry calls {@link #splinter}), so ranged players get shards in return. Versteend (Roze x1.5).
     */
    public static class Splinterling extends Z2Wezen {
        private int splinterPauze;

        public Splinterling(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "splinterling";
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
            return vads(58, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(8, level);
        }

        @Override
        public int xp() {
            return 30 + 4 * mobLevel();
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(0.9, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (splinterPauze > 0) {
                splinterPauze--;
            }
        }

        /** Hit by someone: a fan of 3 shards back at them (at most every 0.5 s). */
        public void splinter(LivingEntity bron) {
            if (splinterPauze > 0 || geplofd() || !(level() instanceof ServerLevel sl) || distanceTo(bron) > 18) {
                return;
            }
            splinterPauze = 10;
            speel("speciaal");
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1f, 1.3f);
            Vec3 van = position().add(0, getBbHeight() * 0.7, 0);
            Vec3 naar = bron.getEyePosition().subtract(0, 0.4, 0).subtract(van);
            for (int i = -1; i <= 1; i++) {
                double a = Math.toRadians(i * 12);
                Vec3 r = new Vec3(naar.x * Math.cos(a) - naar.z * Math.sin(a), naar.y, naar.x * Math.sin(a) + naar.z * Math.cos(a));
                Effecten.schiet(sl, van, van.add(r), 0.7, new DustParticleOptions(0xB8B4C4, 1.2f), p -> klap(p, 0.35, Schade.Soort.AFSTAND, "splinter"));
            }
        }
    }

    /**
     * Galmvleermuis: a crystal-winged bat. It fights a few seconds, then vanishes (only its ringing echo is heard, a little
     * louder each time), circles you unseen and reappears behind you with a hard bite. A rising chime = it is about to strike.
     */
    public static class Galmvleermuis extends Vliegwezen {
        private int fase = 80;
        private boolean onzichtbaar;
        private int beetPauze;

        public Galmvleermuis(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "galmvleermuis";
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
            return vads(34, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(7, level);
        }

        @Override
        public int xp() {
            return 26 + 3 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.BAT_HURT;
        }

        @Override
        protected double snelheid() {
            return onzichtbaar ? 0.2 : 0.16;
        }

        @Override
        protected double minHoogte() {
            return 1.4;
        }

        @Override
        public boolean verborgen() {
            return houding() == VERBORGEN;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (beetPauze > 0) {
                beetPauze--;
            }
            if (doel == null) {
                if (onzichtbaar) {
                    zichtbaar(sl);
                }
                return null;
            }
            fase--;
            if (onzichtbaar) {
                hoek += 0.08;
                if (fase % 15 == 0) {   // only audible: the echo
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.9f, 1.6f + getRandom().nextFloat() * 0.3f);
                    sl.playSound(null, blockPosition(), SoundEvents.BAT_AMBIENT, SoundSource.HOSTILE, 0.5f, 0.8f);
                }
                if (fase == 12) {      // the tell: a rising chime
                    sl.playSound(null, doel.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4f, 2.0f);
                }
                if (fase <= 0) {
                    // reappear right behind the target and bite
                    Vec3 achter = doel.position().subtract(doel.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6)).add(0, 1.2, 0);
                    setPos(achter.x, achter.y, achter.z);
                    zichtbaar(sl);
                    if (distanceTo(doel) < 3.2) {
                        speel("aanval");
                        klap(doel, 1.6, Schade.Soort.MELEE, "galmbeet");
                        sl.playSound(null, blockPosition(), SoundEvents.BAT_HURT, SoundSource.HOSTILE, 1f, 0.6f);
                    }
                    return null;
                }
                return doel.position().add(Math.cos(hoek) * 7, 2.5, Math.sin(hoek) * 7);
            }
            if (fase <= 0) {
                onzichtbaar = true;
                fase = 60 + getRandom().nextInt(30);
                zetHouding(VERBORGEN);
                setInvisible(true);
                sl.sendParticles(new DustParticleOptions(0xC8C0E8, 1.0f), getX(), getY() + 0.4, getZ(), 16, 0.4, 0.3, 0.4, 0);
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.2f, 1.2f);
                return null;
            }
            hoek += 0.06;
            if (beetPauze <= 0 && distanceTo(doel) < 2.2) {
                beetPauze = 25;
                doHurtTarget(sl, doel);
            }
            boolean duik = (fase / 20) % 2 == 0;
            return duik ? doel.position().add(0, 1.0, 0) : doel.position().add(Math.cos(hoek) * 3, 2.2, Math.sin(hoek) * 3);
        }

        private void zichtbaar(ServerLevel sl) {
            onzichtbaar = false;
            fase = 70 + getRandom().nextInt(30);
            zetHouding(0);
            setInvisible(false);
            sl.sendParticles(new DustParticleOptions(0xC8C0E8, 1.0f), getX(), getY() + 0.4, getZ(), 16, 0.4, 0.3, 0.4, 0);
        }
    }

    /**
     * Sneeuwknabbelaar: a white-grey snow predator that hunts in packs of 3-4 (a spawn brings its pack). One howl alerts the
     * pack; they circle and lunge in turns (a short crouch first), never all at once.
     */
    public static class Sneeuwknabbelaar extends Z2Wezen {
        private boolean pakGemaakt;
        private int sprongPauze, sprong = -1;
        private boolean gehuild;

        public Sneeuwknabbelaar(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            sprongPauze = 40 + getRandom().nextInt(40);
        }

        @Override
        public String naam() {
            return "sneeuwknabbelaar";
        }

        @Override
        public Aard aard() {
            return Aard.BEEST;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads(36, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(6.5, level);
        }

        @Override
        public int xp() {
            return 22 + 3 * mobLevel();
        }

        @Override
        protected String houdingAnim(int h) {
            return h == TELEGRAAF ? "loer" : null;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Sneeuwknabbelaar.class));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (!pakGemaakt && tickCount >= 2) {
                pak(sl);
            }
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                gehuild = false;
                return;
            }
            if (!gehuild) {
                gehuild = true;
                speel("speciaal");
                sl.playSound(null, blockPosition(), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.HOSTILE, 1.2f, 1.2f);
                for (Sneeuwknabbelaar k : sl.getEntitiesOfClass(Sneeuwknabbelaar.class, getBoundingBox().inflate(24, 8, 24), k -> !k.geplofd() && k.getTarget() == null)) {
                    k.setTarget(doel);
                    k.gehuild = true;
                }
            }
            if (sprong > 0) {
                getNavigation().stop();
                lookAt(doel, 90, 90);
                if (--sprong == 0) {
                    zetHouding(0);
                    Vec3 v = doel.position().subtract(position());
                    Vec3 h = v.multiply(1, 0, 1).normalize().scale(Math.min(1.3, 0.22 * v.horizontalDistance()));
                    setDeltaMovement(h.x, 0.45, h.z);
                    speel("aanval");
                    sl.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 1f, 1.3f);
                    aanvalPauze = 0;
                    sprong = -2;   // in the air: bite on contact
                }
                return;
            }
            if (sprong == -2) {
                if (distanceTo(doel) < 1.8) {
                    klap(doel, 1.4, Schade.Soort.MELEE, "sprongbeet");
                    sprong = -1;
                } else if (onGround() && getDeltaMovement().y <= 0) {
                    sprong = -1;
                }
                return;
            }
            double d = distanceTo(doel);
            if (--sprongPauze <= 0 && d > 3.5 && d < 8 && onGround() && hasLineOfSight(doel)) {
                // only one pack member lunges at a time
                boolean ander = !sl.getEntitiesOfClass(Sneeuwknabbelaar.class, getBoundingBox().inflate(16), k -> k != this && k.sprong > 0).isEmpty();
                sprongPauze = ander ? 15 : 70 + getRandom().nextInt(40);
                if (!ander) {
                    sprong = 12;
                    zetHouding(TELEGRAAF);
                    Telegraaf.lijn(sl, position(), doel.position(), 1.4, 12, Telegraaf.ROOD);
                }
            }
        }

        private void pak(ServerLevel sl) {
            pakGemaakt = true;
            if (spawner.isEmpty() || klein()) {
                return;
            }
            int vrienden = 2 + getRandom().nextInt(2);
            var sp = Spawners.van(spawner);
            int max = sp == null ? 4 : sp.max();
            int al = sl.getEntitiesOfClass(Sneeuwknabbelaar.class, getBoundingBox().inflate(40, 16, 40), k -> spawner.equals(k.spawner) && !k.geplofd()).size();
            for (int i = 0; i < vrienden && al < max; i++, al++) {
                RijkWezen k = splits(sl, getType(), mobLevel(), false, i);
                if (k instanceof Sneeuwknabbelaar kn) {
                    kn.spawner = spawner;
                    kn.pakGemaakt = true;
                }
            }
        }

        @Override
        protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
            super.addAdditionalSaveData(out);
            out.putBoolean("vr_pak", true);
        }

        @Override
        protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
            super.readAdditionalSaveData(in);
            pakGemaakt = in.getBooleanOr("vr_pak", true);
        }
    }

    /**
     * Kristalnaaldkruiper: lies buried in the snow (only a few crystal needles stick out, no nameplate). Come within 5 blocks
     * and the snow trembles for a moment (a pink-red circle), then it bursts out under you: damage and a knock-up. It fights
     * fast and low; with nobody around for 10 s it digs in again.
     */
    public static class Kristalnaaldkruiper extends Z2Wezen {
        private int uitbarsting = -1, rust;

        public Kristalnaaldkruiper(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "kristalnaaldkruiper";
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
            return 28 + 3 * mobLevel();
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERBORGEN ? "verstopt" : h == TELEGRAAF ? "tril" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.15, false);
        }

        @Override
        public void stelIn(int level, @Nullable Element element, boolean elite, java.util.Set<nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix> affixen) {
            super.stelIn(level, element, elite, affixen);
            if (!level().isClientSide() && uitbarsting < 0 && getTarget() == null) {
                zetHouding(VERBORGEN);
            }
        }

        @Override
        public boolean stil() {
            return super.stil() || houding() == VERBORGEN || houding() == TELEGRAAF;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (houding() == VERBORGEN) {
                setTarget(null);
                if (tickCount % 10 == 0) {
                    Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 5, pp -> pp instanceof Player q && !q.isCreative() && !q.isSpectator());
                    if (p != null) {
                        uitbarsting = 14;
                        zetHouding(TELEGRAAF);
                        setTarget(p);
                        Telegraaf.cirkel(sl, p.position(), 2.2, 14, Telegraaf.ROOD);
                        sl.playSound(null, blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 1.4f, 0.5f);
                    }
                }
                return;
            }
            if (uitbarsting > 0) {
                LivingEntity doel = getTarget();
                if (uitbarsting % 4 == 0) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), getX(), getY() + 0.2, getZ(), 8, 0.6, 0.1,
                            0.6, 0.1);
                }
                if (--uitbarsting == 0) {
                    if (geldig(doel) && distanceTo(doel) < 6) {
                        setPos(doel.getX(), getY(), doel.getZ());
                    }
                    zetHouding(0);
                    speel("speciaal");
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), getX(), getY() + 0.5, getZ(), 40, 0.8,
                            0.4, 0.8, 0.2);
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.3f, 0.8f);
                    for (Player p : spelersRond(position(), 2.4)) {
                        klap(p, 1.5, Schade.Soort.MELEE, "hinderlaag");
                        p.push(0, 0.6, 0);
                        p.hurtMarked = true;
                    }
                    rust = 0;
                }
                return;
            }
            if (!geldig(getTarget())) {
                if (++rust > 200 && onGround()) {
                    zetHouding(VERBORGEN);
                    getNavigation().stop();
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), getX(), getY() + 0.2, getZ(), 20, 0.5,
                            0.1, 0.5, 0.1);
                }
            } else {
                rust = 0;
            }
        }

        @Override
        protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
            super.addAdditionalSaveData(out);
            out.putBoolean("vr_verstopt", houding() == VERBORGEN);
        }

        @Override
        protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
            super.readAdditionalSaveData(in);
            zetHouding(in.getBooleanOr("vr_verstopt", true) ? VERBORGEN : 0);
        }
    }

    /** (for the gametest/dev command: the shout of a number) */
    static Component tekst(String key, Object... args) {
        return Component.translatable("vadsrijk.z2wezens." + key, args);
    }

    static void zeg(ServerLevel sl, LivingEntity e, String key) {
        Getallen.tekst(sl, e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), tekst(key), 0.85f);
    }

    static void plakkerig(LivingEntity p, int ticks, LivingEntity bron) {
        Statussen.geef(p, Status.PLAKKERIG, ticks, 1, bron);
    }
}
