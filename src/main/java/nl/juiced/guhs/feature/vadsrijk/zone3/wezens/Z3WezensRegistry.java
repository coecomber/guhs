package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.LinkedHashMap;
import java.util.Map;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Loot;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Sets;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Duister;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Kruin;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Schors;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Takken;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Velden;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Critter;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Wezen;

/**
 * Het Vadsrijk zone 3 "De Schemerwortel", slice "wezens" (Z3_BRIEF): 14 monster kinds for lv 30-50 (+ the Schimmelbolletje add),
 * GeckoLib models from tools/features/vadsrijk_z3_wezens.py, elites (kern affixes) + 5 named area leaders, the 8 contract elites
 * ({@link Z3Contracten}), the soft lantern rule ({@link Lantaarnlicht}), the layered spawners ({@link Z3WezensSpawners}, spots in
 * {@link Z3WezensPlekken}), 4 critters ({@link Z3Critters}), 15 base types + 3 sets + 8 trophies ({@link Z3Loot}, {@link Z3Sets})
 * and the dev command {@code /guhs vadsrijk z3wezens ...}. Ids: entities and items guhs:vr_z3_mob_*, loot tables z3wezens:*.
 */
public final class Z3WezensRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String naam, EntityType.EntityFactory<T> f, float b, float h) {
        String id = "vr_z3_mob_" + naam;
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.8f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    private static DeferredHolder<EntityType<?>, EntityType<Z3Critter>> critter(String naam, boolean vliegt, boolean bij, float b, float h) {
        String id = "vr_z3_mob_" + naam;
        return ENTITIES.register(id, () -> EntityType.Builder.<Z3Critter>of((t, l) -> new Z3Critter(t, l, vliegt, bij), MobCategory.AMBIENT).sized(b, h)
                .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    // De Kruinzee
    public static final DeferredHolder<EntityType<?>, EntityType<Kruin.Bladrover>> BLADROVER = mob("bladrover", Kruin.Bladrover::new, 0.7f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kruin.GrauweRups>> GRAUWE_RUPS = mob("grauwe_rups", Kruin.GrauweRups::new, 1.3f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kruin.Bijtbloesem>> BIJTBLOESEM = mob("bijtbloesem", Kruin.Bijtbloesem::new, 1.0f, 1.7f);
    // De Takkenbanen
    public static final DeferredHolder<EntityType<?>, EntityType<Takken.Kaasschutter>> KAASSCHUTTER = mob("kaasschutter", Takken.Kaasschutter::new, 0.7f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Takken.Schorsmarter>> SCHORSMARTER = mob("schorsmarter", Takken.Schorsmarter::new, 0.8f, 0.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Takken.GrauweWesp>> GRAUWE_WESP = mob("grauwe_wesp", Takken.GrauweWesp::new, 1.2f, 1.0f);
    // De Schorsstad
    public static final DeferredHolder<EntityType<?>, EntityType<Schors.Schorskruiper>> SCHORSKRUIPER = mob("schorskruiper", Schors.Schorskruiper::new, 1.5f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Schors.Lantaarnloze>> LANTAARNLOZE = mob("lantaarnloze", Schors.Lantaarnloze::new, 0.8f, 1.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Schors.Harsgolem>> HARSGOLEM = mob("harsgolem", Schors.Harsgolem::new, 1.4f, 2.7f);
    // De Wortelvelden
    public static final DeferredHolder<EntityType<?>, EntityType<Velden.Rotzwijn>> ROTZWIJN = mob("rotzwijn", Velden.Rotzwijn::new, 1.5f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Velden.Wortelslang>> WORTELSLANG = mob("wortelslang", Velden.Wortelslang::new, 1.2f, 1.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Velden.MikaSchimmelboer>> MIKA_SCHIMMELBOER = mob("mika_schimmelboer",
            Velden.MikaSchimmelboer::new, 0.8f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Velden.Schimmelbolletje>> SCHIMMELBOLLETJE = mob("schimmelbolletje",
            Velden.Schimmelbolletje::new, 0.6f, 0.6f);
    // Het Wortelduister
    public static final DeferredHolder<EntityType<?>, EntityType<Duister.Wortelschim>> WORTELSCHIM = mob("wortelschim", Duister.Wortelschim::new, 0.8f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Duister.DoveZiener>> DOVE_ZIENER = mob("dove_ziener", Duister.DoveZiener::new, 1.6f, 3.1f);
    // critters
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Critter>> HONINGBIJ = critter("honingbij", true, true, 0.5f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Critter>> SPORENMOT = critter("sporenmot", true, false, 0.45f, 0.35f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Critter>> LANTAARNKEVER = critter("lantaarnkever", true, false, 0.35f, 0.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Critter>> WORTELMUIS = critter("wortelmuis", false, false, 0.4f, 0.35f);

    /** Command name -> monster type (dev command, gametests), in area order (the 14 of ZONE3_PLAN §6). */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> SOORTEN = new LinkedHashMap<>();
    /** Adds (no spawner of their own). */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> ADDS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<Z3Critter>>> CRITTERS = new LinkedHashMap<>();

    static {
        SOORTEN.put("bladrover", BLADROVER);
        SOORTEN.put("grauwe_rups", GRAUWE_RUPS);
        SOORTEN.put("bijtbloesem", BIJTBLOESEM);
        SOORTEN.put("kaasschutter", KAASSCHUTTER);
        SOORTEN.put("schorsmarter", SCHORSMARTER);
        SOORTEN.put("grauwe_wesp", GRAUWE_WESP);
        SOORTEN.put("schorskruiper", SCHORSKRUIPER);
        SOORTEN.put("lantaarnloze", LANTAARNLOZE);
        SOORTEN.put("harsgolem", HARSGOLEM);
        SOORTEN.put("rotzwijn", ROTZWIJN);
        SOORTEN.put("wortelslang", WORTELSLANG);
        SOORTEN.put("mika_schimmelboer", MIKA_SCHIMMELBOER);
        SOORTEN.put("wortelschim", WORTELSCHIM);
        SOORTEN.put("dove_ziener", DOVE_ZIENER);
        ADDS.put("schimmelbolletje", SCHIMMELBOLLETJE);
        CRITTERS.put("honingbij", HONINGBIJ);
        CRITTERS.put("sporenmot", SPORENMOT);
        CRITTERS.put("lantaarnkever", LANTAARNKEVER);
        CRITTERS.put("wortelmuis", WORTELMUIS);
    }

    /** The gear items (one per base type): armour with its own worn look, jewellery as a plain RijkItem. */
    public static final Map<String, DeferredItem<Item>> ITEMS_PER_TYPE = new LinkedHashMap<>();
    /** The contract trophies (vr_z3_mob_trofee_&lt;id&gt;). */
    public static final Map<String, DeferredItem<Item>> TROFEEEN = new LinkedHashMap<>();

    static {
        Z3Loot.STUKKEN.forEach((id, slot) -> ITEMS_PER_TYPE.put(id, item("vr_z3_mob_" + id, slot)));
        for (String t : Z3Loot.TROFEEEN) {
            TROFEEEN.put(t, ITEMS.registerItem("vr_z3_mob_trofee_" + t, Item::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        }
    }

    private static DeferredItem<Item> item(String id, Slot slot) {
        if (slot.sieraad()) {
            return ITEMS.registerItem(id, RijkItem::new, () -> new Item.Properties().stacksTo(1));
        }
        return ITEMS.registerItem(id, RijkItem::new, () -> KernRegistry.harnasEigenschappen(slot, id));
    }

    private Z3WezensRegistry() {
    }

    private static AttributeSupplier attr(double snelheid, double volg, double knock) {
        return VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, snelheid).add(Attributes.FOLLOW_RANGE, volg)
                .add(Attributes.KNOCKBACK_RESISTANCE, knock).build();
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(BLADROVER.get(), attr(0.28, 18, 0.3));
            e.put(GRAUWE_RUPS.get(), attr(0.18, 16, 0.7));
            e.put(BIJTBLOESEM.get(), attr(0.0, 14, 1.0));
            e.put(KAASSCHUTTER.get(), attr(0.26, 20, 0.3));
            e.put(SCHORSMARTER.get(), attr(0.36, 18, 0.2));
            e.put(GRAUWE_WESP.get(), attr(0.3, 18, 0.5));
            e.put(SCHORSKRUIPER.get(), attr(0.3, 16, 0.5));
            e.put(LANTAARNLOZE.get(), attr(0.24, 18, 0.4));
            e.put(HARSGOLEM.get(), attr(0.18, 16, 1.0));
            e.put(ROTZWIJN.get(), attr(0.27, 20, 0.8));
            e.put(WORTELSLANG.get(), attr(0.26, 18, 0.6));
            e.put(MIKA_SCHIMMELBOER.get(), attr(0.25, 18, 0.3));
            e.put(SCHIMMELBOLLETJE.get(), attr(0.28, 14, 0.1));
            e.put(WORTELSCHIM.get(), attr(0.29, 18, 0.3));
            e.put(DOVE_ZIENER.get(), attr(0.2, 18, 1.0));
            e.put(HONINGBIJ.get(), Z3Critter.attributen(0.25).build());
            e.put(SPORENMOT.get(), Z3Critter.attributen(0.2).build());
            e.put(LANTAARNKEVER.get(), Z3Critter.attributen(0.2).build());
            e.put(WORTELMUIS.get(), Z3Critter.attributen(0.32).build());
        });
        Z3Loot.basisTypes();
        Z3Loot.tabellen();
        Spelers.EXTRA_STATS.add(Z3Sets::stats);
        Lantaarnlicht.DRAGERS.add(p -> Z3Sets.stukken(p, Z3Sets.WORTEL) >= 4);
        Z3WezensSpawners.registreer();
        haken();
        nl.juiced.guhs.world.TikWacht.post("Z3WezensRegistry:184", e -> {
            ServerLevel sl = e.getServer().getLevel(Vadsrijk.DIM);
            if (sl != null) {
                Z3Critters.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener(Z3WezensCommando::register);
    }

    /**
     * The damage hooks: contract elites only for their players, the soft lantern rule (-50 % outside the light), the dizzy
     * Grauwe Rups x1.5, the Harsgolem's weak spot x1.5 from behind, the sneak attack on an unaware Dove Ziener x2, the sets.
     */
    private static void haken() {
        Schade.VOOR_KLAP.add((k, n) -> {
            LivingEntity doel = k.doel(), bron = k.bron();
            if (!(doel.level() instanceof ServerLevel sl)) {
                return n;
            }
            // contract elites: nobody else hits them, they hit nobody else
            if (doel instanceof Z3Wezen w && bron instanceof Player p && !w.voor(p)) {
                return 0;
            }
            if (bron instanceof Z3Wezen w && doel instanceof Player p && !w.voor(p)) {
                return 0;
            }
            if (k.soort() == Schade.Soort.WAAR || !(doel instanceof Z3Wezen w)) {
                return n;
            }
            if (w.donker()) {
                n *= Lantaarnlicht.DONKER_FACTOR;
                if (bron instanceof ServerPlayer p && sl.getGameTime() % 40 < 3) {
                    Getallen.tekst(sl, w.getX(), w.getY() + w.getBbHeight() + 0.3, w.getZ(), Component.translatable("vadsrijk.z3wezens.te_donker"), 0.7f);
                }
            }
            if (w instanceof Kruin.GrauweRups r && r.duizelig()) {
                n *= 1.5;
            }
            if (w instanceof Schors.Harsgolem h && h.zwakkePlek(bron)) {
                n *= 1.5;
                Getallen.tekst(sl, h.getX(), h.getY() + h.getBbHeight() + 0.3, h.getZ(), Component.translatable("vadsrijk.z3wezens.zwakke_plek"), 0.8f);
            }
            if (w instanceof Duister.DoveZiener z && bron instanceof Player && z.onwetend()) {
                n *= 2;
                Getallen.tekst(sl, z.getX(), z.getY() + z.getBbHeight() + 0.3, z.getZ(), Component.translatable("vadsrijk.z3wezens.beslopen"), 0.9f);
            }
            if (bron instanceof Player p && Z3Sets.stukken(p, Z3Sets.WORTEL) >= 4 && w.wezenAard() == Z3Wezen.WezenAard.ROT) {
                n *= 1.10;
            }
            return n;
        });
        Schade.NA_KLAP.add(u -> {
            LivingEntity doel = u.klap().doel(), bron = u.klap().bron();
            if (u.schade() <= 0) {
                return;
            }
            // Bloesemset 4: crits leave the creature Gesporen
            if (bron instanceof Player p && u.kritiek() && doel instanceof VadsrijkMob m && Z3Sets.stukken(p, Z3Sets.BLOESEM) >= 4) {
                Statussen.geef(m, Status.GESPOREN, 60, Math.max(1, Spelers.level(p) * 0.3), p);
            }
            // Schorsset 4: below 50 % a resin shield, once per 30 s
            if (doel instanceof ServerPlayer p && Z3Sets.stukken(p, Z3Sets.SCHORS) >= 4 && Spelers.vads(p) < Spelers.maxVads(p) * 0.5
                    && !Statussen.heeft(p, Status.KNUFFELSCHILD) && Z3Sets.harsschild(p)) {
                Statussen.geef(p, Status.KNUFFELSCHILD, 120, Spelers.maxVads(p) * 0.15, p);
                Getallen.tekst((ServerLevel) p.level(), p.getX(), p.getY() + 2.2, p.getZ(), Component.translatable("vadsrijk.z3wezens.harsschild"), 0.8f);
            }
        });
    }
}
