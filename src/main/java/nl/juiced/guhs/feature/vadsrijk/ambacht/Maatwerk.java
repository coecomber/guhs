package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * Maatwerk and Meesterwerk (VOORSTEL §3.4, A1 + A3):
 * <ul>
 *   <li><b>Maatwerk</b> (Smeden: weapons of the 3 classes + helm + borstplaat; Naaien: broek + schoentjes): Episch, base stats at
 *   ~90 % of that tier's dungeon/zone set ({@link #MAATWERK}), you CHOOSE 2 of the bonuses (the rest rolls), +1 guaranteed socket
 *   (max 3), NO set bonus (its base types are in no set), tradeable;</li>
 *   <li><b>Meesterwerk jewellery</b> (Smeden + a boss core): Legendarisch, 3 chosen bonuses + 1 small property ({@link Eigenschap}),
 *   base stats above every dropped piece of its tier: Best in Slot (A3). Bound (gebonden) once made.</li>
 * </ul>
 * Vadsigheid pushes the chosen bonuses up their range (100 %: always in the top 20 %). Base type ids {@code mw_t<tier>_<soort>};
 * items {@code guhs:vr_amb_mw_<soort>} (one item per kind, its name from the base type). Extra data: component {@code guhs:vr_amb_maatwerk}.
 */
public final class Maatwerk {
    public static final double MAATWERK = 0.9;
    /** The gear level of each tier's recipes. */
    public static final int[] GEAR_LEVEL = {0, 15, 30, 45};

