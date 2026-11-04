package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Bevroren;

/** Het Bevroren Knuffelmeer (lv 25-30): IJzelgeest, Schotsbijter, Versteende Wacht, Ijspuffer. */
public final class Meer {
    /** The telegraph colour of frost (ice blue). */
    public static final int IJS = 0x8FD8FF;

    private Meer() {
    }

    /**
     * IJzelgeest: a ragged frost ghost that floats around you throwing ice shards. Every 7 s it marks the ground under you with
     * an ice-blue circle: 1.2 s later that ground freezes over for 5 s and makes anyone standing on it Bevroren.
     */
    public static class IJzelgeest extends Vliegwezen {
        private int schietPauze = 40, vriesPauze = 100, vriesOver = -1;
        private Vec3 vriesPlek;

        public IJzelgeest(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "ijzelgeest";
        }

        @Override
        public Aard aard() {
            return Aard.KRISTAL;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads(34, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(6.5, level);
        }

        @Override
        public int xp() {
            return 30 + 3 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.VEX_AMBIENT;
        }

        @Override
        protected double minHoogte() {
            return 1.8;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (vriesOver > 0 && --vriesOver == 0 && vriesPlek != null) {
                Vec3 plek = vriesPlek;
                sl.playSound(null, net.minecraft.core.BlockPos.containing(plek), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.2f, 0.6f);
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), plek.x, plek.y + 0.1, plek.z, 40, 1.4, 0.05, 1.4,
                        0.1);
                Effecten.wolk(sl, plek, 3, 100, ParticleTypes.SNOWFLAKE, p -> Bevroren.geef(p, 60, this));
                for (Player p : spelersRond(plek, 3)) {
                    Bevroren.geef(p, 60, this);
                }
            }
            if (doel == null) {
                return null;
            }
            hoek += 0.035;
            if (--schietPauze <= 0 && distanceTo(doel) < 14 && hasLineOfSight(doel)) {
                schietPauze = 45 + getRandom().nextInt(20);
                speel("spreuk");
                playSound(SoundEvents.GLASS_BREAK, 0.6f, 1.8f);
                Effecten.schiet(sl, position().add(0, 1.0, 0), doel.getEyePosition().subtract(0, 0.4, 0), 0.55, new DustParticleOptions(IJS, 1.3f),
                        p -> klap(p, 1.0, Schade.Soort.MAGIE, "ijsscherf"));
            }
            if (--vriesPauze <= 0 && distanceTo(doel) < 16) {
                vriesPauze = 140 + getRandom().nextInt(40);
                vriesPlek = new Vec3(doel.getX(), grond(doel.position()), doel.getZ());
                vriesOver = 24;
                speel("speciaal");
                Telegraaf.cirkel(sl, vriesPlek, 3, 24, IJS);
                sl.playSound(null, doel.blockPosition(), SoundEvents.POWDER_SNOW_FALL, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
            return doel.position().add(Math.cos(hoek) * 5.5, 2.4, Math.sin(hoek) * 5.5);
        }
    }

    /**
     * Schotsbijter: waits under the ice (hidden). Walk over its patch and the ice cracks under you (a red circle, a cracking
     * sound): 1 s later its jaws burst up through the floe. It fights on the ice for a while, then dives again and comes up under
     * you somewhere else. Big, toothy, a grey crystal crest.
     */
    public static class Schotsbijter extends Z2Wezen {
        private int scheur = -1, bovenTijd;

        public Schotsbijter(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "schotsbijter";
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
            return vads(52, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(9, level);
        }

        @Override
        public int xp() {
            return 34 + 4 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.GUARDIAN_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERBORGEN ? "onder" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.0, true);
        }

        @Override
        public boolean stil() {
            return super.stil() || houding() == VERBORGEN;
        }

        @Override
        public void stelIn(int level, @Nullable Element element, boolean elite, java.util.Set<nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix> affixen) {
            super.stelIn(level, element, elite, affixen);
            if (!level().isClientSide() && scheur < 0) {
                zetHouding(VERBORGEN);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (houding() == VERBORGEN) {
                if (scheur > 0) {
                    LivingEntity doel = getTarget();
                    if (scheur % 5 == 0) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), getX(), getY() + 0.1, getZ(), 10, 0.7,
                                0.05, 0.7, 0.05);
                        sl.playSound(null, blockPosition(), SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 1f, 0.5f);
                    }
                    if (--scheur == 0) {
                        boven(sl, doel);
                    }
                    return;
                }
                if (tickCount % 10 == 0) {
                    LivingEntity doel = geldig(getTarget()) ? getTarget()
                            : sl.getNearestPlayer(getX(), getY(), getZ(), 9, pp -> pp instanceof Player q && !q.isCreative() && !q.isSpectator());
                    if (doel != null) {
                        setTarget(doel);
                        // move (unseen, under the ice) to where the target stands now, and crack
                        double g = grond(doel.position());
                        if (Math.abs(g - getY()) < 6) {
                            setPos(doel.getX(), g, doel.getZ());
                        }
                        scheur = 20;
                        Telegraaf.cirkel(sl, position(), 2.0, 20, Telegraaf.ROOD);
                    }
                }
                return;
            }
            if (geldig(getTarget())) {
                if (++bovenTijd > 160 && onGround()) {
                    duik(sl);
                }
            } else if (++bovenTijd > 100) {
                duik(sl);
            }
        }

