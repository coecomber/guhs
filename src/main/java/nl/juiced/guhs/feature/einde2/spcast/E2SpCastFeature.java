package nl.juiced.guhs.feature.einde2.spcast;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast: the cast of the Sponsguh story as the story needs it.
 * <ul>
 *   <li>the eight story LOOKS ({@link SpCast}: synced and saved on {@code SponsguhWezen}, drawn by its renderer; also on
 *       cutscene actors through their NBT, {@link SpCast#nbt(String...)});</li>
 *   <li>the story animations of the cast (the table {@code sponsguh.Cast}; models: tools/features/einde2_sp_cast_looks.py) and
 *       the reading of a cutscene's animation by a cast actor ({@code SponsguhWezen.sceneAnimatie});</li>
 *   <li>the Mikas of De Ruige Kroeg: five {@link GuhNpcEntity.Kind}s ({@link KroegMikas}: model, small talk, animations
 *       {@code idle praat kijk_op slaap}) and their residents in Schelpenstad ({@link KroegBewoners});</li>
 *   <li>dev: {@code /guhs einde2 sponsguh cast look|anim|rij|weg|kroeg} ({@link E2SpCastCommando}).</li>
 * </ul>
 * Resources: tools/features/einde2_sp_cast.py (+ _looks, _kroeg).
 */
public final class E2SpCastFeature {
    private E2SpCastFeature() {
    }

    public static void register(IEventBus modBus) {
        for (KroegMikas r : KroegMikas.values()) {
            GuhNpcEntity.ANIMATIE_DUUR.put(r.kind, r::duur);
            NpcRollen.zet(r.kind, KroegMikas.ROL);              // a role a story slice registers for a spot wins over this
        }
        KroegBewoners.register();
        NeoForge.EVENT_BUS.addListener(E2SpCastCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
