package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;

/**
 * einde2 phase B, slice h4_vuur: the Guhvatar statue as a reward (DESIGN 5 "Wat je overhoudt", CONTRACT_E2_B 7.5): the
 * item of {@link GuhvatarBeeldBlock} with the player's own profile, and the last, empty pedestal of the Hall of the
 * Guhvatars in the Zuidelijke Luchttempel ({@code voetstuk_leeg}, bouw_lucht): for a player who finished the story it holds
 * a statue of THEM (a mirror statue: their client draws their own skin on it), shown to that player only (main's Schijn).
 */
public final class Beeld {
    /** The empty pedestal of the Hall (bouw_lucht's marker): the statue stands on it. */
    static final String VOETSTUK = "voetstuk_leeg";

    private Beeld() {
    }

    /** The statue item of this player (their profile on it, so it keeps their skin wherever it is placed). */
    public static ItemStack item(ServerPlayer p) {
        ItemStack stack = new ItemStack(E2H4VuurFeature.BEELD_ITEM.get());
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(p.getGameProfile()));
        return stack;
    }

    /** (the Schijn source) the player's own statue on the empty pedestal of the Hall, once the story is done. */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        ServerLevel g = Plek.guheinde(p);
        if (g == null || E2H4Vuur.FINALE == null || !E2Verhaal.finaleKlaar(p)) {
            return;
        }
        BlockPos o = Plekken.oorsprong(g, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL);
        Plekken.Plek v = Plekken.plek(Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, VOETSTUK);
        if (o == null || v == null) {
            return;
        }
        BlockPos op = o.offset(v.pos()).above();
        if (Schijn.dichtbij(p, op) && g.getBlockState(op).isAir()) {
            gewenst.put(op, E2H4VuurFeature.BEELD.get().defaultBlockState().setValue(GuhvatarBeeldBlock.SPIEGEL, true)
                    .setValue(GuhvatarBeeldBlock.FACING, Direction.SOUTH));
        }
    }
}
