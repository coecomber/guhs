package nl.juiced.guhs.feature.mijnhuisje;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.storage.BankStorage;

/**
 * 1.7.0 (ronde 37-40): the Rugzakzadel ({@link GuhClothes#RUGZAKZADEL}, guh clothes on the back). Lore: in the bag sits a
 * little guh that keeps everything in its tummy, so there is no end to it: a guh wearing it carries any number of plain
 * ground blocks (item tag {@code guhs:rugzakzadel_grond}: wool, the layer stones, dirt, stone, sand, gravel, guhklei...;
 * never ores or anything rare). It lives on the guh ({@code guhs_rugzakzadel} in its persistent data: item id -> count),
 * so it goes along when the guh is picked up and is still there when the saddle goes back on.
 * <p>
 * The Mijn-guhhuisje fills it (its ground blocks go into the bag of a resident wearing it) and empties it into its Bank Guh
 * ({@link #naarBank}: as much as the bank takes, the rest stays in the bag) or, with a button, into its chest.
 */
public final class Rugzak {
    public static final String KEY = "guhs_rugzakzadel";
    public static final TagKey<Item> GROND = TagKey.create(Registries.ITEM, Guhs.id("rugzakzadel_grond"));

    private Rugzak() {
    }

    public static boolean draagt(GuhEntity g) {
        return g.isAlive() && g.getClothes(GuhClothes.Slot.BACK) == GuhClothes.RUGZAKZADEL;
    }

    /** A plain ground block (only plain items: no names, no enchantments, nothing special). */
    public static boolean isGrond(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GROND) && stack.isComponentsPatchEmpty();
    }

    /** What is in the bag: item -> count (in the order it came in). */
    public static Map<Item, Long> inhoud(GuhEntity g) {
        Map<Item, Long> uit = new LinkedHashMap<>();
        CompoundTag t = g.getPersistentData().getCompoundOrEmpty(KEY);
        for (String k : t.keySet()) {
            Identifier id = Identifier.tryParse(k);
            long n = t.getLongOr(k, 0L);
            if (id != null && n > 0) {
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> uit.put(item, n));
            }
        }
        return uit;
    }

    public static long totaal(GuhEntity g) {
        long n = 0;
        for (long v : inhoud(g).values()) {
            n += v;
        }
        return n;
    }

    /** Puts a stack of a plain ground block in (all of it: there is no end to the bag). False: not a ground block. */
    public static boolean stop(GuhEntity g, ItemStack stack) {
        if (!isGrond(stack)) {
            return false;
        }
        voegToe(g, stack.getItem(), stack.getCount());
        return true;
    }

    private static void voegToe(GuhEntity g, Item item, long n) {
        CompoundTag data = g.getPersistentData();
        CompoundTag t = data.getCompoundOrEmpty(KEY).copy();
        String k = BuiltInRegistries.ITEM.getKey(item).toString();
        long nieuw = t.getLongOr(k, 0L) + n;
        if (nieuw <= 0) {
            t.remove(k);
        } else {
            t.putLong(k, nieuw);
        }
        data.put(KEY, t);
    }

    /** Empties the bag into this bank, as much as it takes per kind (the rest stays in). Returns how many went in. */
    public static long naarBank(GuhEntity g, BankStorage bank) {
        long totaal = 0;
        for (Map.Entry<Item, Long> e : inhoud(g).entrySet()) {
            long in = bank.insert(new ItemStack(e.getKey()), e.getValue());
            if (in > 0) {
                voegToe(g, e.getKey(), -in);
                totaal += in;
            }
        }
        return totaal;
    }

    /** Empties the bag into a chest, stack by stack, until it is full (at most {@code max} stacks). Returns how many went in. */
    public static long naarKist(GuhEntity g, IItemHandler kist, int max) {
        long totaal = 0;
        int stapels = 0;
        for (Map.Entry<Item, Long> e : inhoud(g).entrySet()) {
            long over = e.getValue();
            while (over > 0 && stapels < max) {
                int n = (int) Math.min(over, e.getKey().getDefaultMaxStackSize());
                ItemStack rest = ItemHandlerHelper.insertItemStacked(kist, new ItemStack(e.getKey(), n), false);
                int in = n - rest.getCount();
                stapels++;
                if (in <= 0) {
                    break;
                }
                voegToe(g, e.getKey(), -in);
                over -= in;
                totaal += in;
                if (!rest.isEmpty()) {
                    break;
                }
            }
        }
        return totaal;
    }
}
