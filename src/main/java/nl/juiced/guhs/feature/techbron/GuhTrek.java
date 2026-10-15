package nl.juiced.guhs.feature.techbron;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * How a Knuffelgenerator and a Disco-dynamo get their guhs: "they come by themselves". Once per second the source looks
 * for tamed guhs within {@link TechbronGetallen#BEREIK} blocks ({@link Groep#kijk}), gives each a spot of its own and calls
 * it ({@link #roep}: a claim in the guh's persistent data that lasts two seconds, so it ends by itself when the source is
 * gone or unloaded). A called guh walks to its spot ({@link Doel}, added to every guh through {@link GuhHooks#doelen}) and,
 * once there, does the source's emote for as long as it is called (lying asleep on the cushion, dancing on the floor).
 * Only guhs on the source that do the emote count.
 * <p>
 * Which guhs come: tamed guhs ({@link Band#isBandGuh}) that are free. Not one that lives in a Guhhuisje (it has its
 * chores and its own bed), does a chore or plays, is busy with another activity ({@link GuhHooks#isBezig}), does an emote
 * of its own, is ridden or carried, or fights. A guh that was told to sit only counts when it sits on the source. A guh
 * that follows its owner only joins while the owner is near the source ({@link TechbronGetallen#EIGENAAR_BEREIK}) and
 * leaves with them; a guh that was told to stay ("niet rondlopen") stays on the source when you walk away: that is how a
 * factory keeps running.
 */
public final class GuhTrek {
    /** Persistent data of a called guh: the source's kern, its spot, until which game time the call holds. */
    private static final String BRON = "guhs_techbron_bron", X = "guhs_techbron_x", Y = "guhs_techbron_y", Z = "guhs_techbron_z",
            TOT = "guhs_techbron_tot";
    /** ... and: the emote it does now was started by its source (so only that one is stopped again). */
    private static final String EMOTE = "guhs_techbron_emote";
    /** A call lasts this long (the source renews it every {@link TechbronGetallen#KIJK} ticks). */
    private static final int ROEP_TICKS = 2 * TechbronGetallen.KIJK + 5;
    /** A guh this close to its spot (horizontally) has arrived. */
    private static final double AANKOMST = 0.75;
    /** A guh that has been on the source this many looks without reaching its own spot settles where it is. */
    private static final int WACHT = 4;
    /**
     * A guh that takes part keeps counting while it is within this many blocks around the source's zone: a nudge of a guh
     * that walks past does not make what the source gives flicker (and with it a whole net start and stop).
     */
    private static final double SPELING = 0.75;

    private GuhTrek() {
    }

    /** Feature.register: the walking goal of every guh, and the tidy-up of calls that ended. */
    static void register() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(6, new Doel(guh)));   // (after FollowOwnerGoal: a following guh leaves with its owner)
        GuhHooks.tick(GuhTrek::ruimOp);
    }

    // =====================================================================================================================
    // the call
    // =====================================================================================================================

    /** The spot a guh is called to right now, or null. */
    @Nullable
    public static Vec3 plek(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        if (data.getLongOr(TOT, 0L) <= guh.level().getGameTime()) {
            return null;
        }
        return new Vec3(data.getDoubleOr(X, guh.getX()), data.getDoubleOr(Y, guh.getY()), data.getDoubleOr(Z, guh.getZ()));
    }

    /** The kern of the source that calls this guh right now, or null. */
    @Nullable
    public static BlockPos bron(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        return data.getLongOr(TOT, 0L) > guh.level().getGameTime() && data.contains(BRON) ? BlockPos.of(data.getLongOr(BRON, 0L)) : null;
    }

    /** Calls (or keeps calling) a guh to a spot of this source; other activities leave it alone meanwhile. */
    public static void roep(GuhEntity guh, BlockPos bron, Vec3 plek) {
        CompoundTag data = guh.getPersistentData();
        data.putLong(BRON, bron.asLong());
        data.putDouble(X, plek.x);
        data.putDouble(Y, plek.y);
        data.putDouble(Z, plek.z);
        data.putLong(TOT, guh.level().getGameTime() + ROEP_TICKS);
        GuhHooks.bezig(guh, ROEP_TICKS);
    }

    /** Is the emote the guh does now one a source started? */
    public static boolean doetMee(GuhEntity guh, Emote emote) {
        return guh.getPersistentData().getBooleanOr(EMOTE, false) && guh.emotes.current() == emote;
    }

    /** Ends the call: the guh stops the emote its source started and is free again. */
    public static void laatLos(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        if (data.getBooleanOr(EMOTE, false) && guh.emotes.current() != null) {
            guh.emotes.stop();
        }
        boolean geroepen = data.contains(TOT);
        for (String key : List.of(BRON, X, Y, Z, TOT, EMOTE)) {
            data.remove(key);
        }
        if (geroepen) {
            GuhHooks.bezig(guh, 0);
        }
    }

    /** (every guh, every tick, server) A call that ended without a goodbye (the source was broken or unloaded): tidy up. */
    private static void ruimOp(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % TechbronGetallen.KIJK != 0) {
            return;
        }
        CompoundTag data = guh.getPersistentData();
        if (data.contains(TOT) && data.getLongOr(TOT, 0L) <= guh.level().getGameTime()) {
            laatLos(guh);
        }
    }

    /** May this guh come to the source with its kern here at all (see the class text)? */
    static boolean magMee(GuhEntity guh, BlockPos bron, AABB vloer, Vec3 midden, Emote emote) {
        if (!guh.isAlive() || !Band.isBandGuh(guh) || Huisjes.isBewoner(guh) || Huisjes.isBinnen(guh) || guh.getHiddenBy() != null
                || guh.isPassenger() || guh.isVehicle() || guh.isNoAi() || guh.getTarget() != null || guh.isInWater() || guh.isInLava()
                || BandVlaggen.heeft(guh, BandVlaggen.KLUSJE) || BandVlaggen.heeft(guh, BandVlaggen.SPEELT)
                || BandVlaggen.heeft(guh, BandVlaggen.GUHKAMER_GAST)) {
            return false;
        }
        BlockPos van = bron(guh);
        boolean vanOns = bron.equals(van);
        if (van != null && !vanOns) {
            return false;   // another source has it
        }
        if (!vanOns && GuhHooks.isBezig(guh)) {
            return false;   // another activity has it (theekransje, feestbuffet...)
        }
        if (guh.emotes.current() != null && !doetMee(guh, emote)) {
            return false;   // an emote of its own (its owner asked, it waves at somebody): later
        }
        if (guh.isOrderedToSit()) {
            return vloer.contains(guh.position());  // told to sit: it only joins when it sits on the source itself
        }
        if (guh.mayWander()) {
            // it follows its owner: only while they are here (else it would be torn between the two)
            return guh.getOwner() instanceof Player owner && owner.level() == guh.level() && !owner.isSpectator()
                    && owner.position().distanceToSqr(midden) <= (double) TechbronGetallen.EIGENAAR_BEREIK * TechbronGetallen.EIGENAAR_BEREIK;
        }
        return true;
    }

    // =====================================================================================================================
    // the guhs of one source
    // =====================================================================================================================

    /** The guhs of one source: who has which spot, and who really takes part right now. */
    public static final class Groep {
        private final int max;
        private final Emote emote;
        /** Guh -> the index of its spot. */
        private final Map<UUID, Integer> leden = new HashMap<>();
        /** Guh -> how many looks it has been on the source without reaching its own spot. */
        private final Map<UUID, Integer> wacht = new HashMap<>();
        private final List<GuhEntity> bezig = new ArrayList<>();

        public Groep(int max, Emote emote) {
            this.max = max;
            this.emote = emote;
        }

        /** The guhs that took part at the last look (on the source, doing the emote). */
        public List<GuhEntity> bezig() {
            return bezig;
        }

        /** How many guhs have a spot (also the ones still on their way). */
        public int geroepen() {
            return leden.size();
        }

        /**
         * One look (once per second): calls up to {@code max} free guhs around the source to their spots, lets the ones
         * that arrived do the emote, and returns how many take part.
         *
         * @param zone    the box a guh must be in to count (the source's blocks, maybe a little around them, and some air above)
         * @param vloer   the source's own blocks and the air above them (a guh that was told to sit must sit in here to join)
         * @param plekken the spots, at least {@code max} of them
         * @param yaw     which way an arrived guh looks, or NaN for any way
         */
        public int kijk(ServerLevel level, BlockPos bron, AABB zone, AABB vloer, List<Vec3> plekken, float yaw) {
            Vec3 midden = zone.getCenter();
            double r = TechbronGetallen.BEREIK;
            List<GuhEntity> buurt = level.getEntitiesOfClass(GuhEntity.class, zone.inflate(r, r, r), g -> magMee(g, bron, vloer, midden, emote));
            buurt.sort(Comparator.comparingDouble(g -> g.position().distanceToSqr(midden)));
            Map<UUID, Integer> nieuw = new HashMap<>();
            boolean[] bezet = new boolean[max];
            List<GuhEntity> nu = new ArrayList<>();
            for (GuhEntity guh : buurt) {             // who was here keeps its spot
                Integer plek = leden.get(guh.getUUID());
                if (plek != null && plek < max && !bezet[plek]) {
                    bezet[plek] = true;
                    nieuw.put(guh.getUUID(), plek);
                    nu.add(guh);
                }
            }
            for (GuhEntity guh : buurt) {             // the nearest newcomers get the free spots
                if (nieuw.size() >= max) {
                    break;
                }
                if (!nieuw.containsKey(guh.getUUID())) {
                    int vrij = 0;
                    while (bezet[vrij]) {
                        vrij++;
                    }
                    bezet[vrij] = true;
                    nieuw.put(guh.getUUID(), vrij);
                    nu.add(guh);
                }
            }
            for (UUID weg : leden.keySet()) {         // who may not come any more is free again
                if (!nieuw.containsKey(weg)) {
                    wacht.remove(weg);
                    Entity e = level.getEntity(weg);
                    if (e instanceof GuhEntity guh && bron.equals(GuhTrek.bron(guh))) {
                        GuhTrek.laatLos(guh);
                    }
                }
            }
            leden.clear();
            leden.putAll(nieuw);
            bezig.clear();
            for (GuhEntity guh : nu) {
                Vec3 plek = plekken.get(leden.get(guh.getUUID()));
                roep(guh, bron, plek);
                boolean erop = zone.contains(guh.position());
                if (doetMee(guh, emote)) {
                    if (erop || zone.inflate(SPELING, 0, SPELING).contains(guh.position())) {
                        bezig.add(guh);               // (nudged a little by a guh that walked past: it still takes part)
                    } else {
                        guh.emotes.stop();            // shoved off the source: it walks back
                        guh.getPersistentData().remove(EMOTE);
                    }
                    continue;
                }
                guh.getPersistentData().remove(EMOTE);
                boolean aangekomen;
                if (guh.isOrderedToSit()) {
                    aangekomen = erop;
                } else {
                    double dx = guh.getX() - plek.x, dz = guh.getZ() - plek.z;
                    aangekomen = dx * dx + dz * dz <= AANKOMST * AANKOMST && Math.abs(guh.getY() - plek.y) < 1.5;
                    if (!aangekomen && erop) {
                        // on the source, but it does not get to its own spot (others lie or sit in the way): this will do
                        aangekomen = wacht.merge(guh.getUUID(), 1, Integer::sum) >= WACHT;
                    } else if (!erop) {
                        wacht.remove(guh.getUUID());
                    }
                }
                if (aangekomen && GuhEmotes.canStart(guh) && guh.emotes.start(emote, true, GuhEmotes.Source.SELF)) {
                    wacht.remove(guh.getUUID());
                    guh.getPersistentData().putBoolean(EMOTE, true);
                    if (!Float.isNaN(yaw) && !guh.isOrderedToSit()) {
                        guh.setYRot(yaw);
                        guh.yBodyRot = yaw;
                        guh.yHeadRot = yaw;
                    }
                    if (erop) {
                        bezig.add(guh);
                    }
                }
            }
            return bezig.size();
        }

        /** The source stops (no disc, broken): everybody is free again. */
        public void laatLos(ServerLevel level, BlockPos bron) {
            for (UUID lid : leden.keySet()) {
                if (level.getEntity(lid) instanceof GuhEntity guh && bron.equals(GuhTrek.bron(guh))) {
                    GuhTrek.laatLos(guh);
                }
            }
            leden.clear();
            wacht.clear();
            bezig.clear();
        }
    }

    // =====================================================================================================================
    // the walk
    // =====================================================================================================================

    /** A called guh walks to its spot (and stands there until its source lets it do the emote). */
    static final class Doel extends Goal {
        private final GuhEntity guh;
        private int opnieuw;

        Doel(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private Vec3 doel() {
            if (!guh.isTame() || guh.isOrderedToSit() || guh.emotes.current() != null || guh.isVehicle() || guh.isPassenger()) {
                return null;
            }
            Vec3 plek = plek(guh);
            if (plek == null) {
                return null;
            }
            double dx = guh.getX() - plek.x, dz = guh.getZ() - plek.z;
            return dx * dx + dz * dz <= AANKOMST * AANKOMST * 0.6 && Math.abs(guh.getY() - plek.y) < 1.5 ? null : plek;
        }

        @Override
        public boolean canUse() {
            return doel() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel() != null;
        }

        @Override
        public void start() {
            opnieuw = 0;
        }

        @Override
        public void tick() {
            Vec3 plek = doel();
            if (plek == null) {
                return;
            }
            double dx = guh.getX() - plek.x, dz = guh.getZ() - plek.z;
            if (dx * dx + dz * dz < 1.5 * 1.5 && Math.abs(guh.getY() - plek.y) < 0.6) {
                // the last steps: straight to the exact spot (a path only goes to the middle of a block)
                guh.getNavigation().stop();
                guh.getMoveControl().setWantedPosition(plek.x, plek.y, plek.z, 0.8);
            } else if (--opnieuw <= 0) {
                opnieuw = adjustedTickDelay(20);
                guh.getNavigation().moveTo(plek.x, plek.y, plek.z, 1.0);
            }
        }

        @Override
        public void stop() {
            guh.getNavigation().stop();
        }
    }
}
