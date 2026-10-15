package nl.juiced.guhs.feature.einde2.zwevers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.world.WildeDieren;

/**
 * einde2, slice zwevers: where the Leegtewalvisguh comes from. A gentle top-up for every player who has the flag of
 * {@link Walviszicht} and is out among the outer islands (beyond {@link Einde2Ids#STRAAL_MIDDEN} blocks from 0,0): every
 * {@link #TIJD} ticks, 1 in {@link #KANS}, a whale drifts in {@link #MIN_AFSTAND}-{@link #MAX_AFSTAND} blocks away, unless one
 * is within {@link #BEREIK} blocks already. It comes and goes: never saved, and it leaves when nobody who sees it is near
 * ({@link LeegtewalvisguhEntity#checkDespawn}). For a player without the flag nothing ever comes.
 */
public final class Leegtewalvissen {
    public static final int TIJD = 600, KANS = 3;
    /** At most one whale within this many blocks of a player who sees it. */
    public static final int BEREIK = 256;
    public static final int MIN_AFSTAND = 72, MAX_AFSTAND = 120;
    /**
     * einde2 phase B (reizen): may a whale come for this player by itself (besides the flag)? The whale questline keeps the
     * first sighting for its own moment: a player who believed the captain sees no whale before the one that rises for them.
     */
    public static volatile java.util.function.Predicate<ServerPlayer> MAG_KOMEN = p -> true;

    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || level.getGameTime() % TIJD != 250) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (level.getRandom().nextInt(KANS) == 0) {
                aanvullen(level, player, level.getRandom());
            }
        }
    }

    /** May a whale come for a player here, with the nearest one this far away (negative: there is none)? */
    public static boolean magErbij(boolean ziet, double x, double z, double dichtsteWalvis) {
        return ziet && x * x + z * z > (double) Einde2Ids.STRAAL_MIDDEN * Einde2Ids.STRAAL_MIDDEN && (dichtsteWalvis < 0 || dichtsteWalvis > BEREIK);
    }

    /** How far the nearest whale is from this spot (-1: there is none in the level). */
    public static double dichtste(ServerLevel level, Vec3 plek) {
        double best = -1;
        for (LeegtewalvisguhEntity w : LeegtewalvisguhEntity.alle(level)) {
            double d = Math.sqrt(w.distanceToSqr(plek));
            best = best < 0 ? d : Math.min(best, d);
        }
        return best;
    }

    /** One whale for this player, when the rules above allow it and the spot is free. Or null. */
    @Nullable
    public static LeegtewalvisguhEntity aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (!MAG_KOMEN.test(player) || !magErbij(Walviszicht.ziet(player), player.getX(), player.getZ(), dichtste(level, player.position()))) {
            return null;
        }
        double a = random.nextDouble() * Math.PI * 2, d = MIN_AFSTAND + random.nextInt(MAX_AFSTAND - MIN_AFSTAND + 1);
        double x = player.getX() + Math.cos(a) * d, z = player.getZ() + Math.sin(a) * d;
        int y = Mth.clamp(player.getBlockY() - 24 + random.nextInt(36), LeegtewalvisguhEntity.MIN_Y, LeegtewalvisguhEntity.MAX_Y);
        // it heads past the player, some forty blocks to one side
        Vec3 langs = new Vec3(player.getX() - Math.sin(a) * 40, y, player.getZ() + Math.cos(a) * 40);
        float koers = (float) (Mth.atan2(langs.z - z, langs.x - x) * Mth.RAD_TO_DEG) - 90f;
        return op(level, new Vec3(x, y, z), koers);
    }

    /** A come-and-go whale on this spot, heading this way, when the spot and what lies ahead of it are free. Or null. */
    @Nullable
    public static LeegtewalvisguhEntity op(ServerLevel level, Vec3 plek, float koers) {
        if (!level.isPositionEntityTicking(BlockPos.containing(plek))) {
            return null;
        }
        LeegtewalvisguhEntity walvis = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (walvis == null) {
            return null;
        }
        walvis.snapTo(plek.x, plek.y, plek.z, koers, 0);
        Vec3 f = LeegtewalvisguhEntity.vooruit(koers);
        for (int d : new int[]{-24, 0, 24, 48}) {
            if (walvis.inDeWeg(level, plek.x + f.x * d, plek.z + f.z * d)) {
                walvis.discard();
                return null;
            }
        }
        walvis.setYBodyRot(koers);
        walvis.setYHeadRot(koers);
        WildeDieren.markeer(walvis);
        level.addFreshEntity(walvis);
        return walvis;
    }

    private Leegtewalvissen() {
    }
}
