package nl.juiced.guhs.feature.vadsrijk.wezens;

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
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.Vahoegnir;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.SetBonussen;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.WezensLoot;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Anderen;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Effecten;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Klodders;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Zwammen;

/**
 * Het Vadsrijk, slice "wezens" (CONTRACT_VADSRIJK 1, 5, 8): the zone-1 creatures on kern.mob.VadsrijkMob (GeckoLib models,
 * textures and animations from tools/features/vadsrijk_wezens.py), elites, the Kampklodder, Vahoegnir's flyover
 * ({@link DraakVlucht}), the armour base types + sets + loot tables ({@link WezensLoot}, {@link SetBonussen}), the spawners
 * ({@link WezensSpawners}, spots in {@link WezensPlekken}) and the dev command {@code /guhs vadsrijk wezens ...}.
 */
public final class WezensRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String naam, EntityType.EntityFactory<T> f, float b, float h) {
        String id = "vr_mob_" + naam;
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.75f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Klodder>> KLODDER = mob("klodder", Klodders.Klodder::new, 0.8f, 0.75f);
    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Klodder>> KLODDERTJE = mob("kloddertje", Klodders.Klodder::new, 0.45f, 0.42f);
    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Bubbelklodder>> BUBBELKLODDER = mob("bubbelklodder", Klodders.Bubbelklodder::new, 0.8f, 0.85f);
    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Stuiterklodder>> STUITERKLODDER = mob("stuiterklodder", Klodders.Stuiterklodder::new, 0.8f, 0.95f);
    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Moederklodder>> MOEDERKLODDER = mob("moederklodder", Klodders.Moederklodder::new, 1.4f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Druipslijm>> DRUIPSLIJM = mob("druipslijm", Klodders.Druipslijm::new, 0.8f, 0.75f);
    public static final DeferredHolder<EntityType<?>, EntityType<Klodders.Kristalklodder>> KRISTALKLODDER = mob("kristalklodder", Klodders.Kristalklodder::new, 0.85f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zwammen.Hoedjesbol>> HOEDJESBOL = mob("hoedjesbol", Zwammen.Hoedjesbol::new, 0.75f, 0.85f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zwammen.Sporenpuffer>> SPORENPUFFER = mob("sporenpuffer", Zwammen.Sporenpuffer::new, 0.8f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zwammen.Zwamwachter>> ZWAMWACHTER = mob("zwamwachter", Zwammen.Zwamwachter::new, 1.0f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zwammen.Glimzwam>> GLIMZWAM = mob("glimzwam", Zwammen.Glimzwam::new, 0.6f, 0.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zwammen.Tempelridder>> TEMPELRIDDER = mob("tempelridder", Zwammen.Tempelridder::new, 0.9f, 1.95f);
    public static final DeferredHolder<EntityType<?>, EntityType<Zwammen.Sporenpriester>> SPORENPRIESTER = mob("sporenpriester", Zwammen.Sporenpriester::new, 0.8f, 1.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Anderen.Mosgeest>> MOSGEEST = mob("mosgeest", Anderen.Mosgeest::new, 0.75f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Anderen.Knaag>> KNAAG = mob("knaag", Anderen.Knaag::new, 0.7f, 0.6f);

    public static final DeferredHolder<EntityType<?>, EntityType<Kampklodder>> KAMPKLODDER = ENTITIES.register("vr_mob_kampklodder",
            () -> EntityType.Builder.of(Kampklodder::new, MobCategory.CREATURE).sized(2.4f, 2.2f).eyeHeight(1.6f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_mob_kampklodder"))));
    public static final DeferredHolder<EntityType<?>, EntityType<Vahoegnir>> DRAAK = ENTITIES.register("vr_mob_draak",
            () -> EntityType.Builder.<Vahoegnir>of(Vahoegnir::new, MobCategory.MISC).sized(4f, 3f).noSave().fireImmune().clientTrackingRange(32)
                    .updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_mob_draak"))));

    /** Command name -> type (the dev command and the gametests). */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> SOORTEN = new LinkedHashMap<>();

    static {
        SOORTEN.put("klodder", KLODDER);
        SOORTEN.put("kloddertje", KLODDERTJE);
        SOORTEN.put("bubbelklodder", BUBBELKLODDER);
        SOORTEN.put("stuiterklodder", STUITERKLODDER);
        SOORTEN.put("moederklodder", MOEDERKLODDER);
        SOORTEN.put("hoedjesbol", HOEDJESBOL);
        SOORTEN.put("sporenpuffer", SPORENPUFFER);
        SOORTEN.put("zwamwachter", ZWAMWACHTER);
        SOORTEN.put("mosgeest", MOSGEEST);
        SOORTEN.put("druipslijm", DRUIPSLIJM);
        SOORTEN.put("kristalklodder", KRISTALKLODDER);
        SOORTEN.put("knaag", KNAAG);
        SOORTEN.put("glimzwam", GLIMZWAM);
        SOORTEN.put("tempelridder", TEMPELRIDDER);
        SOORTEN.put("sporenpriester", SPORENPRIESTER);
    }

    /** The armour items (one per base type, kern.loot.RijkItem with an equippable slot). */
    public static final Map<String, DeferredItem<Item>> HARNAS = new LinkedHashMap<>();

    static {
        WezensLoot.HARNAS.forEach((id, slot) -> HARNAS.put(id, harnas("vr_mob_" + id, slot)));
    }

    private static DeferredItem<Item> harnas(String id, Slot slot) {
        return ITEMS.registerItem(id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(slot.vanilla).build()));
    }

    private WezensRegistry() {
    }

    private static AttributeSupplier attr(double snelheid, double volg) {
        return VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, snelheid).add(Attributes.FOLLOW_RANGE, volg).build();
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(KLODDER.get(), attr(0.25, 12));
            e.put(KLODDERTJE.get(), attr(0.28, 14));
            e.put(BUBBELKLODDER.get(), attr(0.25, 14));
            e.put(STUITERKLODDER.get(), attr(0.25, 16));
            e.put(MOEDERKLODDER.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 14)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).build());
            e.put(DRUIPSLIJM.get(), attr(0.24, 12));
            e.put(KRISTALKLODDER.get(), attr(0.24, 14));
            e.put(HOEDJESBOL.get(), attr(0.26, 14));
            e.put(SPORENPUFFER.get(), attr(0.2, 12));
            e.put(ZWAMWACHTER.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.21).add(Attributes.FOLLOW_RANGE, 14)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).build());
            e.put(GLIMZWAM.get(), attr(0.24, 14));
            e.put(TEMPELRIDDER.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 18)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).build());
            e.put(SPORENPRIESTER.get(), attr(0.23, 20));
            e.put(MOSGEEST.get(), attr(0.25, 16));
            e.put(KNAAG.get(), attr(0.34, 16));
            e.put(KAMPKLODDER.get(), Kampklodder.attributen().build());
        });
        WezensLoot.basisTypes();
        WezensLoot.tabellen();
        WezensSpawners.registreer();
        haken();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Effecten.tick();
            DraakVlucht.tick();
            WezensCommando.Later.tick();
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl != null && sl.getGameTime() % 20 == 7) {
                WezensSpawners.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener(WezensCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    /** The damage-pipeline hooks: the Zwamwachter's shield, the Kristalklodder's mirror, the Mosgeest's dodge, the set bonuses. */
    private static void haken() {
        Schade.VOOR_KLAP.add((k, n) -> {
            LivingEntity doel = k.doel(), bron = k.bron();
            if (!(doel.level() instanceof ServerLevel sl) || k.soort() == Schade.Soort.WAAR) {
                return n;
            }
            if (doel instanceof Zwammen.Zwamwachter w && w.blokt(bron)) {
                w.blokEffect(sl);
                Getallen.tekst(sl, w.getX(), w.getY() + w.getBbHeight() + 0.3, w.getZ(), Component.translatable("vadsrijk.wezens.geblokt"), 0.8f);
                return n * 0.1;
            }
            if (doel instanceof Klodders.Kristalklodder kk && k.soort() == Schade.Soort.MAGIE && bron != null
                    && kk.getRandom().nextDouble() < Klodders.Kristalklodder.WEERKAATS) {
                kk.speel("speciaal");
                Getallen.tekst(sl, kk.getX(), kk.getY() + kk.getBbHeight() + 0.3, kk.getZ(), Component.translatable("vadsrijk.wezens.weerkaatst"), 0.8f);
                sl.playSound(null, kk.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1.4f);
                double terug = n * 0.5;
                Schade.doe(new Schade.Klap(kk, bron, terug, kk.element(), Schade.Soort.WAAR, false, "weerkaatst"));
                return 0;
            }
            if (doel instanceof Anderen.Mosgeest m && m.probeerOntwijken(bron)) {
                Getallen.tekst(sl, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), Component.translatable("vadsrijk.wezens.ontwijkt"), 0.8f);
                return 0;
            }
            if (doel instanceof Player p && bron instanceof VadsrijkMob) {
                if (SetBonussen.stukken(p, SetBonussen.KLODDER) >= 2 && p.getRandom().nextDouble() < 0.15) {
                    Getallen.tekst(sl, p.getX(), p.getY() + 2.1, p.getZ(), Component.translatable("vadsrijk.wezens.set.ontweken"), 0.7f);
                    return 0;
                }
                if (SetBonussen.stukken(p, SetBonussen.ZWAM) >= 2) {
                    n *= 0.9;
                }
            }
            if (bron instanceof Player p) {
                if (SetBonussen.stukken(p, SetBonussen.KLODDER) >= 4) {
                    n *= 1.12;
                }
                if (k.soort() == Schade.Soort.MAGIE && SetBonussen.stukken(p, SetBonussen.ZWAM) >= 4) {
                    n *= 1.12;
                }
            }
            return n;
        });
        Schade.NA_KLAP.add(u -> {
            if (u.schade() <= 0 || !(u.klap().bron() instanceof Player p) || !(u.klap().doel() instanceof VadsrijkMob m) || m.geplofd()) {
                return;
            }
            if (u.klap().soort() != Schade.Soort.WAAR && SetBonussen.stukken(p, SetBonussen.KLODDER) >= 4 && p.getRandom().nextDouble() < 0.2) {
                Statussen.geef(m, Status.PLAKKERIG, 40, 1, p);
            }
            if (u.klap().soort() == Schade.Soort.MAGIE && SetBonussen.stukken(p, SetBonussen.ZWAM) >= 4) {
                Statussen.geef(m, Status.GESPOREN, 60, Math.max(1, u.schade() * 0.08), p);
            }
        });
    }
}
