package nl.juiced.guhs.feature.guhpad;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.verhaal.Doelen;

/**
 * Het Guhpad (DESIGN_VERHALENPAD A): the stories open the worlds. Resources: tools/features/guhpad.py (+ guhpad_*.py);
 * manual: guhs_work130/reports/slice_guhpad.md.
 * <ul>
 *   <li>{@link GroteVerhalen}: the one registry of the big stories, each with its world;</li>
 *   <li>{@link Guhpad}: the locks (the Knabbelring, the grill portal, the portal of the Knabbelkelder) and what they say;</li>
 *   <li>{@link GuhpadKompas}: the Superkompas option "Mijn verhaal" points to the nearest story you have not done;</li>
 *   <li>{@link GuhpadEvents}: the advancements for the FTB lock quests, the statistic "Verhalen gevolgd", the sync;</li>
 *   <li>client.GuhpadTab: the Guhdex tab Verhalen with the path map on top, the stories per world, and the preview of
 *       "Het echte Guheinde".</li>
 * </ul>
 * The only registry content is the custom statistic {@code guhs:verhalen_gevolgd}. Three other packages each call in with
 * one line: {@code ringh1.Feest} (magBeginnen and Guhdalf's refusal), {@code guheinde.GuheindePortaalBlock} (the lock) and
 * {@code verhaal.Doelen} (the hook {@code anders}).
 */
public final class GuhpadFeature {
    /** The custom statistic "Verhalen gevolgd" (lang stat.guhs.verhalen_gevolgd). */
    public static final String STATISTIEK = "verhalen_gevolgd";
    public static final DeferredRegister<Identifier> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, Guhs.MODID);
    public static final DeferredHolder<Identifier, Identifier> VERHALEN_GEVOLGD = STATS.register(STATISTIEK, () -> Guhs.id(STATISTIEK));

    public static void register(IEventBus modBus) {
        STATS.register(modBus);
        NeoForge.EVENT_BUS.register(GuhpadEvents.class);
        NeoForge.EVENT_BUS.addListener(GuhpadCommands::register);
        // the lock of the grill portal goes FIRST: its message lists the stories that are missing (the Knabbelring's own lock,
        // "chapter 1 is not done", stays behind it and says the same thing once the stories are there)
        GrillPortalBlock.SLOTEN.add(0, Guhpad::grillSlot);
        // "Mijn verhaal" without a questline to follow: the nearest story that is not done yet
        Doelen.anders = GuhpadKompas::doel;
        GroteVerhalen.alle();   // (loads the registry now, so a duplicate id fails at start-up, not in the middle of a game)
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhpadPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private GuhpadFeature() {
    }
}
