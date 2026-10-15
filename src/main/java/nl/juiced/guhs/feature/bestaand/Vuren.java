package nl.juiced.guhs.feature.bestaand;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (bestaand): the four bridge fires of the Spiesburcht, per player. A player at the step {@link
 * BestaandFeature#WACHTER_VUREN} of the Wachter-guh's questline lights a fire bowl ({@link VuurkorfBlock}) by clicking
 * it with something that makes fire: their flag {@code vuur_<nr>} is set and from then on that bowl burns FOR THEM
 * ({@link Schijn}), in every Spiesburcht. The block in the world stays out, so any number of players can light the same
 * bowl. The fourth fire brings the player to the next step.
 */
public final class Vuren {
    public static final int AANTAL = 4;
    /** In the template bestaand_brugvuur_&lt;nr&gt; (a pedestal, a post, the bowl) the bowl stands this far above the corner. */
    public static final int KORF_HOOGTE = 2;
    /** (not saved) dimension -> the loaded fire bowls (their block entities report in and out). */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> KORVEN = new ConcurrentHashMap<>();
    private static final String[] RICHTINGEN = {"oost", "zuid", "west", "noord"};

    private Vuren() {
    }

    static void geladen(ServerLevel level, BlockPos pos) {
        KORVEN.computeIfAbsent(level.dimension(), d -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    static void weg(ServerLevel level, BlockPos pos) {
        Set<BlockPos> korven = KORVEN.get(level.dimension());
        if (korven != null) {
            korven.remove(pos);
        }
    }

    static void wis() {
        KORVEN.clear();
    }

    /** Did this player light the fire of bridge nr? */
    public static boolean brandt(ServerPlayer p, int nr) {
        return BestaandFeature.WACHTER.vlag(p, "vuur_" + nr);
    }

    /** How many of the four this player lit. */
    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (int nr = 0; nr < AANTAL; nr++) {
            n += brandt(p, nr) ? 1 : 0;
        }
        return n;
    }

    /** A click on a fire bowl ({@code aansteker}: with something that lights it). */
    static void klik(ServerPlayer p, BlockPos pos, BlockState state, boolean aansteker) {
        int nr = state.getValue(VuurkorfBlock.NR);
        if (brandt(p, nr)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.vuur.al_aan").withStyle(ChatFormatting.GOLD));
            Schijn.toon(p, pos, state.setValue(VuurkorfBlock.LIT, true));
        } else if (BestaandFeature.WACHTER.stap(p) != BestaandFeature.WACHTER_VUREN) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.vuur.uit").withStyle(ChatFormatting.GRAY));
        } else if (!aansteker) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.vuur.spies").withStyle(ChatFormatting.GRAY));
        } else {
            steekAan(p, pos, state);
        }
    }

    /**
     * This player lights this fire bowl (only at the fires step, and once per bridge): the flag, the flames for them alone,
     * and with the fourth the next step. True when it was lit now.
     */
    public static boolean steekAan(ServerPlayer p, BlockPos pos, BlockState state) {
        int nr = state.getValue(VuurkorfBlock.NR);
        if (BestaandFeature.WACHTER.stap(p) != BestaandFeature.WACHTER_VUREN || brandt(p, nr)) {
            return false;
        }
        BestaandFeature.WACHTER.vlag(p, "vuur_" + nr, true);
        int n = aantal(p);
        Schijn.toon(p, pos, state.setValue(VuurkorfBlock.LIT, true));
        Schijn.geluid(p, SoundEvents.FIRECHARGE_USE, pos, 1.0f, 0.9f + p.getRandom().nextFloat() * 0.2f);
        Schijn.deeltjes(p, ParticleTypes.FLAME, Vec3.atCenterOf(pos).add(0, 0.3, 0), 14, 0.25, 0.03);
        Schijn.deeltjes(p, ParticleTypes.LAVA, Vec3.atCenterOf(pos).add(0, 0.3, 0), 4, 0.1, 0.0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.vuur.aan", Component.translatable("gui.guhs.bestaand.richting."
                + richting(p.level(), pos, nr)), n, AANTAL).withStyle(ChatFormatting.GOLD));
        klaar(p);
        return true;
    }

    /** All four burn: on to the next step (also when the Wachter-guh counted a missing bowl as lit). True when the step changed. */
    static boolean klaar(ServerPlayer p) {
        if (aantal(p) < AANTAL || !BestaandFeature.WACHTER.verder(p, BestaandFeature.WACHTER_VUREN)) {
            return false;
        }
        GidsFeature.grant(p, "barbecuether/bestaand_brugvuren");
        GuhQuests.hint(p, "gui.guhs.bestaand.vuur.alle");
        return true;
    }

    /**
     * Which way this bridge points from the keep, in the world (a copy may be turned, so the template's "east" is not always
     * east): oost / zuid / west / noord. A bowl that stands in no Spiesburcht goes by its nr.
     */
    static String richting(ServerLevel level, BlockPos pos, int nr) {
        StructureStart start = Bezetting.start(level, BestaandFeature.SPIESBURCHT, pos);
        BlockPos midden = start == null ? null : Kopieen.wereld(start, null, Plekken.BURCHT_MIDDEN);
        if (midden == null) {
            return RICHTINGEN[nr];
        }
        int dx = pos.getX() - midden.getX(), dz = pos.getZ() - midden.getZ();
        return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? "oost" : "west") : (dz >= 0 ? "zuid" : "noord");
    }

    /** What a player sees: every loaded fire bowl of a bridge they lit burns. */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        Set<BlockPos> korven = KORVEN.get(p.level().dimension());
        if (korven == null || korven.isEmpty() || aantal(p) == 0) {
            return;
        }
        for (BlockPos pos : korven) {
            if (!Schijn.dichtbij(p, pos)) {
                continue;
            }
            BlockState state = p.level().getBlockState(pos);
            if (!state.is(BestaandFeature.VUURKORF.get())) {
                korven.remove(pos);   // (gone without its block entity saying so)
            } else if (brandt(p, state.getValue(VuurkorfBlock.NR))) {
                gewenst.put(pos, state.setValue(VuurkorfBlock.LIT, true));
            }
        }
    }
}
