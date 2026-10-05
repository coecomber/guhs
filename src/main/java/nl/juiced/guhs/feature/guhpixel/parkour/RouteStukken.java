package nl.juiced.guhs.feature.guhpixel.parkour;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.speelgoed.GlijbaanBlock;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.speelgoed.Speeltjes;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.ZitjeEntity;
import nl.juiced.guhs.registry.ModItems;

/**
 * How a guh on a route really USES one piece ({@link Stap}):
 * <ul>
 *   <li>an {@link Obstakel}: along its {@link Baan}, moved tick by tick (no physics meanwhile, so the bar, the poles and the
 *       cloth never push it aside); the kruiptunnel shows a bump; at the knabbeltafeltje it stands and eats, slowly;</li>
 *   <li>a glijbaantje, wip or schommel: the toy's own seat (ZitjeEntity), exactly like a guh that plays by itself: one lap
 *       down the slide, a short go on the wip or swing;</li>
 *   <li>a pluizige tunnel: the toy's own run through it (hiding included).</li>
 * </ul>
 */
public final class RouteStukken {
    /** The pit stop at the knabbeltafeltje. */
    public static final int EET_TICKS = 80;
    /** On the wip or the schommel during a lap. */
    public static final int ZIT_TICKS = 100;
    /** Guh persistent data: floating over an obstacle until this game time (so a guh that was unloaded halfway lands again). */
    static final String ZWEEF = "guhs_px_parkour_zweef";
    /** (speelgoed.Spelen) the cheer after a ride: skipped on a route, the lap goes on. */
    private static final String JUICH = "guhs_speelgoed_juich";

    /** One piece being used by one guh. */
    public interface Stap {
        /** Where the guh walks to first, and how near is near enough. */
        Vec3 instap();

        default double dichtbij() {
            return 0.9;
        }

        /** It stands at the instap: start. */
        void begin();

        /** Every tick: true = still busy. */
        boolean tick();

        /** Always called at the end (also when it was cut short). */
        void stop();

        /** Cut short now: does the piece count as done? */
        boolean gedaan();

        default int maxTicks() {
            return 400;
        }
    }

    /** The way to use the piece at pos, or null when it is gone (or no piece). */
    @Nullable
    public static Stap maak(GuhEntity guh, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Routes.Soort soort = Routes.soort(state);
        if (soort == null) {
            return null;
        }
        return switch (soort) {
            case OBSTAKEL -> {
                Obstakel o = ((ObstakelBlock) state.getBlock()).soort();
                yield o == Obstakel.KNABBELTAFELTJE ? new Tafel(guh, level, pos, state) : new Over(guh, level, pos, state, o);
            }
            case GLIJBAAN, WIP_SCHOMMEL -> new Zit(guh, level, pos);
            case TUNNEL -> new Tunnel(guh, level, pos);
            case FINISH -> null;
        };
    }

    /** A guh that was floating over an obstacle when it was unloaded (or its goal was dropped): back on its feet. */
    static void land(GuhEntity guh) {
        var data = guh.getPersistentData();
        if (data.contains(ZWEEF) && guh.level().getGameTime() > data.getLongOr(ZWEEF, 0L)) {
            data.remove(ZWEEF);
            guh.noPhysics = false;
            guh.setNoGravity(false);
        }
    }

    /** Floating: no collisions, no gravity (so the bar, the poles and the cloth never push it aside). */
    private static void zweef(GuhEntity guh, boolean aan, boolean zonderZwaartekracht) {
        guh.noPhysics = aan;
        guh.setNoGravity(aan || zonderZwaartekracht);
        guh.setDeltaMovement(Vec3.ZERO);
        if (aan) {
            guh.getPersistentData().putLong(ZWEEF, guh.level().getGameTime() + 5);
        } else {
            guh.getPersistentData().remove(ZWEEF);
        }
    }

