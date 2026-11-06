package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoItems;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * T-ITEMS, "itemisatie 2.0" (VOORSTEL_ROADMAP §2.1/2.3/2.4/2.5 + R2-R5): the new slots (2nd ring, armband, handschoenen, cape,
 * the drankjesriem), the 4 new stats ({@link NieuweStats}), Erfstukken ({@link Erfstukken}), the smid's safety nets
 * ({@link Vangnet}, {@link SmidExtra}) and one balance file section ({@link ItemsBalans}). Registered after every other slice
 * (VadsrijkFeature), so its loot additions see every table.
 * <p>Content: 16 base types for the new slots over zones 1-3 ({@link #STUKKEN}; 7 of them belong to a set, {@link Sets}), and 2
 * items: the Groeipluis (doorgroeien) and the Knuffeltalisman (a consumable against star loss). Drops: the new pieces are added
 * to the creature tables of their zone at server start ({@link #lootErbij}); every boss gives each helper a Groeipluis and a
 * 20 % chance at a Knuffeltalisman (elites 3 %); the smid also sells talismans.
 * Dev: {@code /guhs vadsrijk items ...} ({@link ItemsCommando}).
 */
public final class ItemsRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** One new base type: id, slot, zone (1-3), set (null = loose), extra stats (stat, value at level 1, ...). */
    public record Stuk(String id, Slot slot, int zone, String set, Object[] stats) {
    }

    public static final Map<String, Stuk> STUKKEN = new LinkedHashMap<>();

    static {
        // zone 1 (De Knuffelvlakte, lv 1-20)
        stuk("pluiswantjes", Slot.HANDSCHOENEN, 1, null, Stat.VERDEDIGING, 2, Stat.KRACHT, 2);
        stuk("kloddergrijpers", Slot.HANDSCHOENEN, 1, "klodder", Stat.VERDEDIGING, 2, Stat.BEHENDIGHEID, 2, Stat.KRITIEK, 1);
        stuk("knuffelcapeje", Slot.CAPE, 1, null, Stat.VADS, 12, Stat.ELEMENTWEERSTAND, 2);
        stuk("sporencape", Slot.CAPE, 1, "zwam", Stat.VADS, 10, Stat.WIJSHEID, 2);
        stuk("pluisarmband", Slot.ARMBAND, 1, null, Stat.AANVALSSNELHEID, 2);
        stuk("rozenarmbandje", Slot.ARMBAND, 1, null, Stat.COOLDOWNREDUCTIE, 2);
        // zone 2 (lv 20-30)
        stuk("ijswantjes", Slot.HANDSCHOENEN, 2, null, Stat.VERDEDIGING, 5, Stat.ELEMENTWEERSTAND, 5);
        stuk("kristalhandschoenen", Slot.HANDSCHOENEN, 2, "kristal", Stat.VERDEDIGING, 5, Stat.WIJSHEID, 5);
        stuk("galmcape", Slot.CAPE, 2, null, Stat.VADS, 26, Stat.KRITIEKSCHADE, 5);
        stuk("buitcape", Slot.CAPE, 2, "mika", Stat.VADS, 22, Stat.KRACHT, 4, Stat.BEHENDIGHEID, 3);
        stuk("kristalarmband", Slot.ARMBAND, 2, null, Stat.AANVALSSNELHEID, 3, Stat.KRITIEK, 1);
        // zone 3 (De Schemerwortel, lv 30-50)
        stuk("bloesemhandjes", Slot.HANDSCHOENEN, 3, "bloesem", Stat.VERDEDIGING, 7, Stat.BEHENDIGHEID, 7, Stat.KRITIEK, 2);
        stuk("schorshandschoenen", Slot.HANDSCHOENEN, 3, "schors", Stat.VERDEDIGING, 10, Stat.VADS, 20);
        stuk("wortelcape", Slot.CAPE, 3, "wortel", Stat.VADS, 36, Stat.WIJSHEID, 7, Stat.ELEMENTWEERSTAND, 5);
        stuk("schemercape", Slot.CAPE, 3, null, Stat.VADS, 42, Stat.LOOPSNELHEID, 3);
        stuk("honingarmband", Slot.ARMBAND, 3, null, Stat.COOLDOWNREDUCTIE, 3, Stat.HEALKRACHT, 5);
        stuk("harsarmband", Slot.ARMBAND, 3, null, Stat.AANVALSSNELHEID, 4, Stat.LEVENSROOF, 1);
    }

    public static final Map<String, DeferredItem<Item>> ITEM = new LinkedHashMap<>();

    static {
        for (Stuk s : STUKKEN.values()) {
            ITEM.put(s.id(), ITEMS.registerItem(itemNaam(s.id()), RijkItem::new, () -> new Item.Properties().stacksTo(1)));
        }
    }

    public static final DeferredItem<EcoItems.Uitleg> GROEIPLUIS = ITEMS.registerItem("vr_t_groeipluis", EcoItems.Uitleg::new,
            () -> new Item.Properties().stacksTo(99).rarity(Rarity.EPIC));
    public static final DeferredItem<EcoItems.Uitleg> KNUFFELTALISMAN = ITEMS.registerItem("vr_t_knuffeltalisman", EcoItems.Uitleg::new,
            () -> new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    private ItemsRegistry() {
    }

    private static void stuk(String id, Slot slot, int zone, String set, Object... stats) {
        STUKKEN.put(id, new Stuk(id, slot, zone, set, stats));
        if (set != null) {
            Sets.SET_VAN.put(id, set);
        }
    }

    public static String itemNaam(String id) {
        return "vr_t_" + id;
    }

    /** The base type of a new piece; growth per level like zone 2's loot (main stats 25 % of the base, side stats 6 %). */
    static BasisType basisType(Stuk s) {
        Stats basis = new Stats(), groei = new Stats();
        for (int i = 0; i + 1 < s.stats().length; i += 2) {
            Stat st = (Stat) s.stats()[i];
            double v = ((Number) s.stats()[i + 1]).doubleValue();
            basis.plus(st, v);
            groei.plus(st, v * (st.hoofd() ? 0.25 : 0.06));
        }
        return new BasisType(s.id(), "guhs:" + itemNaam(s.id()), s.slot(), soort(s.slot()), null, Element.GEEN, 0, 0, basis, groei, 2);
    }

    private static String soort(Slot s) {
        return switch (s) {
            case HANDSCHOENEN -> "handschoenen";
            case CAPE -> "cape";
            default -> "sieraad";
        };
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        for (Stuk s : STUKKEN.values()) {
            BasisTypes.registreer(basisType(s));
        }
        Rollen.SLOT_BUDGET = ItemsBalans::budget;
        Rollen.ERF_FACTOR = Erfstukken::factor;
        NieuweStats.installeer();
        Haken.VERSLAGEN.add(Erfstukken::verslagen);
        Haken.VERSLAGEN.add(ItemsRegistry::bazenBuit);
        Haken.BINNEN.add((p, binnen) -> Drankjesriem.stuur(p));
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> lootErbij());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                Drankjesriem.stuur(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Vangnet.vergeet(e.getEntity().getUUID());
            NieuweStats.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener(ItemsCommando::register);
    }

    public static void payloads(PayloadRegistrar r) {
        ItemsPayloads.register(r);
    }

    /** Bosses: a Groeipluis for every helper, 20 % a Knuffeltalisman; elites 3 % a talisman. */
    static void bazenBuit(Haken.Verslagen v) {
        boolean baas = v.mob() instanceof VadsrijkBaas;
        if (!baas && !v.mob().elite()) {
            return;
        }
        for (var p : v.helpers()) {
            if (!Vadsrijk.in(p)) {
                continue;
            }
            if (baas) {
                EcoUtil.geefOfBewaar(p, new ItemStack(GROEIPLUIS.get()));
            }
            if (p.getRandom().nextDouble() < (baas ? 0.20 : 0.03)) {
                EcoUtil.geefOfBewaar(p, new ItemStack(KNUFFELTALISMAN.get()));
            }
        }
    }

    /** Which zone's pieces a loot table gets (0 = none): by the slice prefix of its id. */
    static int zoneVan(String tabel) {
        if (tabel.startsWith("z3wezens:")) {
            return 3;
        }
        if (tabel.startsWith("z2wezens:")) {
            return 2;
        }
        if (tabel.startsWith("wezens:") || tabel.startsWith("kern:") || tabel.startsWith("economie:baas_")) {
            return 1;
        }
        return 0;
    }

    private static boolean klaar;

    /**
     * Once, at server start (every slice has registered its tables): every creature/boss table gets its zone's loose new pieces
     * (weight 1), and a table that drops pieces of a set gets that set's glove/cape at the weight of its first set piece.
     */
    static synchronized void lootErbij() {
        if (klaar) {
            return;
        }
        klaar = true;
        Map<String, String> setStukVan = new LinkedHashMap<>();   // some piece of the set -> set name
        for (BasisType t : BasisTypes.alle()) {
            String s = setVanBestaand(t.id());
            if (s != null) {
                setStukVan.put(t.id(), s);
            }
        }
        for (Map.Entry<String, LootTabel> e : new ArrayList<>(LootTabellen.alle().entrySet())) {
            int zone = zoneVan(e.getKey());
            if (zone == 0) {
                continue;
            }
            LootTabel t = e.getValue();
            List<LootTabel.Regel> r = new ArrayList<>(t.regels());
            if (r.stream().anyMatch(x -> STUKKEN.containsKey(x.basisType()))) {
                continue;
            }
            for (Stuk s : STUKKEN.values()) {
                if (s.set() == null && s.zone() == zone) {
                    r.add(new LootTabel.Regel(s.id(), 1));
                }
            }
            for (Stuk s : STUKKEN.values()) {
                if (s.set() == null) {
                    continue;
                }
                for (LootTabel.Regel x : t.regels()) {
                    if (s.set().equals(setStukVan.get(x.basisType()))) {
                        r.add(new LootTabel.Regel(s.id(), x.gewicht()));
                        break;
                    }
                }
            }
            LootTabellen.registreer(e.getKey(), new LootTabel(r, t.rollen(), t.kans(), t.geluk(), t.muntenMin(), t.muntenMax()));
        }
    }

    /** The set of an existing (armour) base type of any zone, or null. */
    static String setVanBestaand(String id) {
        String s = nl.juiced.guhs.feature.vadsrijk.wezens.loot.SetBonussen.setVan(id);
        if (s == null) {
            s = nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets.setVan(id);
        }
        if (s == null) {
            s = nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Sets.setVan(id);
        }
        return s;
    }
}
