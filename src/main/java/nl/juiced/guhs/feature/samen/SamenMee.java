package nl.juiced.guhs.feature.samen;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.elftocht.ElftochtTocht;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhpolder.PinguhMeeglijden;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knuffelbad.GlijRit;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadProtection;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceGuhEntity;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Your guh really joins in (2.10, samen):
 * <ul>
 *   <li><b>race / circuit</b>: at the start it hops on behind you in the kart (the race guh's second seat) and rides the
 *       whole race along, cheering every lap and at the finish; after the race it hops off where it was;</li>
 *   <li><b>Elf-Guhjestocht</b>: it skates next to you like a tamed Pinguh does (the Guhpolder's PinguhMeeglijden lets it
 *       in through {@link #magMeeSchaatsen}), with little snow sprays;</li>
 *   <li><b>Knuffelbad</b>: when you swim in the Knuffelbad it jumps in and swims along beside you ({@link ZwemGoal}), and
 *       while you slide down a glijbaan it cheers from the side.</li>
 * </ul>
 */
public final class SamenMee {
    /** Own guhs this close (to the racer, after the start teleport) hop on the kart. */
    public static final double KART_BEREIK = 48;
    /** Knuffelbad: the owner within this distance (and swimming) makes the guh swim along. */
    public static final double ZWEM_BEREIK = 20;

    /** A guh riding along in a kart: which guh, where it came from (it goes back there after the race). */
    record Mee(UUID guh, ResourceKey<Level> dim, Vec3 van, int ronde) {
    }

    /** racer -> the guh riding along. */
    private static final Map<UUID, Mee> KART = new ConcurrentHashMap<>();
    /** (Tests) skaters that count as being on the Elf-Guhjestocht; players swimming "in the Knuffelbad". */
    public static final Set<UUID> TEST_SCHAATSERS = ConcurrentHashMap.newKeySet(), TEST_ZWEMMERS = ConcurrentHashMap.newKeySet();

    private SamenMee() {
    }

    /** A guh that's free to join: not sitting, riding, leashed, living in a huisje or busy. */
    static boolean vrij(GuhEntity guh) {
        return guh.isAlive() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isVehicle() && !guh.isLeashed()
                && !Huisjes.isBewoner(guh) && !GuhHooks.isBezig(guh) && !guh.isNoAi();
    }

    // =====================================================================================================================
    // the race / circuit kart
    // =====================================================================================================================

    /** The listener on every race track (RaceBaan.luister). */
    public static final RaceBaan.Extra RACE = new RaceBaan.Extra() {
        @Override
        public void start(RaceGame game, ServerLevel level, ServerPlayer racer) {
            stapIn(game, level, racer);
        }

        @Override
        public void tick(RaceGame game, ServerLevel level, ServerPlayer racer, RaceGuhEntity mount) {
            Mee mee = KART.get(racer.getUUID());
            if (mee == null) {
                return;
            }
            if (!(level.getEntity(mee.guh()) instanceof GuhEntity guh) || guh.getVehicle() != mount) {
                return;
            }
            if (game.lap() != mee.ronde()) {                // a lap done: a happy squeak from the back seat
                KART.put(racer.getUUID(), new Mee(mee.guh(), mee.dim(), mee.van(), game.lap()));
                if (mee.ronde() >= 0) {
                    SamenSpel.juich(guh, racer, SamenSpel.Soort.GOED);
                }
            }
            if ((guh.tickCount & 15) == 0) {
                level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.15, guh.getZ(), 1, 0.15, 0.05, 0.15, 0.01);
            }
        }

        @Override
        public void finish(RaceGame game, ServerLevel level, ServerPlayer racer, int total, RaceGame.Medal medal, boolean record) {
            Mee mee = KART.get(racer.getUUID());
            if (mee != null && level.getEntity(mee.guh()) instanceof GuhEntity guh) {
                guh.getPersistentData().remove(SamenSpel.RUST_TOT);
                boolean goed = record || medal.ordinal() <= RaceGame.Medal.ZILVER.ordinal();
                SamenSpel.juich(guh, racer, record ? SamenSpel.Soort.RECORD : goed ? SamenSpel.Soort.GOED : SamenSpel.Soort.MIS);
                Band.geefHartjes(guh, racer, Reden.MINIGAME.standaard(), Reden.MINIGAME);
            }
        }

        @Override
        public void end(RaceGame game, ServerLevel level) {
            stapUit(level.getServer(), game.racer());
        }
    };

    /** The race starts: the racer's own guh with the most hearts, close by and free, hops on behind them. */
    static boolean stapIn(RaceGame game, ServerLevel level, ServerPlayer racer) {
        RaceGuhEntity mount = game.mount(level);
        if (mount == null || KART.containsKey(racer.getUUID())) {
            return false;
        }
        GuhEntity guh = level.getEntitiesOfClass(GuhEntity.class, racer.getBoundingBox().inflate(KART_BEREIK),
                        g -> Band.isBandGuh(g) && racer.getUUID().equals(g.getOwnerUUID()) && vrij(g))
                .stream().max(Comparator.comparingInt(Band::hartjes)).orElse(null);
        if (guh == null) {
            return false;
        }
        Vec3 van = guh.position();
        guh.getNavigation().stop();
        guh.emotes.stop();
        if (!guh.startRiding(mount)) {                    // (not forced: the race guh's second seat decides)
            return false;
        }
        KART.put(racer.getUUID(), new Mee(guh.getUUID(), level.dimension(), van, -1));
        level.sendParticles(ParticleTypes.POOF, van.x, van.y + 0.3, van.z, 6, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(BandFeature.HARTJE.get(), mount.getX(), mount.getY() + mount.getBbHeight() + 0.5, mount.getZ(), 5, 0.4, 0.2, 0.4, 0.02);
        racer.sendSystemMessage(Component.translatable("gui.guhs.samen.kart", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(racer, "samen_kart");
        GidsFeature.grant(racer, "lieve_vadsjes/samen_kart");
        if (Dagboek.eersteKeer(guh, racer, "samen_kart")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.kart", racer.getGameProfile().name());
        }
        return true;
    }

    /** The race is over: the guh hops off, back to where it was (a little poof). */
    static void stapUit(net.minecraft.server.MinecraftServer server, @Nullable UUID racer) {
        Mee mee = racer == null ? null : KART.remove(racer);
        if (mee == null) {
            return;
        }
        ServerLevel level = server.getLevel(mee.dim());
        if (level == null || !(level.getEntity(mee.guh()) instanceof GuhEntity guh)) {
            return;
        }
        if (guh.getVehicle() instanceof RaceGuhEntity) {
            guh.stopRiding();
        }
        guh.teleportTo(mee.van().x, mee.van().y, mee.van().z);
        guh.setDeltaMovement(Vec3.ZERO);
        guh.resetFallDistance();
        level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.3, guh.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
    }

    /** 1.2.7: the racer left the kart (RaceGuhEntity#removePassenger): the guh riding along goes back to where it was, now. */
    public static void uitDeKart(ServerPlayer racer) {
        stapUit(racer.level().getServer(), racer.getUUID());
    }

    /** The guh riding along with this racer (tests), or null. */
    @Nullable
    public static UUID inKart(ServerPlayer racer) {
        Mee mee = KART.get(racer.getUUID());
        return mee == null ? null : mee.guh();
    }

    /** A guh that still sits in a race guh that has no race (any more), or isn't ours: off it goes. */
    static void opruimen(GuhEntity guh) {
        if (guh.getVehicle() instanceof RaceGuhEntity kart && (guh.tickCount & 7) == 0 && !guh.level().isClientSide()
                && KART.values().stream().noneMatch(m -> m.guh().equals(guh.getUUID()))) {
            guh.stopRiding();
            guh.snapTo(kart.getX(), kart.getY(), kart.getZ());
        }
    }

    // =====================================================================================================================
    // Elf-Guhjestocht: skating along
    // =====================================================================================================================

    /** PinguhMeeglijden.ookMee: your own band guh skates along while you are on the ice of the Elf-Guhjestocht. */
    static boolean magMeeSchaatsen(GuhEntity guh, ServerPlayer skater) {
        return Band.isBandGuh(guh) && skater.getUUID().equals(guh.getOwnerUUID()) && !Huisjes.isBewoner(guh) && !GuhHooks.isBezig(guh)
                && (ElftochtTocht.isBezig(skater) || TEST_SCHAATSERS.contains(skater.getUUID()));
    }

    // =====================================================================================================================
    // every band guh
    // =====================================================================================================================

    static void tick(GuhEntity guh) {
        opruimen(guh);
        int t = guh.tickCount + guh.getId();
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        // skating along (not a Pinguh: they have their own belly slide): snow sprays, the advancement
        if (guh.getVariant() != GuhVariant.PINGUH && PinguhMeeglijden.glijdtMee(guh)) {
            if (t % 5 == 0 && guh.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, guh.getX(), guh.getY() + 0.1, guh.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
            }
            if (t % 100 == 0 && Band.eigenaarOnline(guh) instanceof ServerPlayer owner) {
                GuhAdvancements.grant(owner, "samen_schaatsen");
                GidsFeature.grant(owner, "lieve_vadsjes/samen_schaatsen");
                if (Dagboek.eersteKeer(guh, owner, "samen_schaatsen")) {
                    Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.schaatsen", owner.getGameProfile().name());
                }
                Band.geefHartjes(guh, owner, 1, Reden.MINIGAME);
            }
        }
        // the Knuffelbad: cheering from the side while you slide down
        if (t % 20 == 0) {
            ServerPlayer owner = Band.eigenaarOnline(guh);
            if (owner != null && owner.level() == guh.level() && GlijRit.rijdt(owner) && owner.distanceTo(guh) < 40
                    && !guh.isPassenger() && guh.getRandom().nextInt(3) == 0) {
                SamenSpel.juich(guh, owner, SamenSpel.Soort.GOED);
            }
        }
    }

    // =====================================================================================================================
    // the Knuffelbad: swimming along
    // =====================================================================================================================

    /** Is this owner swimming in the Knuffelbad (so their guh jumps in)? */
    static boolean zwemtInBad(ServerPlayer owner) {
        return TEST_ZWEMMERS.contains(owner.getUUID())
                || (owner.isInWater() && !owner.isPassenger() && KnuffelbadProtection.beschermd(owner.level(), owner.blockPosition()));
    }

    /**
     * Swims along beside its owner in the Knuffelbad (priority 3: above following and wandering). On land it walks (and
     * hops) into the water first; far behind it hops in with a splash. Happy splashes, and the first time: the dagboek.
     */
    public static final class ZwemGoal extends Goal {
        private final GuhEntity guh;
        @Nullable
        private ServerPlayer owner;
        private int samen, vast;

        public ZwemGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if ((guh.tickCount + guh.getId()) % 10 != 0 || !Band.isBandGuh(guh) || !vrij(guh) || guh.level().isClientSide()) {
                return false;
            }
            ServerPlayer p = Band.eigenaarOnline(guh);
            if (p == null || p.level() != guh.level() || p.distanceTo(guh) > ZWEM_BEREIK || !zwemtInBad(p)) {
                return false;
            }
            owner = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && owner.level() == guh.level() && zwemtInBad(owner) && owner.distanceTo(guh) <= ZWEM_BEREIK * 1.5
                    && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isLeashed();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            samen = 0;
            vast = 0;
            guh.emotes.stop();
        }

        @Override
        public void stop() {
            owner = null;
            guh.getNavigation().stop();
        }

        /** The spot beside the owner (on their right, a little behind). */
        static Vec3 plek(ServerPlayer p) {
            Vec3 kijk = Vec3.directionFromRotation(0, p.getYRot());
            Vec3 rechts = new Vec3(-kijk.z, 0, kijk.x);
            return p.position().add(rechts.scale(1.4)).add(kijk.scale(-0.4));
        }

        @Override
        public void tick() {
            ServerPlayer p = owner;
            if (p == null || !(guh.level() instanceof ServerLevel level)) {
                return;
            }
            Vec3 doel = plek(p);
            guh.getLookControl().setLookAt(p, 20f, 20f);
            double afstand = guh.position().distanceTo(doel);
            if (afstand > 12 || vast > 60) {                   // far behind (or no way in): a hop into the water with a splash
                vast = 0;
                level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.3, guh.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                guh.snapTo(doel.x, p.getY(), doel.z, p.getYRot(), 0);
                guh.setDeltaMovement(Vec3.ZERO);
                level.sendParticles(ParticleTypes.SPLASH, guh.getX(), guh.getY() + 0.5, guh.getZ(), 20, 0.4, 0.1, 0.4, 0.1);
                return;
            }
            if (guh.isInWater()) {
                guh.getNavigation().stop();
                Vec3 naar = doel.subtract(guh.position());
                Vec3 v = new Vec3(naar.x, 0, naar.z);
                if (v.lengthSqr() > 0.04) {
                    v = v.normalize().scale(Math.min(0.22, 0.06 + v.length() * 0.08));
                    float yaw = (float) (Math.atan2(v.z, v.x) * (180F / Math.PI)) - 90f;
                    guh.setYRot(yaw);
                    guh.yBodyRot = yaw;
                } else {
                    v = Vec3.ZERO;
                }
                double up = guh.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > guh.getBbHeight() * 0.45 ? 0.04 : 0.0;
                guh.setDeltaMovement(v.x, Math.max(guh.getDeltaMovement().y, up), v.z);
                if (++samen % 40 == 0) {
                    level.sendParticles(ParticleTypes.SPLASH, guh.getX(), guh.getY() + guh.getBbHeight() * 0.6, guh.getZ(), 8, 0.3, 0.05, 0.3, 0.05);
                    level.sendParticles(ParticleTypes.BUBBLE, guh.getX(), guh.getY() + 0.2, guh.getZ(), 4, 0.2, 0.1, 0.2, 0.02);
                }
                if (samen == 60 && p.distanceTo(guh) < 5) {
                    gezwommen(guh, p);
                }
            } else {
                if (tickCount(guh) % 10 == 0 && !guh.getNavigation().moveTo(doel.x, doel.y, doel.z, 1.2)) {
                    vast += 10;
                }
                vast++;
            }
        }
    }

    /** Swimming together for a moment: the advancement, a heart, the dagboek the first time. */
    static void gezwommen(GuhEntity guh, ServerPlayer p) {
        GuhAdvancements.grant(p, "samen_zwemmen");
        GidsFeature.grant(p, "lieve_vadsjes/samen_zwemmen");
        Band.geefHartjes(guh, p, 2, Reden.MINIGAME);
        if (Dagboek.eersteKeer(guh, p, "samen_zwemmen")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.zwemmen", p.getGameProfile().name());
            p.sendOverlayMessage(Component.translatable("gui.guhs.samen.zwemmen", guh.getDisplayName()).withStyle(ChatFormatting.AQUA));
        }
    }

    private static int tickCount(GuhEntity guh) {
        return guh.tickCount + guh.getId();
    }

    /** (Tests) this guh rides along with this racer (as if the race had put it there); null guh: not any more. */
    static void testKart(ServerPlayer racer, @Nullable GuhEntity guh) {
        if (guh == null) {
            KART.remove(racer.getUUID());
        } else {
            KART.put(racer.getUUID(), new Mee(guh.getUUID(), guh.level().dimension(), guh.position(), -1));
        }
    }

    /** (Tests) forget the karts. */
    static void wis() {
        KART.clear();
        TEST_SCHAATSERS.clear();
        TEST_ZWEMMERS.clear();
    }

    static BlockPos pos(Vec3 v) {
        return BlockPos.containing(v);
    }
}