    /**
     * Puts a floating guh on this point now. (Its position is set directly: clients work out the walk animation from how far
     * it moved, and a variant with its own way of travelling cannot get in the way.)
     */
    private static void naar(GuhEntity guh, Vec3 doel) {
        Vec3 d = doel.subtract(guh.position());
        guh.getNavigation().stop();
        guh.setDeltaMovement(Vec3.ZERO);
        guh.setPos(doel.x, doel.y, doel.z);
        guh.fallDistance = 0;
        if (d.x * d.x + d.z * d.z > 1.0e-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            guh.setYRot(yaw);
            guh.setYBodyRot(yaw);
            guh.setYHeadRot(yaw);
        }
        guh.getPersistentData().putLong(ZWEEF, guh.level().getGameTime() + 5);
    }

    // =====================================================================================================================

    /** Over (or through) an obstacle along its Baan. */
    static final class Over implements Stap {
        private final GuhEntity guh;
        private final ServerLevel level;
        private final BlockPos pos;
        private final Direction facing;
        private final Obstakel soort;
        private final Baan baan;
        private Vec3 van;
        private int aanloop, t;
        private boolean bezig, vloogAl;

        Over(GuhEntity guh, ServerLevel level, BlockPos pos, BlockState state, Obstakel soort) {
            this.guh = guh;
            this.level = level;
            this.pos = pos.immutable();
            this.facing = state.getValue(ToestelBlock.FACING);
            this.soort = soort;
            boolean valt = soort.valKans() > 0 && guh.getRandom().nextInt(soort.valKans()) == 0;
            Baan heen = soort.baan(valt);
            if (soort.tweeKanten()) {       // (from the end it comes from)
                Baan terug = heen.gespiegeld();
                Vec3 a = wereld(heen.start()), b = wereld(terug.start());
                this.baan = guh.position().distanceToSqr(b) < guh.position().distanceToSqr(a) ? terug : heen;
            } else {
                this.baan = heen;
            }
        }

        private Vec3 wereld(Vec3 lokaal) {
            return ToestelBlock.wereld(pos, facing, lokaal);
        }

        @Override
        public Vec3 instap() {
            return wereld(baan.start());
        }

        @Override
        public void begin() {
            van = guh.position();
            aanloop = (int) Math.ceil(van.distanceTo(instap()) / 0.2);
            t = 0;
            bezig = true;
            vloogAl = guh.isNoGravity();      // (an ender guh may have been flying: it gets that back)
            zweef(guh, true, false);
        }

        @Override
        public boolean tick() {
            if (!bezig || !(level.getBlockState(pos).getBlock() instanceof ObstakelBlock)) {
                return false;
            }
            if (t < aanloop) {
                naar(guh, van.lerp(instap(), (t + 1) / (double) aanloop));
                t++;
                return true;
            }
            int u = t - aanloop;
            if (u >= baan.duur()) {
                return false;
            }
            Baan.Geluid g = baan.geluidOp(u);
            if (g != null) {
                effect(g);
            }
            Vec3 doel = wereld(baan.plek(u + 1));
            if (baan.stil(u)) {
                guh.setDeltaMovement(Vec3.ZERO);
                guh.getPersistentData().putLong(ZWEEF, level.getGameTime() + 5);
                if (u % 5 == 0) {
                    level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + guh.getBbHeight(), guh.getZ(), 1, 0.2, 0.1, 0.2, 0.01);
                }
            } else {
                naar(guh, doel);
            }
            if (soort == Obstakel.KRUIPTUNNEL) {
                BlockState state = level.getBlockState(pos);
                ObstakelBlock.Kruiptunnel.bobbel(level, pos, ObstakelBlock.Kruiptunnel.lokaalZ(pos, state, doel));
                if (u % 6 == 0) {
                    level.playSound(null, guh.blockPosition(), SoundEvents.WOOL_STEP, SoundSource.NEUTRAL, 0.5f, 1.4f);
                }
            } else if (soort == Obstakel.EVENWICHTSBALK && u % 9 == 4 && !baan.stil(u)) {
                level.playSound(null, guh.blockPosition(), SoundEvents.WOOD_STEP, SoundSource.NEUTRAL, 0.35f, 1.5f);
            }
            t++;
            return true;
        }

