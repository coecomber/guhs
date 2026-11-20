package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import nl.juiced.guhs.feature.fabriek.Bouwwegen;

/**
 * 1.10.10: the Bouwkeet's crafting plan. When a build needs X and the places ({@link Materiaal.Bronnen}) don't have enough
 * of X, the keet may craft X at the crafting table from what IS there, recursively (logs -> planks -> stairs), up to
 * {@link #MAX_DIEPTE} steps deep and never through an item that is already on the way (a loop guard: no block -> ingot ->
 * block). Only shaped/shapeless crafting recipes ({@link Bouwwegen#craftNaar}); no furnace, no stonecutter; ingredients with a
 * crafting remainder (a bucket) are never used.
 * <p>
 * Made at "Bouw!" ({@link #maak}): the build's direct needs are reserved first, only what is left over may be crafted
 * with. A recipe whose ingredients are there goes before one that needs crafting itself. Yields are real (6 planks are 4
 * stairs): what is crafted too much stays as the build's credit ({@code Spook.tegoed}) for its next blocks, and is given
 * back when the build is done or cancelled. {@link #voerUit} then really takes the ingredients and puts the crafted items
 * in that credit. The same plan gives the shortage list and the "12 eiken trappen (worden gemaakt van 3 eiken stammen)"
 * lines.
 */
public final class Werkplan {
    /** How many crafting steps deep (logs -> planks -> stairs is 2). */
    public static final int MAX_DIEPTE = 3;
    /** At most this many recipe tries per plan (a huge build with nothing to craft from stays fast). */
    static final int MAX_WERK = 20000;
    /** At most this many candidates of a tag ingredient are tried by crafting them. */
    static final int MAX_KANDIDATEN = 16;

    /** One crafting step: these inputs (one per grid slot) make {@code aantal} of {@code uit}. */
    public record Stap(Item uit, int aantal, List<Item> in) {
    }

    /** A needed item the keet will make: how many, and what it takes from the places for them (the raw items). */
    public record Gemaakt(Item item, int aantal, Map<Item, Integer> van) {
    }

    private final ServerLevel level;
    private final Materiaal.Bronnen bronnen;
    /** What is still free in the places (after the direct needs and what the plan already took). */
    private Map<Item, Long> bron = new HashMap<>();
    /** Crafted in this plan and not used yet. */
    private Map<Item, Long> vrij = new HashMap<>();
    private final List<Stap> stappen = new ArrayList<>();
    private final Map<Item, Integer> tekort = new LinkedHashMap<>();
    private final Map<Item, Integer> direct = new LinkedHashMap<>();
    private final List<Gemaakt> gemaakt = new ArrayList<>();
    /** The raw items the current top-level item takes from the places. */
    private Map<Item, Integer> van = new LinkedHashMap<>();
    private int werk;
    private final Map<Ingredient, List<Item>> kandidaatCache = new java.util.IdentityHashMap<>();

    private Werkplan(ServerLevel level, Materiaal.Bronnen bronnen) {
        this.level = level;
        this.bronnen = bronnen;
    }

