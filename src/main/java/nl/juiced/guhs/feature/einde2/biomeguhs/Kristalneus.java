package nl.juiced.guhs.feature.einde2.biomeguhs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2, slice biomeguhs: a tamed Kristalguh has a nose for anything that glitters. Now and then, with its owner around,
 * it sniffs up one {@code guhs:kristalkaas_brokje} and drops it at its feet: at most one per Minecraft day per guh (the
 * day is kept on the guh, {@link #DAG}), so a herd of them is a trickle, never a farm.
 */
public final class Kristalneus {
    /** Persistent key: the day (game time / 24000) it last found one, plus one (0: never). */
    public static final String DAG = "guhs_einde2_biomeguhs_brokdag";
    /** It looks every this many ticks; 1 in {@link #KANS} looks finds one (on average within the first quarter of a day). */
    public static final int STAP = 200, KANS = 20;
    /** Its owner must be within this many blocks. */
    public static final double BAAS_BIJ = 16;

    /** Today (the level's game time in Minecraft days). */
    public static long vandaag(GuhEntity guh) {
        return guh.level().getGameTime() / 24000L;
    }

    /** May this guh find one now: tamed, awake, on the ground, its owner near, and none found yet today? */
    public static boolean magVinden(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.KRISTALGUH && Trucs.tam(guh) && guh.onGround() && !guh.isPassenger() && !guh.isBaby()
                && !GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT) && Trucs.baasBij(guh, BAAS_BIJ) != null
                && guh.getPersistentData().getLongOr(DAG, 0L) != vandaag(guh) + 1;
    }

    /** Finds one now, when it may: the brokje at its feet, sparkles, a happy guh. Returns whether it did. */
    public static boolean vind(GuhEntity guh) {
        if (!magVinden(guh) || !(guh.level() instanceof ServerLevel level)) {
            return false;
        }
        guh.getPersistentData().putLong(DAG, vandaag(guh) + 1);
        ItemEntity brokje = new ItemEntity(level, guh.getX(), guh.getY() + 0.3, guh.getZ(), new ItemStack(E2BiomesFeature.KRISTALKAAS_BROKJE.get()));
        brokje.setDeltaMovement(0, 0.2, 0);
        brokje.setPickUpDelay(20);
        level.addFreshEntity(brokje);
        level.sendParticles(ParticleTypes.WAX_ON, guh.getX(), guh.getY() + 0.4, guh.getZ(), 14, 0.35, 0.3, 0.35, 0.0);
        Trucs.geluid(guh, E2BiomeguhsFeature.KRISTALGUH_VONDST.get(), 0.9f, 1f);
        guh.triggerAnim("action", "happy");
        ServerPlayer baas = Trucs.baas(guh);
        if (baas != null) {
            Trucs.meld(baas, guh, "brokje");
            GuhAdvancements.grant(baas, "einde2_biomeguhs_brokje");
        }
        return true;
    }

    /** (GuhHooks.tick, server) */
    static void serverTick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % STAP == 0 && guh.getRandom().nextInt(KANS) == 0) {
            vind(guh);
        }
    }

    private Kristalneus() {
    }
}
