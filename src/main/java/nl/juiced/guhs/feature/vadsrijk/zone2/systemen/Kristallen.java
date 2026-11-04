package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * Zone 2 gems (ronde 15: "edelstenen simpel inzetten"). Six raw crystal kinds from the Splinterpieken, items
 * {@code guhs:vr_z2_kristal_<soort>}, each in three qualities (component {@code guhs:vr_z2_kwaliteit}: 0 ruw, 1 geslepen,
 * 2 stralend) by drop source: normal zone-2 mobs drop ruw, elites geslepen, bosses stralend ({@link #dropKwaliteit}).
 * <p>Gear has 0-2 sockets ({@link Uitrusting#sokjes}; rolled by {@link #sokjes}); the juweliersguh ({@link Juwelier}) puts a
 * crystal straight in and takes it out again for a small fee: a gem is never lost. Five kinds give a stat, the ijsglans gives a
 * small effect (a chance to freeze what you hit, see {@link Bevroren}).
 */
public final class Kristallen {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KWALITEIT = COMPONENTS.registerComponentType("vr_z2_kwaliteit",
            b -> b.persistent(Codec.intRange(0, 2)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final String[] KWALITEITEN = {"ruw", "geslepen", "stralend"};
    /** Stat factor per quality. */
    public static final double[] FACTOR = {1.0, 1.7, 2.6};

    /** One crystal kind: a stat with its value at "ruw" (or null + the ijsglans effect), its colour. */
    public record Soort(String id, @Nullable Stat stat, double waarde, int kleur) {
        public String itemId() {
            return "vr_z2_kristal_" + id;
        }

        public double waarde(int kwaliteit) {
            return waarde * FACTOR[Math.max(0, Math.min(2, kwaliteit))];
        }

        public String naamKey() {
            return "item.guhs." + itemId();
        }
    }

    public static final Map<String, Soort> SOORTEN = new LinkedHashMap<>();
    /** Ijsglans: freeze chance in % per gem at ruw (x FACTOR), all worn ijsglans gems add up, capped at {@link #IJS_MAX}. */
    public static final String IJSGLANS = "ijsglans";
    public static final double IJS_MAX = 35;

    static {
        soort(new Soort("rozekwarts", Stat.VADS, 35, 0xFF8AC8));
        soort(new Soort("zonnesteen", Stat.KRITIEK, 2, 0xFFC84A));
        soort(new Soort("galmkristal", Stat.AANVALSSNELHEID, 3, 0x9AD8FF));
        soort(new Soort("spiegelkwarts", Stat.VERDEDIGING, 4, 0xE4E8F0));
        soort(new Soort("vonkkristal", Stat.ELEMENTSCHADE, 4, 0xFF7A4A));
        soort(new Soort(IJSGLANS, null, 6, 0x7AF0F0));
    }

    public static final Map<String, DeferredItem<KristalItem>> ITEM = new LinkedHashMap<>();

    static {
        for (Soort s : SOORTEN.values()) {
            ITEM.put(s.id(), ITEMS.registerItem(s.itemId(), p -> new KristalItem(p, s.id()), () -> new Item.Properties().stacksTo(64)));
        }
    }

    private Kristallen() {
    }

    private static void soort(Soort s) {
        SOORTEN.put(s.id(), s);
    }

    /** The kern hooks: socket rolls, socket stats, tooltip lines. */
    static void init() {
        Rollen.SOKJES = Kristallen::sokjes;
        Rollen.SOKJE_BONUS = Kristallen::stats;
        nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem.TOOLTIP_EXTRA.add(Kristallen::tooltip);
    }

    // ---- sockets --------------------------------------------------------------------------------------------------------

    /** The socket roll: zone-1 levels (< 20) only 1 at Legendarisch+, zone-2 drops (lv 20+) 0-2, rarer = more. */
    public static int sokjes(BasisType t, int level, Zeldzaamheid z, RandomSource r) {
        if (level < Z2SystemenPlekken.ZONE2_LEVEL) {
            return z.ordinal() >= Zeldzaamheid.LEGENDARISCH.ordinal() ? 1 : 0;
        }
        // chance of a 1st and a 2nd socket per rarity (gewoon .. guhddelijk)
        double[] een = {0.30, 0.45, 0.60, 0.80, 1.0, 1.0};
        double[] twee = {0.0, 0.08, 0.18, 0.35, 0.55, 1.0};
        int i = z.ordinal();
        if (r.nextDouble() >= een[i]) {
            return 0;
        }
        return r.nextDouble() < twee[i] ? 2 : 1;
    }

    @Nullable
    public static Soort soort(String gem) {
        int c = gem.indexOf(':');
        return SOORTEN.get(c < 0 ? gem : gem.substring(0, c));
    }

    public static int kwaliteit(String gem) {
        int c = gem.indexOf(':');
        try {
            return c < 0 ? 0 : Math.max(0, Math.min(2, Integer.parseInt(gem.substring(c + 1))));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String gem(String soort, int kwaliteit) {
        return soort + ":" + kwaliteit;
    }

    /** The stats of the gems in a piece (not when verpluisd: kern returns before). */
    public static Stats stats(Uitrusting u) {
        Stats s = new Stats();
        for (String g : u.sokjes()) {
            Soort k = g.isEmpty() ? null : soort(g);
            if (k != null && k.stat() != null) {
                s.plus(k.stat(), k.waarde(kwaliteit(g)));
            }
        }
        return s;
    }

    /** The freeze chance (%) of the ijsglans gems in one piece. */
    public static double ijsKans(Uitrusting u) {
        double n = 0;
        for (String g : u.sokjes()) {
            Soort k = g.isEmpty() ? null : soort(g);
            if (k != null && k.id().equals(IJSGLANS)) {
                n += k.waarde(kwaliteit(g));
            }
        }
        return n;
    }

    /** Every counted piece of this player (weapon in hand, armour, jewellery: the same rules as kern's Spelers.stats). */
    public static List<Uitrusting> gedragen(ServerPlayer p) {
        List<Uitrusting> l = new ArrayList<>();
        int lvl = Spelers.level(p);
        Uitrusting w = Spelers.wapen(p);
        if (w != null) {
            l.add(w);
        }
        for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Uitrusting u = Rollen.van(p.getItemBySlot(es));
            if (u != null && u.basisType() != null && u.basisType().slot().vanilla == es && u.level() <= lvl) {
                l.add(u);
            }
        }
        for (Slot slot : Slot.SIERADEN) {
            Uitrusting u = Rollen.van(Sieraden.get(p, slot));
            if (u != null && u.basisType() != null && u.basisType().slot() == slot && u.level() <= lvl) {
                l.add(u);
            }
        }
        return l;
    }

    /** All worn ijsglans together (capped). */
    public static double ijsKans(ServerPlayer p) {
        double n = 0;
        for (Uitrusting u : gedragen(p)) {
            if (!u.verpluisd()) {
                n += ijsKans(u);
            }
        }
        return Math.min(IJS_MAX, n);
    }

    // ---- the item -----------------------------------------------------------------------------------------------------------

    public static ItemStack stack(String soort, int kwaliteit, int n) {
        DeferredItem<KristalItem> it = ITEM.get(soort);
        if (it == null) {
            return ItemStack.EMPTY;
        }
        ItemStack s = new ItemStack(it.get(), n);
        s.set(KWALITEIT.get(), Math.max(0, Math.min(2, kwaliteit)));
        return s;
    }

    public static ItemStack stack(String gem) {
        Soort k = soort(gem);
        return k == null ? ItemStack.EMPTY : stack(k.id(), kwaliteit(gem), 1);
    }

    /** The gem id of a crystal stack, or null. */
    @Nullable
    public static String gem(ItemStack s) {
        if (s.getItem() instanceof KristalItem k) {
            return gem(k.soort, s.getOrDefault(KWALITEIT.get(), 0));
        }
        return null;
    }

    /** The quality a drop from this source gets: 0 normal mob, 1 elite, 2 boss. */
    public static int dropKwaliteit(boolean elite, boolean baas) {
        return baas ? 2 : elite ? 1 : 0;
    }

    /** One line "♦ Rozekwarts (geslepen): +60 Vads" for a gem. */
    public static MutableComponent regel(String gem) {
        Soort k = soort(gem);
        if (k == null) {
            return Component.literal("?");
        }
        int q = kwaliteit(gem);
        MutableComponent c = Component.literal("◆ ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(k.kleur())))
                .append(Component.translatable(k.naamKey()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(k.kleur()))))
                .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("vadsrijk.z2.systemen.kwaliteit." + KWALITEITEN[q]).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("): ").withStyle(ChatFormatting.DARK_GRAY));
        return c.append(effect(k, q));
    }

    public static MutableComponent effect(Soort k, int q) {
        double v = k.waarde(q);
        if (k.stat() == null) {
            return Component.translatable("vadsrijk.z2.systemen.kristal.ijsglans.effect", String.format(Locale.ROOT, "%.0f", v))
                    .withStyle(ChatFormatting.AQUA);
        }
        String getal = k.stat().procent ? String.format(Locale.ROOT, "+%.1f%% ", v) : String.format(Locale.ROOT, "+%.0f ", v);
        return Component.literal(getal).withStyle(ChatFormatting.AQUA).append(Component.translatable(k.stat().key()).withStyle(ChatFormatting.GRAY));
    }

    /** Kern tooltip hook: the socket lines (only on pieces with sockets). */
    static void tooltip(Uitrusting u, Consumer<Component> t) {
        if (u.sokjes().isEmpty()) {
            return;
        }
        for (String g : u.sokjes()) {
            if (g.isEmpty()) {
                t.accept(Component.translatable("vadsrijk.z2.systemen.sokje.leeg").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                t.accept(regel(g));
            }
        }
    }

    /** A raw crystal. Its name says the quality; the tooltip what it does and where it goes. */
    public static class KristalItem extends Item {
        public final String soort;

        public KristalItem(Properties p, String soort) {
            super(p);
            this.soort = soort;
        }

        @Override
        public Component getName(ItemStack stack) {
            int q = stack.getOrDefault(KWALITEIT.get(), 0);
            Soort k = SOORTEN.get(soort);
            return Component.translatable("vadsrijk.z2.systemen.kristal.naam", Component.translatable("vadsrijk.z2.systemen.kwaliteit.bijv." + KWALITEITEN[q]),
                    super.getName(stack)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(k == null ? 0xFFFFFF : k.kleur())));
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return stack.getOrDefault(KWALITEIT.get(), 0) >= 2;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            Soort k = SOORTEN.get(soort);
            if (k == null) {
                return;
            }
            int q = stack.getOrDefault(KWALITEIT.get(), 0);
            t.accept(effect(k, q));
            t.accept(Component.translatable("vadsrijk.z2.systemen.kristal.uitleg").withStyle(ChatFormatting.GRAY));
            t.accept(Component.translatable(k.naamKey() + ".flavour").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
