package nl.juiced.guhs.feature.torenpeper;

import java.util.EnumSet;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;

/**
 * A lost Rookguh of the lighthouse's questline: a small, thin Rookguh that can't find its way home through the smoke. It
 * belongs to ONE player (its {@link #gids() guide}): it follows only that player, only while they hold the seinlantaarn, and
 * when it has been led ({@link #LEID_TICKS} of following) to within {@link Vuurtoren#THUIS_STRAAL} blocks of a burning lamp it
 * sees the light, eats its fill (it gets round and rosy like any fed Rookguh) and floats home, which counts for its guide
 * ({@link Vuurtoren#thuisgekomen}).
 * Feeding it six kaasknabbels by hand sends it home just the same.
 * <p>
 * It is never saved and never spawned by the world; {@link Vuurtoren#tik} makes the ones a player still needs. Without its
 * guide (gone, in another dimension, done with that step) it drifts off after a few seconds. Like every Rookguh it can't
 * be hurt and hurts nobody. It is drawn with the Rookguh's own model, smaller (client.TorenpeperClient).
 */
public class VerdwaaldeRookguhEntity extends RookguhEntity {
    /** It notices the lantern from this far, and keeps following up to {@link #VOLG_LOS}. */
    public static final double VOLG_BEREIK = 14.0, VOLG_LOS = 26.0;
    /** Ticks between the bites it takes once it is home. */
    public static final int HAP_TICKS = 8;
    /** It only trusts the light after following its guide this long (a lost Rookguh never comes home by itself). */
    public static final int LEID_TICKS = 20;

    @Nullable
    private UUID gids;
    private BlockPos lamp = BlockPos.ZERO;
    private Vec3 anker = Vec3.ZERO;
    private int alleen, eet, vast, geleid;
    private double vorigeAfstand;
    private boolean geteld, volgt;

    public VerdwaaldeRookguhEntity(EntityType<? extends VerdwaaldeRookguhEntity> type, Level level) {
        super(type, level);
    }

    /** Whose it is and which lamp is home; it drifts around where it stands now. */
    public void begin(UUID gids, BlockPos lamp) {
        this.gids = gids;
        this.lamp = lamp.immutable();
        this.anker = position();
    }

    @Nullable
    public UUID gids() {
        return gids;
    }

    public BlockPos lamp() {
        return lamp;
    }

    /** Has it seen the light (it is on its way home)? */
    public boolean isThuis() {
        return isVahoeg();
    }

    /** Was its homecoming counted for its guide (or is there nobody left to count it for)? */
    public boolean isGeteld() {
        return geteld;
    }

    /** Has it followed its guide long enough to trust the light? */
    public boolean isGeleid() {
        return geleid >= LEID_TICKS;
    }

    /** Is it following its guide right now? */
    public boolean volgt() {
        return volgt;
    }

    /** Is it eating at the lamp (the seconds before it floats home)? */
    public boolean eet() {
        return eet > 0;
    }

    /** Its guide, when they are in this level. */
    @Nullable
    public ServerPlayer gidsSpeler() {
        if (gids == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(gids);
        return p != null && p.level() == level && p.isAlive() ? p : null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new VolgGoal());
        this.goalSelector.addGoal(5, new DwaalGoal());
        this.goalSelector.addGoal(7, new KijkGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer gids = gidsSpeler();
        if (isVahoeg()) {
            if (!geteld) {
                geteld = true;
                if (gids != null) {
                    Vuurtoren.thuisgekomen(gids, this);
                }
            }
            return;
        }
        // nobody to guide it any more (gone, another dimension, done with the step): it drifts off into the smoke
        if (gids == null || TorenpeperFeature.VUURTOREN.stap(gids) != 3 || gids.distanceToSqr(this) > 128.0 * 128.0) {
            if (++alleen > 100) {
                poef(level);
                discard();
            }
            return;
        }
        alleen = 0;
        if (tickCount % 10 == 0) {
            // (a sparkle only its guide sees: "this one is yours")
            level.sendParticles(gids, ParticleTypes.END_ROD, false, false, getX(), getY() + getBbHeight() + 0.35, getZ(), 1, 0.15, 0.1, 0.15, 0.0);
        }
        double dx = getX() - (lamp.getX() + 0.5), dz = getZ() - (lamp.getZ() + 0.5);
        if (isGeleid() && dx * dx + dz * dz <= Vuurtoren.THUIS_STRAAL * Vuurtoren.THUIS_STRAAL && Vuurtoren.brandt(level, lamp)) {
            // home: it eats (the last bite is its guide's: a saved Rookguh on their count) and then floats up by itself
            setDeltaMovement(getDeltaMovement().scale(0.6));
            if (eet++ % HAP_TICKS == 0) {
                feed(fed() + 1 >= NEEDED ? gids : null);
            }
        } else {
            eet = 0;
        }
    }

    private void poef(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() * 0.5, getZ(), 14, 0.5, 0.4, 0.5, 0.02);
    }

