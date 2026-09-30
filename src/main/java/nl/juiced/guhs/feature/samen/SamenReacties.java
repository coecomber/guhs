package nl.juiced.guhs.feature.samen;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;

/**
 * How your guh reacts to you outside the minigames (2.10, samen):
 * <ul>
 *   <li><b>troost</b>: after you died, your guh (the one with the most hearts that was around) is already waiting at your
 *       respawn and gives you a comforting hug;</li>
 *   <li><b>welkom</b>: back after a long time away (at least {@link #WEG_TICKS} ticks in the game, or
 *       {@link #WEG_MS} real time): a welcome-back dance (the knuffeldansje);</li>
 *   <li><b>onweer</b>: in a thunderstorm it comes and cuddles against you (brr!);</li>
 *   <li><b>welterusten</b>: when you go to bed your guhs close by wave you goodnight;</li>
 *   <li><b>bff-knuffel</b>: when a guh does the BFF_KNUFFEL emote near its owner, it stands right in front of them and a
 *       big heart floats above them both.</li>
 * </ul>
 * Cosy only: nothing here ever hurts, blocks or pushes anyone.
 */
public final class SamenReacties {
    /** Away this long (game ticks) = a welcome-back dance. */
    public static final long WEG_TICKS = 20 * 60 * 5;
    /** Or this long in real time (a server that ran on without you, or the world was closed): 30 minutes. */
    public static final long WEG_MS = 30L * 60 * 1000;
    /** "Together" for the welcome: the owner within this distance. */
    public static final double DICHTBIJ = 12;
    /** Persistent data of a guh. */
    static final String LAATST_TIJD = "guhs_samen_laatst_tijd", LAATST_MS = "guhs_samen_laatst_ms", ONWEER_TOT = "guhs_samen_onweer_tot",
            BFF_TOT = "guhs_samen_bff_tot";
    /** (Tests) it thunders for these guhs. */
    public static final java.util.Set<UUID> TEST_ONWEER = ConcurrentHashMap.newKeySet();

    /** Where a player died (for the respawn comfort). */
    private static final Map<UUID, GlobalPos> GESTORVEN = new ConcurrentHashMap<>();
    /** Per player: sleeping in the last tick (for the goodnight wave). */
    private static final Map<UUID, Boolean> SLAAPT = new ConcurrentHashMap<>();
    /** Per player: game time of the last thunder message (once per storm, more or less). */
    private static final Map<UUID, Long> ONWEER_MELDING = new ConcurrentHashMap<>();

    private SamenReacties() {
    }

    // =====================================================================================================================
    // troost: waiting at your respawn
    // =====================================================================================================================

    static void gestorven(ServerPlayer player) {
        GESTORVEN.put(player.getUUID(), GlobalPos.of(player.level().dimension(), player.blockPosition()));
    }

