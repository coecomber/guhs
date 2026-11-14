package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.ArrayList;
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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Donderbuik;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Gorgonzola;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5Bazen;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Zilverstaart;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Loot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Sets;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Buik;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Nest;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Proef;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Rand;

/**
 * Het Vadsrijk zone 5 "Hoogvads", the combat slice (T-Z5WEZENS): 9 creature kinds for lv 56-60 in four contained places
 * ({@link Z5WezensPlekken}), 3 named area leaders, 3 bosses (the spy finale Kootje Zilverstaart, the world boss Donderbuik, the
 * dungeon boss Opzichter Gorgonzola; {@link Z5Bazen}), the Wolkwachtset + jewellery + 6 class weapons ({@link Z5Loot}), 6 named boss
 * items + 3 trophies ({@link Z5BazenLoot}), 3 pets, the own rooms + hatches + fall net ({@link Z5Bouw}) and the dev command
 * {@code /guhs vadsrijk z5wezens ...}. Ids guhs:vr_z5_mob_* / vr_z5_baas_*, lang vadsrijk.z5wezens.*.
 */
public final class Z5WezensRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String id, EntityType.EntityFactory<T> f, float b, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.8f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    // De Proefarena
    public static final DeferredHolder<EntityType<?>, EntityType<Proef.BezetenOefenpop>> BEZETEN_OEFENPOP = mob("vr_z5_mob_bezeten_oefenpop",
            Proef.BezetenOefenpop::new, 0.8f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Proef.Sparringridder>> SPARRINGRIDDER = mob("vr_z5_mob_sparringridder",
            Proef.Sparringridder::new, 0.9f, 1.4f);
    // De Buikgangen + Het Schuilnest
    public static final DeferredHolder<EntityType<?>, EntityType<Buik.MikaSaboteur>> MIKA_SABOTEUR = mob("vr_z5_mob_mika_saboteur", Buik.MikaSaboteur::new, 0.7f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Buik.MikaLontlegger>> MIKA_LONTLEGGER = mob("vr_z5_mob_mika_lontlegger",
            Buik.MikaLontlegger::new, 0.7f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Buik.GrauwePluismijt>> GRAUWE_PLUISMIJT = mob("vr_z5_mob_grauwe_pluismijt",
            Buik.GrauwePluismijt::new, 0.6f, 0.5f);
    // De Wolkrand
    public static final DeferredHolder<EntityType<?>, EntityType<Rand.GrauweWolkduivel>> GRAUWE_WOLKDUIVEL = mob("vr_z5_mob_grauwe_wolkduivel",
            Rand.GrauweWolkduivel::new, 0.9f, 1.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Rand.Mistslier>> MISTSLIER = mob("vr_z5_mob_mistslier", Rand.Mistslier::new, 0.8f, 1.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Rand.MikaHarpoenier>> MIKA_HARPOENIER = mob("vr_z5_mob_mika_harpoenier",
            Rand.MikaHarpoenier::new, 0.7f, 1.7f);
    // Het Schuilnest
    public static final DeferredHolder<EntityType<?>, EntityType<Nest.VermomdeMika>> VERMOMDE_MIKA = mob("vr_z5_mob_vermomde_mika", Nest.VermomdeMika::new, 0.9f, 1.8f);
    // the bosses
    public static final DeferredHolder<EntityType<?>, EntityType<Zilverstaart>> ZILVERSTAART = mob("vr_z5_baas_zilverstaart", Zilverstaart::new, 0.9f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Gorgonzola>> GORGONZOLA = mob("vr_z5_baas_gorgonzola", Gorgonzola::new, 1.5f, 2.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Donderbuik>> DONDERBUIK = mob("vr_z5_baas_donderbuik", Donderbuik::new, 4.0f, 3.6f);
    // mechanic objects (zone 3's Z3Doel with our models)
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> KLEP = mob("vr_z5_baas_klep", Z3Doel::new, 1.2f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> LOKPOP = mob("vr_z5_baas_lokpop", Z3Doel::new, 0.9f, 1.8f);

    /** Command name -> type (dev command, gametests), place order. */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> SOORTEN = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> BAZEN = new LinkedHashMap<>();

    static {
        SOORTEN.put("bezeten_oefenpop", BEZETEN_OEFENPOP);
        SOORTEN.put("sparringridder", SPARRINGRIDDER);
        SOORTEN.put("mika_saboteur", MIKA_SABOTEUR);
        SOORTEN.put("mika_lontlegger", MIKA_LONTLEGGER);
        SOORTEN.put("grauwe_pluismijt", GRAUWE_PLUISMIJT);
        SOORTEN.put("grauwe_wolkduivel", GRAUWE_WOLKDUIVEL);
        SOORTEN.put("mistslier", MISTSLIER);
        SOORTEN.put("mika_harpoenier", MIKA_HARPOENIER);
        SOORTEN.put("vermomde_mika", VERMOMDE_MIKA);
        BAZEN.put("zilverstaart", ZILVERSTAART);
        BAZEN.put("gorgonzola", GORGONZOLA);
        BAZEN.put("donderbuik", DONDERBUIK);
    }

    /** The gear items (armour with its own worn look, virtual-slot pieces plain) and the 6 weapons. */
    public static final Map<String, DeferredItem<Item>> ITEMS_PER_TYPE = new LinkedHashMap<>();

    static {
        Z5Loot.STUKKEN.forEach((id, slot) -> ITEMS_PER_TYPE.put(id, slot.harnas()
                ? ITEMS.registerItem("vr_z5_mob_" + id, RijkItem::new, () -> KernRegistry.harnasEigenschappen(slot, "vr_z5_mob_" + id))
                : ITEMS.registerItem("vr_z5_mob_" + id, RijkItem::new, () -> new Item.Properties().stacksTo(1))));
        for (String w : Z5Loot.WAPENS) {
            ITEMS_PER_TYPE.put(w, ITEMS.registerItem("vr_z5_mob_" + w, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)));
        }
        Z5BazenLoot.TROFEEEN.size();   // (initialises Z5BazenLoot: the named boss items register on ITEMS too)
    }

    /** The pets (G-PETS: the boss's first kill unlocks it). */
    public static final Huisdier PET_DONDERBUIKJE = Huisdieren.registreer(new Huisdier("donderbuikje", "guhs:vr_z5_baas_donderbuik",
            "vadsrijk/zone5/wezens/donderbuik", "textures/vadsrijk/zone5/wezens/donderbuik_roze.png", "vadsrijk/zone5/wezens/huisdier", 0.14f, null));
    public static final Huisdier PET_WARMPJE = Huisdieren.registreer(new Huisdier("warmpje", "guhs:vr_z5_baas_gorgonzola",
            "vadsrijk/zone5/wezens/warmpje", "textures/vadsrijk/zone5/wezens/warmpje.png", "vadsrijk/zone5/wezens/huisdier", 0.5f,
            "textures/vadsrijk/zone5/wezens/warmpje_gloed.png"));
    public static final Huisdier PET_LOKPOPJE = Huisdieren.registreer(new Huisdier("lokpopje", "guhs:vr_z5_baas_zilverstaart",
            "vadsrijk/zone5/wezens/lokpop", "textures/vadsrijk/zone5/wezens/lokpop.png", "vadsrijk/zone5/wezens/huisdier", 0.3f, null));

    private Z5WezensRegistry() {
    }

    private static AttributeSupplier attr(double snelheid, double volg, double knock) {
        return VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, snelheid).add(Attributes.FOLLOW_RANGE, volg)
                .add(Attributes.KNOCKBACK_RESISTANCE, knock).build();
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(BEZETEN_OEFENPOP.get(), attr(0.26, 16, 0.6));
            e.put(SPARRINGRIDDER.get(), attr(0.25, 16, 1.0));
            e.put(MIKA_SABOTEUR.get(), attr(0.28, 18, 0.3));
            e.put(MIKA_LONTLEGGER.get(), attr(0.27, 20, 0.3));
            e.put(GRAUWE_PLUISMIJT.get(), attr(0.33, 16, 0.1));
            e.put(GRAUWE_WOLKDUIVEL.get(), attr(0.3, 18, 0.4));
            e.put(MISTSLIER.get(), attr(0.24, 18, 0.4));
            e.put(MIKA_HARPOENIER.get(), attr(0.27, 20, 0.3));
            e.put(VERMOMDE_MIKA.get(), attr(0.3, 18, 0.3));
            for (var t : List.of(ZILVERSTAART, GORGONZOLA, DONDERBUIK)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                        .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            }
            for (var t : List.of(KLEP, LOKPOP)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
        });
        Z5Loot.basisTypes();
        Z5BazenLoot.typen();
        Z5Loot.tabellen();
        Spelers.EXTRA_STATS.add(Z5Sets::stats);
        gebieden();
        Z5WezensSpawners.registreer();
        Z5Bazen.registreer();
        haken();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerAboutToStartEvent e) -> gebieden());
        nl.juiced.guhs.world.TikWacht.post("Z5WezensRegistry:183", e -> {
            ServerLevel sl = e.getServer().getLevel(Vadsrijk.DIM);
            if (sl != null) {
                Z5Bouw.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener(Z5Bouw::luik);
        NeoForge.EVENT_BUS.addListener(Z5WezensCommando::register);
    }

    /** Our combat areas in FRONT of the town boxes (t-z5verhaal's Hoogvads box is a town: no spawning there). Run again late. */
    public static void gebieden() {
        List<Gebieden.Gebied> l = new ArrayList<>(Z5WezensPlekken.GEBIEDEN);
        java.util.Collections.reverse(l);
        for (Gebieden.Gebied g : l) {
            Gebieden.voegVooraan(g);
        }
    }

    /** The Wolkwachtset hooks: 4 pieces = Schade tegen bazen +8 %, 6 pieces = the cloud cushion. */
    private static void haken() {
        Schade.VOOR_KLAP.add((k, n) -> {
            if (k.bron() instanceof Player p && k.doel() instanceof VadsrijkBaas && Z5Sets.stukken(p) >= 4) {
                return n * (1 + Z5Sets.BAZEN_BONUS);
            }
            return n;
        });
        Schade.NA_KLAP.add(u -> {
            if (u.schade() > 0 && u.klap().doel() instanceof ServerPlayer p && Spelers.vads(p) < Spelers.maxVads(p) * 0.3 && Z5Sets.stukken(p) >= 6
                    && !Statussen.heeft(p, Status.KNUFFELSCHILD) && Z5Sets.kussen(p)) {
                Statussen.geef(p, Status.KNUFFELSCHILD, 120, Spelers.maxVads(p) * 0.2, p);
                Getallen.tekst((ServerLevel) p.level(), p.getX(), p.getY() + 2.2, p.getZ(), Component.translatable("vadsrijk.z5wezens.wolkkussen"), 0.8f);
            }
        });
    }

    /** (tests) */
    static Slot slotVan(String id) {
        return Z5Loot.STUKKEN.get(id);
    }
}
