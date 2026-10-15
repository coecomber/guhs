package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * einde2, slice geestenwereld: the puzzle before the gate of the Witte Lotus-ruines. Nine lotuspuzzeltegels in the floor
 * (markers {@code tegel_1} .. {@code tegel_9}, row by row from the north-west) carry together one big white lotus: four
 * corners, four edges and the middle. Each has to lie at its own turn ({@link #GOED}; the middle is round: any turn).
 * The world is shared, so the tiles are too: whoever lays the last one right solves it for everyone who is there.
 * <p>
 * hook fase B: {@link #OPGELOST} (the gate opens for the player, the story goes on); phase A only says so.
 */
public final class Lotuspuzzel {
    /** The right turn of tegel_1 .. tegel_9 (-1: any). The same table as tools/features/einde2_geestenwereld_bouw.TEGELS. */
    public static final int[] GOED = {0, 0, 1, 3, -1, 1, 3, 2, 2};
    /** Called when a player lays the last tile right (hook fase B: open the gate for this player). */
    public static final List<BiConsumer<ServerPlayer, ServerLevel>> OPGELOST = new CopyOnWriteArrayList<>();

    private Lotuspuzzel() {
    }

    /** Is the lotus whole? {@code tegel.apply(n)} = the state of tegel_n (1..9). */
    public static boolean opgelost(IntFunction<BlockState> tegel) {
        for (int n = 1; n <= 9; n++) {
            BlockState s = tegel.apply(n);
            if (s == null || !s.hasProperty(GeestBlokken.Lotuspuzzeltegel.DRAAI)) {
                return false;
            }
            if (GOED[n - 1] >= 0 && s.getValue(GeestBlokken.Lotuspuzzeltegel.DRAAI) != GOED[n - 1]) {
                return false;
            }
        }
        return true;
    }

    /** After a click on a tile in the Geestenwereld: solved now? Then the lotus glows, everyone near hears it, and the hook. */
    static void na(ServerPlayer p, ServerLevel level) {
        if (level.dimension() != Geestenwereld.DIM) {
            return;
        }
        if (!opgelost(n -> {
            BlockPos at = Geestenwereld.wereld("tegel_" + n);
            return at == null ? null : level.getBlockState(at);
        })) {
            return;
        }
        BlockPos midden = Geestenwereld.wereld("tegel_5");
        if (midden != null) {
            level.sendParticles(ParticleTypes.END_ROD, midden.getX() + 0.5, midden.getY() + 1.2, midden.getZ() + 0.5, 60, 1.4, 0.6, 1.4, 0.05);
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_geestenwereld.opgelost").withStyle(ChatFormatting.LIGHT_PURPLE));
        OPGELOST.forEach(c -> c.accept(p, level));
    }
}