        private void boven(ServerLevel sl, @Nullable LivingEntity doel) {
            zetHouding(0);
            bovenTijd = 0;
            speel("speciaal");
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), getX(), getY() + 0.5, getZ(), 50, 1, 0.4, 1,
                    0.25);
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 30, 0.8, 0.3, 0.8, 0.1);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.3f, 0.5f);
            sl.playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1f, 1.2f);
            for (Player p : spelersRond(position(), 2.2)) {
                klap(p, 1.6, Schade.Soort.MELEE, "ijsbeet");
                p.push(0, 0.55, 0);
                p.hurtMarked = true;
            }
        }

        private void duik(ServerLevel sl) {
            zetHouding(VERBORGEN);
            getNavigation().stop();
            scheur = -1;
            bovenTijd = 0;
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 25, 0.8, 0.2, 0.8, 0.1);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.HOSTILE, 1f, 0.7f);
        }

        @Override
        protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
            super.addAdditionalSaveData(out);
            out.putBoolean("vr_onder", houding() == VERBORGEN);
        }

        @Override
        protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
            super.readAdditionalSaveData(in);
            zetHouding(in.getBooleanOr("vr_onder", true) ? VERBORGEN : 0);
        }
    }

    /**
     * Versteende Wacht: a knight-sized statue of grey crystal, slow and very heavy (lots of Vads, hardly any knockback). Its
     * slam is announced by a red circle in front of it (1.2 s): stand in it and you are hit hard and stunned for a moment.
     * Versteend (Roze x1.5).
     */
    public static class VersteendeWacht extends Z2Wezen {
        public static final int HEF = 1;
        private int slag = -1;
        private Vec3 slagPlek;

        public VersteendeWacht(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "versteende_wacht";
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
            return vads(95, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(10, level);
        }

        @Override
        public int xp() {
            return 44 + 5 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.STONE_HIT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == HEF ? "hef" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(0.85, false);
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (slag < 0 && aanvalPauze <= 0 && level() instanceof ServerLevel sl) {
                slag = 24;
                aanvalPauze = 60;
                lookAt(doel, 90, 90);
                setYBodyRot(getYRot());
                slagPlek = position().add(voor().scale(2.2));
                zetHouding(HEF);
                Telegraaf.cirkel(sl, slagPlek, 2.6, 24, Telegraaf.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2f, 0.5f);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (slag >= 0) {
                getNavigation().stop();
                setYRot(yBodyRot);
                if (slag-- == 0) {
                    zetHouding(0);
                    speel("klap");
                    for (Player p : spelersRond(slagPlek, 2.6)) {
                        klap(p, 2.4, Schade.Soort.MELEE, "verpletter");
                        Statussen.geef(p, Status.VERDOOFD, 20, 1, this);
                    }
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), slagPlek.x, slagPlek.y + 0.1, slagPlek.z, 50,
                            1.2, 0.1, 1.2, 0.2);
                    sl.sendParticles(ParticleTypes.EXPLOSION, slagPlek.x, slagPlek.y + 0.3, slagPlek.z, 1, 0, 0, 0, 0);
                    sl.playSound(null, blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.2f, 0.6f);
                }
            } else if (houding() == HEF) {
                zetHouding(0);
            }
        }
    }

    /**
     * Ijspuffer: a round frost blowfish that waddles over the ice. Close by it puffs itself up (watch it swell, 0.75 s) and blows
     * a frost cloud around itself: 5 s of cold that makes you Bevroren and nibbles at your Vads. Step out of the cloud.
     */
    public static class Ijspuffer extends Z2Wezen {
        private int puf = -1, pufPauze = 60;

        public Ijspuffer(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "ijspuffer";
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
            return vads(42, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade(5.5, level);
        }

        @Override
        public int xp() {
            return 28 + 3 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.PUFFER_FISH_BLOW_UP;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == TELEGRAAF ? "opblazen" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(0.8, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (puf > 0) {
                getNavigation().stop();
                if (--puf == 0) {
                    zetHouding(0);
                    speel("puf");
                    sl.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.HOSTILE, 1.3f, 0.6f);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), 50, 1.6, 0.6, 1.6, 0.05);
                    Effecten.wolk(sl, position(), 3.5, 100, ParticleTypes.SNOWFLAKE, p -> {
                        Bevroren.geef(p, 50, this);
                        klap(p, 0.35, Schade.Soort.MAGIE, "vrieswolk");
                    });
                }
                return;
            }
            if (geldig(doel) && --pufPauze <= 0 && distanceTo(doel) < 6) {
                pufPauze = 160 + getRandom().nextInt(40);
                puf = 15;
                zetHouding(TELEGRAAF);
                Telegraaf.cirkel(sl, position(), 3.5, 15, IJS);
                sl.playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 1.2f, 0.7f);
            }
        }
    }
}
