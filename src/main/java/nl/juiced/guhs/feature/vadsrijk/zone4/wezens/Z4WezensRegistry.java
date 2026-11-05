package nl.juiced.guhs.feature.vadsrijk.zone4.wezens;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Lantaarnlicht;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Sets;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Kust;

/**
 * Het Vadsrijk zone 4 "De Lampionkust", slice "t-z4wezens" (T_BRIEF): 9 ontvadste coast creatures that you cheer up (pink plof,
 * like zone 1) + the Mika kite crew of the Wrak, GeckoLib models from tools/features/vadsrijk_t_z4wezens.py, few and gentle
 * spawners per island ({@link Z4WezensSpawners}, spots in {@link Z4WezensPlekken}), 2 sets + 3 jewellery + 6 class weapons +
 * 2 socket crystals ({@link Z4Loot}, {@link Z4Sets}) and the dev command {@code /guhs vadsrijk z4wezens ...}. Ids: entities and
 * gear items guhs:vr_z4_mob_*, weapons guhs:vr_z4_&lt;wapen&gt;, crystals guhs:vr_z4_kristal_*, loot tables z4wezens:*.
 */
public final class Z4WezensRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String naam, EntityType.EntityFactory<T> f, float b, float h) {
        String id = "vr_z4_mob_" + naam;
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.8f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Mopperkrab>> MOPPERKRAB = mob("mopperkrab", Kust.Mopperkrab::new, 1.1f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Mokmeeuw>> MOKMEEUW = mob("mokmeeuw", Kust.Mokmeeuw::new, 0.8f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Lampionkwal>> LAMPIONKWAL = mob("lampionkwal", Kust.Lampionkwal::new, 0.9f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Bromvlieger>> BROMVLIEGER = mob("bromvlieger", Kust.Bromvlieger::new, 1.1f, 1.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Zeurslak>> ZEURSLAK = mob("zeurslak", Kust.Zeurslak::new, 1.0f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Koraalkobold>> KORAALKOBOLD = mob("koraalkobold", Kust.Koraalkobold::new, 0.6f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Pruilrob>> PRUILROB = mob("pruilrob", Kust.Pruilrob::new, 1.2f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Stekelpluis>> STEKELPLUIS = mob("stekelpluis", Kust.Stekelpluis::new, 0.8f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Mistmopje>> MISTMOPJE = mob("mistmopje", Kust.Mistmopje::new, 0.8f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kust.Vliegerknecht>> VLIEGERKNECHT = mob("vliegerknecht", Kust.Vliegerknecht::new, 0.7f, 1.7f);

    /** Command name -> type (dev command, gametests), along the slinger. The 9 cheer-up creatures, then the Mika crew. */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> SOORTEN = new LinkedHashMap<>();

    static {
        SOORTEN.put("mopperkrab", MOPPERKRAB);
        SOORTEN.put("koraalkobold", KORAALKOBOLD);
        SOORTEN.put("stekelpluis", STEKELPLUIS);
        SOORTEN.put("zeurslak", ZEURSLAK);
        SOORTEN.put("lampionkwal", LAMPIONKWAL);
        SOORTEN.put("mokmeeuw", MOKMEEUW);
        SOORTEN.put("bromvlieger", BROMVLIEGER);
        SOORTEN.put("pruilrob", PRUILROB);
        SOORTEN.put("mistmopje", MISTMOPJE);
        SOORTEN.put("vliegerknecht", VLIEGERKNECHT);
    }

    /** The gear items (one per base type). */
    public static final Map<String, DeferredItem<Item>> ITEMS_PER_TYPE = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> WAPEN_ITEMS = new LinkedHashMap<>();

    static {
        Z4Loot.STUKKEN.forEach((id, slot) -> ITEMS_PER_TYPE.put(id, item("vr_z4_mob_" + id, slot)));
        for (Z4Loot.Nieuw n : Z4Loot.nieuweStukken()) {   // T-ITEMS slots (gloves / cape / armband): only when they exist
            ITEMS_PER_TYPE.put(n.id(), ITEMS.registerItem("vr_z4_mob_" + n.id(), RijkItem::new, () -> new Item.Properties().stacksTo(1)));
        }
        for (String id : Z4Loot.WAPENS) {
            WAPEN_ITEMS.put(id, ITEMS.registerItem("vr_z4_" + id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)));
        }
        // the 2 zone-4 crystals on the zone-2 gem system (same qualities, same juwelier)
        kristal(new Kristallen.Soort("parelkristal", Stat.VADS, 60, 0xF4E8F0, "vr_z4_kristal_parelkristal"));
        kristal(new Kristallen.Soort("zeeglas", Stat.ELEMENTWEERSTAND, 5, 0x7AD8C8, "vr_z4_kristal_zeeglas"));
    }

    private static void kristal(Kristallen.Soort s) {
        Kristallen.SOORTEN.put(s.id(), s);
        Kristallen.ITEM.put(s.id(), ITEMS.registerItem(s.itemId(), p -> new Kristallen.KristalItem(p, s.id()), () -> new Item.Properties().stacksTo(64)));
    }

    private static DeferredItem<Item> item(String id, Slot slot) {
        if (slot.sieraad()) {
            return ITEMS.registerItem(id, RijkItem::new, () -> new Item.Properties().stacksTo(1));
        }
        return ITEMS.registerItem(id, RijkItem::new, () -> KernRegistry.harnasEigenschappen(slot, id));
    }

    private Z4WezensRegistry() {
    }

    private static AttributeSupplier attr(double snelheid, double knock) {
        return VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, snelheid).add(Attributes.FOLLOW_RANGE, 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, knock).build();
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(MOPPERKRAB.get(), attr(0.24, 0.5));
            e.put(MOKMEEUW.get(), attr(0.25, 0.2));
            e.put(LAMPIONKWAL.get(), attr(0.2, 0.3));
            e.put(BROMVLIEGER.get(), attr(0.25, 0.3));
            e.put(ZEURSLAK.get(), attr(0.14, 0.8));
            e.put(KORAALKOBOLD.get(), attr(0.3, 0.1));
            e.put(PRUILROB.get(), attr(0.2, 0.7));
            e.put(STEKELPLUIS.get(), attr(0.16, 0.6));
            e.put(MISTMOPJE.get(), attr(0.22, 0.2));
            e.put(VLIEGERKNECHT.get(), attr(0.28, 0.3));
        });
        Z4Loot.basisTypes();
        Z4Loot.tabellen();
        Z4Loot.kristalHaak();
        Spelers.EXTRA_STATS.add(Z4Sets::stats);
        Lantaarnlicht.DRAGERS.add(p -> Z4Sets.stukken(p, Z4Sets.LAMPION) >= 4);
        Z4WezensSpawners.registreer();
        haken();
        NeoForge.EVENT_BUS.addListener(Z4WezensCommando::register);
    }

    /** Damage hooks: the Mopperkrab's claws (x0.5 from the front), the Schelpenset 4 ("Schade tegen bazen +8%"). */
    private static void haken() {
        Schade.VOOR_KLAP.add((k, n) -> {
            LivingEntity doel = k.doel(), bron = k.bron();
            if (!(doel.level() instanceof ServerLevel sl) || k.soort() == Schade.Soort.WAAR) {
                return n;
            }
            if (doel instanceof Kust.Mopperkrab krab && krab.schildVoor(bron)) {
                n *= 0.5;
                if (sl.getGameTime() % 20 < 4) {
                    Getallen.tekst(sl, krab.getX(), krab.getY() + krab.getBbHeight() + 0.3, krab.getZ(), Component.translatable("vadsrijk.z4wezens.scharen"), 0.7f);
                }
            }
            if (bron instanceof Player p && doel instanceof VadsrijkBaas && Z4Sets.stukken(p, Z4Sets.SCHELPEN) >= 4) {
                n *= Z4Sets.BAAS_FACTOR;
            }
            return n;
        });
    }
}
