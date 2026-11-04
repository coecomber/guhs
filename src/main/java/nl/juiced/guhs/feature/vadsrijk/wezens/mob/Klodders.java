package nl.juiced.guhs.feature.vadsrijk.wezens.mob;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;

/** The slimes of the Klodderweide and the Druipgrotten (CONTRACT_VADSRIJK 5; numbers: base x (1 + 0.3 per level)). */
public final class Klodders {
    private Klodders() {
    }

    static double groei(double basis, int level, double perLevel) {
        return basis * (1 + perLevel * (level - 1));
    }

    /** Klodder (and the small Kloddertje): hops at you and bumps. */
    public static class Klodder extends Slijmwezen {
        public Klodder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public boolean kloddertje() {
            return getType() == WezensRegistry.KLODDERTJE.get();
        }

        @Override
        protected double basisVads(int level) {
            return groei(kloddertje() ? 16 : 36, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(kloddertje() ? 3.5 : 6, level, 0.22);
        }

        @Override
        public int xp() {
            return kloddertje() ? 5 + mobLevel() : 12 + 3 * mobLevel();
        }

        @Nullable
        @Override
        public String lootTabel() {
            return kloddertje() ? null : "wezens:klodder";
        }

        @Override
        protected double hopKracht() {
            return kloddertje() ? 0.36 : 0.32;
        }

        @Override
        protected int hopTijd() {
            return kloddertje() ? 11 : 16;
        }
    }

    /** Bubbelklodder: keeps a few blocks away and spits bubbles that make you Plakkerig. */
    public static class Bubbelklodder extends Slijmwezen {
        private int spuugPauze = 40, spuugOver = -1;

        public Bubbelklodder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(32, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(5, level, 0.22);
        }

        @Override
        public int xp() {
            return 14 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:bubbelklodder";
        }

        @Override
        protected double afstandHouden() {
            return 4.5;
        }