        private void effect(Baan.Geluid g) {
            Vec3 p = guh.position();
            switch (g) {
                case SPRONG -> level.playSound(null, guh.blockPosition(), ParkourSlice.HUP.get(), SoundSource.NEUTRAL, 0.7f, guh.getVoicePitch() * 1.1f);
                case BOING -> {
                    level.playSound(null, guh.blockPosition(), ParkourSlice.BOING.get(), SoundSource.NEUTRAL, 0.9f, 1f);
                    level.playSound(null, guh.blockPosition(), SpeelgoedFeature.WIEEE.get(), SoundSource.NEUTRAL, 0.8f, guh.getVoicePitch());
                    level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.1, p.z, 6, 0.2, 0.05, 0.2, 0.03);
                }
                case LANDING -> {
                    level.playSound(null, guh.blockPosition(), SoundEvents.WOOL_FALL, SoundSource.NEUTRAL, 0.7f, 1.1f);
                    level.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.1, p.z, 4, 0.2, 0.03, 0.2, 0.01);
                }
                case TUNNEL -> level.playSound(null, guh.blockPosition(), SoundEvents.WOOL_STEP, SoundSource.NEUTRAL, 0.8f, 1.2f);
                case TRIP -> level.playSound(null, guh.blockPosition(), SoundEvents.WOOL_STEP, SoundSource.NEUTRAL, 0.5f, 1.6f);
                case WIEBEL -> level.playSound(null, guh.blockPosition(), SoundEvents.WOOD_STEP, SoundSource.NEUTRAL, 0.4f, 1.3f);
                case PLOF -> {
                    level.playSound(null, guh.blockPosition(), ParkourSlice.OEPS.get(), SoundSource.NEUTRAL, 0.8f, guh.getVoicePitch());
                    level.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.3, p.z, 5, 0.2, 0.1, 0.2, 0.02);
                }
                case OEPS -> level.playSound(null, guh.blockPosition(), SoundEvents.WOOL_FALL, SoundSource.NEUTRAL, 0.8f, 0.9f);
                default -> {
                }
            }
        }

        @Override
        public boolean gedaan() {
            return bezig && t - aanloop >= baan.duur() / 2;
        }

        @Override
        public void stop() {
            if (!bezig) {
                return;
            }
            boolean af = t - aanloop >= baan.duur();
            Vec3 veilig = gedaan() ? wereld(baan.eind()) : instap();
            bezig = false;
            zweef(guh, false, vloogAl);
            if (soort == Obstakel.KRUIPTUNNEL) {
                ObstakelBlock.Kruiptunnel.bobbel(level, pos, 99);
            }
            if (!af) {                                           // (cut short: not left hanging in the air or in the cloth)
                guh.teleportTo(veilig.x, veilig.y, veilig.z);
            }
            if (!level.noCollision(guh)) {                       // (landed inside something: up onto the obstacle's own block)
                guh.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            }
        }

        @Override
        public int maxTicks() {
            return aanloop + baan.duur() + 20;
        }
    }

    /** The mandatory pit stop: it stands in front of the knabbeltafeltje and eats, slowly. */
    static final class Tafel implements Stap {
        private final GuhEntity guh;
        private final ServerLevel level;
        private final BlockPos pos;
        private final Vec3 instap;
        private int t = -1;

        Tafel(GuhEntity guh, ServerLevel level, BlockPos pos, BlockState state) {
            this.guh = guh;
            this.level = level;
            this.pos = pos.immutable();
            // (from the side it comes from: the nearest of the four sides of the table)
            Vec3 midden = Vec3.atBottomCenterOf(pos);
            Vec3 d = guh.position().subtract(midden);
            Direction kant = Math.abs(d.x) > Math.abs(d.z) ? (d.x > 0 ? Direction.EAST : Direction.WEST) : (d.z > 0 ? Direction.SOUTH : Direction.NORTH);
            this.instap = midden.add(kant.getStepX() * 0.95, 0, kant.getStepZ() * 0.95);
        }

        @Override
        public Vec3 instap() {
            return instap;
        }

        @Override
        public double dichtbij() {
            return 0.45;
        }

        @Override
        public void begin() {
            t = 0;
            guh.getNavigation().stop();
        }

        @Override
        public boolean tick() {
            if (t < 0 || !(level.getBlockState(pos).getBlock() instanceof ObstakelBlock)) {
                return false;
            }
            guh.getNavigation().stop();
            guh.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (t % 10 == 3) {
                level.playSound(null, guh.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.5f, 1.1f + guh.getRandom().nextFloat() * 0.3f);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.KAAS_KNABBELS.get()), pos.getX() + 0.5, pos.getY() + 0.7,
                        pos.getZ() + 0.5, 4, 0.12, 0.05, 0.12, 0.03);
            }
            if (t == EET_TICKS - 8) {
                level.playSound(null, guh.blockPosition(), ParkourSlice.SMAK.get(), SoundSource.NEUTRAL, 0.7f, guh.getVoicePitch());
            }
            return ++t < EET_TICKS;
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean gedaan() {
            return t >= EET_TICKS - 20;
        }

        @Override
        public int maxTicks() {
            return EET_TICKS + 10;
        }
    }

    /** A glijbaantje, wip or schommel: the toy's own seat. */
    static final class Zit implements Stap {
        private final GuhEntity guh;
        private final ServerLevel level;
        private final BlockPos pos;
        private int plek, wacht;
        private boolean gezeten;

        Zit(GuhEntity guh, ServerLevel level, BlockPos pos) {
            this.guh = guh;
            this.level = level;
            this.pos = pos.immutable();
            if (level.getBlockState(pos).getBlock() instanceof ToestelBlock t) {
                plek = Math.max(0, t.vrijePlek(level, pos));
            }
        }

        @Override
        public Vec3 instap() {
            BlockState state = level.getBlockState(pos);
            return state.getBlock() instanceof ToestelBlock t ? t.instap(pos, state, plek) : Vec3.atBottomCenterOf(pos);
        }

        @Override
        public double dichtbij() {
            return 1.3;
        }

        @Override
        public void begin() {
        }

        @Override
        public boolean tick() {
            if (gezeten) {
                if (guh.isPassenger()) {
                    return true;
                }
                guh.getPersistentData().remove(JUICH);
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ToestelBlock t)) {
                return false;
            }
            int vrij = t.vrijePlek(level, pos);
            if (vrij < 0) {                                       // (someone is on it: wait a little, then go on without)
                guh.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                return ++wacht < 100;
            }
            plek = vrij;
            boolean glijbaan = t instanceof GlijbaanBlock;
            gezeten = ZitjeEntity.zet(level, pos, plek, guh, glijbaan ? 0 : ZIT_TICKS, 1) != null;
            return gezeten;
        }

        @Override
        public void stop() {
            if (gezeten && guh.getVehicle() instanceof ZitjeEntity z) {     // (cut short while it rides: off, without the cheer)
                z.klaar(false);
            }
            guh.getPersistentData().remove(JUICH);
        }

        @Override
        public boolean gedaan() {
            return gezeten;
        }

        @Override
        public int maxTicks() {
            return 700;
        }
    }

    /** A pluizige tunnel: the toy's own session (walks to the nearest entrance, scurries through, hides a moment, comes out). */
    static final class Tunnel implements Stap {
        private final GuhEntity guh;
        @Nullable
        private final KlusTaak taak;
        private boolean klaar;

        Tunnel(GuhEntity guh, ServerLevel level, BlockPos pos) {
            this.guh = guh;
            this.taak = Speeltjes.voor(level, guh, "tunnel", pos, 1);
        }

        @Override
        public Vec3 instap() {
            return guh.position();    // (the session walks to its entrance itself)
        }

        @Override
        public double dichtbij() {
            return 4;
        }

        @Override
        public void begin() {
        }

        @Override
        public boolean tick() {
            if (taak == null) {
                return false;
            }
            boolean verder = taak.tick();
            klaar |= !verder;
            return verder;
        }

        @Override
        public void stop() {
            if (taak != null) {
                taak.stop();
            }
            guh.getPersistentData().remove(JUICH);
        }

        @Override
        public boolean gedaan() {
            return klaar;
        }

        @Override
        public int maxTicks() {
            return taak == null ? 1 : taak.maxTicks();
        }
    }

    private RouteStukken() {
    }
}
