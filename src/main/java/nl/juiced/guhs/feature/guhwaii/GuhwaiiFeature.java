package nl.juiced.guhs.feature.guhwaii;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;

/**
 * 3.0 (Guhverhalen), slice guhwaii: Lilo &amp; Stitch op Guhwai'i (DESIGN_30 §5, without the surf/hula minigames, those are
 * guhwaiispellen). Resources: tools/features/guhwaii.py (+ guhwaii_tex.py, guhwaii_bouw.py, guhwaii_modellen.py).
 * <ul>
 *   <li>The island's own things: guh-palms with a guh face in the trunk ({@link GuhwaiiBlokken.PalmGezicht}, fronds, the
 *       sprouting coconut), the {@code kokosnoot} (hangs, ripens, drops; eat it, or plant it on sand), {@code kokosmelk}, the pink
 *       hibiscus and three more tropical flowers (+ pots), the {@link SchillyEitjesBlock Schilly-eitjes} on the beaches that
 *       hatch into baby Poepschillys and Schillys, and the shallow kaaskoraal reef in the lagoon with guhvisjes
 *       ({@link GuhwaiiWorldgen}).</li>
 *   <li>The crashed capsule of 626-guh (structure {@link #CAPSULE} on the island's hill) with the free-standing
 *       {@link Scanner vadsigheid-scanner}: put any guh on it and watch the VADSIGHEIDSNIVEAU meter break through to
 *       ONBEREKENBAAR VAHOEG; the picture is also a poster ({@code vadsigheid_poster}) and a painting.</li>
 *   <li>Lilo-guh and Nani-guh's stilt house (structure {@link #OHANA} on the beach) and the ohana questline ({@link Ohana}):
 *       626 crashes, Lilo "adopts" him from the asiel, he makes a mess (only mess!), you help him be good, "Ohana betekent
 *       familie...". Then 626-guh is tameable once per player.</li>
 *   <li>The 626-guh ({@link Stitch626}): climbs walls and hangs from ceilings, carries two things at once (chores), and
 *       strums his ukelele: "Aloha, njeg!".</li>
 *   <li>Outfits (source {@code guhwaii}): hula-rokje, bloemenkrans, Stitch-oren + antennes, surfplankje.</li>
 * </ul>
 */
