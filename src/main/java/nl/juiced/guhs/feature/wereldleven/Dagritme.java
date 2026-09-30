package nl.juiced.guhs.feature.wereldleven;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalEvents;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The guh day (2.8, wereldleven) for the guhs of guh villages and the Knuffeldal (its town residents too) and for
 * free-roaming tamed guhs (tamed and set to wander):
 * <ul>
 *   <li>OCHTEND: a big yawn and a stretch (the GAPEN emote), once a morning; waking up from the night with one too.</li>
 *   <li>DAG: tamed guhs wave at a player who comes close (wild guhs already do that by themselves, see GuhEmotes).</li>
 *   <li>DUTJE: an afternoon nap, once a day: in a guh nest close by, or curled up on the spot in a little nestje (the
 *       {@link #DUTJE} flag: the renderer draws the nestje).</li>
 *   <li>AVOND: round a lit campfire close by, sitting, roasting marshmallow knabbels (give them one: they love it).</li>
 *   <li>NACHT: zzz (residents at home; a guh nest close by is taken by the Vadswoud's SleepInNestGoal first).</li>
 * </ul>
 * A busy guh (GuhHooks.isBezig: theekransje, feestbuffet, ...) is left alone; so is a guh that sits, fights, rides or is
 * led. Cheap: one goal per guh that looks around every 2-4 seconds, and a tick hook every second.
 */
public final class Dagritme {
    /** KnusVlaggen bit (wereldleven's own, next to the four shared flags): napping on the spot (a nestje is drawn under it). */
    public static final int DUTJE = 16;
    static final String SLAAP = "guhs_wereldleven_slaap", GAAP_DAG = "guhs_wereldleven_gaap", DUTJE_DAG = "guhs_wereldleven_dutje",
            ZWAAI_TOT = "guhs_wereldleven_zwaai_tot", ZWAAI_BIJ = "guhs_wereldleven_zwaai_bij", KAMPVUUR = "guhs_wereldleven_kampvuur";
    /** How far a guh looks for a nest (nap) and a campfire (evening). */
    public static final int NEST_ZOEK = 12, VUUR_ZOEK = 16;
    /** How far a guh of a Knuffeldal town looks for a campfire (from its house or street to the plein's fire), and how far up or down. */
    public static final int VUUR_ZOEK_BEWONER = 56, VUUR_DY = 4, VUUR_DY_BEWONER = 10;
    /** While a guh walks to a far campfire it may plan a longer path (the guh's own follow range is only 16). */
    private static final Identifier VER_PAD = Guhs.id("wereldleven_kampvuur_pad");
    /** A napping tamed guh wakes up when its owner walks further away than this. */
    public static final double BAAS_WEG = 14;

    /** (Game tests) the part of the day for these guhs (the test level's clock is shared), and guhs that always take part. */
    public static final Map<UUID, Dagdeel> TEST_DAGDEEL = new ConcurrentHashMap<>();
    public static final Set<UUID> TEST_MEE = ConcurrentHashMap.newKeySet();
    /** (Game tests) the area a test's guh looks for nests, campfires and plushies in (not the neighbouring tests'). */
    public static final Map<UUID, AABB> TEST_GEBIED = new ConcurrentHashMap<>();

    /** Is this spot somewhere a guh may go for its nest / fire / plushie? (always, except in a game test: its own area) */
    public static boolean inGebied(GuhEntity guh, BlockPos pos) {
        AABB gebied = TEST_GEBIED.get(guh.getUUID());
        return gebied == null || gebied.contains(Vec3.atCenterOf(pos));
    }

    /** Wild guhs: [checked at game time, 1 = in the Knuffeldal / near a village]. */
    private static final Map<GuhEntity, long[]> OMGEVING = new WeakHashMap<>();
    /** Guhs whose current wave was counted already. */
    private static final Map<GuhEntity, Boolean> ZWAAI_GETELD = new WeakHashMap<>();

    /** A seat at a campfire that a guh is walking to or sits on: (guh, until game time). Keyed by dimension + position. */
    private record Plek(UUID guh, long tot) {
    }

    private static final Map<String, Plek> PLEKKEN = new ConcurrentHashMap<>();

    /** The campfires (lit or not) around a 64x64 cell, for the far look of town guhs (game time of the scan). */
    private record VuurScan(long tijd, List<BlockPos> vuren) {
    }

    private static final Map<String, VuurScan> VUUR_CACHE = new ConcurrentHashMap<>();
    /** How long a cell's campfire scan is kept (ticks), and the cell size (blocks). */
    static final int VUUR_CACHE_TICKS = 100, VUUR_CEL = 64;

    static void register() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(5, new DagritmeGoal(guh)));
        GuhHooks.tick(Dagritme::tick);
        GuhHooks.klik(Dagritme::klik);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> vergeetAlles());
    }

    // --- who, when ------------------------------------------------------------------------------------------------------

    /** The part of the day for this guh (null: no day rhythm here, e.g. in the guh stomach). */
    @Nullable
    public static Dagdeel dagdeel(GuhEntity guh) {
        Dagdeel test = TEST_DAGDEEL.get(guh.getUUID());
        if (test != null) {
            return test;
        }
        Level level = guh.level();
        return level.dimensionType().hasFixedTime() ? null : Dagdeel.huidig(level);
    }

    /** The day of the rhythm (a morning belongs to the day it starts: it begins at 23000). */
    public static long ritmeDag(Level level) {
        return Math.floorDiv(level.getDayTime() + 1000L, 24000L);
    }

    /** Does this guh live the guh day? (a resident, a free-roaming tamed guh, a wild guh in the Knuffeldal or a guh village) */
    public static boolean doetMee(GuhEntity guh) {
        if (guh.getType() != ModEntities.GUH.get() || guh.isNoAi() || guh.getHiddenBy() != null || guh.isPassenger() || guh.isVehicle()
                || guh.isLeashed()) {
            return false;
        }
        if (nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(guh)) {
            return false;    // 2.10: a Guhhuisje resident: its huisje runs its day (HuisjeGoal)
        }
        if (TEST_MEE.contains(guh.getUUID())) {
            return true;
        }
        if (testServer(guh.level())) {
            return false;    // (on the GameTest server only the guhs of our own tests: the other tests' guhs keep still)
        }
        if (GuhHooks.isBewoner(guh)) {
            return true;
        }
        if (guh.isTame()) {
            return guh.mayWander() && !guh.isOrderedToSit();
        }
        return omgeving(guh);
    }

    /** The GameTest server: the day rhythm (and the cart and the cuddles) only for the guhs a test asks for ({@link #TEST_MEE}). */
    public static boolean testServer(Level level) {
        return level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer;
    }

    /** May a guh take part in the wereldleven goals (the cart, the plushies) here? Not the other tests' guhs on the GameTest server. */
    public static boolean magInTest(GuhEntity guh) {
        return TEST_MEE.contains(guh.getUUID()) || !testServer(guh.level());
    }

    private static boolean omgeving(GuhEntity guh) {
        long[] c = omgevingInfo(guh);
        return c != null && c[1] == 1;
    }

    /** Is this guh in a Knuffeldal town (a resident, or a guh wandering its streets)? Then the plein's campfire is its fire. */
    public static boolean inStadje(GuhEntity guh) {
        if (GuhHooks.isBewoner(guh)) {
            return true;
        }
        long[] c = omgevingInfo(guh);
        return c != null && c[2] == 1;
    }

    /** [checked at game time, in the Knuffeldal / near a village, in a Knuffeldal town] (looked up again every 30 s). */
    @Nullable
    private static long[] omgevingInfo(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return null;
        }
        long now = level.getGameTime();
        long[] c = OMGEVING.get(guh);
        if (c == null || now - c[0] > 600) {
            BlockPos pos = guh.blockPosition();
            boolean dal = KnuffeldalEvents.inKnuffeldal(level, pos);
            boolean ja = dal || level.sectionsToVillage(SectionPos.of(pos)) <= 2;
            boolean stad = dal && !testServer(level) && PleinSlot.stadje(level, pos) != null;
            c = new long[] {now, ja ? 1 : 0, stad ? 1 : 0};
            OMGEVING.put(guh, c);
        }
        return c;
    }

    // --- the tick hook: waking up, the morning yawn, the wave --------------------------------------------------------------

    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 20 != 0) {
            return;
        }
        Kaasijsjes.tick(guh);
        if (!guh.level().isClientSide() && guh.getPersistentData().getBooleanOr(KAMPVUUR, false) && !aanHetVuur(guh)) {
            guh.getPersistentData().remove(KAMPVUUR);   // (left over: the chunk unloaded while it sat at the fire, or the evening is over)
        }
        if (guh.isNoAi()) {
            return;    // (show guhs, guards, the lineup: they keep what they have)
        }
        telZwaai(guh);
        CompoundTag data = guh.getPersistentData();
        String slaap = data.getStringOr(SLAAP, "");
        if (!slaap.isEmpty() || (guh.getKnusVlaggen() & DUTJE) != 0) {
            if (moetWakker(guh, slaap)) {
                wakker(guh, slaap.equals("nacht") && dagdeel(guh) == Dagdeel.OCHTEND);
            }
            return;
        }
        Dagdeel d = dagdeel(guh);
        if (d == null || guh.emotes.current() != null || !GuhEmotes.canStart(guh) || GuhHooks.isBezig(guh) || !doetMee(guh)) {
            return;
        }
        if (d == Dagdeel.OCHTEND) {
            long dag = ritmeDag(guh.level()) + 1;   // (+1: 0 means "never")
            if (data.getLongOr(GAAP_DAG, 0L) != dag && guh.getRandom().nextInt(6) == 0) {
                gapen(guh);
            }
        } else if (d == Dagdeel.DAG && guh.isTame()) {
            zwaai(guh);
        }
    }

    /** The morning yawn and stretch. */
    public static boolean gapen(GuhEntity guh) {
        if (!guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF)) {
            return false;
        }
        guh.getPersistentData().putLong(GAAP_DAG, ritmeDag(guh.level()) + 1);
        for (Player p : guh.level().getEntitiesOfClass(Player.class, guh.getBoundingBox().inflate(12), EntitySelector.NO_SPECTATORS)) {
            if (p instanceof ServerPlayer sp) {
                GuhAdvancements.grant(sp, "wereldleven_gapen");
            }
        }
        return true;
    }

    /** A tamed guh waves at a player who comes close (the owner too, when coming back): once per coming, not all the time. */
    private static void zwaai(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        Player near = guh.level().getNearestPlayer(guh.getX(), guh.getY(), guh.getZ(), GuhEmotes.WAVE_RANGE, EntitySelector.NO_SPECTATORS);
        if (near == null) {
            if (guh.level().getNearestPlayer(guh.getX(), guh.getY(), guh.getZ(), GuhEmotes.WAVE_RESET_RANGE, EntitySelector.NO_SPECTATORS) == null) {
                data.remove(ZWAAI_BIJ);
            }
            return;
        }
        String id = near.getUUID().toString();
        if (id.equals(data.getStringOr(ZWAAI_BIJ, ""))) {
            return;
        }
        data.putString(ZWAAI_BIJ, id);
        long now = guh.level().getGameTime();
        if (now >= data.getLongOr(ZWAAI_TOT, 0L) && !near.isInvisible() && guh.emotes.greet(near)) {
            data.putLong(ZWAAI_TOT, now + 20 * 60);
        }
    }

    /** Every guh that waves at a player (wild ones by themselves, tamed ones here) counts once for that player. */
    private static void telZwaai(GuhEntity guh) {
        if (guh.emotes.current() != Emote.ZWAAIEN) {
            ZWAAI_GETELD.remove(guh);
            return;
        }
        UUID target = guh.emotes.lookTarget();
        if (target == null || ZWAAI_GETELD.containsKey(guh) || !(guh.level().getPlayerByUUID(target) instanceof ServerPlayer p)) {
            return;
        }
        ZWAAI_GETELD.put(guh, true);
        KnusVoortgang.tel(p, WereldlevenVoortgang.GEZWAAID, 1);
        GuhAdvancements.grant(p, "wereldleven_gezwaaid");
    }

    // --- sleeping and waking -----------------------------------------------------------------------------------------------

    /** Curls up (the looping SLAPEN emote); soort "dutje" or "nacht". Without a real nest under it a nestje is drawn. */
    public static boolean slaap(GuhEntity guh, String soort, boolean opNest) {
        guh.getNavigation().stop();
        if (!guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF)) {
            return false;
        }
        guh.getPersistentData().putString(SLAAP, soort);
        if (soort.equals("dutje")) {
            guh.getPersistentData().putLong(DUTJE_DAG, ritmeDag(guh.level()) + 1);   // (one nap a day)
        }
        if (!opNest) {
            guh.setKnusVlaggen(guh.getKnusVlaggen() | DUTJE);
        }
        if (soort.equals("dutje")) {
            for (Player p : guh.level().getEntitiesOfClass(Player.class, guh.getBoundingBox().inflate(16), EntitySelector.NO_SPECTATORS)) {
                if (p instanceof ServerPlayer sp) {
                    KnusVoortgang.tel(sp, WereldlevenVoortgang.DUTJES, 1);
                    GuhAdvancements.grant(sp, "wereldleven_dutje");
                }
            }
        }
        return true;
    }

    /** Is this guh really sitting at a campfire right now? (its Dagritme goal runs the campfire plan, in the evening) */
    public static boolean aanHetVuur(GuhEntity guh) {
        if (dagdeel(guh) != Dagdeel.AVOND || !guh.isInSittingPose()) {
            return false;
        }
        return guh.goalSelector.getAvailableGoals().stream()
                .anyMatch(w -> w.isRunning() && w.getGoal() instanceof DagritmeGoal g && g.zitBijVuur());
    }

    public static boolean slaapt(GuhEntity guh) {
        return !guh.getPersistentData().getStringOr(SLAAP, "").isEmpty();
    }

    private static boolean moetWakker(GuhEntity guh, String slaap) {
        if (nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(guh)) {
            return false;   // 2.10: a Guhhuisje resident is woken by its huisje (HuisjeGoal), never by the owner walking away
        }
        if (guh.emotes.current() != Emote.SLAPEN || slaap.isEmpty()) {
            return true;    // (woke up by itself: hurt, pushed, picked up, or reloaded)
        }
        Dagdeel d = dagdeel(guh);
        if (d == null || (slaap.equals("dutje") && d != Dagdeel.DUTJE) || (slaap.equals("nacht") && d != Dagdeel.NACHT)) {
            return true;
        }
        if (guh.isTame() && guh.getOwner() instanceof LivingEntity owner && (owner.level() != guh.level() || owner.distanceTo(guh) > BAAS_WEG)) {
            return true;
        }
        return GuhHooks.isBezig(guh) || !doetMee(guh);   // (another activity came for it: the theekransje, the feestbuffet...)
    }

    /** Wakes up (after the night: with a yawn and a stretch). */
    public static void wakker(GuhEntity guh, boolean gapen) {
        guh.getPersistentData().remove(SLAAP);
        if ((guh.getKnusVlaggen() & DUTJE) != 0) {
            guh.setKnusVlaggen(guh.getKnusVlaggen() & ~DUTJE);
        }
        if (guh.emotes.current() == Emote.SLAPEN) {
            guh.emotes.stop();
        }
        if (gapen && GuhEmotes.canStart(guh)) {
            gapen(guh);
        }
    }

    // --- marshmallows at the campfire -----------------------------------------------------------------------------------------

    /** Give a marshmallow knabbel to a guh sitting at the campfire: it roasts it and eats it, very happily. */
    private static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(KnusTags.MARSHMALLOW)) {
            return InteractionResult.PASS;
        }
        if (guh.level().isClientSide()) {
            return guh.isInSittingPose() && !guh.isOrderedToSit() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!guh.getPersistentData().getBooleanOr(KAMPVUUR, false) || !aanHetVuur(guh) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        roosterMarshmallow(guh, stack.copyWithCount(1));
        stack.consume(1, player);
        guh.heal(guh.getMaxHealth() * 0.1f);
        guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
        ((ServerLevel) guh.level()).sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight(), guh.getZ(), 5,
                guh.getBbWidth() * 0.4, 0.2, guh.getBbWidth() * 0.4, 0.02);
        KnusVoortgang.tel(sp, WereldlevenVoortgang.MARSHMALLOWS, 1);
        GuhAdvancements.grant(sp, "wereldleven_kampvuur");
        sp.sendOverlayMessage(Component.translatable("gui.guhs.wereldleven.marshmallow", guh.getDisplayName()).withStyle(ChatFormatting.GOLD));
        return InteractionResult.SUCCESS;
    }

    /** Roasting: a little flame at the tip of the stick, then munching (crumbs of marshmallow). */
    static void roosterMarshmallow(GuhEntity guh, ItemStack marshmallow) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0, guh.yBodyRot);
        double w = guh.getBbWidth(), h = guh.getBbHeight();
        Vec3 tip = guh.position().add(forward.scale(w * 0.6 + 0.35)).add(0, h * 0.55, 0);
        Vec3 mouth = guh.position().add(forward.scale(w * 0.5)).add(0, h * 0.4, 0);
        level.sendParticles(ParticleTypes.SMALL_FLAME, tip.x, tip.y, tip.z, 2, 0.03, 0.03, 0.03, 0.005);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, marshmallow), mouth.x, mouth.y, mouth.z, 4, 0.08, 0.05, 0.08, 0.04);
        guh.playSound(ModSounds.GUH_EAT.get(), 0.4f, guh.getVoicePitch() * 1.15f);
    }

    // --- the goal -------------------------------------------------------------------------------------------------------------

    /** Walks to the nap spot / the campfire / home, then naps, sits at the fire or sleeps. */
    static class DagritmeGoal extends Goal {
        private enum Plan { DUTJE, KAMPVUUR, NACHT }

        private final GuhEntity guh;
        private int cooldown;
        @Nullable
        private Plan plan;
        @Nullable
        private BlockPos doel;
        @Nullable
        private BlockPos vuur;
        private boolean opNest;
        private boolean klaar;
        private boolean zit;
        private int ticks;
        private long bezigTot;

        DagritmeGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        boolean zitBijVuur() {
            return plan == Plan.KAMPVUUR && zit;
        }

        /** How long it may walk before it gets there (a resident walks from its house to the plein). */
        private int looptijd() {
            return inStadje(guh) ? 20 * 60 : 20 * 30;
        }

        private boolean mag() {
            return guh.isAlive() && !guh.isOrderedToSit() && guh.getTarget() == null && !guh.isInLove() && !guh.isInWater()
                    && guh.getLaunchState() == GuhEntity.LAUNCH_NONE && doetMee(guh)
                    && !(guh.isTame() && guh.getOwner() instanceof LivingEntity owner
                         && (owner.level() != guh.level() || owner.distanceTo(guh) > BAAS_WEG));   // (a tamed guh goes with its owner)
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0) {
                return false;
            }
            cooldown = 40 + guh.getRandom().nextInt(40);
            if (slaapt(guh) || guh.emotes.current() != null || GuhHooks.isBezig(guh) || !mag()) {
                return false;
            }
            Dagdeel d = dagdeel(guh);
            if (d == null) {
                return false;
            }
            klaar = false;
            zit = false;
            switch (d) {
                case DUTJE -> {
                    long dag = ritmeDag(guh.level()) + 1;
                    if (guh.getPersistentData().getLongOr(DUTJE_DAG, 0L) == dag || guh.getRandom().nextInt(3) != 0) {
                        return false;
                    }
                    plan = Plan.DUTJE;   // (the day's nap counts once it really naps: see slaap)
                    doel = nest(guh);
                    opNest = doel != null;
                    if (doel == null) {
                        doel = guh.blockPosition();
                    }
                    return true;
                }
                case AVOND -> {
                    vuur = kampvuur(guh);
                    if (vuur == null) {
                        cooldown = 200 + guh.getRandom().nextInt(100);
                        return false;
                    }
                    doel = plekBijVuur(guh, vuur);
                    if (doel == null) {
                        cooldown = 200;
                        return false;
                    }
                    reserveer(guh, doel, looptijd() + 20 * 10);
                    plan = Plan.KAMPVUUR;
                    return true;
                }
                case NACHT -> {
                    plan = Plan.NACHT;
                    doel = nest(guh);
                    opNest = doel != null;
                    if (doel == null) {
                        BlockPos thuis = GuhHooks.isBewoner(guh) ? GuhHooks.thuis(guh) : null;
                        doel = thuis != null && thuis.distSqr(guh.blockPosition()) < 32 * 32 ? thuis : guh.blockPosition();
                    }
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        private boolean fits(Dagdeel d) {
            return plan != null && d != null && switch (plan) {
                case DUTJE -> d == Dagdeel.DUTJE;
                case KAMPVUUR -> d == Dagdeel.AVOND;
                case NACHT -> d == Dagdeel.NACHT;
            };
        }

        @Override
        public boolean canContinueToUse() {
            if (klaar || doel == null || !mag() || !fits(dagdeel(guh))) {
                return false;
            }
            if (GuhHooks.isBezig(guh) && (bezigTot == 0 || guh.getPersistentData().getLongOr("guhs_knus_bezig_tot", 0L) != bezigTot)) {
                return false;    // (someone else's activity)
            }
            if (plan == Plan.KAMPVUUR) {
                return vuur != null && guh.level().getBlockState(vuur).getBlock() instanceof CampfireBlock
                        && guh.level().getBlockState(vuur).getValue(CampfireBlock.LIT) && (zit || ticks < looptijd());
            }
            return ticks < 20 * 30;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ticks = 0;
            if (plan == Plan.KAMPVUUR && doel != null && doel.distSqr(guh.blockPosition()) > 12 * 12) {
                verPad(true);
            }
            if (doel != null) {
                guh.getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0);
            }
            if (plan == Plan.KAMPVUUR && GuhHooks.isBewoner(guh)) {
                claimBezig(looptijd() + 20 * 10);   // (so a resident may walk from its house to the town's campfire)
            }
        }

        /** A longer path for the walk to a far campfire (on), back to normal (off). */
        private void verPad(boolean aan) {
            AttributeInstance range = guh.getAttribute(Attributes.FOLLOW_RANGE);
            if (range != null) {
                range.removeModifier(VER_PAD);
                if (aan) {
                    range.addTransientModifier(new AttributeModifier(VER_PAD, VUUR_ZOEK_BEWONER, AttributeModifier.Operation.ADD_VALUE));
                }
            }
            guh.getNavigation().setMaxVisitedNodesMultiplier(aan ? 4f : 1f);
        }

        private void claimBezig(int t) {
            GuhHooks.bezig(guh, t);
            bezigTot = guh.getPersistentData().getLongOr("guhs_knus_bezig_tot", 0L);
        }

        @Override
        public void tick() {
            if (doel == null) {
                return;
            }
            ticks++;
            double cx = doel.getX() + 0.5, cz = doel.getZ() + 0.5;
            double dx = guh.getX() - cx, dz = guh.getZ() - cz;
            double flat = Math.sqrt(dx * dx + dz * dz);
            boolean daar = flat < (plan == Plan.KAMPVUUR ? 0.9 : 1.2) && Math.abs(guh.getY() - doel.getY()) < 1.5;
            if (plan == Plan.KAMPVUUR) {
                kampvuurTick(daar, flat, cx, cz);
                return;
            }
            if (daar || (!opNest && plan == Plan.NACHT && doel.equals(guh.blockPosition()))) {
                klaar = slaap(guh, plan == Plan.DUTJE ? "dutje" : "nacht", opNest) || ticks > 20 * 10;
                return;
            }
            if (flat < 1.8 && opNest) {
                guh.getMoveControl().setWantedPosition(cx, doel.getY() + 0.2, cz, 0.7);
            } else if (ticks % 20 == 0 && guh.getNavigation().isDone()) {
                guh.getNavigation().moveTo(cx, doel.getY(), cz, 1.0);
            }
        }

        private void kampvuurTick(boolean daar, double flat, double cx, double cz) {
            if (vuur == null) {
                return;
            }
            if (!zit) {
                if (daar) {
                    zit = true;
                    verPad(false);
                    guh.getNavigation().stop();
                    guh.setInSittingPose(true);
                    guh.getPersistentData().putBoolean(KAMPVUUR, true);
                } else if (flat < 1.6) {
                    guh.getMoveControl().setWantedPosition(cx, doel.getY(), cz, 0.6);
                } else if (ticks % 20 == 0 && guh.getNavigation().isDone()) {
                    guh.getNavigation().moveTo(cx, doel.getY(), cz, 1.0);
                }
                return;
            }
            guh.getNavigation().stop();
            guh.getLookControl().setLookAt(vuur.getX() + 0.5, vuur.getY() + 0.4, vuur.getZ() + 0.5, 10f, 20f);
            if (!guh.isInSittingPose()) {
                guh.setInSittingPose(true);
            }
            if (ticks % (80 + (guh.getId() & 63)) == 0) {
                roosterMarshmallow(guh, new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get()));
            }
            if (GuhHooks.isBewoner(guh) && ticks % 100 == 0) {
                claimBezig(20 * 20);
            }
            if (ticks % 100 == 0 && doel != null) {
                reserveer(guh, doel, 20 * 20);   // (still sitting here)
            }
        }

        @Override
        public void stop() {
            if (zit && !guh.isOrderedToSit()) {
                guh.setInSittingPose(false);
            }
            if (bezigTot != 0 && guh.getPersistentData().getLongOr("guhs_knus_bezig_tot", 0L) == bezigTot) {
                GuhHooks.bezig(guh, 0);   // (only our own claim)
            }
            bezigTot = 0;
            guh.getPersistentData().remove(KAMPVUUR);
            if (plan == Plan.KAMPVUUR && doel != null) {
                geefVrij(guh, doel);
            }
            verPad(false);
            zit = false;
            plan = null;
            vuur = null;
            cooldown = 60 + guh.getRandom().nextInt(60);
            guh.getNavigation().stop();
        }
    }

    // --- finding things -------------------------------------------------------------------------------------------------------

    /** The nearest guh nest (Vadswoud's guhnestje) within {@link #NEST_ZOEK} blocks, or null. */
    @Nullable
    static BlockPos nest(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos here = guh.blockPosition();
        Optional<BlockPos> found = level.getPoiManager().findClosest(t -> t.is(VadswoudFeature.NEST_POI.getKey()),
                p -> level.getBlockState(p).is(VadswoudFeature.GUHNESTJE.get()) && inGebied(guh, p), here, NEST_ZOEK, PoiManager.Occupancy.ANY);
        return found.orElse(null);
    }

    /**
     * The nearest lit campfire (block entities of the chunks around), or null: within {@link #VUUR_ZOEK} blocks, for a
     * guh of a Knuffeldal town (a resident, or one in its streets) within {@link #VUUR_ZOEK_BEWONER} (to the fire on the plein).
     */
    @Nullable
    static BlockPos kampvuur(GuhEntity guh) {
        Level level = guh.level();
        BlockPos center = guh.blockPosition();
        boolean bewoner = inStadje(guh);
        int r = bewoner ? VUUR_ZOEK_BEWONER : VUUR_ZOEK;
        int dy = bewoner ? VUUR_DY_BEWONER : VUUR_DY;
        BlockPos best = null;
        double bestDist = (double) r * r;
        if (bewoner && !TEST_GEBIED.containsKey(guh.getUUID())) {
            // the far look of a town guh: one scan per 64x64 cell every 5 s, shared by all the guhs in it
            for (BlockPos p : vurenRond(level, center)) {
                if (Math.abs(p.getY() - center.getY()) <= dy && p.distSqr(center) <= bestDist) {
                    var state = level.getBlockState(p);
                    if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                        bestDist = p.distSqr(center);
                        best = p;
                    }
                }
            }
            return best;
        }
        for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++) {
            for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof CampfireBlockEntity && be.getBlockState().getValue(CampfireBlock.LIT) && inGebied(guh, be.getBlockPos())
                            && Math.abs(be.getBlockPos().getY() - center.getY()) <= dy) {
                        double d = be.getBlockPos().distSqr(center);
                        if (d <= bestDist) {
                            bestDist = d;
                            best = be.getBlockPos();
                        }
                    }
                }
            }
        }
        return best;
    }

    /** The circles round the fire (from, to blocks): the first circle full, the guhs sit down in a second one. */
    private static final double[][] KRINGEN = {{1.9, 3.2}, {3.2, 4.7}};

    /** A free spot 2-3 blocks from the fire, or 3-4.5 when that circle is full (standing room, a floor, no other guh), or null. */
    @Nullable
    static BlockPos plekBijVuur(GuhEntity guh, BlockPos vuur) {
        Level level = guh.level();
        List<BlockPos> vrij = new ArrayList<>();
        for (int k = 0; k < KRINGEN.length * 2 && vrij.isEmpty(); k++) {
            double van = KRINGEN[k / 2][0], tot = KRINGEN[k / 2][1];
            int dy = -(k % 2);
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d < van || d > tot) {
                        continue;
                    }
                    BlockPos p = vuur.offset(dx, dy, dz);
                    if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                            && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                            && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()
                            && !gereserveerd(guh, p)
                            && level.getEntitiesOfClass(GuhEntity.class, new AABB(p).inflate(0.2), g -> g != guh).isEmpty()) {
                        vrij.add(p);
                    }
                }
            }
        }
        if (vrij.isEmpty()) {
            return null;
        }
        vrij.sort((a, b) -> Double.compare(a.distSqr(guh.blockPosition()), b.distSqr(guh.blockPosition())));
        return vrij.get(guh.getRandom().nextInt(Math.min(3, vrij.size())));
    }

    // --- seats and the campfire cache ---------------------------------------------------------------------------------------------

    private static String sleutel(Level level, BlockPos pos) {
        return level.dimension().identifier() + "|" + pos.asLong();
    }

    /** Is this seat taken (walked to, or sat on) by another guh? */
    static boolean gereserveerd(GuhEntity guh, BlockPos plek) {
        Plek p = PLEKKEN.get(sleutel(guh.level(), plek));
        return p != null && !p.guh().equals(guh.getUUID()) && p.tot() > guh.level().getGameTime();
    }

    /** This guh takes this seat for a while (it walks there, or keeps sitting). */
    static void reserveer(GuhEntity guh, BlockPos plek, int ticks) {
        long nu = guh.level().getGameTime();
        if (PLEKKEN.size() > 512) {
            PLEKKEN.values().removeIf(p -> p.tot() <= nu);
        }
        PLEKKEN.put(sleutel(guh.level(), plek), new Plek(guh.getUUID(), nu + ticks));
    }

    static void geefVrij(GuhEntity guh, BlockPos plek) {
        PLEKKEN.computeIfPresent(sleutel(guh.level(), plek), (k, p) -> p.guh().equals(guh.getUUID()) ? null : p);
    }

    /** The campfires around this spot's cell (the cell plus {@link #VUUR_ZOEK_BEWONER} on every side), scanned at most every 5 s. */
    private static List<BlockPos> vurenRond(Level level, BlockPos center) {
        int celX = Math.floorDiv(center.getX(), VUUR_CEL), celZ = Math.floorDiv(center.getZ(), VUUR_CEL);
        String key = level.dimension().identifier() + "|" + celX + "," + celZ;
        long nu = level.getGameTime();
        VuurScan scan = VUUR_CACHE.get(key);
        if (scan != null && nu >= scan.tijd() && nu - scan.tijd() < VUUR_CACHE_TICKS) {
            return scan.vuren();
        }
        List<BlockPos> vuren = new ArrayList<>();
        int r = VUUR_ZOEK_BEWONER;
        int x0 = celX * VUUR_CEL - r, x1 = celX * VUUR_CEL + VUUR_CEL - 1 + r;
        int z0 = celZ * VUUR_CEL - r, z1 = celZ * VUUR_CEL + VUUR_CEL - 1 + r;
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof CampfireBlockEntity) {
                        vuren.add(be.getBlockPos().immutable());
                    }
                }
            }
        }
        if (VUUR_CACHE.size() > 256) {
            VUUR_CACHE.values().removeIf(s -> nu < s.tijd() || nu - s.tijd() >= VUUR_CACHE_TICKS);
        }
        List<BlockPos> klaar = List.copyOf(vuren);
        VUUR_CACHE.put(key, new VuurScan(nu, klaar));
        return klaar;
    }

    /** (Server stop) forget the seats and the campfire scans. */
    static void vergeetAlles() {
        PLEKKEN.clear();
        VUUR_CACHE.clear();
    }

    private Dagritme() {
    }
}
