package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;

/**
 * T-OPSLAG: the Materiaaltas. One account-wide bag that holds every material kind in any amount (a long per kind). Materials
 * you pick up in the Vadsrijk go straight in (unless the player switched that off), even with a full inventory.
 * <p><b>What is a material?</b> (for t-ambacht and every slice that adds gathering/crafting items) any of:
 * <ul>
 *   <li>the item tag {@code guhs:vr_materiaal} ({@link #TAG}; data packs can add to it);</li>
 *   <li>{@link #registreer}(items...) from your registry class (after your items are registered, e.g. in common setup or lazily:
 *   {@code Materiaaltas.registreer(MijnSpul.ERTS)} takes the ItemLike at call time, so pass the resolved item);</li>
 *   <li>{@link #regel}(predicate) for whole families (e.g. {@code s -> s.getItem() instanceof MijnErts}).</li>
 * </ul>
 * Gear with rolled stats ({@code vr_uitrusting}) is never a material. Kinds are kept apart by item + components, so crystal
 * qualities stay separate. Using materials: {@link Opslag#tel}/{@link Opslag#neem} (inventory + bag) or {@link #tel}/{@link #neem}
 * (bag only). Giving: {@link #geef} (into the bag when allowed, else the inventory).
 */
public final class Materiaaltas {
    public static final TagKey<Item> TAG = ItemTags.create(Guhs.id("vr_materiaal"));
    private static final Set<Item> ITEMS = ConcurrentHashMap.newKeySet();
    private static final List<Predicate<ItemStack>> REGELS = new CopyOnWriteArrayList<>();

    private Materiaaltas() {
    }

    public static void registreer(ItemLike... items) {
        for (ItemLike i : items) {
            ITEMS.add(i.asItem());
        }
    }

    public static void regel(Predicate<ItemStack> r) {
        REGELS.add(r);
    }

    public static boolean isMateriaal(ItemStack s) {
        if (s.isEmpty() || Rollen.van(s) != null) {
            return false;
        }
        if (s.is(TAG) || ITEMS.contains(s.getItem())) {
            return true;
        }
        for (Predicate<ItemStack> r : REGELS) {
            if (r.test(s)) {
                return true;
            }
        }
        return false;
    }

    // ---- the bag (server side; every change bumps tasVersie so an open screen re-syncs) --------------------------------------

    @Nullable
    static OpslagData.Materiaal soort(OpslagData d, ItemStack s) {
        for (OpslagData.Materiaal m : d.materialen) {
            if (ItemStack.isSameItemSameComponents(m.soort, s)) {
                return m;
            }
        }
        return null;
    }

    /** Puts the WHOLE stack into the bag (the stack ends empty). No checks: callers check {@link #isMateriaal} / where. */
    public static long stop(OpslagData d, ItemStack s) {
        if (s.isEmpty()) {
            return 0;
        }
        long n = s.getCount();
        OpslagData.Materiaal m = soort(d, s);
        if (m == null) {
            d.materialen.add(new OpslagData.Materiaal(s, n));
        } else {
            m.aantal += n;
        }
        s.setCount(0);
        d.tasVersie++;
        return n;
    }

    /** Takes up to n of this kind out of the bag as one stack (at most the max stack size); EMPTY when there is none. */
    public static ItemStack haalUit(OpslagData d, ItemStack soort, int n) {
        OpslagData.Materiaal m = soort(d, soort);
        if (m == null || m.aantal <= 0 || n <= 0) {
            return ItemStack.EMPTY;
        }
        int k = (int) Math.min(Math.min(n, soort.getMaxStackSize()), m.aantal);
        ItemStack uit = m.soort.copyWithCount(k);
        m.aantal -= k;
        if (m.aantal <= 0) {
            d.materialen.remove(m);
        }
        d.tasVersie++;
        return uit;
    }

    /** Puts a stack that just came out back (no material check: it was in the bag a moment ago). */
    static void terug(OpslagData d, ItemStack s) {
        stop(d, s);
    }

