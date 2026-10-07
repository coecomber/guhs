package nl.juiced.guhs.feature.ring;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Doelen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-kern): Sam-guh, the loyal gardener guh who walks the whole trip with you (DESIGN_130 4, "Cast").
 * <ul>
 *   <li>Everybody has their OWN Sam-guh: a guh of the variant SAM_GUH that only its player's game knows about
 *       ({@link Zicht#alleenVoor}), made when it is needed and gone when it isn't (never saved for long: a Sam whose player
 *       is away is removed). He walks along from the moment chapter 1 calls {@link #roep} (and in any case from chapter 2
 *       on) in the Guhmensie and the Barbecuether, hops to you when you are far, and comes along when you are put back on
 *       a rest point.</li>
 *   <li>A click: he says what to do now and which way to walk ({@link Ring#vertelDoel}); he hands the ring back when you
 *       lost it; when the ring is heavy he offers his back ("ik kan de ring niet dragen, maar wel jou, njeg"):
 *       {@link #draag}.</li>
 *   <li>At a rest point he cooks ({@link #kook}): one Stoofpotje per rest point per day.</li>
 *   <li>After the story a click tames him for good: a real, own Sam-guh, once per player ({@code VerhaalGuhs.tem}).</li>
 * </ul>
 * Chapters script him with {@link #roep}, {@link #stuurWeg}, {@link #kom}, {@link #draag(ServerPlayer, List, Consumer)},
 * {@link #wacht} and {@link #BIJ_KLIK} (a scene's own answer to a click); a Sam-guh they place themselves as a story copy
 * ({@code VerhaalGuhs.maakKopie(level, VerhaalGuh.SAM_GUH, plek)}, e.g. at the party before he joins) gets its clicks
 * through {@link #BIJ_KLIK} too.
 */
public final class Sam {
    /** Player saved data: Sam walks along (set by {@link #roep}). */
    public static final String MEE = "guhs_ring_sam_mee";
    /** The entity tag and persistent-data key (the owner's UUID) of a companion Sam-guh. */
    public static final String TAG = "guhs_ring_sam";
    /** Persistent data of the companion: what he is doing (see the constants) and until when. */
    static final String DOET = "guhs_ring_sam_doet", TOT = "guhs_ring_sam_tot", VUUR = "guhs_ring_sam_vuur";
    public static final String VOLGT = "", KOOKT = "kookt", DRAAGT = "draagt", WACHT = "wacht";
    /** He starts walking when you are this far, hops to you when you are this far (blocks). */
    public static final double VOLG_AFSTAND = 4.5, SPRING_AFSTAND = 24;
    public static final int KOOK_TICKS = 100;
    /** From this weight of the ring on, a click on Sam means "carry me". */
    public static final double DRAAG_VANAF = 0.5;
    private static final String GEKOOKT = "guhs_ring_gekookt";

    /** What a click on a Sam-guh (the companion or a story copy) does in a scene; the first that isn't PASS wins. */
    public static final List<VerhaalGuhs.Klik> BIJ_KLIK = new CopyOnWriteArrayList<>();

    /** A route Sam carries his player along, and what happens at the end. */
    private record Rit(List<Vec3> route, int stap, @Nullable Consumer<ServerPlayer> daarna) {
    }

    private static final Map<UUID, UUID> SAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Rit> RITTEN = new ConcurrentHashMap<>();

    // =====================================================================================================================
    // for the chapters
    // =====================================================================================================================

    /** Sam-guh joins this player (chapter 1); he appears next to them at once when they are in a world of the story. */
    public static void roep(ServerPlayer p) {
        GuhQuests.saved(p).putBoolean(MEE, true);
        tick(p);
    }

    /** Sam-guh goes home (the companion disappears, he does not walk along any more). */
    public static void stuurWeg(ServerPlayer p) {
        GuhQuests.saved(p).remove(MEE);
        weg(p);
    }

    /** Does Sam-guh walk along with this player right now? */
    public static boolean looptMee(ServerPlayer p) {
        boolean mee = GuhQuests.saved(p).getBooleanOr(MEE, false);
        if (Ring.opReis(p)) {
            return mee || Ring.hoofdstuk(p) >= 2;
        }
        // after the feast he stays at your side until you take him home for good
        return mee && Ring.klaar(p) && VerhaalGuhs.magTemmen(p, VerhaalGuh.SAM_GUH);
    }

    /** This player's Sam-guh when he is in their world right now, else null. */
    @Nullable
    public static GuhEntity van(ServerPlayer p) {
        UUID id = SAMS.get(p.getUUID());
        return id != null && p.level().getEntity(id) instanceof GuhEntity guh && guh.isAlive() ? guh : null;
    }

    /** Hop: Sam-guh stands next to his player (after a rest point poof, a teleport, a scene). */
    public static void kom(ServerPlayer p) {
        GuhEntity sam = van(p);
        if (sam != null && !sam.isVehicle()) {
            Vec3 naast = naast(p);
            sam.teleportTo(naast.x, naast.y, naast.z);
            sam.getNavigation().stop();
            zetDoet(sam, VOLGT, 0);
        }
    }

    /** Sam-guh waits where he stands for this many ticks (a scene, a puzzle room), then follows again. */
    public static void wacht(ServerPlayer p, int ticks) {
        GuhEntity sam = van(p);
        if (sam != null) {
            sam.getNavigation().stop();
            zetDoet(sam, WACHT, ticks);
        }
    }

    /** Is this guh a companion Sam-guh (of whoever)? */
    public static boolean isSam(Entity e) {
        return e.entityTags().contains(TAG);
    }

    /** What the companion is doing: {@link #VOLGT}, {@link #KOOKT}, {@link #DRAAGT} or {@link #WACHT}. */
    public static String doet(GuhEntity sam) {
        CompoundTag data = sam.getPersistentData();
        String doet = data.getStringOr(DOET, VOLGT);
        if (doet.equals(WACHT) && data.getLongOr(TOT, 0L) <= sam.level().getGameTime()) {   // (cooking is ended by guhTick, a ride by its goal)
            data.remove(DOET);
            return VOLGT;
        }
        return doet;
    }

    private static void zetDoet(GuhEntity sam, String doet, int ticks) {
        if (doet.isEmpty()) {
            sam.getPersistentData().remove(DOET);
        } else {
            sam.getPersistentData().putString(DOET, doet);
            sam.getPersistentData().putLong(TOT, sam.level().getGameTime() + ticks);
        }
    }

    // --- carrying ------------------------------------------------------------------------------------------------------------

    /** "Ik kan de ring niet dragen, maar wel jou": the player gets on Sam's back and he walks to the next goal of the story. */
    public static boolean draag(ServerPlayer p) {
        return draag(p, List.of(), null);
    }

    /**
     * Sam-guh carries the player along this route (world positions; empty: towards the story's next goal). The ring weighs
     * nothing while you are carried. It ends when the player gets off or at the end of the route; then {@code daarna}
     * runs (only when the end was reached). False: no Sam here, or the player can't get on.
     */
    public static boolean draag(ServerPlayer p, List<Vec3> route, @Nullable Consumer<ServerPlayer> daarna) {
        GuhEntity sam = van(p);
        if (sam == null) {
            tick(p);
            sam = van(p);
        }
        if (sam == null || !nl.juiced.guhs.feature.verhaal.Duwtje.mag(p)) {
            return false;
        }
        if (sam.distanceToSqr(p) > 36) {
            kom(p);
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        if (!p.startRiding(sam, true, true)) {
            return false;
        }
        RITTEN.put(p.getUUID(), new Rit(List.copyOf(route), 0, daarna));
        sam.getPersistentData().putString(DOET, DRAAGT);
        GuhQuests.say(p, sam, "quest.guhs.ring.sam.draag");
        Ring.behaald(p, "ring_gedragen");
        return true;
    }

    // --- cooking -------------------------------------------------------------------------------------------------------------

    /**
     * Sam-guh cooks at this rest fire: he trots to it, stirs for {@link #KOOK_TICKS} ticks and hands his player a Stoofpotje
     * (once per rest point per day; another time he only says the stew still has to simmer) and says what to do next.
     */
    public static void kook(ServerPlayer p, BlockPos vuur) {
        GuhEntity sam = van(p);
        if (sam == null || !doet(sam).equals(VOLGT)) {
            return;
        }
        zetDoet(sam, KOOKT, KOOK_TICKS);
        sam.getPersistentData().putLong(VUUR, vuur.asLong());
        sam.getNavigation().moveTo(vuur.getX() + 0.5, vuur.getY(), vuur.getZ() + 0.5, 1.1);
    }

    /** Did Sam already cook at this fire for this player today? */
    static boolean alGekookt(ServerPlayer p, BlockPos vuur) {
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(GEKOOKT);
        return t.getLongOr(Long.toString(vuur.asLong()), -1L) == dag(p);
    }

    private static long dag(ServerPlayer p) {
        return nl.juiced.guhs.feature.band.Band.dag(p.level().getServer());
    }

    private static void klaarMetKoken(ServerPlayer p, GuhEntity sam) {
        BlockPos vuur = BlockPos.of(sam.getPersistentData().getLongOr(VUUR, 0L));
        ServerLevel level = p.level();
        if (alGekookt(p, vuur)) {
            GuhQuests.say(p, sam, "quest.guhs.ring.sam.pruttelt");
        } else {
            CompoundTag saved = GuhQuests.saved(p);
            CompoundTag t = saved.getCompoundOrEmpty(GEKOOKT);
            if (t.keySet().size() > 48) {
                t = new CompoundTag();   // (old fires: forgotten, a new day came long ago)
            }
            t.putLong(Long.toString(vuur.asLong()), dag(p));
            saved.put(GEKOOKT, t);
            Minigames.give(p, new ItemStack(RingFeature.STOOFPOTJE.get()));
            GuhQuests.say(p, sam, "quest.guhs.ring.sam.eten." + level.getRandom().nextInt(3));
            level.playSound(null, sam, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
            Ring.behaald(p, "ring_stoofpotje");
        }
        Ring.vertelDoel(p, sam);
    }

    // =====================================================================================================================
    // the companion itself
    // =====================================================================================================================

    /** (every second, per player) Sam-guh is there when he should be, near enough, and gone when he shouldn't. */
    static void tick(ServerPlayer p) {
        boolean wil = p.isAlive() && !p.isSpectator() && Ring.verhaalWereld(p.level()) && looptMee(p);
        GuhEntity sam = van(p);
        if (!wil) {
            if (sam != null || SAMS.containsKey(p.getUUID())) {
                weg(p);
            }
            return;
        }
        if (sam == null) {
            weg(p);   // (one in another world: gone)
            sam = maak(p);
            if (sam == null) {
                return;
            }
        }
        String doet = doet(sam);
        if (!Ring.magOm(p) && !doet.equals(DRAAGT)) {
            // (PHASE3 R09) his player is in a game (a level of Super Guhrio, a race): Sam-guh waits where he stands, at the
            // gate, and neither walks nor hops in after them; when the game is over he follows again
            sam.getNavigation().stop();
            zetDoet(sam, WACHT, 40);
            sam.getPersistentData().putLong(VerhaalGuhs.PLEK, sam.blockPosition().asLong());
            return;
        }
        if (doet.equals(DRAAGT) && sam.getFirstPassenger() != p) {
            // the player got off (or was put back somewhere): the ride is over
            RITTEN.remove(p.getUUID());
            sam.getPersistentData().remove(DOET);
            doet = VOLGT;
        }
        if (doet.equals(VOLGT) && sam.distanceToSqr(p) > SPRING_AFSTAND * SPRING_AFSTAND) {
            kom(p);
        }
        // (a story copy walks back to its spot: Sam's spot is wherever his player is)
        sam.getPersistentData().putLong(VerhaalGuhs.PLEK, (doet.equals(VOLGT) ? p.blockPosition() : sam.blockPosition()).asLong());
        if (!p.isPassenger() && Ring.zwaarte(p) >= DRAAG_VANAF && doet.equals(VOLGT) && p.tickCount % 400 < 20) {
            GuhQuests.say(p, sam, "quest.guhs.ring.sam.zwaar");
        }
    }

    @Nullable
    private static GuhEntity maak(ServerPlayer p) {
        ServerLevel level = p.level();
        GuhEntity sam = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (sam == null) {
            return null;
        }
        Vec3 naast = naast(p);
        sam.setVariant(GuhVariant.SAM_GUH);
        sam.setGuhScale(VerhaalGuhs.SCHAAL);
        sam.setPersonality(GuhPersonality.BRAVE);
        sam.snapTo(naast.x, naast.y, naast.z, p.getYRot(), 0f);
        VerhaalGuhs.markeer(sam, VerhaalGuh.SAM_GUH, p.blockPosition());
        sam.addTag(TAG);
        sam.getPersistentData().putString(TAG, p.getStringUUID());
        Zicht.alleenVoor(sam, p.getUUID());
        SAMS.put(p.getUUID(), sam.getUUID());
        level.addFreshEntity(sam);
        GuhDex.zie(p, GuhVariant.SAM_GUH);
        Ring.behaald(p, "ring_sam");
        return sam;
    }

    private static void weg(ServerPlayer p) {
        UUID id = SAMS.remove(p.getUUID());
        RITTEN.remove(p.getUUID());
        if (id == null) {
            return;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.ejectPassengers();
                e.discard();
            }
        }
    }

    /** A free spot next to (a little behind) the player. */
    static Vec3 naast(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 achter = Vec3.directionFromRotation(0, p.getYRot()).scale(-1.6);
        Vec3 zij = new Vec3(-achter.z, 0, achter.x).normalize();
        for (Vec3 kant : new Vec3[]{achter.add(zij), achter.subtract(zij), achter, zij.scale(1.6), zij.scale(-1.6)}) {
            BlockPos pos = BlockPos.containing(p.getX() + kant.x, p.getY() + 0.2, p.getZ() + kant.z);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos q = pos.above(dy);
                if (level.getBlockState(q.below()).blocksMotion() && !level.getBlockState(q).blocksMotion() && !level.getBlockState(q.above()).blocksMotion()
                        && level.getFluidState(q).isEmpty()) {
                    return Vec3.atBottomCenterOf(q);
                }
            }
        }
        return p.position();
    }

    /** The owner of a companion (online and in its world), or null. */
    @Nullable
    static ServerPlayer baas(GuhEntity sam) {
        try {
            String s = sam.getPersistentData().getStringOr(TAG, "");
            return s.isEmpty() ? null : sam.level().getPlayerByUUID(UUID.fromString(s)) instanceof ServerPlayer p ? p : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** (GuhHooks.tick, server, every guh) the companion's own little jobs; a Sam nobody knows about any more is removed. */
    static void guhTick(GuhEntity guh) {
        if (!isSam(guh)) {
            return;
        }
        ServerPlayer p = baas(guh);
        if (p == null || !guh.getUUID().equals(SAMS.get(p.getUUID()))) {
            guh.ejectPassengers();
            guh.discard();
            return;
        }
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = guh.getPersistentData();
        if (data.getStringOr(DOET, "").equals(KOOKT)) {
            BlockPos vuur = BlockPos.of(data.getLongOr(VUUR, 0L));
            long over = data.getLongOr(TOT, 0L) - level.getGameTime();
            if (over <= 0) {
                data.remove(DOET);
                klaarMetKoken(p, guh);
            } else {
                guh.getLookControl().setLookAt(vuur.getX() + 0.5, vuur.getY() + 0.5, vuur.getZ() + 0.5);
                if (vuur.distToCenterSqr(guh.position()) < 9 && over % 8 == 0) {
                    level.sendParticles(p, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, false, vuur.getX() + 0.5, vuur.getY() + 0.9, vuur.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
                    level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, guh.getX(), guh.getY() + 0.9, guh.getZ(), 1, 0.3, 0.2, 0.3, 0);
                    if (over % 24 == 0) {
                        level.playSound(null, vuur, SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.5f, 1.4f);
                    }
                }
            }
        }
    }

    /** A click on a Sam-guh (VerhaalGuhs.opKlik): the scene hooks first, then the companion's own answers. */
    static InteractionResult klik(GuhEntity sam, ServerPlayer p, InteractionHand hand) {
        for (VerhaalGuhs.Klik k : BIJ_KLIK) {
            InteractionResult r = k.klik(sam, p, hand);
            if (r != InteractionResult.PASS) {
                return r;
            }
        }
        if (!isSam(sam)) {
            return InteractionResult.PASS;   // (a story copy without a scene: the friendly word of VerhaalGuhs)
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        if (baas(sam) != p) {
            GuhQuests.say(p, sam, "quest.guhs.ring.sam.andere_baas");
            return InteractionResult.SUCCESS;
        }
        sam.playSound(ModSounds.GUH_AMBIENT.get(), 1f, sam.getVoicePitch());
        if (Ring.klaar(p)) {
            if (VerhaalGuhs.magTemmen(p, VerhaalGuh.SAM_GUH)) {
                Vec3 hier = sam.position();
                stuurWeg(p);
                GuhEntity eigen = VerhaalGuhs.tem(p, VerhaalGuh.SAM_GUH, hier);
                if (eigen != null) {
                    GuhQuests.say(p, eigen, "quest.guhs.ring.sam.getemd");
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (Ring.kreeg(p) && !Ring.heeft(p)) {
            Ring.geef(p);
            GuhQuests.say(p, sam, "quest.guhs.ring.sam.ring_terug");
            return InteractionResult.SUCCESS;
        }
        if (p.getVehicle() == sam) {
            p.stopRiding();
            return InteractionResult.SUCCESS;
        }
        if (Ring.zwaarte(p) >= DRAAG_VANAF && draag(p)) {
            return InteractionResult.SUCCESS;
        }
        GuhQuests.say(p, sam, "quest.guhs.ring.sam.praat." + p.getRandom().nextInt(5));
        Ring.vertelDoel(p, sam);
        return InteractionResult.SUCCESS;
    }

    // --- his goals (added to every guh by GuhHooks.doelen; they only ever run for a companion) ---------------------------

    /** Walks after his player. */
    static final class Volg extends Goal {
        private final GuhEntity guh;
        private int wacht;

        Volg(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!isSam(guh) || !doet(guh).equals(VOLGT)) {
                return false;
            }
            ServerPlayer p = baas(guh);
            return p != null && guh.distanceToSqr(p) > VOLG_AFSTAND * VOLG_AFSTAND;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer p = baas(guh);
            return p != null && isSam(guh) && doet(guh).equals(VOLGT) && guh.distanceToSqr(p) > 6.25;
        }

        @Override
        public void start() {
            wacht = 0;
        }

        @Override
        public void tick() {
            ServerPlayer p = baas(guh);
            if (p == null) {
                return;
            }
            guh.getLookControl().setLookAt(p, 20f, 20f);
            if (--wacht <= 0) {
                wacht = 10;
                double d = guh.distanceToSqr(p);
                if (!guh.getNavigation().moveTo(p, d > 100 ? 1.45 : 1.2) && d > 64) {
                    kom(p);   // (no way to walk there: a ledge, a gap)
                }
            }
        }

        @Override
        public void stop() {
            guh.getNavigation().stop();
        }
    }

    /** Carries his player: along the route of {@link #draag(ServerPlayer, List, Consumer)}, or towards the story's next goal. */
    static final class Draag extends Goal {
        private final GuhEntity guh;
        private int wacht;

        Draag(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isSam(guh) && guh.getPersistentData().getStringOr(DOET, "").equals(DRAAGT) && guh.getFirstPassenger() instanceof ServerPlayer;
        }

        @Override
        public void start() {
            wacht = 0;
        }

        @Override
        public void tick() {
            if (!(guh.getFirstPassenger() instanceof ServerPlayer p)) {
                return;
            }
            Rit rit = RITTEN.get(p.getUUID());
            Vec3 naar = null;
            if (rit != null && !rit.route().isEmpty()) {
                naar = rit.route().get(Math.min(rit.stap(), rit.route().size() - 1));
                if (naar.distanceToSqr(guh.position()) < 3.0) {
                    if (rit.stap() + 1 >= rit.route().size()) {
                        aangekomen(p, rit);
                        return;
                    }
                    RITTEN.put(p.getUUID(), new Rit(rit.route(), rit.stap() + 1, rit.daarna()));
                    wacht = 0;
                    return;
                }
            } else {
                Verhaallijn lijn = Ring.bezigMet(p);
                Doel doel = lijn == null ? null : Doelen.van(p, lijn);
                BlockPos daar = doel == null ? null : Doelen.zoek(p, doel);
                if (daar == null || daar.distToCenterSqr(guh.position()) < 36) {
                    aangekomen(p, rit);
                    return;
                }
                // (far away: a stretch at a time, the path finder does not look further than that)
                Vec3 kant = Vec3.atBottomCenterOf(daar).subtract(guh.position());
                naar = kant.length() > 24 ? guh.position().add(kant.normalize().scale(24)) : Vec3.atBottomCenterOf(daar);
            }
            if (--wacht <= 0) {
                wacht = 10;
                if (!guh.getNavigation().moveTo(naar.x, naar.y, naar.z, 1.15)) {
                    guh.getMoveControl().setWantedPosition(naar.x, naar.y, naar.z, 1.15);
                }
            }
        }

        private void aangekomen(ServerPlayer p, @Nullable Rit rit) {
            RITTEN.remove(p.getUUID());
            guh.getPersistentData().remove(DOET);
            guh.getNavigation().stop();
            p.stopRiding();
            GuhQuests.say(p, guh, "quest.guhs.ring.sam.aangekomen");
            if (rit != null && rit.daarna() != null) {
                rit.daarna().accept(p);
            }
        }

        @Override
        public void stop() {
            guh.getNavigation().stop();
        }
    }

    static void vergeet(ServerPlayer p) {
        weg(p);
    }

    static void wisAlles() {
        SAMS.clear();
        RITTEN.clear();
    }

    private Sam() {
    }
}
