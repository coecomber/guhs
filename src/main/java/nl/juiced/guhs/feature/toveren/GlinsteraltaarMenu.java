package nl.juiced.guhs.feature.toveren;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

/**
 * The Glinsteraltaar's menu: the vanilla {@link EnchantmentMenu} (same slots, same data slots, so the client opens the
 * vanilla enchanting screen for it: MenuType.ENCHANTMENT) with the server-side calculation redone for the altar. That
 * calculation is vanilla's (26.1.2) step for step, with three differences:
 * <ul>
 * <li>the power is the number of {@link ToverenFeature#KNUFFELBOEKENKAST Knuffelboekenkasten} in the ring
 * ({@link GlinsteraltaarBlock#kracht}); vanilla bookshelves do not count;</li>
 * <li>the pool is the enchantment tag {@code #guhs:glinsteraltaar} (the useful vanilla enchantments, mending among
 * them with vanilla's own rare weight, plus the five table guh enchantments) instead of {@code #in_enchanting_table};</li>
 * <li>durability enchantments ({@code #guhs:duurzaamheid}) are never offered for an unbreakable item
 * ({@link Spreuken#magOp}), and a book that gets a guh enchantment comes out as a {@link Spreukjesboekje}.</li>
 * </ul>
 * The seed is the player's enchantment seed, as in vanilla (it changes after every enchantment). The vanilla data slot of
 * the seed is private and only drives the screen's decorative glyph names, so this menu keeps its own copy.
 */
public class GlinsteraltaarMenu extends EnchantmentMenu {
    private final ContainerLevelAccess access;
    private final RandomSource random = RandomSource.create();
    private int seed;

    public GlinsteraltaarMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
        this.access = access;
        this.seed = inventory.player.getEnchantmentSeed();
    }

    private Container tafel() {
        return getSlot(0).container;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != tafel()) {
            return;
        }
        ItemStack stack = container.getItem(0);
        if (stack.isEmpty() || !stack.isEnchantable()) {
            for (int i = 0; i < 3; i++) {
                costs[i] = 0;
                enchantClue[i] = -1;
                levelClue[i] = -1;
            }
            return;
        }
        access.execute((level, pos) -> {
            bereken(level, pos, stack);
            broadcastChanges();
        });
    }

    /** Vanilla's offers, with the altar's power and pool. */
    private void bereken(Level level, BlockPos pos, ItemStack stack) {
        IdMap<Holder<Enchantment>> holders = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        int kracht = GlinsteraltaarBlock.kracht(level, pos);
        random.setSeed(seed);
        for (int i = 0; i < 3; i++) {
            costs[i] = EnchantmentHelper.getEnchantmentCost(random, i, kracht, stack);
            enchantClue[i] = -1;
            levelClue[i] = -1;
            if (costs[i] < i + 1) {
                costs[i] = 0;
            }
            costs[i] = net.neoforged.neoforge.event.EventHooks.onEnchantmentLevelSet(level, pos, i, kracht, stack, costs[i]);
        }
        for (int i = 0; i < 3; i++) {
            if (costs[i] > 0) {
                List<EnchantmentInstance> list = lijst(level.registryAccess(), stack, i, costs[i]);
                if (!list.isEmpty()) {
                    EnchantmentInstance ench = list.get(random.nextInt(list.size()));
                    enchantClue[i] = holders.getId(ench.enchantment());
                    levelClue[i] = ench.level();
                }
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId < 0 || buttonId >= costs.length) {
            return false;
        }
        ItemStack stack = tafel().getItem(0);
        ItemStack lapis = tafel().getItem(1);
        int prijs = buttonId + 1;
        if ((lapis.isEmpty() || lapis.getCount() < prijs) && !player.hasInfiniteMaterials()) {
            return false;
        }
        if (costs[buttonId] <= 0 || stack.isEmpty()
                || (player.experienceLevel < prijs || player.experienceLevel < costs[buttonId]) && !player.hasInfiniteMaterials()) {
            return false;
        }
        access.execute((level, pos) -> {
            List<EnchantmentInstance> nieuw = lijst(level.registryAccess(), stack, buttonId, costs[buttonId]);
            if (nieuw.isEmpty()) {
                return;
            }
            player.onEnchantmentPerformed(stack, prijs);
            ItemStack uit = stack.getItem().applyEnchantments(stack, nieuw);   // (NeoForge: a book becomes an enchanted book)
            Spreukjesboekje.kleedAan(uit);
            tafel().setItem(0, uit);
            net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, uit, nieuw);
            lapis.consume(prijs, player);
            if (lapis.isEmpty()) {
                tafel().setItem(1, ItemStack.EMPTY);
            }
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer sp) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(sp, uit, prijs);
            }
            seed = player.getEnchantmentSeed();
            tafel().setChanged();
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 1.1F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.3F);
        });
        return true;
    }

    /** What the altar would put on this stack in this slot (vanilla's selectEnchantment over the altar's pool). */
    private List<EnchantmentInstance> lijst(RegistryAccess registries, ItemStack stack, int slot, int kosten) {
        random.setSeed(seed + slot);
        List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(random, stack, kosten, pool(registries, stack));
        if (stack.is(Items.BOOK) && list.size() > 1) {
            list.remove(random.nextInt(list.size()));
        }
        return list;
    }

    /** The altar's pool for this stack: {@code #guhs:glinsteraltaar}, without what may not go on it. */
    static Stream<Holder<Enchantment>> pool(RegistryAccess registries, ItemStack stack) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(Spreuken.ALTAAR).stream().flatMap(HolderSet::stream)
                .filter(h -> Spreuken.magOp(stack, h));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ToverenFeature.GLINSTERALTAAR.get());
    }
}