    /** The plan for these needs with these places (creative: nothing to craft, nothing missing). */
    public static Werkplan maak(ServerLevel level, Map<Item, Integer> nodig, Materiaal.Bronnen bronnen) {
        Werkplan p = new Werkplan(level, bronnen);
        if (bronnen.gratis()) {
            p.direct.putAll(nodig);
            return p;
        }
        Map<Item, Integer> kort = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> e : nodig.entrySet()) {
            long heb = p.inBron(e.getKey());
            int neem = (int) Math.min(heb, e.getValue());
            p.bron.put(e.getKey(), heb - neem);
            if (neem > 0) {
                p.direct.put(e.getKey(), neem);
            }
            if (neem < e.getValue()) {
                kort.put(e.getKey(), e.getValue() - neem);
            }
        }
        for (Map.Entry<Item, Integer> e : kort.entrySet()) {
            p.dek(e.getKey(), e.getValue());
        }
        return p;
    }

    /** Covers {@code n} missing X by crafting; what can't be made goes to the shortage. */
    private void dek(Item x, int n) {
        van = new LinkedHashMap<>();
        int kreeg = neemVrij(x, n);
        while (kreeg < n && werk < MAX_WERK) {
            Set<Item> pad = new HashSet<>();
            if (!maakEen(x, 0, pad)) {
                break;
            }
            kreeg += neemVrij(x, n - kreeg);
        }
        if (kreeg > 0) {
            gemaakt.add(new Gemaakt(x, kreeg, van));
        }
        if (kreeg < n) {
            tekort.put(x, n - kreeg);
        }
    }

    private int neemVrij(Item x, int n) {
        long heb = vrij.getOrDefault(x, 0L);
        int k = (int) Math.min(heb, n);
        if (k > 0) {
            vrij.put(x, heb - k);
        }
        return k;
    }

    private long inBron(Item i) {
        Long v = bron.get(i);
        if (v == null) {
            v = bronnen.tel(i);
            bron.put(i, v);
        }
        return v;
    }

    /** One craft of X (one recipe once); false: no recipe can be filled. */
    private boolean maakEen(Item x, int diepte, Set<Item> pad) {
        if (diepte >= MAX_DIEPTE || werk++ > MAX_WERK || pad.contains(x)) {
            return false;
        }
        pad.add(x);
        try {
            List<Bouwwegen.Weg> wegen = new ArrayList<>(Bouwwegen.craftNaar(level, x));
            if (wegen.isEmpty()) {
                return false;
            }
            Map<Bouwwegen.Weg, Integer> score = new HashMap<>();
            for (Bouwwegen.Weg w : wegen) {
                score.put(w, score(w, pad));
            }
            wegen.sort(Comparator.comparingInt((Bouwwegen.Weg w) -> -score.get(w)).thenComparingInt(Bouwwegen.Weg::nodig));
            for (Bouwwegen.Weg w : wegen) {
                Map<Item, Long> bronKopie = new HashMap<>(bron), vrijKopie = new HashMap<>(vrij);
                Map<Item, Integer> vanKopie = new LinkedHashMap<>(van);
                int stappenVoor = stappen.size();
                List<Item> in = new ArrayList<>(w.in().size());
                boolean ok = true;
                for (Ingredient ing : w.in()) {
                    Item c = kies(ing, diepte, pad);
                    if (c == null) {
                        ok = false;
                        break;
                    }
                    in.add(c);
                }
                if (ok) {
                    vrij.merge(x, (long) w.uit(), Long::sum);
                    stappen.add(new Stap(x, w.uit(), List.copyOf(in)));
                    return true;
                }
                bron = bronKopie;
                vrij = vrijKopie;
                van = vanKopie;
                while (stappen.size() > stappenVoor) {
                    stappen.remove(stappen.size() - 1);
                }
            }
            return false;
        } finally {
            pad.remove(x);
        }
    }

    /** How many slots of this recipe are there right now (without crafting). */
    private int score(Bouwwegen.Weg w, Set<Item> pad) {
        int n = 0;
        for (Ingredient ing : w.in()) {
            for (Item c : kandidaten(ing, pad)) {
                if (vrij.getOrDefault(c, 0L) > 0 || inBron(c) > 0) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    /** Takes one item for this slot: crafted credit first, then the places, else crafts one. Null: impossible. */
    private Item kies(Ingredient ing, int diepte, Set<Item> pad) {
        List<Item> kandidaten = kandidaten(ing, pad);
        for (Item c : kandidaten) {
            if (vrij.getOrDefault(c, 0L) > 0) {
                vrij.merge(c, -1L, Long::sum);
                return c;
            }
        }
        Item beste = null;
        long meeste = 0;
        for (Item c : kandidaten) {
            long heb = inBron(c);
            if (heb > meeste) {
                meeste = heb;
                beste = c;
            }
        }
        if (beste != null) {
            bron.put(beste, meeste - 1);
            van.merge(beste, 1, Integer::sum);
            return beste;
        }
        int geprobeerd = 0;
        for (Item c : kandidaten) {
            if (geprobeerd++ >= MAX_KANDIDATEN || werk > MAX_WERK) {
                break;
            }
            if (maakEen(c, diepte + 1, pad) && vrij.getOrDefault(c, 0L) > 0) {
                vrij.merge(c, -1L, Long::sum);
                return c;
            }
        }
        return null;
    }

    /** The items an ingredient accepts that may be used: not on the way already, no crafting remainder. */
    private List<Item> kandidaten(Ingredient ing, Set<Item> pad) {
        List<Item> alle = kandidaatCache.computeIfAbsent(ing, k -> {
            List<Item> uit = new ArrayList<>();
            k.items().map(Holder::value).forEach(c -> {
                if (c != Items.AIR && !uit.contains(c)) {
                    ItemStack st = new ItemStack(c);
                    if (c.getCraftingRemainder(st) == null && k.test(st)) {
                        uit.add(c);
                    }
                }
            });
            return uit;
        });
        if (pad.isEmpty()) {
            return alle;
        }
        List<Item> uit = new ArrayList<>(alle.size());
        for (Item c : alle) {
            if (!pad.contains(c)) {
                uit.add(c);
            }
        }
        return uit;
    }

    // =====================================================================================================================
    // reading
    // =====================================================================================================================

    /** What is still missing after crafting (empty: everything can be built). */
    public Map<Item, Integer> tekort() {
        return tekort;
    }

    /** What the keet will make, per needed item. */
    public List<Gemaakt> gemaakt() {
        return gemaakt;
    }

    /** How many of each needed item come straight from the places. */
    public Map<Item, Integer> direct() {
        return direct;
    }

    /** How many of this needed item will be crafted. */
    public int maakt(Item item) {
        for (Gemaakt g : gemaakt) {
            if (g.item() == item) {
                return g.aantal();
            }
        }
        return 0;
    }

    public List<Stap> stappen() {
        return stappen;
    }

    /** "12× Eiken trap (worden gemaakt van 3× Eiken stam)". */
    public static Component regel(Gemaakt g) {
        Component naam = new ItemStack(g.item()).getHoverName();
        if (g.van().isEmpty()) {
            return Component.translatable("gui.guhs.bouwhulp.werkbank.regel.rest", g.aantal(), naam);
        }
        return Component.translatable("gui.guhs.bouwhulp.werkbank.regel", g.aantal(), naam, lijst(g.van()));
    }

    /** "3× Eiken stam, 1× Stok". */
    public static MutableComponent lijst(Map<Item, Integer> items) {
        MutableComponent uit = Component.empty();
        boolean eerst = true;
        for (Map.Entry<Item, Integer> e : items.entrySet()) {
            if (!eerst) {
                uit.append(", ");
            }
            uit.append(e.getValue() + "× ").append(new ItemStack(e.getKey()).getHoverName());
            eerst = false;
        }
        return uit;
    }

    /** The chat line at "Bouw!": what the keet crafted (at most 6 lines). */
    public static Component bericht(List<Gemaakt> gemaakt) {
        MutableComponent m = Component.translatable("gui.guhs.bouwhulp.werkbank.bericht").withStyle(ChatFormatting.LIGHT_PURPLE);
        for (int i = 0; i < Math.min(6, gemaakt.size()); i++) {
            m.append(Component.literal("\n  • ").append(regel(gemaakt.get(i))).withStyle(ChatFormatting.GRAY));
        }
        if (gemaakt.size() > 6) {
            m.append(Component.literal("\n  ").append(Component.translatable("gui.guhs.bouwhulp.spook.meer", gemaakt.size() - 6)).withStyle(ChatFormatting.GRAY));
        }
        return m;
    }

    // =====================================================================================================================
    // doing it
    // =====================================================================================================================

    /**
     * Crafts for real: per step its ingredients from {@code bronnen} (the build's places WITH its credit in front, see
     * {@link Materiaal.Bronnen#metTegoed}), the result into {@code tegoed}. A step that can't be filled any more (something
     * changed) stops it: what that step took goes into the credit, so nothing is lost. Returns the number of steps done.
     */
    public int voerUit(Materiaal.Bronnen bronnenMetTegoed, Map<Item, Integer> tegoed) {
        int gedaan = 0;
        for (Stap s : stappen) {
            List<Item> genomen = new ArrayList<>(s.in().size());
            boolean ok = true;
            for (Item i : s.in()) {
                if (bronnenMetTegoed.neem(i, 1) == 1) {
                    genomen.add(i);
                } else {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                genomen.forEach(i -> tegoed.merge(i, 1, Integer::sum));
                break;
            }
            tegoed.merge(s.uit(), s.aantal(), Integer::sum);
            gedaan++;
        }
        return gedaan;
    }
}
