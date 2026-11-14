package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.rijdier.RijdierExtraRegistry;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.bultguh.Bultguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Astrolabiumwachter;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Glimmertje;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Sterrenguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Wormelien;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6Bazen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6BazenEffecten;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Zoutkolos;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Loot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Sets;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Duin;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Kloof;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Ruine;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Zout;

/**
 * Het Vadsrijk zone 6 "Het Zijdeland" (lv 60-70), the combat slice U-Z6WEZENS (ZONE6_PLAN §5-§9): 13 creatures (most of them
 * ontvadst desert animals you CHEER UP) + 5 area leaders, 2 field bosses (Koningin Wormelien by day, De Zoutkolos at night), the
 * dungeon De Verzonken Sterrenwacht (its own rooms {@link Z6Bouw}, trash + 3 bosses + chests), the cave elite De Zoutslang, the
 * loot (4 sets incl. the gloves/cape/armband slots, 5 jewellery, 6 spec-style weapons, 2 crystals, 10 named boss items, 3
 * trophies, 8 pets, Sterrenstof, Gouden Veren), the BULTGUH mount (two seats; {@link Bultguh}), the boss music hooks
 * ({@link Z6Muziek}) and the dev command {@code /guhs vadsrijk z6wezens ...}. Ids guhs:vr_z6_mob_* / vr_z6_baas_* / vr_z6_*, loot
 * tables z6wezens:*, lang vadsrijk.z6wezens.*, arenas z6wezens:*.
 */
