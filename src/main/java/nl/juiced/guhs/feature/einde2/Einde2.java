package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;

/**
 * einde2: the Guheinde expansion (six biomes on the outer islands, their guhs and creatures, the modular Mika-vesting, the
 * big places of the Guhvatar story, the side places, and the cast and the two places of the Sponsguh story). This class is
 * the one door from {@code feature/Features} into the update: it registers what the whole update shares (the structure type
 * {@code guhs:leegte_bouw}, protection, discovery, the /guhs einde2 commands) and then calls the seventeen slices, each in
 * its own package under this one.
 * <p>
 * Shared by every slice: {@link Einde2Ids} (ids, tags, height bands), {@link Plekken} (where a structure and its markers
 * are), {@link Einde2Bescherming}, {@link Einde2Ontdekt}, {@link Einde2Haken} (the hooks for the later story and tech
 * phases), {@link Einde2Test} (the Guheinde's generator in a game test). After the three cast slices: {@link Bewoners} (the
 * residents at the markers), {@link CastTabellen} (the three cast tables as one) and {@link Einde2Toon} (the dev rows of every
 * model). Resources: tools/features/einde2.py and the
 * slices' own tools/features/einde2_*.py; the helper library is tools/features/einde2_kit.py.
 */
public final class Einde2 {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Guhs.MODID);

    /** A big place floating in the high band of the Guheinde, or lying in its deep band (see {@link LeegteBouwStructure}). */
    public static final DeferredHolder<StructureType<?>, StructureType<LeegteBouwStructure>> LEEGTE_BOUW =
            STRUCTURE_TYPES.register("leegte_bouw", () -> () -> LeegteBouwStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LEEGTE_STUK =
            STRUCTURE_PIECES.register("leegte_bouw_stuk", () -> (StructurePieceType.StructureTemplateType) LeegteBouwStructure.Stuk::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LEEGTE_RAND =
            STRUCTURE_PIECES.register("leegte_bouw_rand", () -> (StructurePieceType.ContextlessType) LeegteBouwStructure.Rand::new);

    /** How far around the pieces of a big place nothing can be broken or built. */
    public static final int BESCHERM_RAND = 4;

    private static final List<DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>>> PLAATSHOUDERS = new ArrayList<>();

    /**
     * Registers a stand-in entity type under an id a slice will fill in later ({@link Einde2Plaatshouder}): its attributes,
     * its (empty) renderer and, when the id is a Guhdex creature page, that page are taken care of here. The owner slice
     * replaces this call by its own entity type (and then registers attributes, renderer and page itself).
     */
    public static DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> plaatshouder(DeferredRegister<EntityType<?>> register, String id) {
        DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> type = register.register(id,
                () -> EntityType.Builder.of(Einde2Plaatshouder::new, MobCategory.MISC).sized(0.6f, 0.6f).eyeHeight(0.45f)
                        .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
        PLAATSHOUDERS.add(type);
        return type;
    }

    /** The stand-in entity types that are still there (the client gives each an empty renderer). */
    public static List<DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>>> plaatshouders() {
        return Collections.unmodifiableList(PLAATSHOUDERS);
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        Einde2Bescherming.register();
        Einde2Ontdekt.register();
        NeoForge.EVENT_BUS.addListener(Einde2Commando::register);
        NeoForge.EVENT_BUS.addListener(LeegteBouwStructure::onLevelLoad);
        // (after the world has registered its structure state for the guaranteed copies, which it does at normal priority)
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, Plekken::onLevelLoad);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                GuhDex.onthoudKennerEinde2(player);   // (whoever had a full Guhdex before the six biome guhs keeps the title)
            }
        });
        for (String structuur : Einde2Ids.LEEGTE_STRUCTUREN) {
            Einde2Bescherming.registreer(structuur, BESCHERM_RAND);
        }
        // the seventeen slices, in the order of the build contract
        nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.biomeguhs.E2BiomeguhsFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.mikas.E2MikasFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.appa.E2AppaFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.vetbootje.E2VetbootjeFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.vesting.E2VestingFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.deco.E2DecoFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwlucht.E2BouwLuchtFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwwater.E2BouwWaterFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwaarde.E2BouwAardeFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwvuur.E2BouwVuurFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwzij1.E2BouwZij1Feature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwzij2.E2BouwZij2Feature.register(modBus);
        nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwbikinibodem.E2BouwBikinibodemFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.castvolk.E2CastVolkFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.bouwschelpenstad.E2BouwSchelpenstadFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.castteam.E2CastTeamFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.geestwezens.E2GeestWezensFeature.register(modBus);
        nl.juiced.guhs.feature.einde2.geestenwereld.E2GeestenwereldFeature.register(modBus);   // <einde2_geestenwereld/>
        // phase B + C (CONTRACT_E2_B): the ten slices of the story and tier 5, one line each
        nl.juiced.guhs.feature.einde2.h1lucht.E2H1LuchtFeature.register(modBus);   // <einde2_h1_lucht/>
        nl.juiced.guhs.feature.einde2.h2water.E2H2WaterFeature.register(modBus);   // <einde2_h2_water/>
        nl.juiced.guhs.feature.einde2.h3aarde.E2H3AardeFeature.register(modBus);   // <einde2_h3_aarde/>
        nl.juiced.guhs.feature.einde2.h4vuur.E2H4VuurFeature.register(modBus);   // <einde2_h4_vuur/>
        nl.juiced.guhs.feature.einde2.zij.E2ZijFeature.register(modBus);   // <einde2_zij/>
        nl.juiced.guhs.feature.einde2.geestspel.E2GeestSpelFeature.register(modBus);   // <einde2_geest_spel/>
        nl.juiced.guhs.feature.einde2.reizen.E2ReizenFeature.register(modBus);   // <einde2_reizen/>
        nl.juiced.guhs.feature.einde2.audio.E2AudioFeature.register(modBus);   // <einde2_audio/>
        nl.juiced.guhs.feature.einde2.trap5.E2Trap5Feature.register(modBus);   // <einde2_trap5_machines/>
        nl.juiced.guhs.feature.einde2.ballon.E2BallonFeature.register(modBus);   // <einde2_trap5_ballon/>
        // the merge step of the cast (CONTRACT_E2 11.7, 11.9): residents at the markers, named animations on demand, the dev rows
        Bewoners.register();
        // phase B (CONTRACT_E2_B 9): the story state, the big story of the Guhpad, the veils, the hooks, the dev command
        nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal.register();
        CastTabellen.registreer();
        NeoForge.EVENT_BUS.addListener(Einde2Toon::register);
        // the stand-ins that are left: attributes, and their Guhdex page (the page's name is the entity id)
        modBus.addListener((EntityAttributeCreationEvent event) -> PLAATSHOUDERS.forEach(
                type -> event.put(type.get(), Einde2Plaatshouder.createAttributes().build())));
        for (var type : PLAATSHOUDERS) {
            String pagina = type.getId().getPath().toUpperCase(Locale.ROOT);
            for (GuhVariant v : GuhVariant.values()) {
                if (v.name().equals(pagina)) {
                    GuhDex.creaturePage(v, type);
                }
            }
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.biomeguhs.E2BiomeguhsFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.mikas.E2MikasFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.appa.E2AppaFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.vetbootje.E2VetbootjeFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.vesting.E2VestingFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.deco.E2DecoFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwlucht.E2BouwLuchtFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwwater.E2BouwWaterFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwaarde.E2BouwAardeFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwvuur.E2BouwVuurFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwzij1.E2BouwZij1Feature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwzij2.E2BouwZij2Feature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwbikinibodem.E2BouwBikinibodemFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.castvolk.E2CastVolkFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.bouwschelpenstad.E2BouwSchelpenstadFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.castteam.E2CastTeamFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.geestwezens.E2GeestWezensFeature.payloads(registrar);
        nl.juiced.guhs.feature.einde2.geestenwereld.E2GeestenwereldFeature.payloads(registrar);   // <einde2_geestenwereld/>
        nl.juiced.guhs.feature.einde2.h1lucht.E2H1LuchtFeature.payloads(registrar);   // <einde2_h1_lucht/>
        nl.juiced.guhs.feature.einde2.h2water.E2H2WaterFeature.payloads(registrar);   // <einde2_h2_water/>
        nl.juiced.guhs.feature.einde2.h3aarde.E2H3AardeFeature.payloads(registrar);   // <einde2_h3_aarde/>
        nl.juiced.guhs.feature.einde2.h4vuur.E2H4VuurFeature.payloads(registrar);   // <einde2_h4_vuur/>
        nl.juiced.guhs.feature.einde2.zij.E2ZijFeature.payloads(registrar);   // <einde2_zij/>
        nl.juiced.guhs.feature.einde2.geestspel.E2GeestSpelFeature.payloads(registrar);   // <einde2_geest_spel/>
        nl.juiced.guhs.feature.einde2.reizen.E2ReizenFeature.payloads(registrar);   // <einde2_reizen/>
        nl.juiced.guhs.feature.einde2.audio.E2AudioFeature.payloads(registrar);   // <einde2_audio/>
        nl.juiced.guhs.feature.einde2.trap5.E2Trap5Feature.payloads(registrar);   // <einde2_trap5_machines/>
        nl.juiced.guhs.feature.einde2.ballon.E2BallonFeature.payloads(registrar);   // <einde2_trap5_ballon/>
    }

    public static void creative(Consumer<ItemStack> output) {
        nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature.creative(output);
        nl.juiced.guhs.feature.einde2.biomeguhs.E2BiomeguhsFeature.creative(output);
        nl.juiced.guhs.feature.einde2.mikas.E2MikasFeature.creative(output);
        nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature.creative(output);
        nl.juiced.guhs.feature.einde2.appa.E2AppaFeature.creative(output);
        nl.juiced.guhs.feature.einde2.vetbootje.E2VetbootjeFeature.creative(output);
        nl.juiced.guhs.feature.einde2.vesting.E2VestingFeature.creative(output);
        nl.juiced.guhs.feature.einde2.deco.E2DecoFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwlucht.E2BouwLuchtFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwwater.E2BouwWaterFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwaarde.E2BouwAardeFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwvuur.E2BouwVuurFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwzij1.E2BouwZij1Feature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwzij2.E2BouwZij2Feature.creative(output);
        nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature.creative(output);
        nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwbikinibodem.E2BouwBikinibodemFeature.creative(output);
        nl.juiced.guhs.feature.einde2.castvolk.E2CastVolkFeature.creative(output);
        nl.juiced.guhs.feature.einde2.bouwschelpenstad.E2BouwSchelpenstadFeature.creative(output);
        nl.juiced.guhs.feature.einde2.castteam.E2CastTeamFeature.creative(output);
        nl.juiced.guhs.feature.einde2.geestwezens.E2GeestWezensFeature.creative(output);
        nl.juiced.guhs.feature.einde2.geestenwereld.E2GeestenwereldFeature.creative(output);   // <einde2_geestenwereld/>
        nl.juiced.guhs.feature.einde2.h1lucht.E2H1LuchtFeature.creative(output);   // <einde2_h1_lucht/>
        nl.juiced.guhs.feature.einde2.h2water.E2H2WaterFeature.creative(output);   // <einde2_h2_water/>
        nl.juiced.guhs.feature.einde2.h3aarde.E2H3AardeFeature.creative(output);   // <einde2_h3_aarde/>
        nl.juiced.guhs.feature.einde2.h4vuur.E2H4VuurFeature.creative(output);   // <einde2_h4_vuur/>
        nl.juiced.guhs.feature.einde2.zij.E2ZijFeature.creative(output);   // <einde2_zij/>
        nl.juiced.guhs.feature.einde2.geestspel.E2GeestSpelFeature.creative(output);   // <einde2_geest_spel/>
        nl.juiced.guhs.feature.einde2.reizen.E2ReizenFeature.creative(output);   // <einde2_reizen/>
        nl.juiced.guhs.feature.einde2.audio.E2AudioFeature.creative(output);   // <einde2_audio/>
        nl.juiced.guhs.feature.einde2.trap5.E2Trap5Feature.creative(output);   // <einde2_trap5_machines/>
        nl.juiced.guhs.feature.einde2.ballon.E2BallonFeature.creative(output);   // <einde2_trap5_ballon/>
    }

    private Einde2() {
    }
}
