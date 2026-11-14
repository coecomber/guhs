package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.karakter.KarakterRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kleding.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * Het Vadsrijk zone 6 "Het Zijdeland", slice U-Z6RAID: the first raid, "De Sterrendovende Toren" (2-10 players, 4 floors, 4
 * bosses). Entities {@code vr_z6raid_*}, lang {@code vadsrijk.z6raid.*}, arenas {@code z6raid:<baas>}, dev commands
 * {@code /guhs vadsrijk z6raid ...} ({@link Z6RaidCommando}). Resources: tools/features/vadsrijk_u_z6raid.py; the inside of the
 * tower: tools/vadsrijk/zone6/raid/binnentoren.py.
 */
public final class Z6RaidRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    private static <T extends VadsrijkMob> DeferredHolder<EntityType<?>, EntityType<T>> type(String id, EntityType.EntityFactory<T> f, float w, float h, float oog) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(w, h).eyeHeight(oog).clientTrackingRange(16)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Zandwachter>> ZANDWACHTER = type("vr_z6raid_baas_zandwachter", Zandwachter::new, 2.4f, 4.2f, 3.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Spiegelvizier>> SPIEGELVIZIER = type("vr_z6raid_baas_spiegelvizier", Spiegelvizier::new, 0.9f, 2.3f,
            2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Sterrenbeeld>> STER_KNUFFEL = type("vr_z6raid_baas_ster_knuffel", Sterrenbeeld::new, 2.2f, 3.0f, 2.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Sterrenbeeld>> STER_BULTGUH = type("vr_z6raid_baas_ster_bultguh", Sterrenbeeld::new, 2.0f, 2.4f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Sterrenbeeld>> STER_PLUIS = type("vr_z6raid_baas_ster_pluis", Sterrenbeeld::new, 1.0f, 1.2f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Ringeling>> RINGELING = type("vr_z6raid_baas_ringeling", Ringeling::new, 0.9f, 2.4f, 2.1f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> ZANDPILAAR = type("vr_z6raid_zandpilaar", Z3Doel::new, 1.4f, 3.4f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> DRAAISPIEGEL = type("vr_z6raid_draaispiegel", Z3Doel::new, 1.2f, 2.4f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> STERRENBAKEN = type("vr_z6raid_sterrenbaken", Z3Doel::new, 1.2f, 2.0f, 1.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> STERRENSCHERF = type("vr_z6raid_sterrenscherf", Z3Doel::new, 0.8f, 0.9f, 0.5f);

    /** The rare pet: the Kleine Pluis, cheered up and tiny (only from a clear chest: its "baas" id never matches a kill). */
    public static final Huisdier PET_STERRENPLUISJE = Huisdieren.registreer(new Huisdier(Z6RaidLoot.HUISDIER, "guhs:vr_z6raid_nooit", "vr_z6raid_baas_ster_pluis",
            "textures/vadsrijk/z6raid/ster_pluis_blij.png", "vr_z6raid_baas_ster_pluis", 0.45f, "textures/vadsrijk/z6raid/ster_pluis_blij_glowmask.png"));

    private Z6RaidRegistry() {
    }

    public static void register(IEventBus modBus) {
        Z6RaidLoot.SET_ITEMS.size();   // (the item holders exist before ITEMS is registered)
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            for (var t : List.of(ZANDWACHTER, RINGELING)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                        .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.STEP_HEIGHT, 1.2).build());
            }
            e.put(SPIEGELVIZIER.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            for (var t : List.of(STER_KNUFFEL, STER_BULTGUH, STER_PLUIS)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                        .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            }
            for (var t : List.of(ZANDPILAAR, DRAAISPIEGEL, STERRENBAKEN, STERRENSCHERF)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
        });
        Z6RaidLoot.typen();
        arenas();
        haken();
        Z6RaidHeraut.init();   // X-RAID: the heralds, their role and the tracker line
        nl.juiced.guhs.world.TikWacht.post("Z6RaidRegistry:96", e -> Z6Raid.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(Z6RaidCommando::register);
    }

    public static void payloads(PayloadRegistrar r) {
        Z6RaidPayloads.register(r);
    }

    private static void arenas() {
        for (int k = 1; k <= 4; k++) {
            int verd = k;
            Arena a = new Arena(Z6Raid.arenaId(k), Z6RaidPlekken.midden(k), Z6RaidPlekken.STRAAL, Z6RaidPlekken.landing(k), 5,
                    Z6RaidPlekken.landing(k).above(2), "vadsrijk.z6raid." + Z6Raid.BAZEN[k], 2, k == 4 ? 17 : 10);
            Arenas.registreer(a, sl -> Z6Raid.magSpawnen(verd) ? maak(sl, verd) : null);
        }
    }

    /** The real boss of floor k (lv 70). */
    public static VadsrijkBaas maak(ServerLevel sl, int k) {
        EntityType<? extends Z6RaidBaas> t = switch (k) {
            case 1 -> ZANDWACHTER.get();
            case 2 -> SPIEGELVIZIER.get();
            case 3 -> STER_KNUFFEL.get();
            default -> RINGELING.get();
        };
        Z6RaidBaas b = t.create(sl, EntitySpawnReason.SPAWNER);
        if (b != null) {
            b.stelIn(Z6RaidBaas.LEVEL, Element.ROZE, false, Set.of());
        }
        return b;
    }

    private static void haken() {
        Flauwvallen.CHECKPOINT_KIEZER.add(Z6Raid::checkpoint);
        Haken.FLAUW.add(Z6Raid::flauw);
        KarakterRegistry.ZACHT_ALS.add(Z6Raid::zacht);
        Spelers.EXTRA_STATS.add(Z6RaidLoot::stats);
        Schade.VOOR_KLAP.add((k, n) -> {
            if (k.soort() != Schade.Soort.WAAR && k.bron() instanceof Player p && k.doel() instanceof VadsrijkBaas && Z6RaidLoot.stukken(p) >= 4) {
                return n * Z6RaidLoot.BAAS_FACTOR;
            }
            return n;
        });
        Titels.registreer(new Titels.Titel("z6raid_ringbreker", "gui.guhs.titels.naam.z6raid_ringbreker", ChatFormatting.LIGHT_PURPLE, "guhs:vr_z6raid_ringreplica",
                p -> Z6RaidLoot.heeftTitel(p, "ringbreker")));
        Titels.registreer(new Titels.Titel("z6raid_sterrenaansteker", "gui.guhs.titels.naam.z6raid_sterrenaansteker", ChatFormatting.GOLD, "guhs:vr_z6raid_sterrenstaf",
                p -> Z6RaidLoot.heeftTitel(p, "sterrenaansteker")));
        Cosmetica.registreer(new Cosmetica.Ding(Z6RaidLoot.VLEUGELS, Cosmetica.Soort.VLEUGEL, "geheim:z6raid_vleugels", Cosmetica.Kracht.LUCHTDASH, 0, 6));
    }
}