    /** It lost its guide behind a wall: a puff of smoke, and it is next to them again. */
    private boolean hop(ServerPlayer gids) {
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        RandomSource r = getRandom();
        for (int i = 0; i < 12; i++) {
            double x = gids.getX() + (r.nextDouble() - 0.5) * 5.0, y = gids.getY() + 0.5 + r.nextDouble() * 2.0, z = gids.getZ() + (r.nextDouble() - 0.5) * 5.0;
            if (level.noCollision(this, getType().getDimensions().makeBoundingBox(x, y, z))) {
                poef(level);
                snapTo(x, y, z, getYRot(), 0f);
                setDeltaMovement(Vec3.ZERO);
                poef(level);
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isKnabbel(player.getItemInHand(hand)) || isVahoeg()) {
            return super.mobInteract(player, hand);   // (fed by hand: it goes home that way too)
        }
        if (player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            String key = !p.getUUID().equals(gids) ? "gui.guhs.torenpeper.rookguh.van_ander"
                    : Vuurtoren.houdtLantaarn(p) ? "gui.guhs.torenpeper.rookguh.volgt" : "gui.guhs.torenpeper.rookguh.pak_lantaarn";
            p.sendOverlayMessage(Component.translatable(key, getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() + 0.3f;          // (a smaller Rookguh, a higher little voice)
    }

    // --- AI ----------------------------------------------------------------------------------------------------------------------

    /** Follows its guide while they hold the seinlantaarn; hops to them when it gets stuck or left behind. */
    class VolgGoal extends Goal {
        @Nullable
        private ServerPlayer wie;

        VolgGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean kan(double bereik) {
            if (isVahoeg() || eet > 0) {
                return false;
            }
            wie = gidsSpeler();
            return wie != null && !wie.isSpectator() && Vuurtoren.houdtLantaarn(wie) && distanceToSqr(wie) < bereik * bereik;
        }

        @Override
        public boolean canUse() {
            return kan(VOLG_BEREIK);
        }

        @Override
        public boolean canContinueToUse() {
            return kan(VOLG_LOS);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            volgt = true;
            vast = 0;
            vorigeAfstand = wie == null ? 0 : distanceTo(wie);
        }

        @Override
        public void stop() {
            volgt = false;
            anker = position();                       // (it stays around where its guide left it)
            getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        }

        @Override
        public void tick() {
            if (wie == null) {
                return;
            }
            getLookControl().setLookAt(wie, 30f, 30f);
            geleid++;
            double d = distanceTo(wie);
            if (d > 3.5) {
                getMoveControl().setWantedPosition(wie.getX(), wie.getEyeY() + 0.4, wie.getZ(), 1.3);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.7));
            }
            if (tickCount % 20 == 0) {
                // not getting closer for three seconds, or far behind: hop
                vast = d > 6.0 && d > vorigeAfstand - 0.5 ? vast + 1 : 0;
                vorigeAfstand = d;
                if ((vast >= 3 || d > 18.0) && hop(wie)) {
                    vast = 0;
                }
            }
        }
    }

    /** Lost: it drifts a little around where it is, never far. */
    class DwaalGoal extends Goal {
        DwaalGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isVahoeg() || eet > 0 || getRandom().nextInt(40) != 0) {
                return false;
            }
            MoveControl control = getMoveControl();
            return !control.hasWanted() || new Vec3(control.getWantedX(), control.getWantedY(), control.getWantedZ()).distanceToSqr(position()) < 1.0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            RandomSource r = getRandom();
            getMoveControl().setWantedPosition(anker.x + (r.nextFloat() * 2 - 1) * 5, anker.y + (r.nextFloat() * 2 - 1) * 1.5,
                    anker.z + (r.nextFloat() * 2 - 1) * 5, 0.7);
        }
    }

    /** Looks where it floats. */
    class KijkGoal extends Goal {
        KijkGoal() {
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isVahoeg();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 v = getDeltaMovement();
            if (v.horizontalDistanceSqr() > 1.0E-4) {
                setYRot(-((float) Mth.atan2(v.x, v.z)) * Mth.RAD_TO_DEG);
                yBodyRot = getYRot();
            }
        }
    }
}