        @Override
        protected boolean slijmTick(LivingEntity doel) {
            if (spuugOver > 0 && --spuugOver == 0 && level() instanceof ServerLevel sl) {
                Vec3 van = position().add(0, getBbHeight() * 0.6, 0);
                Effecten.schiet(sl, van, doel.getEyePosition().subtract(0, 0.3, 0), 0.55, new DustParticleOptions(0x9AD8FF, 1.4f), p -> {
                    klap(p, 1.1, Schade.Soort.AFSTAND, "bubbel");
                    Statussen.geef(p, Status.PLAKKERIG, 40, 1, this);
                    p.level().playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 1f, 1.2f);
                });
                playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1.6f);
                return false;
            }
            if (--spuugPauze <= 0 && distanceTo(doel) < 12 && hasLineOfSight(doel)) {
                spuugPauze = 50 + getRandom().nextInt(20);
                spuugOver = 8;
                speel("spuug");
                lookAt(doel, 60, 60);
            }
            return false;
        }
    }

    /** Stuiterklodder: a high bounce that lands on you (the landing spot is telegraphed), Plakkerig on impact. */
    public static class Stuiterklodder extends Slijmwezen {
        private int sprongPauze = 60, laden = -1;
        private boolean inDeLucht;
        private Vec3 landing;

        public Stuiterklodder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(40, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(6.5, level, 0.22);
        }

        @Override
        public int xp() {
            return 16 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:stuiterklodder";
        }

        @Override
        protected boolean slijmTick(LivingEntity doel) {
            if (!(level() instanceof ServerLevel sl)) {
                return false;
            }
            if (inDeLucht) {
                if (onGround() && tickCount % 1 == 0 && getDeltaMovement().y <= 0.01) {
                    inDeLucht = false;
                    Vec3 m = position();
                    for (Player p : spelersRond(m, 2.6)) {
                        klap(p, 1.6, Schade.Soort.MELEE, "stuiter");
                        Statussen.geef(p, Status.PLAKKERIG, 30, 1, this);
                    }
                    sl.sendParticles(ParticleTypes.ITEM_SLIME, m.x, m.y + 0.2, m.z, 30, 1.2, 0.2, 1.2, 0.1);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.4f, 0.6f);
                    hopPauze = 20;
                }
                return true;
            }
            if (laden > 0) {
                if (--laden == 0) {
                    Vec3 naar = landing.subtract(position());
                    double d = Math.sqrt(naar.x * naar.x + naar.z * naar.z);
                    Vec3 h = d < 0.01 ? Vec3.ZERO : new Vec3(naar.x / d, 0, naar.z / d).scale(Math.min(0.9, d / 22.0));
                    setDeltaMovement(h.x, 1.05, h.z);
                    inDeLucht = true;
                    playSound(SoundEvents.SLIME_JUMP, 1.2f, 0.8f);
                }
                return true;
            }
            double d = distanceTo(doel);
            if (onGround() && --sprongPauze <= 0 && d > 3 && d < 14) {
                sprongPauze = 90 + getRandom().nextInt(30);
                laden = 12;
                landing = doel.position();
                speel("stuiter");
                Telegraaf.cirkel(sl, landing, 2.6, 40, Telegraaf.ROZE);
                return true;
            }
            return false;
        }
    }

    /** Moederklodder: always an elite; spits kloddertjes at 66 % and 33 % and bursts into three when she is cheered up. */
    public static class Moederklodder extends Slijmwezen {
        private int gespuugd;

        public Moederklodder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(70, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(7, level, 0.22);
        }

        @Override
        public int xp() {
            return 30 + 4 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:moederklodder";
        }

        @Override
        protected double hopKracht() {
            return 0.26;
        }

        @Override
        protected int hopTijd() {
            return 26;
        }

        @Override
        public void stelIn(int level, @Nullable nl.juiced.guhs.feature.vadsrijk.kern.Element element, boolean elite, java.util.Set<Affix> affixen) {
            super.stelIn(level, element, true, affixen);   // a mother is always an elite
        }

        @Override
        protected boolean slijmTick(LivingEntity doel) {
            double r = vads() / Math.max(1, maxVads());
            int moet = r < 0.33 ? 2 : r < 0.66 ? 1 : 0;
            if (gespuugd < moet && level() instanceof ServerLevel sl) {
                gespuugd++;
                speel("speciaal");
                for (int i = 0; i < 2; i++) {
                    RijkWezen k = splits(sl, WezensRegistry.KLODDERTJE.get(), Math.max(1, mobLevel() - 1), false, i);
                    if (k != null) {
                        k.setTarget(doel);
                    }
                }
                sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.4f, 0.7f);
            }
            return false;
        }

        @Override
        protected void opPlof() {
            super.opPlof();
            if (level() instanceof ServerLevel sl) {
                for (int i = 0; i < 3; i++) {
                    splits(sl, WezensRegistry.KLODDERTJE.get(), Math.max(1, mobLevel() - 1), false, i);
                }
            }
        }
    }

    /**
     * Druipslijm: waits on the ceiling (upside down) until a player walks underneath, then drops with a splash; after that a
     * normal (heavy) slime. No ceiling within 8 blocks: it just starts on the floor.
     */
    public static class Druipslijm extends Slijmwezen {
        public static final int PLAFOND = 1;
        private boolean gekeken, valt;

        public Druipslijm(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(44, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(7, level, 0.22);
        }

        @Override
        public int xp() {
            return 18 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:druipslijm";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == PLAFOND ? "plafond" : null;
        }

        public boolean aanPlafond() {
            return houding() == PLAFOND;
        }

        @Override
        public void tick() {
            if (!level().isClientSide() && !gekeken && !geplofd()) {
                gekeken = true;
                naarPlafond();
            }
            if (!level().isClientSide() && aanPlafond() && !geplofd()) {
                setDeltaMovement(Vec3.ZERO);
                if (tickCount % 10 == 0) {
                    for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(2.5, 14, 2.5),
                            p -> !p.isCreative() && !p.isSpectator() && p.getY() < getY())) {
                        laatVallen(p);
                        break;
                    }
                    if (level() instanceof ServerLevel sl && getRandom().nextInt(3) == 0) {
                        sl.sendParticles(ParticleTypes.DRIPPING_WATER, getX(), getY() - 0.1, getZ(), 1, 0.3, 0, 0.3, 0);
                    }
                }
            }
            super.tick();
        }

        @Override
        protected boolean slijmTick(LivingEntity doel) {
            if (aanPlafond()) {
                if (distanceTo(doel) < 4) {
                    laatVallen(doel);
                }
                return true;
            }
            if (valt) {
                if (onGround() && level() instanceof ServerLevel sl) {
                    valt = false;
                    for (Player p : spelersRond(position(), 2.2)) {
                        klap(p, 1.4, Schade.Soort.MELEE, "druip");
                        Statussen.geef(p, Status.PLAKKERIG, 40, 1, this);
                    }
                    sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.2, getZ(), 24, 1, 0.2, 1, 0.1);
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.5f);
                    hopPauze = 20;
                }
                return true;
            }
            return false;
        }

        @Override
        public void krijgSchade(double n, @Nullable LivingEntity bron) {
            super.krijgSchade(n, bron);
            if (aanPlafond() && !geplofd()) {
                laatVallen(bron);
            }
        }

        private void naarPlafond() {
            BlockPos b = blockPosition();
            for (int dy = 2; dy <= 12; dy++) {
                BlockPos boven = b.above(dy);
                if (!level().getBlockState(boven).getCollisionShape(level(), boven).isEmpty()) {
                    setPos(getX(), boven.getY() - getBbHeight() - 0.01, getZ());
                    setNoGravity(true);
                    zetHouding(PLAFOND);
                    return;
                }
            }
        }

        private void laatVallen(@Nullable LivingEntity doel) {
            setNoGravity(false);
            zetHouding(0);
            valt = true;
            if (doel != null) {
                setTarget(doel);
            }
            playSound(SoundEvents.SLIME_SQUISH, 1f, 0.6f);
        }

        @Override
        public boolean isNoGravity() {
            return aanPlafond() || super.isNoGravity();
        }
    }

    /** Kristalklodder: its crystals bounce a spell back now and then (see WezensRegistry: the Schade.VOOR_KLAP hook). */
    public static class Kristalklodder extends Slijmwezen {
        public Kristalklodder(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return groei(50, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return groei(7.5, level, 0.22);
        }

        @Override
        public int xp() {
            return 20 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:kristalklodder";
        }

        @Override
        protected String tip() {
            return "kristalklodder";
        }

        @Override
        protected net.minecraft.sounds.SoundEvent piepGeluid() {
            return SoundEvents.AMETHYST_BLOCK_HIT;
        }

        /** The chance that a spell bounces back. */
        public static final double WEERKAATS = 0.35;
    }

    /** For the gametests: makes a creature of a type at a spot, set up and in the world. */
    @Nullable
    public static RijkWezen maak(ServerLevel sl, EntityType<?> type, Vec3 pos, int level, boolean elite) {
        if (!(type.create(sl, net.minecraft.world.entity.EntitySpawnReason.COMMAND) instanceof RijkWezen w)) {
            return null;
        }
        w.snapTo(pos.x, pos.y, pos.z, 0, 0);
        w.stelIn(level, null, elite, EnumSet.noneOf(Affix.class));
        sl.addFreshEntity(w);
        return w;
    }
}
