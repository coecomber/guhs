package nl.juiced.guhs.feature.piep;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Verstoppertje: now and then a tamed muisje (its owner close by) sneaks off and hides in a nearby hiding place
 * (block tag {@code guhs:piep/verstopplekken}: flower pots, baskets, barrels, decorated pots...) or, when there is none,
 * in the grass a few blocks away. Hidden it is invisible and peeps now and then (little piepje particles). Right-click
 * that spot (or the muisje) to find it: hearts, a spin, and the counter "gevonden" (Guhdex). Nobody found it after
 * {@link #MAX_VERSTOPT} ticks? It comes out by itself, a bit proud.
 */
public final class Verstoppertje {
    /** Chance per check (every {@link #CHECK} ticks) that a muisje wants to play. */
    public static final int KANS = 18, CHECK = 200;
    /** How long it stays hidden at most (3 minutes). */
    public static final int MAX_VERSTOPT = 3600;
    /** How far it looks for a hiding place, and how close its owner must be. */
    public static final int ZOEK = 8, EIGENAAR = 16;

    private final PieppiepmuisjeEntity muis;
    /** Where it goes to hide / hides (null: not playing). */
    @Nullable
    private BlockPos plek;
    private boolean inBlok;
    private long verstoptSinds, volgendeCheck;
    /** (Tests) always play at the next check. */
    boolean forceer;

    Verstoppertje(PieppiepmuisjeEntity muis) {
        this.muis = muis;
    }

    @Nullable
    public BlockPos plek() {
        return muis.isVerstopt() ? plek : null;
    }

    Goal goal() {
        return new Goal() {
            private int ticks;

            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
            }

            @Override
            public boolean canUse() {
                if (muis.isVerstopt()) {
                    return true;                                        // (keeps the other goals quiet while it hides)
                }
                long now = muis.level().getGameTime();
                if (now < volgendeCheck || !muis.isTame() || muis.isOrderedToSit() || !muis.aan(PiepInstelling.VERSTOPPEN) || muis.isLeashed() || muis.isPassenger()) {
                    return false;
                }
                volgendeCheck = now + CHECK;
                LivingEntity owner = muis.getOwner();
                if (!(owner instanceof ServerPlayer) || owner.distanceToSqr(muis) > EIGENAAR * EIGENAAR) {
                    return false;
                }
                if (!forceer && muis.getRandom().nextInt(KANS) != 0) {
                    return false;
                }
                forceer = false;
                return kiesPlek();
            }

            @Override
            public boolean canContinueToUse() {
                return muis.isVerstopt() || plek != null && ticks < 240;
            }

            @Override
            public void start() {
                ticks = 0;
                if (!muis.isVerstopt() && plek != null) {
                    muis.getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.4);
                }
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void tick() {
                if (muis.isVerstopt() || plek == null) {
                    return;
                }
                ticks++;
                double d = muis.distanceToSqr(plek.getX() + 0.5, plek.getY() + (inBlok ? 0.0 : 0.5), plek.getZ() + 0.5);
                if (d < 1.6 * 1.6 || ticks >= 200) {
                    verstop();
                } else if (ticks % 20 == 0) {
                    muis.getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.4);
                }
            }

            @Override
            public void stop() {
                if (!muis.isVerstopt()) {
                    plek = null;
                }
            }
        };
    }

    /** Picks a hiding place: a hiding block nearby, else a grassy spot a few blocks off. */
    boolean kiesPlek() {
        ServerLevel level = (ServerLevel) muis.level();
        BlockPos base = muis.blockPosition();
        List<BlockPos> blokken = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-ZOEK, -3, -ZOEK), base.offset(ZOEK, 3, ZOEK))) {
            BlockState s = level.getBlockState(p);
            if (s.is(PiepFeature.VERSTOPPLEKKEN)) {
                blokken.add(p.immutable());
            }
        }
        if (!blokken.isEmpty()) {
            plek = blokken.get(muis.getRandom().nextInt(blokken.size()));
            inBlok = true;
            return true;
        }
        for (int i = 0; i < 12; i++) {
            BlockPos p = base.offset(muis.getRandom().nextInt(11) - 5, 0, muis.getRandom().nextInt(11) - 5);
            p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
            if (Math.abs(p.getY() - base.getY()) <= 3 && p.distSqr(base) >= 9 && level.getBlockState(p.below()).isSolid()
                    && level.getFluidState(p).isEmpty()) {
                plek = p;
                inBlok = false;
                return true;
            }
        }
        return false;
    }

    /** Hides now (at its spot). */
    void verstop() {
        if (plek == null) {
            return;
        }
        Vec3 at = inBlok ? new Vec3(plek.getX() + 0.5, plek.getY() + 0.1, plek.getZ() + 0.5) : new Vec3(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
        muis.snapTo(at.x, at.y, at.z, muis.getYRot(), 0);
        muis.setVerstopt(true);
        verstoptSinds = muis.level().getGameTime();
        if (muis.getOwner() instanceof ServerPlayer owner) {
            owner.sendOverlayMessage(Component.translatable("gui.guhs.piep.verstoppertje", muis.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        muis.playSound(PiepFeature.PIEP.get(), 0.9f, 1.4f);
    }

    /** (Tests) hides at this spot right away. */
    void verstopBij(BlockPos pos, boolean blok) {
        plek = pos.immutable();
        inBlok = blok;
        verstop();
    }

    /** Found by this player! */
    void gevonden(ServerPlayer player) {
        if (!muis.isVerstopt()) {
            return;
        }
        uit();
        ServerLevel level = (ServerLevel) muis.level();
        level.sendParticles(ParticleTypes.HEART, muis.getX(), muis.getY() + 0.5, muis.getZ(), 4, 0.25, 0.15, 0.25, 0);
        muis.triggerAnim("actie", "blij");
        level.playSound(null, muis.blockPosition(), PiepFeature.PIEP.get(), SoundSource.NEUTRAL, 1f, 1.5f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.piep.gevonden").withStyle(ChatFormatting.LIGHT_PURPLE));
        PiepVoortgang.tel(player, PiepVoortgang.GEVONDEN, 1);
    }

    /** Comes out of hiding (found, or bored). */
    void uit() {
        muis.setVerstopt(false);
        if (plek != null && inBlok) {
            // out of the pot, onto the ground next to it
            BlockPos naast = plek;
            for (BlockPos p : List.of(plek.north(), plek.south(), plek.east(), plek.west(), plek.above())) {
                if (muis.level().getBlockState(p).getCollisionShape(muis.level(), p).isEmpty()) {
                    naast = p;
                    break;
                }
            }
            muis.snapTo(naast.getX() + 0.5, naast.getY(), naast.getZ() + 0.5, muis.getYRot(), 0);
        }
        plek = null;
        volgendeCheck = muis.level().getGameTime() + CHECK * 3;
    }

    void tick() {
        if (!muis.isVerstopt()) {
            return;
        }
        ServerLevel level = (ServerLevel) muis.level();
        long t = level.getGameTime() - verstoptSinds;
        if (plek == null || t > MAX_VERSTOPT || muis.getOwner() == null) {
            if (muis.getOwner() instanceof ServerPlayer owner && owner.distanceToSqr(muis) < 48 * 48) {
                owner.sendOverlayMessage(Component.translatable("gui.guhs.piep.niet_gevonden").withStyle(ChatFormatting.GRAY));
            }
            uit();
            return;
        }
        muis.setDeltaMovement(Vec3.ZERO);
        if ((t + muis.getId()) % 50 == 0) {
            level.sendParticles(PiepFeature.PIEPJE.get(), muis.getX(), muis.getY() + 0.6, muis.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            level.playSound(null, muis.blockPosition(), PiepFeature.PIEP.get(), SoundSource.NEUTRAL, 0.55f,
                    1.2f + muis.getRandom().nextFloat() * 0.4f);
        }
    }

    /** Is a hidden muisje at (or in) this clicked block? */
    public boolean bij(BlockPos clicked) {
        BlockPos p = plek();
        return p != null && (p.equals(clicked) || p.below().equals(clicked) || muis.distanceToSqr(clicked.getCenter()) < 1.5 * 1.5);
    }

    void save(CompoundTag tag) {
        if (muis.isVerstopt() && plek != null) {
            tag.store("VerstopPlek", BlockPos.CODEC, plek);
            tag.putBoolean("VerstopInBlok", inBlok);
            tag.putLong("VerstoptSinds", verstoptSinds);
        }
    }

    void load(CompoundTag tag) {
        if (tag.contains("VerstopPlek")) {
            plek = (tag).read("VerstopPlek", BlockPos.CODEC).orElse(null);
            inBlok = tag.getBooleanOr("VerstopInBlok", false);
            verstoptSinds = tag.getLongOr("VerstoptSinds", 0L);
            if (plek != null) {
                muis.setVerstopt(true);
            }
        }
    }
}
