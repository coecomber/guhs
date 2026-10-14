package nl.juiced.guhs.feature.einde2.castteam;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * einde2, slice cast_team (CONTRACT_E2 11.4, 11.9): Team Guhvatar and the friends you make on the way, as
 * {@code GuhNpcEntity.Kind}s (the table: {@link CastTeam}). Each kind gets its default role {@link CastTeamRol} (small talk);
 * the client side ({@code client/E2CastTeamClient}) gives each its model and animation file. Nothing is registered besides:
 * no items, no spawn eggs, no pages. Dev runs also get {@code /guhs einde2 cast_team toon|weg} ({@link E2CastTeamCommando}).
 * Resources: tools/features/einde2_cast_team*.py.
 */
public final class E2CastTeamFeature {
    public static void register(IEventBus modBus) {
        for (var kind : CastTeam.kinds()) {
            NpcRollen.zet(kind, CastTeamRol.ROL);
        }
        NeoForge.EVENT_BUS.addListener(E2CastTeamCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2CastTeamFeature() {
    }
}
