package nl.juiced.guhs.feature.einde2.biomeguhs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2, slice biomeguhs: a tamed Schimmelguh glows like a lamp ({@link E2Vlaggen#TRUC}: the client lights up its veins
 * too). The light is real: the invisible block {@code guhs:schimmelguh_licht} ({@link SchimmelguhLichtBlock}) is put in
 * the air where its head is and takes itself away again within a second when no glowing Schimmelguh is next to it, so it
 * follows the guh and nothing is ever left behind. It only ever replaces air. Asleep, the lamp is out.
 */
public final class Schimmellamp {
    /** How often a lamp looks where its light should be (ticks). */
    public static final int STAP = 5;
    /** How far (blocks, counted along the axes) the guh's head may be from its light before the light moves along. */
    public static final int SPELING = 2;

    public static boolean gloeit(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.SCHIMMELGUH && Trucs.doet(guh);
    }

    /** Where this guh's light belongs: the block its head is in. */
    public static BlockPos lichtPlek(GuhEntity guh) {
        return BlockPos.containing(guh.getX(), guh.getY() + guh.getBbHeight() * 0.6, guh.getZ());
    }

    /** (GuhHooks.tick, server) */
    static void serverTick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % STAP != 0) {
            return;
        }
        boolean aan = Trucs.tam(guh) && !GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)
                && guh.emotes.current() != nl.juiced.guhs.feature.emotes.Emote.SLAPEN;
        if (aan != Trucs.doet(guh)) {
            Trucs.zet(guh, aan);
            if (aan) {
                Trucs.geluid(guh, E2BiomeguhsFeature.SCHIMMELGUH_GLOEI.get(), 0.5f, 1.2f);
                ServerPlayer baas = Trucs.baasBij(guh, 16);
                if (baas != null) {
                    GuhAdvancements.grant(baas, "einde2_biomeguhs_gloei");
                }
            }
        }
        if (aan) {
            schijn(guh);
        }
    }

    // (server: where each glowing guh's light block is)
    private static final java.util.Map<GuhEntity, BlockPos> LICHT_VAN = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * The guh's light: it stays where it is for as long as the guh's head is within {@link #SPELING} blocks of it, and only
     * then moves along. (Every move is a light block placed and one removed, and each of those makes the game work the
     * light out again around it and every client in range redraw: a walking guh would do that for every block it walks.)
     */
    static void schijn(GuhEntity guh) {
        BlockPos plek = lichtPlek(guh);
        if (lichtVan(guh) != null) {
            return;
        }
        if (licht(guh.level(), plek)) {
            LICHT_VAN.put(guh, plek);
        }
    }

    /** Server: where this guh's light block is right now (null: it has none within {@link #SPELING} blocks of its head). */
    @javax.annotation.Nullable
    public static BlockPos lichtVan(GuhEntity guh) {
        BlockPos oud = LICHT_VAN.get(guh);
        return oud != null && oud.distManhattan(lichtPlek(guh)) <= SPELING && guh.level().isLoaded(oud)
                && guh.level().getBlockState(oud).is(E2BiomeguhsFeature.SCHIMMELGUH_LICHT.get()) ? oud : null;
    }

    /** Puts the light here when this is plain air (the block ticks itself away when the guh has gone). */
    static boolean licht(Level level, BlockPos pos) {
        BlockState hier = level.getBlockState(pos);
        if (hier.is(E2BiomeguhsFeature.SCHIMMELGUH_LICHT.get())) {
            return true;
        }
        if (!hier.isAir() || !level.isLoaded(pos)) {
            return false;
        }
        return level.setBlock(pos, E2BiomeguhsFeature.SCHIMMELGUH_LICHT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private Schimmellamp() {
    }
}
