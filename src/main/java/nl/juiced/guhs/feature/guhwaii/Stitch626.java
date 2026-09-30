package nl.juiced.guhs.feature.guhwaii;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The 626-guh (GuhVariant STITCH626): a blue alien guh with big ears and an extra pair of little arms. Made to be "te vads"
 * (but a guh can never be too vads!), he's a bit of a rascal, and always lief.
 * <ul>
 *   <li>He climbs straight up walls ({@link #opKlimbaar}: bumping into a wall is a ladder for him, also with you on his back),
 *       and now and then he climbs a wall and hangs upside down from the ceiling for a while, giggling ({@link Plafond});
 *       {@link VerhaalVlaggen#KLIMT} tells the client he clings (the renderer tilts him up the wall / turns him upside down).</li>
 *   <li>His extra arms carry two things at once: chores bring twice as much per trip ({@link #draagFactor} 2).</li>
 *   <li>His own button in the guh menu and his own emote: the ukelele ({@link Emote#UKELELE}), "Aloha, njeg!" with real
 *       chords and music notes.</li>
 * </ul>
 */
public final class Stitch626 implements VariantGedrag {
    public static final Stitch626 GEDRAG = new Stitch626();
    /** (entity persistent data) when he last started strumming (the "Aloha, njeg!" is said once per emote). */
    static final String UKE_START = "guhs_guhwaii_uke";
    /** Every tick this 1-in-N chance to go for a ceiling (checked only while the Plafond goal isn't running). */
    public static final int PLAFOND_KANS = 700;

    private Stitch626() {
    }

    /** Is this guh a 626-guh? */
    public static boolean is(@Nullable Object e) {
        return e instanceof GuhEntity guh && guh.getVariant() == GuhVariant.STITCH626;
    }

    // =================================================================================================================
    // VariantGedrag
    // =================================================================================================================

    @Override
    public boolean opKlimbaar(GuhEntity guh) {
        if (guh.isInWater() || guh.isOrderedToSit() && !guh.isVehicle()) {
            return false;
        }
        return guh.horizontalCollision || GuhHooks.heeft(guh, VerhaalVlaggen.KLIMT);
    }

    @Override
    public int draagFactor(Mob guh) {
        return 2;
    }

    @Nullable
    @Override
    public String speciaalKnop() {
        return "gui.guhs.guhwaii.ukelele";
    }

    @Override
    public void speciaal(GuhEntity guh, ServerPlayer owner) {
        if (!GuhEmotes.canStart(guh)) {
            GuhQuests.say(owner, guh, "gui.guhs.guhwaii.ukelele.niet_nu");
            return;
        }
        guh.emotes.start(Emote.UKELELE, false, GuhEmotes.Source.OWNER);
        guh.getLookControl().setLookAt(owner);
    }

    @Override
    public void tick(GuhEntity guh) {
        Level level = guh.level();
        if (level.isClientSide()) {
            return;
        }
        // clinging to a wall (going up) or hanging from a ceiling: the flag the client draws him by
        if ((guh.tickCount & 1) == 0) {
            Plafond.opruimen(guh);
            boolean klimt = Plafond.hangt(guh) || !guh.onGround() && guh.horizontalCollision && !guh.isInWater() && guh.getDeltaMovement().y > -0.05;
            if (klimt != GuhHooks.heeft(guh, VerhaalVlaggen.KLIMT)) {
                GuhHooks.zet(guh, VerhaalVlaggen.KLIMT, klimt);
            }
        }
        // the ukelele: "Aloha, njeg!" once, then a strum every half second
        if (guh.emotes.current() == Emote.UKELELE && level instanceof ServerLevel server) {
            long nu = level.getGameTime();
            long start = guh.getPersistentData().getLongOr(UKE_START, 0L);
            if (nu - start > Emote.UKELELE.onceTicks + 10) {
                guh.getPersistentData().putLong(UKE_START, nu);
                start = nu;
                aloha(server, guh);
            }
            long t = nu - start;
            if (t % 10 == 3) {
                Vec3 v = guh.getLookAngle();
                GuhwaiiItems.UkeleleItem.tokkel(server, guh.getX() + v.x * 0.4, guh.getY() + 0.5 * guh.getScale(), guh.getZ() + v.z * 0.4,
                        (int) (t / 10) % 4);
            }
        }
    }

    /** "Aloha, njeg!": his owner (and players close by) hear it; the owner gets the ukelele advancement. */
    private static void aloha(ServerLevel level, GuhEntity guh) {
        guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch() * 1.1f);
        for (ServerPlayer p : level.players()) {
            if (p.distanceTo(guh) < 12) {
                GuhQuests.say(p, guh, "gui.guhs.guhwaii.aloha");
                if (guh.isOwnedBy(p)) {
                    GuhwaiiFeature.advancement(p, "ukelele");
                }
            }
        }
    }

    /** (GuhHooks.doelen) every guh gets the ceiling goal; it only ever runs for a 626-guh. */
    static void doelen(GuhEntity guh, GoalSelector goals) {
        goals.addGoal(6, new Plafond(guh));
    }

    // =================================================================================================================
    // hanging from the ceiling
    // =================================================================================================================

    /**
     * Now and then (not sitting, not ridden, not busy, his owner close) a 626-guh next to a wall under a low ceiling climbs
     * up that wall, crawls a little way along the ceiling upside down, hangs there giggling, and lets go (guhs never get
     * hurt by a fall).
     */
    public static class Plafond extends Goal {
        private static final String HANGT = "guhs_guhwaii_hangt";
        private final GuhEntity guh;
        private int fase;          // 0 klimmen, 1 kruipen, 2 hangen
        private int tijd;
        private int plafondY;
        private Direction muur = Direction.NORTH;
        private int kruipen;

        public Plafond(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        /** The 626-guhs on (or on their way to) a ceiling right now (server; the goal runs). */
        private static final java.util.Set<GuhEntity> LOPEND = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

        /** Is he hanging from (or climbing to) a ceiling right now? */
        public static boolean hangt(GuhEntity guh) {
            return LOPEND.contains(guh);
        }

        /** (tick) the goal isn't running but the marker is still there (saved mid-hang): gravity back on. */
        static void opruimen(GuhEntity guh) {
            if (guh.getPersistentData().getBooleanOr(HANGT, false) && !LOPEND.contains(guh)) {
                guh.getPersistentData().remove(HANGT);
                guh.setNoGravity(false);
                GuhHooks.zet(guh, VerhaalVlaggen.KLIMT, false);
            }
        }

        private boolean mag() {
            return is(guh) && guh.isAlive() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isVehicle() && !guh.isInWater()
                    && !GuhHooks.isBezig(guh) && guh.emotes.current() == null && guh.getLaunchState() == GuhEntity.LAUNCH_NONE
                    && !(guh.getOwner() instanceof Player owner && owner.distanceTo(guh) > 10);
        }

        @Override
        public boolean canUse() {
            if (!mag() || !guh.onGround() || guh.getRandom().nextInt(PLAFOND_KANS) != 0) {
                return false;
            }
            return zoek(guh.blockPosition());
        }

        /** A wall right next to him and a ceiling 3-5 blocks up (free in between): remember them. */
        boolean zoek(BlockPos voeten) {
            Level level = guh.level();
            for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(guh.getRandom())) {
                BlockPos wand = voeten.relative(d);
                if (!level.getBlockState(wand).isFaceSturdy(level, wand, d.getOpposite())) {
                    continue;
                }
                for (int dy = 3; dy <= 5; dy++) {
                    BlockPos dak = voeten.above(dy);
                    if (level.getBlockState(dak).isFaceSturdy(level, dak, Direction.DOWN)) {
                        boolean vrij = true;
                        for (int k = 1; k < dy; k++) {
                            vrij &= level.getBlockState(voeten.above(k)).getCollisionShape(level, voeten.above(k)).isEmpty();
                        }
                        if (vrij) {
                            muur = d;
                            plafondY = dak.getY();
                            return true;
                        }
                        break;
                    }
                }
            }
            return false;
        }

        /** (tests) start right away at this spot (false: no wall + ceiling here). */
        public boolean begin() {
            if (!zoek(guh.blockPosition())) {
                return false;
            }
            start();
            return true;
        }

        @Override
        public void start() {
            fase = 0;
            tijd = 0;
            kruipen = 1 + guh.getRandom().nextInt(2);
            guh.getNavigation().stop();
            guh.setNoGravity(true);
            guh.getPersistentData().putBoolean(HANGT, true);
            LOPEND.add(guh);
            GuhHooks.zet(guh, VerhaalVlaggen.KLIMT, true);
            guh.setYRot(muur.toYRot());
            guh.setYBodyRot(muur.toYRot());
            guh.setYHeadRot(muur.toYRot());
        }

        @Override
        public boolean canContinueToUse() {
            return fase < 3 && tijd < 400 && is(guh) && guh.isAlive() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isVehicle()
                    && !guh.isInWater() && !(guh.getOwner() instanceof Player owner && owner.distanceTo(guh) > 12);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            tijd++;
            double top = plafondY - guh.getBbHeight() - 0.01;
            switch (fase) {
                case 0 -> {   // up the wall
                    guh.setDeltaMovement(muur.getStepX() * 0.02, 0.14, muur.getStepZ() * 0.02);
                    if (guh.getY() >= top - 0.05) {
                        guh.setPos(guh.getX(), top, guh.getZ());
                        fase = 1;
                        tijd = 0;
                        guh.playSound(ModSounds.GUH_HAPPY.get(), 0.8f, 1.5f);
                    }
                }
                case 1 -> {   // along the ceiling, away from the wall
                    Direction weg = muur.getOpposite();
                    BlockPos volgende = BlockPos.containing(guh.getX() + weg.getStepX() * 0.6, plafondY, guh.getZ() + weg.getStepZ() * 0.6);
                    boolean dak = guh.level().getBlockState(volgende).isFaceSturdy(guh.level(), volgende, Direction.DOWN);
                    if (tijd > kruipen * 16 || !dak) {
                        fase = 2;
                        tijd = 0;
                        guh.setDeltaMovement(Vec3.ZERO);
                    } else {
                        guh.setDeltaMovement(weg.getStepX() * 0.06, 0, weg.getStepZ() * 0.06);
                        guh.setYRot(weg.toYRot());
                        guh.setYBodyRot(weg.toYRot());
                    }
                    guh.setPos(guh.getX(), top, guh.getZ());
                }
                default -> {  // hanging, giggling
                    guh.setDeltaMovement(Vec3.ZERO);
                    guh.setPos(guh.getX(), top, guh.getZ());
                    if (tijd % 40 == 10 && guh.level() instanceof ServerLevel server) {
                        guh.playSound(ModSounds.GUH_AMBIENT.get(), 0.8f, 1.6f);
                        server.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + 0.2, guh.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
                    }
                    if (tijd > 90 + guh.getRandom().nextInt(60)) {
                        fase = 3;
                    }
                }
            }
        }

        @Override
        public void stop() {
            guh.setNoGravity(false);
            guh.getPersistentData().remove(HANGT);
            LOPEND.remove(guh);
            GuhHooks.zet(guh, VerhaalVlaggen.KLIMT, false);
            guh.setDeltaMovement(0, -0.1, 0);
            fase = 0;
        }
    }
}
