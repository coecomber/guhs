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
            licht(guh.level(), lichtPlek(guh));
        }
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
