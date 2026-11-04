package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;

/** De Wortelvelden (lv 43-46): Rotzwijn, Wortelslang, Mika-schimmelboer (+ its Schimmelbolletjes). */
public final class Velden {
    private Velden() {
    }

    /**
     * Rotzwijn: a big wild boar with mould on its back. It paws the ground (a long red line, 1.5 s) and charges straight along it;
     * into a wall = 2 s dizzy. Good practice for dodging on a mount. Ouwe Rotkeiler (contract) charges twice in a row; Bramzwijn de
     * Brede (contract) charges a three-wide lane and leaves a patch of brambles (Plakkerig) behind.
     */
    public static class Rotzwijn extends Z3Wezen {
        public static final int SCHRAAP = 1, STORM = 2, DUIZELIG = 3;
        private int fase = -1, stormPauze = 60, stormen;
        private Vec3 richting;
        private final Set<UUID> geraakt = new HashSet<>();

        public Rotzwijn(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "rotzwijn";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.ROT;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(66, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(9.5, level);
        }

        @Override
        public int xp() {
            return 50 + 5 * mobLevel();
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.HOGLIN_ANGRY;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == SCHRAAP ? "schraap" : h == STORM ? "storm" : h == DUIZELIG ? "duizelig" : null;
        }

        private boolean breed() {
            return "bramzwijn_de_brede".equals(contract());
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.1, true);
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
                if (houding() == SCHRAAP) {
                    if (fase % 6 == 0) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), getX(), getY() + 0.1, getZ(),
                                8, 0.4, 0.05, 0.4, 0.1);
                    }
                    if (fase == 0) {
                        zetHouding(STORM);
                        fase = 22;
                        geraakt.clear();
                        sl.playSound(null, blockPosition(), SoundEvents.HOGLIN_ATTACK, SoundSource.HOSTILE, 1.5f, 0.7f);
                    }
                } else if (houding() == STORM) {
                    setDeltaMovement(richting.x * 0.75, getDeltaMovement().y, richting.z * 0.75);
                    setYRot((float) Math.toDegrees(Math.atan2(-richting.x, richting.z)));
                    setYBodyRot(getYRot());
                    double br = breed() ? 2.2 : 0.7;
                    for (Player p : spelersIn(getBoundingBox().inflate(br, 0.5, br))) {
                        if (voor(p) && geraakt.add(p.getUUID())) {
                            klap(p, 2.0, Schade.Soort.MELEE, "stormloop");
                            Hulp.duw(p, richting.scale(1.1).add(0, 0.55, 0));
                        }
                    }
                    if (horizontalCollision || fase == 0) {
                        if (breed()) {
                            Effecten.wolk(sl, position(), 3.5, 140, new DustParticleOptions(0x6A8A3A, 1.0f), q -> Hulp.status(q, Status.PLAKKERIG, 30, 1, this));
                        }
                        if ("ouwe_rotkeiler".equals(contract()) && ++stormen < 2 && geldig(getTarget())) {
                            schraap(sl, getTarget(), 18);
                        } else {
                            stormen = 0;
                            if (horizontalCollision) {
                                zetHouding(DUIZELIG);
                                fase = 40;
                                sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
                                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 10, 0.4, 0.1, 0.4, 0.05);
                            } else {
                                zetHouding(0);
                                fase = -1;
                            }
                        }
                    }
                } else if (houding() == DUIZELIG && fase == 0) {
                    zetHouding(0);
                    fase = -1;
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (geldig(doel) && --stormPauze <= 0 && distanceTo(doel) > 4 && distanceTo(doel) < 16 && hasLineOfSight(doel)) {
                stormPauze = 130 + getRandom().nextInt(60);
                schraap(sl, doel, 30);
            }
        }

        private void schraap(ServerLevel sl, LivingEntity doel, int ticks) {
            zetHouding(SCHRAAP);
            fase = ticks;
            richting = Hulp.richting(position(), doel.position());
            Telegraaf.lijn(sl, position(), position().add(richting.scale(16)), breed() ? 5 : 2.2, ticks, Telegraaf.ROOD);
            sl.playSound(null, blockPosition(), SoundEvents.HOGLIN_ANGRY, SoundSource.HOSTILE, 1.4f, 0.8f);
        }

        public boolean duizelig() {
            return houding() == DUIZELIG;
        }
    }

    /**
     * Wortelslang: a long root snake that lives in the sap valleys. Under the ground it is hidden (not hittable); a dust trail runs
     * toward you, then a red circle (0.8 s) and it shoots up under you (knock-up). It fights 6 s above ground and dives again: hit it
     * while it is up. Slangenkoningin Sapsie (contract) leaves sap pools (Plakkerig) where she comes up.
     */
    public static class Wortelslang extends Z3Wezen {
        public static final int OP = 1;
        private int fase = -1, boven, pauze = 40;
        private Vec3 plek;

        public Wortelslang(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "wortelslang";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.BEEST;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(52, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(9, level);
        }

        @Override
        public int xp() {
            return 50 + 5 * mobLevel();
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SNIFFER_DIGGING;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERBORGEN ? "onder" : h == OP ? "omhoog" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.15, true);
        }

        @Override
        public void aanval(LivingEntity doel) {
            if (!verborgen()) {
                super.aanval(doel);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity doel = getTarget();
            if (tickCount < 3 && doel == null) {
                zetHouding(VERBORGEN);
            }
            if (verborgen()) {
                if (fase > 0) {
                    getNavigation().stop();
                    if (--fase == 0) {
                        // up it comes, under the marked spot
                        snapTo(plek.x, plek.y, plek.z, getYRot(), 0);
                        zetHouding(0);
                        speel("speciaal");
                        boven = 120;
                        sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), plek.x, plek.y + 0.3, plek.z,
                                40, 0.8, 0.3, 0.8, 0.2);
                        for (Player p : spelersRond(plek, 2.2)) {
                            if (voor(p)) {
                                klap(p, 1.8, Schade.Soort.MELEE, "wortelbeet");
                                Hulp.duw(p, new Vec3(0, 0.9, 0));
                            }
                        }
                        if ("slangenkoningin_sapsie".equals(contract())) {
                            Effecten.wolk(sl, plek, 3, 160, new DustParticleOptions(0x3FD0C0, 1.1f), q -> Hulp.status(q, Status.PLAKKERIG, 30, 1, this));
                        }
                    } else if (fase % 3 == 0 && geldig(doel)) {
                        // the dust trail toward the target
                        Vec3 a = position(), b = doel.position();
                        Vec3 p = a.add(b.subtract(a).scale(1 - fase / 16.0));
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()), p.x, grond(p) + 0.1, p.z, 6,
                                0.3, 0.05, 0.3, 0.05);
                    }
                    return;
                }
                if (geldig(doel) && --pauze <= 0 && distanceTo(doel) < 16) {
                    pauze = 20;
                    fase = 16;
                    plek = new Vec3(doel.getX(), grond(doel.position()), doel.getZ());
                    sl.playSound(null, blockPosition(), SoundEvents.SNIFFER_DIGGING, SoundSource.HOSTILE, 1.4f, 0.6f);
                    // the circle shows for the last 0.8 s of the trail
                    Telegraaf.cirkel(sl, plek, 2.2, 16, Telegraaf.ROOD);
                }
                return;
            }
            if (boven > 0 && --boven == 0 || (!geldig(doel) && tickCount % 40 == 0 && boven <= 0)) {
                zetHouding(VERBORGEN);
                speel("spuug");
                pauze = 50;
                boven = 0;
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), getX(), getY() + 0.2, getZ(), 20, 0.5,
                        0.2, 0.5, 0.1);
            }
        }
    }

    /**
     * Mika-schimmelboer: a Mika with a spray tank on its back (Water). It sprays mould fields (Gesporen) and calls up 2 little
     * Schimmelbolletjes. Beat it and its fields go with it.
     */
    public static class MikaSchimmelboer extends Z3Wezen {
        public static final int SPROEI = 1;
        private int sproei = -1, sproeiPauze = 50, roepPauze = 120;
        private final List<Vec3> velden = new ArrayList<>();
        private final List<Integer> veldTijd = new ArrayList<>();

        public MikaSchimmelboer(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "mika_schimmelboer";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.MIKA;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(48, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(8.5, level);
        }

        @Override
        public int xp() {
            return 50 + 5 * mobLevel();
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.OCELOT_HURT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == SPROEI ? "mik" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.0, true);
        }

        public int velden() {
            return velden.size();
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            // its fields: alive as long as it is (ZONE3_PLAN: "verslaan = de velden verdwijnen")
            for (int i = velden.size() - 1; i >= 0; i--) {
                Vec3 v = velden.get(i);
                int t = veldTijd.get(i) - 1;
                if (t <= 0) {
                    velden.remove(i);
                    veldTijd.remove(i);
                    continue;
                }
                veldTijd.set(i, t);
                if (t % 4 == 0) {
                    sl.sendParticles(new DustParticleOptions(0x7C93A8, 1.2f), v.x, v.y + 0.2, v.z, 6, 1.4, 0.05, 1.4, 0);
                }
                if (t % 20 == 0) {
                    for (Player p : spelersRond(v, 2.6)) {
                        if (voor(p)) {
                            Hulp.status(p, Status.GESPOREN, 60, Math.max(1, mobLevel() * 0.35), this);
                        }
                    }
                }
            }
            if (stil()) {
                return;
            }
            LivingEntity doel = getTarget();
            if (sproei >= 0) {
                getNavigation().stop();
                if (sproei-- == 0 && geldig(doel)) {
                    zetHouding(0);
                    speel("spuug");
                    Vec3 v = new Vec3(doel.getX(), grond(doel.position()), doel.getZ());
                    sl.playSound(null, blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.2f, 0.6f);
                    for (int i = 1; i <= 8; i++) {
                        Vec3 p = position().add(0, 1.4, 0).add(v.subtract(position()).scale(i / 8.0)).add(0, Math.sin(i / 8.0 * Math.PI) * 1.5, 0);
                        sl.sendParticles(new DustParticleOptions(0x7C93A8, 1.0f), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                    }
                    velden.add(v);
                    veldTijd.add(240);
                    if (velden.size() > 4) {
                        velden.remove(0);
                        veldTijd.remove(0);
                    }
                } else if (sproei < 0) {
                    zetHouding(0);
                }
                return;
            }
            if (!geldig(doel)) {
                return;
            }
            if (--sproeiPauze <= 0 && distanceTo(doel) < 12) {
                sproeiPauze = 90 + getRandom().nextInt(40);
                sproei = 18;
                zetHouding(SPROEI);
                Telegraaf.cirkel(sl, new Vec3(doel.getX(), grond(doel.position()), doel.getZ()), 2.6, 18, Telegraaf.PAARS);
            }
            if (--roepPauze <= 0) {
                roepPauze = 400;
                Hulp.zeg(sl, this, "schimmelboer.roep");
                for (int i = 0; i < 2; i++) {
                    roep(sl, Z3WezensRegistry.SCHIMMELBOLLETJE.get(), Math.max(1, mobLevel() - 2), i);
                }
            }
        }
    }

    /** Schimmelbolletje: a little bouncing mould puff the Schimmelboer calls up (no loot of its own, little xp); it bursts in spores. */
    public static class Schimmelbolletje extends Z3Wezen {
        private int leven = 20 * 40;

        public Schimmelbolletje(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "schimmelbolletje";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.ROT;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(12, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(4, level);
        }

        @Override
        public int xp() {
            return 10 + mobLevel();
        }

        @Override
        protected String loopAnim() {
            return "hop";
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SLIME_SQUISH_SMALL;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.2, false);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide() && !geplofd() && --leven <= 0) {
                discard();
            }
        }

        @Override
        public void aanval(LivingEntity doel) {
            super.aanval(doel);
            if (getRandom().nextInt(3) == 0) {
                Hulp.status(doel, Status.GESPOREN, 40, Math.max(1, mobLevel() * 0.25), this);
            }
        }
    }
}
