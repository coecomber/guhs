package nl.juiced.guhs.feature.bestaand;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * A recipe card of this slice (CONTRACT_130 D5; the Wachter-guh's lantaarnrecept, the Knuffelmaker-guh's knuffelpatroon):
 * an ingredient of its recipes that stays in the crafting grid (like the Timmerguh's bouwboekje), so one is enough for
 * ever. Its NPC gives it once per player, and again when it was lost. The tooltip line is added on the client
 * (BestaandClient: item.guhs.&lt;id&gt;.lore).
 */
public class ReceptItem extends Item {
    public ReceptItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        return new ItemStackTemplate(instance.typeHolder(), 1, instance instanceof ItemStack stack ? stack.getComponentsPatch() : DataComponentPatch.EMPTY);
    }
}
