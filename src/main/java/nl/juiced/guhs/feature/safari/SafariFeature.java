package nl.juiced.guhs.feature.safari;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Protected;

/**
 * Op Safari! (1.6.0), step 1: Safaripark Vadse Bergen and its animals (design ontwerp-safaripark-en-bouwen.md, contract
 * SAFARI_CONTRACT.md).
 * <ul>
 *   <li>the dimension {@code guhs:safaripark} (data/guhs/dimension*, an empty noise world with the biome source
 *       {@link SafariBiomeSource}: the biomes {@code guhs:safari_<zone>} follow the park's zones);</li>
 *   <li>the park, stamped from tile templates in versions ({@link ParkBouwer}), protected ({@link SafariRegels});</li>
 *   <li>the animals: one entity type {@link SafariDier} for every species, and their populations ({@link Populatie});</li>
 *   <li>the dev command {@code /guhs safari} ({@link SafariCommando}, {@link SafariToegang}).</li>
 * </ul>
 * Data written by the generators (tools/features/safari*.py, slices B and C) is read by {@link ParkData}.
 */
public final class SafariFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SafariDier>> DIER = ENTITY_TYPES.register("safari_dier",
            () -> EntityType.Builder.of(SafariDier::new, MobCategory.CREATURE).sized(0.9f, 1.2f).eyeHeight(1.0f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("safari_dier"))));

    static {
        BIOME_SOURCES.register("safari_zones", () -> SafariBiomeSource.CODEC);
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        BIOME_SOURCES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(DIER.get(), SafariDier.createAttributes().build()));
        Protected.add(Safari::in);   // (fire and fluids)
        NeoForge.EVENT_BUS.register(SafariRegels.class);
        NeoForge.EVENT_BUS.addListener(SafariCommando::register);
        nl.juiced.guhs.feature.safari.kern.SafariKern.register();   // step 2: per-player data, safarimunten, events, sync
        nl.juiced.guhs.feature.safari.quests.SafariQuests.register(modBus);   // S4: quests, geheimpjes, titles, dex rewards
    }

    public static void payloads(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        nl.juiced.guhs.feature.safari.kern.SafariKern.payloads(registrar);
    }

    private SafariFeature() {
    }
}
