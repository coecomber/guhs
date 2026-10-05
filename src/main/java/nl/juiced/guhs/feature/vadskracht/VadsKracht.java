package nl.juiced.guhs.feature.vadskracht;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import nl.juiced.guhs.Guhs;

/**
 * Vadskracht (bbq2): the power of the guh machines, in whole VK per second. A <i>net</i> is the connected set of Guhdraad
 * and <i>knopen</i> (sources, consumers, storage: {@link VadsKnoop}). The rule: a net runs when its machines ask no more
 * than its sources give (or its Knabbelbatterijen can cover the difference this second); otherwise the WHOLE net stands
 * still. No slowdown, no priorities. Nets are derived, never saved: {@link VadsNetten} keeps them per level, rebuilds them
 * only when a wire or knoop is placed or removed, and evaluates each once per second.
 * <p>
 * This class is the entry point for every other package: the capability a knoop's block entity is registered under, the
 * tag of the blocks the hover readout works on, and the questions "which net is here" and "does it run".
 */
public final class VadsKracht {
    /**
     * {@code guhs:vadsknoop}: register it for your block entity,
     * {@code event.registerBlockEntity(VadsKracht.KNOOP, MY_BE.get(), (be, ctx) -> be)}; always answer with the block entity
     * itself (the same object every time). A part block answers with its controller's block entity ({@link MachineDeelBlock}).
     */
    public static final BlockCapability<VadsKnoop, Void> KNOOP = BlockCapability.createVoid(Guhs.id("vadsknoop"), VadsKnoop.class);
    /** Block tag {@code guhs:vadskracht}: every block the hover readout works on (tools/features/vadskracht.py {@code toon}). */
    public static final TagKey<Block> TOON = TagKey.create(Registries.BLOCK, Guhs.id("vadskracht"));

    /** The net of the Guhdraad or knoop here, evaluated now (never null; {@link VadsNet#EMPTY} when there is none). */
    public static VadsNet net(ServerLevel level, BlockPos pos) {
        return VadsNetten.van(level).nu(pos);
    }

    /** Cheap: did the net of this knoop (or Guhdraad) run at its last evaluation? */
    public static boolean heeftKracht(ServerLevel level, BlockPos knoop) {
        VadsNet net = VadsNetten.van(level).bekend(knoop);
        return net != null && net.draait();
    }

    /**
     * Something changed here: a knoop or Guhdraad was placed or removed (the nets around it are rebuilt next tick), or what
     * a knoop gives or asks changed (its net is evaluated again next tick). Cheap, call it freely.
     */
    public static void veranderd(ServerLevel level, BlockPos pos) {
        VadsNetten.van(level).veranderd(pos);
    }

    /** {@link #veranderd(ServerLevel, BlockPos)} for code that has a Level (does nothing on the client). */
    public static void veranderd(@Nullable Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            veranderd(server, pos);
        }
    }

    /** Rebuild the nets around this spot next tick, whatever is cached (after {@link VadsKnoop#vadsVerbindt} changed). */
    public static void herbouw(ServerLevel level, BlockPos pos) {
        VadsNetten.van(level).herbouw(pos);
    }

    /** A piece of Guhdraad got a neighbour update: checks whether a knoop appeared or disappeared next to it (GuhWireBlock). */
    public static void buurVeranderd(ServerLevel level, BlockPos draad) {
        VadsNetten.van(level).controleer(draad);
    }

    /** The knoop this block belongs to (a part block gives its controller's), or null. */
    @Nullable
    public static VadsKnoop knoop(Level level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getCapability(KNOOP, pos, null) : null;
    }

    /** The lines of the hover readout for this block, as the server sends them (empty when there is no net here). */
    public static List<Component> regels(ServerLevel level, BlockPos pos) {
        return VadsUitlezing.regels(level, pos);
    }

    private VadsKracht() {
    }
}
