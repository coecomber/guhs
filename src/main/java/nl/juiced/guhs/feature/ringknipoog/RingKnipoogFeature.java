package nl.juiced.guhs.feature.ringknipoog;

import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;

/**
 * bbq2 (ring-knipogen): the seven winks at the older stories that the user picked for the Knabbelring and Super Guhrio,
 * each a mini-cutscene of five to ten seconds that a player sees once ({@link Knipogen}: the scenes, how a wink is played
 * around a scene of the story, and the winks that find their own moment), and Sjokkel, who sets out over the bridge of the
 * mine in chapter 3 and arrives at the feast of chapter 6 ({@link Sjokkel}). Texts: tools/features/ring_knipogen.py.
 * <p>
 * How the winks hang in the story (nothing else of the chapters was changed):
 * <ul>
 *   <li>three chapter scenes play through {@link Knipogen#speel} instead of {@code Cutscenes.speel} (the wink right after
 *       the scene, then the scene's own {@code daarna}): the council in {@code ringh2.Guhvendel.luid}, the mirror in
 *       {@code ringh4.Spiegel.kijk}, the feast in {@code ringh6.Thuis.seconde};</li>
 *   <li>{@code ringh6.Klim.samKlik} asks {@link Knipogen#eerst} before Sam-guh carries;</li>
 *   <li>everything else is looked at from here: a step listener on chapter 3 and a look once a second (the gate of the
 *       mine, Sausuman's hall, Sjokkel), a look every tick at the ?-block of a player in level 1-1.</li>
 * </ul>
 * No blocks, items, entity types or payloads of its own: the guests are the real characters of their stories.
 */
public final class RingKnipoogFeature {
    public static void register(IEventBus modBus) {
        Knipogen.registreer();
        Sjokkel.registreer();
        // wink 7 the moment the narrator card of chapter 3 closes (Knipogen.seconde looks again for whoever missed it then)
        RingH3Feature.LIJN.opStap((p, oud, nieuw) -> {
            if (oud == 0 && nieuw == 1) {
                Knipogen.kloon(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                Knipogen.tik(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Sjokkel::opKlik);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Sjokkel::opKlik2);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> Knipogen.vergeet(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Knipogen.wisAlles());
        NeoForge.EVENT_BUS.addListener(RingKnipoogCommands::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingKnipoogFeature() {
    }
}
