package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * The gathered materials (items {@code guhs:vr_amb_<id>}, stack 64, so they sell as stacks on the market) with a quality
 * (component {@code guhs:vr_amb_kwaliteit}: 0 gewoon, 1 vadsig, 2 extra vadsig; only equal qualities stack), the boss cores
 * ({@code vr_amb_kern_<zone>} + the kernsplinter) and the weekly Ambachtskist. Tiers follow the zones (T1 zone 1 ... T5 zone 5;
 * T6 = zone 6 volgt, {@link Zone6Haken}); tier 0 = a rare one from a glittering spot. Fish "wanneer": "" always, "nacht", "regen",
 * "duik" (coral diving spots only, not a fish). Names/flavour: {@code item.guhs.vr_amb_<id>(.uitleg)}.
 */
public final class Materialen {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KWALITEIT = COMPONENTS.registerComponentType("vr_amb_kwaliteit",
            b -> b.persistent(Codec.intRange(0, 2)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final String[] KWALITEITEN = {"gewoon", "vadsig", "extra_vadsig"};

    /**
     * One material: its ambacht, tier (0 = rare), its colour (icon), and for fish: when it bites ("" always, "nacht", "regen").
     */
    public record Materiaal(String id, Ambacht ambacht, int tier, int kleur, String wanneer) {
        public String itemId() {
            return "vr_amb_" + id;
        }

        public String naamKey() {
            return "item.guhs." + itemId();
        }

        public boolean zeldzaam() {
            return tier == 0;
        }

        /** A coral-diving find (not a fish: not in the fish collection). */
        public boolean duik() {
            return "duik".equals(wanneer);
        }

        /** A real fish (the fish collection). */
        public boolean vis() {
            return ambacht == Ambacht.VISSEN && tier > 0 && !duik();
        }
    }

    public static final Map<String, Materiaal> ALLE = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<MateriaalItem>> ITEM = new LinkedHashMap<>();

    static {
        // Mijnen
        m("pluisijzer", Ambacht.MIJNEN, 1, 0xC8B0C0);
        m("kandijzilver", Ambacht.MIJNEN, 2, 0xD8E8F8);
        m("amberkoper", Ambacht.MIJNEN, 3, 0xE8904A);
        m("glinstergruis", Ambacht.MIJNEN, 0, 0xFFE070);
        // Houthakken
        m("zwamsteel", Ambacht.HOUTHAKKEN, 1, 0xE8D8C0);
        m("kristalden", Ambacht.HOUTHAKKEN, 2, 0x8AC8D8);
        m("schemerhout", Ambacht.HOUTHAKKEN, 3, 0x6A4A7A);
        m("roze_hars", Ambacht.HOUTHAKKEN, 3, 0xFF8AB0);
        m("oerbast", Ambacht.HOUTHAKKEN, 0, 0x8A6A3A);
        // Plukken (a herb for the kitchen/brewery + a fibre for the naaiatelier per tier)
        m("klodderklaver", Ambacht.PLUKKEN, 1, 0x6AD86A);
        m("pluiskatoen", Ambacht.PLUKKEN, 1, 0xFFF0F8);
        m("ijsbloem", Ambacht.PLUKKEN, 2, 0xA8E8FF);
        m("kristalvlas", Ambacht.PLUKKEN, 2, 0xC8D8FF);
        m("lichtkeverkruid", Ambacht.PLUKKEN, 3, 0xD8FF6A);
        m("schemerzijde", Ambacht.PLUKKEN, 3, 0xB89AE8);
        m("gouden_klaver", Ambacht.PLUKKEN, 0, 0xFFD84A);
        // Vissen: per tier a common fish, a night fish and a rain fish (FF14/Animal Crossing)
        v("kloddervisje", 1, 0x7AB8FF, "");
        v("maanbaarsje", 1, 0xC8C8FF, "nacht");
        v("plonsguppie", 1, 0xFF9AC8, "regen");
        v("spiegelforel", 2, 0xE0F0FF, "");
        v("sterrenzalm", 2, 0xFFB89A, "nacht");
        v("sneeuwsnoek", 2, 0xF0FAFF, "regen");
        v("wortelpaling", 3, 0x8A6A4A, "");
        v("lantaarnvis", 3, 0xFFD06A, "nacht");
        v("honingkarper", 3, 0xFFB830, "regen");
        m("parelschelp", Ambacht.VISSEN, 0, 0xFFF0FF);
        // Imkeren (A3: a honey kind per zone; zone 3 has three meadows: one per area)
        m("bloesemhoning", Ambacht.IMKEREN, 3, 0xFFB8D8);
        m("schemerhoning", Ambacht.IMKEREN, 3, 0xE8A040);
        m("wortelhoning", Ambacht.IMKEREN, 3, 0xA86A2A);
        m("bijenwas", Ambacht.IMKEREN, 3, 0xFFE8A0);
        m("koninginnengelei", Ambacht.IMKEREN, 0, 0xFFF8E0);
        // ---- U-AMBACHT2: zone 4 (T4, De Lampionkust) and zone 5 (T5, Hoogvads); the professions grow with every zone ----------
        m("lampionbrons", Ambacht.MIJNEN, 4, 0xF0A050);
        m("hemelzilver", Ambacht.MIJNEN, 5, 0xE8F0FF);
        m("drijfhout", Ambacht.HOUTHAKKEN, 4, 0xC8B49A);
        m("vliegerbamboe", Ambacht.HOUTHAKKEN, 4, 0xB8D86A);
        m("wolkenhout", Ambacht.HOUTHAKKEN, 5, 0xF0EAF8);
        m("zeekraal", Ambacht.PLUKKEN, 4, 0x5AB87A);
        m("zeegaren", Ambacht.PLUKKEN, 4, 0x9AE0E0);
        m("wolkbes", Ambacht.PLUKKEN, 5, 0xB8C8FF);
        m("wolkenwol", Ambacht.PLUKKEN, 5, 0xFAFAFF);
        v("koraalbaars", 4, 0xFF7A8A, "");
        v("maanrog", 4, 0x8A8AE0, "nacht");
        v("regenboogmakreel", 4, 0x7AE0C8, "regen");
        v("roze_koraal", 4, 0xFF6FA8, "duik");     // (coral diving only)
        v("zeesponsje", 4, 0xFFD87A, "duik");      // (coral diving only)
        v("wolkenvis", 5, 0xE8F4FF, "");
        v("sterrenspiering", 5, 0xFFF0A0, "nacht");
        v("donderforel", 5, 0x8AA0D8, "regen");
        m("lampionhoning", Ambacht.IMKEREN, 4, 0xFF9A3A);
        m("duinhoning", Ambacht.IMKEREN, 4, 0xF0D8A0);
        m("wolkenhoning", Ambacht.IMKEREN, 5, 0xFFF8F0);
        m("hemelhoning", Ambacht.IMKEREN, 5, 0xFFE070);
    }

    /**
     * The boss cores (Meesterwerk, index = tier - 1) and their splinter (5 = 1 core at the smidse). U-AMBACHT2: Lampionkern (zone-4
     * bosses), Wolkkern (zone-5 bosses); zone 6 volgt ({@link Zone6Haken#KERN}).
     */
    public static final List<String> KERNEN = List.of("zwamkern", "splinterkern", "wortelkern", "lampionkern", "wolkkern");
    public static final Map<String, DeferredItem<Item>> KERN = new LinkedHashMap<>();
    public static final DeferredItem<Item> KERNSPLINTER;
    public static final DeferredItem<Ambachtskist> AMBACHTSKIST;
    /** U-AMBACHT2: the guh-kite (Naaien, zone 4): decoration for home (A2), comes by the Post. */
    public static final DeferredItem<Item> VLIEGER;

    static {
        for (String k : KERNEN) {
            KERN.put(k, ITEMS.registerItem("vr_amb_kern_" + k, Uitleg::new, () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
        }
        KERNSPLINTER = ITEMS.registerItem("vr_amb_kernsplinter", Uitleg::new, () -> new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
        AMBACHTSKIST = ITEMS.registerItem("vr_amb_ambachtskist", Ambachtskist::new, () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
        VLIEGER = ITEMS.registerItem("vr_amb_vlieger", Uitleg::new, () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    private Materialen() {
    }

    private static void m(String id, Ambacht a, int tier, int kleur) {
        voeg(new Materiaal(id, a, tier, kleur, ""));
    }

    private static void v(String id, int tier, int kleur, String wanneer) {
        voeg(new Materiaal(id, Ambacht.VISSEN, tier, kleur, wanneer));
    }

    private static void voeg(Materiaal m) {
        ALLE.put(m.id(), m);
        ITEM.put(m.id(), ITEMS.registerItem(m.itemId(), p -> new MateriaalItem(p, m), () -> new Item.Properties().stacksTo(64)
                .rarity(m.zeldzaam() ? Rarity.RARE : Rarity.COMMON)));
    }

    // ---- helpers -----------------------------------------------------------------------------------------------------------------

    public static ItemStack stack(String id, int kwaliteit, int n) {
        DeferredItem<MateriaalItem> i = ITEM.get(id);
        if (i == null) {
            DeferredItem<Item> k = KERN.get(id);
            return k == null ? (id.equals("kernsplinter") ? new ItemStack(KERNSPLINTER.get(), n) : ItemStack.EMPTY) : new ItemStack(k.get(), n);
        }
        ItemStack s = new ItemStack(i.get(), n);
        if (kwaliteit > 0) {
            s.set(KWALITEIT.get(), Math.min(2, kwaliteit));
        }
        return s;
    }

    public static int kwaliteit(ItemStack s) {
        Integer q = s.get(KWALITEIT.get());
        return q == null ? 0 : q;
    }

    @Nullable
    public static Materiaal van(ItemStack s) {
        return s.getItem() instanceof MateriaalItem m ? m.materiaal : null;
    }

    /** The materials of one ambacht and tier (rare ones: tier 0). */
    public static List<Materiaal> van(Ambacht a, int tier) {
        List<Materiaal> l = new ArrayList<>();
        for (Materiaal m : ALLE.values()) {
            if (m.ambacht() == a && m.tier() == tier) {
                l.add(m);
            }
        }
        return l;
    }

    /** The rare material of a gathering ambacht (null: none). */
    @Nullable
    public static Materiaal zeldzaam(Ambacht a) {
        List<Materiaal> l = van(a, 0);
        return l.isEmpty() ? null : l.get(0);
    }

    /** A material item: "Vadsig Kandijzilver" with its ambacht/tier and quality in the tooltip. */
    public static class MateriaalItem extends Item {
        public final Materiaal materiaal;

        public MateriaalItem(Properties p, Materiaal m) {
            super(p);
            this.materiaal = m;
        }

        @Override
        public Component getName(ItemStack stack) {
            int q = kwaliteit(stack);
            Component basis = super.getName(stack);
            return q <= 0 ? basis : Component.translatable("vadsrijk.ambacht.kwaliteit.naam." + KWALITEITEN[q], basis)
                    .withStyle(q == 2 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            t.accept(Component.translatable(materiaal.tier() == 0 ? "vadsrijk.ambacht.item.zeldzaam" : "vadsrijk.ambacht.item.tier",
                    Component.translatable(materiaal.ambacht().naamKey()), materiaal.tier()).withStyle(ChatFormatting.GRAY));
            int q = kwaliteit(stack);
            t.accept(Component.translatable("vadsrijk.ambacht.kwaliteit." + KWALITEITEN[q]).withStyle(q == 0 ? ChatFormatting.DARK_GRAY
                    : q == 1 ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE));
            if (!materiaal.wanneer().isEmpty()) {
                t.accept(Component.translatable("vadsrijk.ambacht.vis." + materiaal.wanneer()).withStyle(ChatFormatting.BLUE));
            }
            t.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    /** An item with a grey explanation line ({@code <description id>.uitleg}). */
    public static class Uitleg extends Item {
        public Uitleg(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            t.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The weekly order chest: right-click = open ({@link Bestelbord#openKist}). */
    public static class Ambachtskist extends Uitleg {
        public Ambachtskist(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                player.getItemInHand(hand).shrink(1);
                Bestelbord.openKist(sp);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
