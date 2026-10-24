package nl.juiced.guhs.feature.mijnspellen.karretjes;

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
import nl.juiced.guhs.feature.mijnspellen.MijnspelBlokken;
import nl.juiced.guhs.feature.mijnspellen.Mijnspel;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * Karretjes-wissels (1.7.0 "Onder de Wol", design ronde 41-43): the Wisselgrot, a cave hall under a little station hut
 * (go down with the Wolkenliftje). Conducteur Piepwiel (WISSELGUH) lets carts full of ore roll out of the tunnel; from
 * the balcony you throw the levers so every cart ends in the bin of its ore. A wrong bin and the cart is gone: three gone
 * and the shift is over. Ever more carts, ever faster; three networks (makkelijk, medium, lastig: more switches).
 * Ertsbonnetjes buy the conducteurspetje, building blocks (bielzenhout, ertsgruissteen) and deco (an ore cart to sit in,
 * a station clock, a signal lamp). Game: {@link KarretjesGame}; networks: {@link KarretjesNet}; the carts:
 * {@link ErtsKarretjeEntity}. Resources: tools/features/mijnspellen.py.
 */
public final class KarretjesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final String ID = "karretjes";
    public static final ResourceKey<Structure> STRUCTUUR = ResourceKey.create(Registries.STRUCTURE, Guhs.id("karretjeswissels"));

    private static BlockBehaviour.Properties spelblok(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    private static BlockBehaviour.Properties deco(MapColor colour, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(colour).sound(sound).strength(0.8f).noOcclusion();
    }

    // --- game blocks ------------------------------------------------------------------------------------------------------
    /** The tunnel mouth of a network (in the floor; FACING = the way the carts roll, BAAN = which network). */
    public static final DeferredBlock<MijnspelBlokken.Begin> SPOORBEGIN = BLOCKS.registerBlock("karretjes_spoorbegin", MijnspelBlokken.Begin::new,
            () -> spelblok(Blocks.SPRUCE_PLANKS));
    /** A bin for one ore (SOORT = KarretjesNet.Erts ordinal), laid by the game. */
    public static final DeferredBlock<MijnspelBlokken.Soort> ERTSBAK = BLOCKS.registerBlock("karretjes_ertsbak", p -> new MijnspelBlokken.Soort(p, 12),
            () -> spelblok(Blocks.SPRUCE_PLANKS).noOcclusion());

    // --- building blocks ----------------------------------------------------------------------------------------------------
    /** Old railway sleepers, a warm brown plank. */
    public static final DeferredBlock<Block> BIELZENHOUT = BLOCKS.registerSimpleBlock("bielzenhout", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS));
    public static final DeferredBlock<StairBlock> BIELZENHOUT_TRAP = BLOCKS.registerBlock("bielzenhout_trap",
            p -> new StairBlock(BIELZENHOUT.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS));
    public static final DeferredBlock<SlabBlock> BIELZENHOUT_PLAAT = BLOCKS.registerBlock("bielzenhout_plaat", SlabBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS));
    /** Grey stone speckled with every colour of ore. */
    public static final DeferredBlock<Block> ERTSGRUISSTEEN = BLOCKS.registerSimpleBlock("ertsgruissteen", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE));
    public static final DeferredBlock<Block> ERTSGRUIS_BAKSTENEN = BLOCKS.registerSimpleBlock("ertsgruis_bakstenen", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS));

    // --- deco -----------------------------------------------------------------------------------------------------------------
    /** An ore cart full of glittering ore: right-click to sit in it (choo choo... it doesn't ride). */
    public static final DeferredBlock<GuhFurnitureBlock> ERTSKARRETJE = BLOCKS.registerBlock("ertskarretje",
            p -> new GuhFurnitureBlock(p, 0.5, new double[]{1, 0, 1, 15, 11, 15}), () -> deco(MapColor.COLOR_GRAY, SoundType.METAL));
    /** The station clock of the Wisselgrot (it always says "five past guh"). */
    public static final DeferredBlock<GuhFurnitureBlock> STATIONSKLOKJE = BLOCKS.registerBlock("stationsklokje",
            p -> new GuhFurnitureBlock(p, -1, new double[]{2, 0, 6, 14, 16, 10}), () -> deco(MapColor.COLOR_BLACK, SoundType.METAL));
    /** A signal lamp, red over green (gives light). */
    public static final DeferredBlock<GuhFurnitureBlock> SEINLAMPJE = BLOCKS.registerBlock("seinlampje",
            p -> new GuhFurnitureBlock(p, -1, new double[]{5, 0, 5, 11, 16, 11}), () -> deco(MapColor.COLOR_BLACK, SoundType.LANTERN).lightLevel(s -> 12));

    // --- items ------------------------------------------------------------------------------------------------------------------
    /** The coin: earned per shift, spent at Conducteur Piepwiel's (also works in the grijpmachine). */
    public static final DeferredItem<Item> ERTSBONNETJE = ITEMS.registerSimpleItem("ertsbonnetje", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> BIELZENHOUT_ITEM = ITEMS.registerSimpleBlockItem(BIELZENHOUT);
    public static final DeferredItem<BlockItem> BIELZENHOUT_TRAP_ITEM = ITEMS.registerSimpleBlockItem(BIELZENHOUT_TRAP);
    public static final DeferredItem<BlockItem> BIELZENHOUT_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(BIELZENHOUT_PLAAT);
    public static final DeferredItem<BlockItem> ERTSGRUISSTEEN_ITEM = ITEMS.registerSimpleBlockItem(ERTSGRUISSTEEN);
    public static final DeferredItem<BlockItem> ERTSGRUIS_BAKSTENEN_ITEM = ITEMS.registerSimpleBlockItem(ERTSGRUIS_BAKSTENEN);
    public static final DeferredItem<BlockItem> ERTSKARRETJE_ITEM = ITEMS.registerSimpleBlockItem(ERTSKARRETJE);
    public static final DeferredItem<BlockItem> STATIONSKLOKJE_ITEM = ITEMS.registerSimpleBlockItem(STATIONSKLOKJE);
    public static final DeferredItem<BlockItem> SEINLAMPJE_ITEM = ITEMS.registerSimpleBlockItem(SEINLAMPJE);

    public static final List<DeferredItem<BlockItem>> BOUWBLOKKEN = List.of(BIELZENHOUT_ITEM, BIELZENHOUT_TRAP_ITEM, BIELZENHOUT_PLAAT_ITEM,
            ERTSGRUISSTEEN_ITEM, ERTSGRUIS_BAKSTENEN_ITEM);

    // --- the cart --------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<ErtsKarretjeEntity>> KAR = ENTITIES.register("karretjes_kar",
            () -> EntityType.Builder.<ErtsKarretjeEntity>of(ErtsKarretjeEntity::new, MobCategory.MISC).noLootTable().sized(0.98f, 0.7f)
                    .clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("karretjes_kar"))));

    /** Prices in ertsbonnetjes (a good shift on medium earns ~35, on lastig ~55): the cap, the deco. Blocks: 1 for 4. */
    public static final int PRIJS_PETJE = 50, PRIJS_KARRETJE = 64, PRIJS_KLOKJE = 24, PRIJS_SEINLAMPJE = 20;

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        Minigames.registerGame(ID, KarretjesGame::isPlaying);
        NpcRollen.zet(GuhNpcEntity.Kind.WISSELGUH, KarretjesRole.INSTANCE);
        NeoForge.EVENT_BUS.register(KarretjesEvents.class);
        Mijnspel.STRUCTUREN.add(STRUCTUUR);
        nl.juiced.guhs.item.SuperkompasItem.voegToe("minigames", "karretjeswissels");   // (the Superkompas finds the nearest one)
        KledingBronnen.bron(GuhClothes.KARRETJES_CONDUCTEURSPETJE, ID,
                KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.ertsbonnetjes", PRIJS_PETJE));
        SpelGroepen.groep(new SpelGroepen.Groep(ID, SpelGroepen.Tijdperk.GROTE_GUHSPELEN, SpelGroepen.icoon("ertsbonnetje", Items.MINECART),
                "karretjeswissels", GuhNpcEntity.Kind.WISSELGUH, List.of("karretjes_makkelijk", "karretjes_medium", "karretjes_lastig")));
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ERTSBONNETJE.get()));
        for (DeferredItem<BlockItem> item : BOUWBLOKKEN) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(ERTSKARRETJE_ITEM.get()));
        output.accept(new ItemStack(STATIONSKLOKJE_ITEM.get()));
        output.accept(new ItemStack(SEINLAMPJE_ITEM.get()));
    }

    /** The conducteurspetje unlocked (or in your pockets): the advancement. */
    public static void checkKleding(net.minecraft.server.level.ServerPlayer player) {
        GuhClothes c = GuhClothes.KARRETJES_CONDUCTEURSPETJE;
        if (nl.juiced.guhs.feature.kleding.KledingUnlocks.heeft(player, c)
                || player.getInventory().countItem(nl.juiced.guhs.registry.ModItems.clothingItem(c)) > 0) {
            nl.juiced.guhs.quest.GuhAdvancements.grant(player, "karretjes_kleding");
        }
    }

    private KarretjesFeature() {
    }
}
