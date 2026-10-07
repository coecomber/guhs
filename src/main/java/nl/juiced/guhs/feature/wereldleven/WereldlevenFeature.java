package nl.juiced.guhs.feature.wereldleven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhDex;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * Het guhleven (2.8, slice wereldleven): the world feels alive. tools/features/wereldleven.py makes the resources.
 * <ul>
 *   <li>{@link Dagritme}: guhs in guh villages and the Knuffeldal and free-roaming tamed guhs live a guh day: a yawn and a
 *       stretch in the morning, a wave at players during the day, an afternoon nap in a nestje, the evening round the
 *       campfire with marshmallow knabbels, and zzz at night.</li>
 *   <li>{@link IJscoguhEntity}: IJscoguh Tingeling on his ice-cream bike with a bell, a wandering trader in the Guhmensie
 *       ({@link IJscoguhSpawner}); his {@link Kaasijsjes} (seven flavours, four of them seasonal) give cute effects, also
 *       to your guh (an ice-cream hat, blushing cheeks, floating). Guhs run after his cart.</li>
 *   <li>{@link Koortje}: the guh-xylofoon (six songs in the liedjesboekje), the guh-fluitje; guhs close by sing along,
 *       and singing makes the tuintjes grow (KnusSignalen.zang).</li>
 *   <li>{@link Grijpmachine}: the claw machine (at the kermis and on the Knuffeldal plein), a real claw you steer, paid
 *       with a ticket; 20 plushies (one per real guh variant) and a rare glitter one ({@link KnuffelBlock}). Tamed guhs
 *       cuddle the plushies you put down ({@link Knuffels}); the knuffelkast in the Guhdex (Knus tab).</li>
 * </ul>
 */
