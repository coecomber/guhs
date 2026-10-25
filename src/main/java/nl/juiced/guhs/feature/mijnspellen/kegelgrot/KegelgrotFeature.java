package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.mijnspellen.MijnspelBlokken;
import nl.juiced.guhs.feature.mijnspellen.Mijnspel;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Kegelgrot (1.7.0 "Onder de Wol", design ronde 41-43): a cave bowling alley under a little hut with a big pin on its
 * roof (go down with the Wolkenliftje). De Guhde (KEGELGUH, "the Guhde abides") lends you a pink wool ball: real
 * ten-pin bowling, 10 frames with strikes and spares ({@link Bowling}), on ten plush guh pins that fall over with a piep
 * and are set up again by themselves. Look where the ball should go, hold right-click for power, stand left or right on
 * the approach for a little hook. Three lanes: straight (with gutters), bending, and with obstacles ({@link KegelBaan}).
 * Kegelknopen buy the bowlingshirtje, the zweetbandje, building blocks (kegelbaanhout, retrotegels) and deco (a pin, a
 * ball rack, a neon sign). Game: {@link KegelGame}. Resources: tools/features/mijnspellen.py.
 */
public final class KegelgrotFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final String ID = "kegelen";
    public static final ResourceKey<Structure> STRUCTUUR = ResourceKey.create(Registries.STRUCTURE, Guhs.id("kegelgrot"));

    private static BlockBehaviour.Properties spelblok(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    private static BlockBehaviour.Properties deco(MapColor colour, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(colour).sound(sound).strength(0.8f).noOcclusion();
    }

    // --- game blocks ------------------------------------------------------------------------------------------------------
    /** The foul line of a lane (in the floor; FACING = down the lane, BAAN = which lane). */
    public static final DeferredBlock<MijnspelBlokken.Begin> BAANBEGIN = BLOCKS.registerBlock("kegelgrot_baanbegin", MijnspelBlokken.Begin::new,
            () -> spelblok(Blocks.BIRCH_PLANKS));
    /** The gutter along the straight lane. */
    public static final DeferredBlock<MijnspelBlokken.Laag> GOOT = BLOCKS.registerBlock("kegelgrot_goot", p -> new MijnspelBlokken.Laag(p, 0, 12, 16),
            () -> spelblok(Blocks.WHITE_CONCRETE).noOcclusion());
    /** A soft bumper along the bending lane and the obstacle lane. */
    public static final DeferredBlock<MijnspelBlokken.Laag> BUMPER = BLOCKS.registerBlock("kegelgrot_bumper", p -> new MijnspelBlokken.Laag(p, 0, 8, 16),
            () -> spelblok(Blocks.PINK_WOOL).noOcclusion());
    /** A plush post on the obstacle lane. */
    public static final DeferredBlock<MijnspelBlokken.Laag> PAALTJE = BLOCKS.registerBlock("kegelgrot_paaltje", p -> new MijnspelBlokken.Laag(p, 4, 14, 12),
            () -> spelblok(Blocks.PINK_WOOL).noOcclusion());
    /** Only drawn: the wool ball and the sliding Mika-blokje (their entities show these models). */
    public static final DeferredBlock<Block> WOLBAL_BLOK = BLOCKS.registerBlock("kegelgrot_wolbal", Block::new, () -> spelblok(Blocks.PINK_WOOL).noOcclusion());
    public static final DeferredBlock<Block> MIKABLOKJE = BLOCKS.registerBlock("kegelgrot_mikablokje", Block::new, () -> spelblok(Blocks.PURPLE_WOOL).noOcclusion());

    // --- building blocks ----------------------------------------------------------------------------------------------------
    /** Light polished lane wood. */
    public static final DeferredBlock<Block> KEGELBAANHOUT = BLOCKS.registerSimpleBlock("kegelbaanhout", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS));
    public static final DeferredBlock<StairBlock> KEGELBAANHOUT_TRAP = BLOCKS.registerBlock("kegelbaanhout_trap",
            p -> new StairBlock(KEGELBAANHOUT.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS));
    public static final DeferredBlock<SlabBlock> KEGELBAANHOUT_PLAAT = BLOCKS.registerBlock("kegelbaanhout_plaat", SlabBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS));
    /** Retro tiles, pink and mint checks (the floor of every bowling alley in the eighties, but guh). */
    public static final DeferredBlock<Block> RETROTEGEL = BLOCKS.registerSimpleBlock("retrotegel", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CONCRETE));
    public static final DeferredBlock<Block> RETROTEGEL_PAARS = BLOCKS.registerSimpleBlock("retrotegel_paars", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CONCRETE));

    // --- deco -----------------------------------------------------------------------------------------------------------------
    /** A plush guh pin (the very same as on the lanes; it doesn't fall over here). */
    public static final DeferredBlock<GuhFurnitureBlock> KEGELTJE = BLOCKS.registerBlock("kegeltje",
            p -> new GuhFurnitureBlock(p, -1, new double[]{5, 0, 5, 11, 15, 11}), () -> deco(MapColor.SNOW, SoundType.WOOL).instabreak());
    /** A rack with three wool balls. */
    public static final DeferredBlock<GuhFurnitureBlock> WOLBALREK = BLOCKS.registerBlock("wolbalrek",
            p -> new GuhFurnitureBlock(p, -1, new double[]{0, 0, 3, 16, 12, 13}), () -> deco(MapColor.COLOR_PINK, SoundType.WOOD));
    /** A neon sign with a pin and a ball (gives light). */
    public static final DeferredBlock<GuhFurnitureBlock> NEON_KEGELBORD = BLOCKS.registerBlock("neon_kegelbord",
            p -> new GuhFurnitureBlock(p, -1, new double[]{0, 0, 14, 16, 16, 16}), () -> deco(MapColor.COLOR_MAGENTA, SoundType.GLASS).lightLevel(s -> 13));

    // --- items ------------------------------------------------------------------------------------------------------------------
    /** The coin: earned per game, spent at De Guhde's (also works in the grijpmachine). */
    public static final DeferredItem<Item> KEGELKNOOP = ITEMS.registerSimpleItem("kegelknoop", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    /** The loaned wool ball (only while you bowl). */
    public static final DeferredItem<WolbalItem> WOLBAL = ITEMS.registerItem("kegelgrot_wolbal_leen", WolbalItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> KEGELBAANHOUT_ITEM = ITEMS.registerSimpleBlockItem(KEGELBAANHOUT);
    public static final DeferredItem<BlockItem> KEGELBAANHOUT_TRAP_ITEM = ITEMS.registerSimpleBlockItem(KEGELBAANHOUT_TRAP);
    public static final DeferredItem<BlockItem> KEGELBAANHOUT_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(KEGELBAANHOUT_PLAAT);
    public static final DeferredItem<BlockItem> RETROTEGEL_ITEM = ITEMS.registerSimpleBlockItem(RETROTEGEL);
    public static final DeferredItem<BlockItem> RETROTEGEL_PAARS_ITEM = ITEMS.registerSimpleBlockItem(RETROTEGEL_PAARS);
    public static final DeferredItem<BlockItem> KEGELTJE_ITEM = ITEMS.registerSimpleBlockItem(KEGELTJE);
    public static final DeferredItem<BlockItem> WOLBALREK_ITEM = ITEMS.registerSimpleBlockItem(WOLBALREK);
    public static final DeferredItem<BlockItem> NEON_KEGELBORD_ITEM = ITEMS.registerSimpleBlockItem(NEON_KEGELBORD);

    public static final List<DeferredItem<BlockItem>> BOUWBLOKKEN = List.of(KEGELBAANHOUT_ITEM, KEGELBAANHOUT_TRAP_ITEM, KEGELBAANHOUT_PLAAT_ITEM,
            RETROTEGEL_ITEM, RETROTEGEL_PAARS_ITEM);

    // --- the pins, the ball, the Mika-blokje -------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KegelEntity>> KEGEL = ding("kegelgrot_kegel", 0.3f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<KegelEntity>> BAL = ding("kegelgrot_bal", 0.4f, 0.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<KegelEntity>> SCHUIF = ding("kegelgrot_schuif", 0.7f, 0.6f);

    private static DeferredHolder<EntityType<?>, EntityType<KegelEntity>> ding(String id, float breed, float hoog) {
        return ENTITIES.register(id, () -> EntityType.Builder.<KegelEntity>of(KegelEntity::new, MobCategory.MISC).noLootTable().sized(breed, hoog)
                .clientTrackingRange(6).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    /** Prices in kegelknopen (a good game on medium earns ~35, on lastig ~50): clothes, deco. Blocks: 1 for 4. */
    public static final int PRIJS_SHIRTJE = 70, PRIJS_ZWEETBANDJE = 40, PRIJS_KEGELTJE = 20, PRIJS_REK = 40, PRIJS_NEON = 32;
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.KEGELGROT_BOWLINGSHIRTJE, GuhClothes.KEGELGROT_ZWEETBANDJE);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        Minigames.registerGame(ID, KegelGame::isPlaying);
        NpcRollen.zet(GuhNpcEntity.Kind.KEGELGUH, KegelRole.INSTANCE);
        NeoForge.EVENT_BUS.register(KegelEvents.class);
        Mijnspel.STRUCTUREN.add(STRUCTUUR);
        nl.juiced.guhs.item.SuperkompasItem.voegToe("minigames", "kegelgrot");   // (the Superkompas finds the nearest one)
        KledingBronnen.bron(GuhClothes.KEGELGROT_ZWEETBANDJE, ID, KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kegelknopen", PRIJS_ZWEETBANDJE));
        KledingBronnen.bron(GuhClothes.KEGELGROT_BOWLINGSHIRTJE, ID, KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kegelknopen", PRIJS_SHIRTJE));
        SpelGroepen.groep(new SpelGroepen.Groep(ID, SpelGroepen.Tijdperk.GROTE_GUHSPELEN, SpelGroepen.icoon("kegelknoop", Items.SNOWBALL),
                "kegelgrot", GuhNpcEntity.Kind.KEGELGUH, List.of("kegelen_makkelijk", "kegelen_medium", "kegelen_lastig")));
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KEGELKNOOP.get()));
        for (DeferredItem<BlockItem> item : BOUWBLOKKEN) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(KEGELTJE_ITEM.get()));
        output.accept(new ItemStack(WOLBALREK_ITEM.get()));
        output.accept(new ItemStack(NEON_KEGELBORD_ITEM.get()));
    }

    /** The whole bowling outfit (unlocked, or in your pockets): the advancement. */
    public static void checkKleding(net.minecraft.server.level.ServerPlayer player) {
        boolean all = true;
        for (GuhClothes c : KLEDING) {
            all &= KledingUnlocks.heeft(player, c) || player.getInventory().countItem(ModItems.clothingItem(c)) > 0;
        }
        if (all) {
            GuhAdvancements.grant(player, "kegelen_kleding");
        }
    }

    private KegelgrotFeature() {
    }
}
