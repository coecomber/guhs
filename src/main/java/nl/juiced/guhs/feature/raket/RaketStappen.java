package nl.juiced.guhs.feature.raket;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;

/**
 * The four stages of the Kaasraket (design V4: romp, motor, brandstof, bemanning). The first three are handed in at the
 * Lanceerplatform in portions (click it: everything of the stage that you carry goes in); the fourth is three tame guhs of
 * yours in an astronaut suit near the platform. Tools/features/raket.py writes the FTB quests from the same numbers
 * ({@link #STAPPEN} is read there: keep the format "eis(..., n)" per line).
 */
public final class RaketStappen {
    /** What a stage asks: an item and how many. */
    public record Eis(Supplier<Item> item, int aantal) {
    }

    /** One stage: its key (gui.guhs.raket.stap.&lt;key&gt;) and what it asks (empty: the crew). */
    public record Stap(String key, List<Eis> eisen) {
        public boolean bemanning() {
            return eisen.isEmpty();
        }
    }

    /** How many astronaut guhs the crew is. */
    public static final int BEMANNING = 3;
    /** How far from the platform the crew may stand (blocks). */
    public static final double BEMANNING_BEREIK = 12;

    private static Eis eis(Supplier<Item> item, int aantal) {
        return new Eis(item, aantal);
    }

    /** A guhs item by its id (the stages name items of other features: by id, so this class needs none of them). */
    private static Supplier<Item> guhs(String id) {
        return () -> BuiltInRegistries.ITEM.getValue(Guhs.id(id));
    }

    public static final List<Stap> STAPPEN = List.of(
            new Stap("romp", List.of(
                    eis(guhs("knuffelsteen"), 64),
                    eis(() -> Items.IRON_INGOT, 24),
                    eis(() -> Items.GLASS, 16))),
            new Stap("motor", List.of(
                    eis(guhs("knabbelbatterij"), 2),
                    eis(guhs("guh_oven"), 1),
                    eis(() -> Items.IRON_BLOCK, 4),
                    eis(() -> Items.REDSTONE, 16))),
            new Stap("brandstof", List.of(
                    eis(guhs("block_of_kaasknabbels"), 24),
                    eis(guhs("kaas_saus_bucket"), 4))),
            new Stap("bemanning", List.of()));

    private RaketStappen() {
    }
}
