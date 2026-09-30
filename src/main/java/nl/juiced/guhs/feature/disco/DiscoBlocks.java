package nl.juiced.guhs.feature.disco;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * The blocks and items of the Guhdisco: the four Simon-says tiles, the twinkling dance floor, the mirror blocks of the
 * disco ball, the guhshakes on the bar and the discomunt (the coin you earn by dancing).
 */
public final class DiscoBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** disco_tegel_roze / _blauw / _geel / _groen. */
    public static final Map<DiscoTileBlock.Kleur, DeferredBlock<DiscoTileBlock>> TEGELS = new EnumMap<>(DiscoTileBlock.Kleur.class);

    static {
        for (DiscoTileBlock.Kleur kleur : DiscoTileBlock.Kleur.values()) {
            DeferredBlock<DiscoTileBlock> block = BLOCKS.registerBlock("disco_tegel_" + kleur.id(), p -> new DiscoTileBlock(kleur, p),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.GLASS)
                            .lightLevel(s -> s.getValue(DiscoTileBlock.LIT) ? 15 : 5));
            TEGELS.put(kleur, block);
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    public static final DeferredBlock<DanceFloorBlock> DANSVLOER = BLOCKS.registerBlock("disco_dansvloer", DanceFloorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.8f).sound(SoundType.GLASS).lightLevel(s -> 10));
    public static final DeferredBlock<Block> DISCOBAL = BLOCKS.registerSimpleBlock("disco_bal",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.AMETHYST).lightLevel(s -> 12));
    public static final DeferredBlock<DiscoMilkshakeBlock> MILKSHAKE = BLOCKS.registerBlock("disco_milkshake", DiscoMilkshakeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3f).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 4));

    public static final DeferredItem<BlockItem> DANSVLOER_ITEM = ITEMS.registerSimpleBlockItem(DANSVLOER);
    public static final DeferredItem<BlockItem> DISCOBAL_ITEM = ITEMS.registerSimpleBlockItem(DISCOBAL);
    public static final DeferredItem<BlockItem> MILKSHAKE_ITEM = ITEMS.registerSimpleBlockItem(MILKSHAKE);

    /** The Guhdisco's coin: earned by dancing, spent at the DJ-guh on the disco outfit. */
    public static final DeferredItem<Item> DISCOMUNT = ITEMS.registerSimpleItem("discomunt", () -> new Item.Properties());

    private DiscoBlocks() {
    }
}
