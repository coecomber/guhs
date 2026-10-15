package nl.juiced.guhs.feature.techklus;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlock;

/**
 * bbq2 (tech-klusjes): the new chores of the Guhhuisje around the guh machines. Two chores of this package, appended to
 * the list of the huisje screen and of its overview "Wat kan hier?":
 * <ul>
 *   <li>{@link MachineKlus machines}: empty the out slots of the machines in the home base and keep them filled with
 *       what they were shown ({@link Klusmachines}; tag {@code guhs:techklus/machines});</li>
 *   <li>{@link PlantageKlus plantage}: chop the trees on the Plantagebakken and give an empty bak new saplings.</li>
 * </ul>
 * The other two parts of the slice live in the klusjes package they belong to: the farmen chore harvests more
 * ({@code KlusGebied.oogstbaar}, {@code FarmenKlus.oogstMeer}) and chore output goes into a Hapluikje of the home base
 * when no Bank Guh stands in it ({@code Voorraad.lever}, {@code Voorraad.luikjes}).
 * No blocks, items or payloads of its own. Texts, tags and advancements: tools/features/tech_klusjes.py.
 */
public final class TechklusFeature {
    /** The blocks the machines chore serves (kern blocks with an item capability: in through the top, out through the bottom). */
    public static final TagKey<Block> MACHINES = TagKey.create(Registries.BLOCK, Guhs.id("techklus/machines"));
    /** The names of this package's kinds in the chores' scan of a home base ({@link KlusGebied#registreer}). */
    public static final String MACHINE = "techklus_machine", BAK = "techklus_plantagebak";

    public static final MachineKlus MACHINE_KLUS = new MachineKlus();
    public static final PlantageKlus PLANTAGE_KLUS = new PlantageKlus();

    public static void register(IEventBus modBus) {
        Klusjes.registreer(MACHINE_KLUS);
        Klusjes.registreer(PLANTAGE_KLUS);
        KlusGebied.registreer(MACHINE, (level, pos, state) -> state.is(MACHINES));
        KlusGebied.registreer(BAK, (level, pos, state) -> state.getBlock() instanceof PlantagebakBlock);
        // once a second: look what lies in the machines around the huisjes (what they are shown is what they get)
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            if (event.getServer().getTickCount() % 20 == 7) {
                Klusmachines.kijk(event.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> TechklusCommando.register(event));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private TechklusFeature() {
    }
}
