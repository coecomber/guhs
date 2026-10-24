package nl.juiced.guhs.event;

import java.util.List;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.5.0 fix: two enchanted pieces of Vahoege Vads gear combine in an anvil. The gear is unbreakable
 * ({@code DataComponents.UNBREAKABLE}), so {@code ItemStack#isDamageableItem()} is false, and vanilla
 * {@code AnvilMenu#createResultInternal} only combines two equal items when they are damageable: the result slot stayed
 * empty. Here (after vanilla, through NeoForge's {@link AnvilUpdateEvent}) the enchantments of the right one are merged
 * into the left one exactly like vanilla's item + item branch (26.1.2), minus the durability part: equal levels go up one
 * (up to the max), otherwise the highest level wins, enchantments that don't fit or clash are skipped. XP cost, rename
 * and the REPAIR_COST penalty work as in vanilla. The gear stays unbreakable. Books and renaming alone are left to vanilla.
 */
public final class VadsAambeeld {
    /** The unbreakable Vads gear. */
    private static final List<Supplier<? extends Item>> VADS = new java.util.ArrayList<>(List.of(ModItems.VADS_HELMET, ModItems.VADS_CHESTPLATE,
            ModItems.VADS_LEGGINGS, ModItems.VADS_BOOTS, ModItems.VADS_SWORD, ModItems.VADS_PICKAXE, ModItems.VADS_AXE, ModItems.VADS_SHOVEL,
            ModItems.VADS_HOE, ModItems.VADS_PAXEL, ModItems.VADS_SHEARS));

    static {
        VADS.addAll(nl.juiced.guhs.feature.mijnhulp.MijnhulpFeature.nieuweVads());   // 1.7.0: the Vadsknakker and super vahoege vads
    }

    private VadsAambeeld() {
    }

    public static boolean isVads(ItemStack stack) {
        for (Supplier<? extends Item> item : VADS) {
            if (stack.is(item.get())) {
                return true;
            }
        }
        return false;
    }

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft(), right = event.getRight();
        if (left.isEmpty() || right.isEmpty() || !isVads(left) || !left.is(right.getItem()) || right.has(DataComponents.STORED_ENCHANTMENTS)
                || left.isDamageableItem()) {
            return;   // not two of the same Vads piece (or vanilla can do it itself)
        }
        Player player = event.getPlayer();
        ItemStack result = left.copy();
        ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(result);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(before);
        long tax = (long) left.getOrDefault(DataComponents.REPAIR_COST, 0) + right.getOrDefault(DataComponents.REPAIR_COST, 0);
        int price = 0;
        boolean anyCompatible = false;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(right).entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int current = enchantments.getLevel(holder);
            int level = entry.getIntValue();
            level = current == level ? level + 1 : Math.max(level, current);
            Enchantment enchantment = holder.value();
            boolean compatible = left.supportsEnchantment(holder) || player.getAbilities().instabuild;
            for (Holder<Enchantment> other : enchantments.keySet()) {
                if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                    compatible = false;
                    price++;
                }
            }
            if (compatible) {
                anyCompatible = true;
                level = Math.min(level, enchantment.getMaxLevel());
                enchantments.set(holder, level);
                price += enchantment.getAnvilCost() * level;
                if (left.getCount() > 1) {
                    price = 40;
                }
            }
        }
        ItemEnchantments after = enchantments.toImmutable();
        if (!anyCompatible || after.equals(before)) {
            none(event);   // nothing fits, or nothing would change
            return;
        }
        String name = event.getName();
        if (name != null && !StringUtil.isBlank(name)) {
            if (!name.equals(left.getHoverName().getString())) {
                price++;
                result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            price++;
            result.remove(DataComponents.CUSTOM_NAME);
        }
        int cost = (int) Mth.clamp(tax + price, 0L, Integer.MAX_VALUE);
        if (cost >= 40 && !player.hasInfiniteMaterials()) {
            none(event);   // "Too Expensive!"
            return;
        }
        int repairCost = Math.max(result.getOrDefault(DataComponents.REPAIR_COST, 0), right.getOrDefault(DataComponents.REPAIR_COST, 0));
        result.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(repairCost));
        EnchantmentHelper.setEnchantments(result, after);
        event.setOutput(result);
        event.setXpCost(cost);
        event.setMaterialCost(0);   // (the whole right piece is used up, as vanilla does for item + item)
    }

    private static void none(AnvilUpdateEvent event) {
        event.setOutput(ItemStack.EMPTY);
        event.setXpCost(0);
        event.setMaterialCost(0);
    }
}
