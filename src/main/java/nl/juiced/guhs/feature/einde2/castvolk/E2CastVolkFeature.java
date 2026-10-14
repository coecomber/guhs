package nl.juiced.guhs.feature.einde2.castvolk;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * einde2, slice cast_volk (CONTRACT_E2 11.5): the masters, rulers and townsfolk of the Guhvatar story as
 * {@link GuhNpcEntity.Kind}s (the block {@code <einde2_cast_volk>}; the table {@link CastVolk}). They stand, look at you and
 * say a friendly line ({@link CastVolkRol}); no story logic: phase B attaches roles, the merge step puts them at their
 * markers. Models, textures, animations and texts: tools/features/einde2_cast_volk*.py. Client: client/E2CastVolkClient.
 * Dev run only: {@code /guhs einde2 cast_volk toon | weg} ({@link E2CastVolkCommando}).
 */
public final class E2CastVolkFeature {
    public static void register(IEventBus modBus) {
        for (GuhNpcEntity.Kind kind : CastVolk.kinds()) {
            NpcRollen.zet(kind, CastVolkRol.ROL);
        }
        NeoForge.EVENT_BUS.addListener(E2CastVolkCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    /** (no items: a Kind has no spawn egg; /summon guhs:guh_npc ~ ~ ~ {Kind:"<kind>"} or the dev command shows one) */
    public static void creative(Consumer<ItemStack> output) {
    }

    /** The mix set (0 .. n-1) a crowd member standing at this block shows: the client hides the other sets. */
    public static int mixSet(BlockPos at, int n) {
        return Math.floorMod(at.getX() * 31 + at.getZ() * 17, n);
    }

    private E2CastVolkFeature() {
    }
}
