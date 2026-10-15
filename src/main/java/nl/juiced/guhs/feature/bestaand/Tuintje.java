package nl.juiced.guhs.feature.bestaand;

import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (bestaand): tidying the pindasaus-tuintje of the Spiesburcht, per player. While a player is at the step {@link
 * BestaandFeature#WACHTER_TUIN} of the Wachter-guh's questline, six tufts of Mikakruid stand on the paths of the tuintje
 * ({@link Plekken#MIKAKRUID}) FOR THAT PLAYER ONLY ({@link Schijn}): the spots are air in the world. A click on a tuft
 * (either button) pulls it: the flag {@code kruid_<i>}. The sixth brings the player to the next step. A spot where
 * somebody built something needs no weeding: it counts as pulled.
 */
public final class Tuintje {
    public static final int AANTAL = Plekken.MIKAKRUID.size();

    private Tuintje() {
    }

    public static boolean gewied(ServerPlayer p, int i) {
        return BestaandFeature.WACHTER.vlag(p, "kruid_" + i);
    }

    /** How many of the tufts this player pulled. */
    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < AANTAL; i++) {
            n += gewied(p, i) ? 1 : 0;
        }
        return n;
    }

    /** The tuft whose spot this world position is, in the Spiesburcht around it (-1: none). */
    public static int bij(ServerLevel level, BlockPos pos) {
        StructureStart start = Bezetting.start(level, BestaandFeature.SPIESBURCHT, pos);
        BlockPos lokaal = start == null ? null : Kopieen.lokaal(start, null, pos);
        return lokaal == null ? -1 : Plekken.MIKAKRUID.indexOf(lokaal);
    }

    /** A click of this player on this block: true when it pulled a tuft of Mikakruid. */
    static boolean klik(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (BestaandFeature.WACHTER.stap(p) != BestaandFeature.WACHTER_TUIN || !Bezetting.leeg(level.getBlockState(pos))) {
            return false;
        }
        int i = bij(level, pos);
        return i >= 0 && wied(p, i, pos, true);
    }

    /**
     * This player pulls tuft i (only at the weeding step, each tuft once): the flag, and with the last one the next step.
     * {@code luid}: with the rustle, the leaves and the count on the screen. True when it was pulled now.
     */
    public static boolean wied(ServerPlayer p, int i, BlockPos pos, boolean luid) {
        if (BestaandFeature.WACHTER.stap(p) != BestaandFeature.WACHTER_TUIN || i < 0 || i >= AANTAL || gewied(p, i)) {
            return false;
        }
        BestaandFeature.WACHTER.vlag(p, "kruid_" + i, true);
        int n = aantal(p);
        Schijn.toon(p, pos, p.level().getBlockState(pos));
        if (luid) {
            Schijn.geluid(p, SoundEvents.SWEET_BERRY_BUSH_BREAK, pos, 1.0f, 0.8f + p.getRandom().nextFloat() * 0.3f);
            Schijn.deeltjes(p, new BlockParticleOption(ParticleTypes.BLOCK, BestaandFeature.MIKAKRUID.get().defaultBlockState()),
                    Vec3.atCenterOf(pos), 12, 0.2, 0.05);
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kruid.weg", n, AANTAL).withStyle(ChatFormatting.GREEN));
        }
        if (n >= AANTAL && BestaandFeature.WACHTER.verder(p, BestaandFeature.WACHTER_TUIN)) {
            GuhQuests.hint(p, "gui.guhs.bestaand.kruid.alle");
        }
        return true;
    }

    /** What a player at the weeding step sees: the tufts they did not pull yet, in the Spiesburcht they are at. */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (BestaandFeature.WACHTER.stap(p) != BestaandFeature.WACHTER_TUIN) {
            return;
        }
        ServerLevel level = p.level();
        StructureStart start = Bezetting.start(level, BestaandFeature.SPIESBURCHT, p.blockPosition());
        if (start == null) {
            return;
        }
        BlockState kruid = BestaandFeature.MIKAKRUID.get().defaultBlockState();
        for (int i = 0; i < AANTAL; i++) {
            BlockPos pos = gewied(p, i) ? null : Kopieen.wereld(start, null, Plekken.MIKAKRUID.get(i));
            if (pos == null || !Schijn.dichtbij(p, pos)) {
                continue;
            }
            if (Bezetting.leeg(level.getBlockState(pos))) {
                gewenst.put(pos, kruid);
            } else {
                wied(p, i, pos, false);   // (somebody built there: nothing to weed)
            }
        }
    }
}
