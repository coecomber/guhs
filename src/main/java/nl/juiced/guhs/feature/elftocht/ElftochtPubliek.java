package nl.juiced.guhs.feature.elftocht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.guhpolder.PinguhMeeglijden;

/**
 * The audience along the canal (guhs with the tag {@link #TAG}, placed by the templates): they stay near their spot on
 * the bank, and when a skater glides past they cheer - a VAHOEG jump ("VAHOEG!" pops up above them), a wave or a
 * happy dance, and a cheer you can hear. Each guh then catches its breath for a few seconds.
 * <p>
 * Also: a tamed Pinguh of a skater belly-slides along with its owner during the tour ({@link #pinguhs}).
 */
public final class ElftochtPubliek {
    public static final String TAG = "guhs_elftocht_publiek";
    public static final double JUICH_AFSTAND = 12;
    private static final String THUIS = "guhs_elftocht_thuis", RUST = "guhs_elftocht_rust";
    private static final Emote[] JUICHEN = {Emote.VAHOEG, Emote.ZWAAIEN, Emote.VAHOEG, Emote.DANSEN};

    /** (GuhHooks.tick, every guh, server side) */
    public static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 10 != 0 || !guh.entityTags().contains(TAG) || guh.isTame()) {
            return;
        }
        var data = guh.getPersistentData();
        if (!data.contains(THUIS)) {
            data.putLong(THUIS, guh.blockPosition().asLong());
        }
        BlockPos thuis = BlockPos.of(data.getLongOr(THUIS, 0L));
        if (guh.blockPosition().distSqr(thuis) > 9 && guh.getNavigation().isDone()) {
            guh.getNavigation().moveTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, 0.9);
        }
        long nu = guh.level().getGameTime();
        if (nu < data.getLongOr(RUST, 0L)) {
            return;
        }
        Player schaatser = schaatserBij(guh);
        if (schaatser != null) {
            juich(guh, schaatser);
        }
    }

    /** A skater gliding past, near enough to cheer for. */
    static Player schaatserBij(GuhEntity guh) {
        List<Player> spelers = guh.level().getEntitiesOfClass(Player.class, guh.getBoundingBox().inflate(JUICH_AFSTAND, 4, JUICH_AFSTAND),
                p -> !p.isSpectator() && (ElftochtTocht.isBezig(p) || ElftochtSchaatsen.houdtSchaatsen(p))
                        && p.getDeltaMovement().horizontalDistanceSqr() > 0.004);
        return spelers.isEmpty() ? null : spelers.get(0);
    }

    /** Cheer for this skater: an emote, "VAHOEG!", a cheer; then rest for 4-7 seconds. */
    public static void juich(GuhEntity guh, Player schaatser) {
        var level = guh.level();
        guh.getLookControl().setLookAt(schaatser, 30f, 30f);
        Emote emote = JUICHEN[level.getRandom().nextInt(JUICHEN.length)];
        if (!guh.emotes.start(emote, false, GuhEmotes.Source.SELF) && guh.onGround()) {
            guh.getJumpControl().jump();     // (can't do an emote now: at least a happy hop)
        }
        if (level.getRandom().nextInt(3) == 0) {
            level.playSound(null, guh.blockPosition(), ElftochtFeature.JUICH.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + guh.getBbHeight() + 0.4, guh.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
        }
        guh.getPersistentData().putLong(RUST, level.getGameTime() + 80 + level.getRandom().nextInt(60));
    }

    /**
     * Everyone cheers at once (a stamp, the finish): every audience guh within {@code afstand} blocks of {@code waar}
     * cheers for the skater, rested or not. Returns how many cheered.
     */
    public static int juichAllemaal(ServerLevel level, Vec3 waar, double afstand, Player schaatser) {
        List<GuhEntity> publiek = level.getEntitiesOfClass(GuhEntity.class, new AABB(waar, waar).inflate(afstand, 8, afstand),
                g -> g.entityTags().contains(TAG) && !g.isTame() && g.isAlive());
        for (GuhEntity g : publiek) {
            juich(g, schaatser);
        }
        if (!publiek.isEmpty()) {
            level.playSound(null, BlockPos.containing(waar), ElftochtFeature.JUICH.get(), SoundSource.NEUTRAL, 1f, 1f);
        }
        return publiek.size();
    }

    /** Has this audience guh cheered in the last seconds? (tests) */
    public static boolean heeftGejuicht(GuhEntity guh) {
        return guh.getPersistentData().getLongOr(RUST, 0L) > guh.level().getGameTime();
    }

    /** How far away a skater's tamed Pinguhs are fetched (a hop) to slide along. */
    public static final double PINGUH_HALEN = 48;

    /**
     * A skater's tamed Pinguhs come along (the Guhpolder's {@link PinguhMeeglijden}): the ones within
     * {@link #PINGUH_HALEN} blocks that are too far for it first hop to just behind the skater, then they all belly-slide
     * along beside the skater. Called every second while riding (newcomers join); {@link ElftochtTocht#stop} ends it.
     */
    public static List<GuhEntity> pinguhs(ServerPlayer player) {
        List<GuhEntity> ver = player.level().getEntitiesOfClass(GuhEntity.class, new AABB(player.blockPosition()).inflate(PINGUH_HALEN, 12, PINGUH_HALEN),
                g -> g.isTame() && g.isOwnedBy(player) && g.getVariant() == GuhVariant.PINGUH && !g.isOrderedToSit() && !g.isPassenger()
                        && !g.isLeashed() && !PinguhMeeglijden.glijdtMee(g) && g.distanceTo(player) > PinguhMeeglijden.BEREIK - 2);
        for (GuhEntity g : ver) {
            Vec3 achter = player.position().subtract(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
            if (player.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 0.3, g.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
            }
            g.teleportTo(achter.x, player.getY(), achter.z);
            g.getNavigation().stop();
        }
        return PinguhMeeglijden.start(player);
    }

    private ElftochtPubliek() {
    }
}