    /** After a respawn: the guh with the most hearts that was around (where you died, or where you are now) waits for you. */
    @Nullable
    public static GuhEntity troost(ServerPlayer player) {
        GlobalPos waar = GESTORVEN.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        List<GuhEntity> kandidaten = level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(64),
                g -> Band.isBandGuh(g) && player.getUUID().equals(g.getOwnerUUID()) && SamenMee.vrij(g));
        if (waar != null && waar.dimension() == level.dimension()) {
            kandidaten.addAll(level.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(waar.pos()).inflate(64),
                    g -> Band.isBandGuh(g) && player.getUUID().equals(g.getOwnerUUID()) && SamenMee.vrij(g) && !kandidaten.contains(g)));
        }
        GuhEntity guh = kandidaten.stream().max(Comparator.comparingInt(Band::hartjes)).orElse(null);
        if (guh == null) {
            return null;
        }
        Vec3 plek = voor(player, 1.6);
        if (!vrij(level, guh, plek)) {
            plek = player.position();
        }
        level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.3, guh.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        guh.getNavigation().stop();
        guh.moveTo(plek.x, plek.y, plek.z, guh.getYRot(), 0);
        guh.setDeltaMovement(Vec3.ZERO);
        guh.resetFallDistance();
        SamenSpel.kijk(guh, player.position());
        knuffel(guh, player);
        level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 8, 0.4, 0.2, 0.4, 0.03);
        level.playSound(null, guh.blockPosition(), SamenFeature.BFF.get(), SoundSource.NEUTRAL, 0.7f, 1.1f);
        player.displayClientMessage(Component.translatable("gui.guhs.samen.troost", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        Band.geefHartjes(guh, player, 3, Reden.OVERIG);
        GuhAdvancements.grant(player, "samen_troost");
        if (Dagboek.eersteKeer(guh, player, "samen_troost")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.troost", player.getGameProfile().getName());
        }
        return guh;
    }

    /** Is there room for this guh at this spot (no solid blocks in its box)? */
    static boolean vrij(ServerLevel level, Mob guh, Vec3 plek) {
        net.minecraft.world.phys.AABB box = guh.getDimensions(guh.getPose()).makeBoundingBox(plek).deflate(0.02);
        for (BlockPos b : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.getBlockState(b).getCollisionShape(level, b).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** A spot {@code d} blocks in front of the player (on their level). */
    static Vec3 voor(ServerPlayer player, double d) {
        Vec3 kijk = Vec3.directionFromRotation(0, player.getYRot());
        return player.position().add(kijk.scale(d));
    }

    /** A cuddle (KNUFFELEN) looking at the player. */
    static boolean knuffel(GuhEntity guh, ServerPlayer player) {
        SamenSpel.kijk(guh, player.position());
        if (guh.emotes.start(Emote.KNUFFELEN, false, GuhEmotes.Source.SELF)) {
            guh.emotes.setLookTarget(player.getUUID());
            return true;
        }
        return false;
    }

    // =====================================================================================================================
    // welkom, onweer (every band guh)
    // =====================================================================================================================

    static void tick(GuhEntity guh) {
        int t = guh.tickCount + guh.getId();
        if (t % 20 != 0 || Huisjes.isBinnen(guh)) {
            return;
        }
        ServerPlayer owner = Band.eigenaarOnline(guh);
        if (owner == null || owner.level() != guh.level() || owner.isSpectator()) {
            return;
        }
        double d = owner.distanceTo(guh);
        if (d <= DICHTBIJ) {
            welkomCheck(guh, owner);
        }
    }

    /** The owner is close: after a long time away, the welcome-back dance; then "together" again from now on. */
    static boolean welkomCheck(GuhEntity guh, ServerPlayer owner) {
        CompoundTag data = guh.getPersistentData();
        long nu = guh.level().getGameTime(), ms = System.currentTimeMillis();
        boolean welkom = false;
        if (data.contains(LAATST_TIJD)) {
            long weg = nu - data.getLong(LAATST_TIJD), wegMs = ms - data.getLong(LAATST_MS);
            if (weg >= WEG_TICKS || (wegMs >= WEG_MS && weg >= 200)) {
                welkom = welkom(guh, owner);
            }
        }
        data.putLong(LAATST_TIJD, nu);
        data.putLong(LAATST_MS, ms);
        return welkom;
    }

    /** The welcome-back dance. */
    public static boolean welkom(GuhEntity guh, ServerPlayer owner) {
        if (!(guh.level() instanceof ServerLevel level) || guh.emotes.current() == Emote.SLAPEN || guh.isPassenger()) {
            return false;
        }
        SamenSpel.kijk(guh, owner.position());
        if (guh.emotes.start(Emote.KNUFFELDANSJE, false, GuhEmotes.Source.SELF)) {
            guh.emotes.setLookTarget(owner.getUUID());
        }
        level.playSound(null, guh.blockPosition(), SamenFeature.WELKOM.get(), SoundSource.NEUTRAL, 1f, 1f);
        level.playSound(null, guh.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, guh.getVoicePitch() * 1.2f);
        level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 10, 0.5, 0.3, 0.5, 0.04);
        owner.displayClientMessage(Component.translatable("gui.guhs.samen.welkom", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        Band.geefHartjes(guh, owner, 5, Reden.OVERIG);
        GuhAdvancements.grant(owner, "samen_welkom");
        GidsFeature.grant(owner, "lieve_vadsjes/samen_welkom");
        if (Dagboek.eersteKeer(guh, owner, "samen_welkom")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.welkom", owner.getGameProfile().getName());
        }
        return true;
    }

    /** Does it thunder where this guh is (or in a test)? */
    static boolean onweer(GuhEntity guh) {
        return TEST_ONWEER.contains(guh.getUUID()) || (guh.level().isThundering() && guh.level().canSeeSky(guh.blockPosition().above()))
                || (guh.level().isThundering() && guh.level().isRainingAt(guh.getOwner() != null ? guh.getOwner().blockPosition().above() : guh.blockPosition()));
    }

    /** The cuddle in the storm is done (and told once per storm). */
    static void onweerKnuffel(GuhEntity guh, ServerPlayer owner) {
        knuffel(guh, owner);
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.1, guh.getZ(), 4, 0.3, 0.1, 0.3, 0.02);
        }
        long nu = guh.level().getGameTime();
        Long laatst = ONWEER_MELDING.get(owner.getUUID());
        if (laatst == null || nu - laatst > 2400) {
            ONWEER_MELDING.put(owner.getUUID(), nu);
            owner.displayClientMessage(Component.translatable("gui.guhs.samen.onweer", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            Band.geefHartjes(guh, owner, 2, Reden.KNUFFELEN);
            GuhAdvancements.grant(owner, "samen_onweer");
            if (Dagboek.eersteKeer(guh, owner, "samen_onweer")) {
                Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.onweer", owner.getGameProfile().getName());
            }
        }
    }

    // =====================================================================================================================
    // welterusten: waving when you go to bed
    // =====================================================================================================================

    static void spelerTick(ServerPlayer player) {
        boolean slaapt = player.isSleeping();
        Boolean was = SLAAPT.put(player.getUUID(), slaapt);
        if (slaapt && (was == null || !was)) {
            welterusten(player);
        }
    }

    /** Your guhs close by wave you goodnight. Returns how many did. */
    public static int welterusten(ServerPlayer player) {
        int n = 0;
        GuhEntity eerste = null;
        for (GuhEntity guh : Band.samenGuhs(player, 16)) {
            if (!guh.isAlive() || guh.isPassenger() || guh.emotes.current() == Emote.SLAPEN || GuhHooks.isBezig(guh)) {
                continue;
            }
            SamenSpel.kijk(guh, player.position());
            if (guh.emotes.start(Emote.ZWAAIEN, false, GuhEmotes.Source.SELF)) {
                guh.emotes.setLookTarget(player.getUUID());
                n++;
                if (eerste == null) {
                    eerste = guh;
                }
            }
        }
        if (eerste != null) {
            player.displayClientMessage(Component.translatable(n > 1 ? "gui.guhs.samen.welterusten_meer" : "gui.guhs.samen.welterusten",
                    eerste.getDisplayName(), n - 1).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            GuhAdvancements.grant(player, "samen_welterusten");
            if (Dagboek.eersteKeer(eerste, player, "samen_welterusten")) {
                Dagboek.wistJeDat(eerste, "gui.guhs.wistjedat.samen.welterusten", player.getGameProfile().getName());
            }
        }
        return n;
    }

    // =====================================================================================================================
    // the bff-knuffel
    // =====================================================================================================================

    /** The band bus: a guh started the BFF_KNUFFEL emote. */
    static void moment(Mob mob, @Nullable ServerPlayer speler, Moment m, String waarde) {
        if (m == Moment.EMOTE && Emote.BFF_KNUFFEL.id().equals(waarde) && mob instanceof GuhEntity guh) {
            ServerPlayer owner = Band.eigenaarOnline(guh);
            if (owner != null && owner.level() == guh.level() && owner.distanceTo(guh) < 8) {
                bff(guh, owner);
            }
        }
    }

    /** The guh stands right in front of its owner, they hug, a big heart above both. */
    static void bff(GuhEntity guh, ServerPlayer owner) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 plek = voor(owner, 0.6 + guh.getBbWidth() * 0.5);
        if (guh.distanceTo(owner) > 1.2 + guh.getBbWidth() && vrij(level, guh, plek)) {
            guh.moveTo(plek.x, plek.y, plek.z, guh.getYRot(), 0);   // a happy hop into your arms
            level.sendParticles(ParticleTypes.POOF, plek.x, plek.y + 0.2, plek.z, 4, 0.2, 0.1, 0.2, 0.01);
        }
        SamenSpel.kijk(guh, owner.position());
        guh.emotes.setLookTarget(owner.getUUID());
        for (ServerPlayer p : level.players()) {   // (the big heart above you both: drawn by every client close by)
            if (p.distanceToSqr(guh) < 64 * 64) {
                ModNetworking.sendTo(p, new SamenPayloads.Bff(guh.getId(), owner.getId()));
            }
        }
        owner.displayClientMessage(Component.translatable("gui.guhs.samen.bff", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        long nu = level.getGameTime();
        if (guh.getPersistentData().getLong(BFF_TOT) <= nu) {
            guh.getPersistentData().putLong(BFF_TOT, nu + 600);
            Band.geefHartjes(guh, owner, Reden.KNUFFELEN.standaard(), Reden.KNUFFELEN);
            Band.moment(guh, owner, Moment.GEKNUFFELD, "");
        }
        GidsFeature.grant(owner, "lieve_vadsjes/samen_bff_knuffel");
        if (Dagboek.eersteKeer(guh, owner, "samen_bff_knuffel")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.bff_knuffel", owner.getGameProfile().getName());
        }
    }

    // =====================================================================================================================
    // the goal: walking to you (troost, onweer)
    // =====================================================================================================================

    /**
     * Priority 4: in a thunderstorm the guh walks to its owner and cuddles against them (a hug every ~10 seconds while it
     * thunders). Not while asleep, sitting, riding or busy.
     */
    public static final class ReactieGoal extends Goal {
        private final GuhEntity guh;
        @Nullable
        private ServerPlayer owner;
        private int tijd;

        public ReactieGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean mag() {
            return Band.isBandGuh(guh) && !guh.level().isClientSide && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isLeashed()
                    && !Huisjes.isBinnen(guh) && !GuhHooks.isBezig(guh) && guh.emotes.current() != Emote.SLAPEN;
        }

        @Override
        public boolean canUse() {
            if ((guh.tickCount + guh.getId()) % 20 != 0 || !mag() || guh.getPersistentData().getLong(ONWEER_TOT) > guh.level().getGameTime()
                    || !onweer(guh)) {
                return false;
            }
            ServerPlayer p = Band.eigenaarOnline(guh);
            if (p == null || p.level() != guh.level() || p.distanceTo(guh) > 14 || p.isSleeping() || p.isSpectator()) {
                return false;
            }
            owner = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && tijd < 200 && mag() && owner.isAlive() && owner.level() == guh.level() && owner.distanceTo(guh) < 20;
        }

        @Override
        public void start() {
            tijd = 0;
        }

        @Override
        public void stop() {
            guh.getNavigation().stop();
            guh.getPersistentData().putLong(ONWEER_TOT, guh.level().getGameTime() + 200);
            owner = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            tijd++;
            ServerPlayer p = owner;
            if (p == null) {
                return;
            }
            guh.getLookControl().setLookAt(p, 30f, 30f);
            if (guh.distanceTo(p) > 1.3 + guh.getBbWidth() * 0.6) {
                if (tijd % 10 == 1) {
                    guh.getNavigation().moveTo(p, 1.25);
                }
                return;
            }
            guh.getNavigation().stop();
            onweerKnuffel(guh, p);
            tijd = 200;   // done: the next one in a while
        }
    }

    /** (Tests) forget everything. */
    static void wis() {
        GESTORVEN.clear();
        SLAAPT.clear();
        ONWEER_MELDING.clear();
        TEST_ONWEER.clear();
    }

    static BlockPos blok(Vec3 v) {
        return BlockPos.containing(v);
    }
}