public final class WereldlevenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);

    /** The plushies: one per real guh variant (everything before REISGUH), in GuhVariant order, then the rare glitter one. */
    public static final List<String> KNUFFEL_IDS;
    public static final String GLITTER = "glitter";

    static {
        List<String> ids = new ArrayList<>();
        for (GuhVariant v : GuhVariant.values()) {
            if (v.isBioGuh()) {
                continue;   // biomes3: the Bloesemguh and the Tanukiguh have no plush (the complete knuffelkast stays what it was)
            }
            if (!v.isCharacter() && !v.isVerhaalGuh()) {   // (3.0: the story guhs have no plush; tools/features/wereldleven.py KNUFFEL_IDS)
                ids.add(v.name().toLowerCase(Locale.ROOT));
            }
        }
        ids.add(GLITTER);
        KNUFFEL_IDS = Collections.unmodifiableList(ids);
    }

    // --- blocks ---------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<XylofoonBlock> GUH_XYLOFOON = BLOCKS.registerBlock("guh_xylofoon", XylofoonBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<GrijpmachineBlock> GRIJPMACHINE = BLOCKS.registerBlock("grijpmachine", GrijpmachineBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0f, 6f).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 7));
    /** knuffel_&lt;id&gt; for every id of {@link #KNUFFEL_IDS}. */
    public static final Map<String, DeferredBlock<KnuffelBlock>> KNUFFELS = new LinkedHashMap<>();

    static {
        for (String id : KNUFFEL_IDS) {
            boolean glitter = id.equals(GLITTER);
            KNUFFELS.put(id, BLOCKS.registerBlock("knuffel_" + id, KnuffelBlock::new, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK).strength(0.4f).sound(SoundType.WOOL).noOcclusion().ignitedByLava()
                    .pushReaction(PushReaction.DESTROY).lightLevel(s -> glitter ? 5 : 0)));
        }
    }

    // --- items -----------------------------------------------------------------------------------------------------------
    public static final DeferredItem<FluitjeItem> GUH_FLUITJE = ITEMS.registerItem("guh_fluitje", FluitjeItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> MARSHMALLOW_KNABBEL = ITEMS.registerSimpleItem("marshmallow_knabbel",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build(), nl.juiced.guhs.registry.ModItems.FAST_FOOD));
    public static final DeferredItem<LiedjesboekjeItem> LIEDJESBOEKJE = ITEMS.registerItem("wereldleven_liedjesboekje", LiedjesboekjeItem::new,
            () -> new Item.Properties().stacksTo(1));
    public static final Map<Kaasijsjes.Smaak, DeferredItem<KaasijsjeItem>> KAASIJSJES = new EnumMap<>(Kaasijsjes.Smaak.class);

    static {
        for (Kaasijsjes.Smaak smaak : Kaasijsjes.Smaak.values()) {
            KAASIJSJES.put(smaak, ITEMS.registerItem(smaak.itemId(), p -> new KaasijsjeItem(smaak, p), () -> new Item.Properties().stacksTo(16)
                    .rarity(smaak.seizoen == null ? Rarity.COMMON : Rarity.UNCOMMON)
                    .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible().build(), nl.juiced.guhs.registry.ModItems.FAST_FOOD)));
        }
        ITEMS.registerItem("guh_xylofoon", p -> new BlockItem(GUH_XYLOFOON.get(), p), p -> p.useBlockDescriptionPrefix());
        ITEMS.registerItem("grijpmachine", p -> new DoubleHighBlockItem(GRIJPMACHINE.get(), p), p -> p.useBlockDescriptionPrefix());
        for (var e : KNUFFELS.entrySet()) {
            DeferredBlock<KnuffelBlock> block = e.getValue();
            boolean glitter = e.getKey().equals(GLITTER);
            ITEMS.registerItem("knuffel_" + e.getKey(), p -> new BlockItem(block.get(), p),
                    () -> new Item.Properties().rarity(glitter ? Rarity.EPIC : Rarity.COMMON).useBlockDescriptionPrefix());
        }
    }

    // --- IJscoguh Tingeling ------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<IJscoguhEntity>> IJSCOGUH = ENTITY_TYPES.register("ijscoguh",
            () -> EntityType.Builder.of(IJscoguhEntity::new, MobCategory.CREATURE).sized(1.2f, 2.1f).eyeHeight(1.75f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("guhs:ijscoguh"))));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> IJSCOGUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "ijscoguh_spawn_egg", IJSCOGUH);

    // --- block entity, effects, particles, sounds, the plushies' point of interest ------------------------------------------
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrijpmachineBlockEntity>> GRIJPMACHINE_BE =
            BLOCK_ENTITY_TYPES.register("grijpmachine", () -> new BlockEntityType<>(GrijpmachineBlockEntity::new, GRIJPMACHINE.get()));

    /** Blushing cheeks: hearts float up now and then, and a tiny bit of healing (from the roze / bloesem / appeltaart ijsjes). */
    public static final DeferredHolder<MobEffect, MobEffect> BLOSJES = MOB_EFFECTS.register("blosjes", WereldlevenEffects.Blosjes::new);
    /** Floaty: less gravity, soft landings (from the choco / zonnetje / sneeuw ijsjes). */
    public static final DeferredHolder<MobEffect, MobEffect> ZWEVERIG = MOB_EFFECTS.register("zweverig", WereldlevenEffects.Zweverig::new);

    /** A coloured music note that wobbles up (the koortje, the xylofoon, the fluitje). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ZANGNOOTJE = PARTICLES.register("zangnootje", () -> new SimpleParticleType(false));
    /** A little pink heart (ice creams, blosjes, cuddles). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IJSJESHARTJE = PARTICLES.register("ijsjeshartje", () -> new SimpleParticleType(false));
    /** A puff of steam from the fluitje. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLUITSTOOM = PARTICLES.register("fluitstoom", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> IJSCOBEL = sound("wereldleven.ijscobel");
    public static final DeferredHolder<SoundEvent, SoundEvent> XYLOFOON = sound("wereldleven.xylofoon");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUITJE = sound("wereldleven.fluitje");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRIJPKLAUW = sound("wereldleven.grijpklauw");

    /** Tamed guhs find the plushies near them (to cuddle them). */
    public static final DeferredHolder<PoiType, PoiType> KNUFFEL_POI = POI_TYPES.register("wereldleven_knuffel", () -> {
        ImmutableSet.Builder<BlockState> states = ImmutableSet.builder();
        for (DeferredBlock<KnuffelBlock> b : KNUFFELS.values()) {
            states.addAll(b.get().getStateDefinition().getPossibleStates());
        }
        return new PoiType(states.build(), 0, 1);
    });

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    // --- client hooks (set by WereldlevenClient; no-ops on a dedicated server) --------------------------------------------------
    /** Opens the xylofoon screen at this block (null: the liedjesboekje alone). */
    public static Consumer<BlockPos> openXylofoon = pos -> {
    };

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MOB_EFFECTS.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        POI_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(IJSCOGUH.get(), IJscoguhEntity.createAttributes().build()));
        Minigames.registerGame(Minigames.GRIJPMACHINE, Grijpmachine::speelt);
        GuhDex.creaturePage(GuhVariant.IJSCOGUH, IJSCOGUH);
        WereldlevenVoortgang.register();
        Dagritme.register();
        Knuffels.register();
        Kaasijsjes.register();
        IJscoguhSpawner.register();
        Koortje.register();
        Grijpmachine.register();
        NeoForge.EVENT_BUS.register(WereldlevenEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        WereldlevenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GUH_XYLOFOON.get()));
        output.accept(new ItemStack(GUH_FLUITJE.get()));
        output.accept(new ItemStack(LIEDJESBOEKJE.get()));
        output.accept(new ItemStack(GRIJPMACHINE.get()));
        for (DeferredBlock<KnuffelBlock> b : KNUFFELS.values()) {
            output.accept(new ItemStack(b.get()));
        }
        output.accept(new ItemStack(MARSHMALLOW_KNABBEL.get()));
        for (DeferredItem<KaasijsjeItem> ijsje : KAASIJSJES.values()) {
            output.accept(new ItemStack(ijsje.get()));
        }
        output.accept(new ItemStack(IJSCOGUH_SPAWN_EGG.get()));
    }

    /** The plushie block of a variant id (normal ... pluisguh, glitter), or null. */
    @javax.annotation.Nullable
    public static Block knuffel(String id) {
        DeferredBlock<KnuffelBlock> b = KNUFFELS.get(id);
        return b == null ? null : b.get();
    }

    /** The plushie id of a block (null: not a plushie). */
    @javax.annotation.Nullable
    public static String knuffelId(Block block) {
        for (var e : KNUFFELS.entrySet()) {
            if (e.getValue().get() == block) {
                return e.getKey();
            }
        }
        return null;
    }

    private WereldlevenFeature() {
    }
}