    public static long tel(ServerPlayer p, Item item) {
        long n = 0;
        for (OpslagData.Materiaal m : Opslag.data(p).materialen) {
            if (m.soort.is(item)) {
                n += m.aantal;
            }
        }
        return n;
    }

    public static long tel(ServerPlayer p, ItemStack soort) {
        OpslagData.Materiaal m = soort(Opslag.data(p), soort);
        return m == null ? 0 : m.aantal;
    }

    /** Takes up to n of this item (any components) from the bag; returns how many were taken. */
    public static long neem(ServerPlayer p, Item item, long n) {
        OpslagData d = Opslag.data(p);
        long weg = 0;
        Iterator<OpslagData.Materiaal> it = d.materialen.iterator();
        while (it.hasNext() && weg < n) {
            OpslagData.Materiaal m = it.next();
            if (!m.soort.is(item)) {
                continue;
            }
            long k = Math.min(n - weg, m.aantal);
            m.aantal -= k;
            weg += k;
            if (m.aantal <= 0) {
                it.remove();
            }
        }
        if (weg > 0) {
            d.tasVersie++;
        }
        return weg;
    }

    /**
     * Gives a stack to the player: a material goes into the bag when the player is in the Vadsrijk with the Vadsrijk inventory
     * (and has auto-pickup on); anything else (or outside) goes into the inventory, the rest to kern's found items.
     */
    public static void geef(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        OpslagData d = Opslag.data(p);
        if (isMateriaal(s) && Opslag.magOpenen(p) && !d.autoUit) {
            stop(d, s);
            return;
        }
        p.getInventory().add(s);
        if (!s.isEmpty()) {
            nl.juiced.guhs.feature.vadsrijk.kern.Inventaris.bewaar(p, s);
        }
    }

    /** Puts every material from the main inventory (hotbar + bag, 36 slots) into the bag; returns how many items moved. */
    public static long allesOpbergen(ServerPlayer p) {
        if (!Opslag.magOpenen(p)) {
            return 0;
        }
        return allesOpbergen(p, Opslag.data(p));
    }

    static long allesOpbergen(ServerPlayer p, OpslagData d) {
        long n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (isMateriaal(s)) {
                ItemStack weg = s.copy();
                inv.setItem(i, ItemStack.EMPTY);   // (cleared first, then stored: one tick, one thread, one save file)
                n += stop(d, weg);
            }
        }
        inv.setChanged();
        return n;
    }

    // ---- automatic pickup -----------------------------------------------------------------------------------------------

    /**
     * Picking up a material in the Vadsrijk: it goes into the bag instead of the inventory (works with a full inventory).
     * The vanilla rules stay: the pickup delay and the owner of personal loot (kern's {@code LootBundel} sets a target).
     */
    static void opraap(ItemEntityPickupEvent.Pre e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) {
            return;
        }
        ItemEntity ie = e.getItemEntity();
        ItemStack s = ie.getItem();
        if (!isMateriaal(s) || ie.hasPickUpDelay() || (ie.getTarget() != null && !ie.getTarget().equals(p.getUUID())) || !Opslag.magOpenen(p)) {
            return;
        }
        OpslagData d = Opslag.data(p);
        if (d.autoUit) {
            return;
        }
        int n = s.getCount();
        Item item = s.getItem();
        ItemStack weg = s.copy(), toon = s.copyWithCount(1);
        p.take(ie, n);   // (the fly-to-player animation; must come before discard)
        ie.setItem(ItemStack.EMPTY);
        ie.discard();
        stop(d, weg);
        p.awardStat(Stats.ITEM_PICKED_UP.get(item), n);
        e.setCanPickup(TriState.FALSE);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.35f, 0.9f + p.getRandom().nextFloat() * 0.3f);
        p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.tas.erin", n, toon.getHoverName(), tel(p, toon))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        Tips.toon(p, "vadsrijk.opslag.tip.tas");
    }
}
