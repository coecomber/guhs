package nl.juiced.guhs.feature.vadsrijk.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;

/** The Mosgeest (floats, dodges) and the Grauwe Knaag (a fast grey rodent that comes in groups). */
public final class Anderen {
    private Anderen() {
    }

    /**
     * Mosgeest: floats about 2 blocks above the ground, circles its target and throws moss balls; now and then it whooshes
     * aside instead of being hit (the Schade.VOOR_KLAP hook in WezensRegistry calls {@link #probeerOntwijken}).
     */
    public static class Mosgeest extends RijkWezen {
        private int schietPauze = 40, ontwijkPauze;
        private double hoek;

        public Mosgeest(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            setNoGravity(true);
        }

        @Override
        protected double basisVads(int level) {
            return Klodders.groei(34, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return Klodders.groei(6, level, 0.22);
        }

        @Override
        public int xp() {
            return 18 + 3 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:mosgeest";
        }

        @Override
        public Element standaardElement() {
            return Element.NATUUR;
        }

        @Override
        protected String tip() {
            return "mosgeest";
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
            this.targetSelector.addGoal(2, new RijkWezen.Aggro(this));
        }

        @Override
        public boolean isNoGravity() {
            return !geplofd();
        }

        @Override
        protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (ontwijkPauze > 0) {
                ontwijkPauze--;
            }
            if (stil()) {
                setDeltaMovement(getDeltaMovement().scale(0.5));
                return;
            }
            LivingEntity doel = getTarget();
            int grond = grondOnder(sl);
            Vec3 wil;
            if (doel != null) {
                hoek += 0.04;
                wil = doel.position().add(Math.cos(hoek) * 4.5, 1.8, Math.sin(hoek) * 4.5);
                lookAt(doel, 30, 30);
                if (--schietPauze <= 0 && distanceTo(doel) < 12 && hasLineOfSight(doel)) {
                    schietPauze = 45 + getRandom().nextInt(20);
                    speel("aanval");
                    playSound(SoundEvents.ALLAY_THROW, 1f, 0.8f);
                    Effecten.schiet(sl, position().add(0, 0.8, 0), doel.getEyePosition().subtract(0, 0.4, 0), 0.5, new DustParticleOptions(0x6FB04E, 1.5f),
                            p -> klap(p, 1.0, Schade.Soort.AFSTAND, "mosbal"));
                }
            } else {
                Vec3 thuis = thuisDoel();
                if (thuis != null) {
                    hoek = Math.atan2(thuis.z - getZ(), thuis.x - getX());
                } else if (tickCount % 60 == 0) {
                    hoek = getRandom().nextDouble() * Math.PI * 2;
                }
                wil = position().add(Math.cos(hoek) * 2, 0, Math.sin(hoek) * 2);
                if (tickCount % 60 >= 40) {
                    wil = position();
                }
            }
            double hoogte = Math.max(wil.y, grond + 1.6);
            if (getY() > grond + 6 && doel == null) {
                hoogte = grond + 2;
            }
            Vec3 naar = new Vec3(wil.x - getX(), hoogte - getY(), wil.z - getZ());
            double snel = 0.12 * (affixen.contains(nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix.SNEL) ? 1.4 : 1);
            Vec3 v = naar.lengthSqr() > snel * snel ? naar.normalize().scale(snel) : naar;
            setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.5)));
            if (doel == null && naar.horizontalDistanceSqr() > 0.01) {
                setYRot((float) Math.toDegrees(Math.atan2(-naar.x, naar.z)));
                setYBodyRot(getYRot());
            }
            if (doel != null && aanvalPauze <= 0 && distanceTo(doel) < 1.6) {
                doHurtTarget(sl, doel);
                aanvalPauze = 30;
            }
        }

        /** The floor under it (looking down at most 10 blocks, so under a giant mushroom cap it is the forest floor, not the cap). */
        private int grondOnder(ServerLevel sl) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int dy = 0; dy <= 10; dy++) {
                m.set(getBlockX(), getBlockY() - dy, getBlockZ());
                if (!sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                    return m.getY() + 1;
                }
            }
            return sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
        }

        @Override
        protected boolean gebruiktNavigatie() {
            return false;
        }

        /** The dodge: 30 % chance when it is ready (every 3 s at most): whoosh, 3 blocks to the side. */
        public boolean probeerOntwijken(@Nullable LivingEntity bron) {
            if (geplofd() || stil() || ontwijkPauze > 0 || getRandom().nextDouble() >= 0.3 || !(level() instanceof ServerLevel sl)) {
                return false;
            }
            ontwijkPauze = 60;
            Vec3 van = bron == null ? new Vec3(1, 0, 0) : position().subtract(bron.position()).multiply(1, 0, 1);
            Vec3 opzij = new Vec3(-van.z, 0, van.x).normalize().scale(getRandom().nextBoolean() ? 3 : -3);
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.6, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            BlockPos doel = BlockPos.containing(position().add(opzij));
            if (sl.getBlockState(doel).getCollisionShape(sl, doel).isEmpty() && sl.getBlockState(doel.above()).getCollisionShape(sl, doel.above()).isEmpty()) {
                setPos(getX() + opzij.x, getY(), getZ() + opzij.z);
            }
            speel("ontwijk");
            sl.playSound(null, blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.HOSTILE, 0.8f, 1.6f);
            return true;
        }
    }

    /** Grauwe Knaag: fast, comes in a pack of 2-3 (it brings its friends when a spawner makes it), bites and hops back. */
    public static class Knaag extends RijkWezen {
        private boolean pakGemaakt;

        public Knaag(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected double basisVads(int level) {
            return Klodders.groei(30, level, 0.3);
        }

        @Override
        protected double basisSchade(int level) {
            return Klodders.groei(5.5, level, 0.22);
        }

        @Override
        public int xp() {
            return 12 + 2 * mobLevel();
        }

        @Override
        public String lootTabel() {
            return "wezens:knaag";
        }

        @Override
        public Element standaardElement() {
            return Element.GEEN;
        }

        @Override
        protected SoundEvent piepGeluid() {
            return SoundEvents.BAT_AMBIENT;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Knaag.class));
            this.targetSelector.addGoal(2, new RijkWezen.Aggro(this));
        }

        @Override
        public void aanval(LivingEntity doel) {
            super.aanval(doel);
            Vec3 weg = position().subtract(doel.position()).multiply(1, 0, 1);
            if (weg.lengthSqr() > 1.0E-4) {
                weg = weg.normalize().scale(0.55);
                setDeltaMovement(weg.x, 0.32, weg.z);
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || pakGemaakt || tickCount < 2 || !(level() instanceof ServerLevel sl)) {
                return;
            }
            pakGemaakt = true;
            if (spawner.isEmpty() || klein()) {
                return;
            }
            int vrienden = 1 + getRandom().nextInt(2);
            int al = sl.getEntitiesOfClass(Knaag.class, getBoundingBox().inflate(40, 16, 40), k -> spawner.equals(k.spawner) && !k.geplofd()).size();
            var sp = nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners.van(spawner);
            int max = sp == null ? 4 : sp.max();
            for (int i = 0; i < vrienden && al < max; i++, al++) {
                RijkWezen k = splits(sl, getType(), mobLevel(), false, i);
                if (k instanceof Knaag kn) {
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

}