public final class Z6WezensRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String id, EntityType.EntityFactory<T> f, float b, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.8f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> baas(String id, EntityType.EntityFactory<T> f, float b, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.8f).clientTrackingRange(16)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    // De Roze Duinen
    public static final DeferredHolder<EntityType<?>, EntityType<Duin.Zandvlooi>> ZANDVLOOI = mob("vr_z6_mob_zandvlooi", Duin.Zandvlooi::new, 0.5f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Duin.Fennekguh>> FENNEKGUH = mob("vr_z6_mob_fennekguh", Duin.Fennekguh::new, 0.7f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Duin.Springpiep>> SPRINGPIEP = mob("vr_z6_mob_springpiep", Duin.Springpiep::new, 0.6f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Duin.Duinkruiper>> DUINKRUIPER = mob("vr_z6_mob_duinkruiper", Duin.Duinkruiper::new, 1.0f, 0.6f);
    // De Palmenkloven
    public static final DeferredHolder<EntityType<?>, EntityType<Kloof.Dadelaapje>> DADELAAPJE = mob("vr_z6_mob_dadelaapje", Kloof.Dadelaapje::new, 0.6f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kloof.Gierguh>> GIERGUH = mob("vr_z6_mob_gierguh", Kloof.Gierguh::new, 1.0f, 0.9f);
    // De Zoutvlakte (+ the cave elite)
    public static final DeferredHolder<EntityType<?>, EntityType<Zout.Spiegelslang>> SPIEGELSLANG = mob("vr_z6_mob_spiegelslang", Zout.Spiegelslang::new, 1.1f, 0.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zout.Zoutkreeft>> ZOUTKREEFT = mob("vr_z6_mob_zoutkreeft", Zout.Zoutkreeft::new, 1.2f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zout.Zandgeestje>> ZANDGEESTJE = mob("vr_z6_mob_zandgeestje", Zout.Zandgeestje::new, 0.8f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zout.Zoutslang>> ZOUTSLANG = mob("vr_z6_mob_zoutslang", Zout.Zoutslang::new, 1.4f, 1.2f);
    // Het Ruïnedal + De Moederoase (+ the beacon guards everywhere)
    public static final DeferredHolder<EntityType<?>, EntityType<Ruine.Ruinewachter>> RUINEWACHTER = mob("vr_z6_mob_ruinewachter", Ruine.Ruinewachter::new, 1.0f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Ruine.MikaLampendover>> MIKA_LAMPENDOVER = mob("vr_z6_mob_mika_lampendover",
            Ruine.MikaLampendover::new, 0.7f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Ruine.MikaRingdrager>> MIKA_RINGDRAGER = mob("vr_z6_mob_mika_ringdrager",
            Ruine.MikaRingdrager::new, 0.8f, 1.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Ruine.GedoofdSterretje>> GEDOOFD_STERRETJE = mob("vr_z6_mob_gedoofd_sterretje",
            Ruine.GedoofdSterretje::new, 0.8f, 0.8f);
    // the bosses
    public static final DeferredHolder<EntityType<?>, EntityType<Wormelien>> WORMELIEN = baas("vr_z6_baas_wormelien", Wormelien::new, 3.0f, 3.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zoutkolos>> ZOUTKOLOS = baas("vr_z6_baas_zoutkolos", Zoutkolos::new, 2.6f, 4.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<Astrolabiumwachter>> ASTROLABIUMWACHTER = baas("vr_z6_baas_astrolabiumwachter",
            Astrolabiumwachter::new, 2.4f, 3.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<Glimmertje>> GLIMMERTJE = baas("vr_z6_baas_glimmertje", Glimmertje::new, 0.9f, 2.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Sterrenguh>> STERRENGUH = baas("vr_z6_baas_sterrenguh", Sterrenguh::new, 3.0f, 3.8f);
    // mechanic objects (zone 3's Z3Doel with our models)
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> ZOUTKORST = mob("vr_z6_baas_zoutkorst", Z3Doel::new, 1.4f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> ZINGKRISTAL = mob("vr_z6_baas_zingkristal", Z3Doel::new, 0.9f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> VUURSCHAAL = mob("vr_z6_baas_vuurschaal", Z3Doel::new, 1.0f, 1.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> TELESCOOP = mob("vr_z6_baas_telescoop", Z3Doel::new, 1.0f, 2.0f);

    /** Command name -> type (dev command, gametests), the plan's order. */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> SOORTEN = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> BAZEN = new LinkedHashMap<>();

    static {
        SOORTEN.put("zandvlooi", ZANDVLOOI);
        SOORTEN.put("fennekguh", FENNEKGUH);
        SOORTEN.put("springpiep", SPRINGPIEP);
        SOORTEN.put("duinkruiper", DUINKRUIPER);
        SOORTEN.put("dadelaapje", DADELAAPJE);
        SOORTEN.put("gierguh", GIERGUH);
        SOORTEN.put("spiegelslang", SPIEGELSLANG);
        SOORTEN.put("zoutkreeft", ZOUTKREEFT);
        SOORTEN.put("zandgeestje", ZANDGEESTJE);
        SOORTEN.put("ruinewachter", RUINEWACHTER);
        SOORTEN.put("mika_lampendover", MIKA_LAMPENDOVER);
        SOORTEN.put("mika_ringdrager", MIKA_RINGDRAGER);
        SOORTEN.put("gedoofd_sterretje", GEDOOFD_STERRETJE);
        SOORTEN.put("zoutslang", ZOUTSLANG);
        BAZEN.put("wormelien", WORMELIEN);
        BAZEN.put("zoutkolos", ZOUTKOLOS);
        BAZEN.put("astrolabiumwachter", ASTROLABIUMWACHTER);
        BAZEN.put("glimmertje", GLIMMERTJE);
        BAZEN.put("sterrenguh", STERRENGUH);
    }

    /** The gear items (armour with its own worn look, virtual-slot pieces plain), the 6 weapons, the crystals, Sterrenstof. */
    public static final Map<String, DeferredItem<Item>> ITEMS_PER_TYPE = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> WAPEN_ITEMS = new LinkedHashMap<>();
    /** Sterrenstof: star dust from Zandgeestjes and bosses (a material for the Naaien T6 star silk, u-ambacht2). */
    public static final DeferredItem<Item> STERRENSTOF = ITEMS.registerItem("vr_z6_sterrenstof", Item::new, () -> new Item.Properties().stacksTo(64));
    /** Het Sterrenkompas: the Verzonken Sterrenguh's gift (hq08). */
    public static final DeferredItem<Item> STERRENKOMPAS = ITEMS.registerItem("vr_z6_sterrenkompas", Item::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    /** The zone-6 tack: the Wormelien saddle blanket (every land mount; first kill of Koningin Wormelien). */
    public static final Uiterlijk.Variant TUIG_WORMELIEN = Uiterlijk.registreer(new Uiterlijk.Variant(Uiterlijk.As.TUIG, "wormelien_zadeldeken", null,
            0xFFF6A8C0, Uiterlijk.Bron.baas("guhs:vr_z6_baas_wormelien")));

    static {
        Z6Loot.STUKKEN.forEach((id, slot) -> ITEMS_PER_TYPE.put(id, slot.harnas()
                ? ITEMS.registerItem("vr_z6_mob_" + id, RijkItem::new, () -> KernRegistry.harnasEigenschappen(slot, "vr_z6_mob_" + id))
                : ITEMS.registerItem("vr_z6_mob_" + id, RijkItem::new, () -> new Item.Properties().stacksTo(1))));
        for (String w : Z6Loot.WAPENS) {
            WAPEN_ITEMS.put(w, ITEMS.registerItem("vr_z6_" + w, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)));
        }
        kristal(new Kristallen.Soort("rozenkwarts", Stat.KRITIEK, 3, 0xFFB8D0, "vr_z6_kristal_rozenkwarts"));
        kristal(new Kristallen.Soort("sterrenzout", Stat.BAZENSCHADE, 3, 0xE8E4F4, "vr_z6_kristal_sterrenzout"));
        if (!RijdierExtraRegistry.TUIG.containsKey(TUIG_WORMELIEN.id())) {
            RijdierExtraRegistry.TUIG.put(TUIG_WORMELIEN.id(), ITEMS.registerItem("vr_rijtuig_" + TUIG_WORMELIEN.id(),
                    p -> new RijdierExtraRegistry.TuigItem(p, TUIG_WORMELIEN.id()), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        }
        Z6BazenLoot.TROFEEEN.size();   // (initialises Z6BazenLoot: the named boss items register on ITEMS too)
    }

    private static void kristal(Kristallen.Soort s) {
        Kristallen.SOORTEN.put(s.id(), s);
        Kristallen.ITEM.put(s.id(), ITEMS.registerItem(s.itemId(), p -> new Kristallen.KristalItem(p, s.id()), () -> new Item.Properties().stacksTo(64)));
    }

    /** The pets: 4 that drop from creatures, Bobbeltje (the story), 3 from the bosses' first kill. */
    public static final List<Huisdier> HUISDIEREN = List.of(
            pet("fennekje", "", "fennekguh", 0.6f),
            pet("springpiepje", "", "springpiep", 0.6f),
            pet("sterretje", "", "gedoofd_sterretje", 0.6f),
            pet("dadelaapje", "", "dadelaapje", 0.6f),
            pet("bobbeltje", "", "bobbeltje", 0.4f),
            pet("wormelientje", "guhs:vr_z6_baas_wormelien", "wormelientje", 0.2f),
            pet("zoutklontje", "guhs:vr_z6_baas_zoutkolos", "zoutklontje", 0.6f),
            pet("sterrenguhtje", "guhs:vr_z6_baas_sterrenguh", "sterrenguhtje", 0.22f));

    private static Huisdier pet(String id, String baas, String model, float schaal) {
        boolean eigen = List.of("bobbeltje", "wormelientje", "zoutklontje", "sterrenguhtje").contains(model);   // own little models
        String tex = eigen ? model : model + "_blij";   // a creature's model in its cheered-up colours
        return Huisdieren.registreer(new Huisdier(id, baas.isEmpty() ? "guhs:vr_z6_huisdier_" + id : baas, "vadsrijk/zone6/wezens/" + model,
                "textures/vadsrijk/zone6/wezens/" + tex + ".png", "vadsrijk/zone6/wezens/huisdier", schaal, null));
    }

    private Z6WezensRegistry() {
    }

    private static AttributeSupplier attr(double snelheid, double knock) {
        return VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, snelheid).add(Attributes.FOLLOW_RANGE, 22)
                .add(Attributes.KNOCKBACK_RESISTANCE, knock).build();
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(ZANDVLOOI.get(), attr(0.3, 0.0));
            e.put(FENNEKGUH.get(), attr(0.32, 0.2));
            e.put(SPRINGPIEP.get(), attr(0.3, 0.1));
            e.put(DUINKRUIPER.get(), attr(0.24, 0.5));
            e.put(DADELAAPJE.get(), attr(0.3, 0.1));
            e.put(GIERGUH.get(), attr(0.28, 0.3));
            e.put(SPIEGELSLANG.get(), attr(0.27, 0.3));
            e.put(ZOUTKREEFT.get(), attr(0.22, 0.7));
            e.put(ZANDGEESTJE.get(), attr(0.22, 0.4));
            e.put(ZOUTSLANG.get(), attr(0.26, 0.9));
            e.put(RUINEWACHTER.get(), attr(0.2, 0.9));
            e.put(MIKA_LAMPENDOVER.get(), attr(0.28, 0.3));
            e.put(MIKA_RINGDRAGER.get(), attr(0.26, 0.4));
            e.put(GEDOOFD_STERRETJE.get(), attr(0.22, 0.2));
            for (var t : List.of(WORMELIEN, ZOUTKOLOS, ASTROLABIUMWACHTER, GLIMMERTJE, STERRENGUH)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                        .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            }
            for (var t : List.of(ZOUTKORST, ZINGKRISTAL, VUURSCHAAL, TELESCOOP)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
        });
        Z6Loot.basisTypes();
        Z6BazenLoot.typen();
        Z6Loot.tabellen();
        Z6Loot.haken();
        Spelers.EXTRA_STATS.add(Z6Sets::stats);
        Z6WezensSpawners.registreer();
        Z6Bazen.registreer();
        Z6BazenEffecten.register();
        Bultguh.registreer();
        haken();
        nl.juiced.guhs.world.TikWacht.post("Z6WezensRegistry:242", e -> {
            ServerLevel sl = e.getServer().getLevel(Vadsrijk.DIM);
            if (sl != null) {
                Z6Bouw.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener(Z6Bouw::luik);
        NeoForge.EVENT_BUS.addListener(Z6WezensCommando::register);
    }

    /** Damage hooks: the Zoutkreeft's crust, the set bonuses against bosses and the Sterrenwacht star. */
    private static void haken() {
        Schade.VOOR_KLAP.add((k, n) -> {
            LivingEntity doel = k.doel(), bron = k.bron();
            if (!(doel.level() instanceof ServerLevel sl) || k.soort() == Schade.Soort.WAAR) {
                return n;
            }
            if (doel instanceof Zout.Zoutkreeft kreeft) {
                double f = kreeft.schildFactor(bron);
                n *= f;
                if (sl.getGameTime() % 15 < 3) {
                    Getallen.tekst(sl, kreeft.getX(), kreeft.getY() + kreeft.getBbHeight() + 0.3, kreeft.getZ(),
                            Component.translatable(f > 1 ? "vadsrijk.z6wezens.zoutkreeft.zwak" : "vadsrijk.z6wezens.zoutkreeft.korst"), 0.7f);
                }
            }
            if (bron instanceof Player p && doel instanceof VadsrijkBaas) {
                n *= Z6Sets.bazenFactor(p);
            }
            if (bron instanceof ServerPlayer p && doel instanceof VadsrijkMob && Z6Sets.sterretje(p)) {
                n *= Z6Sets.STERRETJE;
                Getallen.tekst(sl, doel.getX(), doel.getY() + doel.getBbHeight() + 0.5, doel.getZ(), Component.translatable("vadsrijk.z6wezens.set.sterretje"), 0.9f);
            }
            return n;
        });
    }

    /** (tests) */
    static Slot slotVan(String id) {
        return Z6Loot.STUKKEN.get(id);
    }
}