    /** What a made piece remembers: the chosen stats and the Meesterwerk property ("" = none). */
    public record Info(List<String> gekozen, String eigenschap, boolean meesterwerk) {
        public static final Codec<Info> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("gekozen").forGetter(Info::gekozen),
                Codec.STRING.optionalFieldOf("eigenschap", "").forGetter(Info::eigenschap),
                Codec.BOOL.optionalFieldOf("meesterwerk", false).forGetter(Info::meesterwerk)).apply(i, Info::new));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Info>> INFO = Materialen.COMPONENTS.registerComponentType("vr_amb_maatwerk",
            b -> b.persistent(Info.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Info.CODEC)));

    /** A small Meesterwerk property (clear names, user rule). */
    public enum Eigenschap {
        BAZEN("bazen", 8), ELITES("elites", 10), EERSTE_KLAP("eerste_klap", 25), TAAI("taai", 6), NOODKRACHT("noodkracht", 12);

        public final String id;
        public final int procent;

        Eigenschap(String id, int procent) {
            this.id = id;
            this.procent = procent;
        }

        public String key() {
            return "vadsrijk.ambacht.eigenschap." + id;
        }

        @Nullable
        public static Eigenschap van(String id) {
            for (Eigenschap e : values()) {
                if (e.id.equals(id)) {
                    return e;
                }
            }
            return null;
        }
    }

    /** One kind of made gear: its item, slot, class (weapons) and which ambacht makes it. */
    public record Soort(String id, Slot slot, @Nullable String klasse, String wapen, Ambacht ambacht) {
        public String itemId() {
            return "vr_amb_mw_" + id;
        }

        public String basisId(int tier) {
            return "mw_t" + tier + "_" + id;
        }
    }

    public static final Map<String, Soort> SOORTEN = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<MaatwerkItem>> ITEM = new LinkedHashMap<>();

    static {
        s(new Soort("zwaard", Slot.WAPEN, Klassen.RIDDER, "zwaard", Ambacht.SMEDEN));
        s(new Soort("boog", Slot.WAPEN, Klassen.JAGER, "boog", Ambacht.SMEDEN));
        s(new Soort("staf", Slot.WAPEN, Klassen.MAGIER, "staf", Ambacht.SMEDEN));
        s(new Soort("helm", Slot.HOOFD, null, "harnas", Ambacht.SMEDEN));
        s(new Soort("borstplaat", Slot.BORST, null, "harnas", Ambacht.SMEDEN));
        s(new Soort("broek", Slot.BENEN, null, "harnas", Ambacht.NAAIEN));
        s(new Soort("schoentjes", Slot.VOETEN, null, "harnas", Ambacht.NAAIEN));
        s(new Soort("ring", Slot.RING, null, "sieraad", Ambacht.SMEDEN));
        s(new Soort("ketting", Slot.KETTING, null, "sieraad", Ambacht.SMEDEN));
        s(new Soort("oorbel", Slot.OORBEL, null, "sieraad", Ambacht.SMEDEN));
    }

    private Maatwerk() {
    }

    private static void s(Soort s) {
        SOORTEN.put(s.id(), s);
        ITEM.put(s.id(), Materialen.ITEMS.registerItem(s.itemId(), MaatwerkItem::new, () -> s.slot() == Slot.WAPEN
                ? new Item.Properties().stacksTo(1).component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                : s.slot().harnas() ? KernRegistry.harnasEigenschappen(s.slot(), null) : new Item.Properties().stacksTo(1)));
    }

    /**
     * The base types (call once at registration). Reference per tier (the zone's dropped set, ~average per slot) x 0.9:
     * armour {verdediging, vads}; weapons {damage, per level} from the zone weapons; jewellery = Meesterwerk, ABOVE the drops.
     */
    static void basisTypes() {
        double[][] helm = {{}, {3, 10}, {9, 28}, {15, 46}}, borst = {{}, {5, 16}, {15, 46}, {24, 74}}, broek = {{}, {4, 13}, {12, 37}, {20, 63}},
                voet = {{}, {2, 9}, {6, 23}, {13, 40}};
        double[][] wapen = {{}, {5.5, 1.1}, {5.95, 1.19}, {6.35, 1.27}};
        for (int t = 1; t <= 3; t++) {
            String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
            wapen("zwaard", t, r, Element.ROZE, wapen[t], Stats.van(Stat.KRACHT, 2.0 * t), Stats.van(Stat.KRACHT, 0.35));
            wapen("boog", t, j, Element.NATUUR, wapen[t], Stats.van(Stat.BEHENDIGHEID, 2.0 * t), Stats.van(Stat.BEHENDIGHEID, 0.35));
            wapen("staf", t, m, Element.VUUR, wapen[t], Stats.van(Stat.WIJSHEID, 2.0 * t), Stats.van(Stat.WIJSHEID, 0.35));
            harnas("helm", t, helm[t]);
            harnas("borstplaat", t, borst[t]);
            harnas("broek", t, broek[t]);
            harnas("schoentjes", t, voet[t]);
            // Meesterwerk jewellery: tier 3 ~20 % above the zone-3 drops (honingring/harsketting/sporenoorbel), tiers 1/2 scaled down
            double f = t == 3 ? 1.0 : t == 2 ? 0.62 : 0.35;
            sieraad("ring", t, Stats.van(Stat.KRITIEK, 4 * f, Stat.KRITIEKSCHADE, 10 * f));
            sieraad("ketting", t, Stats.van(Stat.VADS, 50 * f, Stat.VERDEDIGING, 12 * f));
            sieraad("oorbel", t, Stats.van(Stat.ELEMENTSCHADE, 11 * f, Stat.COOLDOWNREDUCTIE, 2.5 * f));
        }
    }

    private static void wapen(String id, int t, String klasse, Element e, double[] dmg, Stats basis, Stats groei) {
        Soort s = SOORTEN.get(id);
        BasisTypes.registreer(BasisType.wapen(s.basisId(t), "guhs:" + s.itemId(), s.wapen(), klasse, e, dmg[0] * MAATWERK, dmg[1] * MAATWERK,
                basis.maal(MAATWERK), groei.maal(MAATWERK), 0));
    }

    private static void harnas(String id, int t, double[] r) {
        Soort s = SOORTEN.get(id);
        double vd = r[0] * MAATWERK, vads = r[1] * MAATWERK;
        BasisTypes.registreer(new BasisType(s.basisId(t), "guhs:" + s.itemId(), s.slot(), "harnas", null, Element.GEEN, 0, 0,
                Stats.van(Stat.VERDEDIGING, vd, Stat.VADS, vads), Stats.van(Stat.VERDEDIGING, vd * 0.25, Stat.VADS, vads * 0.2), 0));
    }

    private static void sieraad(String id, int t, Stats basis) {
        Soort s = SOORTEN.get(id);
        Stats groei = new Stats();
        basis.alles().forEach((st, v) -> groei.plus(st, v * 0.06));
        BasisTypes.registreer(BasisType.sieraad(s.basisId(t), "guhs:" + s.itemId(), s.slot(), basis, groei, 0));
    }

    // ---- making ------------------------------------------------------------------------------------------------------------

    /** The stats a player may choose from (the whole bonus pool). */
    public static List<Stat> keuzes() {
        return List.of(Stat.values());
    }

    /**
     * Makes a piece: tier, the chosen stats (2 for Maatwerk, 3 for Meesterwerk; extras ignored, duplicates dropped), Vadsigheid,
     * maker, the Meesterwerk property (null = Maatwerk).
     */
    public static ItemStack maak(Soort s, int tier, List<Stat> gekozen, int vadsigheid, String maker, @Nullable Eigenschap eigenschap, RandomSource r) {
        BasisType t = BasisTypes.van(s.basisId(tier));
        if (t == null) {
            return ItemStack.EMPTY;
        }
        boolean meester = eigenschap != null;
        Zeldzaamheid z = meester ? Zeldzaamheid.LEGENDARISCH : Zeldzaamheid.EPISCH;
        int level = GEAR_LEVEL[Math.max(1, Math.min(3, tier))];
        int kies = meester ? 3 : 2;
        Set<Stat> gezien = EnumSet.noneOf(Stat.class);
        List<Bonus> bonussen = new ArrayList<>();
        List<String> namen = new ArrayList<>();
        double laag = Math.max(0, Math.min(1, vadsigheid / 100.0)) * 0.8;
        for (Stat st : gekozen) {
            if (bonussen.size() >= kies || !gezien.add(st)) {
                continue;
            }
            double[] b = Bonussen.bereik(st, level);
            double u = laag + (1 - laag) * r.nextDouble();
            double v = b[0] + (b[1] - b[0]) * u;
            v = st.procent ? Math.round(v * 10) / 10.0 : Math.round(v);
            bonussen.add(new Bonus(st, Math.max(b[0], Math.min(b[1], v))));
            namen.add(st.name().toLowerCase(java.util.Locale.ROOT));
        }
        int rest = z.maxBonus - bonussen.size();
        for (int poging = 0; poging < 20 && rest > 0; poging++) {
            for (Bonus b : Bonussen.rol(rest, level, z, r)) {
                if (rest > 0 && gezien.add(b.stat())) {
                    bonussen.add(b);
                    rest--;
                }
            }
        }
        int sokjes = Math.min(3, Rollen.SOKJES.aantal(t, level, z, r) + 1);
        Uitrusting u = new Uitrusting(t.id(), z, level, bonussen, t.element(), 0, 0, false, meester, r.nextLong(),
                java.util.Collections.nCopies(sokjes, ""));
        ItemStack stack = Rollen.stack(u);
        stack.set(INFO.get(), new Info(namen, meester ? eigenschap.id : "", meester));
        Gemaakt.zet(stack, vadsigheid, maker);
        return stack;
    }

    @Nullable
    public static Info info(ItemStack s) {
        return s.isEmpty() ? null : s.get(INFO.get());
    }

    // ---- the Meesterwerk properties (kern Schade.VOOR_KLAP) ----------------------------------------------------------------

    /** The properties a player wears (on their jewellery). */
    static Set<Eigenschap> gedragen(ServerPlayer p) {
        Set<Eigenschap> s = EnumSet.noneOf(Eigenschap.class);
        for (Slot sl : Slot.SIERADEN) {
            Info i = info(Sieraden.get(p, sl));
            Uitrusting u = Rollen.van(Sieraden.get(p, sl));
            if (i != null && u != null && !u.verpluisd() && u.level() <= Spelers.level(p)) {
                Eigenschap e = Eigenschap.van(i.eigenschap());
                if (e != null) {
                    s.add(e);
                }
            }
        }
        return s;
    }

    static double voorKlap(Schade.Klap k, double schade) {
        if (k.bron() instanceof ServerPlayer p) {
            Set<Eigenschap> e = gedragen(p);
            if (!e.isEmpty()) {
                double f = 1;
                if (e.contains(Eigenschap.BAZEN) && k.doel() instanceof VadsrijkBaas) {
                    f += Eigenschap.BAZEN.procent / 100.0;
                }
                if (e.contains(Eigenschap.ELITES) && k.doel() instanceof VadsrijkMob m && m.elite()) {
                    f += Eigenschap.ELITES.procent / 100.0;
                }
                if (e.contains(Eigenschap.EERSTE_KLAP) && k.doel() instanceof VadsrijkMob m && m.vads() >= m.maxVads()) {
                    f += Eigenschap.EERSTE_KLAP.procent / 100.0;
                }
                if (e.contains(Eigenschap.NOODKRACHT) && Spelers.vads(p) < Spelers.maxVads(p) * 0.3) {
                    f += Eigenschap.NOODKRACHT.procent / 100.0;
                }
                schade *= f;
            }
        }
        if (k.doel() instanceof ServerPlayer p && k.bron() instanceof VadsrijkBaas && gedragen(p).contains(Eigenschap.TAAI)) {
            schade *= 1 - Eigenschap.TAAI.procent / 100.0;
        }
        return schade;
    }

    /** A made piece of gear: its base type's name (+ "Vadsig"), and the Maatwerk lines in the tooltip. */
    public static class MaatwerkItem extends RijkItem {
        public MaatwerkItem(Properties p) {
            super(p);
        }

        @Override
        public Component getName(ItemStack stack) {
            Uitrusting u = Rollen.van(stack);
            BasisType bt = u == null ? null : u.basisType();
            if (u == null || bt == null) {
                return super.getName(stack);
            }
            MutableComponent n = Component.empty();
            if (u.verpluisd()) {
                n.append(Component.translatable("vadsrijk.kern.verpluisd")).append(" ");
            }
            if (Gemaakt.vadsigheid(stack) >= 100) {
                n.append(Component.translatable("vadsrijk.ambacht.vadsig")).append(" ");
            }
            n.append(Component.translatable(bt.naamKey()));
            if (u.sterren() > 0) {
                n.append(" +" + u.sterren());
            }
            return n.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(u.zeldzaamheid().rgb)).withItalic(false));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            Info i = info(stack);
            if (i != null) {
                t.accept(Component.translatable(i.meesterwerk() ? "vadsrijk.ambacht.mw.meesterwerk" : "vadsrijk.ambacht.mw.maatwerk")
                        .withStyle(i.meesterwerk() ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD));
            }
            super.appendHoverText(stack, ctx, display, t, flag);
            if (i != null) {
                if (!i.gekozen().isEmpty()) {
                    MutableComponent g = Component.translatable("vadsrijk.ambacht.mw.gekozen");
                    for (int k = 0; k < i.gekozen().size(); k++) {
                        g.append(k == 0 ? " " : ", ").append(Component.translatable("vadsrijk.kern.stat." + i.gekozen().get(k)));
                    }
                    t.accept(g.withStyle(ChatFormatting.DARK_AQUA));
                }
                Eigenschap e = Eigenschap.van(i.eigenschap());
                if (e != null) {
                    t.accept(Component.translatable(e.key(), e.procent).withStyle(ChatFormatting.GOLD));
                }
                t.accept(Component.translatable("vadsrijk.ambacht.mw.geen_set").withStyle(ChatFormatting.DARK_GRAY));
            }
            Gemaakt.tooltip(stack, t);
        }
    }
}
