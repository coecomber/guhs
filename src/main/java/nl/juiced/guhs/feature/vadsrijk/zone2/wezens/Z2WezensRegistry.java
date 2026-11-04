package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import java.util.LinkedHashMap;
import java.util.Map;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Loot;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Burcht;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Critter;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Galm;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Kloof;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Meer;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;

/**
 * Het Vadsrijk zone 2 "De Splinterpieken", slice "wezens" (Z2_BRIEF): 14 monster kinds for lv 20-35 + the Spiegelburcht adds
 * (GeckoLib models from tools/features/vadsrijk_z2_wezens.py), elites (kern affixes, zone-1 look), the status Bevroren
 * ({@link Bevroren}, an adapter: kern has no such status), the spawners ({@link Z2WezensSpawners}, spots in {@link Z2WezensPlekken}),
 * the critters ({@link Z2Critters}), 15 base types + 2 sets + a table per creature ({@link Z2Loot}, {@link Z2Sets}) and the dev
 * command {@code /guhs vadsrijk z2wezens ...}. Ids: entities and items guhs:vr_z2_mob_*, loot tables z2wezens:*.
 */
public final class Z2WezensRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String naam, EntityType.EntityFactory<T> f, float b, float h) {
        String id = "vr_z2_mob_" + naam;
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.8f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    private static DeferredHolder<EntityType<?>, EntityType<Critter>> critter(String naam, boolean vliegt, float b, float h) {
        String id = "vr_z2_mob_" + naam;
        return ENTITIES.register(id, () -> EntityType.Builder.<Critter>of((t, l) -> new Critter(t, l, vliegt), MobCategory.AMBIENT).sized(b, h)
                .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    // Galmhellingen
    public static final DeferredHolder<EntityType<?>, EntityType<Galm.Splinterling>> SPLINTERLING = mob("splinterling", Galm.Splinterling::new, 1.2f, 2.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Galm.Galmvleermuis>> GALMVLEERMUIS = mob("galmvleermuis", Galm.Galmvleermuis::new, 0.9f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Galm.Sneeuwknabbelaar>> SNEEUWKNABBELAAR = mob("sneeuwknabbelaar", Galm.Sneeuwknabbelaar::new,
            0.9f, 0.95f);
    public static final DeferredHolder<EntityType<?>, EntityType<Galm.Kristalnaaldkruiper>> KRISTALNAALDKRUIPER = mob("kristalnaaldkruiper",
            Galm.Kristalnaaldkruiper::new, 1.1f, 0.8f);
    // Bevroren Knuffelmeer
    public static final DeferredHolder<EntityType<?>, EntityType<Meer.IJzelgeest>> IJZELGEEST = mob("ijzelgeest", Meer.IJzelgeest::new, 0.8f, 1.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Meer.Schotsbijter>> SCHOTSBIJTER = mob("schotsbijter", Meer.Schotsbijter::new, 1.4f, 1.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Meer.VersteendeWacht>> VERSTEENDE_WACHT = mob("versteende_wacht", Meer.VersteendeWacht::new,
            1.1f, 2.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Meer.Ijspuffer>> IJSPUFFER = mob("ijspuffer", Meer.Ijspuffer::new, 1.0f, 1.0f);
    // Splinterkloof
    public static final DeferredHolder<EntityType<?>, EntityType<Kloof.MikaMijnwerker>> MIKA_MIJNWERKER = mob("mika_mijnwerker", Kloof.MikaMijnwerker::new,
            0.7f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kloof.MikaBoorder>> MIKA_BOORDER = mob("mika_boorder", Kloof.MikaBoorder::new, 0.8f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kloof.Kloofgier>> KLOOFGIER = mob("kloofgier", Kloof.Kloofgier::new, 1.2f, 1.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Kloof.Kristalspin>> KRISTALSPIN = mob("kristalspin", Kloof.Kristalspin::new, 1.5f, 0.9f);
    // Spiegelburcht
    public static final DeferredHolder<EntityType<?>, EntityType<Burcht.Spiegelwacht>> SPIEGELWACHT = mob("spiegelwacht", Burcht.Spiegelwacht::new, 1.0f, 2.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Burcht.Scherfgeest>> SCHERFGEEST = mob("scherfgeest", Burcht.Scherfgeest::new, 0.9f, 1.2f);
    // critters
    public static final DeferredHolder<EntityType<?>, EntityType<Critter>> SNEEUWHAAS = critter("sneeuwhaas", false, 0.5f, 0.55f);
    public static final DeferredHolder<EntityType<?>, EntityType<Critter>> IJSVOGEL = critter("ijsvogel", true, 0.45f, 0.45f);
    public static final DeferredHolder<EntityType<?>, EntityType<Critter>> KRISTALMOT = critter("kristalmot", true, 0.4f, 0.35f);

    /** Command name -> monster type (the dev command and the gametests), in area order. */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> SOORTEN = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<Critter>>> CRITTERS = new LinkedHashMap<>();

    static {
        SOORTEN.put("splinterling", SPLINTERLING);
        SOORTEN.put("galmvleermuis", GALMVLEERMUIS);
        SOORTEN.put("sneeuwknabbelaar", SNEEUWKNABBELAAR);
        SOORTEN.put("kristalnaaldkruiper", KRISTALNAALDKRUIPER);
        SOORTEN.put("ijzelgeest", IJZELGEEST);
        SOORTEN.put("schotsbijter", SCHOTSBIJTER);
        SOORTEN.put("versteende_wacht", VERSTEENDE_WACHT);
        SOORTEN.put("ijspuffer", IJSPUFFER);
        SOORTEN.put("mika_mijnwerker", MIKA_MIJNWERKER);
        SOORTEN.put("mika_boorder", MIKA_BOORDER);
        SOORTEN.put("kloofgier", KLOOFGIER);
        SOORTEN.put("kristalspin", KRISTALSPIN);
        SOORTEN.put("spiegelwacht", SPIEGELWACHT);
        SOORTEN.put("scherfgeest", SCHERFGEEST);
        CRITTERS.put("sneeuwhaas", SNEEUWHAAS);
        CRITTERS.put("ijsvogel", IJSVOGEL);
        CRITTERS.put("kristalmot", KRISTALMOT);
    }

    /** The gear items (one per base type): armour with its own worn look, jewellery as a plain RijkItem. */
    public static final Map<String, DeferredItem<Item>> ITEMS_PER_TYPE = new LinkedHashMap<>();

    static {
        Z2Loot.STUKKEN.forEach((id, slot) -> ITEMS_PER_TYPE.put(id, item("vr_z2_mob_" + id, slot)));
    }

    private static DeferredItem<Item> item(String id, Slot slot) {
        if (slot.sieraad()) {
            return ITEMS.registerItem(id, RijkItem::new, () -> new Item.Properties().stacksTo(1));
        }
        return ITEMS.registerItem(id, RijkItem::new, () -> KernRegistry.harnasEigenschappen(slot, id));
    }

    private Z2WezensRegistry() {
    }

    private static AttributeSupplier attr(double snelheid, double volg, double knock) {
        return VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, snelheid).add(Attributes.FOLLOW_RANGE, volg)
                .add(Attributes.KNOCKBACK_RESISTANCE, knock).build();
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(SPLINTERLING.get(), attr(0.2, 16, 0.9));
            e.put(GALMVLEERMUIS.get(), attr(0.3, 20, 0.2));
            e.put(SNEEUWKNABBELAAR.get(), attr(0.33, 20, 0.3));
            e.put(KRISTALNAALDKRUIPER.get(), attr(0.3, 14, 0.5));
            e.put(IJZELGEEST.get(), attr(0.25, 18, 0.4));
            e.put(SCHOTSBIJTER.get(), attr(0.26, 16, 0.8));
            e.put(VERSTEENDE_WACHT.get(), attr(0.18, 16, 1.0));
            e.put(IJSPUFFER.get(), attr(0.2, 14, 0.4));
            e.put(MIKA_MIJNWERKER.get(), attr(0.27, 18, 0.3));
            e.put(MIKA_BOORDER.get(), attr(0.25, 20, 0.3));
            e.put(KLOOFGIER.get(), attr(0.3, 26, 0.6));
            e.put(KRISTALSPIN.get(), attr(0.3, 16, 0.5));
            e.put(SPIEGELWACHT.get(), attr(0.22, 18, 0.9));
            e.put(SCHERFGEEST.get(), attr(0.28, 20, 0.3));
            e.put(SNEEUWHAAS.get(), Critter.attributen(0.3).build());
            e.put(IJSVOGEL.get(), Critter.attributen(0.25).build());
            e.put(KRISTALMOT.get(), Critter.attributen(0.2).build());
        });
        Z2Loot.basisTypes();
        Z2Loot.tabellen();
        Spelers.EXTRA_STATS.add(Z2Sets::stats);
        Spelers.EXTRA_STATS.add(Bevroren::stats);
        Z2WezensSpawners.registreer();
        haken();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Bevroren.tick();
            Verslagen.tick();
            ServerLevel sl = e.getServer().getLevel(Vadsrijk.DIM);
            if (sl != null) {
                Z2Critters.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> Verslagen.binnen(e));
        NeoForge.EVENT_BUS.addListener(Z2WezensCommando::register);
        Haken.FLAUW.add(Bevroren::wis);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    /** The damage hooks: hidden = not hittable, Bevroren, the Spiegelwacht's mirror, the Splinterling's shards, the sets. */
    private static void haken() {
        Schade.VOOR_KLAP.add((k, n) -> {
            LivingEntity doel = k.doel(), bron = k.bron();
            if (!(doel.level() instanceof ServerLevel sl)) {
                return n;
            }
            if (doel instanceof Z2Wezen w && w.verborgen()) {
                return 0;
            }
            if (k.soort() == Schade.Soort.WAAR) {
                return n;
            }
            if (doel instanceof Burcht.Spiegelwacht s && s.spiegelt(bron)) {
                if (k.soort() == Schade.Soort.MELEE) {
                    n *= 0.5;
                } else if (bron != null) {
                    s.spiegelEffect(sl);
                    Getallen.tekst(sl, s.getX(), s.getY() + s.getBbHeight() + 0.3, s.getZ(), Component.translatable("vadsrijk.z2wezens.gespiegeld"), 0.8f);
                    Schade.doe(new Schade.Klap(s, bron, n * 0.4, s.element(), Schade.Soort.WAAR, false, "gespiegeld"));
                    return 0;
                }
            }
            if (bron instanceof Player p && doel instanceof VadsrijkMob m && Z2Sets.stukken(p, Z2Sets.MIKA) >= 4) {
                if (m.ontvadst()) {
                    n *= 1.15;
                }
                if (m.vads() >= m.maxVads() - 0.01) {
                    n *= 1.25;
                }
            }
            return Bevroren.klap(k, n);
        });
        Schade.NA_KLAP.add(u -> {
            LivingEntity doel = u.klap().doel(), bron = u.klap().bron();
            if (u.schade() <= 0 || bron == null) {
                return;
            }
            if (doel instanceof Galm.Splinterling s && bron instanceof Player && u.klap().soort() != Schade.Soort.WAAR) {
                s.splinter(bron);
            }
            if (bron instanceof Player p && doel instanceof VadsrijkMob m && u.klap().soort() != Schade.Soort.WAAR
                    && Z2Sets.stukken(p, Z2Sets.KRISTAL) >= 4 && p.getRandom().nextDouble() < 0.2 && m.level() instanceof ServerLevel sl) {
                sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                        net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK.defaultBlockState()), m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 20,
                        0.5, 0.4, 0.5, 0.2);
                for (VadsrijkMob ander : sl.getEntitiesOfClass(VadsrijkMob.class, m.getBoundingBox().inflate(3), x -> x != m && !x.geplofd())) {
                    Schade.doe(new Schade.Klap(p, ander, u.schade() * 0.25, nl.juiced.guhs.feature.vadsrijk.kern.Element.GEEN, Schade.Soort.WAAR, false,
                            "splinterslag"));
                }
            }
        });
    }
}
