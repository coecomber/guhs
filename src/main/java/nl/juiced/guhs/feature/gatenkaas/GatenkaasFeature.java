package nl.juiced.guhs.feature.gatenkaas;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
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

import net.minecraft.resources.Identifier;
/**
 * De Gatenkaasgrotten (2.7): the underground of the Guhmension (below about y 40) in some places turns into one big
 * cheese with holes. Round gatenkaas holes (the {@link GatenkaasHolteFeature}), kaas stalactites and stalagmites that
 * drip kaassaus ({@link KaasStalactietBlock}), glowing kaasmos, rare kaaskorrel ore and an abandoned cheese mine shaft.
 * <p>
 * Somewhere deep in those caves lies de Stille Voorraadkelder, the secret larder of the Mika's (an Ancient City parody):
 * knabbelsensoren hear you walk, eat and dig ({@link Knabbelgeluid}), knabbelschreeuwers count it, and at the third
 * warning the Vadswaker ({@link VadswakerEntity}), a blind Mika who hears you chew, climbs out of the ground. Sneaking is
 * safe for your paws; the effect guhs:stil ({@link #STIL}, from a stille knabbel) makes you completely inaudible.
 * Resources: tools/features/gatenkaas.py.
 */
public final class GatenkaasFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    public static final ResourceKey<Biome> GATENKAASGROTTEN = ResourceKey.create(Registries.BIOME, Guhs.id("gatenkaasgrotten"));
    public static final ResourceKey<Structure> VOORRAADKELDER = ResourceKey.create(Registries.STRUCTURE, Guhs.id("stille_voorraadkelder"));
    public static final ResourceKey<Structure> MIJNSCHACHT = ResourceKey.create(Registries.STRUCTURE, Guhs.id("gatenkaas_mijnschacht"));

    // --- the rock of the cheese caves -----------------------------------------------------------------------------------
    private static BlockBehaviour.Properties cheese() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.2f, 4f).requiresCorrectToolForDrops().sound(SoundType.TUFF);
    }

    private static BlockBehaviour.Properties aged() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(2.5f, 6f).requiresCorrectToolForDrops()
                .sound(SoundType.DEEPSLATE_BRICKS);
    }

    public static final DeferredBlock<Block> GATENKAAS = BLOCKS.registerSimpleBlock("gatenkaas", () -> cheese());
    public static final DeferredBlock<Block> GATENKAAS_STENEN = BLOCKS.registerSimpleBlock("gatenkaas_stenen",
            () -> cheese().sound(SoundType.TUFF_BRICKS));
    public static final DeferredBlock<StairBlock> GATENKAAS_STENEN_TRAP = BLOCKS.registerBlock("gatenkaas_stenen_trap",
            p -> new StairBlock(GATENKAAS_STENEN.get().defaultBlockState(), p), () -> cheese().sound(SoundType.TUFF_BRICKS));
    public static final DeferredBlock<SlabBlock> GATENKAAS_STENEN_PLAAT = BLOCKS.registerBlock("gatenkaas_stenen_plaat",
            SlabBlock::new, () -> cheese().sound(SoundType.TUFF_BRICKS));
    public static final DeferredBlock<WallBlock> GATENKAAS_STENEN_MUUR = BLOCKS.registerBlock("gatenkaas_stenen_muur",
            WallBlock::new, () -> cheese().sound(SoundType.TUFF_BRICKS).forceSolidOn());
    /** Old, dark, aged cheese: the building stone of the Stille Voorraadkelder (smelt gatenkaas bricks). */
    public static final DeferredBlock<Block> BELEGEN_KAAS_STENEN = BLOCKS.registerSimpleBlock("belegen_kaas_stenen",
            () -> aged());
    public static final DeferredBlock<Block> BELEGEN_KAAS_TEGELS = BLOCKS.registerSimpleBlock("belegen_kaas_tegels",
            () -> aged().sound(SoundType.DEEPSLATE_TILES));
    public static final DeferredBlock<StairBlock> BELEGEN_KAAS_TRAP = BLOCKS.registerBlock("belegen_kaas_trap",
            p -> new StairBlock(BELEGEN_KAAS_STENEN.get().defaultBlockState(), p), () -> aged());
    public static final DeferredBlock<SlabBlock> BELEGEN_KAAS_PLAAT = BLOCKS.registerBlock("belegen_kaas_plaat",
            SlabBlock::new, () -> aged());
    public static final DeferredBlock<WallBlock> BELEGEN_KAAS_MUUR = BLOCKS.registerBlock("belegen_kaas_muur",
            WallBlock::new, () -> aged().forceSolidOn());

    // --- decoration: stalactites, glowing moss, the ore -----------------------------------------------------------------
    public static final DeferredBlock<KaasStalactietBlock> KAAS_STALACTIET = BLOCKS.registerBlock("kaas_stalactiet", KaasStalactietBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().noOcclusion().sound(SoundType.POINTED_DRIPSTONE)
                    .randomTicks().strength(1.2f, 3f).dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((s, l, p) -> false));
    public static final DeferredBlock<Block> KAASMOS = BLOCKS.registerSimpleBlock("kaasmos",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1f).sound(SoundType.MOSS).lightLevel(s -> 9)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<CarpetBlock> KAASMOS_TAPIJT = BLOCKS.registerBlock("kaasmos_tapijt", CarpetBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1f).sound(SoundType.MOSS_CARPET).lightLevel(s -> 6)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<DropExperienceBlock> KAASKORRELERTS = BLOCKS.registerBlock("kaaskorrelerts",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            () -> cheese().strength(2.5f, 4f).lightLevel(s -> 3));

    // --- de Stille Voorraadkelder --------------------------------------------------------------------------------------
    public static final DeferredBlock<KnabbelsensorBlock> KNABBELSENSOR = BLOCKS.registerBlock("knabbelsensor", KnabbelsensorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f).sound(SoundType.SCULK_SENSOR).noOcclusion()
                    .lightLevel(s -> s.getValue(KnabbelsensorBlock.ACTIVE) ? 8 : 2).isRedstoneConductor((s, l, p) -> false));
    public static final DeferredBlock<KnabbelschreeuwerBlock> KNABBELSCHREEUWER = BLOCKS.registerBlock("knabbelschreeuwer",
            KnabbelschreeuwerBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3f, 3f)
                    .sound(SoundType.SCULK_SHRIEKER).noOcclusion().lightLevel(s -> s.getValue(KnabbelschreeuwerBlock.SHRIEKING) ? 10 : 0)
                    .isRedstoneConductor((s, l, p) -> false));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<MobEffect, MobEffect> STIL = MOB_EFFECTS.register("stil", StilEffect::new);

    /** The crunchy crystal from old cheese (like the crunchy bits in old Gouda). Rare: only in the gatenkaas. */
    public static final DeferredItem<Item> KAASKORREL = ITEMS.registerSimpleItem("kaaskorrel", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    /** A knabbel you nibble very, very quietly: a while guhs:stil. */
    public static final DeferredItem<Item> STILLE_KNABBEL = ITEMS.registerSimpleItem("stille_knabbel", () -> new Item.Properties().rarity(Rarity.UNCOMMON)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).alwaysEdible().build(),
                    net.minecraft.world.item.component.Consumables.defaultFood().onConsume(
                            new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new MobEffectInstance(STIL, 20 * 90, 0), 1f)).build()));

    public static final DeferredHolder<EntityType<?>, EntityType<VadswakerEntity>> VADSWAKER = ENTITY_TYPES.register("vadswaker",
            () -> EntityType.Builder.of(VadswakerEntity::new, MobCategory.MONSTER).sized(1.9f, 1.75f).eyeHeight(1.2f)
                    .clientTrackingRange(16).fireImmune().notInPeaceful().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vadswaker"))));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VADSWAKER_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "vadswaker_spawn_egg", VADSWAKER);   // 1.0.0 colours 0x6B4A2E / 0xF2C94C (26.1: no tint)

    public static final DeferredHolder<Feature<?>, GatenkaasHolteFeature> HOLTE = FEATURES.register("gatenkaas_holte",
            () -> new GatenkaasHolteFeature(NoneFeatureConfiguration.CODEC));

    static {
        for (DeferredBlock<?> block : List.of(GATENKAAS, GATENKAAS_STENEN, GATENKAAS_STENEN_TRAP, GATENKAAS_STENEN_PLAAT, GATENKAAS_STENEN_MUUR,
                BELEGEN_KAAS_STENEN, BELEGEN_KAAS_TEGELS, BELEGEN_KAAS_TRAP, BELEGEN_KAAS_PLAAT, BELEGEN_KAAS_MUUR,
                KAAS_STALACTIET, KAASMOS, KAASMOS_TAPIJT, KAASKORRELERTS, KNABBELSENSOR, KNABBELSCHREEUWER)) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        MOB_EFFECTS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(VADSWAKER.get(), VadswakerEntity.createAttributes().build()));
        Knabbelgeluid.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(GatenkaasEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GatenkaasEvents::onVadswakerDeath);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(GATENKAAS, GATENKAAS_STENEN, GATENKAAS_STENEN_TRAP, GATENKAAS_STENEN_PLAAT, GATENKAAS_STENEN_MUUR,
                BELEGEN_KAAS_STENEN, BELEGEN_KAAS_TEGELS, BELEGEN_KAAS_TRAP, BELEGEN_KAAS_PLAAT, BELEGEN_KAAS_MUUR,
                KAAS_STALACTIET, KAASMOS, KAASMOS_TAPIJT, KAASKORRELERTS, KNABBELSENSOR, KNABBELSCHREEUWER)) {
            output.accept(new ItemStack(block.get()));
        }
        for (var item : List.of(KAASKORREL, STILLE_KNABBEL, VADSWAKER_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private GatenkaasFeature() {
    }
}
