package nl.juiced.guhs.feature.wereldleven;

import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The plushies (2.8, wereldleven): a free-roaming tamed guh that sees a plushie you put down walks up to it and gives it
 * a big hug (the KNUFFELEN emote: hearts), now and then. Plushies you get go into your knuffelkast (Guhdex, Knus tab).
 */
public final class Knuffels {
    /** How far a guh sees a plushie. */
    public static final int ZOEK = 10;
    /** Between two cuddles (ticks): 2-4 minutes. */
    public static final int RUST = 20 * 120;
    static final String KNUFFEL_TOT = "guhs_wereldleven_knuffel_tot";

    static void register() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(6, new KnuffelGoal(guh)));
    }

    /** A player got a plushie (the grijpmachine, picked up): into the knuffelkast. Returns true when it was new. */
    public static boolean ontdek(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem bi)) {
            return false;
        }
        String id = WereldlevenFeature.knuffelId(bi.getBlock());
        if (id == null) {
            return false;
        }
        boolean nieuw = KnusVoortgang.ontdek(player, WereldlevenVoortgang.KNUFFELKAST, id);
        if (nieuw && KnusVoortgang.ontdekt(player, WereldlevenVoortgang.KNUFFELKAST).size() >= WereldlevenFeature.KNUFFEL_IDS.size()) {
            WereldlevenVoortgang.toon(player, "wereldleven_knuffelkast_vol");
        }
        return nieuw;
    }

    /** The nearest plushie within {@link #ZOEK} blocks of a guh, or null. */
    @Nullable
    static BlockPos knuffel(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return null;
        }
        Optional<BlockPos> found = level.getPoiManager().findClosest(t -> t.is(WereldlevenFeature.KNUFFEL_POI.getKey()),
                p -> level.getBlockState(p).is(KnusTags.KNUFFELS) && Dagritme.inGebied(guh, p), guh.blockPosition(), ZOEK, PoiManager.Occupancy.ANY);
        return found.orElse(null);
    }

    /** Walks to a plushie and hugs it. */
    static class KnuffelGoal extends Goal {
        private final GuhEntity guh;
        private int cooldown;
        private int ticks;
        private boolean klaar;
        @Nullable
        private BlockPos knuffel;

        KnuffelGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean mag() {
            return guh.isTame() && guh.mayWander() && !guh.isOrderedToSit() && guh.getTarget() == null && !guh.isPassenger() && !guh.isVehicle()
                    && !guh.isLeashed() && !Dagritme.slaapt(guh) && guh.getHiddenBy() == null;
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0) {
                return false;
            }
            cooldown = 60 + guh.getRandom().nextInt(60);
            if (!mag() || GuhHooks.isBezig(guh) || guh.emotes.current() != null || !Dagritme.magInTest(guh)
                    || guh.level().getGameTime() < guh.getPersistentData().getLong(KNUFFEL_TOT)) {
                return false;
            }
            knuffel = knuffel(guh);
            return knuffel != null && (knuffel.distSqr(guh.blockPosition()) < 5 * 5 || guh.getRandom().nextInt(2) == 0);
        }

        @Override
        public boolean canContinueToUse() {
            return !klaar && knuffel != null && ticks < 20 * 20 && mag() && guh.level().getBlockState(knuffel).is(KnusTags.KNUFFELS);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ticks = 0;
            klaar = false;
            if (knuffel != null) {
                guh.getNavigation().moveTo(knuffel.getX() + 0.5, knuffel.getY(), knuffel.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void tick() {
            if (knuffel == null) {
                return;
            }
            ticks++;
            guh.getLookControl().setLookAt(knuffel.getX() + 0.5, knuffel.getY() + 0.4, knuffel.getZ() + 0.5, 30f, 30f);
            double dx = guh.getX() - (knuffel.getX() + 0.5), dz = guh.getZ() - (knuffel.getZ() + 0.5);
            double reach = 1.1 + guh.getBbWidth() * 0.5;
            if (dx * dx + dz * dz <= reach * reach) {
                klaar = knuffel(guh, knuffel) || ticks > 20 * 5;
            } else if (ticks % 20 == 0 && guh.getNavigation().isDone()) {
                guh.getNavigation().moveTo(knuffel.getX() + 0.5, knuffel.getY(), knuffel.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void stop() {
            guh.getNavigation().stop();
            knuffel = null;
        }
    }

    /** The hug itself: KNUFFELEN, hearts, and it counts for the owner. */
    public static boolean knuffel(GuhEntity guh, BlockPos knuffel) {
        guh.getNavigation().stop();
        guh.getLookControl().setLookAt(knuffel.getX() + 0.5, knuffel.getY() + 0.4, knuffel.getZ() + 0.5, 90f, 90f);
        if (!guh.emotes.start(Emote.KNUFFELEN, false, GuhEmotes.Source.SELF)) {
            return false;
        }
        guh.getPersistentData().putLong(KNUFFEL_TOT, guh.level().getGameTime() + RUST + guh.getRandom().nextInt(RUST));
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), knuffel.getX() + 0.5, knuffel.getY() + 0.9, knuffel.getZ() + 0.5, 4,
                    0.25, 0.15, 0.25, 0.01);
        }
        if (guh.getOwner() instanceof LivingEntity owner && owner instanceof ServerPlayer sp && sp.level() == guh.level() && sp.distanceTo(guh) < 32) {
            KnusVoortgang.tel(sp, WereldlevenVoortgang.KNUFFELS, 1);
            GuhAdvancements.grant(sp, "wereldleven_knuffel");
        }
        String knuffelId = WereldlevenFeature.knuffelId(guh.level().getBlockState(knuffel).getBlock());   // 2.10: the plush it hugged
        if (knuffelId != null) {
            nl.juiced.guhs.feature.band.Band.moment(guh, guh.getOwner() instanceof ServerPlayer sp && sp.level() == guh.level() && sp.distanceTo(guh) < 32 ? sp : null,
                    nl.juiced.guhs.feature.band.Moment.KNUFFEL, knuffelId);
        }
        return true;
    }

    /** Is this block a plushie? */
    public static boolean isKnuffel(Block block) {
        return WereldlevenFeature.knuffelId(block) != null;
    }

    private Knuffels() {
    }
}
