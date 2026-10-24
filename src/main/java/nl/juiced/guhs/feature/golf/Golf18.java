package nl.juiced.guhs.feature.golf;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;

/**
 * 1.7.0 "Rondje Guhmension" (design: ontwerp-guhs170-mijnen.md, ronde 25 and 31): the blocks of the back 9 of the guh golf
 * course, and what the Golfguh's shop sells for golfballetjes. Resources: tools/features/golf18.py.
 * <ul>
 *   <li>on the course (unbreakable, no item): ijsvilt (slippery felt), the sliding guh block and its rail, the hub of the
 *       turning arms, the snapping tooth's jaw, the 45 degree bank wall, the looping;</li>
 *   <li>building blocks: ruitjesvilt (checkered felt, three colours, with stairs and slabs);</li>
 *   <li>deco: the golf rack, golf balls on tees, a hole with a flag, the golf cart (you can sit in it) and the golf trophy
 *       (a reward, not for sale).</li>
 * </ul>
 */
public final class Golf18 {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);

    private static BlockBehaviour.Properties courseOnly(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    private static BlockBehaviour.Properties deco(MapColor colour, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(colour).sound(sound).strength(0.8f).noOcclusion();
    }

    // --- on the course ----------------------------------------------------------------------------------------------------
    /** IJsvilt: icy blue felt, the ball rolls on and on (Guhpiek). */
    public static final DeferredBlock<Block> IJSVILT = BLOCKS.registerSimpleBlock("golfbaan_ijsvilt",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LIGHT_BLUE_WOOL));
    public static final DeferredBlock<Block> SCHUIFGUH = BLOCKS.registerSimpleBlock("golfbaan_schuifguh", () -> courseOnly(Blocks.PINK_WOOL));
    public static final DeferredBlock<Golf18Blocks.Schuifrail> SCHUIFRAIL = BLOCKS.registerBlock("golfbaan_schuifrail",
            Golf18Blocks.Schuifrail::new, () -> courseOnly(Blocks.PINK_WOOL));
    /** The hub of the Kruimeldraaimolen: its four arms (molenwieken) turn round it one block lower. */
    public static final DeferredBlock<Block> DRAAIAS = BLOCKS.registerSimpleBlock("golfbaan_draaias", () -> courseOnly(Blocks.WHITE_WOOL));
    /** The upper jaw over a snapping tooth of the Grote Guhmond: the tooth hangs below it, and snaps down to the tongue. */
    public static final DeferredBlock<Block> HAPTAND = BLOCKS.registerSimpleBlock("golfbaan_haptand", () -> courseOnly(Blocks.PINK_WOOL));
    public static final DeferredBlock<Golf18Blocks.Bankhoek> BANKHOEK = BLOCKS.registerBlock("golfbaan_bankhoek",
            Golf18Blocks.Bankhoek::new, () -> courseOnly(Blocks.WHITE_CONCRETE).noOcclusion());
    public static final DeferredBlock<Golf18Blocks.Looping> LOOPING = BLOCKS.registerBlock("golfbaan_looping",
            Golf18Blocks.Looping::new, () -> courseOnly(Blocks.PINK_WOOL));

    // --- building blocks: ruitjesvilt ---------------------------------------------------------------------------------------
    /** The colours of ruitjesvilt: pink-white (the argyle vads sweater's), mint-white and yellow-white. */
    public static final List<String> RUITJES = List.of("ruitjesvilt", "ruitjesvilt_mint", "ruitjesvilt_geel");
    public static final DeferredBlock<Block> RUITJESVILT = ruitjes("ruitjesvilt", MapColor.COLOR_PINK);
    public static final DeferredBlock<Block> RUITJESVILT_MINT = ruitjes("ruitjesvilt_mint", MapColor.COLOR_LIGHT_GREEN);
    public static final DeferredBlock<Block> RUITJESVILT_GEEL = ruitjes("ruitjesvilt_geel", MapColor.COLOR_YELLOW);
    public static final DeferredBlock<StairBlock> RUITJESVILT_TRAP = trap("ruitjesvilt_trap", RUITJESVILT);
    public static final DeferredBlock<SlabBlock> RUITJESVILT_PLAAT = plaat("ruitjesvilt_plaat", RUITJESVILT);
    public static final DeferredBlock<StairBlock> RUITJESVILT_MINT_TRAP = trap("ruitjesvilt_mint_trap", RUITJESVILT_MINT);
    public static final DeferredBlock<SlabBlock> RUITJESVILT_MINT_PLAAT = plaat("ruitjesvilt_mint_plaat", RUITJESVILT_MINT);
    public static final DeferredBlock<StairBlock> RUITJESVILT_GEEL_TRAP = trap("ruitjesvilt_geel_trap", RUITJESVILT_GEEL);
    public static final DeferredBlock<SlabBlock> RUITJESVILT_GEEL_PLAAT = plaat("ruitjesvilt_geel_plaat", RUITJESVILT_GEEL);

    // --- deco ------------------------------------------------------------------------------------------------------------
    /** A golf bag with clubs and balls against the wall. */
    public static final DeferredBlock<GuhFurnitureBlock> GOLFREK = BLOCKS.registerBlock("golfrek",
            p -> new GuhFurnitureBlock(p, -1, new double[]{3, 0, 9, 13, 16, 16}), () -> deco(MapColor.COLOR_PINK, SoundType.WOOD));
    public static final DeferredBlock<Golf18Blocks.GolfbalTee> GOLFBAL_TEE = BLOCKS.registerBlock("golfbal_tee",
            Golf18Blocks.GolfbalTee::new, () -> deco(MapColor.SNOW, SoundType.WOOL).instabreak());
    public static final DeferredBlock<Golf18Blocks.GolfVlag> GOLF_VLAGGETJE = BLOCKS.registerBlock("golf_vlaggetje",
            Golf18Blocks.GolfVlag::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).noOcclusion());
    /** The golf cart: a pink guh cart with a striped roof; right-click to sit in it. */
    public static final DeferredBlock<GuhFurnitureBlock> GOLFKARRETJE = BLOCKS.registerBlock("golfkarretje",
            p -> new GuhFurnitureBlock(p, 0.45, new double[]{0, 0, 0, 16, 8, 16}, new double[]{0, 8, 12, 16, 16, 16}),
            () -> deco(MapColor.COLOR_PINK, SoundType.WOOD));
    public static final DeferredBlock<Golf18Blocks.Golftrofee> GOLFTROFEE = BLOCKS.registerBlock("golftrofee",
            Golf18Blocks.Golftrofee::new, () -> deco(MapColor.GOLD, SoundType.METAL).lightLevel(s -> 5));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Golf18Blocks.TrofeeBlockEntity>> TROFEE_BE = BLOCK_ENTITIES.register(
            "golftrofee", () -> new BlockEntityType<>(Golf18Blocks.TrofeeBlockEntity::new, GOLFTROFEE.get()));

    /** Keeps the Golfguh's chunk going while somebody plays the far holes of an 18-hole course (refreshed by the golfer). */
    public static final DeferredHolder<TicketType, TicketType> TICKET = TICKETS.register("golfbaan",
            () -> new TicketType(100L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED));

    // --- items -----------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> IJSVILT_ITEM = ITEMS.registerSimpleBlockItem(IJSVILT);
    public static final List<DeferredItem<BlockItem>> RUITJES_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(RUITJESVILT), ITEMS.registerSimpleBlockItem(RUITJESVILT_TRAP), ITEMS.registerSimpleBlockItem(RUITJESVILT_PLAAT),
            ITEMS.registerSimpleBlockItem(RUITJESVILT_MINT), ITEMS.registerSimpleBlockItem(RUITJESVILT_MINT_TRAP), ITEMS.registerSimpleBlockItem(RUITJESVILT_MINT_PLAAT),
            ITEMS.registerSimpleBlockItem(RUITJESVILT_GEEL), ITEMS.registerSimpleBlockItem(RUITJESVILT_GEEL_TRAP), ITEMS.registerSimpleBlockItem(RUITJESVILT_GEEL_PLAAT));
    public static final DeferredItem<BlockItem> GOLFREK_ITEM = ITEMS.registerSimpleBlockItem(GOLFREK);
    public static final DeferredItem<BlockItem> GOLFBAL_TEE_ITEM = ITEMS.registerSimpleBlockItem(GOLFBAL_TEE);
    public static final DeferredItem<BlockItem> GOLF_VLAGGETJE_ITEM = ITEMS.registerSimpleBlockItem(GOLF_VLAGGETJE);
    public static final DeferredItem<BlockItem> GOLFKARRETJE_ITEM = ITEMS.registerItem("golfkarretje", p -> new BlockItem(GOLFKARRETJE.get(), p),
            () -> new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> GOLFTROFEE_ITEM = ITEMS.registerItem("golftrofee", p -> new BlockItem(GOLFTROFEE.get(), p),
            () -> new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    private static DeferredBlock<Block> ruitjes(String id, MapColor colour) {
        return BLOCKS.registerSimpleBlock(id, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).mapColor(colour));
    }

    private static DeferredBlock<StairBlock> trap(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, p -> new StairBlock(van.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofFullCopy(van.get()));
    }

    private static DeferredBlock<SlabBlock> plaat(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(van.get()));
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TICKETS.register(modBus);
    }

    static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : RUITJES_ITEMS) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(IJSVILT_ITEM.get()));
        output.accept(new ItemStack(GOLFREK_ITEM.get()));
        output.accept(new ItemStack(GOLFBAL_TEE_ITEM.get()));
        output.accept(new ItemStack(GOLF_VLAGGETJE_ITEM.get()));
        output.accept(new ItemStack(GOLFKARRETJE_ITEM.get()));
        output.accept(new ItemStack(GOLFTROFEE_ITEM.get()));
    }

    private Golf18() {
    }
}
