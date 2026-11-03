package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.bouwhulp.blokken.BouwBlokken;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Leverkist's safety net (balans 9.4, ronde 17): per kind of MATERIAL the Leverkist takes at most the cap of balans 9.3 per
 * hour; the allowance saves up to {@value #SPAAR_UREN} hours (like the buffer), and starts full. Wool is the exception (no
 * cap), and so is everything that is not a building material (eggs, milk, bread, cheese...). Real time (millis), like the
 * village's clocks: it also fills while you are offline.
 * <p>
 * For the core's "Naar huis sturen": {@link #beperk} splits a list into what may go now (and uses the allowance) and what
 * stays in the village; {@link #ruimte} tells how much of a stack may go now (for the UI).
 */
public final class LeverkistPlafond {
    public static final int SPAAR_UREN = 8;
    private static final long UUR = 3_600_000L;

    /** Kind -> cap per hour (balans 9.3; stammen/planken: stammen count as themselves, the planks cap stays for planks). */
    public static final Map<String, Integer> PER_UUR = new LinkedHashMap<>();

    static {
        PER_UUR.put("stammen", 150);
        PER_UUR.put("planken", 600);
        PER_UUR.put("steen_ruw", 900);      // stone, cobble AND clay (balans: "Steen ruw + klei")
        PER_UUR.put("steen_bewerkt", 500);  // bricks, stone bricks, smooth stone
        PER_UUR.put("zand", 900);
        PER_UUR.put("glas", 180);           // glass and glasscherven (1 scherf = 1 glass in the oven)
        PER_UUR.put("bouwkeet", 500);       // the Bouwkeet's building blocks
        PER_UUR.put("kaasknabbels", 80);    // own, lower cap (balans 9.3)
    }

    /** What may go now and what stays (both new stacks). */
    public record Beperkt(List<ItemStack> mee, List<ItemStack> blijft) {
    }

    private LeverkistPlafond() {
    }

    /** The kind of material of this stack (null: no cap: wool, food, guh things...). */
    @Nullable
    public static String soort(ItemStack s) {
        if (s.isEmpty() || s.is(ItemTags.WOOL)) {
            return null;
        }
        if (s.is(ModItems.KAAS_KNABBELS.get())) {
            return "kaasknabbels";
        }
        if (s.is(KoppelingFeature.GLASSCHERF.get()) || s.is(Items.GLASS) || s.is(Items.GLASS_PANE)) {
            return "glas";
        }
        if (s.is(ItemTags.LOGS)) {
            return "stammen";
        }
        if (s.is(ItemTags.PLANKS)) {
            return "planken";
        }
        if (s.is(Items.SAND) || s.is(Items.RED_SAND)) {
            return "zand";
        }
        if (s.is(Items.COBBLESTONE) || s.is(Items.STONE) || s.is(Items.CLAY_BALL) || s.is(Items.CLAY) || s.is(Items.GRAVEL)) {
            return "steen_ruw";
        }
        if (s.is(Items.BRICK) || s.is(Items.BRICKS) || s.is(Items.STONE_BRICKS) || s.is(Items.SMOOTH_STONE)) {
            return "steen_bewerkt";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(s.getItem());
        if ("guhs".equals(id.getNamespace()) && BouwBlokken.PER_ID.containsKey(id.getPath())) {
            return "bouwkeet";
        }
        return null;
    }

    /** How many of this stack may go now (its whole count when it has no cap). */
    public static int ruimte(MinecraftServer s, UUID eigenaar, ItemStack stack, long nuMillis) {
        String soort = soort(stack);
        if (soort == null) {
            return stack.getCount();
        }
        KoppelingData d = KoppelingData.get(s);
        bijvullen(d, eigenaar, nuMillis);
        return Math.min(stack.getCount(), (int) Math.floor(d.speler(eigenaar).tegoed.getOrDefault(soort, max(soort))));
    }

    /** Splits the list: what may go now (the allowance is used) and what stays in the village. */
    public static Beperkt beperk(MinecraftServer s, UUID eigenaar, List<ItemStack> spul, long nuMillis) {
        KoppelingData d = KoppelingData.get(s);
        bijvullen(d, eigenaar, nuMillis);
        Map<String, Double> tegoed = d.speler(eigenaar).tegoed;
        List<ItemStack> mee = new ArrayList<>(), blijft = new ArrayList<>();
        for (ItemStack st : spul) {
            if (st.isEmpty()) {
                continue;
            }
            String soort = soort(st);
            if (soort == null) {
                mee.add(st.copy());
                continue;
            }
            double over = tegoed.getOrDefault(soort, max(soort));
            int n = Math.min(st.getCount(), (int) Math.floor(over));
            if (n > 0) {
                mee.add(st.copyWithCount(n));
                tegoed.put(soort, over - n);
            }
            if (n < st.getCount()) {
                blijft.add(st.copyWithCount(st.getCount() - n));
            }
        }
        d.setDirty();
        return new Beperkt(mee, blijft);
    }

    /** The allowance of one kind now (tests, the UI). */
    public static double tegoed(MinecraftServer s, UUID eigenaar, String soort, long nuMillis) {
        KoppelingData d = KoppelingData.get(s);
        bijvullen(d, eigenaar, nuMillis);
        return d.speler(eigenaar).tegoed.getOrDefault(soort, max(soort));
    }

    static double max(String soort) {
        return PER_UUR.getOrDefault(soort, 0) * (double) SPAAR_UREN;
    }

    /** Adds what came in since the last look (cap per hour x hours), never above 8 hours' worth. */
    static void bijvullen(KoppelingData d, UUID eigenaar, long nu) {
        KoppelingData.Speler sp = d.speler(eigenaar);
        if (sp.tegoedTijd <= 0 || nu < sp.tegoedTijd) {
            sp.tegoedTijd = nu;
            d.setDirty();
            return;
        }
        double uren = (nu - sp.tegoedTijd) / (double) UUR;
        if (uren <= 0) {
            return;
        }
        for (Map.Entry<String, Integer> e : PER_UUR.entrySet()) {
            Double was = sp.tegoed.get(e.getKey());
            if (was != null) {
                sp.tegoed.put(e.getKey(), Math.min(max(e.getKey()), was + e.getValue() * uren));
            }
        }
        sp.tegoedTijd = nu;
        d.setDirty();
    }
}