public final class GuhwaiiFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    /** The biome (made by the fundament) and the two structures of this slice. */
    public static final ResourceKey<Biome> GUHWAII = nl.juiced.guhs.feature.verhaal.VerhaalFeature.GUHWAII;
    public static final ResourceKey<Structure> OHANA = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guhwaii_ohana"));
    public static final ResourceKey<Structure> CAPSULE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guhwaii_capsule"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALM_BOOM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("guhwaii_palm"));
    public static final TreeGrower PALM_GROWER = new TreeGrower("guhs:guhwaii_palm", Optional.empty(), Optional.of(PALM_BOOM), Optional.empty());
    /** The source of the four outfit pieces (KledingBronnen). */
    public static final String BRON = "guhwaii";

    // --- the guh-palm ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> PALM_STAM = BLOCKS.registerBlock("guhwaii_palm_stam", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).mapColor(MapColor.WOOD));
    public static final DeferredBlock<GuhwaiiBlokken.PalmGezicht> PALM_GEZICHT = BLOCKS.registerBlock("guhwaii_palm_gezicht",
            GuhwaiiBlokken.PalmGezicht::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).mapColor(MapColor.WOOD));
    public static final DeferredBlock<GuhwaiiBlokken.PalmBlad> PALM_BLAD = BLOCKS.registerBlock("guhwaii_palm_blad", GuhwaiiBlokken.PalmBlad::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES).mapColor(MapColor.PLANT).sound(SoundType.AZALEA_LEAVES));
    public static final DeferredBlock<GuhwaiiBlokken.Kiemplant> PALM_KIEMPLANT = BLOCKS.registerBlock("guhwaii_palm_kiemplant",
            GuhwaiiBlokken.Kiemplant::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SAPLING).mapColor(MapColor.WOOD));
    public static final DeferredBlock<Block> PALM_PLANKEN = BLOCKS.registerSimpleBlock("guhwaii_palm_planken",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_PLANKS).mapColor(MapColor.SAND));
    public static final DeferredBlock<GuhwaiiBlokken.Kokosnoot> KOKOSNOOT = BLOCKS.registerBlock("kokosnoot", GuhwaiiBlokken.Kokosnoot::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.4f).sound(SoundType.WOOD).noOcclusion().randomTicks()
                    .pushReaction(PushReaction.DESTROY));

    // --- flowers ------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<GuhwaiiBlokken.TropischeBloem> ROZE_HIBISCUS = bloem("roze_hibiscus", MapColor.COLOR_PINK);
    public static final DeferredBlock<GuhwaiiBlokken.TropischeBloem> PLUMERIA = bloem("guhwaii_plumeria", MapColor.SNOW);
    public static final DeferredBlock<GuhwaiiBlokken.TropischeBloem> PARADIJSBLOEM = bloem("guhwaii_paradijsbloem", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<GuhwaiiBlokken.TropischeBloem> ORCHIDEE = bloem("guhwaii_orchidee", MapColor.COLOR_PURPLE);
    public static final List<DeferredBlock<GuhwaiiBlokken.TropischeBloem>> BLOEMEN = List.of(ROZE_HIBISCUS, PLUMERIA, PARADIJSBLOEM, ORCHIDEE);
    public static final DeferredBlock<FlowerPotBlock> POT_HIBISCUS = pot(ROZE_HIBISCUS);
    public static final DeferredBlock<FlowerPotBlock> POT_PLUMERIA = pot(PLUMERIA);
    public static final DeferredBlock<FlowerPotBlock> POT_PARADIJSBLOEM = pot(PARADIJSBLOEM);
    public static final DeferredBlock<FlowerPotBlock> POT_ORCHIDEE = pot(ORCHIDEE);

    // --- the beach, the capsule, the house -------------------------------------------------------------------------------------
    public static final DeferredBlock<SchillyEitjesBlock> SCHILLY_EITJES = BLOCKS.registerBlock("schilly_eitjes", SchillyEitjesBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TURTLE_EGG).mapColor(MapColor.COLOR_LIGHT_GREEN).randomTicks().noOcclusion());
    public static final DeferredBlock<ScannerBlock> VADSIGHEID_SCANNER = BLOCKS.registerBlock("vadsigheid_scanner", ScannerBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.5f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> 7));
    public static final DeferredBlock<GuhwaiiBlokken.Poster> VADSIGHEID_POSTER = BLOCKS.registerBlock("vadsigheid_poster", GuhwaiiBlokken.Poster::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.3f).sound(SoundType.WOOL).noOcclusion()
                    .noCollission().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<GuhwaiiBlokken.Rommeltje> ROMMELTJE = BLOCKS.registerBlock("guhwaii_rommeltje", GuhwaiiBlokken.Rommeltje::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instabreak().sound(SoundType.WOOL).noOcclusion().noCollission()
                    .pushReaction(PushReaction.DESTROY));

    // --- items -------------------------------------------------------------------------------------------------------------------
    public static final FoodProperties KOKOS_ETEN = new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build();
    public static final FoodProperties KOKOSMELK_ETEN = new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).alwaysEdible()
            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1f).build();
    public static final DeferredItem<GuhwaiiItems.KokosnootItem> KOKOSNOOT_ITEM = ITEMS.registerItem("kokosnoot", GuhwaiiItems.KokosnootItem::new,
            () -> new Item.Properties().food(KOKOS_ETEN));
    public static final DeferredItem<GuhwaiiItems.KokosmelkItem> KOKOSMELK = ITEMS.registerItem("kokosmelk", GuhwaiiItems.KokosmelkItem::new,
            () -> new Item.Properties().food(KOKOSMELK_ETEN).stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
    public static final DeferredItem<GuhwaiiItems.UkeleleItem> UKELELE = ITEMS.registerItem("guhwaii_ukelele", GuhwaiiItems.UkeleleItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    static {
        for (DeferredBlock<?> block : List.of(PALM_STAM, PALM_PLANKEN)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        for (DeferredBlock<?> block : List.of(PALM_GEZICHT, PALM_BLAD, SCHILLY_EITJES, ROMMELTJE, ROZE_HIBISCUS, PLUMERIA, PARADIJSBLOEM, ORCHIDEE)) {
            ITEMS.registerItem(block.getId().getPath(), p -> new GuhwaiiItems.LoreBlockItem(block.get(), p));
        }
        ITEMS.registerItem("vadsigheid_scanner", p -> new GuhwaiiItems.LoreBlockItem(VADSIGHEID_SCANNER.get(), p),
                () -> new Item.Properties().rarity(Rarity.RARE));
        ITEMS.registerItem("vadsigheid_poster", p -> new GuhwaiiItems.LoreBlockItem(VADSIGHEID_POSTER.get(), p),
                () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    // --- worldgen ------------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, GuhwaiiWorldgen.Palm> PALM_FEATURE = FEATURES.register("guhwaii_palm",
            () -> new GuhwaiiWorldgen.Palm(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhwaiiWorldgen.Rif> RIF_FEATURE = FEATURES.register("guhwaii_rif",
            () -> new GuhwaiiWorldgen.Rif(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhwaiiWorldgen.Nestje> NESTJE_FEATURE = FEATURES.register("guhwaii_nestje",
            () -> new GuhwaiiWorldgen.Nestje(NoneFeatureConfiguration.CODEC));

    private static DeferredBlock<GuhwaiiBlokken.TropischeBloem> bloem(String id, MapColor colour) {
        return BLOCKS.registerBlock(id, p -> new GuhwaiiBlokken.TropischeBloem(MobEffects.REGENERATION, 4f, p),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).mapColor(colour).noOcclusion());
    }

    private static DeferredBlock<FlowerPotBlock> pot(DeferredBlock<GuhwaiiBlokken.TropischeBloem> bloem) {
        return BLOCKS.registerBlock("potted_" + bloem.getId().getPath(), p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, bloem, p),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener(GuhwaiiFeature::setup);
        NeoForge.EVENT_BUS.register(GuhwaiiEvents.class);
        nl.juiced.guhs.feature.Protected.add(GuhwaiiEvents::beschermd);
        // the 626-guh: climbs, carries two, ukelele (+ hangs from ceilings: a goal of its own)
        VariantGedragen.zet(GuhVariant.STITCH626, Stitch626.GEDRAG);
        GuhHooks.doelen(Stitch626::doelen);
        // the ohana questline: Lilo-guh (default role), Nani-guh, the story copy of 626
        NpcRollen.zet(GuhNpcEntity.Kind.LILO_GUH, Ohana.LILO);
        NpcRollen.zet(GuhNpcEntity.Kind.NANI_GUH, Ohana.NANI);
        VerhaalGuhs.opKlik(VerhaalGuh.STITCH626, Ohana::klik626);
        Ohana.luisteraars();
        for (GuhClothes c : List.of(GuhClothes.GUHWAII_HULAROKJE, GuhClothes.GUHWAII_BLOEMENKRANS, GuhClothes.GUHWAII_STITCHOREN,
                GuhClothes.GUHWAII_SURFPLANKJE)) {
            KledingBronnen.bron(c, BRON);
        }
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(PALM_STAM.get(), 5, 5);
            fire.setFlammable(PALM_GEZICHT.get(), 5, 5);
            fire.setFlammable(PALM_PLANKEN.get(), 5, 20);
            fire.setFlammable(PALM_BLAD.get(), 30, 60);
            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            pot.addPlant(ROZE_HIBISCUS.getId(), POT_HIBISCUS);
            pot.addPlant(PLUMERIA.getId(), POT_PLUMERIA);
            pot.addPlant(PARADIJSBLOEM.getId(), POT_PARADIJSBLOEM);
            pot.addPlant(ORCHIDEE.getId(), POT_ORCHIDEE);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhwaiiPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(PALM_STAM, PALM_GEZICHT, PALM_BLAD, PALM_PLANKEN, ROZE_HIBISCUS, PLUMERIA, PARADIJSBLOEM, ORCHIDEE, SCHILLY_EITJES,
                VADSIGHEID_SCANNER, VADSIGHEID_POSTER)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(KOKOSNOOT_ITEM.get()));
        output.accept(new ItemStack(KOKOSMELK.get()));
        output.accept(new ItemStack(UKELELE.get()));
    }

    /**
     * Grants both advancements of a Guhwai'i moment: the hidden {@code guhs:quest/guhwaii_<naam>} (FTB) and the visible
     * {@code guhs:verhalen/guhwaii_<naam>} (the Guhverhalen tab).
     */
    public static void advancement(ServerPlayer player, String naam) {
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guhwaii_" + naam);
        nl.juiced.guhs.feature.gids.GidsFeature.grant(player, "verhalen/guhwaii_" + naam);
    }

    private GuhwaiiFeature() {
    }
}
