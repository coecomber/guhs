package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

/** De Schorsstad (lv 40-43): Schorskruiper, Lantaarnloze, Harsgolem. */
public final class Schors {
    private Schors() {
    }

    /**
     * Schorskruiper: a wooden spider that passes for bark on the wall or floor (hidden: no nameplate, not hittable). Walk past
     * within 4 blocks and it jumps out at you; then it bites and spins webs (Plakkerig + a sticky patch). Idle 12 s: it hides again.
     */
    public static class Schorskruiper extends Z3Wezen {
        private int webPauze = 50, verveeld = 239;

        public Schorskruiper(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "schorskruiper";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.BEEST;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(44, level);
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
        public double aggroBereik() {
            return verborgen() ? 0 : 10;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SPIDER_AMBIENT;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == VERBORGEN ? "verstopt" : null;
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(1.15, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (verborgen()) {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                if (tickCount % 5 == 0) {
                    for (Player p : spelersIn(getBoundingBox().inflate(4, 2.5, 4))) {
                        if (voor(p)) {
                            zetHouding(0);
                            speel("speciaal");
                            sl.playSound(null, blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.HOSTILE, 1.4f, 0.6f);
                            sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
                            sl.sendParticles(new DustParticleOptions(0x6B5A4A, 1.4f), getX(), getY() + 0.4, getZ(), 24, 0.6, 0.3, 0.6, 0);
                            klap(p, 1.4, Schade.Soort.MELEE, "verrassing");
                            Hulp.duw(p, Hulp.richting(position(), p.position()).scale(0.6).add(0, 0.4, 0));
                            setTarget(p);
                            verveeld = 0;
                            break;
                        }
                    }
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (!geldig(doel)) {
                if (++verveeld > 240) {
                    verveeld = 0;
                    zetHouding(VERBORGEN);
                }
                return;
            }
            verveeld = 0;
            if (--webPauze <= 0 && distanceTo(doel) > 2.5 && distanceTo(doel) < 12 && hasLineOfSight(doel)) {
                webPauze = 90 + getRandom().nextInt(30);
                speel("spuug");
                sl.playSound(null, blockPosition(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 1f, 1.5f);
                Effecten.schiet(sl, position().add(0, 0.6, 0), doel.getEyePosition().subtract(0, 0.6, 0), 0.6, new DustParticleOptions(0xE8E2D2, 1.3f), p -> {
                    if (voor(p)) {
                        klap(p, 0.8, Schade.Soort.AFSTAND, "web");
                        Hulp.status(p, Status.PLAKKERIG, 60, 1, this);
                        Effecten.wolk(sl, p.position(), 1.8, 100, new DustParticleOptions(0xD8D2C4, 0.9f), q -> Hulp.status(q, Status.PLAKKERIG, 30, 1, this));
                    }
                });
            }
        }
    }

    /**
     * Lantaarnloze: the grey ghost of a tree-folk lantern bearer (Water). Every ~9 s it blows the light out around it (a grey circle
     * 1 s): darkness for whoever stands inside, and it hits harder while its lantern is out. Click it then to light its lantern
     * again: it is stunned for 5 s.
     */
    public static class Lantaarnloze extends Z3Vliegwezen {
        public static final int BLAAS = 1, UIT = 2;
        private int blaas = -1, blaasPauze = 80, uit, schot = 50;

        public Lantaarnloze(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "lantaarnloze";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.GEEST;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(40, level);
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
        public double aggroBereik() {
            return 13;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.SOUL_ESCAPE.value();
        }

        @Override
        protected String houdingAnim(int h) {
            return h == BLAAS ? "blaas" : h == UIT ? "uit" : null;
        }

        /** Its lantern is out (it is stronger; a click lights it again). */
        public boolean lantaarnUit() {
            return houding() == UIT;
        }

        @Override
        protected InteractionResult mobInteract(Player p, InteractionHand hand) {
            if (lantaarnUit() && !geplofd() && voor(p)) {
                if (level() instanceof ServerLevel sl) {
                    aansteken(sl);
                }
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(p, hand);
        }

        /** Light its lantern again: 5 s stunned. */
        public void aansteken(ServerLevel sl) {
            uit = 0;
            zetHouding(0);
            Statussen.geef(this, Status.VERDOOFD, 100, 1, null);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.2, getZ(), 20, 0.3, 0.3, 0.3, 0.02);
            sl.sendParticles(new DustParticleOptions(0xFFC86A, 1.5f), getX(), getY() + 1.2, getZ(), 20, 0.5, 0.5, 0.5, 0);
            sl.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 1.2f);
            Hulp.zeg(sl, this, "lantaarn.aan");
        }

        @Override
        protected double snelheid() {
            return 0.12;
        }

        @Nullable
        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (blaas >= 0) {
                if (blaas-- == 0) {
                    zetHouding(UIT);
                    uit = 160;
                    speel("speciaal");
                    sl.playSound(null, blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 1.6f, 0.6f);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 3, 0.6, 3, 0.02);
                    for (Player p : spelersRond(position(), 6)) {
                        if (voor(p)) {
                            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
                        }
                    }
                }
                return position();
            }
            if (uit > 0 && --uit == 0) {
                zetHouding(0);
            }
            if (doel == null) {
                return null;
            }
            if (--blaasPauze <= 0 && uit <= 0) {
                blaasPauze = 180 + getRandom().nextInt(60);
                blaas = 20;
                zetHouding(BLAAS);
                Telegraaf.cirkel(sl, position(), 6, 20, Telegraaf.GRIJS);
                return position();
            }
            if (--schot <= 0 && distanceTo(doel) < 14 && hasLineOfSight(doel)) {
                schot = 55 + getRandom().nextInt(20);
                speel("spreuk");
                sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1f, 1.4f);
                Effecten.schiet(sl, position().add(0, 1.3, 0), doel.getEyePosition(), 0.55, new DustParticleOptions(0x7FB8D8, 1.3f), p -> {
                    if (voor(p)) {
                        klap(p, lantaarnUit() ? 1.5 : 1.1, Schade.Soort.MAGIE, "dwaallicht");
                    }
                });
            }
            hoek += 0.025;
            double r = distanceTo(doel) < 4 ? 6 : 5;
            return doel.position().add(Math.cos(hoek) * r, 2.2, Math.sin(hoek) * r);
        }

    }

    /**
     * Harsgolem: a slow, sturdy golem of grey resin (Water). It slams the floor (a red circle 1.2 s) and leaves sticky puddles
     * (Plakkerig). At 30 % an Amberroze crystal breaks out of its back: hits from behind then do x1.5 (Z3WezensRegistry).
     * Harskolos is its area leader; De Harsreus (contract) sends three rings of shockwaves outward.
     */
    public static class Harsgolem extends Z3Wezen {
        public static final int HEF = 1;
        private int slag = -1, slagPauze = 70, ring = -1;
        private boolean kristal;

        public Harsgolem(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "harsgolem";
        }

        @Override
        public WezenAard wezenAard() {
            return WezenAard.HARS;
        }

        @Override
        public Element standaardElement() {
            return Element.WATER;
        }

        @Override
        protected double basisVads(int level) {
            return vads3(96, level);
        }

        @Override
        protected double basisSchade(int level) {
            return schade3(10, level);
        }

        @Override
        public int xp() {
            return xp3(1.5);
        }

        @Override
        public double aggroBereik() {
            return 9;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.HONEY_BLOCK_STEP;
        }

        @Override
        protected String houdingAnim(int h) {
            return h == HEF ? "hef" : null;
        }

        /** The Amberroze weak spot is out (below 30 %) and this hit comes from behind. */
        public boolean zwakkePlek(@Nullable LivingEntity bron) {
            return kristal && bron != null && vanAchter(bron);
        }

        @Override
        protected void registerGoals() {
            standaardDoelen(0.95, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || isNoAi() || stil() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (!kristal && vads() < maxVads() * 0.3) {
                kristal = true;
                speel("speciaal");
                Hulp.zeg(sl, this, "harsgolem.kristal");
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5f, 0.7f);
            }
            if (kristal && tickCount % 10 == 0) {
                Vec3 rug = position().subtract(voor().scale(0.7)).add(0, getBbHeight() * 0.7, 0);
                sl.sendParticles(new DustParticleOptions(0xFF7AC8, 1.3f), rug.x, rug.y, rug.z, 3, 0.2, 0.2, 0.2, 0);
            }
            if (ring >= 0) {
                // De Harsreus: rings at 3, 6, 9 blocks, one every 10 ticks
                if (ring % 10 == 0) {
                    double r = 3 + 3 * (ring / 10);
                    sl.sendParticles(new DustParticleOptions(0xD89A3A, 1.6f), getX(), getY() + 0.2, getZ(), 40, r * 0.7, 0.05, r * 0.7, 0);
                    for (Player p : spelersRond(position(), r + 0.5)) {
                        double d = p.position().distanceTo(position());
                        if (voor(p) && d > r - 2 && p.onGround()) {
                            klap(p, 1.2, Schade.Soort.MELEE, "schokgolf");
                        }
                    }
                }
                if (++ring > 20) {
                    ring = -1;
                }
            }
            if (slag >= 0) {
                getNavigation().stop();
                if (slag-- == 0) {
                    zetHouding(0);
                    speel("klap");
                    sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 0.6f);
                    sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 2, 1, 0.1, 1, 0);
                    for (Player p : spelersRond(position(), 4)) {
                        if (voor(p)) {
                            klap(p, 1.9, Schade.Soort.MELEE, "grondschok");
                        }
                    }
                    Effecten.wolk(sl, position(), 2.6, 120, new DustParticleOptions(0xC88A2A, 1.0f), q -> Hulp.status(q, Status.PLAKKERIG, 30, 1, this));
                    if ("de_harsreus".equals(contract())) {
                        ring = 0;
                    }
                }
                return;
            }
            LivingEntity doel = getTarget();
            if (geldig(doel) && --slagPauze <= 0 && distanceTo(doel) < 4.5) {
                slagPauze = 110 + getRandom().nextInt(40);
                slag = 24;
                zetHouding(HEF);
                Telegraaf.cirkel(sl, position(), 4, 24, Telegraaf.ROOD);
            }
        }
    }
}
